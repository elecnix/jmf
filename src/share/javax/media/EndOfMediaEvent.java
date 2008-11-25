/*
 * @(#)EndOfMediaEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * An <code>EndOfMediaEvent</code> indicates that the <code>Controller</code> 
 * has reached the end of its media and is stopping.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21.
*/

public class EndOfMediaEvent extends StopEvent {

    public EndOfMediaEvent(Controller from,
			   int previous, int current, int target,
			   Time mediaTime) {
	super(from, previous, current, target, mediaTime);
    }
}
