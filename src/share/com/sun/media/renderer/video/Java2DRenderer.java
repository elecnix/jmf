/*
 * @(#)Java2DRenderer.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

import javax.media.*;
import javax.media.renderer.VideoRenderer;
import javax.media.Format;
import javax.media.format.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Vector;
import java.awt.color.ColorSpace;
import java.awt.geom.*;

import com.sun.media.*;
import com.sun.media.util.Arch;

/**
 * Renderer for RGB images using BufferedImage
 */
public class Java2DRenderer implements Blitter {

    /*************************************************************************
     * Variables and Constants
     *************************************************************************/

    private transient AffineTransformOp savedATO = null;
    private transient DirectColorModel dcm = null;
    private transient Image destImage = null;

    /*************************************************************************
     * Constructor
     *************************************************************************/

    public Java2DRenderer() {
	try {
	    Class.forName("java.awt.Graphics2D");
	} catch (ClassNotFoundException e) {
	    throw new RuntimeException("No Java2D");
	}
    }

    /**
     * Processes the data and creates an image to be rendered
     */
    public Image process(Buffer buffer, Object cacheInputImage,
			 Object cacheOutputImage, Dimension size) {
	return (Image) cacheInputImage;
    }
    
    public synchronized void draw(Graphics g, Component component, Image lastImage,
				  int dx, int dy, int dw, int dh,
				  int sx, int sy, int sw, int sh) {
	if (lastImage == null || dw < 1 || dh < 1)
	    return;
	if (savedATO == null) {
	    AffineTransform at = new AffineTransform((float)dw / sw, 0,
						     0, (float)dh / sh,
						     0, 0);
	    AffineTransformOp ato = new AffineTransformOp(at, null);

	    savedATO = ato;
	    destImage = ato.createCompatibleDestImage((BufferedImage)lastImage,
						      (ColorModel)dcm);
	}
	((AffineTransformOp)savedATO).filter((BufferedImage)lastImage, (BufferedImage)destImage);
	if (g != null && lastImage != null && g instanceof Graphics2D) {
	    ((Graphics2D)g).drawImage(destImage, 0, 0, component);
	}
    }
    
    public int newData(Buffer buffer, Vector cacheInputImage,
			Vector cacheOutputImage, Vector cacheInputData) {
	Object data = buffer.getData();
	if (!(data instanceof int[]))
	    return -1;
	RGBFormat format = (RGBFormat) buffer.getFormat();
	int rMask, gMask, bMask;
	/*
	rMask = 0x000000FF;
	gMask = 0x0000FF00;
	bMask = 0x00FF0000;
	*/
	
	rMask = format.getRedMask();
	gMask = format.getGreenMask();
	bMask = format.getBlueMask();
	int [] masks = new int[3];
	masks[0] = rMask;
	masks[1] = gMask;
	masks[2] = bMask;
	
	DataBuffer db = new DataBufferInt((int[])data,
					  format.getLineStride() *
					    format.getSize().height);
		
	SampleModel sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,
							  format.getLineStride(),
							  format.getSize().height,
							  masks);
	WritableRaster wr = Raster.createWritableRaster(sm, db, new Point(0, 0));

	dcm = new DirectColorModel(24, rMask, gMask, bMask);
	BufferedImage sourceImage = new BufferedImage((ColorModel)dcm, wr, true, null);

	cacheInputData.addElement(data);
	cacheInputImage.addElement(sourceImage);
	cacheOutputImage.addElement(sourceImage);  // dummy
	synchronized (this) {
	    savedATO = null;
	}
	return cacheInputImage.size() - 1;
    }

    public void resized(Component c) {
	savedATO = null;
    }
}
