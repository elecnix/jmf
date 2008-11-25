/*
 * @(#)TrackPanel.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.control.*;
//import javax.media.Format;
//import javax.media.format.*;
//import javax.media.format.*;
//import javax.media.protocol.*;
//import javax.media.datasink.*;



public abstract class TrackPanel extends JMPanel {

    protected TrackControl      trackControl = null;
    protected Format            arrSupportedFormats [] = null;
    protected Vector            vectorContSuppFormats = new Vector ();
    protected ActionListener    listenerEnableTrack = null;


    public TrackPanel( TrackControl trackControl, ActionListener listenerEnableTrack ) {
        this.trackControl = trackControl;
        this.listenerEnableTrack = listenerEnableTrack;
        this.arrSupportedFormats = trackControl.getSupportedFormats ();
    }


    public TrackControl getTrackControl () {
        return ( trackControl );
    }

    public void updateTrack () {
        Format         format;

        if ( isTrackEnabled() == false ) {
            trackControl.setEnabled ( false );
            return;
        }
        format = getFormat ();
        if ( format == null ) {
            MessageDialog.createErrorDialog ( getFrame(), "Internal error. " +
					      "Unable to match choosen format. Track will be disabled." );
            trackControl.setEnabled ( false );
        }
        else {
            trackControl.setEnabled ( true );
            trackControl.setFormat ( format );
        }
    }

    public abstract boolean isTrackEnabled ();
    public abstract Format getFormat ();

}


