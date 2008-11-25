/*
 * @(#)TimeoutEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTP listener that a certain SSRC has not sent packets
 * in a long  while and can be considered timed-out.  This call has
 * the exact  same semantics as the 'bye()'  callback, in that the
 * SSRC may  refer to a participant as a whole or just one of
 * several  of a participant's ReceiveStreams.  In other words,
 * RTPSM treats an SSRC timing  out the same as it treats a BYE from
 * that SSRC.   See the documentation for bye() (below)  for more
 * information. <p>
 *
 * For obvious reasons, there is no 'reason' string to hand back to
 * the  listener. <P>
 * The timeout event is sent in case of RTP/RTCP packets not being
 * received from active senders as well as passive receivers. In case
 * of passive receivers, the ReceiveStream parameter is null.
 * Currently, the timeout is set to 30 mins
 */
public class TimeoutEvent extends ReceiveStreamEvent{
    /**
     * True if the participant is leaving the
     * session;  after this call returns
     * the given participant object is invalid.  False if the timed-out
     * SSRC  is not the only SSRC
     * owned by the participant so the participant is remaining in the
     * session.    <P>
     */
    private boolean participantBye;

    public TimeoutEvent(SessionManager from,
			Participant participant,
			ReceiveStream recvStream,
			boolean participantBye){
	super(from, recvStream, participant);
	this.participantBye = participantBye;
    }
    /**
     * True if the participant is leaving the
     * session;  after this call returns
     * the given participant object is invalid.  False if the timed-out
     * SSRC  is not the only SSRC
     * owned by the participant so the participant is remaining in the
     * session.    <P>
     */
    public boolean participantLeaving(){
	return participantBye;
    }
}
	
