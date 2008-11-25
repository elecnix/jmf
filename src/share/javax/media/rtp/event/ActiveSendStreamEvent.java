/*
 * @(#)ActiveSendStreamEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTPSendStreamListener that data packets/RTCP packets have
 * started arriving after having stopped arriving. 
 *
 */
public class ActiveSendStreamEvent extends SendStreamEvent{
    
    public ActiveSendStreamEvent(SessionManager from,
				 Participant participant,
				 SendStream sendStream){
	
	super(from, sendStream, participant);
    }
}
