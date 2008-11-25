/*
 * @(#)AudioCodecChain.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import javax.media.format.*;
import javax.media.format.*;
import javax.media.*;
import java.util.Vector;
import java.awt.Component;
import java.awt.Dimension;
import com.sun.media.ui.GainControlComponent;

public class AudioCodecChain extends CodecChain {

    public AudioCodecChain(AudioFormat input) throws UnsupportedFormatException {
	AudioFormat af = input;

	if (!buildChain(input))
	    throw new UnsupportedFormatException(input);

	// Do not open the renderer as yet.
	// We'll only do it when the data is being prefetched.
	renderer.close();

	firstBuffer = false;
    }

    Component gainComp = null;

    public Component getControlComponent() {
	if (gainComp != null)
	    return gainComp;

	Control c = (Control)renderer.getControl("javax.media.GainControl");
	if (c != null)
	    gainComp = new GainControlComponent((GainControl)c);
	return gainComp;
    }

    public void reset() {
    }
}
