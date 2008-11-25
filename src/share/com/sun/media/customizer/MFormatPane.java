/*
 * @(#)MFormatPane.java	1.9 02/12/17
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  This class defines the media format page. It list all the file
 *  format supported by jmf 2.x
 *
 *  @version 2.0
 */

public class MFormatPane extends JPanel {
    // the sequence is critical for codecpane
    public static final int AU = 0;
    public static final int AIFF = 1;
    public static final int GSM = 2;
    public static final int WAV = 3;
    public static final int MP2 = 4;
    public static final int MP3 = 5;
    public static final int MOV = 6;
    public static final int AVI = 7;
    public static final int MPEG = 8;
    public static final int MVR = 9;
    public static final int MIDI = 10;
    public static final int RMF = 11;
    public static final int CDAUDIO = 12;

    JCheckBox[] mformats = new JCheckBox[13];
    boolean[] resultMFormat = new boolean[13];

    public MFormatPane() {
	mformats[AU] = new JCheckBox(I18N.getResource("MFormatPane.AU"), false);
	mformats[AIFF] = new JCheckBox(I18N.getResource("MFormatPane.AIFF"), false);
	mformats[GSM] = new JCheckBox(I18N.getResource("MFormatPane.GSM"), false);
	mformats[MP2] = new JCheckBox(I18N.getResource("MFormatPane.MP2"), false);
	mformats[MP3] = new JCheckBox(I18N.getResource("MFormatPane.MP3"), false);
	mformats[WAV] = new JCheckBox(I18N.getResource("MFormatPane.WAV"), false);
	mformats[MOV] = new JCheckBox(I18N.getResource("MFormatPane.MOV"), false);
	mformats[AVI] = new JCheckBox(I18N.getResource("MFormatPane.AVI"), false);
	mformats[MPEG] = new JCheckBox(I18N.getResource("MFormatPane.MPEG"), false);
	mformats[MVR] = new JCheckBox(I18N.getResource("MFormatPane.MVR"), false);
	mformats[MIDI] = new JCheckBox(I18N.getResource("MFormatPane.MIDI"), false);
	mformats[RMF] = new JCheckBox(I18N.getResource("MFormatPane.RMF"), false);
	mformats[CDAUDIO] = new JCheckBox(I18N.getResource("MFormatPane.CDAUDIO"), false);

	setLayout(new GridLayout(5,3));
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("MFormatPane.TITLE")));

	for ( int i = 0; i < 13; i++) {
	    mformats[i].setEnabled(false);
	    add(mformats[i]);
	}
    }

    public boolean[] getState() {
	for (int i = 0; i < 13; i++) {
	    if (mformats[i].isEnabled() && mformats[i].isSelected())
		resultMFormat[i] = true;
	    else 
		resultMFormat[i] = false;
	} 
	return (resultMFormat);
    }

    public void setHighlight(boolean[] funcs, boolean[] protocols, int release) {
	// mvr/swf/spl/midi/rmf only supported in playback
	boolean value = (protocols[ProtocolPane.FILE] ||
	                protocols[ProtocolPane.HTTP] ||
	                protocols[ProtocolPane.HTTPS] ||
	                protocols[ProtocolPane.FTP] ) &&
	                funcs[GeneralPane.PLAY];
	mformats[MVR].setEnabled(value);
	mformats[MIDI].setEnabled(value);
	mformats[RMF].setEnabled(value);
	
	// mpeg only supported when loading from files and transmitting
	// can't play or dump into file
	value = (funcs[GeneralPane.RTPTRANS] && protocols[ProtocolPane.RTP] )
                || (release > 1 );
	mformats[MPEG].setEnabled(value);
	
	// other media formats
	value = ( protocols[ProtocolPane.FILE] ||
	          protocols[ProtocolPane.HTTP] ||
	          protocols[ProtocolPane.HTTPS] ||
		  protocols[ProtocolPane.FTP] ) &&
	          funcs[GeneralPane.MFILE];
	for ( int i = AU; i <= AVI; i++)
	    mformats[i].setEnabled(value);

	value = ( release == 3 );
	mformats[CDAUDIO].setEnabled(value);

	mformats[MP3].setEnabled(false);

    }

    public void disableAll() {
	for ( int i = 0; i < 13; i++)
	    mformats[i].setEnabled(false);
    }

}

