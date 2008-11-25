/*
 * @(#)LocalCollisionEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTP client that the SSRC it was using collided  with
 * another SSRC in the session.  A new 
 * SSRC is  provided for the client's use; however,  the client can
 * override this value by returning a different one.  <P>
 * 
 */
public class LocalCollisionEvent extends SessionEvent{
    /**
      The ReceiveStream object with the colliding SSRC
     */
    private ReceiveStream recvStream;
    /**
     * The new SSRC that the SessionManager will use
     * for  sending stream, unless
     * the client returns a different value
     */
    private long newSSRC;

    public LocalCollisionEvent(SessionManager from,
			       ReceiveStream  recvStream,
			       long newSSRC){
	super(from);
	this.recvStream = recvStream;
	this.newSSRC = newSSRC;
    }

    public ReceiveStream getReceiveStream(){
	return recvStream;
    }

    public long getNewSSRC(){
	return newSSRC;
    }
}
    
