/*
 * @(#)RealizeCompleteEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>RealizeCompleteEvent</code> is posted when a <code>Controller</code> finishes
 * <I>Realizing</I>. This occurs when a <code>Controller</code> moves
 * from the <i>Realizing</i> state to the <i>Realized</i>
 * state, or as an acknowledgement that the <code>realize</code>
 * method was called and the <code>Controller</code> is already
 * <i>Realized</i>.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21
 */
public class RealizeCompleteEvent extends TransitionEvent {

    public RealizeCompleteEvent(Controller from,
				int previous, int current, int target) {
	super(from, previous, current, target);
    }
}
