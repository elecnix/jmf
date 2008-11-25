/*
 * @(#)TrackPanelVideo.java	1.4 02/08/21
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

import com.sun.media.ui.VideoFormatChooser;


public class TrackPanelVideo extends TrackPanel implements ActionListener {

    private VideoFormat         formatOld;
    private String              strContentType = null;
    private VideoFormatChooser  chooserVideoFormat;


    public TrackPanelVideo ( TrackControl trackControl, ActionListener listenerEnableTrack ) {
        super ( trackControl, listenerEnableTrack );

        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setContentType( String strContentType ) {
        int            i;
        int            nSize;
        VideoFormat    formatVideo;

        arrSupportedFormats = trackControl.getSupportedFormats();
        this.strContentType = strContentType;
        nSize = arrSupportedFormats.length;
        vectorContSuppFormats = new Vector ();

        // Add the supported formats to the vector
        for ( i = 0;  i < nSize;  i++ ) {
            if ( !(arrSupportedFormats[i] instanceof VideoFormat) )
                continue;
            formatVideo = (VideoFormat) arrSupportedFormats[i];
            // assume that processor reports only valid formats
            vectorContSuppFormats.addElement( formatVideo );
        }

        chooserVideoFormat.setSupportedFormats ( vectorContSuppFormats );
        chooserVideoFormat.setCurrentFormat ( formatOld );
    }

    public boolean isTrackEnabled () {
        boolean     boolEnabled;
        boolEnabled = chooserVideoFormat.isTrackEnabled ();
        return ( boolEnabled );
    }

    public Format getFormat () {
        Format         format;
        format = chooserVideoFormat.getFormat ();
        return ( format );
    }

    public void setDefaults ( boolean boolTrackEnable, Format format ) {
        chooserVideoFormat.setTrackEnabled ( boolTrackEnable );
        if ( format instanceof VideoFormat ) {
            formatOld = (VideoFormat) format;
            chooserVideoFormat.setCurrentFormat ( formatOld );
        }
    }

    private void init() throws Exception {
        this.setLayout ( new BorderLayout() );
        formatOld = (VideoFormat) trackControl.getFormat ();
        chooserVideoFormat = new VideoFormatChooser ( arrSupportedFormats, formatOld, true, this );
        this.add ( chooserVideoFormat, BorderLayout.NORTH );
    }

    public void actionPerformed( ActionEvent event ) {
        String        strCmd;
        ActionEvent   eventNew;

        strCmd = event.getActionCommand ();
        if ( strCmd.equals(VideoFormatChooser.ACTION_TRACK_ENABLED) ) {
            eventNew = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED,
					 event.getActionCommand() );
            listenerEnableTrack.actionPerformed ( eventNew );
        } else if ( strCmd.equals(VideoFormatChooser.ACTION_TRACK_DISABLED) ) {
            eventNew = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED,
					 event.getActionCommand() );
            listenerEnableTrack.actionPerformed ( eventNew );
        }
    }


}


