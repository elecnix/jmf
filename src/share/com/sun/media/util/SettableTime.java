/*
 * @(#)SettableTime.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import javax.media.Time;

public class SettableTime extends Time {
    
    public SettableTime() {
	super(0L);
    }

    public SettableTime(long nanoseconds) {
	super(nanoseconds);
    }

    public SettableTime(double seconds) {
	super(seconds);
    }

    public final Time set(long nanoseconds) {
	this.nanoseconds = nanoseconds;
	return this;
    }

    public final Time set(double seconds) {
	nanoseconds = secondsToNanoseconds(seconds);
	return this;
    }

}
