/*
 * @(#)RegisterPlugIn.java	1.10 98/11/13
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.ContentDescriptor;

public class RegisterPlugIn {

    /*
    public static void registerPlugIn(String className) {
	try {
	    Class pic = Class.forName(className);
	    Object instance = pic.newInstance();
	    Format [] inputs = null;
	    Format [] outputs = null;
	    int type;

	    if (instance instanceof Demultiplexer) {
		type = PlugInManager.DEMULTIPLEXER;
		inputs = ((Demultiplexer)instance).getSupportedInputContentDescriptors();
		outputs = new Format[0];
	    } else if (instance instanceof Codec) {
		type = PlugInManager.CODEC;
		inputs = ((Codec)instance).getSupportedInputFormats();
		outputs = ((Codec)instance).getSupportedOutputFormats(null);
	    } else if (instance instanceof Renderer) {
		type = PlugInManager.RENDERER;
		inputs = ((Renderer)instance).getSupportedInputFormats();
		outputs = new Format[0];
	    } else if (instance instanceof Multiplexer) {
		type = PlugInManager.MULTIPLEXER;
		inputs = new Format[0];
		outputs = ((Multiplexer)instance).getSupportedOutputContentDescriptors(null);
	    } else if (instance instanceof Effect) {
		type = PlugInManager.EFFECT;
		inputs = ((Effect)instance).getSupportedInputFormats();
		outputs = ((Effect)instance).getSupportedOutputFormats(null);
	    } else
		type = 0;
	    boolean result = false;
	    
	    if (type != 0) {
		result = PlugInManager.addPlugIn(className, inputs, outputs, type, false);
		if (!result){
		    PlugInManager.removePlugIn(className);
		    result = PlugInManager.addPlugIn(className, inputs, outputs,
						     type, false);
		}
		if (result) {
		    PlugInManager.commit();
		    System.err.println("     Added plugin " + className);
		}
		
	    }
	} catch (ClassNotFoundException cnfe) {
	    System.err.println("     Couldn't find the class - " + className);
	} catch (Exception e) {
	    System.err.println("Exception: " + e);
	}
    }
    */

    public static void main(String [] args) {
	PlugInManager pim = new PlugInManager();
	try {
	    pim.commit();
	} catch (Exception e) {
	    System.err.println("Error committing the PlugInManager");
	}
    }
}
