/*
 * @(#)AuParser.java	1.23 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.parser.audio;

import java.io.IOException;
import javax.media.Time;
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

public class AuParser extends BasicPullParser {
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
    private int bytesPerSecond;
    private int blockSize;
    private long minLocation;
    private long maxLocation;
    private PullSourceStream stream = null;

    public final static int AU_SUN_MAGIC =     0x2e736e64;
    public final static int AU_SUN_INV_MAGIC = 0x646e732e;
    public final static int AU_DEC_MAGIC =	    0x2e736400;
    public final static int AU_DEC_INV_MAGIC = 0x0064732e;

    public final static int AU_ULAW_8 = 1; /* 8-bit ISDN u-law */
    public final static int AU_LINEAR_8 = 2; /* 8-bit linear PCM */
    public final static int AU_LINEAR_16 = 3; /* 16-bit linear PCM */
    public final static int AU_LINEAR_24 = 4; /* 24-bit linear PCM */
    public final static int AU_LINEAR_32 = 5; /* 32-bit linear PCM */
    public final static int AU_FLOAT = 6; /* 32-bit IEEE floating point */
    public final static int AU_DOUBLE = 7; /* 64-bit IEEE floating point */
    public final static int AU_ADPCM_G721 = 23; /* 4-bit CCITT g.721 ADPCM */
    public final static int AU_ADPCM_G722 = 24; /* CCITT g.722 ADPCM */
    public final static int AU_ADPCM_G723_3 = 25; /* CCITT g.723 3-bit ADPCM */
    public final static int AU_ADPCM_G723_5 = 26; /* CCITT g.723 5-bit ADPCM */
    public final static int AU_ALAW_8 = 27; /* 8-bit ISDN A-law */

    private static ContentDescriptor[] supportedFormat = new ContentDescriptor[] {new ContentDescriptor("audio.basic")};

    public ContentDescriptor [] getSupportedInputContentDescriptors() {
	return supportedFormat;
    }

    public Track[] getTracks() throws IOException, BadHeaderException {

	if (tracks[0] != null)
	    return tracks;
	
	stream = (PullSourceStream) streams[0];
	if (cacheStream != null) {
	    // Disable jitter buffer during parsing of the header
	    cacheStream.setEnabledBuffering(false);
	}
	readHeader();
	if (cacheStream != null) {
	    cacheStream.setEnabledBuffering(true);
	}

	minLocation = getLocation(stream);
	if (dataSize == -1) { // Unknown
	    maxLocation = Long.MAX_VALUE; // ??
	} else {
	    maxLocation = minLocation + dataSize;
	}
	// System.out.println("Location after readHeader() is " + minLocation);
	// System.out.println("minLocation is " + minLocation);
	// System.out.println("maxLocation is " + maxLocation);

	tracks[0] = new AuTrack((AudioFormat) format,
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

	boolean bigEndian;

	int magic = readInt(stream, /* bigEndian = */ true);
	// System.out.println("Magic is " + Integer.toHexString(magic));
        if ( magic == AU_SUN_MAGIC || magic == AU_DEC_MAGIC ) {
	   bigEndian = true;
        } else if ( magic == AU_SUN_INV_MAGIC || magic == AU_DEC_INV_MAGIC ) {
	   bigEndian = false;
        } else {
            throw new BadHeaderException("Invalid magic number " +
					 Integer.toHexString(magic));
        }


	int headerSize = readInt(stream);

	if (headerSize < 24) {
	    throw new BadHeaderException("AU Parser: header size should be atleast 24 but is "
					 + headerSize);
	}

	dataSize = readInt(stream);

	if (dataSize == -1) {
	    // Unknown DataSize
	    // System.out.println("Unknown datasize");
	    long contentLength = stream.getContentLength();
	    if ( contentLength != SourceStream.LENGTH_UNKNOWN ) {
		dataSize = (int) (contentLength - headerSize);
		if (dataSize < 0) {
		    dataSize = -1;
		}
	    }
	}

	int encoding = readInt(stream);

	int sampleSizeInBits;
	blockSize = -1;
	switch (encoding) {
	    case AU_ULAW_8:
		encodingString = AudioFormat.ULAW;
		sampleSizeInBits = 8;
		blockSize = 1;
		break;
	    case AU_ALAW_8:
		encodingString = AudioFormat.ALAW;
		sampleSizeInBits = 8;
		blockSize = 1;
		break;
	    case AU_LINEAR_8:
		encodingString = AudioFormat.LINEAR;
		sampleSizeInBits = 8;
		blockSize = 1;
		break;
	    case AU_LINEAR_16:
		encodingString = AudioFormat.LINEAR;
		sampleSizeInBits = 16;
		blockSize = 2;
		break;
	    case AU_LINEAR_24:
		encodingString = AudioFormat.LINEAR;
		sampleSizeInBits = 24;
		blockSize = 3;
		break;   
	    case AU_LINEAR_32:
		encodingString = AudioFormat.LINEAR;
		sampleSizeInBits = 32;
		blockSize = 4;
		break;   
	    case AU_FLOAT:
		encodingString = "float"; // AudioFormat.JAUDIO_FLOAT;
		sampleSizeInBits = 32;
		blockSize = 4;
		break;
	    case AU_DOUBLE:
		encodingString = "double"; // AudioFormat.JAUDIO_DOUBLE;
		sampleSizeInBits = 64;
		blockSize = 8;
		break;
	    case AU_ADPCM_G721:
		encodingString = "??? what adpcm"; // AudioFormat.JAUDIO_G721_ADPCM;
		sampleSizeInBits = 4;
		break;
	    case AU_ADPCM_G723_3:
		encodingString = "G723_3"; // AudioFormat.JAUDIO_G723_3;
		sampleSizeInBits = 3;
		break;   
	    case AU_ADPCM_G723_5:
		encodingString = "G723_5"; // AudioFormat.JAUDIO_G723_5;
		sampleSizeInBits = 5;
		break;   
	    default: 
		throw new BadHeaderException("Unsupported encoding: " +
					     Integer.toHexString(encoding));
      	}


	int sampleRate = readInt(stream);

	if  ( sampleRate < 0 )
	    throw new BadHeaderException("Negative Sample Rate " + sampleRate);

	int channels = readInt(stream);

	if  ( channels < 1 )
	    throw new BadHeaderException("Number of channels is " + channels);

	if (blockSize != -1)
	    blockSize *= channels;

	// System.out.println("blockSize is " + blockSize);
	// System.out.println("dataSize is " + dataSize);
	// System.out.println("dataSize is " + Integer.toHexString(dataSize));
	// System.out.println("sampleRate is " + sampleRate);

	skip(stream, headerSize - (6*4));

        // bytesPerSecond cannot be negative because of the above checks.
	bytesPerSecond = channels * sampleSizeInBits * sampleRate / 8;

	int frameSizeInBytes = channels * sampleSizeInBits / 8;
	bufferSize = bytesPerSecond;
	// System.out.println("bytesPerSecond is " + bytesPerSecond);
	// System.out.println("frameSizeInBytes is " + frameSizeInBytes);
	// System.out.println("bufferSize is " + bufferSize);
	if (dataSize != -1) {
	    double durationSeconds = (double) dataSize / bytesPerSecond;
	    duration = new Time(durationSeconds);
	}
	// System.out.println("duration is " + duration.getSeconds());


	boolean signed = true;
	format = new AudioFormat(encodingString,
				 sampleRate,
				 sampleSizeInBits,
				 channels,
				 bigEndian ? AudioFormat.BIG_ENDIAN : AudioFormat.LITTLE_ENDIAN,
				 signed ? AudioFormat.SIGNED : AudioFormat.UNSIGNED,
				 frameSizeInBytes * 8,
				 Format.NOT_SPECIFIED, // No FRAME_RATE specified
				 Format.byteArray);
	// System.out.println("Audio format is " + format);
    }


    // TODO: Should reset sequence number after a setPosition
    // TODO: Optimize
    public Time setPosition(Time where, int rounding) {
	if (! seekable ) {
	    return getMediaTime();
	}
	if (blockSize < 0) {
	    // System.out.println("ERROR: setPosition not implemented for this encoding "
	    //	       + encodingString);
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
	return duration;
    }

    /**
     * Returns a descriptive name for the plug-in.
     * This is a user readable string.
     */
    public String getName() {
	return "Parser for AU file format";
    }
    
    class AuTrack extends BasicTrack {
	private double sampleRate;
	private float timePerFrame;
	private SettableTime frameToTime = new SettableTime();

	AuTrack(AudioFormat format, boolean enabled, Time startTime,
	       int numBuffers, int bufferSize,
	       long minLocation, long maxLocation) {
	    super(AuParser.this, 
		  format, enabled, AuParser.this.duration,
		  startTime, numBuffers, bufferSize,
		  AuParser.this.stream, minLocation, maxLocation);

	    double sampleRate = format.getSampleRate();
	    int channels = format.getChannels();
	    int sampleSizeInBits = format.getSampleSizeInBits();

	    float bytesPerSecond;
	    float bytesPerFrame;
	    float samplesPerFrame;

	    long durationNano = this.duration.getNanoseconds();
	}

	AuTrack(AudioFormat format, boolean enabled, Time startTime,
		 int numBuffers, int bufferSize) {
	    this(format, enabled,
		  startTime, numBuffers, bufferSize,
		  0L, Long.MAX_VALUE);

	}

    }
}
