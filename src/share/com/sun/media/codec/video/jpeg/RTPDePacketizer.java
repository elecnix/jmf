/*
 * @(#)RTPDePacketizer.java	1.28 00/09/21
 *
 * Copyright 1998-2000 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.codec.video.jpeg;

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.JPEGFormat;
import java.awt.Dimension;

import com.sun.media.*;
import java.util.Vector;

/**
 * This codec is a JPEG RTP depacketizer. It receives individual RTP
 * buffers with a JPEG_RTP format. These buffers will be used to
 * reconstruct a complete frame in the JPEG format. Once a frame is
 * constructed, it is sent over to the next node i.e. a node capable of
 * handling the JPEG format.
 */
public class RTPDePacketizer
{
    private JPEGFrame currentFrame = null;
    protected byte frameBuffer[] = null;
    protected int sequenceNumber = 0;
    protected int quality = 0;
    protected int type = -1;

    byte [] lastJFIFHeader = null;
    int lastQuality = -2;
    int lastType = -1;
    int lastWidth = -1;
    int lastHeight = -1;

         

    
    public int getQuality() {
	return quality;
    }
    
    public int getType() {
	return type;
    }
    
    /**
     * This method will reconstruct a JPEG frame from individual RTP
     packets. The reconstruction process waits till all the packtes of
     a frame are received and send this over to the decoder only if
     all the frames were received. If a the first packet of a frame is
     not received, all other packets belonging to this frame are
     discarded. 
     */
    
    public int process(Buffer inBuffer, Buffer outBuffer)
    {
	// If we've been decoding a frame and packets from another
	// frame has arrived, then we are missing the a few packets
	// for the current frame.  We'll discard the current frame. 
	if (currentFrame != null && 
	    inBuffer.getTimeStamp() != currentFrame.rtptimestamp) {
	    currentFrame = null;
	}

	// Check if this is the first packet from a new frame.
	// The first packet has a framgment offset of 0.
	if (getFragOffset((byte[])inBuffer.getData(), inBuffer.getOffset()) == 0) {
	    currentFrame = new JPEGFrame(this, inBuffer, (byte[])outBuffer.getData());
	} else if (currentFrame != null) {
	    // This is a new packet for the current frame.
	    currentFrame.add(inBuffer, 0);
	} else {
	    // If we don't have a current frame, then we are missing the
	    // first packet for this frame.  We'll discard the current packet.
	    return PlugIn.OUTPUT_BUFFER_NOT_FILLED;
	}

	// If this is the last packet from this frame, we'll need to
	// check if all the packets from this frame has been received.
        if ((inBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) !=0) {
	    if (currentFrame.gotAllPackets(inBuffer.getSequenceNumber())) {
		currentFrame.completeTransfer(inBuffer, outBuffer);
	 	currentFrame = null;
		return PlugIn.BUFFER_PROCESSED_OK;
	    } else {
	 	currentFrame = null;
		return PlugIn.OUTPUT_BUFFER_NOT_FILLED;
	    }
	}

	return PlugIn.OUTPUT_BUFFER_NOT_FILLED;
    }


    public int getFragOffset(byte data[], int doff) {
	// Fragment offset is the 2nd, 3rd & 4th byte of the JPEG Hdr.
	int foff = 0;
	foff |= (data[doff + 1] & 0xff) << 16;
	foff |= (data[doff + 2] & 0xff) << 8;
	foff |= (data[doff + 3] & 0xff);

	return foff;
    }
}



class JPEGFrame
{
    public long rtptimestamp = -1;

    /*the current seqnumber upto which the frame has been correctly
    /*ordered*/ 
    public int dataLength = 0;

    private RTPDePacketizer depacketizer;
    private int hdrOffset = 0;	// size of the inserted JFIF header.

    private long firstSeq, numPkts = 0;

    final int FRAME_BUFFER_INITIAL_SIZE= 32000;
    

