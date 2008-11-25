/*
 * @(#)SliderComp.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.*;
import java.awt.event.*;

public class SliderComp extends BasicComp
    implements ActionListener {

    float  value;
    float  minValue;
    float  maxValue;
    float  initialValue;
    Scroll scroll;
    TextField tfIndicator;
    
    private static final int MIN = 0;
    private static final int MAX = 1000;
    private static final int PAGESIZE = 100;
    
    public SliderComp(String label, float min, float max, float initial) {
	super(label);
	this.minValue = min;
	this.maxValue = max;
	this.initialValue = initial;
	this.value = initial;
	
	setLayout(new BorderLayout());
	Label lab = new Label(label, Label.LEFT);
	add("West", lab);

	scroll = new Scroll();

	add("Center", scroll);
	scroll.setActionListener(this);
	scroll.setValue(toRatio(value));
	
	//tfIndicator = new TextField(5);
	//add("East", tfIndicator);
	//tfIndicator.addActionListener(this);
    }

    public void setValue(int value) {
	this.value = (float) value;
	scroll.setValue(toRatio(value));
    }

    public void setValue(float value) {
	this.value = value;
	scroll.setValue(toRatio(value));
    }

    public int getIntValue() {
	return (int) value;
    }

    public float getFloatValue() {
	return value;
    }

    public void actionPerformed(ActionEvent ae) {
	float scrollValue = scroll.getValue();
	value = fromRatio(scrollValue);
	//tfIndicator.setText(Float.toString(value));
	informListener();	
    }

    private float toRatio(float value) {
	float diff = maxValue - minValue;
	return (value - minValue) / diff;
    }

    private float fromRatio(float value) {
	return value * (maxValue - minValue) + minValue;
    }
}
