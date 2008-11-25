/*
 * @(#)DeallocateEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>DeallocateEvent</code> is posted as an acknowledgement of the
 * invocation of the <code>deallocate</code> method. It implies that
 * the scarce resources associated with this <code>Controller</code>
 * are no longer available and must be reacquired.
 * <p>
 * A  <code>DeallocateEvent</code> can be posted at any time regardless of the <CODE>Controller's</CODE>
 * previous or current state.
 * <code>DeallocateEvent</code> is a <code>StopEvent</code> because if the <code>Controller</code>
 * is in the <I>Started</I> state when the event is posted, it transitions to one of the
 * <i>Stopped</i> states.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21.
*/

public class DeallocateEvent extends StopEvent {

    public DeallocateEvent(Controller from,
			   int previous, int current, int target,
			   Time mediaTime) {
	super(from, previous, current, target, mediaTime);
    }
}
