/*
 * @(#)TimeBase.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package javax.media;

/**
 * A <code>TimeBase</code> is a constantly ticking source of time,
 * much like a crystal.<p>
 *
 * Unlike a <code>Clock</code>, a <code>TimeBase</code> cannot be temporally
 * transformed, reset, or stopped.
 *
 * @see Clock
 * @version 1.4, 02/08/21.
 */
public interface TimeBase {

    /**
     * Get the current time of this <code>TimeBase</code>.
     *
     * @return the current <code>TimeBase</code> time.
     */
    public Time getTime();

    /**
     * Get the current time of the <code>TimeBase</code>
     * specified in nanoseconds.
     *
     * @return the current <code>TimeBase</code> time in
     * nanoseocnds.
     */
    public long getNanoseconds();

}
