/*
 * @(#)MessageBox.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.*;
import java.awt.event.*;

/**
 * Displays an error message or any other message, with the given title and
 * message
 */
public class MessageBox extends Frame {

    public MessageBox(String title, String message) {
	super(title);
	setLayout( new BorderLayout() );

	// Panel at the bottom with a flow layout to put an OK button
	Panel bottom = new Panel();
	bottom.setLayout( new FlowLayout() );
	Button ok = new Button("Grrr!!!");
	ok.addActionListener( new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		dispose();
	    }
	} );
	bottom.add(ok);

	// Add a message label.
	add("Center", new Label(message, Label.CENTER));
	// Add the panel with the button to the south
	add("South", bottom);
	// Frame requires explicit setVisible(true)
	setVisible(true);
    }

    public void addNotify() {
	super.addNotify();
	pack();
    }
}
