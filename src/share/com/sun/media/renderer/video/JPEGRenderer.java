/*
 * @(#)JPEGRenderer.java	1.17 02/08/21
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

import com.sun.media.*;
import com.sun.media.util.Arch;

/**
 * Renderer for JPEG images.
 * @since JMF 2.0
 */
public class JPEGRenderer extends BasicVideoRenderer implements SlowPlugIn {

    /*************************************************************************
     * Variables and Constants
     *************************************************************************/

    // The descriptive name of this renderer
    private static final String MyName = "JPEG Renderer";

    private VideoFormat supportedJPEG = null;
    private VideoFormat supportedMJPG = null;

    private boolean forceToFail = false;


    public void forceToUse() {
	// Override the forceToFail Flag.
	forceToFail = false;
    }


    /*************************************************************************
     * Constructor
     *************************************************************************/

    public JPEGRenderer() {
	super(MyName);

	// Check if the native jpeg decoder is there.  If it is, disable
	// this renderer by return null from setInputFormat.
	if (plugInExists("com.sun.media.codec.video.jpeg.NativeDecoder", PlugInManager.CODEC)) {
	    try {
		JMFSecurityManager.loadLibrary("jmutil");
		JMFSecurityManager.loadLibrary("jmjpeg");
		forceToFail = true;
	    } catch (Throwable t) { }
	}

	// Prepare supported input formats and preferred format
	supportedJPEG = new VideoFormat(VideoFormat.JPEG,
					null,
					Format.NOT_SPECIFIED,
					Format.byteArray,
					// frame rate
					Format.NOT_SPECIFIED);
	supportedMJPG = new VideoFormat(VideoFormat.MJPG,
					 null,
					 Format.NOT_SPECIFIED,
					 Format.byteArray,
					// frame rate
					Format.NOT_SPECIFIED);


	supportedFormats = new VideoFormat[1];
	supportedFormats[0] = supportedJPEG;
	//supportedFormats[1] = supportedMJPG;
    }

    /*************************************************************************
     * PlugIn methods
     *************************************************************************/
    
    /**
     * Opens the plug-in software or hardware component and acquires
     * necessary resources. If all the needed resources could not be
     * acquired, it throws a ResourceUnavailableException. Data should not
     * be passed into the plug-in without first calling this method.
     */
    public void open() throws ResourceUnavailableException {
    }

    /**
     * Resets the state of the plug-in. Typically at end of media or when media
     * is repositioned.
     */
    public void reset() {
    }
    
    /*************************************************************************
     * Renderer methods
     *************************************************************************/
    
    /**
     * Set the data input format.
     * @return false if the format is not supported.
     */
    public Format setInputFormat(Format format) {
	if (forceToFail)
	    return null;
	if (super.setInputFormat(format) != null) {
	    reset();
	    return format;
	} else
	    return null;
    }

    /**
     * Processes the data and renders it to the output device
     * represented by this renderer.
     */
    public synchronized int doProcess(Buffer buffer) {
	if (component == null)
	    return BUFFER_PROCESSED_OK;
	if (!buffer.getFormat().equals(inputFormat)) {
	    Format in = buffer.getFormat();
	    if (!(in.matches(supportedJPEG)))
		return BUFFER_PROCESSED_FAILED;
	    else {
		inputFormat = (VideoFormat) in;
	    }
	}

	Dimension size = inputFormat.getSize();
	
	Object data = buffer.getData();
	if (!(data instanceof byte[]))
	    return BUFFER_PROCESSED_FAILED;

	Image im = Toolkit.getDefaultToolkit().createImage((byte[])data);
	MediaTracker tracker = new MediaTracker(component);

	Dimension d = component.getSize();
	outWidth = d.width;
	outHeight = d.height;

	tracker.addImage(im, 0);
	try {
	    tracker.waitForAll();
	} catch (Exception e) {}
	
	Graphics g = component.getGraphics();
	if (g != null) {
	    g.drawImage(im, 0, 0, outWidth, outHeight,
			0, 0, size.width, size.height, component);
	}
	return BUFFER_PROCESSED_OK;
    }

    protected void repaint() {
    }
}
