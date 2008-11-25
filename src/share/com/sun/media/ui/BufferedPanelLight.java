/*
 * @(#)BufferedPanelLight.java	1.1 98/07/30
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class BufferedPanelLight extends Container {

    public BufferedPanelLight(LayoutManager layout) {
//	super(layout);
        super ();
        this.setLayout ( layout );

	buffered = true;
	autoFlushing = true;
	background = null;
	windowCreated = false;
	buffer = null;
	bufferGraphics = null;
	damage = new Region();
    }

    public BufferedPanelLight() {
	this(null);
    }
    
    public boolean isBuffered() {
	return buffered;
    }

    public void setBuffered(boolean buffered) {
	if (buffered != this.buffered) {
	    this.buffered = buffered;
	    if (buffered) {
		repaint();
	    }
	}
    }

    public boolean isAutoFlushing() {
	return autoFlushing;
    }
    
    public void setAutoFlushing(boolean flushing) {
	if (flushing != autoFlushing) {
	    autoFlushing = flushing;
	}
    }
    
    public Image getBackgroundTile() {
	return background;
    }

    public void setBackgroundTile(Image background) {
	this.background = background;
	repaint();
    }

    public void addNotify() {
	super.addNotify();
	windowCreated = true;
	if (buffered) {
	    createBufferImage();
	    repaint();
	}
    }

    public void reshape(int x, int y, int width, int height) {
	Rectangle old = getBounds();
	
	super.reshape(x, y, width, height);
	if (windowCreated && 
	    ((width != old.width) || (height != old.height))) {
	    if (buffered) {
		// Resize the image
		createBufferImage();
		repaint();
	    }
	}
    }

    public void flushBuffer() {
	Dimension size = getSize();
	super.repaint(0, 0, 0, size.width, size.height);

    }
    
    void createBufferImage() {
	Dimension size = getSize();
	
	if ((size.width > 0) && (size.height > 0)) {

	    buffer = createImage(size.width, size.height);
	    if (buffer != null) {
		bufferGraphics = buffer.getGraphics();
	    }
	}
    }

    protected void renderBuffer() {
	Region rects;
	Rectangle rect;

	// Anything need repainting?
	if (damage.isEmpty()) {
	    return;
	}
	
	// Make sure we have a buffer to flush
	if (buffer == null) {
	    return;
	}

	// Keep a copy of the damage region and then reset it
	synchronized(damage) {
	    rects = damage;
	    damage = new Region();
	}
	
	// Cycle through each damage rectangle and paint the children
	// back to front into the buffer image
	for (Enumeration e = rects.rectangles(); e.hasMoreElements(); ) {
	    rect = (Rectangle)(e.nextElement());
	    render(rect);
	}
    }

    protected void render(Rectangle rect) {
	
	Rectangle clip;
	Component children[] = getComponents();
	Component child;
	Graphics g;

	synchronized(buffer) {
	    // Clear the background
	    bufferGraphics.setClip(rect);
	    paintBackground(bufferGraphics);
	    bufferGraphics.setColor(getForeground());

	    // Paint the children if they are visible and intersect
	    // the current rectangle
	    for (int c = children.length - 1; c >= 0; c--) {
		child = children[c];
		if (isLightweight(child) && child.isVisible()) {
		    clip = child.getBounds();
		    if (clip.intersects(rect)) {
			g = bufferGraphics.create(clip.x, clip.y,
						  clip.width, clip.height);
			child.paint(g);
			g.dispose();
		    }
		}
	    }
	    bufferGraphics.setClip(0, 0, getSize().width, getSize().height);
	}
    }

    protected void paintBackground(Graphics g) {
	Dimension size = getSize();

	if (background == null) {
	    // Just fill with the background color
	    g.setColor(getBackground());
	    g.fillRect(0, 0, size.width, size.height);
	} else {
	    // Tile the background image to cover the window
	    Rectangle tile = new Rectangle(0, 0, 
					   background.getWidth(this),
					   background.getHeight(this));
	    Rectangle clip = g.getClipBounds();
	    
	    while (tile.y < size.height) {
		while (tile.x < size.width) {
		    if ((clip == null) || (clip.intersects(tile))) {
			g.drawImage(background, tile.x, tile.y, this);
		    }
		    tile.x += tile.width;
		}
		tile.x = 0;
		tile.y += tile.height;
	    }
	}
    }

    boolean isLightweight(Component comp) {
	return comp.getPeer() instanceof java.awt.peer.LightweightPeer;
    }
    
    public void repaint(long time, int x, int y, int width, int height) {
	if (buffered) {
	    synchronized(damage) {
		damage.addRectangle(new Rectangle(x, y, width, height));
	    }
	    if (autoFlushing) {
		flushBuffer();
	    }
	} else {
	    super.repaint(time, x, y, width, height);
	}
    }

    public void update(Graphics g) {
	// It is not necessary to have the system clear the background
	// for us...
	if (buffered) {
	    paint(g);
	} else {
	    super.update(g);
	}
    }

    public void paint(Graphics g) {
	if ((buffered) && (buffer != null)) {
	    renderBuffer();
	    g.drawImage(buffer, 0, 0, this);
	} else {
	    super.paint(g);
	}
    }

    protected boolean buffered;
    protected boolean autoFlushing;
    protected Image background;
    protected boolean windowCreated;
    protected transient Image buffer;
    protected transient Graphics bufferGraphics;
    protected transient Region damage;
    protected Object lock = new Object();

    private void readObject(ObjectInputStream is) throws IOException, 
	ClassNotFoundException {

	is.defaultReadObject();
	damage = new Region();
    }
}
