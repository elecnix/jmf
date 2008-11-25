/*
 * @(#)RemoteCollisionEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTP listener that two remote participants were using
 * the same  SSRC simultaneously. Since participants are obligated to detect
 * collisions, the  remote parties should eventually start sending
 * data or  control packets with new SSRCs.   Thus, this callback will
 * usually  precede two newReceiveStream() callbacks; these new
 * ReceiveStreams   will (eventually) be associated with the existing
 * Participants. The old  ReceiveStreams  associated with those
 * participants will be discarded and no longer returned by their
 * respective  getStreams() methods.
 */
public class RemoteCollisionEvent extends RemoteEvent{
    /**
     *  collidingSSRC The remote SSRC
     */
    private  long collidingSSRC;

    public RemoteCollisionEvent(SessionManager from,
				long ssrc){
	super(from);
	collidingSSRC = ssrc;
    }
    /**
     *  collidingSSRC The remote SSRC
     */
    public long getSSRC(){
	return collidingSSRC;
    }
}
	
