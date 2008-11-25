/*
 * @(#)V4LDeviceQuery.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.v4l;

import javax.media.*;
import javax.media.Format;
import javax.media.format.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

public class V4LDeviceQuery extends CaptureDeviceInfo {

    transient private V4LCapture capture;
    
    transient private Vector vecFormats = new Vector();
    transient private byte [] buffer = new byte[768 * 576 * 4];
    
    transient protected Dimension [] sizes = {
	new Dimension(160, 120),
	new Dimension(320, 240),
	new Dimension(640, 480),
	new Dimension(176, 144),
	new Dimension(352, 288),
	new Dimension(768, 576)
    };

    private void addFormat(Format fin) {
	Enumeration enum = vecFormats.elements();
	while (enum.hasMoreElements()) {
	    Format f = (Format) enum.nextElement();
	    if (f.equals(fin))
		return;
	}

	//System.err.println("New format = " + fin);
	vecFormats.addElement(fin);
    }

    private void tryFormat(V4LCapture capture, int palette, int width, int height) {
	System.err.println("Trying " + palette + " " + width + " " + height);
	if (capture.setFormat(capture.paletteToDepth(palette),
			      palette, width, height, 30f) < 0)
	    return;

	if (capture.start() < 0)
	    return;

	// Try a few times
	for (int i = 0; i < 5; i++) {
	    if (capture.readNextFrame(buffer, 0, buffer.length) >= 0) {
		// Format is supported, add it
		Format f = capture.paletteToFormat(palette, new Dimension(width, height));
		System.err.println("Format is " + f);
		if (f != null)
		    addFormat(f);
		capture.stop();
		return;
	    }
	}
	// Not supported
	
	capture.stop();
    }
    
    public V4LDeviceQuery(int index) {
	int iPal, iSize;
	int i;
	VCapability vcap = new VCapability();
	capture = new V4LCapture(index);
	capture.getCapability(vcap);
	
	name = "v4l:" + vcap.name + ":" + index;
	System.err.println("Name = " + name);
	tryFormat(capture, VPicture.VIDEO_PALETTE_RGB24, 320, 240);
	for (iPal = VPicture.VIDEO_PALETTE_RGB565; iPal < VPicture.VIDEO_PALETTE_YUV410P; iPal++) {
	    for (iSize = 0; iSize < sizes.length; iSize++) {
		tryFormat(capture, iPal, sizes[iSize].width, sizes[iSize].height);
	    }
	}

	formats = new Format[vecFormats.size()];
	Enumeration enum = vecFormats.elements();
	
	i = 0;
	while (enum.hasMoreElements()) {
	    Format f = (Format) enum.nextElement();
	    formats[i++] = f;
	}
	locator = new MediaLocator("v4l://" + index);
    }
}

