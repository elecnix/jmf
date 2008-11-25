/*
 * @(#)DirectSoundAuto.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.AudioFormat;
import java.util.Vector;
import java.util.Enumeration;
import com.sun.media.protocol.dsound.DirectSoundStream;

public class DirectSoundAuto {
    private static final String detectClass = "com.sun.media.protocol.dsound.DSound";
    CaptureDeviceInfo [] devices = null;

    public static void main(String[] args) {
       new DirectSoundAuto();
       System.exit(0);
    }

    private boolean supports(AudioFormat af) {
	try {
	    com.sun.media.protocol.dsound.DSound ds;
	    ds = new com.sun.media.protocol.dsound.DSound(af, 1024);
	    ds.open();
	    ds.close();
	} catch (Exception e) {
	    System.err.println(e);
	    return false;
	}
	return true;
    }
    
    public DirectSoundAuto() {
	boolean supported = false;
	// instance JavaSoundDetector to check is javasound's capture is availabe
	try {
	    Class cls = Class.forName(detectClass);
	    supported = true;
	} catch (Throwable t) {
	    supported = false;
	    // t.printStackTrace();
	}
	
	System.out.println("DirectSound Capture Supported = " + supported);

	if (supported) {
	    // It's there, start to register JavaSound with CaptureDeviceManager
	    Vector devices = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
	    
	    // remove the old direct sound capturers
	    String name;
	    Enumeration enum = devices.elements();
	    while (enum.hasMoreElements()) {
		CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();	
		name = cdi.getName();
		if (name.startsWith(com.sun.media.protocol.dsound.DataSource.NAME))
		    CaptureDeviceManager.removeDevice(cdi);
	    }
	    int LE = AudioFormat.LITTLE_ENDIAN;
	    int SI = AudioFormat.SIGNED;
	    int US = AudioFormat.UNSIGNED;
	    int UN = AudioFormat.NOT_SPECIFIED;
	    float [] Rates = new float[] {
		48000, 44100, 32000, 22050, 16000, 11025, 8000
	    };
	    Vector formats = new Vector(4);
	    for (int rateIndex = 0; rateIndex < Rates.length; rateIndex++) {
		float rate = Rates[rateIndex];
		AudioFormat af;
		af = new AudioFormat(AudioFormat.LINEAR, rate, 16, 2, LE, SI);
		if (supports(af)) formats.addElement(af);
		af = new AudioFormat(AudioFormat.LINEAR, rate, 16, 1, LE, SI);
		if (supports(af)) formats.addElement(af);
		af = new AudioFormat(AudioFormat.LINEAR, rate, 8, 2, UN, US);
		if (supports(af)) formats.addElement(af);
		af = new AudioFormat(AudioFormat.LINEAR, rate, 8, 1, UN, US);
		if (supports(af)) formats.addElement(af);
	    }

	    AudioFormat [] formatArray = new AudioFormat[formats.size()];
	    for (int fa = 0; fa < formatArray.length; fa++)
		formatArray[fa] = (AudioFormat) formats.elementAt(fa);

	    CaptureDeviceInfo cdi = new CaptureDeviceInfo(
		com.sun.media.protocol.dsound.DataSource.NAME,
		new MediaLocator("dsound://"),
		formatArray);
	    CaptureDeviceManager.addDevice(cdi);
	    try {
		CaptureDeviceManager.commit();
		System.out.println("DirectSoundAuto: Committed ok");
	    } catch (java.io.IOException ioe) {
		System.err.println("DirectSoundAuto: error committing cdm");
	    }
	}
    }
}
	
	

    
