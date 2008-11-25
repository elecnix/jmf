/*
 * @(#)Packetizer.java	1.14  02/08/21 SMI
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

package com.sun.media.codec.video.mpeg;

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;

import java.awt.Dimension;
import java.util.Vector;

import com.sun.media.*;

public class Packetizer extends BasicCodec {

    public static float RATE_TABLE[] = {
			0.0f, 23.976f, 24.f, 25.f, 29.97f,
			30.f, 50.f, 59.94f, 60.f
    };

    private static char[] hexChar = {
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    // following values returned by do<segment> routines
    protected static int SEGMENT_DONE = 1;
    protected static int SEGMENT_REPEAT = 2;
    protected static int SEGMENT_DONE_BUFFER_FULL = 3;

    // maximum packet length
    protected static int PACKET_MAX = 1456;

    private static boolean debug = false;

    private VideoFormat inputFormat = null;
    private VideoFormat outputFormat = null;

    private boolean inputEOM = false;
    private boolean expectingNewInput = true;
    private boolean expectingNewOutput = true;
    private boolean resetTime = true;
    private boolean resetInProgress = true;

    // all output buffers -- used for a slice that spans packets
    // type: Buffer
    private Vector outputQueue = new Vector();

    // all input buffers -- used for a segment that spans buffers
    // should never exceed 2 buffers
    private Vector inputQueue = new Vector();

    // known MPEG segments
    // type: MPEGSegments
    private Vector segmentQueue = new Vector();

    // holds last sequence header encountered
    private byte[] sequenceHeader = null;

    private int frameWidth = 0;
    private int frameHeight = 0;
    private double frameRate = 0.0;

    // nanoseconds per picture (frame) based on frameRate
    private long picNanos = 0;

    // time in nanoseconds from last GOP
    private long gopTime = 0;

    // time in nanoseconds from last position time
    private long startTime = 1;

    // time in nanoseconds from last picture header
    //  == gopTime + (picture count * picNanos)
    private long frameTime = 0;
    private long frameCount = 0;

    // sequence number for next packet
    private int sequenceNumber = 0;

    // RTP MPEG header without N, S, B, E set
    private byte[] mpegHeader = { 0, 0, 0, 0 };

    
    // Initialize default formats.
    public Packetizer() {
	inputFormats = new Format[] { new VideoFormat(VideoFormat.MPEG) };
	outputFormats = new Format[] { new VideoFormat(VideoFormat.MPEG_RTP) };
    }
    
    protected Format getInputFormat() {
	return inputFormat;
    }

    protected Format getOutputFormat() {
	return outputFormat;
    }

    // Return supported output formats
    public Format [] getSupportedOutputFormats(Format in) {
	if (in == null)
	    return outputFormats;

	// Make sure the input is MPEG video format
	if (matches(in, inputFormats) == null)
	    return new Format[0];
        
	Format out [] = new Format[1];
	out[0] = makeMPEGFormat(in);
	return out;
    }

    public Format setInputFormat(Format input) {
	inputFormat = (VideoFormat) input;
	return input;
    }

    public Format setOutputFormat(Format output) {
	if (!(output instanceof VideoFormat)) return null;
	outputFormat = makeMPEGFormat(output);
	return output;
    }

    private final VideoFormat makeMPEGFormat(Format in) {
	VideoFormat vf = (VideoFormat)in;
	return new VideoFormat(VideoFormat.MPEG_RTP,
			vf.getSize(),
			VideoFormat.NOT_SPECIFIED,
			Format.byteArray,
			vf.getFrameRate());
    }
    
    public void open() throws ResourceUnavailableException {
	if (inputFormat == null || outputFormat == null)
	    throw new ResourceUnavailableException(
				"Incorrect formats set on MPEG converter");
	startTime = 1;		// to get past RTPSinkStream
	frameRate = 0.0;
	picNanos = 0;
	sequenceNumber = 0;
	resetTime = true;
    }

    public synchronized void close() {
	reset();
    }

    public void reset() {
	// Anything to do?
	super.reset();
	outputQueue.removeAllElements();
	inputQueue.removeAllElements();
	segmentQueue.removeAllElements();
	inputEOM = false;
	expectingNewInput = true;
	expectingNewOutput = true;
	resetInProgress = true;
	resetTime = true;
	sequenceHeader = null;
	frameWidth = 0;
	frameHeight = 0;
	mpegHeader[0] = 0;
	mpegHeader[1] = 0;
	mpegHeader[2] = 0;
	mpegHeader[3] = 0;
	gopTime = 1;		// to get past RTPSinkStream
	frameTime = 0;
	frameCount = 0;
	if (debug) {
	    System.err.println("Packetizer(V): reset completed");
	}

    }
    
    public synchronized int process(Buffer inBuffer, Buffer outBuffer) {
	if (outputQueue.size() > 0) {
	    Buffer qbuf = (Buffer) outputQueue.firstElement();
	    outputQueue.removeElementAt(0);

	    outBuffer.setData((byte[]) qbuf.getData());
	    outBuffer.setOffset(qbuf.getOffset());
	    outBuffer.setLength(qbuf.getLength());
	    outBuffer.setFlags(qbuf.getFlags());
	    outBuffer.setTimeStamp(qbuf.getTimeStamp());
	    outBuffer.setSequenceNumber(sequenceNumber++);
	    outBuffer.setFormat(outputFormat);
	    expectingNewOutput = true;
	    return INPUT_BUFFER_NOT_CONSUMED;
	}
	if (isEOM(inBuffer)) {
	    inputEOM = true;
	    if (segmentQueue.isEmpty()) {
		propagateEOM(outBuffer);
		outBuffer.setSequenceNumber(sequenceNumber++);
		return BUFFER_PROCESSED_OK;
	    }
	}
	if (inBuffer.isDiscard()) {
	    updateOutput(outBuffer, outputFormat, 0, 0);
	    outBuffer.setDiscard(true);
	    return OUTPUT_BUFFER_NOT_FILLED;
	}

	int retVal = BUFFER_PROCESSED_FAILED;
	try {
	    retVal = doProcess(inBuffer, outBuffer);
	} catch (Exception ex) {
	    ex.printStackTrace();
	    return BUFFER_PROCESSED_FAILED;
	}

	// get the outputFormat from the buffer only if this is the first time.
	if (outputFormat == null) {
	    outputFormat = makeMPEGFormat(inBuffer.getFormat());
	}

	if (retVal != OUTPUT_BUFFER_NOT_FILLED) {
	    outBuffer.setSequenceNumber(sequenceNumber++);
	}
	return retVal;

    }

    public String getName() {
	return "MPEG Video Packetizer";
    }

    public void finalize() {
	close();
    }
 
    private int doProcess(Buffer inBuffer, Buffer outBuffer) {
	if (expectingNewInput) {
	    if (!inputEOM) {
		if (inBuffer.getData() == null) {
		    return OUTPUT_BUFFER_NOT_FILLED;
		}
		if (resetTime) {
		    // get the new position time from a setStartTime
		    startTime = inBuffer.getTimeStamp();
		    if (debug) {
			System.err.println("Packetizer(V): new synctime set: "
						+ startTime);
		    }
		    if (startTime == 0)
			startTime = 1;	// to get past RTPSinkStream
		    resetTime = false;
		}
		inputQueue.addElement(copyInto(inBuffer));
	    }
	    expectingNewInput = false;
	}

	if (expectingNewOutput) {
	    byte[] outData = (byte[]) outBuffer.getData();
	    if (outData == null || outData.length < PACKET_MAX) {
		outData = new byte[PACKET_MAX];
		outBuffer.setData(outData);
	    }
	    System.arraycopy(mpegHeader, 0, outData, 0, 4);
	    outBuffer.setOffset(0);
	    outBuffer.setLength(4);
	    outBuffer.setFlags(0);
	    outBuffer.setHeader(null);
	    outBuffer.setFormat(outputFormat);
	    expectingNewOutput = false;
	}

	if (segmentQueue.isEmpty()) {
	    findFirstStartCode();
	    if (segmentQueue.isEmpty()) {
		expectingNewInput = true;
		return OUTPUT_BUFFER_NOT_FILLED;
	    }
	}
	MPEGSegment mseg = (MPEGSegment) segmentQueue.firstElement();
	while (mseg != null) {
	    if (mseg.getLength() < 0) {
		// need a new input buffer to calculate length for this segment
		expectingNewInput = true;
		return OUTPUT_BUFFER_NOT_FILLED;
	    }
	    int startCode = mseg.startCode;
	    int res = 0;
	    if (startCode == 0xb3) {		// sequence header
		res = doSequenceHeader(mseg, outBuffer);
	    } else if (startCode == 0xb7) {		// sequence end
		res = doSequenceEnd(mseg, outBuffer);
	    } else if (startCode == 0xb8) {		// GOP
		res = doGOP(mseg, outBuffer);
	    } else if (startCode == 0) {		// picture header
		res = doPicture(mseg, outBuffer);
	    } else if (startCode >= 1 && startCode <= 0xaf) {	// slice
		res = doSlice(mseg, outBuffer);
	    } else {
		// unknown start code, simply skip it
		res = SEGMENT_DONE;
	    }
	    if (res == SEGMENT_DONE) {
		segmentQueue.removeElementAt(0);
		if (segmentQueue.isEmpty()) {
		    expectingNewInput = true;
		    if (outBuffer.getLength() > 4) {
			return BUFFER_PROCESSED_OK;
		    } else {
			return OUTPUT_BUFFER_NOT_FILLED;
		    }
		}
		mseg = (MPEGSegment) segmentQueue.firstElement();
		continue;
	    }
	    if (res == SEGMENT_DONE_BUFFER_FULL) {
		segmentQueue.removeElementAt(0);
		// output buffer has data to be sent
		outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_NO_DROP);
		if (expectingNewInput) {
		    return BUFFER_PROCESSED_OK;
		}
		return INPUT_BUFFER_NOT_CONSUMED;
	    }
	    if (res == SEGMENT_REPEAT) {
		// output buffer has data to be sent
		outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_NO_DROP);
		if (expectingNewInput) {
		    return BUFFER_PROCESSED_OK;
		}
		return INPUT_BUFFER_NOT_CONSUMED;
	    }
	}
	return BUFFER_PROCESSED_FAILED;
    }
 
    private Buffer copyInto(Buffer src) {
	Buffer dest = new Buffer();
	dest.copy(src);
	dest.setFlags(dest.getFlags() | Buffer.FLAG_NO_DROP);
	src.setData(null);
	src.setHeader(null);
	src.setLength(0);
	src.setOffset(0);
	return dest;
    }

    protected  String toHex (byte[] inData, int inOffset) {
	String hex = new String();
	for (int i = 0; i < 4; i++) {
	    hex += hexChar[(inData[inOffset + i] >> 4) & 0x0f];
	    hex += hexChar[inData[inOffset + i] & 0x0f];
	}
	return hex;
    }

    private int doSequenceHeader(MPEGSegment sh, Buffer outBuffer) {
	sequenceHeader = new byte[sh.getLength()];
	sh.copyData(sequenceHeader, 0);
	frameWidth = (sequenceHeader[4] & 0xff) << 4
					| (sequenceHeader[5] & 0xf0) >> 4;
	frameHeight = (sequenceHeader[5] & 0x0f) << 8
					| (sequenceHeader[6] & 0xff);
	int frix = (sequenceHeader[7] & 0x0f);
	if (frix > 0 && frix <= 8)
	    frameRate = RATE_TABLE[frix];
	picNanos = (long)((1000 * 1000000) / frameRate);
//	int bitrate = (sequenceHeader[8] & 0xff) << 10
//					| (sequenceHeader[9] & 0xff) << 2
//					| (sequenceHeader[6] & 0xc0) >> 6;
	return SEGMENT_DONE;
    }

    private int copySequenceHeader(Buffer outBuffer) {
	if (sequenceHeader == null)
	    return 0;
	System.arraycopy(sequenceHeader, 0,
					outBuffer.getData(),
					outBuffer.getLength(),
					sequenceHeader.length);
	outBuffer.setLength(outBuffer.getLength() + sequenceHeader.length);
	return sequenceHeader.length;
    }

    private int doSequenceEnd(MPEGSegment se, Buffer outBuffer) {
//	se.copyData((byte[]) outBuffer.getData(), outBuffer.getLength());
//	outBuffer.setLength(outBuffer.getLength() + se.getLength());
	return SEGMENT_DONE;
    }

    private int doGOP(MPEGSegment gop, Buffer outBuffer) {
//	byte[] gb = new byte[4];
//	gop.copyData(4, 4, gb, 0);
//	long gtm = ((gb[0] & 0x7c) >> 2) * 60 * 60 * 1000 * 1000000;
//	gtm += ((gb[0] & 0x03) << 4 | (gb[1] & 0xf0) >> 4) * 60 * 1000
//								* 1000000;
//	gtm += ((gb[1] & 0x07) << 3 | (gb[2] & 0xe0) >> 5) * 1000
//								* 1000000;
//	long gpic = (gb[2] & 0x1f) << 1 | (gb[3] & 0x80) >> 7;
//	if (gpic != 0 || gtm != 0) {
//	    gopTime = gtm + (gpic * picNanos);
//	} else if (frameCount == 0) {
//	    gopTime = 1;		// to get past RTPSinkStream
//	}

	if (frameCount == 0) {
	    gopTime = 1 + startTime;		// to get past RTPSinkStream
	} else {
	    gopTime = frameCount * picNanos + startTime;
	}

	// put a sequence header before each GOP to allow RTP clients
	// to connect beginning with any GOP
	copySequenceHeader(outBuffer);
	gop.copyData((byte[]) outBuffer.getData(), outBuffer.getLength());
	outBuffer.setLength(outBuffer.getLength() + gop.getLength());
	return SEGMENT_DONE;
    }

    private int doPicture(MPEGSegment ph, Buffer outBuffer) {
	byte[] pic = new byte[ph.getLength()];
	ph.copyData(pic, 0);
	int cnt = (pic[4] & 0xff) << 2 | (pic[5] & 0xc0) >> 6;
	int type = (pic[5] & 0x38) >> 3;
	mpegHeader[0] = (byte) ((cnt >> 8) & 0x02);
	mpegHeader[1] = (byte) cnt;
	mpegHeader[2] = (byte) type;		// ignore N, S, B, E for now
	if (type == 1) {
	    mpegHeader[3] = 0;
	} else {
	    int next = (pic[7] & 0x07) << 1 | (pic[8] & 0x80) >> 7;
	    if (type > 2)
		next |= (pic[8] & 0x78) << 1;
	    mpegHeader[3] = (byte) next;
	}
	resetInProgress = false;
	byte[] outData = (byte[]) outBuffer.getData();
	System.arraycopy(mpegHeader, 0, outData, 0, 4);

	// by the time a picture is encountered,
	// sequence header & GOP should already be copied into buffer
	if (outBuffer.getLength() > 8
					&& outData[4] == 0
					&& outData[5] == 0
					&& outData[6] == 1
					&& (outData[7] & 0xff) == 0xb3) {
	    outData[2] |= 0x20;		// set S (section header present)
	}

	ph.copyData((byte[]) outBuffer.getData(), outBuffer.getLength());
	outBuffer.setLength(outBuffer.getLength() + ph.getLength());
	outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_KEY_FRAME);
	frameCount++;
	frameTime = gopTime + (cnt * picNanos);
	outBuffer.setTimeStamp(frameTime);
	outBuffer.setFormat(outputFormat);
	return SEGMENT_DONE;
    }

    private int doSlice(MPEGSegment slice, Buffer outBuffer) {
	byte[] outData = (byte[]) outBuffer.getData();
	if (slice.getLength() < PACKET_MAX - outBuffer.getLength()) {
	    slice.copyData(outData, outBuffer.getLength());
	    outBuffer.setLength(outBuffer.getLength() + slice.getLength());
	    outBuffer.setTimeStamp(frameTime);
	    outBuffer.setFormat(outputFormat);
	    outData[2] |= 0x18;		// set B, E (begin, end slice)
	    if (segmentQueue.size() > 1) {
		MPEGSegment mse = (MPEGSegment) segmentQueue.elementAt(1);
		if (mse.startCode < 1 || mse.startCode > 0xaf) {
		    outBuffer.setFlags(outBuffer.getFlags()
						| Buffer.FLAG_RTP_MARKER);
		    expectingNewOutput = true;
		    return SEGMENT_DONE_BUFFER_FULL;
		}
	    } else if (inputEOM) {
		outBuffer.setFlags(outBuffer.getFlags()
						| Buffer.FLAG_RTP_MARKER);
		expectingNewOutput = true;
		return SEGMENT_DONE_BUFFER_FULL;
	    }
	    return SEGMENT_DONE;
	}
	if ((outData[2] & 0x18) != 0) {
	    // there are slices in the buffer but this one won't fit
	    // send the buffer as is then come back to this slice
	    expectingNewOutput = true;
	    return SEGMENT_REPEAT;
	}
	int len = PACKET_MAX - outBuffer.getLength();
	slice.copyData(0, len, outData, outBuffer.getLength());
	outBuffer.setLength(outBuffer.getLength() + len);
	outBuffer.setTimeStamp(frameTime);
	outBuffer.setFormat(outputFormat);
	outData[2] |= 0x10;		// set B (begin slice)
	int off = len;
	len = slice.getLength() - len;

	Buffer b = null;
	// Now queue up additional output buffers to complete the slice
	while (len > 0) {
	    b = new Buffer();
	    outData = new byte[PACKET_MAX];
	    b.setData(outData);
	    b.setTimeStamp(frameTime);
	    b.setHeader(null);
	    b.setFormat(outputFormat);
	    b.setFlags(outBuffer.getFlags());
	    b.setOffset(0);
	    System.arraycopy(mpegHeader, 0, outData, 0, 4);
	    int l = len;
	    if (len > PACKET_MAX - 4)
		l = PACKET_MAX - 4;
	    slice.copyData(off, l, (byte[]) b.getData(), 4);
	    b.setLength(l + 4);
	    off += l;
	    len -= l;
	    if (len <= 0)
		outData[2] |= 0x08;		// set E (end slice)
	    outputQueue.addElement(b);
	}
	if (segmentQueue.size() > 1) {
	    MPEGSegment mse = (MPEGSegment) segmentQueue.elementAt(1);
	    if (mse.startCode < 1 || mse.startCode > 0xaf) {
		b.setFlags(b.getFlags() | Buffer.FLAG_RTP_MARKER);
		expectingNewOutput = true;
		return SEGMENT_DONE_BUFFER_FULL;
	    }
	} else if (inputEOM) {
	    b.setFlags(b.getFlags() | Buffer.FLAG_RTP_MARKER);
	    expectingNewOutput = true;
	    return SEGMENT_DONE_BUFFER_FULL;
	}
	expectingNewOutput = true;
	return SEGMENT_DONE_BUFFER_FULL;

    }

    private void findFirstStartCode() {
	if (inputQueue.isEmpty())
	    return;
	Buffer inBuffer = (Buffer) inputQueue.firstElement();
	// now the buffer is in the pipe, drop it from inputQueue
	inputQueue.removeElementAt(0);
	byte[] inData = (byte[]) inBuffer.getData();
	int off = inBuffer.getOffset();
	int len = inBuffer.getLength();
	while (len > 4) {
	    if (inData[off] == 0 && inData[off+1] == 0 && inData[off+2] == 1) {
		// treat extension and user_data as part of current header
		if ((inData[off+3] & 0xff) != 0xb5
					&& (inData[off+3] & 0xff) != 0xb2) {
		    if (resetInProgress) {
			// after reset, need a sequence_header_code or GOP
			if ((inData[off+3] & 0xff) == 0xb3
					|| (inData[off+3] & 0xff) == 0xb8) {
			    MPEGSegment ns =
					new MPEGSegment((inData[off+3] & 0xff),
							off, inBuffer);
			    segmentQueue.addElement(ns);
			    return;
			}
		    } else {
			MPEGSegment ns = new MPEGSegment((inData[off+3] & 0xff),
							off, inBuffer);
			segmentQueue.addElement(ns);
			return;
		    }
		}
	    }
	    off++;
	    len--;
	}
	// didn't find anything, try the next buffer
	expectingNewInput = true;
    }

    /*
     * If length == -1, the end of segment hasn't been determined.
     * offset is absolute in buffer (no need to add buffer's offset).
     * The most common case is a segment is contained in a single
     * buffer so endBuffer is null.
     * Assumption: a segment will never span more than two buffers.
     */
    class MPEGSegment {
	int startCode = -1;
	int offset = -1;	// offset in startBuffer for the start code
	int length = -1;
	Buffer startBuffer = null;
	Buffer endBuffer = null;

	MPEGSegment(int code, int off, Buffer buf) {
	    startCode = code;
	    offset = off;
	    startBuffer = buf;
	}

	// off is relative to start of segment
	void copyData(byte[] dest, int outoffset) {
	    copyData(0, length, dest, outoffset);
	}

	// off is relative to start of segment
	void copyData(int off, byte[] dest, int outoffset) {
	    copyData(off, length - off, dest, outoffset);
	}

	// off is relative to start of segment
	void copyData(int off, int len, byte[] dest, int outoffset) {
	    if (off + len > length) {
		len = length - off;
	    }
	    if (endBuffer == null) {
		// completely contained in single buffer
		System.arraycopy(startBuffer.getData(), offset + off,
					dest, outoffset, len);
		return;
	    }
	    // len1 = portion of segment in startBuffer
	    // len2 = portion of segment in endBuffer
	    int len1 = startBuffer.getLength()
					- (offset - startBuffer.getOffset());
	    int len2 = length - len1;
	    if (off + len <= len1) {
		// still only copying from first buffer
		System.arraycopy(startBuffer.getData(), offset + off,
					dest, outoffset, len);
		return;
	    }
	    if (off >= len1) {
		// only copying from second buffer
		off -= len1;
		System.arraycopy(endBuffer.getData(),
					endBuffer.getOffset() + off,
					dest, outoffset, len);
		return;
	    }
	    // worst case, part of first buffer plus part of second buffer
	    int l = len1 - off;
	    System.arraycopy(startBuffer.getData(), offset + off,
					dest, outoffset, l);
	    len -= l;	// remaining length to copy from second buffer
	    System.arraycopy(endBuffer.getData(), endBuffer.getOffset(),
					dest, outoffset + l, len);
	}

	int getLength() {
	    if (length < 0)
		calculateLength();
	    return length;
	}

	private void calculateLength() {
	    if (length > 0)
		return;
	    int off = findNextStart();	// try in same buffer
	    if (off > offset) {
		length = off - offset;
		return;
	    }
	    if (inputEOM) {
		// at EOM and no more start codes, consume remainder of buffer
		length = startBuffer.getLength()
					- (offset - startBuffer.getOffset());
		return;
	    }
	    if (endBuffer == null) {
		if (inputQueue.isEmpty())
		    return;			// need to get another buffer
		endBuffer = (Buffer) inputQueue.firstElement();
		inputQueue.removeElementAt(0);
	    }
	    // handle case where start code straddles buffers
	    off = findNextStartBetweenBuffers();
	    if (off > offset) {
		length = off - offset;
		return;
	    }
	    off = findNextStartInEndBuffer();
	    length = startBuffer.getLength()
					- (offset - startBuffer.getOffset());
	    length += off - endBuffer.getOffset();
	}

	private int findNextStart() {
	    byte[] inData = (byte[]) startBuffer.getData();
	    int off = offset + 4;
	    int len = startBuffer.getLength()
				- ((offset + 4) - startBuffer.getOffset());
	    while (len > 3) {
		if (inData[off] == 0
						&& inData[off+1] == 0
						&& inData[off+2] == 1) {
		    // treat extension and user_data as part of current header
		    if ((inData[off+3] & 0xff) != 0xb5
					&& (inData[off+3] & 0xff) != 0xb2) {
			MPEGSegment ns = new MPEGSegment((inData[off+3] & 0xff),
							off, startBuffer);
			segmentQueue.addElement(ns);
			return off;
		    }
		}
		off++;
		len--;
	    }
	    return -1;
	}

	// ugly brute force check for start code beginning in last 3 bytes
	// of startBuffer
	private int findNextStartBetweenBuffers() {
	    byte[] inData = (byte[]) startBuffer.getData();
	    byte[] inData2 = (byte[]) endBuffer.getData();
	    int off = startBuffer.getOffset() + startBuffer.getLength() - 3;
	    if (off <= offset)
		return -1;	// already spanning buffer, look beyond here
	    int off2 = endBuffer.getOffset();
	    if (inData[off] == 0 && inData[off+1] == 0 && inData[off+2] == 1) {
		// treat extension and user_data as part of current header
		if ((inData2[off2] & 0xff) != 0xb5
					&& (inData[off2] & 0xff) != 0xb2) {
		    MPEGSegment ns = new MPEGSegment((inData2[off2] & 0xff),
							off, startBuffer);
		    ns.endBuffer = endBuffer;
		    segmentQueue.addElement(ns);
		    endBuffer = null;	// not needed for this segment
		    return off;
		}
	    }
	    if (inData[off+1] == 0 && inData[off+2] == 0
						&& inData2[off2] == 1) {
		// treat extension and user_data as part of current header
		if ((inData2[off2+1] & 0xff) != 0xb5
					&& (inData[off2+1] & 0xff) != 0xb2) {
		    MPEGSegment ns = new MPEGSegment((inData2[off2+1] & 0xff),
							off+1, startBuffer);
		    ns.endBuffer = endBuffer;
		    segmentQueue.addElement(ns);
		    endBuffer = null;	// not needed for this segment
		    return off+1;
		}
	    }
	    if (inData[off+2] == 0 && inData2[off2] == 0
						&& inData2[off2+1] == 1) {
		// treat extension and user_data as part of current header
		if ((inData2[off2+2] & 0xff) != 0xb5
					&& (inData[off2+2] & 0xff) != 0xb2) {
		    MPEGSegment ns = new MPEGSegment((inData2[off2+2] & 0xff),
							off+2, startBuffer);
		    ns.endBuffer = endBuffer;
		    segmentQueue.addElement(ns);
		    endBuffer = null;	// not needed for this segment
		    return off+2;
		}
	    }
	    return -1;
	}

	private int findNextStartInEndBuffer() {
	    byte[] inData = (byte[]) endBuffer.getData();
	    int off = endBuffer.getOffset();
	    int len = endBuffer.getLength();
	    while (len > 3) {
		if (inData[off] == 0
						&& inData[off+1] == 0
						&& inData[off+2] == 1) {
		    // treat extension and user_data as part of current header
		    if ((inData[off+3] & 0xff) != 0xb5
					&& (inData[off+3] & 0xff) != 0xb2) {
			MPEGSegment ns = new MPEGSegment((inData[off+3] & 0xff),
							off, endBuffer);
			segmentQueue.addElement(ns);
			return off;
		    }
		}
		off++;
		len--;
	    }
	    return -1;
	}

    }


}
