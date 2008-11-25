/*
 * @(#)ProgressControlAdapter.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.*;

/**
 * Adapter class for ProgressControl.
 */
public class ProgressControlAdapter extends AtomicControlAdapter
implements ProgressControl {

    /*************************************************************************
     * VARIABLES
     *************************************************************************/
    
    Control [] controls = null;
    StringControl frc = null;
    StringControl brc = null;
    StringControl vpc = null;
    StringControl apc = null;
    StringControl ac  = null;
    StringControl vc  = null;
    

    /*************************************************************************
     * METHODS
     *************************************************************************/    
    /**
     * Takes in the list of controls to use as progress controls.
     */
    public ProgressControlAdapter(StringControl frameRate,
				  StringControl bitRate,
				  StringControl videoProps,
				  StringControl audioProps,
				  StringControl videoCodec,
				  StringControl audioCodec) {
	super(null, true, null);
	frc = frameRate;
	brc = bitRate;
	vpc = videoProps;
	apc = audioProps;
	vc  = videoCodec;
	ac  = audioCodec;
    }

    /*************************************************************************
     * ProgressControl implementation
     *************************************************************************/
    
    /**
     * Returns the frame rate control.
     */
    public StringControl getFrameRate() {
	return frc;
    }

    /**
     * Returns the bit rate control.
     */
    public StringControl getBitRate() {
	return brc;
    }

    /**
     * Returns the audio properties control.
     */
    public StringControl getAudioProperties() {
	return apc;
    }

    /**
     * Returns the video properties control.
     */
    public StringControl getVideoProperties() {
	return vpc;
    }

    public StringControl getVideoCodec() {
	return vc;
    }

    public StringControl getAudioCodec() {
	return ac;
    }

    
    /*************************************************************************
     * GroupControl implementation
     *************************************************************************/
    
    /**
     * Returns an array that contains all the progress controls.
     */
    public Control[] getControls() {
	if (controls == null) {
	    controls = new Control[6];
	    controls[0] = frc;
	    controls[1] = brc;
	    controls[2] = vpc;
	    controls[3] = apc;
	    controls[4] = ac;
	    controls[5] = vc;
	}
	return controls;
    }
}
