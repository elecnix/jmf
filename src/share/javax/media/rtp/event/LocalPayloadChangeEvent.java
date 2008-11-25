/*
 * @(#)LocalPayloadChangeEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTP listener that a local RTP sender has changed the
 * payload  type of a data stream.
 */
public class LocalPayloadChangeEvent extends SendStreamEvent{
    /**
     * The old payload type.
     */
    private int oldpayload;
    /**
     * The new payload type
     */
    private int newpayload;
    public LocalPayloadChangeEvent (SessionManager from,
				   SendStream sendStream,
				   int oldpayload,
				   int newpayload){
	super(from, sendStream, null);
	this.oldpayload = oldpayload;
	this.newpayload = newpayload;
    }
    /**
     * The new payload type
     */
    public int getNewPayload(){
	return newpayload;
    }
}
