/*
 * @(#)ClockStoppedException.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>ClockStoppedException</code> is thrown when a method that 
 * expects the <I>Clock</I> to be <I>Started</I> is
 * called on a <I>Stopped</I>&nbsp;<code>Clock</code>.
 * For example, this exception is thrown if <code>mapToTimeBase</code> 
 * is called on a <I>Stopped</I>&nbsp;<code>Clock</code>.
 * 
 * @version 1.2, 02/08/21
 */

public class ClockStoppedException extends MediaException {

    public ClockStoppedException() {
	super();
    }
    
    public ClockStoppedException(String reason) {
	super(reason);
    }
}
