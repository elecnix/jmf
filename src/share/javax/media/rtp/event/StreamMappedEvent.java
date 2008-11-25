/*
 * @(#)StreamMappedEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTP listener that a previously 'orphaned'
 * ReceiveStream has been associated with an Participant.The
 * ReceiveStream  is now available through the Participant
 * object's getStreams() method; similarly, the Participant is available
 * through the  ReceiveStream object's getParticipant() method. <P>
 *
 * This callback occurs after a newReceiveStream() callback, if and
 * when an RTCP  packet arrives which matches the SSRC of the
 * orphaned  ReceiveStream.The Participant to  which this ReceiveStream
 * is being mapped may be new or may already existed; in the former
 * case,  the newParticipant() callback would have immediately
 * preceded this one.  <P> 
 *
 */
public class StreamMappedEvent extends ReceiveStreamEvent{
    public StreamMappedEvent(SessionManager from,
				 ReceiveStream recvStream,
				 Participant participant){
	super(from, recvStream, participant);
    }
}
	  
			      
