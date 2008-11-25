/*
 * @(#)RendererPane.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** 
 *  This class defines the available renderer page
 *  
 *  @version 2.0
 */
 
public class RendererPane extends JPanel {
    public static final int SUNAUDIO = 0;
    public static final int JAVASOUND = 1;
    public static final int DAUDIO = 2;
    public static final int AWT = 3;
    public static final int JPEG = 4;
    public static final int XLIB = 5;
    public static final int XIL = 6;
    public static final int SUNRAY = 7;
    public static final int DDRAW = 8;
    public static final int GDI = 9;

    JCheckBox[] rndr = new JCheckBox[10];
    boolean[] resultRndr = new boolean[10];

    public RendererPane() {
	rndr[SUNAUDIO] = new JCheckBox(I18N.getResource("RendererPane.SUNAUDIO"), false);
	rndr[JAVASOUND] = new JCheckBox(I18N.getResource("RendererPane.JAVASOUND"), false);
	rndr[DAUDIO] = new JCheckBox(I18N.getResource("RendererPane.DAUDIO"), false);
	rndr[AWT] = new JCheckBox(I18N.getResource("RendererPane.AWT"), false);
	rndr[JPEG] = new JCheckBox(I18N.getResource("RendererPane.JPEG"), false);
	rndr[XLIB] = new JCheckBox(I18N.getResource("RendererPane.XLIB"), false);
	rndr[XIL] = new JCheckBox(I18N.getResource("RendererPane.XIL"), false);
	rndr[SUNRAY] = new JCheckBox(I18N.getResource("RendererPane.SUNRAY"), false);
	rndr[DDRAW] = new JCheckBox(I18N.getResource("RendererPane.DDRAW"), false);
	rndr[GDI] = new JCheckBox(I18N.getResource("RendererPane.GDI"), false);

	JPanel apane = new JPanel(new GridLayout(3,1));
	JPanel vpane = new JPanel(new GridLayout(3,3));

	for ( int i = 0 ; i < 3; i++) {
	    rndr[i].setEnabled(false);
	    apane.add(rndr[i]);
	}

	for ( int i = 3 ; i < 10; i++) {
	    rndr[i].setEnabled(false);
	    vpane.add(rndr[i]);
	}

        apane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("RendererPane.ARNDR")));
        vpane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("RendererPane.VRNDR")));

	this.setLayout(new GridLayout(2,1));
	this.add(apane);
	this.add(vpane);
    }

    public boolean[] getState() {
	for (int i = 0; i < 10; i++) {
	    if (rndr[i].isEnabled() && rndr[i].isSelected())
		resultRndr[i] = true;
	    else 
		resultRndr[i] = false;
	} 
	return (resultRndr);
    }

    public void enableAll(int release) {
	for ( int i = 0; i < 10; i++)
	    rndr[i].setEnabled(true);

	if ( release == 1 ) { // AJ
	   for ( int i = XLIB; i<= GDI; i++) 
	       rndr[i].setEnabled(false);
	   rndr[DAUDIO].setEnabled(false);
	} else if ( release == 2) { // SPP
	    for ( int i = DDRAW; i <= GDI; i++)
		rndr[i].setEnabled(false);
	} else if ( release == 3 ) { // WPP
	    for ( int i = XLIB; i<= SUNRAY; i++)
		rndr[i].setEnabled(false);
	}
	
    }

    public void disableAll() {
	for ( int i = 0; i < 10; i++)
	    rndr[i].setEnabled(false);
    }

}

