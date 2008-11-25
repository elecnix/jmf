/*
 * @(#)JMFPropertiesGen.java	1.30 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;
import javax.media.CaptureDeviceInfo;

/**
 * A simple utility to create jmf.properties
 * Usage:
 *       java JMFPropertiesGen         [ for native+java plugins ]
 *       java JMFPropertiesGen java    [ for only java plugins ]
 */
public class JMFPropertiesGen {
    public static void main(String[] args) {
	
	String [] nativeList = RegistryGen.nativePlugins;
	String [] defaultList = RegistryGen.defaultPlugins;
	String [] mergedList;
	boolean allJava = false;
	
	if (args.length > 0 && args[0].equalsIgnoreCase("java")) {
	    allJava = true;
	} 
	
	mergedList = RegistryGen.findAllPlugInList(allJava, defaultList, nativeList);
	RegistryGen.registerPlugIns(mergedList);

	// The following are code to register specific capture devices:
	if (!allJava) {
	    String fileSeparator = System.getProperty("file.separator");
	    if (fileSeparator.equals("/")) {
		Registry.set("secure.cacheDir", "/tmp");
	    } else {
		Registry.set("secure.cacheDir", "C:" + fileSeparator + "temp");
	    }
	    try {
		Registry.commit();
	    } catch (Exception e) {
	    }
	    // Register JavaSound capturer:
	    // CaptureDeviceInfo cdis[] = com.sun.media.protocol.javasound.DataSource.listCaptureDeviceInfo();
	    // RegistryGen.registerCaptureDevices(cdis);
	}
	System.exit(0);
    }
}
