package com.github.teocci.ntptimesync;

import com.github.teocci.ntptimesync.Utils.LogHelper;
import com.github.teocci.ntptimesync.Utils.Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static com.github.teocci.ntptimesync.Utils.Config.SERVER_ADDR;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Feb-02
 */
public class TimeClient
{
    private static final String TAG = LogHelper.makeLogTag(TimeClient.class);

    private Socket clientSocket;
    private NTPRequest ntpRequest;
    private NTPRequest minDelayNtpRequest;    // NTP request that had min value for delay


    /**
     * Start the client and calculate NTP values
     */
    public TimeClient()
    {
        try {
            ntpRequest = new NTPRequest();

            LogHelper.e(TAG, "=========================");
            LogHelper.e(TAG, "  Offset \t\t Delay");
            LogHelper.e(TAG, "=========================");

            // A total of 10 measurements
            for (int i = 0; i < 10; i++) {

                // Open a socket to server
                clientSocket = new Socket(InetAddress.getByName(SERVER_ADDR), Util.HOST_PORT);

                // Send NTP request
                sendNTPRequest();

                // Do measurements
                ntpRequest.calculateOandD();

                // Check if this is the minimum delay NTP Request so far
                if (minDelayNtpRequest == null || ntpRequest.getDelay() < minDelayNtpRequest.getDelay())
                    minDelayNtpRequest = ntpRequest;

                // wait 300ms before next iteration
                Util.sleepThread(300);
            }

            // Calculate based on min value of d
            doFinalDelayCalculation();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNTPRequest()
    {
        // set T1
        ntpRequest.setT1(System.nanoTime());

        // send request object
        try {

            ObjectOutputStream oOs = new ObjectOutputStream(clientSocket.getOutputStream());
            oOs.writeObject(ntpRequest);

            // wait for server's response
            ObjectInputStream oIs = new ObjectInputStream(clientSocket.getInputStream());
            ntpRequest = (NTPRequest) oIs.readObject();

            // Close streams
            oOs.close();
            oIs.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }


        // Emulate network delay - sleep before recording time stamps
        Util.sleepThread(Util.getRandomDelay());

        // set t4
        ntpRequest.setT4((long) (System.currentTimeMillis()));
    }

    public static void main(String[] args)
    {
        new TimeClient();
    }

    /**
     * Selects a NTPRequest based on min value of delay for each request
     */
    private void doFinalDelayCalculation()
    {
        LogHelper.e(TAG, "------------------------");
        LogHelper.e(TAG, "Selected time difference   : " + minDelayNtpRequest.getDelay());
        LogHelper.e(TAG, "Corresponding clock offset : " + minDelayNtpRequest.getOffset());
        LogHelper.e(TAG, "Corresponding accuracy   : "
                + minDelayNtpRequest.getAccuracyMin()
                + " to "
                + minDelayNtpRequest.getAccuracyMax());
    }
}
