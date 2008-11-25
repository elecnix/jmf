/*
 * @(#)DePacketizer.java	1.7	02/08/21 SMI
 *
 * Copyright 1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.codec.audio.mpa;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import java.lang.Math;

public class DePacketizer extends com.sun.media.codec.audio.AudioCodec {

    private static int OUT_BUF_SIZE = 1024 * 4;

    private static int  MAX_SEQ = 65535;

    private static Format[] defaultSupportedOutputFormats = new Format[] {
			new AudioFormat(AudioFormat.MPEG, 44100., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEG, 48000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEG, 32000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEG, 22050., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEG, 24000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEG, 16000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEG, 11025., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEG, 12000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEG, 8000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 44100., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 48000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 32000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 22050., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 24000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 16000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 11025., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 12000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED),
			new AudioFormat(AudioFormat.MPEGLAYER3, 8000., 16,
						Format.NOT_SPECIFIED,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED)
    };

    private boolean bufferContinued = false;
    private boolean frameContinued = false;
    private int frameSize = 0;
    private int frameBegin = 0;
    private int frameOffset = 0;
    private long frameTimeStamp = 0;
    private long bufTimeStamp = 0;
    private long prevSeq = -1;
    private long outSeq = 0;
    private MPAParse mpaParse = new MPAParse();
    private MPAHeader mpaHeader = new MPAHeader();

    /*
     * Generate debug messages
     */
    private static final boolean debug = false;

    public DePacketizer () {
	inputFormats = new Format[] { new AudioFormat(AudioFormat.MPEG_RTP) };
    }

    public String getName () {
	return "MPEG Audio DePacketizer";
    }

    public Format [] getSupportedOutputFormats (Format in) {

	if (in == null) {
	    return defaultSupportedOutputFormats;
	}

	if (matches(in, inputFormats) == null) {
	    return new Format[1];
	}

	if (! (in instanceof AudioFormat) ) {
	    return defaultSupportedOutputFormats;
	}

	if (outputFormat != null) {
	    return new Format[] { outputFormat };
	}

	AudioFormat af =(AudioFormat) in;
	AudioFormat of = new AudioFormat(AudioFormat.MPEG,
			(af.getSampleRate() == Format.NOT_SPECIFIED
					? 44100. : af.getSampleRate()),
			(af.getSampleSizeInBits() == Format.NOT_SPECIFIED
					? 16 : af.getSampleSizeInBits()),
			(af.getChannels() == Format.NOT_SPECIFIED
					? 2 : af.getChannels()));
	return new Format[] { of };
    }

    public void open () {

    }

    public void close () {
	
    }

    public int process (Buffer inputBuffer, Buffer outputBuffer) {
	try {
	    return doProcess(inputBuffer, outputBuffer);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	return BUFFER_PROCESSED_FAILED;
    }

    public int doProcess (Buffer inputBuffer, Buffer outputBuffer) {

	int id;				// MPEG1 or MPEG2
	int layer;			// Audio layer 1, 2, or 3
	int crc;			// CRC present = 1
	int bitrate_index;		// index for constant bit rate
	int sampling_index;		// sampling frequency indicator
	int padding_bit;		// padding present = 1
	int channel_index;		// mono = 3, otherwise stereo

	String encoding;
	double sampleRate;
	int channels;

	if (!checkInputBuffer(inputBuffer) ) {
	    return BUFFER_PROCESSED_FAILED;
	}
	
	if (isEOM(inputBuffer) ) {
	    propagateEOM(outputBuffer);
	    mpaParse.reset();
	    return BUFFER_PROCESSED_OK;
	}

	byte[] inData = (byte[])inputBuffer.getData();
	int inOffset = inputBuffer.getOffset();
	int inLength = inputBuffer.getLength();
	int packetOffset = ((inData[inOffset+2]&0xff)<<8)
						+ (inData[inOffset+3]&0xff);
	inOffset += 4;	// skip MPEG audio specific RTP header
	inLength -= 4;
	if (packetOffset > 0) {
	    // this is a continuation of a previous frame
	    if (!frameContinued) {
		// Not expecting a continuation, just drop it
		if (debug) {
		    System.err.println(
		      "DePacketizer: Not expecting continuation, frame offset "
					+ packetOffset);
		}
		return OUTPUT_BUFFER_NOT_FILLED;
	    }

	    if (inputBuffer.getTimeStamp() != frameTimeStamp) {
		// Not part of the continuation, just drop it
		if (debug) {
		    System.err.println(
				"DePacketizer: Timestamp mismatch, expecting "
					+ frameTimeStamp + " got "
					+ inputBuffer.getTimeStamp());
		}
		dropFrame(outputBuffer);
		return OUTPUT_BUFFER_NOT_FILLED;
	    }

	    if (getSequenceDiff(prevSeq, inputBuffer.getSequenceNumber())
									!= 1) {
		// Not part of the continuation, just drop it
		if (debug) {
		    System.err.println(
			"DePacketizer: Sequence out of order, expecting "
					+ (prevSeq+1) + " got "
					+ inputBuffer.getSequenceNumber());
		}
		dropFrame(outputBuffer);
		return OUTPUT_BUFFER_NOT_FILLED;
	    }
	    prevSeq = inputBuffer.getSequenceNumber();

	    // copy to output buffer
	    if (!copyBuffer(inData, inOffset, inLength,
				outputBuffer, frameBegin + frameOffset)) {
		// result in buffer overflow, just drop the frame
		if (debug) {
		    System.err.println(
			"DePacketizer: Buffer overflow on continuation");
		}
		dropFrame(outputBuffer);
		return OUTPUT_BUFFER_NOT_FILLED;
	    }

	    frameOffset += inLength;

	    // <=== is this all of it?
	    if (frameOffset < frameSize) {
		return OUTPUT_BUFFER_NOT_FILLED;
	    }
	    frameContinued = false;
	    frameOffset = 0;
	    if (mpaHeader.layer == 3 && frameBegin == 0) {
		// need at least 2 frames of MP3
		return OUTPUT_BUFFER_NOT_FILLED;
	    }
	    outputBuffer.setTimeStamp(bufTimeStamp);
	    outputBuffer.setFlags(outputBuffer.getFlags()
							| Buffer.FLAG_NO_DROP);
	    bufferContinued = false;
	    return BUFFER_PROCESSED_OK;
	} else {
	    if (frameContinued) {
		if (debug) {
		    System.err.println(
				"DePacketizer: expected continuation missing "
					+ inputBuffer.getSequenceNumber());
		}
		dropFrame(outputBuffer);
	    }

	    frameContinued = false;
	    if (debug &&
		getSequenceDiff(prevSeq, inputBuffer.getSequenceNumber())
									!= 1) {
		System.err.println(
			"DePacketizer: Sequence number mismatch, expecting "
					+ (prevSeq + 1) + " got "
					+ inputBuffer.getSequenceNumber());
	    }
	    prevSeq = inputBuffer.getSequenceNumber();
	    frameTimeStamp = inputBuffer.getTimeStamp();
	    int rc = mpaParse.getHeader(mpaHeader, inData, inOffset, inLength);
	    if (rc != mpaParse.MPA_OK && rc != mpaParse.MPA_HDR_DOUBTED) {
		return BUFFER_PROCESSED_FAILED;
	    }

	    encoding = (mpaHeader.layer == 3) ? AudioFormat.MPEGLAYER3
							: AudioFormat.MPEG;
	    AudioFormat af = (AudioFormat)outputFormat;
	    if (af == null || !(encoding.equalsIgnoreCase(af.getEncoding()))
				|| af.getSampleRate() != mpaHeader.samplingRate
				|| af.getChannels() != mpaHeader.nChannels) {
		// Change in format occurred
		outputFormat = new AudioFormat(encoding,
						mpaHeader.samplingRate, 16,
						mpaHeader.nChannels,
						AudioFormat.BIG_ENDIAN,
						AudioFormat.SIGNED);
	    }
	    frameSize = mpaHeader.bitsInFrame >> 3;
	    if (frameSize > inLength) {
		if (!bufferContinued) {
		    outputBuffer.setLength(0);
		    outputBuffer.setOffset(0);
		    bufTimeStamp = frameTimeStamp;
		    outputBuffer.setFormat(outputFormat);
		    outputBuffer.setSequenceNumber(outSeq++);
		}
		bufferContinued = true;
		frameContinued = true;
		frameBegin = outputBuffer.getLength();
		frameOffset = inLength;
		// copy to output buffer
		copyBuffer(inData, inOffset, inLength,
					outputBuffer, outputBuffer.getLength());
		return OUTPUT_BUFFER_NOT_FILLED;
	    }
	}
	if (mpaHeader.layer == 3 && inLength < ((frameSize * 2) - 2)) {
	    // Buffer up more than just one frame's worth of MP3
	    if (!bufferContinued) {
		outputBuffer.setLength(0);
		outputBuffer.setOffset(0);
		bufTimeStamp = frameTimeStamp;
		byte[] outData = (byte[])outputBuffer.getData();
		if (outData == null || outData.length < OUT_BUF_SIZE) {
		    outData = new byte[OUT_BUF_SIZE];
		    outputBuffer.setData(outData);
		}
	    }
	    // copy to output buffer
	    if (!copyBuffer(inData, inOffset, inLength,
				outputBuffer, outputBuffer.getLength())) {
		// result in buffer overflow, send what's already in buffer
		outputBuffer.setFormat(outputFormat);
		outputBuffer.setSequenceNumber(outSeq++);
		outputBuffer.setTimeStamp(bufTimeStamp);
		outputBuffer.setFlags(outputBuffer.getFlags()
							| Buffer.FLAG_NO_DROP);
		bufferContinued = false;
		return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	    }
	    // see if another frame + padding will fit, if not send
	    // this buffer on.
	    if (outputBuffer.getLength() + frameSize + 4 > OUT_BUF_SIZE) {
		// another packet won't fit, send what's in buffer
		outputBuffer.setFormat(outputFormat);
		outputBuffer.setSequenceNumber(outSeq++);
		outputBuffer.setTimeStamp(bufTimeStamp);
		outputBuffer.setFlags(outputBuffer.getFlags()
							| Buffer.FLAG_NO_DROP);
		bufferContinued = false;
		return BUFFER_PROCESSED_OK;
	    } else {
		bufferContinued = true;
		return OUTPUT_BUFFER_NOT_FILLED;
	    }

	} else {
	    Object outData = outputBuffer.getData();
	    outputBuffer.setData(inputBuffer.getData());
	    inputBuffer.setData(outData);
	    outputBuffer.setLength(inLength);
	    outputBuffer.setFormat(outputFormat);
	    outputBuffer.setOffset(inOffset);
	    outputBuffer.setSequenceNumber(outSeq++);
	    outputBuffer.setTimeStamp(frameTimeStamp);
	    outputBuffer.setFlags(outputBuffer.getFlags()
							| Buffer.FLAG_NO_DROP);
	    return BUFFER_PROCESSED_OK;
	}
    }

    private boolean copyBuffer(byte[] inData, int inOff, int inLen,
					Buffer outputBuffer, int outOff) {
	byte[] outData = (byte[])outputBuffer.getData();
	if (outData == null || outOff + inLen > outData.length) {
	    // will overflow buffer, get a bigger buffer
	    if (outOff + inLen > OUT_BUF_SIZE) {
		// bigger than maximum buffer, now what?
		return false;
	    }
	    byte[] newData = new byte[OUT_BUF_SIZE];
	    if (outOff > 0) {
		System.arraycopy(outData, 0, newData, 0, outData.length);
	    }
	    outData = newData;
	    outputBuffer.setData(outData);
	}
	System.arraycopy(inData, inOff, outData, outOff, inLen);
	outputBuffer.setLength(outputBuffer.getLength() + inLen);

	return true;
    }

    /*
     * the RTP sequence number is unsigned 16 bit counter that
     * wraps around. Allow for the case where it has wrapped.
     * @param p sequence number of the suspected previous packet
     * @param c sequence number of the current (or next) packet
     * @return int difference in sequence numbers
     */
    private int  getSequenceDiff(long p, long c) {
	if (c > p)
	    return (int) (c - p);
	if (c == p)
	    return 0;
	if (p > MAX_SEQ - 100 && c < 100) {
	    // Allow for the case where sequence number has wrapped.
	    return (int) ((MAX_SEQ - p) + c + 1);
	}
	return (int) (c - p);
    }

    private void dropFrame(Buffer outputBuffer) {
	outputBuffer.setLength(frameBegin - outputBuffer.getOffset());
	frameBegin = outputBuffer.getLength() + outputBuffer.getOffset();
	frameSize = 0;
	frameOffset = 0;
	frameContinued = false;
    }

}

