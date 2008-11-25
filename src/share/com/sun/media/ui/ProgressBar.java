/*
 * @(#)ProgressBar.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.*;
import javax.media.*;

public class ProgressBar extends Slider {

    private CachingControl      cc = null;
    private Color               cb, cd, cm;
    private ProgressBarThread   threadUpdate;

    public ProgressBar(CachingControl cc) {
	this.cc = cc;
	setGrabberVisible(false);
	setBackground(DefaultControlPanel.colorBackground);

        threadUpdate = new ProgressBarThread ( this, cc );
        threadUpdate.start();
    }

    public void update(Graphics g) {
	paint(g);
    }

    public void paint(Graphics g) {
	if (cc == null)
	    super.paint(g);
	else {
	    long len = cc.getContentLength();
	    long progress = cc.getContentProgress();

	    if (len < 1) {
		return;
	    }

	    if (progress > len)
		len = progress;

	    setDisplayPercent((int)(100 * progress / len));
	    super.paint(g);
	}
    }
}


class ProgressBarThread extends Thread {

    private Component           progressBar;
    private CachingControl      cachingControl;
    private long                lengthContent;


    public ProgressBarThread ( Component progressBar, CachingControl cachingControl ) {
        this.progressBar = progressBar;
        this.cachingControl = cachingControl;

        lengthContent = cachingControl.getContentLength();
    }

    public void run () {
        long    lengthProgress = 0;

        while ( lengthProgress < lengthContent ) {
            try {
                sleep ( 300 );
            }
            catch ( Exception exception ) {
            }

            lengthProgress = cachingControl.getContentProgress();
            progressBar.repaint();
        }
    }
}

    
