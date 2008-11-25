/*
 * @(#)JmpxAudio.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.jmpx;

import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.renderer.audio.device.JavaSoundOutput;

public class JmpxAudio extends JavaSoundOutput {

    private long count = 0;

    // All for the Hack of JavaSound!
    private long JSHackIncr = 0;
    private long JSHackCount = 0;
    private long JSHackSample = 0;
    private long JSHackSampleCount = 0;
    private Object pauseSync = new Object();
    static final long MAX_TOLERENCE = 50L;
    static final long OFFSET = 0;
    private int sampleCountLow;
    
    public JmpxAudio() {
    }

    public boolean initialize(AudioFormat format) {
	JSHackIncr = (long)(format.getSampleRate() / 60);
	return super.initialize(format, bufferSize(format));
    }

    long lastPos = 0;
    long originPos = 0;
    long totalCount = 0;
    
    /*
     * We are doing this since the JNI doesn't seem to like getSamplesPlayed().
     * which returns a long instead of an int for some reason...
     */
    public int getSampleCountHigh() {
	long pos = dataLine.getFramePosition();
	// Check for wrap around
	if (pos < lastPos) {
	    totalCount += lastPos - originPos;
	    originPos = pos;
	}
	
	lastPos = pos;
	
	long s = totalCount + pos - originPos - count + JSHackSampleCount;
	if (s == JSHackSample) {
	    if (JSHackCount++ > MAX_TOLERENCE) {
		JSHackSampleCount += JSHackIncr;
		JSHackCount = 0;
	    }
	} else {
	    JSHackCount = 0;
	    JSHackSample = s;
	}
	sampleCountLow = (int) (s & 0xFFFFFFFFL);
	return (int) ((s >> 32) & 0xFFFFFFFFL);
    }

    public int getSampleCountLow() {
	return sampleCountLow;
    }

    public void pause() {
	synchronized (pauseSync) {
	    super.pause();
	}
    }

    public void resume() {
	synchronized (pauseSync) {
	    super.resume();
	}
    }

    public int write(byte data[], int off, int len) {
	synchronized (pauseSync) {
	    if (paused) {
System.err.println("writing when paused: " + len);
		return len;
	    }
	    return super.write(data, off, len);
	}
    }

    public void flush() {
 	super.flush();
    }

    public void resetSamples() {
	if (dataLine != null) {
	    count = dataLine.getFramePosition();
	    lastPos = count;
	    originPos = count;
	    totalCount = count;
	}
	JSHackSampleCount = 0;
    }

    static private int bufferSize(AudioFormat f) {
	int bufferSize = 0;
	bufferSize = 256*1024;
	return ++bufferSize; // make it an odd number
    }

}


