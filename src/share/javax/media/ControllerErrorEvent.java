/*
 * @(#)ControllerErrorEvent.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <CODE>ControllerErrorEvent</CODE> describes an event that is
 * generated when an error condition occurs that will cause
 * a <code>Controller</code> to cease functioning.  Events
 * should only subclass from <code>ControllerErrorEvent</code> if the 
 * error being reported will result in catastrophic failure if action is
 * not taken, or if the <code>Controller</code> has already failed.
 *
 * A <CODE>ControllerErrorEvent</CODE> indicates that
 * the <code>Controller</code> is closed.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.3, 02/08/21
 */
public class ControllerErrorEvent extends ControllerClosedEvent {


    public ControllerErrorEvent(Controller from) {
        super(from);
    }

    public ControllerErrorEvent(Controller from, String why) {
        super(from, why);
    }
							     
    /**
     * Returns the String representation of this event's values.
     */
    public String toString() {
	return getClass().getName() + "[source=" + eventSrc + 
	    ",message=" + message + "]";
    }
}