    int lquantOffset = (
		       2 + // For FF D8 (SOI)
		       APP0.length +
		       2 + // For FF DB (DQT)
                       2 + // length
                       1  // tableNo
		       );

    int cquantOffset = (
			lquantOffset +
			64 + // size of luma quant table
		       2 + // For FF DB (DQT)
                       2 + // length
                       1  // tableNo
		       );

    public JPEGFrame( RTPDePacketizer depacketizer,
		      Buffer          buffer,
		      byte            frameBuffer[])
    {
	this.depacketizer= depacketizer;
	firstSeq = buffer.getSequenceNumber();

	if (depacketizer.frameBuffer == null) {
	    if (frameBuffer != null)
		depacketizer.frameBuffer = frameBuffer;
	    else
		depacketizer.frameBuffer = new byte[FRAME_BUFFER_INITIAL_SIZE];
	}

        rtptimestamp = buffer.getTimeStamp();	
	int extraskip = 0;
	// If the first JPEG packet doesn't have a
	//    JFIF header, generate it.
	// This is true for vic
	if (!hasJFIFHeader(buffer))
	    extraskip = generateJFIFHeader(buffer);
	
	add( buffer, extraskip );
    }

    public void add(Buffer buffer, int extraskip)
    {
	int chunkSize= buffer.getLength() - 8 - extraskip;
	
	int foff = depacketizer.getFragOffset((byte[])buffer.getData(),
					buffer.getOffset());

	foff += hdrOffset;	// If a JFIF header is inserted,
				// we need to shift the data.
	                        // 2 bytes is for the EOI marker

	if (depacketizer.frameBuffer.length >= foff + chunkSize + 2)
	{
	    System.arraycopy( (byte[])buffer.getData(),
	                      buffer.getOffset() + 8 + extraskip, //RTP Hdr + JPEG Hdr
		              depacketizer.frameBuffer,
		              foff,
		              chunkSize);
	    
	    dataLength+= chunkSize;
	    numPkts++;
	}
	else
        {
            // 2 bytes is for the EOI marker
	    increaseFrameBuffer( foff + chunkSize + 2);
	    add( buffer, extraskip);
	}
    }


    public boolean gotAllPackets(long lastSeq) {
	return (lastSeq - firstSeq + 1 == numPkts);
    }

    public void completeTransfer(Buffer inBuffer, Buffer outBuffer)
    {
	int offset = inBuffer.getOffset();
	java.awt.Dimension d;
	
	byte[] inBuff = (byte[])inBuffer.getData();
	// height is the 8th byte in 8 bit pixels
	int height = inBuff[offset + 7] & 0xff;
	// width is the 7th byte in 8bit pixels
	int width = inBuff[offset + 6] & 0xff;
	// Q factor is the 6th byte
	depacketizer.quality = inBuff[offset +5] & 0xff;
	// type is the 5th byte
	depacketizer.type = inBuff[offset + 4] & 0xff;
	
	d = new java.awt.Dimension(width * 8, height *8);
		    	    
	inBuffer.setFormat(new VideoFormat(VideoFormat.JPEG,
					   d,
					   0,
					   inBuffer.getFormat().getDataType(),
					   Format.NOT_SPECIFIED));

	if (!((depacketizer.frameBuffer[dataLength-2] == (byte) 0xff) &&
	      (depacketizer.frameBuffer[dataLength-1] == (byte) 0xd9))) {
	    depacketizer.frameBuffer[dataLength++] = (byte) 0xff;
	    depacketizer.frameBuffer[dataLength++] = (byte) 0xd9; // EOI
	}

	outBuffer.setData( depacketizer.frameBuffer);
	outBuffer.setSequenceNumber(depacketizer.sequenceNumber++);
	outBuffer.setLength( dataLength);
	depacketizer.frameBuffer = null;
    }
    
