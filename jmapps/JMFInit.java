/*
 * @(#)JMFInit.java	1.14 03/04/30
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import com.sun.media.util.Registry;
import java.awt.*;
import java.io.*;
import java.util.Vector;
import javax.media.*;
import javax.media.format.AudioFormat;
import com.sun.media.ExclusiveUse;

public class JMFInit extends Frame implements Runnable {

    private String tempDir = "/tmp";
    private TextArea textBox;
    private boolean done = false;
    private String userHome;
    
    public JMFInit(String [] args) {
	super("Initializing JMF...");

	createGUI();

	updateTemp(args);

	if (args.length > 2) {
	    boolean allow = false;
	    if (args[2].equals("1"))
		allow = true;
	    Registry.set("secure.allowCaptureFromApplets", new Boolean(allow));
	    allow = false;
	    if (args.length > 3) {
		if (args[3].equals("1"))
		    allow = true;
		Registry.set("secure.allowSaveFileFromApplets", new Boolean(allow));
	    }
	    try {
		Registry.commit();
	    } catch (Exception e) {
	    }
	}

	Thread detectThread = new Thread(this);
	detectThread.run();

	int slept = 0;
	while (!done && slept < 60 * 1000 * 2) {
	    try {
		Thread.currentThread().sleep(500);
	    } catch (InterruptedException ie) {
	    }
	    slept += 500;
	}
	
	if (!done) {
	    message("Aborting detection!");
	}
	
	try {
	    Thread.currentThread().sleep(2000);
	} catch (InterruptedException ie) {
	}
    }

    public void run() {
	detectDirectAudio();
	detectS8DirectAudio();
	detectCaptureDevices();
	done = true;
    }
    
    private void updateTemp(String [] args) {
	if (args.length > 0) {
	    tempDir = args[0];
	    message("Setting cache directory to " + tempDir);
	    Registry r = new Registry();
	    try {
		r.set("secure.cacheDir", tempDir);
		r.commit();
		message("Updated registry");
	    } catch (Exception e) {
		message("Couldn't update registry!");
	    }
	}
    }

    private void detectCaptureDevices() {
	// check if JavaSound capture is available
	message("Looking for Audio capturer");
	Class dsauto = null;
	try { 
	    dsauto = Class.forName("DirectSoundAuto");
	    dsauto.newInstance();
	    message("Finished detecting DirectSound capturer");
	} catch (ThreadDeath td) {
	    throw td;
	} catch (Throwable t ) {
	}

	Class jsauto = null;
	try { 
	    jsauto = Class.forName("JavaSoundAuto");
	    jsauto.newInstance();
	    message("Finished detecting javasound capturer");
	} catch (ThreadDeath td) {
	    throw td;
	} catch (Throwable t ) {
	    message("JavaSound capturer detection failed!");
	}

	// Check if VFWAuto or SunVideoAuto is available
	message("Looking for video capture devices");
	Class auto = null;
        Class autoPlus = null;
	try {
	    auto = Class.forName("VFWAuto");
	} catch (Exception e) {
	}
	if (auto == null) {
	    try {
		auto = Class.forName("SunVideoAuto");
	    } catch (Exception ee) {
	    }
	    try {
		autoPlus = Class.forName("SunVideoPlusAuto");
	    } catch (Exception ee) {
	    }
	}
	if (auto == null) {
	    try {
		auto = Class.forName("V4LAuto");
	    } catch (Exception ee) {
	    }
	}
	try {
	    Object instance = auto.newInstance();
            if (autoPlus != null) {
                Object instancePlus = autoPlus.newInstance();
            }
	    message("Finished detecting video capture devices");
	} catch (ThreadDeath td) {
	    throw td;
	} catch (Throwable t) {
	    message("Capture device detection failed!");
	}
    }

    private void detectDirectAudio() {
	Class cls;
	int plType = PlugInManager.RENDERER;
	String dar = "com.sun.media.renderer.audio.DirectAudioRenderer";
	try {
	    // Check if this is the Windows Performance Pack - hack
	    cls = Class.forName("VFWAuto");
	    // Check if DS capture is supported, otherwise fail DS renderer
	    // since NT doesn't have capture
	    cls = Class.forName("com.sun.media.protocol.dsound.DSound");
	    // Find the renderer class and instantiate it.
	    cls = Class.forName(dar);
	    
	    Renderer rend = (Renderer) cls.newInstance();
	    try {
		// Set the format and open the device
		AudioFormat af = new AudioFormat(AudioFormat.LINEAR,
						 44100, 16, 2);
		rend.setInputFormat(af);
		rend.open();
		Format [] inputFormats = rend.getSupportedInputFormats();
		// Register the device
		PlugInManager.addPlugIn(dar, inputFormats, new Format[0],
					plType);
		// Move it to the top of the list
		Vector rendList =
		    PlugInManager.getPlugInList(null, null, plType);
		int listSize = rendList.size();
		if (rendList.elementAt(listSize - 1).equals(dar)) {
		    rendList.removeElementAt(listSize - 1);
		    rendList.insertElementAt(dar, 0);
		    PlugInManager.setPlugInList(rendList, plType);
		    PlugInManager.commit();
		    //System.err.println("registered");
		}
		rend.close();
	    } catch (Throwable t) {
		//System.err.println("Error " + t);
	    }
	} catch (Throwable tt) {
	}
    }

    private void detectS8DirectAudio() {
	Class cls;
	int plType = PlugInManager.RENDERER;
	String dar = "com.sun.media.renderer.audio.DirectAudioRenderer";
	try {
	    // Check if this is the solaris Performance Pack - hack
	    cls = Class.forName("SunVideoAuto");

	    // Find the renderer class and instantiate it.
	    cls = Class.forName(dar);
	    
	    Renderer rend = (Renderer) cls.newInstance();

	    if ( rend instanceof ExclusiveUse &&
                 !((ExclusiveUse)rend).isExclusive()) {
		// sol8+, DAR supports mixing
		Vector rendList = PlugInManager.getPlugInList(null, null, plType);
		int listSize = rendList.size();
		boolean found = false;
		String rname = null;

		for ( int i = 0; i < listSize; i++) {
		    rname = (String)(rendList.elementAt(i));
		    if ( rname.equals(dar) ) { // DAR is in the registry
			found = true;
			rendList.removeElementAt(i);
			break;
		    }
		}
		
		if ( found ) {
		    rendList.insertElementAt(dar, 0);
		    PlugInManager.setPlugInList(rendList, plType);
		    PlugInManager.commit();
		}
	    }
	} catch (Throwable tt) {
	}
    }

    private void message(String mesg) {
	textBox.append(mesg + "\n");
    }

    private void createGUI() {
	textBox = new TextArea(5, 50);
	add("Center", textBox);
	textBox.setEditable(false);
	addNotify();
	setVisible(true);
	pack();
    }
    
    public static void main(String [] args) {
	new JMFInit(args);
	System.exit(0);
    }
}
