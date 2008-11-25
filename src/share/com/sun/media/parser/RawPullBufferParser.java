/*
 * @(#)RawPullBufferParser.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.parser;

import java.io.IOException;
import javax.media.*;
import javax.media.Buffer;
import javax.media.protocol.*;
import javax.media.format.*;
import com.sun.media.*;


public class RawPullBufferParser extends RawPullStreamParser {

    static final String NAME = "Raw pull stream parser";

    public String getName() {
	return NAME;
    }

    public void setSource(DataSource source)
	throws IOException, IncompatibleSourceException {

	if (!(source instanceof PullBufferDataSource)) {
	    throw new IncompatibleSourceException("DataSource not supported: " + source);
	} else {
	    streams = ((PullBufferDataSource) source).getStreams();
	}


	if ( streams == null) {
	    throw new IOException("Got a null stream from the DataSource");
	}

	if (streams.length == 0) {
	    throw new IOException("Got a empty stream array from the DataSource");
	}

	if (!supports(streams))
	    throw new IncompatibleSourceException("DataSource not supported: " + source);

	this.source = source;
	this.streams = streams;
	
	// System.out.println("content length is " + streams[0].getContentLength());
    }

    /**
     * Override this if the Parser has additional requirements
     * from the PullBufferStream
     */
    protected boolean supports(SourceStream[] streams) {
	return ( (streams[0] != null) &&
		 (streams[0] instanceof PullBufferStream) );
    }

    /**
     * Opens the plug-in software or hardware component and acquires
     * necessary resources. If all the needed resources could not be
     * acquired, it throws a ResourceUnavailableException. Data should not
     * be passed into the plug-in without first calling this method.
     */
    public void open() {
	if (tracks != null)
	    return;
	tracks = new Track[streams.length];
	for (int i = 0; i < streams.length; i++) {
	    tracks[i] = new FrameTrack(this, (PullBufferStream)streams[i]);
	}
    }


    ////////////////////////
    //
    // Inner class
    ////////////////////////

    class FrameTrack implements Track {
	Demultiplexer parser;
	PullBufferStream pbs;
	boolean enabled = true;
	Format format = null;
	TrackListener listener;
	Integer stateReq = new Integer(0);
	
	public FrameTrack(Demultiplexer parser, PullBufferStream pbs) {
	    this.pbs = pbs;
	    format = pbs.getFormat();
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
	    return parser.getDuration(); 
	}

	public Time getStartTime() {
	    return new Time(0);
	}

	public void setTrackListener(TrackListener l) {
	    listener = l;
	}

	public void readFrame(Buffer buffer) {

	    // If the buffer is empty, just allocate some random number.
	    if (buffer.getData() == null)
		buffer.setData(new byte[500]);

	    try {
		pbs.read(buffer);
	    } catch (IOException e) {
		buffer.setDiscard(true);
	    }
	}

	public int mapTimeToFrame(Time t) {
	    return -1;
	}

	public Time mapFrameToTime(int frameNumber) {
	    return new Time(0);
	}

    }
}

