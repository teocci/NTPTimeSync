package com.github.teocci.ntptimesync.Utils;

import java.util.concurrent.ThreadLocalRandom;

public class Util
{
    /**
     * Get a random delay between 10 - 100 ms
     *
     * @return
     */
    public static long getRandomDelay()
    {
        return ThreadLocalRandom.current().nextLong(10, 100 + 1);
    }

    /**
     * Attempts to sleep the calling thread for given duration
     *
     * @param duration
     */
    public static void sleepThread(long duration)
    {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
