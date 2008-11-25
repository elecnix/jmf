/*
 * @(#)SilenceSuppressionAdapter.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Implementation for SilenceSuppressionControl
 */
public class SilenceSuppressionAdapter implements javax.media.control.SilenceSuppressionControl {
    protected Codec owner=null;
    protected boolean silenceSuppression=false;
    protected boolean isSetable;
    Component component=null;
    String CONTROL_STRING="Silence Suppression";


    public SilenceSuppressionAdapter(Codec newOwner, boolean newSilenceSuppression, boolean newIsSetable) {
        silenceSuppression= newSilenceSuppression;
        owner = newOwner;
        isSetable = newIsSetable;
    }


    /**
     * Returns if silence suppression was enabled
     * @return if silence suppression was enabled
     */
    public boolean getSilenceSuppression(){
        return silenceSuppression;
    }

    /**
     * Sets the silence suppression mode.
     * Decoders ignore this method.
     * @param newSilenceSuppression the requested silence suppression
     * mode
     * @return the actual still image transmission mode that was set
     */
    public boolean setSilenceSuppression(boolean newSilenceSuppression) {
        return silenceSuppression;
    }

    /**
     * Returns if silence suppression is supported
     * @return if silence suppression is supported
     */
    public boolean isSilenceSuppressionSupported() {
         return silenceSuppression;
    }

    public Component getControlComponent() {
        if (component ==null ) {
            Panel componentPanel=new Panel();
            componentPanel.setLayout(new BorderLayout() );
            componentPanel.add("Center",new Label(CONTROL_STRING,Label.CENTER) );
            Checkbox cb=new Checkbox(null,null,silenceSuppression);
            cb.setEnabled(isSetable);
            cb.addItemListener( (ItemListener) new SilenceSuppresionAdapterListener(cb) );

            componentPanel.add("East",cb );
            componentPanel.invalidate();
            component=componentPanel;

        }
        return (Component)component;
    }

    class SilenceSuppresionAdapterListener implements java.awt.event.ItemListener  {
         Checkbox cb;
         public SilenceSuppresionAdapterListener(Checkbox source) {
             cb=source;
         }

         public void itemStateChanged(ItemEvent e) {
             try {
                 boolean newSilenceSuppression = cb.getState() ;
//DEBUG                 System.out.println("newStillImage "+newStillImage);

                 setSilenceSuppression(silenceSuppression);
             } catch (Exception exception) {
	     }

             cb.setState(silenceSuppression);

         }

     }
}

