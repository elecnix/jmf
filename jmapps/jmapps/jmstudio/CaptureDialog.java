/*
 * @(#)CaptureDialog.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.jmstudio;


import java.util.Vector;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.control.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.ui.TabControl;
import com.sun.media.ui.AudioFormatChooser;
import com.sun.media.ui.VideoFormatChooser;

import jmapps.ui.*;
import jmapps.util.*;


public class CaptureDialog extends JMDialog implements ItemListener {

    private JMAppsCfg           cfgJMApps;
    private Vector              vectorDevices = null;
    private Vector              vectorAudioDevices = null;
    private Vector              vectorVideoDevices = null;

    private Panel               panelDevices;
    private Checkbox            checkUseVideo = null;
    private Checkbox            checkUseAudio = null;
    private Choice              comboVideoDevice = null;
    private Choice              comboAudioDevice = null;
    private Panel               panelVideoFormat = null;
    private Panel               panelAudioFormat = null;
    private AudioFormatChooser  chooserAudio = null;
    private VideoFormatChooser  chooserVideo = null;

    private Button              buttonOK;
    private Button              buttonCancel;


    public CaptureDialog ( Frame parentFrame, JMAppsCfg cfgJMApps ) {
        super ( parentFrame, JMFI18N.getResource("jmstudio.capture.title"), true );

        this.cfgJMApps = cfgJMApps;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isVideoDeviceUsed () {
        boolean         boolUsed = false;

        if ( checkUseVideo != null )
            boolUsed = checkUseVideo.getState ();
        return ( boolUsed );
    }

    public boolean isAudioDeviceUsed () {
        boolean         boolUsed = false;

        if ( checkUseAudio != null )
            boolUsed = checkUseAudio.getState ();
        return ( boolUsed );
    }

    public CaptureDeviceInfo getVideoDevice () {
        int                     i;
        CaptureDeviceInfo       infoCaptureDevice = null;

        if ( comboVideoDevice != null  &&  isVideoDeviceUsed() ) {
            i = comboVideoDevice.getSelectedIndex ();
            infoCaptureDevice = (CaptureDeviceInfo) vectorVideoDevices.elementAt ( i );
        }
        return ( infoCaptureDevice );
    }

    public CaptureDeviceInfo getAudioDevice () {
        int                     i;
        CaptureDeviceInfo       infoCaptureDevice = null;

        if ( comboAudioDevice != null  &&  isAudioDeviceUsed() ) {
            i = comboAudioDevice.getSelectedIndex ();
            infoCaptureDevice = (CaptureDeviceInfo) vectorAudioDevices.elementAt ( i );
        }
        return ( infoCaptureDevice );
    }

    public VideoFormat getVideoFormat () {
        VideoFormat    format = null;

        if ( chooserVideo != null  &&  isVideoDeviceUsed() )
            format = (VideoFormat) chooserVideo.getFormat ();
        return ( format );
    }

    public AudioFormat getAudioFormat () {
        AudioFormat    format = null;

        if ( chooserAudio != null  &&  isAudioDeviceUsed() )
            format = (AudioFormat) chooserAudio.getFormat ();
        return ( format );
    }

    public DataSource createCaptureDataSource () {
        DataSource          dataSource = null;
        String              audioDeviceName = null;
        String              videoDeviceName = null;
        CaptureDeviceInfo   cdi;

        cdi = getAudioDevice();
        if (cdi != null && isAudioDeviceUsed())
            audioDeviceName = cdi.getName();
        cdi = getVideoDevice();
        if (cdi != null && isVideoDeviceUsed())
            videoDeviceName = cdi.getName();
        dataSource = JMFUtils.createCaptureDataSource(audioDeviceName,
						      getAudioFormat(),
						      videoDeviceName,
						      getVideoFormat());

        return ( dataSource );
    }

    private void init () throws Exception {
        Panel       panel;
        Panel       panelButtons;
        JMPanel     panelContent;
        Label       label;


        this.setLayout ( new BorderLayout() );

        panelContent = new JMPanel ( new BorderLayout() );
        panelContent.setEmptyBorder ( 6, 6, 6, 6 );
        panelContent.setBackground ( Color.lightGray );
        this.add ( panelContent, BorderLayout.CENTER );

        vectorDevices = CaptureDeviceManager.getDeviceList ( null );
        if ( vectorDevices == null  ||  vectorDevices.size() < 1 ) {
            label = new Label ( JMFI18N.getResource("jmstudio.capture.nodevices") );
            panelContent.add ( label, BorderLayout.CENTER );
        }
        else {
            panelDevices = new Panel ( new GridLayout(1,0,6,6) );
            panelContent.add ( panelDevices, BorderLayout.CENTER );

            panel = createVideoPanel ();
            if ( panel != null )
                panelDevices.add ( panel );
            panel = createAudioPanel ();
            if ( panel != null )
                panelDevices.add ( panel );
        }

        panel = new JMPanel ( new FlowLayout(FlowLayout.CENTER) );
        panelContent.add ( panel, BorderLayout.SOUTH );
        if ( vectorDevices != null  &&  vectorDevices.size() > 0 )
            panelButtons = createButtonPanel ( new String[] { ACTION_OK, ACTION_CANCEL } );
        else
            panelButtons = createButtonPanel ( new String[] { ACTION_OK } );
        panel.add ( panelButtons );

        this.pack ();
        this.setResizable ( false );
    }

    private Panel createVideoPanel () throws Exception {
        int                 i, j;
        int                 nCount;
        JMPanel             panelVideo;
        JMPanel             panelContent;
        Panel               panel;
        Panel               panelTemp;
        CaptureDeviceInfo   infoCaptureDevice;
        Format              arrFormats [];
        boolean             boolState = true;
        VideoFormat         formatDefault = null;
        String              strDeviceName;
        boolean             boolContains;
        JMAppsCfg.CaptureDeviceData   dataCapture = null;


        nCount = vectorDevices.size ();
        vectorVideoDevices = new Vector ();
        for ( i = 0;  i < nCount;  i++ ) {
            infoCaptureDevice = (CaptureDeviceInfo) vectorDevices.elementAt ( i );
            arrFormats = infoCaptureDevice.getFormats ();
            for ( j = 0;  j < arrFormats.length;  j++ ) {
                if ( arrFormats[j] instanceof VideoFormat ) {
                    vectorVideoDevices.addElement ( infoCaptureDevice );
                    break;
                }
            }
        }

        if ( vectorVideoDevices.isEmpty() )
            return ( null );

        if ( cfgJMApps != null )
            dataCapture = cfgJMApps.getLastCaptureVideoData();
        if ( dataCapture!= null ) {
            boolState = dataCapture.boolUse;
            if ( dataCapture.format instanceof VideoFormat )
                formatDefault = (VideoFormat) dataCapture.format;
        }
        panelVideo = new JMPanel ( new BorderLayout(6,6) );
        panelVideo.setEtchedBorder ();

        panelContent = new JMPanel ( new BorderLayout(6,6) );
        panelContent.setEmptyBorder ( 6, 6, 6, 6 );
        panelVideo.add ( panelContent, BorderLayout.CENTER );
        panel = panelContent;

        panelTemp = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelTemp, BorderLayout.NORTH );
        checkUseVideo = new Checkbox ( JMFI18N.getResource("jmstudio.capture.video.usedevice"), boolState );
        checkUseVideo.addItemListener ( this );
        panelTemp.add ( checkUseVideo, BorderLayout.WEST );
        panelTemp = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelTemp, BorderLayout.CENTER );
        panel = panelTemp;

        panelTemp = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelTemp, BorderLayout.NORTH );
        comboVideoDevice = new Choice ();
        panelTemp.add ( comboVideoDevice, BorderLayout.CENTER );
        nCount = vectorVideoDevices.size ();
        boolContains = false;
        for ( i = 0;  i < nCount;  i++ ) {
            infoCaptureDevice = (CaptureDeviceInfo) vectorVideoDevices.elementAt ( i );
            strDeviceName = infoCaptureDevice.getName ();
            comboVideoDevice.addItem ( strDeviceName );
            if ( boolContains == false  &&  dataCapture!= null
                                &&  dataCapture.strDeviceName != null ) {
                boolContains = dataCapture.strDeviceName.equals ( strDeviceName );
            }
        }
        if ( boolContains == true )
            comboVideoDevice.select ( dataCapture.strDeviceName );
        comboVideoDevice.addItemListener ( this );
        comboVideoDevice.setEnabled ( boolState );

        panelVideoFormat = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelVideoFormat, BorderLayout.CENTER );
        createVideoChooser ( formatDefault);
        if ( chooserVideo != null )
            chooserVideo.setEnabled ( boolState );

        return ( panelVideo );
    }

    private void createVideoChooser ( VideoFormat formatDefault ) {
        int                     i;
        CaptureDeviceInfo       infoCaptureDevice;
        Format                  arrFormats [];

        if ( panelVideoFormat == null )
            return;

        panelVideoFormat.removeAll ();

        i = comboVideoDevice.getSelectedIndex ();
        infoCaptureDevice = (CaptureDeviceInfo) vectorVideoDevices.elementAt ( i );
        arrFormats = infoCaptureDevice.getFormats ();
        chooserVideo = new VideoFormatChooser ( arrFormats, formatDefault, false, null, true);
        panelVideoFormat.add ( chooserVideo, BorderLayout.CENTER );
    }

    private Panel createAudioPanel () throws Exception {
        int                 i, j;
        int                 nCount;
        JMPanel             panelAudio;
        JMPanel             panelContent;
        Panel               panel;
        Panel               panelTemp;
        CaptureDeviceInfo   infoCaptureDevice;
        Format              arrFormats [];
        boolean             boolState = true;
        AudioFormat         formatDefault = null;
        String              strDeviceName;
        boolean             boolContains;
        JMAppsCfg.CaptureDeviceData   dataCapture = null;


        nCount = vectorDevices.size ();
        vectorAudioDevices = new Vector ();
        for ( i = 0;  i < nCount;  i++ ) {
            infoCaptureDevice = (CaptureDeviceInfo) vectorDevices.elementAt ( i );
            arrFormats = infoCaptureDevice.getFormats ();
            for ( j = 0;  j < arrFormats.length;  j++ ) {
                if ( arrFormats[j] instanceof AudioFormat ) {
                    vectorAudioDevices.addElement ( infoCaptureDevice );
                    break;
                }
            }
        }

        if ( vectorAudioDevices.isEmpty() )
            return ( null );

        if ( cfgJMApps != null )
            dataCapture = cfgJMApps.getLastCaptureAudioData();
        if ( dataCapture!= null ) {
            boolState = dataCapture.boolUse;
            if ( dataCapture.format instanceof AudioFormat )
                formatDefault = (AudioFormat) dataCapture.format;
        }

        panelAudio = new JMPanel ( new BorderLayout(6,6) );
        panelAudio.setEtchedBorder ();

        panelContent = new JMPanel ( new BorderLayout(6,6) );
        panelContent.setEmptyBorder ( 6, 6, 6, 6 );
        panelAudio.add ( panelContent, BorderLayout.CENTER );
        panel = panelContent;

        panelTemp = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelTemp, BorderLayout.NORTH );
        checkUseAudio = new Checkbox ( JMFI18N.getResource("jmstudio.capture.audio.usedevice"), boolState );
        checkUseAudio.addItemListener ( this );
        panelTemp.add ( checkUseAudio, BorderLayout.WEST );
        panelTemp = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelTemp, BorderLayout.CENTER );
        panel = panelTemp;

        panelTemp = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelTemp, BorderLayout.NORTH );
        comboAudioDevice = new Choice ();
        panelTemp.add ( comboAudioDevice, BorderLayout.CENTER );
        nCount = vectorAudioDevices.size ();
        boolContains = false;
        for ( i = 0;  i < nCount;  i++ ) {
            infoCaptureDevice = (CaptureDeviceInfo) vectorAudioDevices.elementAt ( i );
            strDeviceName = infoCaptureDevice.getName ();
            comboAudioDevice.addItem ( strDeviceName );
            if ( boolContains == false  &&  dataCapture!= null
                                &&  dataCapture.strDeviceName != null ) {
                boolContains = dataCapture.strDeviceName.equals ( strDeviceName );
            }
        }
        if ( boolContains == true )
            comboAudioDevice.select ( dataCapture.strDeviceName );
        comboAudioDevice.addItemListener ( this );
        comboAudioDevice.setEnabled ( boolState );

        panelAudioFormat = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelAudioFormat, BorderLayout.CENTER );
        createAudioChooser ( formatDefault );
        if ( chooserAudio != null )
            chooserAudio.setEnabled ( boolState );

        return ( panelAudio );
    }

    private void createAudioChooser ( AudioFormat formatDefault ) {
        int                     i;
        CaptureDeviceInfo       infoCaptureDevice;
        Format                  arrFormats [];

        if ( panelAudioFormat == null )
            return;

        panelAudioFormat.removeAll ();

        i = comboAudioDevice.getSelectedIndex ();
        infoCaptureDevice = (CaptureDeviceInfo) vectorAudioDevices.elementAt ( i );
        arrFormats = infoCaptureDevice.getFormats ();
        chooserAudio = new AudioFormatChooser ( arrFormats, formatDefault, false, null );
        panelAudioFormat.add ( chooserAudio, BorderLayout.CENTER );
    }

    private void saveCfgData () {
        CaptureDeviceInfo               cdi;
        JMAppsCfg.CaptureDeviceData   dataCapture;

        if ( cfgJMApps == null )
            return;

        // audio
        dataCapture = cfgJMApps.createCaptureDeviceDataObject ();
        dataCapture.boolUse = isAudioDeviceUsed ();
        if ( comboAudioDevice != null )
            dataCapture.strDeviceName = comboAudioDevice.getSelectedItem ();
        if ( chooserAudio != null )
            dataCapture.format = chooserAudio.getFormat ();
        cfgJMApps.setLastCaptureAudioData ( dataCapture );

        // video
        dataCapture = cfgJMApps.createCaptureDeviceDataObject ();
        dataCapture.boolUse = isVideoDeviceUsed ();
        if ( comboVideoDevice != null )
            dataCapture.strDeviceName = comboVideoDevice.getSelectedItem ();
        if ( chooserVideo != null )
            dataCapture.format =  chooserVideo.getFormat ();
        cfgJMApps.setLastCaptureVideoData ( dataCapture );
    }

    public void actionPerformed ( ActionEvent event ) {
        int                  nIndex;
        String               strCmd;
        CaptureDeviceInfo    infoCaptureDevice;


        strCmd = event.getActionCommand ();
        if ( strCmd.equals(ACTION_OK) ) {
            this.setAction ( ACTION_OK );
            saveCfgData ();
        }
        else {
            this.setAction ( ACTION_CANCEL );
        }

        dispose();
    }

    public void itemStateChanged ( ItemEvent event ) {
        Object          objectSource;
        boolean         boolEnable;

        objectSource = event.getSource ();

        if ( objectSource == checkUseVideo ) {
            boolEnable = checkUseVideo.getState ();
            comboVideoDevice.setEnabled ( boolEnable );
            chooserVideo.setEnabled ( boolEnable );
        }
        else if ( objectSource == checkUseAudio ) {
            boolEnable = checkUseAudio.getState ();
            comboAudioDevice.setEnabled ( boolEnable );
            chooserAudio.setEnabled ( boolEnable );
        }
        else if ( objectSource == comboVideoDevice ) {
            createVideoChooser ( null );
            validate ();
        }
        else if ( objectSource == comboAudioDevice ) {
            createAudioChooser ( null );
            validate ();
        }
    }

    public void windowClosing ( WindowEvent event ) {
        this.setAction ( ACTION_CANCEL );
        this.dispose ();
    }

    public String toString () {
        String                  strValue = "";
        CaptureDeviceInfo       cdiAudio;
        CaptureDeviceInfo       cdiVideo;
    	MediaLocator            deviceURL;

        cdiAudio = getAudioDevice();
        if ( cdiAudio != null  &&  isAudioDeviceUsed() ) {
            deviceURL = cdiAudio.getLocator ();
            if ( strValue.length() > 0 )
                strValue = strValue + " & ";
            strValue = strValue + deviceURL.toString ();
        }

        cdiVideo = getVideoDevice ();
        if ( cdiVideo != null  &&  isVideoDeviceUsed() ) {
            deviceURL = cdiVideo.getLocator ();
            if ( strValue.length() > 0 )
                strValue = strValue + " & ";
            strValue = strValue + deviceURL.toString ();
        }

        return ( strValue );
    }


}


