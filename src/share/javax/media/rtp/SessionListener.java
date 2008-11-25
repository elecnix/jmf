/*
 * @(#)SessionListener.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import javax.media.rtp.event.*;
import java.util.EventListener;

/**
 * Interface that generates the callback for all SessionEvents.
 * These events are LocalCollisionEvent that pertain to the local
 * participant and NewParticipantEvent that will inform the listener of
 * every new/unique participant that joins the session. For all other
 * state transitions event of the participant
 * i.e. Active/Inactive/Timeout/ByeEvent etc. see ReceiveStreamListener  
 */
public interface SessionListener extends EventListener{
    /** Method called back in the SessionListener to notify
     * listener of all Session Events.SessionEvents could be one
     * of NewParticipantEvent or LocalCollisionEvent.  
     */
    public void update( SessionEvent event);
}
    
    
    
    
    
    






