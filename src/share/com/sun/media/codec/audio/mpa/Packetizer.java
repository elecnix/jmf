/*
 * @(#)Packetizer.java	1.14	02/08/21 SMI
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
import com.sun.media.controls.*;

/**
 * Implements a MPEG Packetizer.
 */
public class Packetizer extends com.sun.media.codec.audio.AudioCodec {

    /**
     * State:
     *		expect input buffer to start with a new frame
     *		start a new buffer
     */
    private static final int NEW_FRAME = 0;	// start new frame, new buffer

    /**
     * State:
     *		input buffer starts with continuation of previous frame
     *		add to existing buffer (buffer not full)
     */
    private static final int CONT_FRAME = 1;	// continue frame, same buffer

    /**
     * State:
     *		input buffer starts with continuation of previous frame
     *		start a new buffer (use header offset)
     */
    private static final int CONT_BUFFER = 2;	// continue frame, new buffer

    /**
     * State:
     *		expect input buffer to start with a new frame
     *		add to existing buffer (buffer not full)
     */
    private static final int FILL_BUFFER = 3;	// new frame, same buffer

    private int state = NEW_FRAME;


    /**
     * Maximum MPA framesize is based on one frame of MPEG1 Layer 2
     * at 32000 Hz with a bitrate of 384000 + padding.
     * (Will have to change with MPEG 2.5 support.)
     */
    public static final int MAX_MPA_FRAMESIZE = 1729;	// frame size in bytes


    /**
     * Maximum framesize is based on IP packet size for most
     * ethernet networks.
     * Minimum framesize is based on one frame of MPEG1 Layer 2
     * at 44100 Hz with a bitrate of 32000 + padding + RTP header.
     */
    public static final int MAX_FRAMESIZE = 1456;	// frame size in bytes
    public static final int MIN_FRAMESIZE = 110;	// frame size in bytes
    public static final int DEFAULT_FRAMESIZE = MAX_FRAMESIZE;

    /*
     * Generate debug messages
     */
    private static final boolean debug = false;

    private byte[] pendingData = new byte[32 * 1024 * 4];
    private int pendingDataSize = 0;
    private int pendingDataOffset = 0;
    private boolean expectingSameInputBuffer = false;
    private boolean inputEOM = false;
    private boolean setMark = true;
    private boolean resetTime = true;

    private int frameSize = 0;
    private int frameOffset = 0;
    private long frameCount = 0;

    private int packetSize = 0;
    private long packetSeq = 0;
    private long currentTime = 1;	// to get past RTPSinkStream
    private long deltaTime = 0;

    private MPAHeader mpaHeader = null;
    private MPAParse mpaParse = new MPAParse();


    public Packetizer () {
	packetSize = DEFAULT_FRAMESIZE;
	inputFormats = new AudioFormat[] {
			    new AudioFormat(AudioFormat.MPEGLAYER3,
					    16000., 
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEGLAYER3, 
					    22050.,
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEGLAYER3, 
					    24000.,
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEGLAYER3, 
					    32000.,
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEGLAYER3, 
					    44100.,
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEGLAYER3, 
					    48000.,
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEG,
					    16000., 
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEG,
					    22050., 
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEG,
					    24000., 
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEG,
					    32000., 
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEG,
					    44100., 
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED),
			    new AudioFormat(AudioFormat.MPEG,
					    48000., 
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED, 
					    Format.NOT_SPECIFIED,    // endian
					    AudioFormat.SIGNED), 
	};
	outputFormats  = new AudioFormat[] {
			    new AudioFormat(AudioFormat.MPEG_RTP,
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED,
					    Format.NOT_SPECIFIED,    // endian
				 	    AudioFormat.SIGNED)
	} ;

    }

    public String getName () {
	return "MPEG Audio Packetizer";
    }

