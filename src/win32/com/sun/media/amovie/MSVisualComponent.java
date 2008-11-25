/*
 * @(#)MSVisualComponent.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.amovie;

import java.awt.*;

public class MSVisualComponent extends Canvas implements com.ms.awt.HeavyComponent {

    AMController amc;
    
    public MSVisualComponent(AMController amc) {
	this.amc = amc;
    }

    public boolean needsHeavyPeer() {
	return true;
    }

    public void addNotify() {
	super.addNotify();
	amc.peerExists = true;
	amc.setOwner( this );
	if (amc.amovie != null) {
	    getPreferredSize();
	    amc.amovie.setWindowPosition(0, 0, amc.outWidth, amc.outHeight);
	}
    }

    public void removeNotify() {
	amc.peerExists = false;
	if (amc.amovie != null) {
	    amc.amovie.setVisible(0);
	    amc.amovie.setOwner(0);
	}
	super.removeNotify();
    }
	
    public Dimension getPreferredSize() {
	if (amc.amovie != null) {
	    int width = amc.amovie.getVideoWidth();
	    int height = amc.amovie.getVideoHeight();
	    return new Dimension(width, height);
	} else {
	    return new Dimension(1, 1);
	}
    }

    public Dimension getMinimumSize() {
	return new Dimension(1, 1);
    }

    public void layout() {
	if (amc.amovie != null) {
	    int w = amc.amovie.getVideoWidth();
	    int h = amc.amovie.getVideoHeight();
	    if (amc.pwidth == -1) {
		amc.pwidth = w;
		amc.pheight = h;
		amc.outWidth = amc.pwidth;
		amc.outHeight = amc.pheight;
		// amc.sendSizeChangeEvent(amc.outWidth, amc.outHeight, 1.0F);
	    }
	    if (amc.peerExists) {
		amc.setOwner( this );
		amc.amovie.setWindowPosition(0, 0, amc.outWidth, amc.outHeight);
	    }
	}
    }
}
