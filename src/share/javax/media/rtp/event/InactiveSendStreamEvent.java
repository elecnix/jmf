/*
 * @(#)InactiveSendStreamEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the SendStreamListener that data has stopped
 * arriving on this SendStream.
 */
public class InactiveSendStreamEvent extends SendStreamEvent{
 
    public InactiveSendStreamEvent(SessionManager from,
				   Participant participant,
				   SendStream sendStream){
	super(from, sendStream, participant);
	
    }
}
