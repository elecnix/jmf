/*
 * @(#)VideoCodec.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.video;

import javax.media.format.*;
import javax.media.format.*;
import javax.media.*;
import com.sun.media.*;
import java.awt.Dimension;

public abstract class VideoCodec extends BasicCodec {

    protected String PLUGIN_NAME;
    protected VideoFormat defaultOutputFormats[];
    protected VideoFormat supportedInputFormats[];
    protected VideoFormat supportedOutputFormats[];
    protected VideoFormat inputFormat;
    protected VideoFormat outputFormat;
    protected final boolean DEBUG = true;


    public String getName() {
	return PLUGIN_NAME;
    }

    public Format [] getSupportedInputFormats() {
	return supportedInputFormats;
    }

    public Format setInputFormat(Format format) {
	if ( !(format instanceof VideoFormat) ||
           (null == matches(format, supportedInputFormats)) )
	        return null;

	inputFormat = (VideoFormat)format;
	return format;
    }


    public Format setOutputFormat(Format format) {

	// This methods assumes setInputFormat has already been called.

	if ( !(format instanceof VideoFormat) ||
           (null == matches(format, getMatchingOutputFormats(inputFormat))) )
	        return null;

	outputFormat = (VideoFormat)format;

	return format;
    }


    protected Format getInputFormat() {
	return inputFormat;
    }

    protected Format getOutputFormat() {
	return outputFormat;
    }

    protected  Format[] getMatchingOutputFormats(Format in) {
        return new Format[0];
    }

    public Format [] getSupportedOutputFormats(Format in) {

        // null input format
        if (in == null) {
            return defaultOutputFormats;
        }

        // mismatch input format
        if ( !(in instanceof VideoFormat ) ||
             (matches(in,supportedInputFormats) == null) ) {
                return new Format[0];

        }

        // match input format
        return getMatchingOutputFormats(in);

   }



    public boolean checkFormat(Format format) {
       Dimension inSize = ((VideoFormat) format).getSize();
        if (!inSize.equals(outputFormat.getSize()) ) {
           videoResized();
        }
        return true;
    }

    protected void videoResized() {
    }

    protected void updateOutput(Buffer outputBuffer, Format format,
				int length, int offset) {
	outputBuffer.setFormat(format);
	outputBuffer.setLength(length);
	outputBuffer.setOffset(offset);
    }
}
