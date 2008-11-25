/*
 * @(#)RTPDePacketizer.java	1.8  02/08/21 SMI
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

import com.sun.media.*;
import java.util.Vector;

// Following only needed to capture frames to output file
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * This codec is a MPEG RTP depacketizer. It receives individual RTP
 * buffers with a MPEG_RTP format. These buffers will be used to
 * reconstruct a complete frame in the MPEG format. Once a frame is
 * constructed, it is sent over to the next node i.e. a node capable of
 * handling the MPEG format.
 *
 * This codec currently only supports MPEG 1 RTP headers, it doesn't
 * deal with the MPEG 2 extension headers.
 */
public class RTPDePacketizer {

    public static float RATE_TABLE[] = {
			0.0f, 23.976f, 24.f, 25.f, 29.97f,
			30.f, 50.f, 59.94f, 60.f
    };

    private static char[] hexChar = {
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private long discardtimestamp = -1;
    private MPEGFrame currentframe = null;
    private boolean newframe = true;
    private boolean gotSequenceHeader = false;
    private boolean sequenceSent = false;
    private byte[] sequenceHeader = null;
    private boolean gopset = false;
    private int closedGop = 0;		// closed_gop flag from last GOP
    private int ref_pic_temp = -1;
    private int dep_pic_temp = -1;
    private int sequenceNumber = 0;
    private int width = 352;
    private int height = 240;
    private float frameRate = VideoFormat.NOT_SPECIFIED;
    private VideoFormat outFormat = null;

    /*
     * If the MPEG decoder has been tweaked to deal with dropped
     * slices, partial frames and missing I or P frames, you
     * can set fullFrameOnly to false and allowHeadless to true.
     * The depacketizer will recognize missing GOP and picture_start
     * segments and fill them in if possible. It will also only
     * include full slices, dropping any missing or partial slices.
     * Since the MPEG spec says there can be no gaps in the slice pattern
     * for MPEG 1 the default is to only present complete frames and
     * drop any frames that can not have forward or backward motion
     * resolved.
     */
    private boolean allowHeadless = false;
    private boolean fullFrameOnly = true;
    private boolean droppedPFrame = false;
    private boolean droppedIFrame = false;

    /* Left in to simplify debugging. When capture = true, each frame
     * forwarded to the decoder is captured to disk. The overhead
     * of disk capture increases the number of dropped frames so
     * any problem relating to dropped frames is likely to be exacerbated.
     */
    private boolean capture = false;
    private OutputStream captureFile = null;

    private static final boolean debug = false;

    RTPDePacketizer() {
	if (capture) {
	    try {
		captureFile = new BufferedOutputStream(
				new FileOutputStream("/tmp/rtpstream.mpg"));
	    } catch(IOException ioe) {
		System.err.println("RTPDePacketizer: unable to open file "
									+ ioe);
		capture = false;
	    }
	}
    }

    /*
     * finalize is only useful if capture is true. It will not get invoked
     * until garbage collection occurs. To get it to work in JMStudio,
     * select close for the RTP stream then start to play some other file
     * to trigger garbage collection.
     */
    public void finalize() {
	if (capture) {
	    try {
		captureFile.flush();
		captureFile.close();
		System.err.println("RTPDePacketizer: closed file");
	    } catch(IOException ioe) {
		System.err.println("RTPDePacketizer: unable to close file "
									+ ioe);
		capture = false;
	    }
	}
    }

    /**
     * This method will reconstruct a MPEG frame from individual RTP
     packets. The reconstruction process waits till all the packtes of
     a frame are received and send this over to the decoder only if
     all the frames were received. If a the first packet of a frame is
     not received, all other packets belonging to this frame are
     discarded. If an out of order packet is received, it is kept
     around until the last packet of the frame is received and which
     time the depacketizer will attempt to reorder the frames. 
     */ 
    public int process(Buffer inBuffer, Buffer outBuffer) {
	// if this timestamp of this inBuffer belongs to a frame we
	// have marked as a to-be discarded frame, discard this packet.
	// We don't want the outData to be sent to the decoder.
	// Allow -1 through -- the timestamp isn't being set.
	if ((inBuffer.getTimeStamp() == discardtimestamp) &&
						(discardtimestamp != -1)) {
		return PlugIn.OUTPUT_BUFFER_NOT_FILLED; 
	}

	// if the newframe is not set and we have received a packet
	// with a RTP timestamp different from the RTPtimestamp of the
	// current frame, we have lost the last packet(s) of the
	// current frame. Discard the current frame
	if ( (!newframe) && (currentframe != null) &&
	    (inBuffer.getTimeStamp() != currentframe.rtptimestamp)) {
	    if (allowHeadless || firstPacket(inBuffer)) {
		boolean haveframe = false;
		if (fullFrameOnly) {
		    if (debug) {
			System.err.print(
			    "!newframe & timestamp mismatch & firstPacket ");
		    }
		    dropFrame();
		} else {
		    haveframe = constructFrame(outBuffer);
		}
		currentframe = createNewFrame(inBuffer);
		if (haveframe) {
		    return PlugIn.BUFFER_PROCESSED_OK;
		} else {
		    // if the whole frame is a single packet
		    if ((currentframe.getFirst().getFlags()
					& Buffer.FLAG_RTP_MARKER) != 0) {
			// copy all elements of the vector into one big buffer.
			if (constructFrame(outBuffer)) {
			    return PlugIn.BUFFER_PROCESSED_OK;
			}
		    }
		    return PlugIn.OUTPUT_BUFFER_NOT_FILLED;
		}
	    }
	    else {
		discardtimestamp = inBuffer.getTimeStamp();
		if (fullFrameOnly) {
		    if (debug) {
			System.err.print("!newframe & timestamp mismatch ");
		    }
		    dropFrame();
		    dropBufferFrame(inBuffer);
		} else if (compareSequenceNumbers(currentframe.seqno,
					inBuffer.getSequenceNumber()) > 0) {
		    // this is a new packet, complete the previous frame
		    if (constructFrame(outBuffer)) {
			return PlugIn.BUFFER_PROCESSED_OK; //return prev frame
		    }
		}
		// this is an old packet, don't complete the frame
		return PlugIn.OUTPUT_BUFFER_NOT_FILLED;
	    }
	}
	// if we are ready for a newframe and receive the first packet
	// of this frame, create a new frame buffer, else mark this
	// frame as a discard frame.
	if (newframe) {
	    if (firstPacket(inBuffer)) {
		newframe = false;
		currentframe = createNewFrame(inBuffer);
		// if the whole frame is a single packet
		if ((currentframe.getFirst().getFlags()
					& Buffer.FLAG_RTP_MARKER) != 0) {
		    // copy all elements of the vector into one big buffer.
		    if (constructFrame(outBuffer)) {
			return PlugIn.BUFFER_PROCESSED_OK;
		    }
		}
		return PlugIn.OUTPUT_BUFFER_NOT_FILLED;
	    }
	    if (fullFrameOnly) {
		if (debug) {
		    System.err.print("newframe & not firstPacket ");
		}
		dropBufferFrame(inBuffer);
	    }
	    discardtimestamp = inBuffer.getTimeStamp();
	    newframe = true;
	    return PlugIn.OUTPUT_BUFFER_NOT_FILLED;
	}
	// add the packet to the queue
	int ret = addToFrame(inBuffer, outBuffer);
	return ret;
    } // end of process.


    protected  String toHex (byte[] inData, int inOffset) {
	String hex = new String();
	for (int i = 0; i < 4; i++) {
	    hex += hexChar[(inData[inOffset + i] >> 4) & 0x0f];
	    hex += hexChar[inData[inOffset + i] & 0x0f];
	}
	return hex;
    }



    private int addToFrame(Buffer inBuffer, Buffer outBuffer) {

      Buffer b = copyInto(inBuffer);

      currentframe.add(b);

	if ((b.getFlags() & Buffer.FLAG_RTP_MARKER) != 0) {
	    // copy all elements of the vector into one big buffer.
	    if (constructFrame(outBuffer)) {
		return PlugIn.BUFFER_PROCESSED_OK;
	    }
	}
	return PlugIn.OUTPUT_BUFFER_NOT_FILLED;
    }



    private void constructGop(Buffer outBuffer) {
	byte[] dest = (byte[])outBuffer.getData();
	int outoffset = outBuffer.getLength();
	if (sequenceHeader != null) {
	    System.arraycopy(sequenceHeader, 0, dest, outoffset,
							 sequenceHeader.length);
	    outBuffer.setLength(outBuffer.getLength() + sequenceHeader.length);
	    outoffset += sequenceHeader.length;
	    sequenceSent = true;
	}
	dest[outoffset] = 0;
	dest[outoffset+1] = 0;
	dest[outoffset+2] = 1;
	dest[outoffset+3] = (byte) 0xb8;

	// drop_frame_flag + time_code_hours + 2 bits time_code_minutes
	dest[outoffset+4] = (byte) 0x80;
	// 4 bits time_code_minute + marker_bit + 3 bits time_code_seconds
	dest[outoffset+5] = (byte) 0x08;
	// 3 bits time_code_seconds + 5 bits time_code_picture
	dest[outoffset+6] = 0;
	// 3 bits time_code_picture + closed_gop + broken_link
	dest[outoffset+7] = (byte) (closedGop | 0x20);

	outBuffer.setLength(outBuffer.getLength() + 8);
	ref_pic_temp = 0;	// don't know the correct value...
	dep_pic_temp = -1;

    }


    private void constructPicture(Buffer inBuffer, Buffer outBuffer) {
	byte[] payload = (byte[])inBuffer.getData();
	int offset = inBuffer.getOffset();
	byte[] dest = (byte[])outBuffer.getData();
	int outoffset = outBuffer.getLength();
	int next = 0;
	dest[outoffset] = 0;
	dest[outoffset+1] = 0;
	dest[outoffset+2] = 1;
	dest[outoffset+3] = 0;

	// set 8 of 10 bits of temporal reference
	dest[outoffset+4] = (byte) ((payload[offset] & 0x03) << 6
					| (payload[offset+1] & 0xfc) >> 2);

	int ptype = payload[offset+2] & 0x07;
	int back = (payload[offset+3] & 0xf0) >> 4;
	int fwd = payload[offset+3] & 0x0f;

	// set last 2 bits of temporal reference plus 3 bits picture type
	// leave vbv_delay 0 (only first 3 bits of vbv_delay in this byte)
	dest[outoffset+5] = (byte) ((payload[offset+1] & 0x02) << 6
								| ptype << 3);
	dest[outoffset+6] = 0;	// next 8 bits of vbv_delay
	if (ptype == 1) {
	    dest[outoffset+7] = 0;	// last 5 bits of vbv_delay
	    outBuffer.setLength(outBuffer.getLength() + 8);
	} else {
	    // last 5 bits vbv_delay + full_pel_forward_vector
	    //				+ first 2 bits forward_f_code
	    next = fwd >> 1;
	    dest[outoffset+7] = (byte) next;
	    // last 1 bit forward_f_code
	    next = (fwd & 0x01) << 7;
	    if (ptype > 2) {
		// last 1 bit forward_f_code + full_pel_backward_vector
		//					+ backward_f_code
		next |= back << 3;
	    }
	    dest[outoffset+8] = (byte) next;
	    outBuffer.setLength(outBuffer.getLength() + 9);
	}
    }



    // need to insure picture start and possibly GOP are present.
    private void constructHeaders(Buffer outBuffer) {

	boolean havePicture = false;

	int outoffset = 0;
	byte[] dest = (byte[]) outBuffer.getData();
	Buffer src = (Buffer)currentframe.data.elementAt(0);
	byte[] payload = (byte[])src.getData();
	int offset = src.getOffset();
	int tr = (payload[offset] & 0x03) << 8 | (payload[offset+1] & 0xff);
	int type = payload[offset+2] & 0x07;

	// first determine what headers are present
	if (src.getLength() >= 8	&& (payload[offset+2] & 0x10) == 0x10
					&& payload[offset+4] == 0
					&& payload[offset+5] == 0
					&& payload[offset+6] == 1) {
	    int startCode = payload[offset+7] & 0xff;
	    if (startCode == 0xb3) {
		// Found sequence start, just reset counters
		sequenceSent = true;
		ref_pic_temp = tr;
		dep_pic_temp = -1;
		return;
	    } else if (startCode == 0xb8) {
		// Found sequence GOP, insert sequence start and reset counters
		if (sequenceHeader != null) {
		    System.arraycopy(sequenceHeader, 0, dest,
							outBuffer.getLength(),
							sequenceHeader.length);
		    outBuffer.setLength(outBuffer.getLength()
							+sequenceHeader.length);
		    sequenceSent = true;
		}
		ref_pic_temp = tr;
		dep_pic_temp = -1;
		return;
	    } else if (startCode == 0)
		havePicture = true;
	}
	ref_pic_temp++;
	dep_pic_temp++;
	if (type < 3) {
	    // it's either a I or P picture
	    if (tr < ref_pic_temp) {
		constructGop(outBuffer);
	    }
	    ref_pic_temp = tr;	// in case we missed some
	} else {
	    // it's a B picture
	    if (tr < dep_pic_temp) {
		constructGop(outBuffer);
	    }
	    dep_pic_temp = tr;	// in case we missed some
	}
	if (!havePicture)
	    constructPicture(src, outBuffer);
    }



    private void dropFrame() {
	Buffer src = (Buffer)currentframe.data.firstElement();
	dropBufferFrame(src);
    }

    private void dropBufferFrame(Buffer src) {
	int type = ((byte[])src.getData())[src.getOffset() + 2] & 0x07;
	if (type == 1)
	    droppedIFrame = true;
	else if (type == 2)
	    droppedPFrame = true;
	if (debug) {
	    System.err.println("Dropping " + ((type == 1) ? "I"
					: ((type == 2) ? "P"
					: ((type == 3) ? "B" : "D")))
				+ " frame");
	}
	newframe = true;
	if (debug && (type <= 0 || type > 3)) {
	    System.err.println("Invalid type " + type + " header "
			+ toHex((byte[])src.getData(), src.getOffset()));
	    System.err.println("Buffer length " + src.getLength());
	}
	currentframe = null;
	return;
    }



    private boolean constructFrame(Buffer outBuffer) {
	Buffer src = (Buffer)currentframe.data.lastElement();
	int type = ((byte[])src.getData())[src.getOffset() + 2] & 0x07;
	if (fullFrameOnly) {
	    if (type >= 2 && (droppedIFrame || droppedPFrame)) {
		if (debug) {
		    System.err.print("Previously dropped I / P frame ");
		}
		dropFrame();
		return false;
	    } else if (type == 1) {
		droppedIFrame = false;	// found an I frame, reset all
		droppedPFrame = false;
	    }

	    // validate the buffers received

	    // does frame end with RTP mark
	    if ((src.getFlags() & Buffer.FLAG_RTP_MARKER) == 0) {
		if (debug) {
		    System.err.print("No RTP marker ");
		}
		dropFrame();
		return false;
	    }
	    // are all sequence numbers present
	    for (int i = currentframe.data.size() - 2; i >= 0; i--) {
		Buffer prev = (Buffer)currentframe.data.elementAt(i);
		if (compareSequenceNumbers(prev.getSequenceNumber(),
						src.getSequenceNumber()) != 1) {
		    if (debug) {
			System.err.print("Missing sequence # ");
		    }
		    dropFrame();
		    return false;
		}
		src = prev;
	    }

	}

	boolean noslices = true;
	byte[] dest = (byte[]) outBuffer.getData();
	if (dest == null || dest.length < currentframe.datalength
						+ sequenceHeader.length + 16)
	    dest = new byte[currentframe.datalength + sequenceHeader.length+16];
	//outBuffer.setFlags(0);
	outBuffer.setData(dest);
	outBuffer.setOffset(0);
	outBuffer.setLength(0);

	// make sure there are sufficient headers
	constructHeaders(outBuffer);
	if (!sequenceSent) {
	    if (debug) {
		System.err.print("!sequence sent ");
	    }
	    dropFrame();
	    return false;
	}

	int outoffset = outBuffer.getLength();

	// copy all elements of the vector into one big buffer.
	bufloop: for (int i = 0; i < currentframe.data.size(); i++) {
	    src = (Buffer)currentframe.data.elementAt(i);
	    byte[] payload = (byte[])src.getData();
	    int offset = src.getOffset();
	    if ((payload[offset+2] & 0x10) != 0x10)
		continue bufloop;	// doesn't contain beginning of slice
	    if ((payload[offset+2] & 0x08) == 0x08) {
		// buffer contains complete slices, copy it
		System.arraycopy(payload,
				 offset + 4,	// 4 byte MPEG header
				 dest,
				 outoffset,
				 src.getLength() - 4);
		outoffset+= src.getLength() - 4;
		noslices = false;
		continue bufloop;
	    }

	    // Have beginning of slice, now need to find the end of the
	    // slice and ALL packets in between
	    long seq = src.getSequenceNumber();
	    int j;
	    for (j = i+1; j < currentframe.data.size(); j++) {
		Buffer next = (Buffer)currentframe.data.elementAt(j);
		if (compareSequenceNumbers(seq,
					next.getSequenceNumber()) != 1) {
		    if (i == 0) {
			// if this is the first packet of the frame need
			// to get sequence header, GOP and picture start
			// if they are present. Take everything up to
			// the first slice start.
			offset += 4;	// skip MPEG RTP header
			int len = src.getLength() - 4;
			int off = offset;
			while (len > 4) {
			    if (payload[off+0] == 0
					&& payload[off+1] == 0
					&& payload[off+2] == 1
					&& (payload[off+3] & 0xff) > 0
					&& (payload[off+3] & 0xff) <= 0xaf)
				break;	// hit beginning of slice
			    off++;
			    len--;
			}
			if (off == offset)
				continue bufloop;	// none present
			System.arraycopy(payload,
					 offset,
					 dest,
					 outoffset,
					 off - offset);
			outoffset+= off - offset;
		    }
		    continue bufloop;
		}
		seq = next.getSequenceNumber();
		if (( ((byte[]) next.getData())[next.getOffset()+2]
							    & 0x08) == 0x08) {
		    break;				// found end of slice
		}
	    }
	    if (j == currentframe.data.size()) {
		break bufloop; // ran past end of array, drop last slice
	    }
	    for (int k = i; k <= j; k++) {
		src = (Buffer)currentframe.data.elementAt(k);
		System.arraycopy((byte[])src.getData(),
				 src.getOffset() + 4,	// 4 byte MPEG header
				 dest,
				 outoffset,
				 src.getLength() - 4);
		outoffset+= src.getLength() - 4;
	    }
	    noslices = false;
	    i = j;
	}
	if (outFormat == null || outFormat.getSize().width != width
				|| outFormat.getSize().height != height
				|| outFormat.getFrameRate() != frameRate) {
	    java.awt.Dimension d = new java.awt.Dimension(width, height);
	    outFormat = new VideoFormat(VideoFormat.MPEG,
				   d,
				   javax.media.format.VideoFormat.NOT_SPECIFIED,
				   Format.byteArray,
				   frameRate
				   );
	}
	    
	outBuffer.setLength(outoffset);
	outBuffer.setFormat(outFormat);

	if (noslices) {
	    outBuffer.setFlags(outBuffer.FLAG_DISCARD);
	} else {
	    //outBuffer.setFlags(outBuffer.getFlags() | outBuffer.FLAG_NO_SYNC);
	}
	    
	outBuffer.setTimeStamp(currentframe.rtptimestamp);
	outBuffer.setSequenceNumber(sequenceNumber++);

	newframe = true;
	currentframe = null;
	if (noslices)
	    return false;
	if (capture) {
	    try {
		captureFile.write((byte[]) outBuffer.getData(),
				outBuffer.getOffset(), outBuffer.getLength());
	    } catch(IOException ioe) {
		System.err.println(
			"RTPDePacketizer: write error for sequence number "
			+ outBuffer.getSequenceNumber() + " : " + ioe);
		capture = false;
	    }
	}
	return true;
    }
    

    private boolean firstPacket(Buffer inBuffer) {
	if (inBuffer == null)
	    return false;
	byte[] payload = (byte[])inBuffer.getData();
	if (payload == null)
	  return false;
	int offset = inBuffer.getOffset();
	int len = inBuffer.getLength();
	if (len < 12)
	    return false;

	// First, must find a sequence header to indicate start of movie
	if (!gotSequenceHeader) {
	    // Look for a sequence header. Until one is found discard frames.
	    if ((payload[offset+2] & 0x20) == 0x20
					&& payload[offset+4] == 0
					&& payload[offset+5] == 0
					&& payload[offset+6] == 1
					&& (payload[offset+7] & 0xff) == 0xb3) {
		// Found a sequence header, get width, height, frame rate

		// width is 12 bits at sequence header + 4
		width = (payload[offset + 8] & 0xff) << 4
					    | (payload[offset + 9] & 0xf0) >> 4;
		// height is 12 bits at sequence header + 5.5
		height = (payload[offset + 9] & 0x0f) << 8
					    | (payload[offset + 10] & 0xff);
		// frameRate index is 4 bits at sequence header + 6.5
		// int frix = (payload[offset + 11] & 0x0f);
		// if (frix > 0 && frix <= 8)
		//     frameRate = RATE_TABLE[frix];
		gotSequenceHeader = true;
		offset += 4;
		len -= 4;
		int off = offset;
		while (len > 8) {
		  if (payload[off+0] == 0 && payload[off+1] == 0
						&& payload[off+2] == 1) {
		      if ((payload[off+3] & 0xff) == 0xb8) {
			gopset = true;
			closedGop = payload[off+7] & 0x40;
			// force broken_link bit on
			payload[off+7] = (byte) (payload[off+7] & 0x20);
			sequenceHeader = new byte[off - offset];
			System.arraycopy(payload,
					 offset,
					 sequenceHeader,
					 0,
					 sequenceHeader.length);
			return true;		// hit group of pictures (GOP)
		      }
		  }
		  off++;
		  len--;
		}
		return true;
	    }
	    return false;
	}

	// check for a start of picture in the MPEG header and
	// return true if found
	if ((payload[offset+2] & 0x10) != 0x10)
	    return false;	// doesn't contain beginning of slice
	offset += 4;		// skip MPEG RTP header
	len -= 4;
	while (len > 8) {
	  if (payload[offset+0] == 0 && payload[offset+1] == 0
						&& payload[offset+2] == 1) {
	      if (payload[offset+3] == 0)
		return true;		// hit start of picture
	      if ((payload[offset+3] & 0xff) == 0xb8) {
		gopset = true;
		closedGop = payload[offset+7] & 0x40;
		// force broken_link bit on
		payload[offset+7] = (byte) (payload[offset+7] | 0x20);
		return true;		// hit group of pictures (GOP)
	      }
	      if ((payload[offset+3] & 0xff) <= 0xaf)
		return false;		// hit beginning of slice
	  }
	  offset++;
	  len--;
	}
	return false;
    }

    static private int  MAX_SEQ = 65535;

    /*
     * the RTP sequence number is unsigned 16 bit counter that
     * wraps around. Allow for the case where it has wrapped.
     * @param p sequence number of the suspected previous buffer
     * @param c sequence number of the current (or next) buffer
     * @return int difference in sequence numbers
     */
    private int  compareSequenceNumbers(long p, long c) {
	if (c > p)
	    return (int) (c - p);
	if (c == p)
	    return 0;
	if (p > MAX_SEQ - 100 && c < 100) {
	    // Allow for the case where sequence number has wrapped.
	    return (int) ((MAX_SEQ - p) + c + 1);
	}
	return -1;
    }

    private MPEGFrame  createNewFrame(Buffer inBuffer) {
	Buffer b = copyInto(inBuffer);
	
	MPEGFrame newframe = new MPEGFrame(b);
	newframe.add(b);
	return newframe;
    }

    private Buffer copyInto(Buffer src) {
	Buffer dest = new Buffer();
	dest.copy(src);
	src.setData(null);
	src.setHeader(null);
	src.setLength(0);
	src.setOffset(0);
	return dest;
    }

    class MPEGFrame {

	public long rtptimestamp = -1;

	/* the current seqnumber upto which the frame has been correctly
	/* ordered */ 
	public long seqno = -1;
	private int datalength = 0;
	private Vector data;
    
	public MPEGFrame(Buffer buffer) {
	    data = new Vector();
	    rtptimestamp = buffer.getTimeStamp();
	}
	public void add(Buffer buffer) {
	    // don't add bad buffers
	    if (buffer == null || buffer.getData() == null
						|| buffer.getLength() < 4)
		return;
	    if (compareSequenceNumbers(seqno, buffer.getSequenceNumber()) > 0) {
		data.addElement(buffer);
		seqno = buffer.getSequenceNumber();
	    } else {
		long sq = buffer.getSequenceNumber();
		for (int i = 0; i < data.size(); i++) {
		    long bsq = ((Buffer) data.elementAt(i)).getSequenceNumber();
		    if (compareSequenceNumbers(bsq, sq) < 0) {
			data.insertElementAt(buffer, i);
			break;
		    } else if (sq == bsq) {
			// duplicate -- don't add it to the frame
			return;
		    }
		}
	    }
	    datalength += buffer.getLength() -4;
	}
	public Vector getData() {
	    return data;
	}
	public Buffer getFirst() {
	    if (data.size() > 0)
		return (Buffer) data.firstElement();
	    else
		return null;
	}
	public int getLength() {
	    return datalength;
	}
    }
    
}


