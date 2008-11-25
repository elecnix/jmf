/*
 * @(#)InactiveReceiveStreamEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the SessionListener that data & control packets have stopped
 * arriving on this ReceiveStream. The time for which the RTPSM waits
 * for data & control packets before notifying the listeners of this event is
 * approximately equal to the RTCP report interval as specified in the
 * RTP draft. 
 * InactiveRecvStreamEvent will also be generated to say the passive
 * receivers that have timed out. The timeout used for this is the 5 times
 * the report interval as per the draft and is measured for RTCP
 * packets for RTCP packets from this passive receiver. In the case of
 * a passive receiver, the ReceiveStream will be null.
 * If data or control is resumed from this member, an
 * ActiveRecvStreamEvent will be generated. 
 * In case of the RTP draft, a member is to be timed out if an RTP/RTCP
 * packet is not received in the timeout time. In our implementation, the
 * InactiveRecvStreamEvent is sent, but the member is not removed from
 * our cache until a much larger timeout (set to 30 min).At this time,
 * a TimeOutEvent is sent to the ReceiveStreamListener.This is done in
 * order to take care of inactivity due to network partitions. 
 */
public class InactiveReceiveStreamEvent extends ReceiveStreamEvent{
    /**
     * Set to true if this is the last or only RTPStream owned by this
     * participant
     */
    private boolean laststream;
    public InactiveReceiveStreamEvent(SessionManager from,
				   Participant participant,
				   ReceiveStream recvStream,
				   boolean laststream){
	super(from, recvStream, participant);
	this.laststream = laststream;
	
    }
    /**
     * Set to true if this is the last or only RTPStream owned by this
     * participant.
     */
    public boolean isLastStream(){
	return laststream;
    }
}
