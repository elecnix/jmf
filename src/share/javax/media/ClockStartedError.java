/*
 * @(#)ClockStartedError.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * <code>ClockStartedError</code> is thrown by a <i>Started</i>&nbsp;<code>Clock</code> 
 * when a method is invoked that is not legal on a <code>Clock</code> in the <i>Started</i> 
 * state.  For example, this error is thrown if <code>syncStart</code> or 
 * <code>setTimeBase</code> is invoked on a <I>Started</I>&nbsp;<code>Clock</code>.
 * <code>ClockStartedError</code> is also thrown if <code>addController</code> is 
 * invoked on a <I>Started</I>&nbsp;<code>Player</code>.
 *
 * @see Player
 * @see Controller
 * @see Clock
 * @version 1.2, 02/08/21.
 */

public class ClockStartedError extends MediaError {

    /**
     * Construct a <CODE>ClockStartedError</CODE> that contains the specified reason message.
     */
    public ClockStartedError(String reason) {
       super(reason);
    }

    /**
     * Construct a <CODE>ClockStartedError</CODE> with no message.
     */
    public ClockStartedError() {
	super();
    }
}
