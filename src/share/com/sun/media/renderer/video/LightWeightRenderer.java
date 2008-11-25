/*
 * @(#)LightWeightRenderer.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

/**
 * Renderer for RGB images using LightWeight components.
 */
public class LightWeightRenderer extends AWTRenderer {

    // The descriptive name of this renderer
    private static final String MyName = "LightWeight Renderer";

    /*************************************************************************
     * Constructor
     *************************************************************************/

    public LightWeightRenderer() {
	super(MyName);
    }

    public boolean isLightWeight() {
	return true;
    }
}
