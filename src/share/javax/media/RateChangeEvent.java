/*
 * @(#)RateChangeEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>RateChangeEvent</code> is a <code>ControllerEvent</code> that is posted when 
 * a <code>Controller's</code> rate changes.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21.
 */
public class RateChangeEvent extends ControllerEvent {


    float rate;
    
    public RateChangeEvent(Controller from, float newRate) {
	super(from);
	rate = newRate;
    }

    /**
     * Get the new rate of the <code>Controller</code> that
     * generated this event.
     *
     * @return The <code>Controller's</code> new rate.
     */
    public float getRate() {
	return rate;
    }

    /**
     * Returns the String representation of this event's values.
     */
    public String toString() {
	return getClass().getName() + "[source=" + eventSrc + 
	    ",rate=" + rate + "]";
    }
}
