package com.github.teocci.ntptimesync;

import com.github.teocci.ntptimesync.Utils.LogHelper;
import com.github.teocci.ntptimesync.Utils.Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Feb-02
 */
public class TimeClient
{
    private static final String TAG = LogHelper.makeLogTag(TimeClient.class);

    private Socket clientSocket;
    private NTPRequest mNtpRequest;
    private NTPRequest minDelayNtpRequest;    // NTP request that had min value for delay


    /**
     * Start the client and calculate NTP values
     */
    public TimeClient()
    {
        try {
            mNtpRequest = new NTPRequest();

            LogHelper.e(TAG, "=========================");
            LogHelper.e(TAG, "  o \t\t  d");
            LogHelper.e(TAG, "=========================");

            // A total of 10 measurements
            for (int i = 0; i < 10; i++) {

                // Open a socket to server
                clientSocket = new Socket(InetAddress.getByName(Util.HOST_ADDR), Util.HOST_PORT);

                // Send NTP request
                sendNTPRequest();

                // Do measurements
                mNtpRequest.calculateOandD();

                // Check if this is the minimum delay NTP Request so far
                if (minDelayNtpRequest == null || mNtpRequest.getD() < minDelayNtpRequest.getD())
                    minDelayNtpRequest = mNtpRequest;

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
        mNtpRequest.setT1(System.currentTimeMillis());

        // send request object
        try {

            ObjectOutputStream oOs = new ObjectOutputStream(clientSocket.getOutputStream());
            oOs.writeObject(mNtpRequest);

            // wait for server's response
            ObjectInputStream oIs = new ObjectInputStream(clientSocket.getInputStream());
            mNtpRequest = (NTPRequest) oIs.readObject();

            // Close streams
            oOs.close();
            oIs.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }


        // Emulate network delay - sleep before recording time stamps
        Util.sleepThread(Util.getRandomDelay());

        // set t4
        mNtpRequest.setT4((long) (System.currentTimeMillis()));
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
        LogHelper.e(TAG, "Selected time difference   : " + minDelayNtpRequest.getD());
        LogHelper.e(TAG, "Corresponding clock offset : " + minDelayNtpRequest.getO());
        LogHelper.e(TAG, "Corresponding accuracy   : "
                + minDelayNtpRequest.getAccuracyMin()
                + " to "
                + minDelayNtpRequest.getAccuracyMax());
    }

}
