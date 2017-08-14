package com.github.teocci.ntptimesync;

import com.github.teocci.ntptimesync.Utils.LogHelper;
import com.github.teocci.ntptimesync.Utils.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeServer
{
    private static final String TAG = LogHelper.makeLogTag(TimeServer.class);

    private ServerSocket serverSocket;
    NTPRequest ntpRequest;

    /**
     * Start Time server and listen for connections
     */
    public TimeServer()
    {
        try {
            serverSocket = new ServerSocket(Util.HOST_PORT);
            LogHelper.e(TAG, "Server started on port: " + Util.HOST_PORT);
            LogHelper.e(TAG, "waiting for connection");

            // Always keep trying for new client connections
            while (true) {
                try {
                    Socket incomingSocket = serverSocket.accept();

                    // Handle the incoming NTP request on a new thread
                    NTPRequestHandler ntpReqHandler = new NTPRequestHandler(incomingSocket);
                    new Thread(ntpReqHandler).start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                serverSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static void main(String[] args)
    {
        new TimeServer();
    }


    /**
     * NTPRequest Handler for the server side
     */
    private class NTPRequestHandler implements Runnable
    {
        private Socket clientSocket;

        public NTPRequestHandler(Socket clientSocket)
        {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run()
        {
            InputStream is;
            try {
                is = clientSocket.getInputStream();
                ObjectInputStream oIs = new ObjectInputStream(is);
                ntpRequest = (NTPRequest) oIs.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            // Emulate network delay - sleep before recording time stamps
            Util.sleepThread(Util.getRandomDelay());

            // set T2 value
            ntpRequest.setT2(System.nanoTime() + Util.SERVER_OFFSET);

            // add random delay between 10 to 100 ms - simulating processing delay (not required)
            Util.sleepThread(Util.getRandomDelay());

            // set T3 value
            ntpRequest.setT3(System.nanoTime() + Util.SERVER_OFFSET);

            // Respond to client
            sendNTPAnswer(ntpRequest);
        }

        private void sendNTPAnswer(NTPRequest request)
        {
            // write to client socket
            try {
                ObjectOutputStream oOs = new ObjectOutputStream(clientSocket.getOutputStream());
                oOs.flush(); // -TODO- Flush before write?
                oOs.writeObject(request);
                oOs.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            // Close socket
            try {
                clientSocket.close();
            } catch (Exception e) {
                LogHelper.e(TAG, "failed to close socket");
                e.printStackTrace();
            }
        }
    }
}
