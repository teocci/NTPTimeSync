package com.github.teocci.ntptimesync;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.time.Clock;

import com.github.teocci.ntptimesync.Utils.LogHelper;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpUtils;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import static com.github.teocci.ntptimesync.Utils.Config.BASE_HOST_PORT;
import static com.github.teocci.ntptimesync.Utils.Config.LOCAL_HOST_PORT;

/**
 * This is an example program demonstrating how to use the NTPUDPClient
 * class. This program sends a Datagram client request packet to a
 * Network time Protocol (NTP) service port on a specified server,
 * retrieves the time, and prints it to standard output along with
 * the fields from the NTP message header (e.g. stratum level, reference id,
 * poll interval, root delay, mode, ...)
 * See <A HREF="ftp://ftp.rfc-editor.org/in-notes/rfc868.txt"> the spec </A>
 * for details.
 * <p>
 * Usage: NTPClient <hostname-or-address-list>
 * <br>
 * Example: NTPClient clock.psu.edu
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Aug-18
 */
public class NTPClient
{
    private static final String TAG = LogHelper.makeLogTag(NTPClient.class);

    public static final int MTU = 1500;

    private static final NumberFormat numberFormat = new java.text.DecimalFormat("0.00");

    private static byte[] buffer = new byte[MTU];

