/*
 * @(#)ReceiveStreamListener.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import javax.media.rtp.event.*;
import java.util.EventListener;

/**
 * Interface that generates the callback for all RTPSessionManager
 * Events. This interface will generate callbacks for events that
 * pertain to a particular RTPRecvStream. This interface will also
 * generate callbacks that pertain to state transitions of
 * active/inactive of a passive participant as well.i.e. Active,
 * Inactive, Timeout,ByeEvent will also be generated for passive
 * participants and RTPRecvStream will be null in that case. 
 */
public interface ReceiveStreamListener extends EventListener{
    /** Method called back in the RTPSessionListener to notify
     * listener of all ReceiveStream Events.
     */
    public void update( ReceiveStreamEvent event);
}
    
    
    
