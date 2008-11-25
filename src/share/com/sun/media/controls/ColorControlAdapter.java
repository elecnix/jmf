/*
 * @(#)ColorControlAdapter.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import javax.media.*;
import com.sun.media.*;
import com.sun.media.controls.*;
import java.awt.*;

/**
 * A group of controls that modify the color of the video output from this
 * player.
 */
public class ColorControlAdapter extends AtomicControlAdapter
implements ColorControl {


    /*************************************************************************
     * VARIABLES
     *************************************************************************/
    
    NumericControl brightness;
    NumericControl contrast;
    NumericControl saturation;
    NumericControl hue;
    BooleanControl grayscale;
    Control [] controls;

    /*************************************************************************
     * METHODS
     *************************************************************************/

    public ColorControlAdapter(NumericControl b,
			       NumericControl c,
			       NumericControl s,
			       NumericControl h,
			       BooleanControl g,
			       Component comp,
			       boolean def,
			       Control parent) {
	
	super(comp, def, parent);
	
	brightness = b;
	contrast = c;
	saturation = s;
	hue = h;
	grayscale = g;

	int n = 0;
	n += (b == null) ? 0 : 1;
	n += (c == null) ? 0 : 1;
	n += (s == null) ? 0 : 1;
	n += (h == null) ? 0 : 1;
	n += (g == null) ? 0 : 1;
	controls = new Control[n];
	
	n = 0;
	if (b != null)
	    controls[n++] = b;
	if (c != null)
	    controls[n++] = c;
	if (s != null)
	    controls[n++] = s;
	if (h != null)
	    controls[n++] = h;
	if (g != null)
	    controls[n++] = g;
    }

    /*************************************************************************
     * IMPLEMENTS GroupControl
     *************************************************************************/
    
    public Control [] getControls() {
	return controls;
    }

    /*************************************************************************
     * IMPLEMENTS ColorControl
     *************************************************************************/
    
    /**
     * Returns a brightness control object for the video output.
     */
    public NumericControl getBrightness() {
	return brightness;
    }

    /**
     * Returns a contrast control object for the video output.
     */
    public NumericControl getContrast() {
	return contrast;
    }

    /**
     * Returns a color saturation control object for the video output.
     */
    public NumericControl getSaturation() {
	return saturation;
    }

    /**
     * Returns a hue control object for the video output.
     */
    public NumericControl getHue() {
	return hue;
    }

    /**
     * Returns a grayscale control object for the video output. Grayscale
     * output can be turned on or off.
     */
    public BooleanControl getGrayscale() {
	return grayscale;
    }
}
