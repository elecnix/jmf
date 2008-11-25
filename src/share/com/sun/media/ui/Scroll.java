/*
 * @(#)Scroll.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.ui;

import java.awt.*;
import java.awt.event.*;

public class Scroll extends Component
implements MouseListener, MouseMotionListener {
    
    Image imageGrabber;
    Image imageGrabberX;
    Image imageGrabberDown;

    Graphics paintG = null;
    
    boolean grabbed;
    boolean entered;
    int grabberPosition;
    int leftBorder = 8;
    int rightBorder = 8;
    int sliderWidth;
    int width, height;
    int displayPercent;
    float detents[];
    Dimension dimension;
    float lower = 0.0f, upper = 1.0f, range = 1.0f, value = 0.5f;
    boolean dragging = false;
    boolean grabberVisible = true;
    ActionListener actionListener = null;

    public Scroll() {
	this(null, null);
    }

    public Scroll(float detents[]) {
	this(detents, null);
    }

    public Scroll(float detents[], Color background) {
	imageGrabber = BasicComp.fetchImage("grabber.gif");
	imageGrabberDown = BasicComp.fetchImage("grabber-pressed.gif");
	imageGrabberX = BasicComp.fetchImage("grabber-disabled.gif");

	this.detents = detents;
	if (background != null) {
	    setBackground(background);
	}
	width = 115;
	height = 18;
	displayPercent = 100;
	dimension = new Dimension(width, height);
	sliderWidth = width - leftBorder - rightBorder;
	setSize(width, height);
	setVisible(true);
	grabbed = false;
	entered = false;
	addMouseListener( this );
	addMouseMotionListener( this );
    }

    public void setActionListener(ActionListener al) {
	actionListener = al;
    }
    
    public void setValue(float value) {
	lower = 0.0f;
	upper = 1.0f;
	range = upper - lower;
	setSliderPosition(value - lower, range);
	repaint();
    }

    public float getValue() {
	return value;
    }
    
    public void setEnabled(boolean state) {
	super.setEnabled(state);
	repaint();
    }

    public Point getPosition() {
	return new Point(grabberPosition + leftBorder, 10);
    }

    public void setDisplayPercent(int percent) {
	if (percent != displayPercent) {
	    displayPercent = percent;
	    if (displayPercent > 100) {
		displayPercent = 100;
	    } else if (displayPercent < 0) {
		displayPercent = 0;
	    }
	    repaint();
	}
    }

    public void paint(Graphics g) {
	Dimension size = getSize();
	int x;
	int y = (size.height / 2) - 2;

	paintG = g;
	
	int grabberX = grabberPosition + leftBorder - 5;

	// Draw the trough
	g.setColor(getBackground());
	y = (getSize().height / 2) - 2;
	g.draw3DRect(2, y, size.width - 4, 3, false);

	// Draw the display in the trough
	if (displayPercent < 100) {
	    g.setColor(Color.green);
	    x = sliderWidth * displayPercent / 100 + 3;
	    y += 2;
	    g.drawLine(x, y, size.width - 4, y);
	}

	// Draw the marks for detents
	if (detents != null && detents.length != 0) {
	    paintG.setColor(Color.black);
	    for (int i = 0; i < detents.length; i++) {
		x = leftBorder + (int)(detents[i] * sliderWidth / range);
		paintG.drawLine(x, 12, x, 15);
	    }
	}

	// Draw the grabber
	if (grabberVisible) {
	    Image image;
	    if (isEnabled()) {
		if (grabbed || entered)
		    image = imageGrabberDown;
		else
		    image = imageGrabber;
	    } else {
		image = imageGrabberX;
	    }
	    paintG.drawImage(image, grabberX, 4, this);
	}
    }

    private int limitGrabber(int mousex) {
	int x = mousex - leftBorder;
	if (x < 0)
	    x = 0;
	else if (x > sliderWidth)
	    x = sliderWidth;
	return x;
    }

    private void setSliderPosition(float value, float range) {
	grabberPosition = (int) ( value / range * sliderWidth);
    }

    private void seek() {
	value = ((float)grabberPosition/sliderWidth);
	if (detents != null && detents.length > 0 && dragging) {
	    float tolerance = 0.05f;
	    for (int i = 0; i < detents.length; i++) {
		if (Math.abs(detents[i] - value) <= tolerance)
		    value = detents[i];
	    }
	}
	
	repaint();
	if (actionListener != null)
	    actionListener.actionPerformed(new ActionEvent(this,
							   ActionEvent.ACTION_PERFORMED,
							   "scroll"));
	
    }
    
    /*************************************************************************
     * MouseListener methods
     *************************************************************************/
    
    public void mousePressed(MouseEvent me) {
	int modifier = me.getModifiers();
	if ((modifier & InputEvent.BUTTON2_MASK) == 0 &&
	    (modifier & InputEvent.BUTTON3_MASK) == 0 ) {
	    if (isEnabled()) {
		dragging = false;
		grabbed = true;
		grabberPosition = limitGrabber(me.getX());
		seek();
	    }
	}
    }
    
    public void mouseReleased(MouseEvent me) {
	int modifier = me.getModifiers();
	if ((modifier & InputEvent.BUTTON2_MASK) == 0 &&
	    (modifier & InputEvent.BUTTON3_MASK) == 0 ) {
	    if (isEnabled()) {
		dragging = false;
		grabbed = false;
		repaint();
	    }
	}
    }
    
    public void mouseDragged(MouseEvent me) {
	int modifier = me.getModifiers();
	if ((modifier & InputEvent.BUTTON2_MASK) == 0 &&
	    (modifier & InputEvent.BUTTON3_MASK) == 0 ) {
	    if (isEnabled()) {
		dragging = true;
		grabberPosition = limitGrabber(me.getX());
		seek();
	    }
	}
    }

    public void mouseEntered(MouseEvent me) {
	entered = true;
	repaint();
    }
    
    public void mouseExited(MouseEvent me) {
	entered = false;
	repaint();
    }
    
    public void mouseClicked(MouseEvent me) { }
    
    public void mouseMoved(MouseEvent me) { }
    

    /*************************************************************************
     * Component methods
     *************************************************************************/
    
    public void setSize(int width, int height) {
	super.setSize(width, height);
	paintG = null;
	repaint();
    }

    public Dimension getPreferredSize() {
	return dimension;
    }
}
    
    
