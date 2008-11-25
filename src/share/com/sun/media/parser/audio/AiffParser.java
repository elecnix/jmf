/*
 * @(#)AiffParser.java	1.14 02/08/21
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
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import com.sun.media.parser.BasicPullParser;
import com.sun.media.parser.BasicTrack;
import com.sun.media.util.SettableTime;


/**
 *  TODO: find out about the wave chunk in aiff files with "32 bit integer compression"
 *  Why does the header say AIFF instead of AIFC? Why does the header say 32 bits/pixel
 *  even though when I created in in QT3.0, it said 16 bits/pixel.
 */

public class AiffParser extends BasicPullParser {
    private Time duration = Duration.DURATION_UNKNOWN;
    private Format format = null;
    private Track[] tracks = new Track[1]; // Only 1 track is there for aiff
    private int numBuffers = 4; // TODO: check
    private int bufferSize = -1;
    private int dataSize;
    private SettableTime mediaTime = new SettableTime(0L);
    private PullSourceStream stream = null;
    private int maxFrame;
    private int blockSize = 0;
    private double sampleRate = -1.0;
    private long minLocation;
    private long maxLocation;
    private String encodingString = null;
    private int samplesPerBlock = 1;
    private double timePerBlockNano = -1.0;
    private double locationToMediaTime = -1;

    public final static String FormID = "FORM"; //   ID for Form Chunk
    public final static String FormatVersionID = "FVER";   // ID for Format Version Chunk
    public final static String CommonID = "COMM";   // ID for Common Chunk
    public final static String SoundDataID = "SSND";   // ID for Sound Data Chunk
    private static ContentDescriptor[] supportedFormat = new ContentDescriptor[] {new ContentDescriptor("audio.x_aiff")};

    public final static int CommonIDSize = 18;   // ID for Common Chunk for AIFF

    private boolean isAIFC = false;
    private boolean commonChunkSeen = false; // COMM chunk mandatory
    private boolean soundDataChunkSeen = false; // COMM chunk mandatory
    private boolean formatVersionChunkSeen = false; // mandatory for aifc


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

