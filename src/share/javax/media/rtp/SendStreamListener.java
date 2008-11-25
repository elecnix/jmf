/*
 * @(#)SendStreamListener.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import javax.media.rtp.event.*;
import java.util.EventListener;

/**
 * Interface that generates the callback for  RTPSessionManager
 * Events. This interface will generate callbacks for events that
 * pertain to a particular SendStream. 
 */
public interface SendStreamListener extends EventListener{
    /** Method called back in the RTPSessionListener to notify
     * listener of all SendStream Events.
     */
    public void update( SendStreamEvent event);
}
    
    
