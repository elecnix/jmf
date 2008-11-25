/*
 * @(#)NewReceiveStreamEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTP listener that a new stream of RTP data packets
 * has been  detected; in RTP parlance, this means that RTP data
 * packets have been received from an SSRC  that had not previously
 * been sending data.  <P>
 *
 * New ReceiveStreams announced with this callback may be created in
 * one of  two states. If the data packets have an SSRC that the
 * RTPSM has already seen in RTCP control  packets on this session,
 * then the  new ReceiveStream is created in the "unorphaned"
 * state; in other  words, it is permanently associated with an
 * Participant. This Participant,  whose presence would have
 * been  announced previously with the newParticipant() callback, is
 * obtainable  from the getParticipant() method in the
 * ReceiveStream.<P>
 * 
 * If, however, the SSRC in the data packets has never before been
 * seen, the  new ReceiveStream is created in the "orphaned" state
 * and  (as of yet) has no associated Participant; a call to
 * getParticipant on the ReceiveStream will return null.  When and
 * if an RTCP  packet arrives with the same SSRC, the CNAME
 * contained therein  will be extracted and checked  against the CNAMEs of
 * existing participants.  If there is a match, than we have a case
 * where a  participant is sending a data stream with an SSRC it was
 * not  previously using; this should only  happen when a
 * participant is  sending multiple streams. In this situation a
 * recvStreamMapped() callback will be invoked by the RTPSM.  If the
 * CNAME  was unrecognized then this is a  case of a brand new
 * participant,  so in addition to recvStreamMapped(), RTPSM will
 * also invoke  the newParticipant() callback *before* calling
 * recvStreamMapped(). <P>
 *
 */
public class NewReceiveStreamEvent extends ReceiveStreamEvent{
    public NewReceiveStreamEvent(SessionManager from,
			      ReceiveStream recvStream){
	super(from, recvStream, recvStream.getParticipant());
    }
}
