package com.github.teocci.ntptimesync;

import com.github.teocci.ntptimesync.Utils.LogHelper;

import java.io.Serializable;


/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Feb-02
 */

public class NTPRequest implements Serializable
{
    private static final String TAG = LogHelper.makeLogTag(TimeClient.class);

    private static final long serialVersionUID = 1L;

    private long t1; //time at client when client request was transmitted
    private long t2; //time at server when client request was received
    private long t3; //time at server after adding random delay
    private long t4; //time at client when server response was received
    private double offset;
    private double delay;

    public NTPRequest() {}

    public long getT1()
    {
        return t1;
    }

    public void setT1(long t1)
    {
        this.t1 = t1;
    }

    public long getT2()
    {
        return t2;
    }

    public void setT2(long t2)
    {
        this.t2 = t2;
    }

    public long getT3()
    {
        return t3;
    }

    public void setT3(long t3)
    {
        this.t3 = t3;
    }

    public long getT4()
    {
        return t4;
    }

    public void setT4(long t4)
    {
        this.t4 = t4;
    }

    public double getDelay()
    {
        return delay;
    }

    public double getOffset()
    {
        return offset;
    }


    /**
     * Calculates D and O values
     */
    public void calculateOandD()
    {
        /**
         * Delay evaluation formula :
         * 		delay = t + t’ = T(i-2) - T(i-3) + T (i) - T(i-1)
         *
         *  Our mapping is:
         *  T(i-3) -> T1
         *  T(i-2) -> T2
         *  T(i-1) -> T3
         *  T(i)   -> T4
         *
         * Revised formula for our notation :
         *  	delay = T2 - T1 + T4 - T3
         */
        delay = (t2 - t1) + (t4 - t3);

        /**
         * Offset formula :
         * 		offset = 1/2 * (T(i-2) - T(i-3) + T(i-1) - T(i))
         *
         * Revised formula for our notation :
         * 		offset = 1/2 * (T2 - T1 + T3 - T4)
         */
        offset = 0.5 * (t2 - t1 + t3 - t4);

        // Print these values
        LogHelper.e(TAG, offset + "\t\t" + delay);
    }

    /**
     * Calculates the min accuracy of this measurement
     *
     * @return min accuracy
     */
    public double getAccuracyMin()
    {
        return offset - (delay / 2);
    }

    /**
     * Calculates the max accuracy of this measurement
     *
     * @return max accuracy
     */
    public double getAccuracyMax()
    {
        return offset + (delay / 2);
    }
}
