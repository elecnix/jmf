/*
 * @(#)TextComp.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.*;
import java.awt.event.*;

public class TextComp extends BasicComp implements ActionListener {

    String value;
    int    size;
    boolean mutable;
    Label  compLabel;
    TextField compText;

    public TextComp(String label, String initial, int size, boolean mutable) {
	super(label);
	this.value = initial;
	this.size = size;
	this.mutable = mutable;

	setLayout( new BorderLayout() );
	Label lab = new Label(label, Label.LEFT);
	add("West", lab);
	if (!mutable) {
	    compLabel = new Label(initial, Label.LEFT);
	    add("Center", compLabel);
	} else {
	    compText = new TextField(initial, size);
	    add("Center", compText);
	    compText.addActionListener(this);
	}
    }

    public float getFloatValue() {
	value = getValue();
	try {
	    float retVal = Float.valueOf(value).floatValue();
	    return retVal;
	} catch (NumberFormatException nfe) {
	    return 0.0f;
	}
    }
    
    public int getIntValue() {
	value = getValue();
	try {
	    int retVal = Integer.valueOf(value).intValue();
	    return retVal;
	} catch (NumberFormatException nfe) {
	    return 0;
	}
    }

    public String getValue() {
	if (mutable) {
	    return compText.getText();
	} else {
	    return compLabel.getText();
	}
    }

    public void setValue(String s) {
	value = s;
	if (mutable) {
	    compText.setText(s);
	} else {
	    compLabel.setText(s);
	}
	repaint();
    }

    public void actionPerformed(ActionEvent ae) {
	informListener();
    }
}
