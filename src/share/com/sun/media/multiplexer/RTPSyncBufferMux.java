/*
 * @(#)RTPSyncBufferMux.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.multiplexer;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import com.sun.media.rtp.FormatInfo;

public class RTPSyncBufferMux extends RawSyncBufferMux {

    FormatInfo rtpFormats = new FormatInfo();

    public RTPSyncBufferMux() {
	super();
	supported = new ContentDescriptor[1];
	supported[0] = new ContentDescriptor(ContentDescriptor.RAW_RTP);
	monoIncrTime = true;
    }

    /**
     * Returns a descriptive name for the plug-in.
     * This is a user readable string.
     */
    public String getName(){
	return "RTP Sync Buffer Multiplexer";
    }

    public Format setInputFormat(Format input, int trackID) {

	// Screen for the supported formats.
	if (!com.sun.media.rtp.RTPSessionMgr.formatSupported(input))
	    return null;

	return super.setInputFormat(input, trackID);
    }
}
