/*
 * @(#)SendStreamEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * The SendStreamEvent will notify a listener of all events that
 * are received on a particular SendStream. This allows the user to
 * get details on all the SendStreams as they transition through
 * various states. SendStreamEvents can be one of 
 *
 * ActiveSendStreamEvent
 * InactiveSendStreamEvent
 * NewSendStreamEvent
 * SendPayloadChangeEvent
 */  
public class SendStreamEvent extends RTPEvent{

    private SendStream sendStream = null;
    private Participant participant = null;
    
    public SendStreamEvent(SessionManager from,
			      SendStream stream,
			      Participant participant){
	super(from);
	sendStream = stream;
	this.participant = participant;
    }

    public SendStream getSendStream(){
	return sendStream;
    }
    public Participant getParticipant(){
	return participant;
    }
}
