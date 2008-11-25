/*
 * @(#)RestartingEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>RestartingEvent</code> indicates that a <code>Controller</code> has moved from
 * the <i>Started</i> state back to the <i>Prefetching</i> state
 * (a <i>Stopped</i> state) and intends to return to the
 * <i>Started</i> state when <i>Prefetching</i> is complete.
 * This  occurs when a <i>Started</i>&nbsp;<code>Player</code>
 * is asked to change its rate or media time
 * and to fulfill the request must prefetch its media again.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21.
*/

public class RestartingEvent extends StopEvent {

    public RestartingEvent(Controller from,
			   int previous, int current, int target,
			   Time mediaTime) {
	super(from, previous, current, target, mediaTime);
    }
}
