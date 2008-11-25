/*
 * @(#)Group.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.*;

public class Group extends Container {

    public Group() {
	this(null, 0);
    }

    public Group(LayoutManager layout) {
	this(layout, 0);
    }
	
    public Group(LayoutManager layout, int border) {
	setLayout(layout);
	this.border = border;
    }

    public void paint(Graphics g) {
	Dimension size = getSize();

	super.paint(g);
	if (border > 0) {
	    g.setColor(getBackground());
	    g.drawRect(0, 0, size.width - 1, size.height - 1);
	    g.draw3DRect(border - 2, border - 2, 
			 size.width - 1 - 2 * (border - 2),
			 size.height - 1 - 2 * (border - 2), false);
	}
    }

    public Insets getInsets() {
	return new Insets(border, border, border, border);
    }
	
    protected int border;
}