    public  Format[] getSupportedOutputFormats (Format in) {
	if (in == null) {
	    return new Format[] { new AudioFormat(AudioFormat.MPEG_RTP) };
	}

	if (matches(in, inputFormats) == null) {
	    return new Format[1];
	}

	if (! (in instanceof AudioFormat) ) {
	    return new Format[] { new AudioFormat(AudioFormat.MPEG_RTP) };
	}

	return getMatchingOutputFormats(in);

    }

    protected  Format[] getMatchingOutputFormats (Format in) {

	AudioFormat af =(AudioFormat) in;

	outputFormats = new AudioFormat[] {
			    new AudioFormat(AudioFormat.MPEG_RTP,
					    af.getSampleRate(),
					    af.getSampleSizeInBits(),
				 	    af.getChannels(),
					    af.getEndian(),
				 	    AudioFormat.SIGNED,
					    af.getFrameSizeInBits(),
					    af.getFrameRate(),
					    Format.byteArray)
	};
	return  outputFormats;
    }

    public void open () throws ResourceUnavailableException {
	setPacketSize(packetSize);
	reset();
	currentTime = 1;
	packetSeq = 0;
	resetTime = true;
    }

    public synchronized void reset () {
	super.reset();
	mpaParse.reset();
	resetPendingData();
	state = NEW_FRAME;
	setMark = true;
	expectingSameInputBuffer = false;
	frameSize = 0;
	frameOffset = 0;
	frameCount = 0;
	resetTime = true;
	deltaTime = 0;
	if (debug) {
	    System.err.println("Packetizer(A): reset completed");
	}
    }

    public void close () {

    }

