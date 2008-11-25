/*
 * @(#)StopByRequestEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>StopByRequestEvent</code> indicates that the <code>Controller</code> has stopped in response to a <code>stop</code> call. 
 * This event is posted as an acknowledgement even if the <code>Controller</code>
 * is already <i>Stopped</i>.
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21.
*/

public class StopByRequestEvent extends StopEvent {

    public StopByRequestEvent(Controller from,
			   int previous, int current, int target,
			   Time mediaTime) {
	super(from, previous, current, target, mediaTime);
    }
}