    private void increaseFrameBuffer( int amount) {
	//System.out.println( "JPEG Frame: increasing frame buffer by " + amount + " bytes.");
	
	byte newFrameBuffer[]= new byte[ amount];

	// Copy from the old buffer to the new buffer.
	System.arraycopy( depacketizer.frameBuffer,
			  0,
			  newFrameBuffer,
			  0,
			  depacketizer.frameBuffer.length);

	depacketizer.frameBuffer = newFrameBuffer;
    }

    private boolean hasJFIFHeader(Buffer buffer) {
	byte [] data = (byte[]) buffer.getData();
	int offset = buffer.getOffset();
	if (! ((data[offset+8] & 0xFF) == 0xFF &&
	       (data[offset+9] & 0xFF) == 0xD8   ) )
	    return false;
	else
	    return true;
    }

    private int generateJFIFHeader(Buffer buffer) {
	int extraskip = 0;
	byte [] data = (byte[]) buffer.getData();
	int offset = buffer.getOffset();
	int type = data[offset + 4] & 0xff;
	// Q factor is the 6th byte
	int quality = data[offset +5] & 0xff;
	// width is the 7th byte in 8bit pixels
	int width = data[offset + 6] & 0xff;
	// height is the 8th byte in 8 bit pixels
	int height = data[offset + 7] & 0xff;

	if (quality == depacketizer.lastQuality &&
	    width == depacketizer.lastWidth &&
	    height == depacketizer.lastHeight &&
	    type == depacketizer.lastType) {

	    System.arraycopy(depacketizer.lastJFIFHeader,
			     0,
			     depacketizer.frameBuffer,
			     0,
			     depacketizer.lastJFIFHeader.length);
	    hdrOffset = depacketizer.lastJFIFHeader.length;
	} else {
	    hdrOffset = makeHeaders(depacketizer.frameBuffer, 0,
				      type, quality, width, height);
	    depacketizer.lastJFIFHeader = new byte[hdrOffset];
	    System.arraycopy(depacketizer.frameBuffer, 0,
			     depacketizer.lastJFIFHeader, 0,
			     hdrOffset);
	    depacketizer.lastQuality = quality;
	    depacketizer.lastType = type;
	    depacketizer.lastWidth = width;
	    depacketizer.lastHeight = height;
	}

	if (quality >= 100) {
	    extraskip = 132;

	    System.arraycopy(data, offset+8+4,
                             depacketizer.frameBuffer, lquantOffset,
			     64);

	    System.arraycopy(data, offset+8+4+64,
                             depacketizer.frameBuffer, cquantOffset,
			     64);

	}
	dataLength += depacketizer.lastJFIFHeader.length;
	return extraskip;
    }
    
    /**************************************************************
     * JFIF HEADER GENERATION CODE
     **************************************************************/
    static final byte [] APP0 = { (byte) 0xFF, (byte) 0xE0, 0, 16,
				  0x4A, 0x46, 0x49, 0x46, 0, 1, 1,
				  0, 0, 1, 0, 1, 0, 0
    };

