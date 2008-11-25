/*
 * @(#)V4LAuto.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import com.sun.media.protocol.v4l.*;

public class V4LAuto {

    public V4LAuto() {
        Vector devices = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
	Enumeration enum = devices.elements();
	while (enum.hasMoreElements()) {
	    CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
	    String name = cdi.getName();
	    if (name.startsWith("v4l:"))
		CaptureDeviceManager.removeDevice(cdi);
	}
	
	for (int i = 0; i < 10; i++) {	    
	    autoDetect(i);
	}
    }
    
    protected CaptureDeviceInfo autoDetect(int cardNo) {
	CaptureDeviceInfo cdi = null;
	try {
	    cdi = new V4LDeviceQuery(cardNo);
	    if ( cdi != null && cdi.getFormats() != null &&
		 cdi.getFormats().length > 0) {
		// Commit it to disk. Its a new device
		if (CaptureDeviceManager.addDevice(cdi)) {
		    System.err.println("Added device " + cdi);
		    CaptureDeviceManager.commit();
		}
	    }
	} catch (Throwable t) {
	    System.err.println(t);
	    if (t instanceof ThreadDeath)
		throw (ThreadDeath)t;
	}
	
	return cdi;
    }

    public static void main(String [] args) {
	V4LAuto a = new V4LAuto();
	System.exit(0);
    }
}

