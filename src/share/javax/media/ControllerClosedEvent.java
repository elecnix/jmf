/*
 * @(#)ControllerClosedEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <CODE>ControllerClosedEvent</CODE> describes an event that is
 * generated when an a <code>Controller</code> is closed. This implies
 * that the <code>Controller</code> is no longer operational.
 **/

public class ControllerClosedEvent extends ControllerEvent {

    protected String message;
    
    /**
     * Construct a <CODE>ControllerClosedEvent</CODE>.
     */
    public ControllerClosedEvent(Controller from) {
        super(from);
	message = new String("");
    }

    public ControllerClosedEvent(Controller from, String why) { 
	super(from);
	message = why;
    }

    /**
     * Obtain the message describing why this event
     * occurred.
     *
     * @return Message describing event cause.
     */
    public String getMessage() {
	return message;
    }
     
}
