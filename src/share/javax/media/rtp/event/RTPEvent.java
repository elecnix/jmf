/*
 * @(#)RTPEvent.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;
import javax.media.MediaEvent;
import java.util.EventObject;

/** The Base class of all event notification in the
 * SessionManager. All RTP Events must extend this base class
 */
public class RTPEvent extends MediaEvent{
    /** The SessionManager generating the RTPEvent
     */
    private SessionManager eventSrc;
    /**RTPEvent constructor takes in the SessionManager which has
     * generated this event as a argument to its constructor
     * @param from The SessionManager generating this event
     */
    public RTPEvent (SessionManager from){
	super(from);
	eventSrc = from;
    }
   
    /**The source of this RTPEvent
     */
    public Object getSource(){
	return eventSrc;
    }
    /**
     * The SessionManager which is the source of this RTPEvent
     */
    public SessionManager getSessionManager(){
	return eventSrc;
    }
    /**The String representation of this event
     */
    public String toString(){
	return getClass().getName() + "[source = " + eventSrc+ "]";
    }
}