    /*
     * Given an RTP/JPEG type code, q factor, width, and height,
     * generate a frame and scan headers that can be prepended
     * to the RTP/JPEG data payload to produce a JPEG compressed
     * image in interchange format (except for possible trailing
     * garbage and absence of an EOI marker to terminate the scan).
     */
    private int makeHeaders(byte [] p, int offset,
			    int type, int q, int w, int h) {
        int lqt[] = new int[64];
        int cqt[] = new int[64];
	byte samp;
	
        /* convert from blocks to pixels */
        w *= 8;
        h *= 8;

        makeQTables(q, lqt, cqt);

        p[offset++] = (byte) 0xff;
        p[offset++] = (byte) 0xd8;            /* SOI */

	// APP0 marker
	for (int app = 0; app < APP0.length; app++) {
	    p[offset++] = APP0[app];
	}
	
	// TODO: pass q to makeQuantHeader. makeQuantHeader should
	// just skip computing the quant header as the quant data
	// is dynamic. Note that makeHeaders will be called only
	// if quality, width, height or type changes between frames.
        offset = makeQuantHeader(p, offset, lqt, 0);
        offset = makeQuantHeader(p, offset, cqt, 1);
	
        offset = makeHuffmanHeader(p, offset,
				   lum_dc_codelens, lum_dc_codelens.length,
				   lum_dc_symbols, lum_dc_symbols.length,
				   0, 0);
        offset = makeHuffmanHeader(p, offset,
				   lum_ac_codelens, lum_ac_codelens.length,
				   lum_ac_symbols, lum_ac_symbols.length,
				   0, 1);
        offset = makeHuffmanHeader(p, offset,
				   chm_dc_codelens, chm_dc_codelens.length,
				   chm_dc_symbols, chm_dc_symbols.length,
				   1, 0);
        offset = makeHuffmanHeader(p, offset,
				   chm_ac_codelens, chm_ac_codelens.length,
				   chm_ac_symbols, chm_ac_symbols.length,
				   1, 1);
	
        p[offset++] = (byte) 0xff;
        p[offset++] = (byte) 0xc0;			    /* SOF */
        p[offset++] = 0;				    /* length msb */
        p[offset++] = 17;				    /* length lsb */
        p[offset++] = 8;				    /* 8-bit precision */
        p[offset++] = (byte) ((h >> 8) & 0xFF);		    /* height msb */
        p[offset++] = (byte) ( h       & 0xFF);		    /* height lsb */
        p[offset++] = (byte) ((w >> 8) & 0xFF);		    /* width msb */
        p[offset++] = (byte) ( w       & 0xFF);		    /* width lsb */
        p[offset++] = 3;				    /* number of components */
        p[offset++] = 0;				    /* comp 0 */
        if (type == JPEGFormat.DEC_444)
	    p[offset++] = 0x11;    /* hsamp = 2, vsamp = 1 */
        else if (type == JPEGFormat.DEC_420)
	    p[offset++] = 0x22;    /* hsamp = 2, vsamp = 2 */
	else
	    p[offset++] = 0x21;
        p[offset++] = 0;               /* quant table 0 */
        p[offset++] = 1;               /* comp 1 */
	
	p[offset++] = 0x11;
        p[offset++] = 1;               /* quant table 1 */
        p[offset++] = 2;               /* comp 2 */
        p[offset++] = 0x11;
        p[offset++] = 1;               /* quant table 1 */

        p[offset++] = (byte) 0xff;
        p[offset++] = (byte) 0xda;            /* SOS */
        p[offset++] = 0;               /* length msb */
        p[offset++] = 12;              /* length lsb */
        p[offset++] = 3;               /* 3 components */
        p[offset++] = 0;               /* comp 0 */
        p[offset++] = 0;               /* huffman table 0 */
        p[offset++] = 1;               /* comp 1 */
        p[offset++] = 0x11;            /* huffman table 1 */
        p[offset++] = 2;               /* comp 2 */
        p[offset++] = 0x11;            /* huffman table 1 */
        p[offset++] = 0;               /* first DCT coeff */
        p[offset++] = 63;              /* last DCT coeff */
        p[offset++] = 0;               /* sucessive approx. */

        return offset;
    }

    static int lum_dc_codelens[] = {
        0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
    };

