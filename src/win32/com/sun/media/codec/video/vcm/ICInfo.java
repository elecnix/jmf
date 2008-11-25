/*
 * @(#)ICInfo.java	1.3 03/04/24
 *
 * Copyright (c) 1996-2003 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.vcm;

public class ICInfo {

    public String fccType;
    public String fccHandler;
    public int    dwFlags; 
    public int    dwVersion;
    public int    dwVersionICM;
    public String szName;
    public String szDescription;
    public String szDriver;

    public ICInfo() {
	fccType = "vidc";
    }

    public String toString() {
	return "FCC = " + fccHandler +
	    "\t Flags = " + dwFlags +
	    "\t Version = " + dwVersion +
	    "\t Name = " + szName +
	    "\t Description = " + szDescription +
	    "\t Driver = " + szDriver;
    }
}
