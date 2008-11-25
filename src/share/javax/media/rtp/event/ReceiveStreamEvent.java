/*
 * @(#)ReceiveStreamEvent.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * The ReceiveStreamEvent will notify a listener of all events that
 * are received on a particular ReceiveStream. This allows the user to
 * get details on all the ReceiveStreams as they transition through
 * various states. ReceiveStreamEvents can be one of 
 * ActiveReceiveStreamEvent
 * ApplicationEvent
 * InactiveReceiveStreamEvent
 * NewReceiveStreamEvent
 * PayloadChangeEvent
 * ReceiveStreamMappedEvent
 * TimeoutEvent
 */  
public class ReceiveStreamEvent extends RTPEvent{

    private ReceiveStream recvStream = null;
    private Participant participant = null;
    
    public ReceiveStreamEvent(SessionManager from,
			      ReceiveStream stream,
			      Participant participant){
	super(from);
	recvStream = stream;
	this.participant = participant;
    }

    public ReceiveStream getReceiveStream(){
	return recvStream;
    }

    public Participant getParticipant(){
	return participant;
    }
}
