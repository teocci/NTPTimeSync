package com.github.teocci.ntptimesync;

import com.github.teocci.ntptimesync.Utils.LogHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.DecimalFormat;

import static com.github.teocci.ntptimesync.Utils.Config.BASE_HOST_PORT;
import static com.github.teocci.ntptimesync.Utils.Config.LOCAL_HOST_PORT;

/**
 * NtpClient - an NTP client for Java.  This program connects to an NTP server
 * and prints the response to the console.
 * <p>
 * The local clock offset calculation is implemented according to the SNTP
 * algorithm specified in RFC 2030.
 * <p>
 * Note that on windows platforms, the current time-of-day timestamp is limited
 * to an resolution of 10ms and adversely affects the accuracy of the results.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Aug-21
 */
public class SntpClient
{
    private static final String TAG = LogHelper.makeLogTag(SntpClient.class);

    public static void main(String[] args) throws IOException
    {
        String serverName;
        String local = "";

        // Process command-line args
        if (args.length == 1) {
            serverName = args[0];
        } else if (args.length == 2) {
            local = args[0];
            serverName = args[1];
        } else {
            printUsage();
            return;
        }

        // Send request
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(serverName);
        byte[] buf = new NtpMessage().toByteArray();
        DatagramPacket packet = new DatagramPacket(
                buf,
                buf.length,
                address,
                local.equals("-local") ? LOCAL_HOST_PORT : BASE_HOST_PORT
        );

        // Set the transmit timestamp *just* before sending the packet
        // ToDo: Does this actually improve performance or not?
        NtpMessage.encodeTimestamp(
                packet.getData(),
                40,
                (System.currentTimeMillis() / 1000.0) + 2208988800.0
        );

        socket.send(packet);

        // Get response
        LogHelper.e(TAG, "NTP request sent, waiting for response...\n");
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        // Immediately record the incoming timestamp
        double destinationTimestamp = (System.currentTimeMillis() / 1000.0) + 2208988800.0;


        // Process response
        NtpMessage msg = new NtpMessage(packet.getData());

        // Corrected, according to RFC2030 errata
        double roundTripDelay = (destinationTimestamp - msg.originateTimestamp) - (msg.transmitTimestamp - msg.receiveTimestamp);

        double localClockOffset = ((msg.receiveTimestamp - msg.originateTimestamp) +
                (msg.transmitTimestamp - destinationTimestamp)) / 2;


        // Display response
        LogHelper.e(TAG, "NTP server: " + serverName);
        LogHelper.e(TAG, msg.toString());

        LogHelper.e(TAG, "Dest. timestamp\t\t: " +
                NtpMessage.timestampToString(destinationTimestamp));

        LogHelper.e(TAG, "Round-trip delay\t\t: " +
                new DecimalFormat("0.00").format(roundTripDelay * 1000) + " ms");

        LogHelper.e(TAG, "Local clock offset\t: " +
                new DecimalFormat("0.00").format(localClockOffset * 1000) + " ms");

        socket.close();
    }


    /**
     * Prints usage
     */
    static void printUsage()
    {
        LogHelper.e(TAG, "NtpClient - an NTP client for Java.\n" + "\n" +
                "This program connects to an NTP server and prints the response to the console.\n" + "\n" + "\n" +
                "Usage: java NtpClient [-local] server\n"
        );
    }
}
