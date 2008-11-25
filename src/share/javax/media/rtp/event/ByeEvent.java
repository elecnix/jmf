/*
 * @(#)ByeEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTP listener that an RTCP 'BYE' indication has been
 * received.   If the packet was received from a passive receiver, the
 * recvStream argument will be null since  no ReceiveStream would have
 * been  created; the 'participant' argument indicates the party
 * leaving the  session.In this case the given Participant is no
 * longer valid  after  this method  returns (i.e. the RTPSM removes
 * it from its  participant list and drops its reference; further
 * method calls to  it will fail).  <P> 
 *
 * If the SSRC sending the BYE was sending RTP data, the
 * ReceiveStream   representing that data stream will be passed in as
 * the  'recvStream' argument; in this case there is  an implicit
 * removePlayer performed on the recvStream and it becomes invalid
 * as well.   In this case the participant may or may not be removed
 * from the  session and invalidated;  if the participant was
 * sending multiple data streams it is possible to send a BYE for
 * one of the  streams but not for the participant as a whole.  The
 * 'participantBye' parameter indicates whether  the participant
 * is leaving the session; this is always true for passive receivers
 * (see above  paragraph). <P>
 */
public class ByeEvent extends TimeoutEvent{
    /**
     *  A string sent by the terminating party, explaining
     * why it is  leaving the session
     */
    private String reason;
    public ByeEvent(SessionManager from,
		    Participant participant,
		    ReceiveStream recvStream,
		    String reason,
		    boolean participantBye){
	super(from, participant, recvStream, participantBye);
	this.reason = reason;
    }
    
    /**
     *  A string sent by the terminating party, explaining
     * why it is  leaving the session
     */
    public String getReason(){
	return reason;
    }
}
