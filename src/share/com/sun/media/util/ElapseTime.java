/*
 * @(#)ElapseTime.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import javax.media.*;
import javax.media.format.*;


/**
 * A utility class to keep track of the passage of time as data
 * buffers are being processed.
 */
public class ElapseTime {

    public long value = 0;

    public void setValue(long t) {
	value = t;
    }

    public long getValue() {
	return value;
    }

    public boolean update(int len, long ts, Format f) {
	if (f instanceof AudioFormat) {
	    long t;
	    if ((t = ((AudioFormat)f).computeDuration(len)) > 0)
	    	value += t;
	    else if (ts > 0)
		value = ts;
	    else
		return false;
	} else if (ts > 0)
	    value = ts;
	else
	    return false;

	return true;
    }


    /**
     * Convert audio: length (bytes) to duration (nanoseconds).
     */
    static public long audioLenToTime(long len, AudioFormat af) {
	return af.computeDuration(len);
    }


    /**
     * Convert audio: duration (nanoseconds) to length (bytes).
     */
    static public long audioTimeToLen(long duration, AudioFormat af) {

	long units, bytesPerSec;

	if (af.getSampleSizeInBits() > 0) {
	    units = (long)(af.getSampleSizeInBits() * af.getChannels());
	    bytesPerSec = (long)((units * af.getSampleRate()) / 8);
	} else if (af.getFrameSizeInBits() != Format.NOT_SPECIFIED &&
		   af.getFrameRate() != Format.NOT_SPECIFIED) {
	    units = af.getFrameSizeInBits();
	    bytesPerSec = (long)((units * af.getFrameRate()) / 8);
	} else {
	    units = bytesPerSec = 0;
	}

	// The length returned needs to be in multiples of audio sample 
	// chunk unit.
	return (bytesPerSec == 0 ? 0 : 
		(long)((duration * bytesPerSec) / 1000000000) / units * units);
    }
}

