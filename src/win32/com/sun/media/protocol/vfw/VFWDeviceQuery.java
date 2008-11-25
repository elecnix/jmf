/*
 * @(#)VFWDeviceQuery.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.vfw;

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import com.sun.media.vfw.*;
import com.sun.media.util.WindowUtil;

public class VFWDeviceQuery extends CaptureDeviceInfo {

    private int capHandle;
    
    private Vector vecFormats = null;
    
    protected Dimension [] sizes = {
	new Dimension(80, 60),
	new Dimension(160, 120),
	new Dimension(176, 144),
	//new Dimension(240, 180),
	new Dimension(320, 240),
	new Dimension(352, 288),
	new Dimension(640, 480),
	new Dimension(768, 576)
    };

    protected String [] knownFourCC = {
	"YUY2",
	"Y411",
	"YVU9",
	"YV12",
	"I420",
	"MJPG",
	"VDEC",
	"VGPX",
	"WINX",
	"MPGI"
    };

    protected int [] knownBitCount = {
	16, // YUY2
	12, // Y411
	9,  // YVU9
	12, // YV12
	12, // I420 // YUV12
	24, // MJPG compressed
	16, // VDEC ???
	24, // VGPX ???
	24, // WINX ???
	24, // MPGI ???
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

    public VFWDeviceQuery(int index) {
	//System.err.println("Before creating window");
	int parentWindow = VFWCapture.createWindow("Crap");
	//System.err.println("After creating window");
	
	capHandle = VFWCapture.capCreateCaptureWindow("Test",
						      parentWindow,
						      0, 0, 320, 240,
						      VFWCapture.getNextID());
	if (capHandle == 0) {
	    VFWCapture.destroyWindow(parentWindow);
	    throw new RuntimeException("Could not create capture window");
	}

	if (!VFWCapture.capDriverConnect(capHandle, index)) {
	    VFWCapture.destroyWindow(capHandle);
	    VFWCapture.destroyWindow(parentWindow);
	    throw new RuntimeException("Unsupported device");
	}
	
	BitMapInfo bmi = new BitMapInfo();
	VideoFormat vf;
	vecFormats = new Vector();

	// Get the default format
	VFWCapture.capGetVideoFormat(capHandle, bmi);
	if ( bmi.biWidth == 0 ||
	     bmi.biHeight == 0 ||
	     (bmi.biBitCount == 8 && bmi.fourcc.equalsIgnoreCase("RGB"))) {
	    // nothing
	} else {
	    vf = bmi.createVideoFormat(byte[].class);
	    addFormat(vf);
	}
	
	// Apply and verify different RGB formats.
	int i;
	int bitCount;
	// 16-bit RGB
	BitMapInfo bmTry = new BitMapInfo("RGB", 0, 0,
					  1, 16, 0, 0, 0);
	for (i = 0; i < sizes.length; i++) {
	    Dimension size = sizes[i];
	    bmTry.biWidth = size.width;
	    bmTry.biHeight = size.height;
	    bmTry.biSizeImage = size.width * size.height * 2;
	    VFWCapture.capSetVideoFormat(capHandle, bmTry);
	    VFWCapture.capGetVideoFormat(capHandle, bmi);
	    if ( bmi.biWidth == 0 ||
		 bmi.biHeight == 0 ) {
		continue;
	    }
	    vf = bmi.createVideoFormat(byte[].class);
	    addFormat(vf);
	}
	
	// 24-bit RGB
	bmTry = new BitMapInfo("RGB", 0, 0,
			       1, 24, 0, 0, 0);
	for (i = 0; i < sizes.length; i++) {
	    Dimension size = sizes[i];
	    bmTry.biWidth = size.width;
	    bmTry.biHeight = size.height;
	    bmTry.biSizeImage = size.width * size.height * 3;
	    VFWCapture.capSetVideoFormat(capHandle, bmTry);
	    VFWCapture.capGetVideoFormat(capHandle, bmi);
	    if ( bmi.biWidth == 0 ||
		 bmi.biHeight == 0 ) {
		continue;
	    }
	    vf = bmi.createVideoFormat(byte[].class);
	    addFormat(vf);
	}
	/*
	// 32-bit RGB
	bmTry = new BitMapInfo("RGB", 0, 0,
			       1, 32, 0, 0, 0);
	for (i = 0; i < sizes.length; i++) {
	    Dimension size = sizes[i];
	    bmTry.biWidth = size.width;
	    bmTry.biHeight = size.height;
	    bmTry.biSizeImage = size.width * size.height * 4;
	    VFWCapture.capSetVideoFormat(capHandle, bmTry);
	    VFWCapture.capGetVideoFormat(capHandle, bmi);
	    if ( bmi.biWidth == 0 ||
		 bmi.biHeight == 0 ) {
		continue;
	    }
	    vf = bmi.createVideoFormat(byte[].class);
	    addFormat(vf);
	}
	*/
	String partName = VFWCapture.capDriverGetName(capHandle);

	// Special case for VGPX
	boolean hasVGPX = false;
	int startCode = 0;
	int endCode = knownFourCC.length;
	// Special case for Color QuickCam 2
	if (partName.startsWith("Color QuickCam video")) {
	    startCode = 6;
	    endCode = 7;
	}
	if (partName.startsWith("Logitech QuickCam")) {
	    startCode = 6;
	    endCode = 6;
	}
	// Apply and verify other known formats.
	for (int j = startCode; j < endCode; j++) {
	    //System.err.println("Trying " + knownFourCC[j]);
	    bitCount = knownBitCount[j];
	    bmTry = new BitMapInfo(knownFourCC[j], 0, 0,
				   1, bitCount, 0, 0, 0);
	    for (i = 0; i < sizes.length; i++) {
		Dimension size = sizes[i];
		bmTry.biWidth = size.width;
		bmTry.biHeight = size.height;
 		if (knownFourCC[j].equals("VGPX") &&
 		    (size.width == 80 || size.width == 176 ||
 		     size.width > 320))
 		    continue;
		bmTry.biSizeImage = size.width * size.height * bitCount / 8;
		//System.err.println("Trying " + bmTry);
		VFWCapture.capSetVideoFormat(capHandle, bmTry);
		VFWCapture.capGetVideoFormat(capHandle, bmi);
		if ( bmi.biWidth == 0 ||
		     bmi.biHeight == 0 ) {
		    continue;
		}
		vf = bmi.createVideoFormat(byte[].class);
		if (knownFourCC[j].equals("VGPX"))
		    hasVGPX = true;
		addFormat(vf);
	    }
	    if (hasVGPX)
		break;
	}

	String driverVerson = VFWCapture.capDriverGetVersion(capHandle);
	VFWCapture.capDriverDisconnect(capHandle);
	VFWCapture.destroyWindow(capHandle);
	VFWCapture.destroyWindow(parentWindow);

	//[This check doesn't seem to work. Some good devices also return a null
	if (/*partName != null && !partName.equals("null")*/ true) {
	    
	    name = "vfw:" + partName + ":" + index;
	    String strLocator = "vfw://" + index;
	    
	    //System.err.println("Registering Device " + name);
	    //System.err.println("Device URL = " + locator);
	    
	    formats = new Format[vecFormats.size()];
	    Enumeration enum = vecFormats.elements();
	    
	    i = 0;
	    while (enum.hasMoreElements()) {
		Format f = (Format) enum.nextElement();
		formats[i++] = f;
	    }

	    locator = new MediaLocator(strLocator);
	} else {
	    throw new RuntimeException("Unsupported device");
	}
    }
}