	tracks[0] = new AiffTrack((AudioFormat) format,
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
     * Creats format and computes bufferSize
     */
    private void /* for now void */ readHeader()
	throws IOException, BadHeaderException {

 	boolean signed = true;

	String magic = readString(stream);
	if (!(magic.equals(FormID))) {
	    throw new BadHeaderException("AIFF Parser: expected string " +
					 FormID + ", got "
					 + magic);
	}

	int fileLength = readInt(stream) + 8;

	String formType = readString(stream);
	if (formType.equals("AIFC")) {
	    isAIFC = true;
	} else {
	    encodingString = AudioFormat.LINEAR; // and signed is true
	}

	int remainingLength = fileLength - 12;
	
	String compressionType = null;
	int offset = 0;
	int channels = -1;
	int sampleSizeInBits = -1;


	while (remainingLength >= 8) {
	    String type = readString(stream);
	    int size = readInt(stream);
	    remainingLength -= 8;

	    /**
	     * The Format Version Chunk contains a timestamp field that indicates
	     * when the format version of this AIFF-C file was defined. This in
	     * turn indicates what format rules this file conforms to and allows
	     * you to ensure that your application can handle a particular AIFF-C
	     * file. Every AIFF-C file must contain one and only one Format Version
	     * Chunk
	     */
	    if (type.equals(FormatVersionID)) {
		if (!isAIFC) {
		    // System.err.println("Warning: AIFF file shouldn't have Format version chunk");
		}
		int timestamp = readInt(stream);
		if (size != 4) {
		    throw new BadHeaderException("Illegal FormatVersionID: chunk size is not 4 but "
						 + size);
		}
		formatVersionChunkSeen = true;
	    } else if (type.equals(CommonID)) {
		if (size < CommonIDSize) {
		    throw new BadHeaderException("Size of COMM chunk should be atleast " +
						 CommonIDSize);
		}
		channels = readShort(stream);

		if  ( channels < 1 )
		    throw new BadHeaderException("Number of channels is " + channels);
		
		maxFrame = readInt(stream);
		sampleSizeInBits = readShort(stream);
		if (sampleSizeInBits <= 0) {
		    throw new BadHeaderException("Illegal sampleSize " + sampleSizeInBits);
		}
		sampleRate = readIeeeExtended(stream);
		if  ( sampleRate < 0 )
		    throw new BadHeaderException("Negative Sample Rate " + sampleRate);

		int remainingCommSize = size - CommonIDSize;
		if (isAIFC) {
		    /**
		     * AIFC files have compressionType and compressionName
		     * as extra fields
		     */
		    if (remainingCommSize < 4) {
			throw new BadHeaderException("COMM chunk in AIFC doesn't have compressionType info");
		    }
		    compressionType = readString(stream);
		    if (compressionType == null) {
			throw new BadHeaderException("Compression type for AIFC is null");
		    }
		    skip(stream, remainingCommSize-4); // skip compressionName
		}
		commonChunkSeen = true;
	    } else if (type.equals(SoundDataID)) {
		if (soundDataChunkSeen) {
		    throw new BadHeaderException("Cannot have more than 1 Sound Data Chunk");
		}

		offset = readInt(stream);
		blockSize = readInt(stream);
		minLocation = getLocation(stream);
		dataSize = size - 8;
		maxLocation = minLocation + dataSize; // TODO: Verify
		
		soundDataChunkSeen = true;
		if (commonChunkSeen) {
		    // parsing of mandatory chunks done
		    remainingLength -= 8;
		    break;
		}
		skip(stream, size-8);
	    } else {
		// System.err.println("Chunk " + type + " not handled");
		skip(stream, size);
	    }
	    remainingLength -= size;
	}

	/**
	 * Commented out the following even though it is valid, because
	 * in order to significantly speedup up parsing time (especially
	 * in the http case) the optional chunks following the Sound Data
	 * Chunk (SSND) are not processed unless the mandatory chunk
	 * COMM comes after SSND. Skipping past the typically large SSND
	 * chunk to parse optional chunks will cause the parse time to be high
	 * when using a slow http connection. Though the FVER chunk in the case
	 * of AIFC (compressed format) is mandatory for AIFC, it is not
	 * currently used; therefore unless it comes before the mandatory
	 * COMM or SSND chunks, it is not processed. Since the FVER chunk may
	 * come after the SSND chunk and hence not processed, the following code
	 * to check for it is commented out.
	 */
	// if (isAIFC && !formatVersionChunkSeen) {
	//   throw new BadHeaderException("Mandatory chunk FVER not present in AIFC file");
	// }

	if ( !commonChunkSeen ) {
	    throw new BadHeaderException("Mandatory chunk COMM missing");
	}

	if ( !soundDataChunkSeen ) {
	    throw new BadHeaderException("Mandatory chunk SSND missing");
	}

	double durationSeconds = -1;

	if (isAIFC) {
	    String c = compressionType;
	    if (c.equalsIgnoreCase("NONE")) 
		encodingString = AudioFormat.LINEAR;
	    else if (c.equalsIgnoreCase("twos"))
		encodingString = AudioFormat.LINEAR;
	    else if (c.equalsIgnoreCase("raw")) {
		encodingString = AudioFormat.LINEAR;
		signed = false;
	    } else if ( c.equalsIgnoreCase("ULAW")) {
		encodingString = AudioFormat.ULAW;
		sampleSizeInBits = 8;
		signed = false;
	    } else if ( c.equalsIgnoreCase("ALAW")) {
		encodingString = AudioFormat.ALAW;
		sampleSizeInBits = 8;
		signed = false;
	    } else if ( c.equalsIgnoreCase("G723")) {

		/**
		 * For some reason, aiff files
		 * specify block size incorrectly as 0
		 * for mac3, mac6, ima4
		 * So, compute blockSize
		 */

		/**
		 * TODO: get/compute samplesPerBlock
		 * If block size is incorrectly specified
		 * as 0, compute it if possible
		 */
		encodingString = AudioFormat.G723;
		// samplesPerBlock = ???? TODO
		// blockSize = ???? TODO
		// timePerBlockNano = ???? TODO
	    } else if ( c.equalsIgnoreCase("MAC3")) {
		// 'MAC3' Samples have been compressed using MACE 3:1.
		encodingString = AudioFormat.MAC3;
		// 2 bytes represent 6 samples
		blockSize = 2;
		samplesPerBlock = 6;
		timePerBlockNano = (samplesPerBlock * 1.0E9) / sampleRate;	    
	    } else if ( c.equalsIgnoreCase("MAC6")) {
		// 'MAC6' Samples have been compressed using MACE 6:1.
		encodingString = AudioFormat.MAC6;
		// 1 byte represent 6 samples
		blockSize = 1;
		samplesPerBlock = 6;
		timePerBlockNano = (samplesPerBlock * 1.0E9) / sampleRate;
	    } else if ( c.equalsIgnoreCase("IMA4")) {
		encodingString = AudioFormat.IMA4;
		/**
		 * Each packet contains 64 samples. Each sample is 4 bits/channel.
		 * So 64 samples is 32 bytes/channel.
		 * The 2 in the equation refers two bytes that the Apple's
		 * IMA compressor puts at the front of each packet, which 
		 * are referred to as predictor bytes
		 */
		blockSize = (32 + 2) * channels;
		samplesPerBlock = 64;
		timePerBlockNano = (samplesPerBlock * 1.0E9) / sampleRate;	    
	    }
	    else 
		throw new BadHeaderException("Unsupported encoding" + c);
	}


	if (blockSize == 0)
	    blockSize = channels * sampleSizeInBits / 8;

	/**
	 *  There are few aiff files that have the maxFrame value in the
	 *  aiff header as 0. maxFrame is needed to compute media duration.
	 *  maxFrame can also be computed as (dataSize / blockSize).
	 *  But files that report maxFrame as 0 also report a very high and
	 *  incorrect number for dataSize.
	 *  This number, typically 2130706432, is much higher that the
	 *  fileSize. The maxFrame that is computed is obviously wrong.
	 *  So, maxFrame is not computed if header says that it is 0.
	 *  Examples of such files are bark16.aiff, nick16.aiff, pcm.aiff.
	 *  Apple's MoviePlayer is unable to play these files.
	 *  Since aiff is an Apple format, we can assume that these files
	 *  are illegal or malformed aiff files.
	 *  JMF will, however, play these files. Duration will be unknown
	 *  until end of Media. After end of media, dataSize can be
	 *  computed as (maxLocation - minLocation) and duration can
	 *  be computed.
	 *  Ironically, for files that report a corect maxFrame in the
	 *  header, the computed maxFrame (ie dataSize/blockSize) agrees
	 *  with that number.
	 */
	// if (maxFrame == 0) {
	//    System.out.println("maxFrame is 0, computing it");
	//    maxFrame = dataSize / blockSize;
	//}

	bufferSize = blockSize * (int) (sampleRate / samplesPerBlock);
	durationSeconds = (maxFrame * samplesPerBlock) / sampleRate;
	// System.out.println("durationSeconds is " + durationSeconds);
	
	if (durationSeconds > 0)
	    duration = new Time(durationSeconds);

	locationToMediaTime = samplesPerBlock / (sampleRate * blockSize);

	format = new AudioFormat(encodingString,
				 sampleRate,
				 sampleSizeInBits,
				 channels,
				 AudioFormat.BIG_ENDIAN,
				 signed ? AudioFormat.SIGNED : AudioFormat.UNSIGNED,
				 /*frameSizeInBits=*/ blockSize*8,
				 Format.NOT_SPECIFIED, // No FRAME_RATE specified
				 Format.byteArray);
	// System.out.println("format is " + format);
    }


    public Time setPosition(Time where, int rounding) {
	if (! seekable ) {
	    return getMediaTime();
	}

	long time = where.getNanoseconds();
	long newPos;

	if (time < 0)
	    time = 0;


	// timePerBlockNano is only computed for compressed formats
	// where mutiple samples are packed into a packet or block
	if (timePerBlockNano == -1) {
	    // LINEAR, ULAW, ALAW
	    // TODO: Precalculate constant expressions
	    int bytesPerSecond = (int) sampleRate * blockSize;
	    double newPosd = time * sampleRate * blockSize / 1000000000.0;
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
	} else {
	    // IMA4, MAC3, MAC6
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
	    newPos = blockNumInt * blockSize;
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
	return where;
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
	if (maxFrame <= 0) {
	    if ( tracks[0] != null ) {
		long mediaSizeAtEOM = ((BasicTrack) tracks[0]).getMediaSizeAtEOM();
		if (mediaSizeAtEOM > 0) {
		    maxFrame = (int) (mediaSizeAtEOM / blockSize);
		    double durationSeconds = (maxFrame * samplesPerBlock) / sampleRate;
		    if (durationSeconds > 0)
			duration = new Time(durationSeconds);
		}
	    }
	}
	return duration;
    }

    /**
     * Returns a descriptive name for the plug-in.
     * This is a user readable string.
     */
    public String getName() {
	return "Parser for AIFF file format";
    }

    class AiffTrack extends BasicTrack {
	private double sampleRate;
	private float timePerFrame; // TODO calculate this
	private SettableTime frameToTime = new SettableTime();

	AiffTrack(AudioFormat format, boolean enabled, Time startTime,
	       int numBuffers, int bufferSize,
	       long minLocation, long maxLocation) {
	    super(AiffParser.this, 
		  format, enabled, AiffParser.this.duration,
		  startTime, numBuffers, bufferSize,
		  AiffParser.this.stream, minLocation, maxLocation);

	    double sampleRate = format.getSampleRate();
	    int channels = format.getChannels();
	    int sampleSizeInBits = format.getSampleSizeInBits();

	    float bytesPerSecond;
	    float samplesPerFrame;

	    // timePerBlockNano is only computed for compressed formats
	    // where mutiple samples are packed into a packet or block
	    if (timePerBlockNano == -1) {
		// LINEAR, ULAW, ALAW
		bytesPerSecond = (float) (sampleRate * blockSize);
		timePerFrame = bufferSize / bytesPerSecond;
	    } else {
		// IMA4, MAC3, MAC6
		float blocksPerFrame = bufferSize / (float) blockSize;
		samplesPerFrame = blocksPerFrame * samplesPerBlock;
		timePerFrame = (float) (samplesPerFrame / sampleRate);
	    }
	}

	AiffTrack(AudioFormat format, boolean enabled, Time startTime,
		 int numBuffers, int bufferSize) {
	    this(format, enabled,
		  startTime, numBuffers, bufferSize,
		  0L, Long.MAX_VALUE);

	}
    }


    /**
      * read_ieee_extended
      * Extended precision IEEE floating-point conversion routine.
      * @argument DataInputStream
      * @return double
      * @exception IOException
      */
    private double readIeeeExtended(PullSourceStream stream) 
	throws IOException {

	double f = 0;
        int expon = 0;
        long hiMant = 0, loMant = 0;
        long t1, t2;
	double huge = 3.40282346638528860e+38;

	int s;

	expon = readShort(stream);
	hiMant = readInt(stream);
	if (hiMant < 0)  // 2's complement
	   hiMant += 4294967296L;

        loMant = readInt(stream);
        if (loMant < 0)  // 2's complement
           loMant += 4294967296L;

	if (expon == 0 && hiMant == 0 && loMant == 0) {
		f = 0;
	} else {
    	    if (expon == 0x7FFF)
	       f = huge;
	    else {
	       expon -= 16383;
	       expon -= 31;
	       f = (hiMant * Math.pow(2, expon));
	       expon -= 32;
	       f += (loMant * Math.pow(2, expon));
	    }
	}
        return f;
    }
}

