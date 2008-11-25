/*
 * @(#)WavParser.java	1.30 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.parser.audio;

import java.io.IOException;
import javax.media.Time;
import javax.media.Duration;
import javax.media.Track;
import javax.media.BadHeaderException;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.Positionable;
import javax.media.Format;
import javax.media.protocol.ContentDescriptor;
import javax.media.format.AudioFormat;
import com.sun.media.format.WavAudioFormat;
import com.sun.media.parser.BasicPullParser;
import com.sun.media.parser.BasicTrack;
import com.sun.media.util.SettableTime;

/************************************************************
Only 'fmt ' and 'data' chunks supported.
Not computing header (length of the header) as it is misleading.
You can have for example 'fmt ' chunk 'fact' chunk and 'data'
chunk.

Changes:
WAVE_FORMAT_GSM (0x31) is now WAVE_FORMAT_GSM610 as there is also
an MS GSM
sampleSizeInBits field not applicable for GSM610.

For WAVE_FORMAT_ADPCM, WAVE_FORMAT_DVI_ADPCM and WAVE_FORMAT_GSM610
the extra fields are now read. The useful extra field here is
samples per block.

************************************************************/

public class WavParser extends BasicPullParser {
    private Time duration = Duration.DURATION_UNKNOWN;
    private WavAudioFormat format = null;
    private Track[] tracks = new Track[1]; // Only 1 track is there for wave
    private int numBuffers = 4; // TODO: check
    private int bufferSize;
    private int dataSize;
    private SettableTime mediaTime = new SettableTime(0L);
    private int encoding;
    private String encodingString;
    private int sampleRate;
    private int channels;
    private int sampleSizeInBits;
    private int blockAlign;
    private int samplesPerBlock;
    private long minLocation;
    private long maxLocation;
    private double locationToMediaTime = -1;
    private double timePerBlockNano = -1.0;
    private PullSourceStream stream = null;
    private static ContentDescriptor[] supportedFormat = new ContentDescriptor[] {new ContentDescriptor("audio.x_wav")};

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
	maxLocation = minLocation + dataSize;
	// System.out.println("Location after readHeader() is " + minLocation);
	// System.out.println("minLocation is " + minLocation);
	// System.out.println("maxLocation is " + maxLocation);
	