    /**
     * Process <code>TimeInfo</code> object and print its details.
     *
     * @param info <code>TimeInfo</code> object.
     */
    public static void processResponse(TimeInfo info)
    {
        NtpV3Packet message = info.getMessage();
        int stratum = message.getStratum();
        String refType;
        if (stratum <= 0) {
            refType = "(Unspecified or Unavailable)";
        } else if (stratum == 1) {
            refType = "(Primary Reference; e.g., GPS)"; // GPS, radio clock, etc.
        } else {
            refType = "(Secondary Reference; e.g. via NTP or SNTP)";
        }
        // stratum should be 0..15...
        LogHelper.e(TAG, " Stratum: " + stratum + " " + refType);
        int version = message.getVersion();
        int li = message.getLeapIndicator();
        LogHelper.e(TAG, " leap=" + li + ", version="
                + version + ", precision=" + message.getPrecision());

        LogHelper.e(TAG, " mode: " + message.getModeName() + " (" + message.getMode() + ")");
        int poll = message.getPoll();
        // poll value typically btwn MINPOLL (4) and MAXPOLL (14)
        LogHelper.e(TAG, " poll: " + (poll <= 0 ? 1 : (int) Math.pow(2, poll))
                + " seconds" + " (2 ** " + poll + ")");
        double disp = message.getRootDispersionInMillisDouble();
        LogHelper.e(TAG, " rootdelay=" + numberFormat.format(message.getRootDelayInMillisDouble())
                + ", rootdispersion(ms): " + numberFormat.format(disp));

        int refId = message.getReferenceId();
        String refAddr = NtpUtils.getHostAddress(refId);
        String refName = null;
        if (refId != 0) {
            if (refAddr.equals("127.127.1.0")) {
                refName = "LOCAL"; // This is the ref address for the Local Clock
            } else if (stratum >= 2) {
                // If reference id has 127.127 prefix then it uses its own reference clock
                // defined in the form 127.127.clock-type.unit-num (e.g. 127.127.8.0 mode 5
                // for GENERIC DCF77 AM; see refclock.htm from the NTP software distribution.
                if (!refAddr.startsWith("127.127")) {
                    try {
                        InetAddress addr = InetAddress.getByName(refAddr);
                        String name = addr.getHostName();
                        if (name != null && !name.equals(refAddr)) {
                            refName = name;
                        }
                    } catch (UnknownHostException e) {
                        // some stratum-2 servers sync to ref clock device but fudge stratum level higher... (e.g. 2)
                        // ref not valid host maybe it's a reference clock name?
                        // otherwise just show the ref IP address.
                        refName = NtpUtils.getReferenceClock(message);
                    }
                }
            } else if (version >= 3 && (stratum == 0 || stratum == 1)) {
                refName = NtpUtils.getReferenceClock(message);
                // refname usually have at least 3 characters (e.g. GPS, WWV, LCL, etc.)
            }
            // otherwise give up on naming the beast...
        }
        if (refName != null && refName.length() > 1) {
            refAddr += " (" + refName + ")";
        }

        LogHelper.e(TAG, " Reference Identifier:\t" + refAddr);

        TimeStamp refNtpTime = message.getReferenceTimeStamp();
//        LogHelper.e(TAG," Reference Timestamp:\t" + refNtpTime + "  " + refNtpTime.toDateString());

        // Originate Time is time request sent by client (t1)
        TimeStamp origNtpTime = message.getOriginateTimeStamp();
//        LogHelper.e(TAG," Originate Timestamp:\t" + origNtpTime + "  " + origNtpTime.toDateString());

        long destTime = info.getReturnTime();
        // Receive Time is time request received by server (t2)
        TimeStamp rcvNtpTime = message.getReceiveTimeStamp();
//        LogHelper.e(TAG," Receive Timestamp:\t" + rcvNtpTime + "  " + rcvNtpTime.toDateString());

        // Transmit time is time reply sent by server (t3)
        TimeStamp xmitNtpTime = message.getTransmitTimeStamp();
//        LogHelper.e(TAG," Transmit Timestamp:\t" + xmitNtpTime + "  " + xmitNtpTime.toDateString());

        // Destination time is time reply received by client (t4)
        TimeStamp destNtpTime = TimeStamp.getNtpTime(destTime);
        LogHelper.e(TAG," Destination Timestamp:\t" + destNtpTime + "  " + destNtpTime.toDateString());

        TimeStamp currentNtpTime = TimeStamp.getCurrentTime();
        LogHelper.e(TAG, " Current NTP Timestamp:\t" + currentNtpTime + "  " + currentNtpTime.toDateString());
        setLong(currentNtpTime.getSeconds(), 8, 12);
        setLong(currentNtpTime.getFraction(), 12, 16);

        LogHelper.e(TAG, " Current NTP Timestamp (seconds)\t: " + currentNtpTime.getSeconds());
        LogHelper.e(TAG, " Current NTP Timestamp (fraction)\t: " + currentNtpTime.getFraction());

        Clock.systemDefaultZone();
        LogHelper.e(TAG, " System Default Zone: " + Clock.systemDefaultZone());

        info.computeDetails(); // compute offset/delay if not already done
        Long offsetValue = info.getOffset();
        Long delayValue = info.getDelay();
        String delay = (delayValue == null) ? "N/A" : delayValue.toString();
        String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

        LogHelper.e(TAG, " Roundtrip delay(ms) = " + delay
                + ", clock offset(ms) = " + offset); // offset in ms
    }

    private static void setLong(long n, int begin, int end)
    {
        for (end--; end >= begin; end--) {
            buffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }

    public static void main(String[] args)
    {
        if (args.length == 0) {
            System.err.println("Usage: NTPClient [-local] <hostname-or-address-list>");
            System.exit(1);
        }

        NTPUDPClient client = new NTPUDPClient();
        // We want to timeout if a response takes longer than 10 seconds
        client.setDefaultTimeout(10000);
        try {
            client.open();
            boolean isLocal = false;
            for (String arg : args) {
                if (arg.equals("-local")) {
                    isLocal = true;
                    continue;
                }
                LogHelper.e(TAG, "");
                try {
                    InetAddress hostAddr = InetAddress.getByName(arg);
                    LogHelper.e(TAG, "> " + hostAddr.getHostName() + "/" + hostAddr.getHostAddress());
                    TimeInfo info = client.getTime(hostAddr, isLocal ? LOCAL_HOST_PORT : BASE_HOST_PORT);
                    processResponse(info);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        client.close();
    }
}