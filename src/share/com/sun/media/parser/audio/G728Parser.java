/*
 * @(#)G728Parser.java	1.9	02/08/21
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.parser.audio;

import java.io.IOException;
import javax.media.Time;
import javax.media.*;
import javax.media.Duration;
import javax.media.Track;
import javax.media.BadHeaderException;
import javax.media.protocol.SourceStream;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.Positionable;
import javax.media.Format;
import javax.media.protocol.ContentDescriptor;
import javax.media.format.AudioFormat;
import com.sun.media.parser.BasicPullParser;
import com.sun.media.parser.BasicTrack;
import com.sun.media.util.SettableTime;

public class G728Parser extends BasicPullParser {
    private static final int CODEFRAMESIZE = 5;	// code frame size in bytes
    private static final int DATAFRAMESIZE = 40;// data frame size in bytes
    private static final int FRAMERATE = 400;	// frames per second
    private Time duration = Duration.DURATION_UNKNOWN;
    private Format format = null;
    private Track[] tracks = new Track[1]; // Only 1 track is there for wave
    private int numBuffers = 4; // TODO: check
    private int bufferSize;
    private int dataSize;
    private SettableTime mediaTime = new SettableTime(0L);
    private int encoding;
    private String encodingString;
    private int sampleRate;
    private int samplesPerBlock;
    private int bytesPerSecond = CODEFRAMESIZE * FRAMERATE;
    private int blockSize = CODEFRAMESIZE;
    private long minLocation;
    private long maxLocation;
    private PullSourceStream stream = null;


    private static ContentDescriptor[] supportedFormat = new ContentDescriptor[] {new ContentDescriptor("audio.g728")};

    public ContentDescriptor [] getSupportedInputContentDescriptors() {
	return supportedFormat;
    }

    public Track[] getTracks() throws IOException, BadHeaderException {

	if (tracks[0] != null)
	    return tracks;
	
	stream = (PullSourceStream) streams[0];
	// Since the readHeader doesn't read anything there
	// is no need to disable buffering
	readHeader();

	// get data a frame at a time
	bufferSize = CODEFRAMESIZE;
	tracks[0] = new G728Track((AudioFormat) format,
				/*enabled=*/ true,
				new Time(0),
				numBuffers,
				bufferSize,
				minLocation,
				maxLocation
				);
	return tracks;
    }

    private void /* for now void */ readHeader()
	throws IOException, BadHeaderException {

	minLocation = getLocation(stream); // Should be zero

	long contentLength = stream.getContentLength();
	if ( contentLength != SourceStream.LENGTH_UNKNOWN ) {
	    double durationSeconds = contentLength / bytesPerSecond;
	    duration = new Time(durationSeconds);
	    maxLocation = contentLength;
	} else {
	    maxLocation = Long.MAX_VALUE;
	}

	format = new AudioFormat(AudioFormat.G728,
				 8000,
				 16,
				 1,
				 AudioFormat.BIG_ENDIAN,
				 AudioFormat.SIGNED,
				 (CODEFRAMESIZE * 8),
				 FRAMERATE,
				 Format.byteArray);
    }


    // TODO: Should reset sequence number after a setPosition
    // TODO: Optimize
    public Time setPosition(Time where, int rounding) {
	if (! seekable ) {
	    return getMediaTime();
	}

	long time = where.getNanoseconds();
	long newPos;

	if (time < 0)
	    time = 0;

	double newPosd = time * bytesPerSecond / 1000000000.0;
	double remainder = (newPosd % blockSize);
	
	newPos = (long) (newPosd - remainder);

	if (remainder > 0) {
	    switch (rounding) {
	    case Positionable.RoundUp:
		newPos += blockSize;
		break;
	    case Positionable.RoundNearest:
		if (remainder > (blockSize / 2.0))
		    newPos += blockSize;
		break;
	    }
	}

// 	if ( newPos > maxLocation )
// 	    newPos = maxLocation;
	
	newPos += minLocation;
	((BasicTrack) tracks[0]).setSeekLocation(newPos);
	if (cacheStream != null) {
	    synchronized(this) {
		// cacheStream.setPosition(where.getNanoseconds());
		cacheStream.abortRead();
	    }
	}
	return where; // TODO: return the actual time value
    }

    public Time getMediaTime() {
	return null;
    }

    public Time getDuration() {
	if ( duration.equals(Duration.DURATION_UNKNOWN) &&
	     ( tracks[0] != null ) ) {
	    long mediaSizeAtEOM = ((BasicTrack) tracks[0]).getMediaSizeAtEOM();
	    if (mediaSizeAtEOM > 0) {
		double durationSeconds = mediaSizeAtEOM / bytesPerSecond;
		duration = new Time(durationSeconds);
	    }
	}
	return duration;
    }

    /**
     * Returns a descriptive name for the plug-in.
     * This is a user readable string.
     */
    public String getName() {
	return "G.728 Parser";
    }

    class G728Track extends BasicTrack {
	private double sampleRate;
	private float timePerFrame;
	private SettableTime frameToTime = new SettableTime();

	G728Track(AudioFormat format, boolean enabled, Time startTime,
	       int numBuffers, int bufferSize,
	       long minLocation, long maxLocation) {
	    super(G728Parser.this, 
		  format, enabled, G728Parser.this.duration,
		  startTime, numBuffers, bufferSize,
		  G728Parser.this.stream, minLocation, maxLocation);

	    double sampleRate = format.getSampleRate();
	    int channels = format.getChannels();
	    int sampleSizeInBits = format.getSampleSizeInBits();

	    float bytesPerSecond;
	    float bytesPerFrame;
	    float samplesPerFrame;

	    long durationNano = this.duration.getNanoseconds();
	}

	G728Track(AudioFormat format, boolean enabled, Time startTime,
		 int numBuffers, int bufferSize) {
	    this(format, enabled,
		  startTime, numBuffers, bufferSize,
		  0L, Long.MAX_VALUE);

	}
    }

}