	tracks[0] = new WavTrack(format,
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
	
	String magicRIFF = readString(stream);
	// System.out.println("magicRIFF is " + magicRIFF);
	if (!(magicRIFF.equals("RIFF"))) {
	    // System.out.println("Fatal Error: Expected magic string RIFF, got " + magicRIFF);
	    throw new BadHeaderException("WAVE Parser: expected magic string RIFF, got "
					 + magicRIFF);
	}
	
	int length = readInt(stream, /* bigEndian = */ false);

        // System.out.println("length is " + length);

	String magicWAVE = readString(stream);
	// System.out.println("magicWAVE is " + magicWAVE);
	if (!(magicWAVE.equals("WAVE"))) {
	    // System.out.println("Fatal Error: Expected magic string WAVE, got " +
	    //		       magicWAVE);
	    throw new BadHeaderException("WAVE Parser: expected magic string WAVE, got "
					 + magicWAVE);

	}

	length += 8; // Add 4 bytes for RIFF and WAVE fields
	// Only the required chunks 'fmt ' and 'data' are supported.
	// There are no restrictions upon the order of the chunks within a WAVE file,
	// with the exception that the Format chunk must precede the Data chunk.

	// Skip all chunks until you reach the 'fmt ' chunk
	while ( ! (readString(stream)).equals("fmt ") ) {
	    int size = readInt(stream, /* bigEndian = */ false);
	    skip(stream, size);
	}

	// Handle Format chunk 'fmt '
	int formatSize = readInt(stream, /*bigEndian = */false);
	int remainingFormatSize = formatSize;
	// System.out.println("formatSize is " + formatSize);

	// formatSize should be atleast 16
	if (formatSize < 16) {
	    // TODO: throw new BadHeaderException
	    // Is this necessary? Check.
	}
	encoding = readShort(stream, /* bigEndian = */ false);
	encodingString = (String) WavAudioFormat.formatMapper.get(new Integer(encoding));
	if (encodingString == null) {
	    encodingString = "unknown";
	}
	channels = readShort(stream, /* bigEndian = */ false);

	sampleRate = readInt(stream, /* bigEndian = */ false);
	int bytesPerSecond = readInt(stream, /* bigEndian = */ false);

	blockAlign = readShort(stream, /* bigEndian = */ false);

	sampleSizeInBits = readShort(stream, /* bigEndian = */ false);
	if (encoding == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {
	    // Some files have sampleSizeInBits as 0
	    sampleSizeInBits = 16;
	}
	samplesPerBlock = -1;
	remainingFormatSize -= 16;
	
	{ //  Workaround for files with bad headers
	    /**
	     * Some files, for e.g TooGood_22_8bit.wav and
	     * US3_44_Stereo.wav, have inconsistent fmt chunk
	     * They are uncompressed, yet have fmt size of 18
	     * and claim extra header size of 40.
	     * Should throw a BadHeaderException. But these
	     * files play fine on Windows player. Hence the
	     * workaround to makes these and potentially others
	     * like this play.
	     */

	    if ( ((remainingFormatSize > 0) &&
		   encoding == WavAudioFormat.WAVE_FORMAT_PCM) ||
		 (remainingFormatSize <= 2) ) {
		skip(stream, remainingFormatSize);
		remainingFormatSize = 0;
	    }
	}

	byte[] codecSpecificHeader = null;
	int extraFieldsSize = 0;
	if (remainingFormatSize >= 2) {
	    extraFieldsSize = readShort(stream, /* bigEndian = */ false);
	    remainingFormatSize -= 2;

	    if (extraFieldsSize > 0) {
		codecSpecificHeader = new byte[extraFieldsSize];
		readBytes(stream, codecSpecificHeader, codecSpecificHeader.length);
		remainingFormatSize -= extraFieldsSize;
	    }
	}

	switch (encoding) {
	case WavAudioFormat.WAVE_FORMAT_ADPCM:
	case WavAudioFormat.WAVE_FORMAT_DVI_ADPCM:
	case WavAudioFormat.WAVE_FORMAT_GSM610:
	    {
		if (extraFieldsSize < 2) {
		    throw new BadHeaderException("msadpcm: samplesPerBlock field not available");
		}
		samplesPerBlock = BasicPullParser.parseShortFromArray(codecSpecificHeader,
						      /* bigEndian = */ false);
		
		locationToMediaTime = (double) samplesPerBlock / (sampleRate * blockAlign);
	    }
	    break;
	default: // CHECK
	    locationToMediaTime = 1.0 / (sampleRate * blockAlign);
	    break;

	}
	
	if (remainingFormatSize < 0) {
	    // TODO: Should throw BadHeaderException
	    throw new BadHeaderException("WAVE Parser: incorrect chunkSize in the fmt chunk");

	}
	if (remainingFormatSize > 0) {
	    skip(stream, remainingFormatSize);
	}

	// Skip all chunks until you reach the 'data' chunk
	while ( ! (readString(stream)).equals("data") ) {
	    int size = readInt(stream, /* bigEndian = */ false);
	    skip(stream, size);
	}

	// Handle Format chunk 'data'

	dataSize = readInt(stream, /* bigEndian = */ false);

	// Compute the maximum number of integral
	// blocks that can stored within the averageBytesPerSecond.
        if ( blockAlign != 0 )
	    if ( bytesPerSecond < dataSize )
                bufferSize = bytesPerSecond -
				  (bytesPerSecond % blockAlign);
	    else 
		bufferSize = dataSize - (dataSize % blockAlign);
        else  
	    if (bytesPerSecond < dataSize)
                bufferSize = bytesPerSecond;
	    else
		bufferSize = dataSize;

	// There is only 1 track.
	double durationSeconds = -1;

// 	if (samplesPerBlock > 0) {
// 	    // ADPCM or DVI_ADPCM or GSM610
// 	    durationSeconds = (float)dataSize/blockAlign*samplesPerBlock/sampleRate;
// 	    timePerBlockNano = (samplesPerBlock * 1.0E9) / sampleRate;	    
// 	    // System.out.println("timePerBlockNano is " + timePerBlockNano);
// 	} else if ( encoding == WavAudioFormat.WAVE_FORMAT_DSPGROUP_TRUESPEECH ) {
// 	    timePerBlockNano = (blockAlign * 1.0E9 / bytesPerSecond);
// 	    durationSeconds = (float)dataSize/bytesPerSecond;
// 	} else {
// 	    durationSeconds = (float)dataSize/bytesPerSecond;
// 	}
	if ( (channels * sampleSizeInBits / 8) == blockAlign ) {
	    /**
	     * Will Handle formats like 
	     * WAVE_FORMAT_PCM, WAVE_FORMAT_MULAW, WAVE_FORMAT_ALAW
	     * WAVE_IBM_FORMAT_MULAW, WAVE_IBM_FORMAT_ALAW etc.
	     */
	    
 	    durationSeconds = (float)dataSize/bytesPerSecond;
	} else if (samplesPerBlock > 0) {
 	    // ADPCM or DVI_ADPCM or GSM610
 	    durationSeconds = (float)dataSize/blockAlign*samplesPerBlock/sampleRate;
 	    timePerBlockNano = (samplesPerBlock * 1.0E9) / sampleRate;	    
	} else {
	    // WAVE_FORMAT_DSPGROUP_TRUESPEECH and others
 	    timePerBlockNano = (blockAlign * 1.0E9 / bytesPerSecond);
 	    durationSeconds = (float)dataSize/bytesPerSecond;
	}

	duration = new Time(durationSeconds);
	// System.out.println("Encoding: " + encoding );
	// System.out.println("Encoding String: " + encodingString );
	// System.out.println("Channels: " + channels );
	// System.out.println("Sample Rate: "+ sampleRate);
	// System.out.println("bytesPerSec: " + bytesPerSecond);
	// System.out.println("Block Align: " + blockAlign);
	// System.out.println("sample size in bits : " + sampleSizeInBits );
	// System.out.println("samplesPerBlock (not applicable if -ve) is " + samplesPerBlock);
	// System.out.println("durationSeconds is " + durationSeconds);


	// TODO: CHECK
	boolean signed;
	if (sampleSizeInBits > 8)
	    signed = true;
	else
	    signed = false;

	format = new WavAudioFormat(encodingString,
				    sampleRate,
				    sampleSizeInBits,
				    channels,
				    /*frameSizeInBits=*/blockAlign * 8,
				    bytesPerSecond,
				    AudioFormat.LITTLE_ENDIAN,
				    signed ? AudioFormat.SIGNED : AudioFormat.UNSIGNED,
				    Format.NOT_SPECIFIED, // No FRAME_RATE specified
				    Format.byteArray,
				    codecSpecificHeader);

	// System.out.println("Audio format is " + format);
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

	// if ( (channels * sampleSizeInBits / 8) == blockAlign ) {
	if ( timePerBlockNano <= 0 ) {
	    /**
	     * Will Handle formats like 
	     * WAVE_FORMAT_PCM, WAVE_FORMAT_MULAW, WAVE_FORMAT_ALAW
	     * WAVE_IBM_FORMAT_MULAW, WAVE_IBM_FORMAT_ALAW etc.
	     */

	    // TODO: Precalculate constant expressions
	    int bytesPerSecond = sampleRate * blockAlign;
	    double newPosd = time * sampleRate * blockAlign / 1000000000.0;
	    double remainder = (newPosd % blockAlign);
	    
	    newPos = (long) (newPosd - remainder);

	    if (remainder > 0) {
		switch (rounding) {
		case Positionable.RoundUp:
		    newPos += blockAlign;
		    break;
		case Positionable.RoundNearest:
		    if (remainder > (blockAlign / 2.0))
			newPos += blockAlign;
		    break;
		}
	    }
	} else {
 	    // ADPCM, DVI_ADPCM, GSM610 and others where
	    // (channels * sampleSizeInBytes) != blockAlign

	    double blockNum = time / timePerBlockNano;
	    int blockNumInt = (int) blockNum;
	    double remainder = blockNum - blockNumInt;

	    if (remainder > 0) {
		switch (rounding) {
		case Positionable.RoundUp:
		    blockNumInt++;
		    break;
		case Positionable.RoundNearest:
		    if (remainder > 0.5)
			blockNumInt++;
		    break;
		}
	    }
	    newPos = blockNumInt * blockAlign;
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
	    mediaTime.set( location * locationToMediaTime);
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
	return "Parser for WAV file format";
    }




    
    class WavTrack extends BasicTrack {
	private double sampleRate;
	private float timePerFrame;
	private SettableTime frameToTime = new SettableTime();

	WavTrack(WavAudioFormat format, boolean enabled, Time startTime,
	       int numBuffers, int bufferSize,
	       long minLocation, long maxLocation) {
	    super(WavParser.this, 
		  format, enabled, WavParser.this.duration,
		  startTime, numBuffers, bufferSize,
		  WavParser.this.stream, minLocation, maxLocation);

	    double sampleRate = format.getSampleRate();
	    int channels = format.getChannels();
	    int sampleSizeInBits = format.getSampleSizeInBits();
	    int blockSize = format.getFrameSizeInBits() / 8;

	    float bytesPerSecond;
	    float bytesPerFrame;
	    float samplesPerFrame;

	    // Note: compiler doesn't accept a switch statement here
	    if ( (encoding == WavAudioFormat.WAVE_FORMAT_PCM) ||
		 (encoding == WavAudioFormat.WAVE_FORMAT_MULAW) ||
		 (encoding == WavAudioFormat.WAVE_FORMAT_ALAW) ||
		 (encoding == WavAudioFormat.WAVE_IBM_FORMAT_MULAW) ||
		 (encoding == WavAudioFormat.WAVE_IBM_FORMAT_ALAW) ) {
		bytesPerSecond = (float) sampleRate * blockSize;
		bytesPerFrame = bufferSize;
		timePerFrame = bufferSize / bytesPerSecond;
	    } else if ( (encoding == WavAudioFormat.WAVE_FORMAT_ADPCM) ||
                        (encoding == WavAudioFormat.WAVE_FORMAT_DVI_ADPCM) ||
                        (encoding == WavAudioFormat.WAVE_FORMAT_GSM610) ) {
		
		bytesPerFrame = bufferSize;
		float blocksPerFrame = bufferSize / (float) blockSize;
		samplesPerFrame = blocksPerFrame * samplesPerBlock;
		timePerFrame = (float) (samplesPerFrame / sampleRate);
	    } else {
		// TODO: see if you need to do anything special
		// in this case. If not, modify the if statement
		// so that ADPCM, DVI_ADPCM and GSM610 is one
		// case and the rest is the else case
		bytesPerSecond = (float) sampleRate * blockSize;
		bytesPerFrame = bufferSize;
		timePerFrame = bufferSize / bytesPerSecond;
	    }
	}

	WavTrack(WavAudioFormat format, boolean enabled, Time startTime,
		 int numBuffers, int bufferSize) {
	    this(format, enabled,
		  startTime, numBuffers, bufferSize,
		  0L, Long.MAX_VALUE);

	}
    }
}
