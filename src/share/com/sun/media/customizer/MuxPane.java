/*
 * @(#)MuxPane.java	1.6 02/08/29
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  This is the class to define the multiplexer page
 *
 *  @version 2.0
 */

public class MuxPane extends JPanel {
    // the sequence is critical for codecpane
    public static final int AU = 0;
    public static final int AIFF = 1;
    public static final int GSM = 2;
    public static final int WAV = 3;
    public static final int MP2 = 4;
    public static final int MP3 = 5;
    public static final int MOV = 6;
    public static final int AVI = 7;

    JCheckBox[] muxs = new JCheckBox[8];
    boolean[] resultMux = new boolean[8];

    public MuxPane() {
	muxs[GSM] = new JCheckBox(I18N.getResource("MuxPane.GSM"), false);
	muxs[MP2] = new JCheckBox(I18N.getResource("MuxPane.MP2"), false);
	muxs[MP3] = new JCheckBox(I18N.getResource("MuxPane.MP3"), false); 
	muxs[WAV] = new JCheckBox(I18N.getResource("MuxPane.WAV"), false);
	muxs[AIFF] = new JCheckBox(I18N.getResource("MuxPane.AIFF"), false);
	muxs[AU] = new JCheckBox(I18N.getResource("MuxPane.AU"), false);
	muxs[MOV] = new JCheckBox(I18N.getResource("MuxPane.MOV"), false);
	muxs[AVI] = new JCheckBox(I18N.getResource("MuxPane.AVI"), false);
	
	JPanel apane = new JPanel(new GridLayout(2,3));
	JPanel vpane = new JPanel(new GridLayout(2,1));
        apane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("MuxPane.ATITLE")));
        vpane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("MuxPane.VTITLE")));
	
	for ( int i = 0; i < 6; i++) {
	    muxs[i].setEnabled(false);
	    apane.add(muxs[i]);
	}

	for ( int i = 6; i < 8; i++) {
	    muxs[i].setEnabled(false);
	    vpane.add(muxs[i]);
	}

	setLayout(new GridLayout(2,1));
	this.add(apane);
	this.add(vpane);
    }

    public boolean[] getState() {
	for (int i = 0; i < 8; i++) {
	    if (muxs[i].isEnabled() && muxs[i].isSelected())
		resultMux[i] = true;
	    else 
		resultMux[i] = false;
	} 
	return (resultMux);
    }

    public void enableAll() {
	for ( int i = 0; i < 8; i++) 
	    muxs[i].setEnabled(true);
	muxs[MP3].setEnabled(false);
    }

    public void disableAll() {
	for ( int i = 0; i < 8; i++) 
	    muxs[i].setEnabled(false);
    }

}

