/*
 * @(#)DePacketizer.java	1.3  02/08/21 SMI
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

import com.sun.media.*;

public class DePacketizer extends BasicCodec {

    private VideoFormat inputFormat = null;
    private VideoFormat outputFormat = null;

    // RTP depacketizer if inputFormat is MPEG_RTP
    private RTPDePacketizer rtpdp = null;
    
    
    // Initialize default formats.
    public DePacketizer() {
        inputFormats = new Format[] { new VideoFormat(VideoFormat.MPEG_RTP) };
        outputFormats = new Format[] { new VideoFormat(VideoFormat.MPEG) };
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
	return new VideoFormat(VideoFormat.MPEG,
			vf.getSize(),
			VideoFormat.NOT_SPECIFIED,
			Format.byteArray,
			vf.getFrameRate());
    }
    
    public void open() throws ResourceUnavailableException {
        if (inputFormat == null || outputFormat == null)
            throw new ResourceUnavailableException(
			"Incorrect formats set on MPEG video depacketizer");
	rtpdp = new RTPDePacketizer();
    }

    public synchronized void close() {
	rtpdp = null;
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

        // if the encoding is MPEG_RTP, send this packet over to the
        // depacketizer, which will do all the work for you. If the
        // depacketizer has not finished constructing a frame, dont
        // send it for decoding and just return. If the depacketizer
        // has finished constructing an entire frame, send it to the
        // decoder.


        int retVal = rtpdp.process(inBuffer, outBuffer);

        // return any value from the depacketizer except,
        // BUFFER_PROCESSED_OK, which indicates a complete frame
        // is ready for decoding
        if (retVal != BUFFER_PROCESSED_OK) {
            return retVal;
        }

        // at this time, the outBuffer contains a complete MPEG
        // frame of format MPEG

        // get the outputFormat from the buffer only if this is the first time.
        if (outputFormat == null) {
            outputFormat = (VideoFormat) outBuffer.getFormat();
        }

        return BUFFER_PROCESSED_OK;
    }

    public void finalize() {
        close();
    }
    
    public String getName() {
        return "MPEG Video DePacketizer";
    }
}
