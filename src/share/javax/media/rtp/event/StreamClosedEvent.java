/*
 * @(#)StreamClosedEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Informs the RTP listener that a transmitting stream has been
 * closed in the RTP SessionManager
 */
public class StreamClosedEvent extends SendStreamEvent{
    public StreamClosedEvent(SessionManager from,
			    SendStream sendStream){
      super(from, sendStream, sendStream.getParticipant());
    }
}
