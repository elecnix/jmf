/*
 * @(#)H261Adapter.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Implementation for H261Control
 */
public class H261Adapter implements javax.media.control.H261Control {

    Codec owner=null;
    boolean isSetable;
    boolean stillImage=false;
    Component component=null;
    String CONTROL_STRING="Still Image Transmission";


    public H261Adapter(Codec newOwner, boolean newStillImage, boolean newIsSetable) {
        stillImage= newStillImage;
        owner = newOwner;
        isSetable = newIsSetable;
    }


    /**
     * Returns if still image transmission was enabled
     * @return if still image transmission was enabled
     */
    public boolean getStillImageTransmission(){
        return stillImage;
    }

    /**
     * Sets the still image transmission mode
     * @param newStillImageTransmission the requested still image transmission
     * mode
     * @return the actual still image transmission mode that was set
     */
    public boolean setStillImageTransmission(boolean newStillImageTransmission){
        return stillImage;
    }

    /**
     * Returns if still image transmission is supported
     * @return if still image transmission is supported
     */
    public boolean isStillImageTransmissionSupported() {
         return stillImage;
    }

    public Component getControlComponent() {
        if (component ==null ) {
            Panel componentPanel=new Panel();
            componentPanel.setLayout(new BorderLayout() );
            componentPanel.add("Center",new Label(CONTROL_STRING,Label.CENTER) );
            Checkbox cb=new Checkbox(null,null,stillImage);
            cb.setEnabled(isSetable);
            cb.addItemListener( (ItemListener) new H261AdapterListener(cb) );

            componentPanel.add("East",cb );
            componentPanel.invalidate();
            component=componentPanel;

        }
        return (Component)component;
    }

    class H261AdapterListener implements java.awt.event.ItemListener  {

	Checkbox cb;

	public H261AdapterListener(Checkbox source) {
	    cb=source;
	}

	public void itemStateChanged(ItemEvent e) {
	    try {
		boolean newStillImage= cb.getState() ;
		System.out.println("newStillImage "+newStillImage);
		
		setStillImageTransmission(newStillImage);
	    } catch (Exception exception) {
	    }

	    cb.setState(stillImage);
	}
    }
}

