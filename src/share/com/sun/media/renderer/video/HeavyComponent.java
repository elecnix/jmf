/*
 * @(#)HeavyComponent.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

import java.awt.*;

public class HeavyComponent extends Canvas {

    BasicVideoRenderer bvr = null;
    
    public HeavyComponent() {
    }

    public void setRenderer(BasicVideoRenderer bvr) {
	this.bvr = bvr;
    }

    public synchronized void paint(Graphics g) {
	if (bvr != null) 
	    bvr.repaint();
    }
    
    public synchronized void update(Graphics g) {
    }
    
    public Dimension getMinimumSize() {
	return new Dimension(1, 1);
    }
    
    public Dimension getPreferredSize() {
	if (bvr != null)
	    return bvr.myPreferredSize();
	else
	    return super.getPreferredSize();
    }
    
    public synchronized void addNotify() {
	super.addNotify();
	if (bvr != null)
	    bvr.setAvailable(true);
    }
    
    public synchronized void removeNotify() {
	if (bvr != null)
	    bvr.setAvailable(false);
	super.removeNotify();
    }
}
