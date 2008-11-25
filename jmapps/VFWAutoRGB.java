/*
 * @(#)VFWAutoRGB.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

import javax.media.*;
import javax.media.Format;
import javax.media.format.*;

import com.sun.media.vfw.*;

public class VFWAutoRGB {

    final Dimension size160 = new Dimension(160, 120);
    final Dimension size176 = new Dimension(176, 144);
    final Dimension size320 = new Dimension(320, 240);
    final Dimension size352 = new Dimension(352, 288);
    final Dimension size640 = new Dimension(640, 480);
    final Dimension size768 = new Dimension(768, 576);

    Dimension [] sizes = new Dimension [] {
	size160, size176, size320, size352, size640, size768
    };
    
    VideoFormat [] videoFormats;

    public VFWAutoRGB() {
	MediaLocator locator;
	String name;
	
	prepareFormats();
	
        Vector devices = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
        Enumeration enum = devices.elements();

        while (enum.hasMoreElements()) {
            CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
            name = cdi.getName();
            if (name.startsWith("vfw:"))
                CaptureDeviceManager.removeDevice(cdi);
        }

	System.out.println("\nWarning:\nThis program assumes that your " +
			   "capture devices support RGB capture\n");

        int nDevices = 0;
        for (int i = 0; i < 10; i++) {
            name = VFWCapture.capGetDriverDescriptionName(i);
            if (name != null && name.length() > 1) {
                System.out.println("Found device " + name);
                System.out.println("Registering for RGB capture");

		name = "vfw:" + name + ":" + i;
		locator = new MediaLocator("vfw://" + i);
		CaptureDeviceInfo cdi = new CaptureDeviceInfo(name,
							      locator,
							      videoFormats);
		CaptureDeviceManager.addDevice(cdi);
		
                nDevices++;
            }
        }
	try {
	    CaptureDeviceManager.commit();
	} catch (IOException ioe) {
	    System.err.println("Error committing---");
	    ioe.printStackTrace();
	}
    }

    protected void prepareFormats() {
	videoFormats = new VideoFormat[sizes.length * 2]; // For RGB16/24

	for (int i = 0; i < sizes.length; i++) {
	    Dimension size = sizes[i];
	    videoFormats[i] =
		new RGBFormat(size,
			      size.width * size.height * 3,
			      Format.byteArray,
			      Format.NOT_SPECIFIED,
			      24,
			      3, 2, 1,
			      3, size.width * 3,
			      RGBFormat.TRUE,
			      Format.NOT_SPECIFIED);
	    videoFormats[i+sizes.length] =
		new RGBFormat(size,
			      size.width * size.height * 2,
			      Format.byteArray,
			      Format.NOT_SPECIFIED,
			      16,
			      0x7C00, 0x03E0, 0x001F,
			      2, size.width * 2,
			      RGBFormat.TRUE,
			      RGBFormat.LITTLE_ENDIAN);
	}			    
    }
    
    public static void main(String [] args) {
        VFWAutoRGB a = new VFWAutoRGB();
        System.exit(0);
    }
}
	
