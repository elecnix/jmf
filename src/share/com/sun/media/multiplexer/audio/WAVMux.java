/*
 * @(#)WAVMux.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.multiplexer.audio;

import javax.media.Time;
import javax.media.Duration;
import javax.media.Buffer;
import javax.media.Multiplexer;
import javax.media.Format;
import javax.media.PlugIn;
import javax.media.protocol.Seekable;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceStream;
import javax.media.protocol.SourceTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import com.sun.media.BasicPlugIn;
import com.sun.media.format.WavAudioFormat;
import javax.media.format.UnsupportedFormatException;
import java.io.IOException;
import javax.media.Control;
import javax.media.IncompatibleSourceException;
import javax.media.format.AudioFormat;

public class WAVMux extends com.sun.media.multiplexer.BasicMux {

    private AudioFormat audioFormat;
    private WavAudioFormat wavAudioFormat;
    private int sampleSizeInBits;
    private double sampleRate;
    private int channels;
    private byte[] codecSpecificHeader;
    private short wFormatTag;
    private int blockAlign = 0;
    private int bytesPerSecond = 0;
    private int dataSizeOffset;
    private int numberOfSamplesOffset;
    private int factChunkLength = 0;
    
    public WAVMux() {
	supportedInputs = new Format[1];
	supportedInputs[0] = new AudioFormat(null);
	//				     8000.0,
	//				     33 * 8,
	//				     1);
	supportedOutputs = new ContentDescriptor[1];
	supportedOutputs[0] = new FileTypeDescriptor(FileTypeDescriptor.WAVE);
    }

    public String getName() {
	return "WAVE Audio Multiplexer";
    }

    // Little Endian Format for intersecting with the input format.
    Format signed = new AudioFormat(null,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.LITTLE_ENDIAN,
			AudioFormat.SIGNED);

    Format unsigned = new AudioFormat(null,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.LITTLE_ENDIAN,
			AudioFormat.UNSIGNED);

    public Format setInputFormat(Format input, int trackID) {
	if (!(input instanceof AudioFormat))
	    return null;
	AudioFormat format = (AudioFormat) input;
	sampleRate =  format.getSampleRate();

	String reason = null;

	// Check to see if some of these restrictions can be removed
	/*if (format.getEndian() == AudioFormat.BIG_ENDIAN)
	    reason = "Endian should be LITTLE_ENDIAN";
	/*
	else if ( Math.abs(sampleRate - 8000.0) > epsilon )
	    reason = "Sample rate should be 8000. Cannot handle sample rate " + sampleRate;
 	else if (format.getFrameSizeInBits() != (33*8))
 	    reason = "framesize should be 33 bytes";
	else if (format.getChannels() != 1)
	    reason = "Number of channels should be 1";		
	*/
	
	audioFormat = (AudioFormat) format;
	if (format instanceof WavAudioFormat) {
	  wavAudioFormat = (WavAudioFormat) format;
	}

	String encodingString = audioFormat.getEncoding();
	sampleSizeInBits = audioFormat.getSampleSizeInBits();

	if (encodingString.equalsIgnoreCase(AudioFormat.LINEAR)) {
	    if ( sampleSizeInBits > 8 ) {

	        if (audioFormat.getEndian() == AudioFormat.BIG_ENDIAN)
		    return null;

		if (audioFormat.getSigned() == AudioFormat.UNSIGNED)
		    return null;

		// Data has to be little endian and signed.
	        if (audioFormat.getEndian() == AudioFormat.NOT_SPECIFIED ||
		    audioFormat.getSigned() == AudioFormat.NOT_SPECIFIED) {
		    format = (AudioFormat)audioFormat.intersects(signed);
		}

	    } else {

		if (audioFormat.getSigned() == AudioFormat.SIGNED)
		    return null;

		// Data has to be unsigned.
	        if (audioFormat.getEndian() == AudioFormat.NOT_SPECIFIED ||
		    audioFormat.getSigned() == AudioFormat.NOT_SPECIFIED) {
		    format = (AudioFormat)audioFormat.intersects(unsigned);
		}
	    }
	}
	
	Integer formatTag =
	    (Integer)
	       WavAudioFormat.reverseFormatMapper.get(encodingString.toLowerCase());
	if (formatTag == null) {
	    reason = "Cannot handle format";
	    return null;
	}
	
	// For certain encodings additional encoding-specific information
	// is required which can only be obtained thru WavAudioFormat
	wFormatTag = formatTag.shortValue();
	switch (wFormatTag) {
	    case WavAudioFormat.WAVE_FORMAT_ADPCM:
	    case WavAudioFormat.WAVE_FORMAT_DVI_ADPCM:
	    case WavAudioFormat.WAVE_FORMAT_GSM610:
		if (wavAudioFormat == null) {
		    reason = "A WavAudioFormat is required " +
			" to provide encoding specific information for this " +
			"encoding " + wFormatTag;
	        }
        } 
	
	if (wavAudioFormat != null) {
	    codecSpecificHeader = wavAudioFormat.getCodecSpecificHeader();
	    bytesPerSecond = wavAudioFormat.getAverageBytesPerSecond();
	}
	
	sampleRate = audioFormat.getSampleRate();
	channels = audioFormat.getChannels();

	if (reason != null) {
	    return null;
	} else {
	    inputs[0] = format;
	    return format;
	}
    }

    public int setNumTracks(int nTracks) {
	if (nTracks != 1)
	    return 1;
	else
	    return super.setNumTracks(nTracks);
    }

    protected void writeHeader() {
	int formatSize = 16;  // Minimum formatSize
	bufClear();
	audioFormat = (AudioFormat) inputs[0];

	codecSpecificHeader = null;
	if (audioFormat instanceof WavAudioFormat)
	    codecSpecificHeader = ((WavAudioFormat)audioFormat).getCodecSpecificHeader();
	if (codecSpecificHeader != null) {
	    formatSize += ( 2 +  // for extra field size)
			    codecSpecificHeader.length);
	} else if (wFormatTag == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {

	    formatSize += ( 2 +  // for extra field size)
		       12); // Extra info for mp3 is 12
	}

	bufWriteBytes("RIFF");
	bufWriteInt(0); // This is filled in later when filesize is known
	bufWriteBytes("WAVE");

	bufWriteBytes("fmt "); // Format Chunk

	bufWriteIntLittleEndian(formatSize);
	int frameSizeInBits = audioFormat.getFrameSizeInBits();
	if (frameSizeInBits > 0) {
	    blockAlign = frameSizeInBits / 8;
	} else {
	    blockAlign = (sampleSizeInBits / 8) * channels;
	}


	bufWriteShortLittleEndian(wFormatTag); // encoding

	bufWriteShortLittleEndian((short) channels);

	bufWriteIntLittleEndian((int) sampleRate);


	int frameRate = Format.NOT_SPECIFIED;
	if (wFormatTag == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {
	    blockAlign = 1;
	    frameRate = (int) audioFormat.getFrameRate();
	    if (frameRate != Format.NOT_SPECIFIED) {
		bytesPerSecond = frameRate;
	    }
	}

	if (bytesPerSecond <= 0) {
	    bytesPerSecond = channels * sampleSizeInBits * (int) sampleRate / 8;
	}

	bufWriteIntLittleEndian(bytesPerSecond);

	bufWriteShortLittleEndian((short) blockAlign);

	if (wFormatTag == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {
	    bufWriteShortLittleEndian((short) 0);
	} else {
	    bufWriteShortLittleEndian((short) sampleSizeInBits);
	}

	if (codecSpecificHeader != null) {
	    bufWriteShortLittleEndian((short) codecSpecificHeader.length);
	    bufWriteBytes(codecSpecificHeader);
	} else if (wFormatTag == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {
	    /**
	     * Note: TODO: Info on the extra fields in WAVE_FORMAT_MPEG_LAYER3
	     * not available. After looking at several wav files, the wID,
	     * fdwFlags, nFramesPerBlock and nCodecDelay always have the
	     * value 1, 2, 1 and 1393
	     * Only the nBlockSize value varies from file to file
	     * Setting these value until I can get the meaning of
	     * these fields in codecSpecificHeader
	     */
	    /*
	      8kbit    8000 mono    72
	      16kbit   8000 mono   144
	      8kbit   11025 mono    52
	      16kbit  11025 mono   104
	      32kbit  11025 stereo   208
	      16kbit  11025 mono   104
 	      16kbit  16000 mono    72
	      24kbit  22050 mono    78
	      32kbit   8000 stereo  288
              128kbit 44100 stereo  417??
              56k     24000 stereo   168

	     */

	    int blockSize;
	    if (frameRate > 0) {
		float temp = (72F * frameRate * 8) / 8000F;
		temp *= (8000F / sampleRate);
		blockSize = (int) temp;
	    } else {
		blockSize = 417; // ???
	    }

	    bufWriteShortLittleEndian((short) 12); // Length of extra data
	    bufWriteShortLittleEndian((short)1); // wID
	    bufWriteIntLittleEndian(2); // fdwFlags
	    bufWriteShortLittleEndian((short) blockSize); // nBlockSize
	    bufWriteShortLittleEndian((short)1); // nFramesPerBlock
	    bufWriteShortLittleEndian((short)1393); // nCodecDelay
	}	    

	factChunkLength = 0;
	if (wFormatTag != WavAudioFormat.WAVE_FORMAT_PCM) {
	    // Compressed formats require a FACT chunk
	    bufWriteBytes("fact"); // Fact Chunk
	    bufWriteIntLittleEndian(4);
	    numberOfSamplesOffset = filePointer;
	    bufWriteInt(0);   // This is filled in later when numberOfSamples is known
	    factChunkLength = 12;
	}

	bufWriteBytes("data"); // Data Chunk
	dataSizeOffset = (int) filePointer;
	bufWriteInt(0);        // This is filled in later when datasize is known
	/*
	fileSize = 4 + // for RIFF
	           4 + // file length
	           4 + // for WAVE

	           4 + // 'fmt ' chunk id
	           4 + //  'fmt ' chunk size field
                   formatSize +
	           4 + // 'data' chunk id
	           4; //  'data' chunk size field
	*/
	bufFlush();
    }

    protected void writeFooter() {
	// Write the file size
	seek(4);
	bufClear();
	bufWriteIntLittleEndian(fileSize - 8); // -8 since we should skip the first
	                                       // two ints
	bufFlush();
	// Write the data size
	seek(dataSizeOffset);
	bufClear();
	int dataSize = fileSize - (dataSizeOffset + 4);
	bufWriteIntLittleEndian(dataSize);
	bufFlush();
	if (factChunkLength > 0) {
	    // Has a Fact chunk
	    // Calculate numberOfSamples
	    // bytesPerSecond won't be zero
	    float duration = (float) dataSize / bytesPerSecond;
	    int numberOfSamples = (int) (duration * sampleRate);
	    seek(numberOfSamplesOffset);
	    bufClear();
	    bufWriteIntLittleEndian(numberOfSamples);
	    bufFlush();
	}
    }
}
