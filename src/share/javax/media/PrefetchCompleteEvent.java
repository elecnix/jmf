/*
 * @(#)PrefetchCompleteEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>PrefetchCompleteEvent</code> is posted when a <code>Controller</code> finishes
 * <I>Prefetching</I>. This occurs when a <code>Controller</code> 
 * moves from the <i>Prefetching</i> state to the <i>Prefetched</i>
 * state, or as an acknowledgement that the <code>prefetch</code>
 * method was called and the <code>Controller</code> is already <i>Prefetched</i>.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21.
 */
public class PrefetchCompleteEvent extends TransitionEvent {

    public PrefetchCompleteEvent(Controller from,
				 int previous, int current, int target) {
	super(from, previous, current, target);
    }
}
