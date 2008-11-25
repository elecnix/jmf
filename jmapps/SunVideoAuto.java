/*
 * @(#)SunVideoAuto.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import com.sun.media.protocol.sunvideo.*;

public class SunVideoAuto {

    private static String DEVICE_PREFIX = "/dev/rtvc";
    private static String PROTOCOL = "sunvideo";
    private static String LOCATOR_PREFIX = PROTOCOL + "://";
    CaptureDeviceInfo [] devices = null;
    int currentID = -1;

    XILCapture xilCap;

    Vector formats = null;
    
    int [] ports = { 1, 2, 0 };		// most likely ports for a device

    int [] scales = { 2, 4, 1 };	// supported scales / sizes


    public SunVideoAuto() {
        Vector devices = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
	Enumeration enum = devices.elements();
	while (enum.hasMoreElements()) {
	    CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
	    String devName = cdi.getLocator().getProtocol();
	    if (devName.equals(PROTOCOL))
		CaptureDeviceManager.removeDevice(cdi);
	}
	
	int nDevices = 0;
	for (int i = 0; i < 7; i++) {
	    File fl = new File(DEVICE_PREFIX + i);
	    if (fl.exists()) {
		doDevice(i);
		nDevices++;
	    }
	}
	try {
	    CaptureDeviceManager.commit();
	    System.err.println("SunVideoAuto: Committed ok");
	} catch (java.io.IOException ioe) {
	    System.err.println("SunVideoAuto: error committing cdm");
	}
    }

    private void addFormat(Format fin) {
	Enumeration enum = formats.elements();
	while (enum.hasMoreElements()) {
	    Format f = (Format) enum.nextElement();
	    if (f.equals(fin))
		return;
	}

	//System.err.println("New format = " + fin);
	formats.addElement(fin);
    }

    private void doDevice(int index) {

	xilCap = new XILCapture(null);
	VideoFormat vf;
	formats = new Vector();
	boolean gotPort = false;

	if (!xilCap.connect(index)) {
	    dummyDevice(index);
	    return;
	}
	
	
	for (int i = 0; i < ports.length; i++) {
	    if (xilCap.setPort(ports[i])) {
		getJpegFormats(i);
		getRGBFormats(i);
	    }
	}
	xilCap.disconnect();

	if (formats.size() > 0)
	    addDevice(index);
	else
	    dummyDevice(index);
    }


    private void getRGBFormats(int index) {
	if (!xilCap.setCompress("RGB"))
	    return;
	for (int i = 0; i < scales.length; i++) {
	    xilCap.setScale(scales[i]);
	    // To get the real values, start the device
	    if (xilCap.start()) {
		Dimension size = new Dimension(xilCap.getWidth(),
						xilCap.getHeight());
		int stride = xilCap.getLineStride();
		int maxbuf = stride * size.width;
		addFormat(new RGBFormat(size, maxbuf, byte[].class,
					15f,
					24,
					3, 2, 1, 3, stride,
					Format.FALSE,
					Format.NOT_SPECIFIED));
	    }
	    xilCap.stop();
	}
    }

    private void getJpegFormats(int index) {
	if (!xilCap.setCompress("Jpeg"))
	    return;
	for (int i = 0; i < scales.length; i++) {
	    xilCap.setScale(scales[i]);
	    // To get the real values, start the device
	    if (xilCap.start()) {
		Dimension size = new Dimension(xilCap.getWidth(),
						xilCap.getHeight());
		// approximate the max for high quality
		int maxbuf = 3 * size.width * size.height;
		addFormat(new VideoFormat(VideoFormat.JPEG, size, maxbuf,
				byte[].class, 15f));
	    }
	    xilCap.stop();
	}
    }

    private void dummyDevice(int index) {
	// Can't get to the device, use the barest formats
	addFormat(new VideoFormat(VideoFormat.JPEG));
	addFormat(new RGBFormat());
	addDevice(index);
    }


    private void addDevice(int index) {

	String name = "SunVideo device " + index;
	String locator = LOCATOR_PREFIX + index;

	Format [] farray = new Format[formats.size()];
	Enumeration enum = formats.elements();

	int i = 0;
	while (enum.hasMoreElements()) {
	    Format f = (Format) enum.nextElement();
	    farray[i++] = f;
	}
	
	CaptureDeviceInfo cdi = new CaptureDeviceInfo(name,
					new MediaLocator(locator), farray);
	CaptureDeviceManager.addDevice(cdi);
    }

    public static void main(String [] args) {
	SunVideoAuto a = new SunVideoAuto();
	System.exit(0);
    }
}

