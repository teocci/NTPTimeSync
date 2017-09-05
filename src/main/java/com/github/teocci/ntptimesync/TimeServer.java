package com.github.teocci.ntptimesync;

import com.github.teocci.ntptimesync.Utils.LogHelper;
import com.github.teocci.ntptimesync.Utils.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static com.github.teocci.ntptimesync.Utils.Config.LOCAL_HOST_PORT;
import static com.github.teocci.ntptimesync.Utils.Config.SERVER_OFFSET;

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
            serverSocket = new ServerSocket(LOCAL_HOST_PORT);
            LogHelper.e(TAG, "Server started on port: " + LOCAL_HOST_PORT);
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
                ObjectInputStream reader = new ObjectInputStream(is);
                ntpRequest = (NTPRequest) reader.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            // Emulate network delay - sleep before recording time stamps
            Util.sleepThread(Util.getRandomDelay());

            // set T2, which is the server's timestamp of the request packet reception,
//            ntpRequest.setT2(System.nanoTime() + SERVER_OFFSET);
            ntpRequest.setT2(System.currentTimeMillis() + SERVER_OFFSET);

            // add random delay between 10 to 100 ms - simulating processing delay (not required)
            Util.sleepThread(Util.getRandomDelay());

            // set T3, which is the server's timestamp of the response packet transmission
//            ntpRequest.setT3(System.nanoTime() + SERVER_OFFSET);
            ntpRequest.setT3(System.currentTimeMillis() + SERVER_OFFSET);

            // Respond to client
            sendNTPAnswer(ntpRequest);
        }

        private void sendNTPAnswer(NTPRequest request)
        {
            // write to client socket
            try {
                ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
                writer.flush(); // -TODO- Flush before write?
                writer.writeObject(request);
                writer.close();
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
