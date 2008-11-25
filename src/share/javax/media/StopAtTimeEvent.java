/*
 * @(#)StopAtTimeEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>StopAtTimeEvent</code> indicates that the <code>Controller</code> has stopped because it reached 
 * its stop time.

 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21.
*/

public class StopAtTimeEvent extends StopEvent {

    public StopAtTimeEvent(Controller from,
			   int previous, int current, int target,
			   Time mediaTime) {
	super(from, previous, current, target, mediaTime);
    }
}
