/*
 * @(#)RemoteEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * An RemoteEvent notifies users of events that occur from remote
 * participants. These events can be used by an RTCP monitor for
 * monitoring reception quality and statistics of the Session.
 * RemoteEvents are one of ReceiverReportEvent,
 * RecvSenderReportEvent or RemoteCollisionEvent.
 */
public class RemoteEvent extends RTPEvent{

    public RemoteEvent (SessionManager from){
	super(from);
    }
}
			   
