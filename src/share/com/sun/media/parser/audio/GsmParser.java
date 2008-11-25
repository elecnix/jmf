/*
 * @(#)GsmParser.java	1.14 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
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

public class GsmParser extends BasicPullParser {
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
    private int bytesPerSecond = 1650; // 33 * 50
    private int blockSize = 33;
    private long minLocation;
    private long maxLocation;
    private PullSourceStream stream = null;


    private static ContentDescriptor[] supportedFormat = new ContentDescriptor[] {new ContentDescriptor("audio.x_gsm")};

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
	bufferSize = bytesPerSecond;
	tracks[0] = new GsmTrack((AudioFormat) format,
				/*enabled=*/ true,
				new Time(0),
				numBuffers,
				bufferSize,
				minLocation,
				maxLocation
				);
	return tracks;
    }

    /**
     * GSM
     * 8000 samples per sec.
     * 160 samples represent 20 milliseconds and GSM represents them
     * in 33 bytes. So frameSize is 33 bytes and there are 50 frames
     * in one second. One second is 1650 bytes.
     */
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

	boolean signed = true;
	boolean bigEndian = false;
	format = new AudioFormat(AudioFormat.GSM,
				 8000,  // sampleRate,
				 16,    // sampleSizeInBits,
				 1,     // channels,
				 bigEndian ? AudioFormat.BIG_ENDIAN : AudioFormat.LITTLE_ENDIAN,
				 signed ? AudioFormat.SIGNED : AudioFormat.UNSIGNED,
				 (blockSize * 8), // frameSizeInBits
				 Format.NOT_SPECIFIED, // No FRAME_RATE specified
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

    // Can be moved to base class. au parser also uses same code.
    public Time getMediaTime() {
	long location;
	long seekLocation = ((BasicTrack) tracks[0]).getSeekLocation();
	if (seekLocation != -1)
	    location = seekLocation - minLocation;
	else
	    location = getLocation(stream) - minLocation;
	synchronized(mediaTime) {
	    mediaTime.set( location / (double) bytesPerSecond );
	}
	return mediaTime;
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
	return "Parser for raw GSM";
    }

    class GsmTrack extends BasicTrack {
	private double sampleRate;
	private float timePerFrame = 0.020F; // 20 milliseconds
	private SettableTime frameToTime = new SettableTime();

	GsmTrack(AudioFormat format, boolean enabled, Time startTime,
	       int numBuffers, int bufferSize,
	       long minLocation, long maxLocation) {
	    super(GsmParser.this, 
		  format, enabled, GsmParser.this.duration,
		  startTime, numBuffers, bufferSize,
		  GsmParser.this.stream, minLocation, maxLocation);

	    double sampleRate = format.getSampleRate();
	    int channels = format.getChannels();
	    int sampleSizeInBits = format.getSampleSizeInBits();

	    float bytesPerSecond;
	    float bytesPerFrame;
	    float samplesPerFrame;

	    long durationNano = this.duration.getNanoseconds();
	}

	GsmTrack(AudioFormat format, boolean enabled, Time startTime,
		 int numBuffers, int bufferSize) {
	    this(format, enabled,
		  startTime, numBuffers, bufferSize,
		  0L, Long.MAX_VALUE);

	}
    }

}
