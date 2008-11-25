/*
 * @(#)MediaTimeSetEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>MediaTimeSetEvent</code> is posted by a <code>Controller</code> when its 
 * media-time has been set with the <code>setMediaTime</code> method. 
 *  
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, MediaTimeSetEvent.java.
 */
public class MediaTimeSetEvent extends ControllerEvent {


    Time mediaTime;
    
    public MediaTimeSetEvent(Controller from, Time newMediaTime) {
	super(from);
	mediaTime = newMediaTime;
    }

    /**
     * Get the new media time of the <code>Controller</code> that
     * generated this event.
     *
     * @return The <code>Controller's</code> new media time.
     */
    public Time getMediaTime() {
	return mediaTime;
    }

    /**
     * Returns the String representation of this event's values.
     */
    public String toString() {
	return getClass().getName() + "[source=" + eventSrc + 
	    ",mediaTime=" + mediaTime + "]";
    }
}
