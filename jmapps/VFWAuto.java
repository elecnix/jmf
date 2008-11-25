/*
 * @(#)VFWAuto.java	1.4 03/04/25
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;

import com.sun.media.protocol.vfw.*;
import com.sun.media.util.WindowUtil;


public class VFWAuto {

    public VFWAuto() {
        Vector devices = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
        Enumeration enum = devices.elements();

        while (enum.hasMoreElements()) {
            CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
            String name = cdi.getName();
            if (name.startsWith("vfw:"))
                CaptureDeviceManager.removeDevice(cdi);
        }
	
        int nDevices = 0;
        for (int i = 0; i < 10; i++) {
            String name = VFWCapture.capGetDriverDescriptionName(i);
            if (name != null && name.length() > 1) {
                System.err.println("Found device " + name);
                System.err.println("Querying device. Please wait...");
                com.sun.media.protocol.vfw.VFWSourceStream.autoDetect(i);
                nDevices++;
            }
        }
    }

    public static void main(String [] args) {
        VFWAuto a = new VFWAuto();
        System.exit(0);
    }
}

