package com.github.teocci.ntptimesync.Utils;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-23
 */
public class Config
{
    public static final String EOL = "\r\n";

    public static final String LOG_PREFIX = "[NTP]";

    public static final String SERVER_NAME = "NTP Server";

    /**
     * Network IP address
     */
    public static final String SERVER_ADDR = "192.168.1.130";

    /**
     * Network IP address
     */
    public static String HOST_ADDR = "127.0.0.1";

    /**
     * Network port
     */
    public static int HOST_PORT = 27780;

    /**
     * Induced offset between server and client clocks (client is lagging)
     */
    public static final long SERVER_OFFSET = 0;
}
