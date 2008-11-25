/*
 * @(#)Handler.java	1.4 99/03/22
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
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
import javax.media.format.UnsupportedFormatException;
import java.io.IOException;
import javax.media.Control;
import javax.media.IncompatibleSourceException;
import javax.media.format.AudioFormat;

public class GSMMux extends com.sun.media.multiplexer.BasicMux {

    public GSMMux() {
	supportedInputs = new Format[1];
	supportedInputs[0] = new AudioFormat(AudioFormat.GSM);
	supportedOutputs = new ContentDescriptor[1];
	supportedOutputs[0] = new FileTypeDescriptor(FileTypeDescriptor.GSM);
    }

    public String getName() {
	return "GSM Multiplexer";
    }

    public Format setInputFormat(Format input, int trackID) {
	if (!(input instanceof AudioFormat))
	    return null;
	AudioFormat format = (AudioFormat) input;
	double sampleRate =  format.getSampleRate();

	String reason = null;
	double epsilon = 0.25;

	// Check to see if some of these restrictions can be removed
 	if (!format.getEncoding().equalsIgnoreCase(AudioFormat.GSM))
	    reason = "Encoding has to be GSM";
	else if ( Math.abs(sampleRate - 8000.0) > epsilon )
	    reason = "Sample rate should be 8000. Cannot handle sample rate " + sampleRate;
 	else if (format.getFrameSizeInBits() != (33*8))
 	    reason = "framesize should be 33 bytes";
	else if (format.getChannels() != 1)
	    reason = "Number of channels should be 1";		

	if (reason != null) {
	    return null;
	} else {
	    inputs[0] = format;
	    return format;
	}
    }
}