    static int lum_dc_symbols[] = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
    };

    static int lum_ac_codelens[] = {
        0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 0x7d,
    };

    static int lum_ac_symbols[] = {
        0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12,
        0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
        0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08,
        0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
        0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16,
        0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
        0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
        0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
        0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
        0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
        0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
        0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
        0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
        0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
        0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
        0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4,
        0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
        0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea,
        0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa
    };

    static int chm_dc_codelens[] = {
        0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
    };

    static int chm_dc_symbols[] = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
    };

    static int chm_ac_codelens[] = {
        0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 0x77,
    };
    
    static int chm_ac_symbols[] = {
	0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
        0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
        0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91,
        0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
        0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34,
        0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
        0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38,
        0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
        0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
        0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
        0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
        0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
        0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96,
        0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
        0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4,
        0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
        0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2,
        0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
        0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9,
        0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa
    };

    private int makeQuantHeader(byte [] p, int offset,
				int [] qt, int tableNo) {
        p[offset++] = (byte) 0xff;
        p[offset++] = (byte) 0xdb;			    /* DQT */
        p[offset++] = 0;				    /* length msb */
        p[offset++] = 67;				    /* length lsb */
        p[offset++] = (byte) tableNo;
	for (int i = 0; i < 64; i++) {
	    p[offset++] = (byte) qt[i];
	}
        return offset;
    }

    private int makeHuffmanHeader(byte [] p, int offset,
				  int [] codelens, int ncodes,
				  int [] symbols, int nsymbols,
				  int tableNo, int tableClass) {
	int i;
	
        p[offset++] = (byte) 0xff;
        p[offset++] = (byte) 0xc4;            /* DHT */
        p[offset++] = 0;               /* length msb */
        p[offset++] = (byte) (3 + ncodes + nsymbols); /* length lsb */
        p[offset++] = (byte) ((tableClass << 4) | tableNo);
	for (i = 0; i < ncodes; i++)
	    p[offset++] = (byte) codelens[i];
	for (i = 0; i < nsymbols; i++)
	    p[offset++] = (byte) symbols[i];
        return offset;
    }

    static int ZigZag[] = {
	0,  1,  8, 16,  9,  2,  3, 10,
        17, 24, 32, 25, 18, 11,  4,  5,
        12, 19, 26, 33, 40, 48, 41, 34,
        27, 20, 13,  6,  7, 14, 21, 28,
        35, 42, 49, 56, 57, 50, 43, 36,
        29, 22, 15, 23, 30, 37, 44, 51,
        58, 59, 52, 45, 38, 31, 39, 46,
        53, 60, 61, 54, 47, 55, 62, 63
    };
    
    /*
     * Table K.1 from JPEG spec.
     */
    static int jpeg_luma_quantizer[] = {
        16, 11, 10, 16, 24, 40, 51, 61,
        12, 12, 14, 19, 26, 58, 60, 55,
        14, 13, 16, 24, 40, 57, 69, 56,
        14, 17, 22, 29, 51, 87, 80, 62,
        18, 22, 37, 56, 68, 109, 103, 77,
        24, 35, 55, 64, 81, 104, 113, 92,
        49, 64, 78, 87, 103, 121, 120, 101,
        72, 92, 95, 98, 112, 100, 103, 99
    };

    /*
     * Table K.2 from JPEG spec.
     */
    static int jpeg_chroma_quantizer[] = {
        17, 18, 24, 47, 99, 99, 99, 99,
        18, 21, 26, 66, 99, 99, 99, 99,
        24, 26, 56, 99, 99, 99, 99, 99,
        47, 66, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99
    };

    /*
     * Call MakeTables with the Q factor and two int[64] return arrays
     */
    private void makeQTables(int q, int [] lum_q, int [] chr_q) {
	int i;
	int factor = q;
	
	if (q < 1) factor = 1;
	if (q > 99) factor = 99;
	if (q < 50)
	    q = 5000 / factor;
	else
	    q = 200 - factor*2;

	for (i = 0; i < 64; i++) {
	    int lq = (jpeg_luma_quantizer[ZigZag[i]] * q + 50) / 100;
	    int cq = (jpeg_chroma_quantizer[ZigZag[i]] * q + 50) / 100;

	    /* Limit the quantizers to 1 <= q <= 255 */
	    if (lq < 1)
		lq = 1;
	    else if (lq > 255)
		lq = 255;
	    lum_q[i] = lq;
	    
	    if (cq < 1)
		cq = 1;
	    else if (cq > 255)
		cq = 255;
	    chr_q[i] = cq;
	    //	    System.err.println("Values = " + lum_q[i] + " " + chr_q[i]);
	}
    }



}
