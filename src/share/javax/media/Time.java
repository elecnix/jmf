/*
 * @(#)Time.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 *
 * Time is kept at nanosecond precision.
 * <p>
 * This is the JMF 1.0 
 * <a href = http://www.javasoft.com/products/java-media/jmf/forDevelopers/playerapi/javax.media.Time.html> Time </a> 
 * interface.
 *
 * @version 1.6 98/06/23
 */

public class Time implements java.io.Serializable {

    public final static long   	ONE_SECOND  = 1000000000L;
    public final static Time 	TIME_UNKNOWN = new Time(Long.MAX_VALUE - 1);
    private final static double	NANO_TO_SEC = 1.0E-9;

    /**
     * Time in nanoseconds.
     */
    protected long nanoseconds;

    /**
     * Constructs a Time using nanoseconds.
     *
     * @param nano Number of nanoseconds for this time.
     */
    public Time(long nanoseconds) {

	this.nanoseconds = nanoseconds;
    }

    /**
     * Constructs a Time using seconds.
     *
     * @param seconds Time specified in seconds.
     */
    public Time(double seconds) {

	nanoseconds = secondsToNanoseconds(seconds);
    }

    /**
     * Converts seconds to nanoseconds.
     */
    protected long secondsToNanoseconds(double seconds) {

	return (long)(seconds * ONE_SECOND);
    }

    /**
     * Gets the time value in nanoseconds.
     *
     * @return time value in nanoseconds.
     */
    public long	getNanoseconds() {

	return nanoseconds;
    }

    /**
     * Gets the time value in seconds.
     *
     * @return time value in seconds.
     */
    public double getSeconds() {

	return nanoseconds * NANO_TO_SEC;
    }
}
