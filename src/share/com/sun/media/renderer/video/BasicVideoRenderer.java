/*
 * @(#)BasicVideoRenderer.java	1.22 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

import javax.media.*;
import javax.media.control.FrameGrabbingControl;
import javax.media.renderer.VideoRenderer;
import javax.media.Format;
import javax.media.format.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Vector;

import com.sun.media.*;

/**
 * A base implementation for a VideoRenderer
 * @since JMF 2.0
 */
public abstract class BasicVideoRenderer extends BasicPlugIn
implements VideoRenderer, FrameGrabbingControl {

    /*************************************************************************
     * Variables and Constants
     *************************************************************************/
    
    // The descriptive name of this renderer
    protected String name;

    protected transient VideoFormat [] supportedFormats = null;

    protected VideoFormat inputFormat = null;

    protected int outWidth = -1;
    protected int outHeight = -1;
    protected int inWidth = -1;
    protected int inHeight = -1;
    protected Component component = null;
    protected ComponentListener compListener = null;
    protected boolean componentAvailable = false;
    
    protected Rectangle bounds = null;
    protected boolean started = false;

    protected Control [] controls = null;
    protected FrameGrabbingControl frameGrabber = null;
    protected ExtBuffer lastBuffer = new ExtBuffer();
    protected Object lastData = null, lastHdr = null;

    /*************************************************************************
     * Constructor
     *************************************************************************/
    
    public BasicVideoRenderer(String name) {
	this.name = name;
    }

    /*************************************************************************
     * PlugIn methods
     *************************************************************************/
    
    /**
     * Returns a descriptive name for the plug-in.
     * This is a user readable string.
     */
    public String getName() {
	return name;
    }

    /**
     * Lists the possible input formats supported by this plug-in.
     */
    public Format [] getSupportedInputFormats() {
	return supportedFormats;
    }

    /**
     * Opens the plug-in software or hardware component and acquires
     * necessary resources. If all the needed resources could not be
     * acquired, it throws a ResourceUnavailableException. Data should not
     * be passed into the plug-in without first calling this method.
     */
    public void open() throws ResourceUnavailableException {
	// sub class can override
    }

    /**
     * Closes the plug-in component and releases resources. No more data
     * will be accepted by the plug-in after a call to this method. The
     * plug-in can be reinstated after being closed by calling
     * <code>open</code>.
     */
    public void close() {
	// sub class can override
    }
    
    /**
     * Resets the state of the plug-in. Typically at end of media or when media
     * is repositioned.
     */
    public void reset() {
	// sub class can override
    }

    public int process(Buffer inbuffer) {

	if (inbuffer.getLength() == 0)
	    return BUFFER_PROCESSED_OK;

	int result;
	//System.err.println("BasicVideoRenderer.process() not implemented");
	synchronized (lastBuffer) {
	    result = doProcess(inbuffer);
	    // Keep the last buffer
	    if (  result == BUFFER_PROCESSED_OK ) {
		lastBuffer.copy(inbuffer, true);
	    }
	}
	return result;
    }

    protected abstract int doProcess(Buffer buffer);
    
    /*************************************************************************
     * Renderer methods
     *************************************************************************/

    /**
     * Set the data input format.
     * @return null if the format is not supported.
     */
    public Format setInputFormat(Format format) {
	if (matches(format, supportedFormats) != null) {
	    inputFormat = (VideoFormat) format;
	    Dimension size = inputFormat.getSize();
	    if (size != null) {
		inWidth = size.width;
		inHeight = size.height;
	    }
	    return format;
	} else
	    return null;
    }

    public void start() {
	started = true;
    }

    public void stop() {
	started = false;
    }

    /*************************************************************************
     * VideoRenderer methods
     *************************************************************************/
    
    /**
     * Returns an AWT component that it will render to. Returns null
     * if it is not rendering to an AWT component.
     */
    public java.awt.Component getComponent() {
	if (component == null) {
	    // TODO: Try MSHeavyComponent for MS VM
	    try {
		Class mshc = Class.forName("com.sun.media.renderer.video.MSHeavyComponent");
		if (mshc != null)
		    component = (Component) mshc.newInstance();
	    } catch (Throwable t) {
		component = new HeavyComponent();
	    }
	    ((HeavyComponent)component).setRenderer( this );
	    component.setBackground(getPreferredBackground());
	    if (compListener == null)
		compListener = new CompListener();
	    component.addComponentListener(compListener);
	}
	return component;
    }

    /**
     * Requests the renderer to draw into a specified AWT component.
     * Returns false if the renderer cannot draw into the specified
     * component.
     */
    public synchronized boolean setComponent(java.awt.Component comp) {
	reset();
	component = comp;
	if (compListener == null)
	    compListener = new CompListener();
	component.addComponentListener(compListener);
	return true;
    }

    /**
     * Sets the region in the component where the video is to be
     * rendered to. Video is to be scaled if necessary. If <code>rect</code>
     * is null, then the video occupies the entire component.
     */
    public void setBounds(Rectangle rect) {
	bounds = rect;
    }

    /**
     * Returns the region in the component where the video will be
     * rendered to. Returns null if the entire component is being used.
     */
    public Rectangle getBounds() {
	return bounds;
    }

    /*************************************************************************
     * Local methods
     *************************************************************************/

    protected Color getPreferredBackground() {
	return Color.black;
    }
    
    void resized(Component c) {
	if (c != null && c == component) {
	    Dimension d = component.getSize();
	    outWidth = d.width;
	    outHeight = d.height;
	    //repaint();
	}
    }

    protected synchronized void setAvailable(boolean on) {
	componentAvailable = on;
	if (!componentAvailable)
	    removingComponent();
    }

    protected void removingComponent() {
    }

    protected Dimension myPreferredSize() {
	return new Dimension(inWidth, inHeight);
    }

    protected boolean isStarted() {
	return started;
    }

    protected void repaint() {
	System.err.println("repaint call not implemented on this renderer");
    }

    /*************************************************************************
     * Controls methods
     *************************************************************************/

    public Object [] getControls() {
	if (controls != null)
	    return controls;

	frameGrabber = (FrameGrabbingControl) this;
	controls = new Control[1];
	controls[0] = frameGrabber;
	return controls;
    }

    public Component getControlComponent() {
	return null;
    }

    public Buffer grabFrame() {
	synchronized (lastBuffer) {
	    Buffer newBuffer = new Buffer();
	    newBuffer.setFormat(lastBuffer.getFormat());
	    newBuffer.setFlags(lastBuffer.getFlags());
	    newBuffer.setLength(lastBuffer.getLength());
	    newBuffer.setOffset(0);
	    newBuffer.setHeader(lastBuffer.getHeader());
	    newBuffer.setData(lastBuffer.getData());
	    
	    Object data = lastBuffer.getData();
	    int length = lastBuffer.getLength();
	    Object newData;
	    if (data instanceof byte[])
		newData = new byte[length];
	    else if (data instanceof short[])
		newData = new short[length];
	    else if (data instanceof int[])
		newData = new int[length];
	    else
		return newBuffer;
	    System.arraycopy(data, lastBuffer.getOffset(),
			     newData, 0,
			     length);
	    newBuffer.setData(newData);
	    return newBuffer;
	}
    }
    
    /*************************************************************************
     * INNER CLASSES
     *************************************************************************/

    public class CompListener extends ComponentAdapter {
	public void componentResized(ComponentEvent ce) {
	    resized(ce.getComponent());
	}
    }
}
