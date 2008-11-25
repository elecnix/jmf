/*
 * @(#)BasicTrack.java	1.22 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

// TODO: make this an inner class of BasicPullParser if it makes sense. (ie if it works
// for wav and qt parsers)

package com.sun.media.parser;

import java.io.IOException;
import javax.media.*;
import javax.media.Format;
import javax.media.protocol.SourceStream;
import javax.media.protocol.PullSourceStream;

public class BasicTrack implements Track {

    private Format format;
    private boolean enabled = true;
    protected Time duration;
    private Time startTime;
    private int numBuffers;
    private int dataSize;
    private PullSourceStream stream;
    private long minLocation;
    private long maxLocation;
    private long maxStartLocation;
    private BasicPullParser parser; // TODO: Won't need this if inner class.
    private long sequenceNumber = 0;
    private TrackListener listener;
    private long seekLocation = -1L;
    private long mediaSizeAtEOM = -1L; // update when EOM implied by IOException occurs
    private boolean warnedUserOfReadPastEOM = false;

    public BasicTrack(BasicPullParser parser,
	       Format format, boolean enabled, Time duration, Time startTime,
	       int numBuffers, int dataSize, PullSourceStream stream) {
	this(parser, format,  enabled,  duration,  startTime,
	     numBuffers, dataSize, stream,
	     0L, Long.MAX_VALUE);
    }


    /**
     * Note to implementors who want to use this class.
     * If the maxLocation is not known, then
     * specify Long.MAX_VALUE for this parameter
     */
    public BasicTrack(BasicPullParser parser,
	       Format format, boolean enabled, Time duration, Time startTime,
	       int numBuffers, int dataSize, PullSourceStream stream,
	       long minLocation, long maxLocation) {
	this.parser = parser;

	this.format = format;
	this.enabled = enabled;
	this.duration = duration;
	this.startTime = startTime;
	this.numBuffers = numBuffers;
	this.dataSize = dataSize;
	this.stream = stream;
	this.minLocation = minLocation;
	this.maxLocation = maxLocation;
	maxStartLocation = maxLocation - dataSize;
    }

    public Format getFormat() {
	return format;
    }


    public void setEnabled(boolean t) {
	enabled = t;
    }

    public boolean isEnabled() {
	return enabled;
    }

    public Time getDuration() {
	return duration;
    }


    public Time getStartTime() {
	return startTime;
    }

    // TODO: create a list of TrackListeners
    public void setTrackListener(TrackListener l) {
	listener = l;
    }
    
    
    public synchronized void setSeekLocation(long location) {
	seekLocation = location;
    }

    public synchronized long getSeekLocation() {
	return seekLocation;
    }

    public void readFrame(Buffer buffer) {

	if (buffer == null)
	    return;

	if (!enabled) {
	    buffer.setDiscard(true);
	    return;
	}

	buffer.setFormat(format); // Need to do this every time ???
	Object obj = buffer.getData();
	byte[] data;
	long location;
	boolean needToSeek;
	
	synchronized(this) {
	    if (seekLocation != -1) {
		location = seekLocation;
		if (seekLocation < maxLocation)
		    seekLocation = -1;
		needToSeek = true;
	    } else {
		location = parser.getLocation(stream);
		needToSeek = false;
	    }
	}

	int needDataSize;

	//TODO START
	if (location < minLocation) {
	    // TODO: should probably seek or skip to minLocation
	    buffer.setDiscard(true);
	    return;
	} else if (location >= maxLocation) {
	    buffer.setLength(0);
	    buffer.setEOM(true);
	    return;
	} else if (location > maxStartLocation) {
	    needDataSize = dataSize - (int) (location - maxStartLocation);
	} else {
	    needDataSize = dataSize;
	}
	//TODO END

	if  ( (obj == null) ||
	      (! (obj instanceof byte[]) ) ||
	      ( ((byte[])obj).length < needDataSize) ) {
	    // System.out.println("readFrame creating new byte data of size " + needDataSize);
	    data = new byte[needDataSize];
	    buffer.setData(data);
	} else {
	    data = (byte[]) obj;
	}
	try {
	    if ( (parser.cacheStream != null) && (listener != null) ) {
		if ( parser.cacheStream.willReadBytesBlock(location, needDataSize) ) {
		    // System.out.println("read will block: " + location + " : " +
		    // needDataSize);
		    listener.readHasBlocked(this);
		}
	    }
	    if (needToSeek) {
		// TODO: need to handle case where the stream is not seekable and
		// caching is not beeing done (ie cacheStream is null)
		long pos = ((javax.media.protocol.Seekable)stream).seek(location);
		if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
		    buffer.setDiscard(true);
		    return;
		}
	    }
	    if (parser.getMediaTime() != null)
		buffer.setTimeStamp(parser.getMediaTime().getNanoseconds());
	    else
		buffer.setTimeStamp(Buffer.TIME_UNKNOWN);
	    buffer.setDuration(Buffer.TIME_UNKNOWN);

	    int actualBytesRead = parser.readBytes(stream, data, needDataSize);
	    buffer.setOffset(0);
	    buffer.setLength(actualBytesRead);
	    // TODO: need setSequenceNumber and getSequenceNumber in Buffer
	    buffer.setSequenceNumber(++sequenceNumber);
// 	    System.out.println("Time stamp: " + (buffer.getTimeStamp())/1.E9);
	} catch (IOException e) {
	    if (maxLocation != Long.MAX_VALUE) {
		// Known maxLocation. So, this is a case of
		// deliberately reading past EOM
		if (!warnedUserOfReadPastEOM) {
		    com.sun.media.Log.warning("Warning: Attempt to read past End of Media");
		    com.sun.media.Log.warning("This typically happens if the duration is not known or");
		    com.sun.media.Log.warning("if the media file has incorrect header info");
		    warnedUserOfReadPastEOM = true;
		}
		buffer.setLength(0); // Need this??
		buffer.setEOM(true);
	    } else {
		// Unknown maxLocation, due to unknown content length
		// EOM reached before the required bytes could be read.
		long length = parser.streams[0].getContentLength();
		if ( length != SourceStream.LENGTH_UNKNOWN ) {
		    // If content-length is known, discard this buffer, update
		    // maxLocation, maxStartLocation and mediaSizeAtEOM.
		    //  The next readFrame will read the remaining data till EOM.
		    maxLocation = length;
		    maxStartLocation = maxLocation - dataSize;
		    mediaSizeAtEOM = maxLocation - minLocation;
		    buffer.setLength(0); // Need this??
		    buffer.setDiscard(true);
		} else {
		    // Content Length is still unknown after an IOException.
		    // We can still discard this buffer and keep discarding
		    // until content length is known. But this may go into
		    // into an infinite loop, if there are real IO errors
		    // So, return EOM
		    maxLocation = parser.getLocation(stream);
		    maxStartLocation = maxLocation - dataSize;
		    mediaSizeAtEOM = maxLocation - minLocation;
		    buffer.setLength(0); // Need this??
		    buffer.setEOM(true);

		}
			   
	    }
	    // TODO: $$$$ Update maxFrame and duration
	}
	
	// System.out.println("parser's location: " + parser.getLocation(stream));
	// parser.getMediaTime(); // Side effect printout
    }

    public int mapTimeToFrame(Time t) {
	return FRAME_UNKNOWN;
    }

    public Time mapFrameToTime(int frameNumber) {
	return TIME_UNKNOWN;
    }


    public long getMediaSizeAtEOM() {
	return mediaSizeAtEOM; // updated when EOM implied by IOException occurs
    }
}

