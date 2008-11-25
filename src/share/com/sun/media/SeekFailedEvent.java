/*
 * @(#)SeekFailedEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import javax.media.*;

/**
 * A <code>SeekFailedEvent</code> indicates that the <code>Controller</code> could not
 * start at the current media time (set using setMediaTime).
*/

public class SeekFailedEvent extends StopEvent {

    public SeekFailedEvent(Controller from,
			   int previous, int current, int target,
			   Time mediaTime) {
	super(from, previous, current, target, mediaTime);
    }
}
