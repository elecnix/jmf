/*
 * @(#)PanelMediaTargetNetwork.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.export;

import java.awt.*;
import java.awt.event.*;

import com.sun.media.util.JMFI18N;

import jmapps.util.*;
import jmapps.ui.*;


public class PanelMediaTargetNetwork extends JMPanel
implements TextListener, FocusListener {

    private JMAppsCfg                 cfgJMApps = null;

    private Panel       panelLabels;
    private Panel       panelAddress;
    private Panel       panelPort;
    private Panel       panelTtl;

    private TextField   arrTextVideoTrackAddress [][] = null;
    private TextField   arrTextAudioTrackAddress [][] = null;
    private TextField   arrTextVideoPort [] = null;
    private TextField   arrTextAudioPort [] = null;
    private Choice      arrComboVideoTtl [] = null;
    private Choice      arrComboAudioTtl [] = null;

    private int         nVideoCount = 0;
    private int         nAudioCount = 0;
    
    private static final int    MAX_TRACK_COUNT = 8;


    public PanelMediaTargetNetwork ( JMAppsCfg cfgJMApps ) {
    	super ();

        this.cfgJMApps = cfgJMApps;
    	try {
    	    init ();
    	}
    	catch ( Exception exception ) {
    	    exception.printStackTrace ();
    	}
    }

    public void setJMStudioCfg ( JMAppsCfg cfgJMApps ) {
        int                     i;
        JMAppsCfg.RtpData     dataRtp;


        this.cfgJMApps = cfgJMApps;
        if ( cfgJMApps == null )
            return;

    	for ( i = 0;  i < MAX_TRACK_COUNT;  i++ ) {
            dataRtp = cfgJMApps.getLastTransmitRtpData ( "Video " + (i + 1) );
            if ( dataRtp != null ) {
                arrTextVideoTrackAddress[i][0].setText ( dataRtp.strAddress0 );
                arrTextVideoTrackAddress[i][1].setText ( dataRtp.strAddress1 );
                arrTextVideoTrackAddress[i][2].setText ( dataRtp.strAddress2 );
                arrTextVideoTrackAddress[i][3].setText ( dataRtp.strAddress3 );
                arrTextVideoPort[i].setText ( dataRtp.strPort );
                arrComboVideoTtl[i].select ( dataRtp.strTtl );
            }
            dataRtp = cfgJMApps.getLastTransmitRtpData ( "Audio " + (i + 1) );
            if ( dataRtp != null ) {
                arrTextAudioTrackAddress[i][0].setText ( dataRtp.strAddress0 );
                arrTextAudioTrackAddress[i][1].setText ( dataRtp.strAddress1 );
                arrTextAudioTrackAddress[i][2].setText ( dataRtp.strAddress2 );
                arrTextAudioTrackAddress[i][3].setText ( dataRtp.strAddress3 );
                arrTextAudioPort[i].setText ( dataRtp.strPort );
                arrComboAudioTtl[i].select ( dataRtp.strTtl );
            }
        }
    }

    public String[] getVideoAddresses () {
    	int         i, j;
    	String      arrValues [];

    	arrValues = new String [nVideoCount];
    	for ( i = 0;  i < nVideoCount;  i++ ) {
    	    arrValues[i] = arrTextVideoTrackAddress[i][0].getText().trim();
            for ( j = 1;  j < 4;  j++ )
    	        arrValues[i] = arrValues[i] + "." + arrTextVideoTrackAddress[i][j].getText().trim();
    	}

    	return ( arrValues );
    }

    public String[] getAudioAddresses () {
    	int         i, j;
    	String      arrValues [];

    	arrValues = new String [nAudioCount];
    	for ( i = 0;  i < nAudioCount;  i++ ) {
    	    arrValues[i] = arrTextAudioTrackAddress[i][0].getText().trim();
            for ( j = 1;  j < 4;  j++ )
    	        arrValues[i] = arrValues[i] + "." + arrTextAudioTrackAddress[i][j].getText().trim();
    	}
    	return ( arrValues );
    }

    public String[] getVideoPorts () {
    	int         i, j;
    	String      arrValues [];

    	arrValues = new String [nVideoCount];
    	for ( i = 0;  i < nVideoCount;  i++ )
    	    arrValues[i] = arrTextVideoPort[i].getText().trim();
    	return ( arrValues );
    }

    public String[] getAudioPorts () {
    	int         i, j;
    	String      arrValues [];

    	arrValues = new String [nAudioCount];
    	for ( i = 0;  i < nAudioCount;  i++ )
    	    arrValues[i] = arrTextAudioPort[i].getText().trim();
    	return ( arrValues );
    }

    public String[] getVideoTtls () {
    	int         i, j;
        String      arrValues [];

    	arrValues = new String [nVideoCount];
    	for ( i = 0;  i < nVideoCount;  i++ )
    	    arrValues[i] = arrComboVideoTtl[i].getSelectedItem().trim();
    	return ( arrValues );
    }

    public String[] getAudioTtls () {
    	int         i, j;
    	String      arrValues [];

    	arrValues = new String [nAudioCount];
    	for ( i = 0;  i < nAudioCount;  i++ )
    	    arrValues[i] = arrComboAudioTtl[i].getSelectedItem().trim();
    	return ( arrValues );
    }

    private void init () throws Exception {
        int                     i, j;
    	Panel	                panel;
    	Panel	                panelTemp;
    	Panel	                panelDescription;
        JMAppsCfg.RtpData     dataRtp;


    	this.setLayout ( new BorderLayout(6,6) );

    	panel = new Panel ( new BorderLayout(6,6) );
    	this.add ( panel, BorderLayout.NORTH );

    	panelDescription = new Panel ( new GridLayout(0,1) );
    	panel.add ( panelDescription, BorderLayout.NORTH );
    	panelDescription.add ( new Label(JMFI18N.getResource("jmstudio.export.targetnw.label1")) );
    	panelDescription.add ( new Label(JMFI18N.getResource("jmstudio.export.targetnw.label2")) );

    	panelLabels = new Panel ( new GridLayout(0,1,6,6) );
    	panel.add ( panelLabels, BorderLayout.WEST );

    	panelAddress = new Panel ( new GridLayout(0,1,6,6) );
    	panel.add ( panelAddress, BorderLayout.CENTER );

    	panelTemp = new Panel ( new BorderLayout(6,6) );
    	panel.add ( panelTemp, BorderLayout.EAST );

    	panelPort = new Panel ( new GridLayout(0,1,6,6) );
    	panelTemp.add ( panelPort, BorderLayout.CENTER );

    	panelTtl = new Panel ( new GridLayout(0,1,6,6) );
    	panelTemp.add ( panelTtl, BorderLayout.EAST );

        arrTextVideoTrackAddress = new TextField [MAX_TRACK_COUNT][4];
        arrTextAudioTrackAddress = new TextField [MAX_TRACK_COUNT][4];
        arrTextVideoPort = new TextField [MAX_TRACK_COUNT];
        arrTextAudioPort = new TextField [MAX_TRACK_COUNT];
        arrComboVideoTtl = new Choice [MAX_TRACK_COUNT];
        arrComboAudioTtl = new Choice [MAX_TRACK_COUNT];
    	for ( i = 0;  i < MAX_TRACK_COUNT;  i++ ) {
    	    for ( j = 0;  j < 4;  j++ ) {
    	    	arrTextVideoTrackAddress[i][j] = new TextField ();
    	    	arrTextVideoTrackAddress[i][j].addTextListener ( this );
		arrTextVideoTrackAddress[i][j].addFocusListener(this);
    	    	arrTextAudioTrackAddress[i][j] = new TextField ();
    	    	arrTextAudioTrackAddress[i][j].addTextListener ( this );
    	    	arrTextAudioTrackAddress[i][j].addFocusListener(this);
    	    }

            arrTextVideoPort[i] = new TextField ( 5 );
	    arrTextVideoPort[i].addFocusListener(this);
            arrTextAudioPort[i] = new TextField ( 5 );
	    arrTextAudioPort[i].addFocusListener(this);
	    
    	    arrComboVideoTtl[i] = new Choice ();
    	    arrComboVideoTtl[i].addItem ( "1" );
    	    arrComboVideoTtl[i].addItem ( "2" );
    	    arrComboVideoTtl[i].addItem ( "3" );
    	    arrComboVideoTtl[i].addItem ( "4" );
    	    arrComboVideoTtl[i].addItem ( "8" );
    	    arrComboVideoTtl[i].addItem ( "16" );
    	    arrComboVideoTtl[i].addItem ( "32" );
    	    arrComboVideoTtl[i].addItem ( "64" );
    	    arrComboVideoTtl[i].addItem ( "128" );
    	    arrComboVideoTtl[i].addItem ( "255" );

    	    arrComboAudioTtl[i] = new Choice ();
    	    arrComboAudioTtl[i].addItem ( "1" );
    	    arrComboAudioTtl[i].addItem ( "2" );
    	    arrComboAudioTtl[i].addItem ( "3" );
    	    arrComboAudioTtl[i].addItem ( "4" );
    	    arrComboAudioTtl[i].addItem ( "8" );
    	    arrComboAudioTtl[i].addItem ( "16" );
    	    arrComboAudioTtl[i].addItem ( "32" );
    	    arrComboAudioTtl[i].addItem ( "64" );
    	    arrComboAudioTtl[i].addItem ( "128" );
    	    arrComboAudioTtl[i].addItem ( "255" );

            if ( cfgJMApps != null ) {
                dataRtp = cfgJMApps.getLastTransmitRtpData ( "Video " + (i + 1) );
                if ( dataRtp != null ) {
                    arrTextVideoTrackAddress[i][0].setText ( dataRtp.strAddress0 );
                    arrTextVideoTrackAddress[i][1].setText ( dataRtp.strAddress1 );
                    arrTextVideoTrackAddress[i][2].setText ( dataRtp.strAddress2 );
                    arrTextVideoTrackAddress[i][3].setText ( dataRtp.strAddress3 );
                    arrTextVideoPort[i].setText ( dataRtp.strPort );
                    arrComboVideoTtl[i].select ( dataRtp.strTtl );
                }
                dataRtp = cfgJMApps.getLastTransmitRtpData ( "Audio " + (i + 1) );
                if ( dataRtp != null ) {
                    arrTextAudioTrackAddress[i][0].setText ( dataRtp.strAddress0 );
                    arrTextAudioTrackAddress[i][1].setText ( dataRtp.strAddress1 );
                    arrTextAudioTrackAddress[i][2].setText ( dataRtp.strAddress2 );
                    arrTextAudioTrackAddress[i][3].setText ( dataRtp.strAddress3 );
                    arrTextAudioPort[i].setText ( dataRtp.strPort );
                    arrComboAudioTtl[i].select ( dataRtp.strTtl );
                }
            }

        }
    }

    public void setTracks ( boolean arrVideoTracksEnabled[], boolean arrAudioTracksEnabled[] ) {
    	int	i, j;
    	Label	label;
    	Panel	panelAddressEntry;
        String  strAudio = JMFI18N.getResource("jmstudio.export.targetnw.audio");
        String  strVideo = JMFI18N.getResource("jmstudio.export.targetnw.video");


        nVideoCount = arrVideoTracksEnabled.length;
        nAudioCount = arrAudioTracksEnabled.length;

    	panelLabels.removeAll ();
    	panelAddress.removeAll ();
    	panelPort.removeAll ();
    	panelTtl.removeAll ();

    	label = new Label ( JMFI18N.getResource("jmstudio.export.targetnw.track"), Label.CENTER );
    	panelLabels.add ( label );
    	label = new Label ( JMFI18N.getResource("jmstudio.export.targetnw.sessionaddr"), Label.CENTER );
    	panelAddress.add ( label );
    	label = new Label ( JMFI18N.getResource("jmstudio.export.targetnw.port"), Label.CENTER );
    	panelPort.add ( label );
    	label = new Label ( JMFI18N.getResource("jmstudio.export.targetnw.ttl"), Label.CENTER );
    	panelTtl.add ( label );

    	for ( i = 0;  i < nVideoCount;  i++ ) {
    	    label = new Label ( strVideo + " " + (i + 1), Label.LEFT );
            label.setEnabled ( arrVideoTracksEnabled[i] );
    	    panelLabels.add ( label );

    	    panelAddressEntry = new Panel ( new GridLayout(1,0,2,2) );
    	    panelAddress.add ( panelAddressEntry );
    	    for ( j = 0;  j < 4;  j++ ) {
                arrTextVideoTrackAddress[i][j].setEnabled ( arrVideoTracksEnabled[i] );
    	    	panelAddressEntry.add ( arrTextVideoTrackAddress[i][j] );
    	    }
            arrTextVideoPort[i].setEnabled ( arrVideoTracksEnabled[i] );
    	    panelPort.add ( arrTextVideoPort[i] );
            arrComboVideoTtl[i].setEnabled ( arrVideoTracksEnabled[i] );
    	    panelTtl.add ( arrComboVideoTtl[i] );
    	}

    	for ( i = 0;  i < nAudioCount;  i++ ) {
    	    label = new Label ( strAudio + " " + (i + 1), Label.LEFT );
            label.setEnabled ( arrAudioTracksEnabled[i] );
    	    panelLabels.add ( label );

    	    panelAddressEntry = new Panel ( new GridLayout(1,0,2,2) );
    	    panelAddress.add ( panelAddressEntry );
    	    for ( j = 0;  j < 4;  j++ ) {
                arrTextAudioTrackAddress[i][j].setEnabled ( arrAudioTracksEnabled[i] );
    	    	panelAddressEntry.add ( arrTextAudioTrackAddress[i][j] );
    	    }
            arrTextAudioPort[i].setEnabled ( arrAudioTracksEnabled[i] );
    	    panelPort.add ( arrTextAudioPort[i] );
            arrComboAudioTtl[i].setEnabled ( arrAudioTracksEnabled[i] );
    	    panelTtl.add ( arrComboAudioTtl[i] );
    	}

    }

    public void textValueChanged ( TextEvent event ) {
    	Object		objectSource;
    	TextField	textField;
    	String		strValue;

    	objectSource = event.getSource ();
    	if ( objectSource instanceof TextField ) {
    	    textField = (TextField) objectSource;
    	    strValue = textField.getText ();
    	    if ( strValue.length() > 2 )
    	    	textField.transferFocus ();
    	}
    }

    public boolean checkValidFields ( boolean boolDisplayMessage ) {
        int       i, j;
        int       nValue;
        String    strValue;


        for ( i = 0;  i < nVideoCount;  i++ ) {
            for ( j = 0;  j < 4;  j++ ) {
                if ( !arrTextVideoTrackAddress[i][j].isEnabled() )
                    continue;
                strValue = arrTextVideoTrackAddress[i][j].getText ();
                try {
                    nValue = Integer.valueOf(strValue).intValue();
                }
                catch ( Exception exception ) {
                    nValue = -1;
                }
                if ( nValue < 0  ||  nValue > 255 ) {
                    if ( boolDisplayMessage == true )
                        MessageDialog.createErrorDialog ( getFrame(),
                                        JMFI18N.getResource("jmstudio.error.video.sessionaddr")
                                        + " " + (i + 1) );
                    return ( false );
                }
            }

            if ( !arrTextVideoPort[i].isEnabled() )
                continue;
            strValue = arrTextVideoPort[i].getText ();
            try {
                nValue = Integer.valueOf(strValue).intValue();
            }
            catch ( Exception exception ) {
                nValue = -1;
            }
            if ( nValue < 0  ||  (nValue & 0x0001) == 0x0001 ) {
                if ( boolDisplayMessage == true )
                    MessageDialog.createErrorDialog ( getFrame(),
                                JMFI18N.getResource("jmstudio.error.video.port")
                                + " " + (i + 1) );
                return ( false );
            }

        }

        for ( i = 0;  i < nAudioCount;  i++ ) {
            for ( j = 0;  j < 4;  j++ ) {
                if ( !arrTextAudioTrackAddress[i][j].isEnabled() )
                    continue;
                strValue = arrTextAudioTrackAddress[i][j].getText ();
                try {
                    nValue = Integer.valueOf(strValue).intValue();
                }
                catch ( Exception exception ) {
                    nValue = -1;
                }
                if ( nValue < 0  ||  nValue > 255 ) {
                    if ( boolDisplayMessage == true )
                        MessageDialog.createErrorDialog ( getFrame(),
                                        JMFI18N.getResource("jmstudio.error.audio.sessionaddr")
                                        + " " + (i + 1) );
                    return ( false );
                }
            }

            if ( !arrTextAudioPort[i].isEnabled() )
                continue;
            strValue = arrTextAudioPort[i].getText ();
            try {
                nValue = Integer.valueOf(strValue).intValue();
            }
            catch ( Exception exception ) {
                nValue = -1;
            }
            if ( nValue < 0  ||  (nValue & 0x0001) == 0x0001 ) {
                if ( boolDisplayMessage == true )
                    MessageDialog.createErrorDialog ( getFrame(),
                                JMFI18N.getResource("jmstudio.error.audio.port")
                                + " " + (i + 1) );
                return ( false );
            }

        }

        return ( true );
    }

    public void saveData () {
        int                     i;
        JMAppsCfg.RtpData     dataRtp;


        if ( cfgJMApps == null )
            return;

    	for ( i = 0;  i < MAX_TRACK_COUNT;  i++ ) {
            dataRtp = cfgJMApps.createRtpDataObject ();
            dataRtp.strAddress0 = arrTextVideoTrackAddress[i][0].getText ();
            dataRtp.strAddress1 = arrTextVideoTrackAddress[i][1].getText ();
            dataRtp.strAddress2 = arrTextVideoTrackAddress[i][2].getText ();
            dataRtp.strAddress3 = arrTextVideoTrackAddress[i][3].getText ();
            dataRtp.strPort = arrTextVideoPort[i].getText ();
            dataRtp.strTtl = arrComboVideoTtl[i].getSelectedItem ();
            cfgJMApps.setLastTransmitRtpData ( dataRtp, "Video " + (i + 1) );

            dataRtp = cfgJMApps.createRtpDataObject ();
            dataRtp.strAddress0 = arrTextAudioTrackAddress[i][0].getText ();
            dataRtp.strAddress1 = arrTextAudioTrackAddress[i][1].getText ();
            dataRtp.strAddress2 = arrTextAudioTrackAddress[i][2].getText ();
            dataRtp.strAddress3 = arrTextAudioTrackAddress[i][3].getText ();
            dataRtp.strPort = arrTextAudioPort[i].getText ();
            dataRtp.strTtl = arrComboAudioTtl[i].getSelectedItem ();
            cfgJMApps.setLastTransmitRtpData ( dataRtp, "Audio " + (i + 1) );
        }
    }

    public void focusGained(FocusEvent fe) {
	Object source = fe.getSource();
	// Select the text in the field
	if (source instanceof TextField)
	    ((TextField)source).selectAll();
    }

    public void focusLost(FocusEvent fe) {
	Object source = fe.getSource();
	// Deselect the text in the field
	if (source instanceof TextField)
	    ((TextField)source).select(1, 0);
    }

}


