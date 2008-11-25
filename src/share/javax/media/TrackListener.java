/*
 * @(#)TrackListener.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package javax.media;

public interface TrackListener {
 
    /**
     * The readFrame call has blocked.
     * This info can be used to
     * stop the clock, and send a RestartingEvent
     */

    void readHasBlocked(Track t);

}
