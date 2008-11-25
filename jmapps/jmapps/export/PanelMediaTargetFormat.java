/*
 * @(#)PanelMediaTargetFormat.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.export;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
//import javax.media.datasink.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.ui.TabControl;
import com.sun.media.ui.AudioFormatChooser;
import com.sun.media.ui.VideoFormatChooser;

import jmapps.ui.*;


public class PanelMediaTargetFormat extends JMPanel implements ActionListener, ItemListener {

    private Processor               processor = null;
    private String                  strTargetType = PanelMediaTargetType.TYPE_OTHER;
    private ContentDescriptor       arrContentDescriptors [] = null;
    private Hashtable               hashtableContentDescriptors = null;
    private TrackControl            arrTrackControls [] = null;
    private String                  arrAllowContentType [] = null;

    private JMPanel         panelContent;
    private Choice          comboContentType;
    private TabControl      tabControl;
    private Vector          vectorPanelsAudio;
    private Vector          vectorPanelsVideo;
    private Vector          vectorTracksAudio;
    private Vector          vectorTracksVideo;

    private Image           imageAudioEn = null;
    private Image           imageAudioDis = null;
    private Image           imageVideoEn = null;
    private Image           imageVideoDis = null;


    public PanelMediaTargetFormat () {
    	super ();

    	try {
    	    init ();
    	}
    	catch ( Exception exception ) {
    	    exception.printStackTrace ();
    	}
    }

    public void setAllowContentType ( String arrAllowContentType[] ) {
        this.arrAllowContentType = arrAllowContentType;
    }

    private void init () throws Exception {
    	Panel	   panelDescription;
//        Toolkit    toolkit;

    	this.setLayout ( new BorderLayout(6,6) );

    	panelDescription = new Panel ( new GridLayout(0,1) );
    	this.add ( panelDescription, BorderLayout.NORTH );
    	panelDescription.add ( new Label(JMFI18N.getResource("jmstudio.export.format.label1")) );
    	panelDescription.add ( new Label(JMFI18N.getResource("jmstudio.export.format.label2")) );

        panelContent = new JMPanel ( new BorderLayout(6,6) );
        panelContent.setEmptyBorder ( 6, 6, 6, 6 );
        this.add ( panelContent, BorderLayout.CENTER );

        imageAudioEn = ImageArea.loadImage ( "audio.gif", this, true );
        imageAudioDis = ImageArea.loadImage ( "audio-disabled.gif", this, true );
        imageVideoEn = ImageArea.loadImage ( "video.gif", this, true );
        imageVideoDis = ImageArea.loadImage ( "video-disabled.gif", this, true );
    }

    public void setProcessor ( Processor processor, String strContType, String strTargetType ) {
        this.processor = processor;
        this.strTargetType = strTargetType;

        arrContentDescriptors = processor.getSupportedContentDescriptors ();
        arrTrackControls = processor.getTrackControls ();

        panelContent.removeAll ();
        buildPage ();

        if ( strTargetType.equals(PanelMediaTargetType.TYPE_NETWORK) )
            strContType = (new ContentDescriptor(ContentDescriptor.RAW_RTP)).toString ();
        if ( strTargetType.equals(PanelMediaTargetType.TYPE_SCREEN) )
            strContType = (new ContentDescriptor(ContentDescriptor.RAW)).toString ();

        if ( strContType != null )
            comboContentType.select ( strContType );
        changeContentType ();
    }

    public void updateProcessorFormat () {
        int                 i;
        int                 nCount;
        VideoFormatChooser  panelVideo;
        AudioFormatChooser  panelAudio;
        TrackControl        trackControl;
        Format              format;


        nCount = vectorPanelsVideo.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            panelVideo = (VideoFormatChooser) vectorPanelsVideo.elementAt ( i );
            trackControl = (TrackControl) vectorTracksVideo.elementAt ( i );

            if ( panelVideo.isTrackEnabled() == false ) {
                trackControl.setEnabled ( false );
                continue;
            }
            format = panelVideo.getFormat ();
            if ( format == null ) {
                MessageDialog.createErrorDialog ( this.getFrame(), "Internal error. Unable to match choosen video format. Track will be disabled." );
                trackControl.setEnabled ( false );
            }
            else {
                trackControl.setEnabled ( true );
                trackControl.setFormat ( format );
            }
        }

        nCount = vectorPanelsAudio.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            panelAudio = (AudioFormatChooser) vectorPanelsAudio.elementAt ( i );
            trackControl = (TrackControl) vectorTracksAudio.elementAt ( i );

            if ( panelAudio.isTrackEnabled() == false ) {
                trackControl.setEnabled ( false );
                continue;
            }
            format = panelAudio.getFormat ();
            if ( format == null ) {
                MessageDialog.createErrorDialog ( this.getFrame(), "Internal error. Unable to match choosen audio format. Track will be disabled." );
                trackControl.setEnabled ( false );
            }
            else {
                trackControl.setEnabled ( true );
                trackControl.setFormat ( format );
            }
        }
    }

    public boolean[] getEnabledVideoTracks () {
        int                 i;
        int                 nCount;
        VideoFormatChooser  panelVideo;
        boolean             arrResult [];


        nCount = vectorPanelsVideo.size ();
        arrResult = new boolean [ nCount ];
        for ( i = 0;  i < nCount;  i++ ) {
            panelVideo = (VideoFormatChooser) vectorPanelsVideo.elementAt ( i );
            arrResult[i] = panelVideo.isTrackEnabled ();
        }
        return ( arrResult );
    }

    public boolean[] getEnabledAudioTracks () {
        int                 i;
        int                 nCount;
        AudioFormatChooser  panelAudio;
        boolean             arrResult [];


        nCount = vectorPanelsAudio.size ();
        arrResult = new boolean [ nCount ];
        for ( i = 0;  i < nCount;  i++ ) {
            panelAudio = (AudioFormatChooser) vectorPanelsAudio.elementAt ( i );
            arrResult[i] = panelAudio.isTrackEnabled ();
        }
        return ( arrResult );
    }

    public void actionPerformed ( ActionEvent event ) {
        String        strCmd;
        Object        objectSource;

        strCmd = event.getActionCommand ();
        objectSource = event.getSource ();

        if ( strCmd.equals(AudioFormatChooser.ACTION_TRACK_ENABLED) ) {
            if ( objectSource instanceof AudioFormatChooser )
                tabControl.setPageImage ( (Panel)objectSource, imageAudioEn );
        }
        else if ( strCmd.equals(AudioFormatChooser.ACTION_TRACK_DISABLED) ) {
            if ( objectSource instanceof AudioFormatChooser )
                tabControl.setPageImage ( (Panel)objectSource, imageAudioDis );
        }
        else if ( strCmd.equals(VideoFormatChooser.ACTION_TRACK_ENABLED) ) {
            if ( objectSource instanceof VideoFormatChooser )
                tabControl.setPageImage ( (Panel)objectSource, imageVideoEn );
        }
        else if ( strCmd.equals(VideoFormatChooser.ACTION_TRACK_DISABLED) ) {
            if ( objectSource instanceof VideoFormatChooser )
                tabControl.setPageImage ( (Panel)objectSource, imageVideoDis );
        }
    }

    public void itemStateChanged ( ItemEvent event ) {
        Object              objectSource;

        objectSource = event.getSource ();
        if ( objectSource == comboContentType ) {
            changeContentType ();
        }
    }

    private void buildPage () {
        int      i;
        int      nCount;
        String   strContType;
        Panel    panelFormat;
        Panel    panel;
        Label    label;


        panelFormat = new Panel ( new BorderLayout(6,6) );
        panelContent.add ( panelFormat, BorderLayout.NORTH );

        label = new Label ( JMFI18N.getResource("jmstudio.export.format.format") );
        panelFormat.add ( label, BorderLayout.WEST );
        comboContentType = new Choice ();
        comboContentType.addItemListener ( this );
        nCount = arrContentDescriptors.length;
        hashtableContentDescriptors = new Hashtable ();
        for ( i = 0;  i < nCount;  i++ ) {
            strContType = arrContentDescriptors[i].getContentType();
            // filter
            if ( !isContentTypeAllowed(strContType) )
                continue;

            strContType = arrContentDescriptors[i].toString ();
            comboContentType.addItem ( strContType );
            hashtableContentDescriptors.put ( strContType, arrContentDescriptors[i] );
        }
        panelFormat.add ( comboContentType, BorderLayout.CENTER );

        panel = buildTrackFormatPanel ();
        panelContent.add ( panel, BorderLayout.CENTER );
    }

    private Panel buildTrackFormatPanel () {
//        Panel               panel;
        AudioFormatChooser  chooserAudio;
        VideoFormatChooser  chooserVideo;
        int                 i;
        int                 nCount;
        int                 nIndexAudio;
        int                 nIndexVideo;
        int                 nAudioTrackCount = 0;
        int                 nVideoTrackCount = 0;
        Format              format;
        String              strTitle;
        String              strEncoding;
        String              strAudio = JMFI18N.getResource("jmstudio.export.format.audio");
        String              strVideo = JMFI18N.getResource("jmstudio.export.format.video");
        String              strHinted = JMFI18N.getResource("jmstudio.export.format.hinted");


        tabControl = new TabControl ( TabControl.ALIGN_TOP );

        nIndexAudio = 0;
        nIndexVideo = 0;
        nAudioTrackCount = 0;
        nVideoTrackCount = 0;
        vectorPanelsAudio = new Vector ();
        vectorPanelsVideo = new Vector ();
        vectorTracksAudio = new Vector ();
        vectorTracksVideo = new Vector ();

        nCount = arrTrackControls.length;
        for ( i = 0;  i < nCount;  i++ ) {
            format = arrTrackControls[i].getFormat ();
            if ( format instanceof VideoFormat )
                nVideoTrackCount++;
            if ( format instanceof AudioFormat )
                nAudioTrackCount++;
        }

        for ( i = 0;  i < nCount;  i++ ) {
            format = arrTrackControls[i].getFormat ();
            if ( format instanceof AudioFormat ) {
                if ( nAudioTrackCount < 2 )
                    strTitle = new String ( strAudio );
                else {
                    nIndexAudio++;
                    strTitle = new String ( strAudio + " " + nIndexAudio );
                }
                strEncoding = format.getEncoding ();
                if ( strEncoding.endsWith("/rtp") )
                    strTitle = strTitle + " " + strHinted;
                chooserAudio = new AudioFormatChooser ( arrTrackControls[i].getSupportedFormats(), (AudioFormat)format, true, this );
                chooserAudio.setTrackEnabled ( arrTrackControls[i].isEnabled() );
                tabControl.addPage ( chooserAudio, strTitle, imageAudioEn );
                vectorPanelsAudio.addElement ( chooserAudio );
                vectorTracksAudio.addElement ( arrTrackControls[i] );
            }
            else if ( format instanceof VideoFormat ) {
                if ( nVideoTrackCount < 2 )
                    strTitle = new String ( strVideo );
                else {
                    nIndexVideo++;
                    strTitle = new String ( strVideo + " " + nIndexVideo );
                }
                strEncoding = format.getEncoding ();
                if ( strEncoding.endsWith("/rtp") )
                    strTitle = strTitle + " " + strHinted;
                chooserVideo = new VideoFormatChooser ( arrTrackControls[i].getSupportedFormats(), (VideoFormat)format, true, this );
                chooserVideo.setTrackEnabled ( arrTrackControls[i].isEnabled() );
                tabControl.addPage ( chooserVideo, strTitle, imageVideoEn );
                vectorPanelsVideo.addElement ( chooserVideo );
                vectorTracksVideo.addElement ( arrTrackControls[i] );
            }
        }

        return ( tabControl );
    }

    private void changeContentType () {
        int                 i;
        int                 nCount;
        VideoFormatChooser  panelVideo;
        AudioFormatChooser  panelAudio;
        TrackControl        trackControl;
        String              strContentType;
        ContentDescriptor   contentDescriptor;


        strContentType = comboContentType.getSelectedItem ();
        contentDescriptor = (ContentDescriptor) hashtableContentDescriptors.get ( strContentType );

        if ( processor.setContentDescriptor(contentDescriptor) == null ) {
            System.err.println ( "Error setting content descriptor on " + "processor" );
        }

        nCount = vectorPanelsVideo.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            panelVideo = (VideoFormatChooser) vectorPanelsVideo.elementAt ( i );
            trackControl = (TrackControl) vectorTracksVideo.elementAt ( i );
            panelVideo.setSupportedFormats ( trackControl.getSupportedFormats(), (VideoFormat)trackControl.getFormat() );
        }

        nCount = vectorPanelsAudio.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            panelAudio = (AudioFormatChooser) vectorPanelsAudio.elementAt ( i );
            trackControl = (TrackControl) vectorTracksAudio.elementAt ( i );
            panelAudio.setSupportedFormats ( trackControl.getSupportedFormats(), (AudioFormat)trackControl.getFormat() );
        }
    }

    private boolean isContentTypeAllowed ( String strContType ) {
        int        i;
        boolean    boolResult = false;
        String     strTypeRaw;
        String     strTypeRawRtp;

        if ( arrAllowContentType != null ) {
            for ( i = 0;  i < arrAllowContentType.length  &&  boolResult == false;  i++ ) {
                if ( arrAllowContentType[i].equalsIgnoreCase(strContType) )
                    boolResult = true;
            }
        }
        else if ( strTargetType.equals(PanelMediaTargetType.TYPE_NETWORK) ) {
            strTypeRaw = ContentDescriptor.mimeTypeToPackageName ( ContentDescriptor.RAW );
            strTypeRawRtp = ContentDescriptor.mimeTypeToPackageName ( ContentDescriptor.RAW_RTP );
            if ( strContType.equals(strTypeRaw)  ||  strContType.equals(strTypeRawRtp) )
                boolResult = true;
        }
        else
            boolResult = true;

        return ( boolResult );
    }

}


