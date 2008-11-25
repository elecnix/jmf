/*
 * @(#)MergingCDPushBDS.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
 
package com.ibm.media.protocol;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.control.FormatControl;
import java.util.Vector;

public class MergingCDPushBDS extends MergingPushBufferDataSource implements CaptureDevice {

    FormatControl [] fcontrols = null;
    
    public MergingCDPushBDS(PushBufferDataSource[] sources) {
	super(sources);
	consolidateFormatControls(sources);
    }

    public FormatControl [] getFormatControls() {
	return fcontrols;
    }

    public CaptureDeviceInfo getCaptureDeviceInfo() {
	return null;
    }
    
    protected void consolidateFormatControls(PushBufferDataSource[] sources) {
	Vector fcs = new Vector(1);
	for (int i = 0; i < sources.length; i++) {
	    if (sources[i] instanceof CaptureDevice) {
		CaptureDevice cd = (CaptureDevice) sources[i];
		FormatControl [] cdfcs = cd.getFormatControls();
		for (int j = 0; j < cdfcs.length; j++)
		    fcs.addElement(cdfcs[j]);
	    }
	}
	if (fcs.size() > 0) {
	    fcontrols = new FormatControl[fcs.size()];
	    for (int f = 0; f < fcs.size(); f++)
		fcontrols[f] = (FormatControl) fcs.elementAt(f);
	} else {
	    fcontrols = new FormatControl[0];
	}
    }	
}


