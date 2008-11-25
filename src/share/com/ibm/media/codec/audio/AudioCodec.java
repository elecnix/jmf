/*
 * @(#)AudioCodec.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio;

import javax.media.format.*;
import javax.media.format.*;
import javax.media.*;
import com.sun.media.*;

public abstract class AudioCodec extends BasicCodec {

    protected String PLUGIN_NAME;
    protected AudioFormat defaultOutputFormats[];
    protected AudioFormat supportedInputFormats[];
    protected AudioFormat supportedOutputFormats[];
    protected AudioFormat inputFormat;
    protected AudioFormat outputFormat;
    protected final boolean DEBUG = true;

    public String getName() {
	return PLUGIN_NAME;
    }

    public Format [] getSupportedInputFormats() {
	return supportedInputFormats;
    }


    public Format setInputFormat(Format format) {
	if ( !(format instanceof AudioFormat) ||
           (null == matches(format, supportedInputFormats)) )
	        return null;

	inputFormat = (AudioFormat)format;
	return format;
    }


    public Format setOutputFormat(Format format) {

	// This methods assumes setInputFormat has already been called.

	if ( !(format instanceof AudioFormat) ||
           (null == matches(format, getMatchingOutputFormats(inputFormat))) )
	        return null;

	outputFormat = (AudioFormat)format;

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
        if (in==null) {
            return defaultOutputFormats;
        }

        // mismatch input format
        if ( !(in instanceof AudioFormat ) ||
             (matches(in,supportedInputFormats)==null) ) {
                return new Format[0];

        }

        // match input format
        return getMatchingOutputFormats(in);

   }


    /**
     * Checks the header of the compressed audio packet and detects any format
     * changes. Does not modify the buffer in any way.
     * TBD: how to select spesific output format
     */
    public boolean checkFormat(Format format) {
        return true;
    }


}
