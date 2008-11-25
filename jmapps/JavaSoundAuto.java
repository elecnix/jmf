/*
 * @(#)JavaSoundAuto.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import java.util.Vector;
import java.util.Enumeration;
import com.sun.media.protocol.javasound.JavaSoundSourceStream;

public class JavaSoundAuto {
    private static final String detectClass = "JavaSoundDetector";
    CaptureDeviceInfo [] devices = null;

    public static void main(String[] args) {
       new JavaSoundAuto();
       System.exit(0);
    }

    public JavaSoundAuto() {
	boolean supported = false;
	// instance JavaSoundDetector to check is javasound's capture is availabe
	try {
	    Class cls = Class.forName(detectClass);
	    JavaSoundDetector detect = (JavaSoundDetector)cls.newInstance();
	    supported = detect.isSupported();
	} catch (Throwable t) {
	    supported = false;
	    // t.printStackTrace();
	}
	
	System.out.println("JavaSound Capture Supported = " + supported);

	if (supported) {
	    // It's there, start to register JavaSound with CaptureDeviceManager
	    Vector devices = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
	    
	    // remove the old javasound capturers
	    String name;
	    Enumeration enum = devices.elements();
	    while (enum.hasMoreElements()) {
		CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();	
		name = cdi.getName();
		if (name.startsWith("JavaSound"))
		    CaptureDeviceManager.removeDevice(cdi);
	    }
	    
	    // collect javasound capture device info from JavaSoundSourceStream
	    // and register them with CaptureDeviceManager
	    CaptureDeviceInfo[] cdi =  com.sun.media.protocol.javasound.JavaSoundSourceStream.listCaptureDeviceInfo();
	    if ( cdi != null ){
		for (int i = 0; i < cdi.length; i++)
		    CaptureDeviceManager.addDevice(cdi[i]);
		try {
		    CaptureDeviceManager.commit();
		    System.out.println("JavaSoundAuto: Committed ok");
		} catch (java.io.IOException ioe) {
		    System.err.println("JavaSoundAuto: error committing cdm");
		}
	    }
		
	}
    }
}
	
	

    
