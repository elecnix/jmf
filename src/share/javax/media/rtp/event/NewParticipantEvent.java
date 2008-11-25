/*
 * @(#)NewParticipantEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;

/**Informs the RTP listener that an RTCP packet has just been received
 * from a heretofore unknown participant. Thus, an Participant
 * object has been created to  represent the new participant.
 */
public class NewParticipantEvent extends SessionEvent{
    /**
     * The new Participant object. 
     */
    private Participant participant;

    public NewParticipantEvent(SessionManager from,
			  Participant participant){
	super(from);
	this.participant = participant;
    }
    /**
     * The new Participant object. 
     */
    public Participant getParticipant(){
	return participant;
    }
}