    public synchronized int process (Buffer inputBuffer, Buffer outputBuffer) {
	if (inputBuffer.isDiscard()) {
	    updateOutput(outputBuffer, outputFormat, 0, 0);
	    outputBuffer.setDiscard(true);
	    return BUFFER_PROCESSED_OK;
	}
	try {
	    int rc = doProcess(inputBuffer, outputBuffer);
	    if (rc != OUTPUT_BUFFER_NOT_FILLED) {
		outputBuffer.setSequenceNumber(packetSeq++);
		outputBuffer.setTimeStamp(currentTime);
	    }
	    if (inputEOM) {
		if (outputBuffer.getLength() == 0) {
		    propagateEOM(outputBuffer);
		    outputBuffer.setSequenceNumber(packetSeq++);
		    mpaParse.reset();
		    resetPendingData();
		    state = NEW_FRAME;
		    return BUFFER_PROCESSED_OK;
		} else {
		    if (rc == OUTPUT_BUFFER_NOT_FILLED) {
			outputBuffer.setSequenceNumber(packetSeq++);
			outputBuffer.setTimeStamp(currentTime);
		    }
		    rc = INPUT_BUFFER_NOT_CONSUMED;
		    expectingSameInputBuffer = true;
		}
	    } else if (pendingDataSize <= MAX_MPA_FRAMESIZE + 9) {
		rc &= ~INPUT_BUFFER_NOT_CONSUMED;
		shiftPendingData();
	    }
	    return rc;
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	return BUFFER_PROCESSED_FAILED;
    }

    protected int doProcess (Buffer inputBuffer, Buffer outputBuffer) {

	if (!checkInputBuffer(inputBuffer) ) {
	    return BUFFER_PROCESSED_FAILED;
	}
	
	inputEOM = false;
	if (isEOM(inputBuffer) ) {
	    if (pendingDataSize == 0 ) {
		propagateEOM(outputBuffer);
		mpaParse.reset();
		resetPendingData();
		state = NEW_FRAME;
		return BUFFER_PROCESSED_OK;
	    }
	    inputEOM = true;
	}

	byte[] inData = (byte[])inputBuffer.getData();
	int inOffset = inputBuffer.getOffset();
	int inLength = inputBuffer.getLength();
	if (!expectingSameInputBuffer) {
	    if (inLength > 0 && inData != null) {
		if (resetTime) {
		    // get the new position time from a setStartTime
		    // reset the time on output buffers to match
		    currentTime = inputBuffer.getTimeStamp();
		    if (debug) {
			System.err.println("Packetizer(A): new synctime set: "
						+ currentTime);
		    }
		    if (currentTime == 0)
			currentTime = 1;	// to get past RTPSinkStream
		    resetTime = false;
		}
		if (inLength > pendingData.length - pendingDataSize) {
		    if (debug) {
			System.err.println("Packetizer(A): overflow buffer has "
			+ pendingDataSize + " bytes, trying to add " + inLength
			+ "\nPacketizer(A): flushing data");
		    }
		    mpaParse.reset();
		    resetPendingData();
		}
		System.arraycopy(inData, inOffset, pendingData,
					pendingDataOffset + pendingDataSize,
					inLength);
		pendingDataSize += inLength;
	    } else if (!inputEOM) {
		// ignore empty buffers except at EOM
		return OUTPUT_BUFFER_NOT_FILLED;
	    }
	    expectingSameInputBuffer = true;
	}
	if (mpaHeader == null)
	    mpaHeader = getMPAHeader(pendingData, pendingDataOffset,
							pendingDataSize);
	switch (state) {
	    case NEW_FRAME:			// start new frame, new buffer
		return newFrameNewBuffer (outputBuffer);
	    case CONT_FRAME:			// continue frame, same buffer
		return continueFrameInBuffer (outputBuffer);
	    case CONT_BUFFER:			// continue frame, new buffer
		return continueFrameNewBuffer (outputBuffer);
	    case FILL_BUFFER:			// new frame, same buffer
		return newFrameInBuffer (outputBuffer);
	}
	return BUFFER_PROCESSED_FAILED;
    }

    protected  int continueFrameNewBuffer (Buffer outputBuffer) {
	// this is a continuation of a previous frame in a new buffer

	int copyLen = Math.min(pendingDataSize, frameSize - frameOffset);
	checkMPAHeader(pendingData, pendingDataOffset, pendingDataSize);
	if (mpaHeader != null) {
	    if (mpaHeader.headerOffset == pendingDataOffset) {
		// Actually not a continuation, starting a new frame
		state = NEW_FRAME;
		return newFrameNewBuffer (outputBuffer);
	    }
	}
	copyLen = Math.min(copyLen, packetSize - 4);
	setStartOfBuffer(outputBuffer, frameOffset);
	// copy to output buffer
	if (!copyBuffer(pendingData, pendingDataOffset, copyLen, outputBuffer)){
	    // result in buffer overflow, just drop the frame
	    state = NEW_FRAME;
	    return BUFFER_PROCESSED_FAILED;
	}

	frameOffset += copyLen;
	outputBuffer.setTimeStamp(currentTime);
	outputBuffer.setFormat(outputFormat);

	// <=== is this all of it?
	if (copyLen < pendingDataSize) {
	    pendingDataOffset += copyLen;
	    pendingDataSize -= copyLen;
	    if (frameOffset >= frameSize
				|| (mpaHeader != null
				&& mpaHeader.headerOffset == pendingDataOffset))
		state = NEW_FRAME;
	    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	}

	pendingDataOffset += copyLen;
	pendingDataSize -= copyLen;
	shiftPendingData();
	if (!isOutputBufferFull(outputBuffer)) {
	    state = CONT_FRAME;
	    return OUTPUT_BUFFER_NOT_FILLED;
	}
	return BUFFER_PROCESSED_OK;
    }

    protected  int continueFrameInBuffer (Buffer outputBuffer) {
	checkMPAHeader(pendingData, pendingDataOffset, pendingDataSize);
	int copyLen = Math.min(pendingDataSize, frameSize - frameOffset);
	if (mpaHeader != null) {
	    if (mpaHeader.headerOffset == pendingDataOffset) {
		// Actually not a continuation, starting a new frame
		state = FILL_BUFFER;
		return newFrameInBuffer (outputBuffer);
	    }
	}
	copyLen = Math.min(copyLen, packetSize - outputBuffer.getLength());
	// copy to output buffer
	if (!copyBuffer(pendingData, pendingDataOffset, copyLen, outputBuffer)){
	    // result in buffer overflow, just drop the frame
	    state = NEW_FRAME;
	    return BUFFER_PROCESSED_FAILED;
	}
	frameOffset += copyLen;

	outputBuffer.setTimeStamp(currentTime);
	outputBuffer.setFormat(outputFormat);

	// <=== is this all of it?
	if (copyLen < pendingDataSize) {
	    pendingDataOffset += copyLen;
	    pendingDataSize -= copyLen;
	    if (mpaHeader != null
			&& mpaHeader.headerOffset == pendingDataOffset) {
		// Keep trying to add frames until they won't fit
		state = FILL_BUFFER;
		return newFrameInBuffer (outputBuffer);
	    } else
		state = CONT_BUFFER;
	    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	}
	pendingDataOffset += copyLen;
	pendingDataSize -= copyLen;

	shiftPendingData();
	if (!isOutputBufferFull(outputBuffer)) {
	    state = CONT_FRAME;
	    return OUTPUT_BUFFER_NOT_FILLED;
	}
	return BUFFER_PROCESSED_OK;
    }

    protected  int newFrameNewBuffer (Buffer outputBuffer) {
	if (mpaHeader == null) {
	    // expecting new frame, haven't found one in this buffer
	    shiftPendingData();
	    return OUTPUT_BUFFER_NOT_FILLED;
	}
	if (mpaHeader.headerOffset != pendingDataOffset) {
	    if (debug) {
		System.err.println("Packetizer(A): Offset mismatch, buffer="
					+ pendingDataOffset + " header="
					+ mpaHeader.headerOffset + " seq="
					+ packetSeq + " frame=" + frameCount);
	    }
	    pendingDataSize += pendingDataOffset - mpaHeader.headerOffset;
	    pendingDataOffset = mpaHeader.headerOffset;
	}
	frameSize = mpaHeader.bitsInFrame >> 3;
	String encoding = AudioFormat.MPEG_RTP;
	AudioFormat af = (AudioFormat)outputFormat;
	if (af == null || af.getEncoding() != encoding
				|| af.getSampleRate() != mpaHeader.samplingRate
				|| af.getChannels() != mpaHeader.nChannels) {
	    // Change in format occurred
	    int endian = AudioFormat.BIG_ENDIAN;
	    if (af != null)
		endian = af.getEndian();
	    outputFormat = new AudioFormat(encoding,
						mpaHeader.samplingRate,
						16, mpaHeader.nChannels,
						endian,
						AudioFormat.SIGNED);
	}
	setStartOfBuffer(outputBuffer, 0);
	int copyLen = Math.min(mpaHeader.bitsInFrame >> 3, pendingDataSize);
	copyLen = Math.min(copyLen, packetSize - 4);
	// copy to output buffer
	if (!copyBuffer(pendingData, pendingDataOffset, copyLen, outputBuffer)){
	    // result in buffer overflow, just drop the frame
	    state = NEW_FRAME;
	    return BUFFER_PROCESSED_FAILED;
	}
	frameOffset = copyLen;
	frameCount++;
	currentTime += deltaTime;
	deltaTime = (((long)mpaHeader.nSamples * 1000L * 1000000L)
					/ mpaHeader.samplingRate);
	outputBuffer.setFormat(outputFormat);
	outputBuffer.setTimeStamp(currentTime);

	if (copyLen < pendingDataSize) {
	    pendingDataOffset += copyLen;
	    pendingDataSize -= copyLen;
	    if (copyLen == mpaHeader.bitsInFrame >> 3) {
		// Keep trying to add frames until they won't fit
		state = FILL_BUFFER;
		mpaHeader = getMPAHeader(pendingData, pendingDataOffset,
							pendingDataSize);
		return newFrameInBuffer (outputBuffer);
	    }
	    state = CONT_BUFFER;
	    mpaHeader = null;
	    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	}
	pendingDataOffset += copyLen;
	pendingDataSize -= copyLen;
	if (copyLen == mpaHeader.bitsInFrame >> 3) {
	    // frame size and input buffer length exactly matched
	    shiftPendingData();
	    if (isOutputBufferFull(outputBuffer)) {
		state = NEW_FRAME;
		return BUFFER_PROCESSED_OK;
	    }
	    state = FILL_BUFFER;
	    return OUTPUT_BUFFER_NOT_FILLED;
	}
	if (!isOutputBufferFull(outputBuffer)) {
	    state = CONT_FRAME;
	    shiftPendingData();
	    return OUTPUT_BUFFER_NOT_FILLED;
	}
	state = CONT_BUFFER;
	shiftPendingData();
	return BUFFER_PROCESSED_OK;
    }

    protected  int newFrameInBuffer (Buffer outputBuffer) {
	if (mpaHeader == null) {
	    // expecting new frame, haven't found one in this buffer
	    state = NEW_FRAME;
	    shiftPendingData();
	    return BUFFER_PROCESSED_OK;
	}
	// Make sure there's at least a full buffer and an extra MPA header
	if (pendingDataSize <= MAX_MPA_FRAMESIZE + 9 && !inputEOM) {
	    state = FILL_BUFFER;
	    shiftPendingData();
	    return OUTPUT_BUFFER_NOT_FILLED;
	}
	if (mpaHeader.headerOffset != pendingDataOffset) {
	    if (debug) {
		System.err.println("Packetizer(A): Offset mismatch(2), buffer="
					+ pendingDataOffset + " header="
					+ mpaHeader.headerOffset + " seq="
					+ packetSeq + " frame=" + frameCount);
	    }
	    pendingDataSize += pendingDataOffset - mpaHeader.headerOffset;
	    pendingDataOffset = mpaHeader.headerOffset;
	}
	int copyLen = mpaHeader.bitsInFrame >> 3;
	if (copyLen > pendingDataSize) {
	    // not all of the frame is here, get next input buffer
	    state = FILL_BUFFER;
	    shiftPendingData();
	    return OUTPUT_BUFFER_NOT_FILLED;
	}
	if (copyLen > packetSize - outputBuffer.getLength()) {
	    // not all of the frame will fit, send what's in the buffer
	    state = NEW_FRAME;
	    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	}
	// copy to output buffer
	if (!copyBuffer(pendingData, pendingDataOffset, copyLen, outputBuffer)){
	    // result in buffer overflow, just drop the frame
	    state = NEW_FRAME;
	    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	}
	frameCount++;
	deltaTime += (((long)mpaHeader.nSamples * 1000L * 1000000L)
					/ mpaHeader.samplingRate);

	// Keep trying to add frames until they won't fit
	pendingDataOffset += copyLen;
	pendingDataSize -= copyLen;

	if (pendingDataSize == 0) {
	    // frame size and input buffer length exactly matched
	    state = FILL_BUFFER;
	    shiftPendingData();
	    return OUTPUT_BUFFER_NOT_FILLED;
	}
	state = FILL_BUFFER;
	mpaHeader = getMPAHeader(pendingData, pendingDataOffset,
							pendingDataSize);
	return newFrameInBuffer (outputBuffer);
    }

    protected  void shiftPendingData () {
	// Move the last chunk to the beginning of the pendingData buffer
	if (pendingDataOffset != 0 && pendingDataSize > 0)
	    System.arraycopy(pendingData, pendingDataOffset,
					pendingData, 0, pendingDataSize);
	pendingDataOffset = 0;
	expectingSameInputBuffer = false;
	mpaHeader = null;

    }

    protected  void resetPendingData () {
	pendingDataSize = 0;
	pendingDataOffset = 0;
	expectingSameInputBuffer = false;
	mpaHeader = null;

    }

    /**
     * check the mpaHeader against frameSize and frameOffset
     * to see if it's a bogus header
     */
    protected  void checkMPAHeader (byte[] inData, int inOffset,
								int inLength) {

	int off = inOffset + frameSize - frameOffset;
	if (mpaHeader == null || mpaHeader.headerOffset == off) {
	    return;
	}
	int len = inLength - (frameSize - frameOffset);
	mpaHeader = getMPAHeader(inData, off, len);
    }

    protected  MPAHeader getMPAHeader (byte[] inData, int inOffset,
								int inLength) {

	MPAHeader header = new MPAHeader();
	int rc = mpaParse.getHeader(header, inData, inOffset, inLength);
	if (rc == mpaParse.MPA_OK) {
	    return header;
	}
	if (inputEOM && rc == mpaParse.MPA_HDR_DOUBTED) {
	    return header;
	}
	return null;
    }

    public  java.lang.Object[] getControls () {
	if (controls == null) {
	     controls = new Control[1];
	     controls[0] = new PacketSizeAdapter(this,packetSize, true);
	}
	return (Object[])controls;
    }

    public synchronized void setPacketSize (int newPacketSize) {
	packetSize=newPacketSize;
    }

    private boolean isOutputBufferFull(Buffer outputBuffer) {
	// don't bother if there's only a small amount left
	if (packetSize > outputBuffer.getLength() + 40) {
	    return false;
	}
	return true;
    }

    private void setStartOfBuffer(Buffer outputBuffer, int frameOff) {
	byte[] outData = (byte[])outputBuffer.getData();
	if (outData == null || packetSize > outData.length) {
	    // get a bigger buffer
	    outData = new byte[packetSize];
	    outputBuffer.setData(outData);
	}
	outData[0] = 0;
	outData[1] = 0;
	outData[2] = (byte) (frameOff >> 8);
	outData[3] = (byte) frameOff;
	outputBuffer.setOffset(0);
	outputBuffer.setLength(4);
	if (setMark) {
	    outputBuffer.setFlags(Buffer.FLAG_RTP_MARKER);
	    setMark = false;
	} else {
	    outputBuffer.setFlags(0);
	}
    }

    private boolean copyBuffer(byte[] inData, int inOff, int inLen,
							Buffer outputBuffer) {
	byte[] outData = (byte[])outputBuffer.getData();
	int outOff = outputBuffer.getLength();
	if (outOff + inLen > outData.length) {
	    // will overflow buffer, get a bigger buffer
	    if (outOff + inLen > packetSize) {
		// bigger than permitted packetsize, now what?
		return false;
	    }
	    byte[] newData = new byte[packetSize];
	    if (outOff > 0) {
		System.arraycopy(outData, 0, newData, 0, outOff);
	    }
	    outData = newData;
	    outputBuffer.setData(outData);
	}
	System.arraycopy(inData, inOff, outData, outOff, inLen);
	outputBuffer.setLength(outOff + inLen);

	return true;
    }

}


class PacketSizeAdapter extends com.sun.media.controls.PacketSizeAdapter {

    public PacketSizeAdapter (Codec newOwner, int newPacketSize,
							boolean newIsSetable) {
	super(newOwner,newPacketSize,newIsSetable);
    }

    public int setPacketSize (int numBytes) {

	if (numBytes < Packetizer.MIN_FRAMESIZE) {
	    numBytes = Packetizer.MIN_FRAMESIZE;
	}

	if (numBytes > Packetizer.MAX_FRAMESIZE) {
	    numBytes = Packetizer.MAX_FRAMESIZE;
	}
	packetSize = numBytes;

	((Packetizer)owner).setPacketSize(packetSize);

	return packetSize;
    }

}
