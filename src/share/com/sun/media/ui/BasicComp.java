/*
 * @(#)BasicComp.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import javax.media.Control;

import java.awt.*;
import java.awt.event.*;

public class BasicComp extends Container {

    protected String label = null;
    private ActionListener al = null;
    static Panel panel = new Panel(); // needed for imageTracker
    Control control = null;
    int width, height;

    protected BasicComp(String label) {
	this.label = label;
    }

    public void setActionListener(ActionListener al) {
	this.al = al;
    }

    protected void informListener() {
	if (al != null)
	    al.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
					       label));
    }
    
    // Load an image from the image library.
    static synchronized public Image fetchImage(String name) {

       Image image=null;
       byte[] bits;

       bits = ImageLib.getImage(name);
       if (bits==null)
           return null;

       image = Toolkit.getDefaultToolkit().createImage(bits);

       try {  // wait for image
           MediaTracker imageTracker = new MediaTracker(panel);
           imageTracker.addImage(image, 0);
           imageTracker.waitForID(0);
       } catch (InterruptedException e) {
           System.err.println("ImageLoader: Interrupted at waitForID");
       }

	return image;
    }

    public String getLabel () {
        return ( label );
    }
    
}


