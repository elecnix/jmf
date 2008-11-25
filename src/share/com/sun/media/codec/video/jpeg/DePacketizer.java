/*
 * @(#)DePacketizer.java	1.6 01/02/13
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
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
import javax.media.control.*;

import java.awt.Component;
import java.awt.Dimension;

import com.sun.media.*;
import com.sun.media.util.*;

public class DePacketizer extends BasicCodec {

    private VideoFormat inputFormat = null;
    private JPEGFormat outputFormat = null;

    // Decimation values. Initial value is unknown (-1)
    private int decimation = -1;
    private int quality = JPEGFormat.NOT_SPECIFIED;

    // RTP depacketizer if inputFormat is JPEG_RTP
    private RTPDePacketizer rtpdp = null;

    int DEFAULT_WIDTH = 320;
    int DEFAULT_HEIGHT = 240;
    
    
    // Initialize default formats.
    public DePacketizer()
    {
        inputFormats = new Format[] { new VideoFormat(VideoFormat.JPEG_RTP) };
        outputFormats = new Format[] { new VideoFormat(VideoFormat.JPEG) };
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

        // Make sure the input is JPEG video format
        if (matches(in, inputFormats) == null)
            return new Format[0];
        
        Format out [] = new Format[1];
        out[0] = makeJPEGFormat(in);
        return out;
    }

    public Format setInputFormat(Format input) {
        inputFormat = (VideoFormat) input;
	if (opened) {
	    outputFormat = makeJPEGFormat(inputFormat);
	}
        return input;
    }

    public Format setOutputFormat(Format output) {
	if (!(output instanceof VideoFormat)) return null;
        outputFormat = makeJPEGFormat(output);
        return outputFormat;
    }

    private final JPEGFormat makeJPEGFormat(Format in) {
	VideoFormat vf = (VideoFormat)in;
	return new JPEGFormat((vf.getSize() != null ? vf.getSize() : 
			       new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)),
			      VideoFormat.NOT_SPECIFIED,
			      Format.byteArray,
			      vf.getFrameRate(), 
			      quality,
			      decimation);
    }
    
    public void open() throws ResourceUnavailableException {
        if (inputFormat == null || outputFormat == null)
            throw new ResourceUnavailableException("Incorrect formats set on JPEG converter");
	rtpdp = new RTPDePacketizer();
	super.open();
    }

    public synchronized void close() {
	rtpdp = null;
	super.close();
    }

    public void reset() {
        // Anything to do?
    }
    
    public synchronized int process(Buffer inBuffer, Buffer outBuffer) {
        if (isEOM(inBuffer)) {
            propagateEOM(outBuffer);
            return BUFFER_PROCESSED_OK;
        }
        if (inBuffer.isDiscard()) {
            updateOutput(outBuffer, outputFormat, 0, 0);
            outBuffer.setDiscard(true);
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        // if the encoding is JPEG_RTP, send this packet over to the
        // depacketizer, which will do all the work for you. If the
        // depacketizer has not finished constructing a frame, dont
        // send it for decoding and just return. If the depacketizer
        // has finished constructing an entire frame, send it to the
        // decoder after getting a handle over the decimation and
        // quality from the depacketizer.


        int retVal = rtpdp.process(inBuffer, outBuffer);

        // return any value from the depacketizer except,
        // BUFFER_PROCESSED_OK, which indicates a complete frame
        // is ready for decoding
        if (retVal != BUFFER_PROCESSED_OK) {
            return retVal;
        }

        // at this time, the inBuffer contains a complete JPEG
        // frame of format JPEG
        int type = rtpdp.getType();
        int q = rtpdp.getQuality();

        // compute thje outputFormat only if the type has changed
        // or this is the first time.
        if (type != decimation || q != quality) {
            decimation = type;
	    quality = q;
            outputFormat = makeJPEGFormat(inBuffer.getFormat());
	}
	outBuffer.setFormat(outputFormat);

	// the following code fragment has been removed since the RTPDePacketizer
	// now uses the outBuffer for frame assembly.
	
	/*
        byte [] inData = (byte[]) inBuffer.getData();
        byte [] outData = (byte[]) outBuffer.getData();

        if (outData == null || outData.length < inData.length) {
            outData = new byte[inData.length];
            outBuffer.setData(outData);
        }
	*/
	// Neither copying or setting the data is optimal.
	// We'll need to rewrite RTPDePacketizer to reuse the array.
	// -ivg
	//System.arraycopy((byte[])inData, 0, (byte[])outData, 0, inBuffer.getLength());
	/*
	outBuffer.setData(inData);
        
        outBuffer.setLength(inBuffer.getLength());
	*/
	
	outBuffer.setOffset(0);
	
        outBuffer.setTimeStamp(inBuffer.getTimeStamp());
	
        inBuffer.setLength(0);

	outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_KEY_FRAME);

        return BUFFER_PROCESSED_OK;
    }

    public void finalize() {
        close();
    }
    
    public String getName() {
        return "JPEG DePacketizer";
    }
}
