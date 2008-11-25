/*
 * @(#)SessionEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/** 
 * SessionEvent are events that pertain to the Session as a whole
 * and that dont belong to a ReceiveStream in particular or a remote
 * participant necessarily. SessionEvent is one of
 * NewParticipantEvent or LocalCollisionEvent.
 */
public class SessionEvent extends RTPEvent{

    public SessionEvent (SessionManager from){
	super(from);
    }
}
	
