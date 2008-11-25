/*
 * @(#)GenerateImage.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.*;
import java.awt.image.*;

public class GenerateImage {

    private IndexColorModel icm = null;
    private DirectColorModel dcm = null;

    private Image image = null;
    private byte [] data = null;
    private int width;
    private int height;
    byte reds[]   = new byte[256];
    byte greens[] = new byte[256];
    byte blues[]  = new byte[256];

    private native int getColors(byte [] colors, int component);
    private native boolean generateImage(String imageName);
    
    public GenerateImage() {
	int ncolors = getColors(reds, 0);
	getColors(greens, 1);
	getColors(blues, 2);
	icm = new IndexColorModel(8, 256, reds, greens, blues, 0);
    }
    
    public Image getImage(String imageName) {
	image = null;
	data = null;
	if (generateImage(imageName)) {
	    createImage();
	    return image;
	} else
	    return  null;
    }

    protected synchronized void createBuffer(int w, int h) {
	width = w;
	height = h;
	data = new byte[w * h];
    }

    protected synchronized void createImage() {
	MemoryImageSource mis = new MemoryImageSource(width, height,
						      icm, data, 0, width);
	Toolkit tk = Toolkit.getDefaultToolkit();
	image = tk.createImage(mis);
	tk.prepareImage(image, width, height, null);
	
    }
}
