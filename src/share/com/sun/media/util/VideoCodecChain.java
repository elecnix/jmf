/*
 * @(#)VideoCodecChain.java	1.18 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import javax.media.format.*;
import javax.media.format.*;
import javax.media.renderer.VideoRenderer;
import javax.media.*;
import java.util.Vector;
import java.awt.Component;
import java.awt.Dimension;

public class VideoCodecChain extends CodecChain {

    public VideoCodecChain(VideoFormat vf) throws UnsupportedFormatException {
	Dimension size = vf.getSize();
	VideoFormat inputFormat = vf;

	if (size == null || vf == null)
	    throw new UnsupportedFormatException(vf);

	if (!buildChain(vf))
	    throw new UnsupportedFormatException(vf);
    }


    /**
     * MPEG video is not raw format. However, MonitorAdapter is
     * only setting the render flag true for I frames on MPEG video
     * so it can be treated as raw. Otherwise all frames must go
     * to the decoder and the current decoder overloads the CPU.
     */
    boolean isRawFormat(Format format) {
	// If raw format, no need to decode just to keep state
	return ((format instanceof RGBFormat || format instanceof YUVFormat ||
		(format.getEncoding() != null &&
		(format.getEncoding().equalsIgnoreCase(VideoFormat.JPEG) ||
		format.getEncoding().equalsIgnoreCase(VideoFormat.MPEG)))));
    }


    public Component getControlComponent() {
	if (renderer instanceof VideoRenderer)
	    return ((VideoRenderer)renderer).getComponent();
	else
	    return null;
    }
}
