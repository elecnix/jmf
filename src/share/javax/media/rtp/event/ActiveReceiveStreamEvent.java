/*
 * @(#)ActiveReceiveStreamEvent.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the SessionListener that data packets/RTCP packets have
 * commenced arriving after having stopped arriving. In case of an
 * active sender, this pertains to the state of packets on the
 * ReceiveStream. In case of passive receivers, the ReceiveStream will
 * be null.
 *
 */
public class ActiveReceiveStreamEvent extends ReceiveStreamEvent{
    public ActiveReceiveStreamEvent(SessionManager from,
				 Participant participant,
				 ReceiveStream recvStream){
	
	super(from, recvStream, participant);
    }
}
