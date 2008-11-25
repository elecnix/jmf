/*
 * @(#)TrackPanelAudio.java	1.4 02/08/21
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
import javax.media.format.*;
//import javax.media.protocol.*;
//import javax.media.datasink.*;

import com.sun.media.ui.AudioFormatChooser;


public class TrackPanelAudio extends TrackPanel implements ActionListener {

    private AudioFormat         formatOld;
    private String              strContentType = null;
    private AudioFormatChooser  chooserAudioFormat;

    public TrackPanelAudio ( TrackControl trackControl, ActionListener listenerEnableTrack ) {
        super ( trackControl, listenerEnableTrack );

        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setContentType ( String strContentType ) {
        int                   i;
        int                   nSize;
        AudioFormat           formatAudio;

        arrSupportedFormats = trackControl.getSupportedFormats();
        this.strContentType = strContentType;
        nSize = arrSupportedFormats.length;
        vectorContSuppFormats = new Vector ();

        for ( i = 0;  i < nSize;  i++ ) {
            if ( !(arrSupportedFormats[i] instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) arrSupportedFormats[i];
            vectorContSuppFormats.addElement ( formatAudio );
        }

        chooserAudioFormat.setSupportedFormats ( vectorContSuppFormats );
        chooserAudioFormat.setCurrentFormat ( formatOld );
    }

    public boolean isTrackEnabled () {
        boolean     boolEnabled;
        boolEnabled = chooserAudioFormat.isTrackEnabled ();
        return ( boolEnabled );
    }

    public Format getFormat () {
        Format         format;
        format = chooserAudioFormat.getFormat ();
        return ( format );
    }

    public void setDefaults ( boolean boolTrackEnable, Format format ) {
        chooserAudioFormat.setTrackEnabled ( boolTrackEnable );
        if ( format instanceof AudioFormat ) {
            formatOld = (AudioFormat) format;
            chooserAudioFormat.setCurrentFormat ( formatOld );
        }
    }

    private void init () throws Exception {
        this.setLayout ( new BorderLayout() );
        formatOld = (AudioFormat) trackControl.getFormat ();
        chooserAudioFormat = new AudioFormatChooser ( arrSupportedFormats, formatOld, true, this );
        this.add ( chooserAudioFormat, BorderLayout.NORTH );
    }

    /**
     * This method overwrites the ActionListener method to process events
     * from buttons, track pages, and Progress dialog.
     * @param    event    action event
     */
    public void actionPerformed ( ActionEvent event ) {
        String        strCmd;
        ActionEvent   eventNew;

        strCmd = event.getActionCommand ();
        if ( strCmd.equals(AudioFormatChooser.ACTION_TRACK_ENABLED) ) {
            eventNew = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, event.getActionCommand() );
            listenerEnableTrack.actionPerformed ( eventNew );
        }
        else if ( strCmd.equals(AudioFormatChooser.ACTION_TRACK_DISABLED) ) {
            eventNew = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, event.getActionCommand() );
            listenerEnableTrack.actionPerformed ( eventNew );
        }
    }


}


