/*
 * @(#)JavaSoundDetector.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import javax.sound.sampled.*;


public class JavaSoundDetector {

    boolean supported = false;

    public JavaSoundDetector() {
	try {
	    DataLine.Info info = new DataLine.Info(TargetDataLine.class,
						   null,
						   AudioSystem.NOT_SPECIFIED);
	    supported = AudioSystem.isLineSupported(info);

	} catch (Exception ex) {
	    supported = false;
	}
    }

    public boolean isSupported() {
	return supported;
    }

}
	
