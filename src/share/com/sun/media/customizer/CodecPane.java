/*
 * @(#)CodecPane.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/** 
 *  This class defines the codec page
 *
 *  @version 2.0
 */

public class CodecPane extends JPanel {
    /*
    public static final int MPA = 0;
    public static final int MSADPCM = 1;
    public static final int ULAW = 2;
    public static final int ALAW = 3;
    public static final int GSM = 4;
    public static final int DVI = 5;
    public static final int IMA4 = 6;
    public static final int IMA4_MS = 7;
    public static final int GSM_MS = 8;
    public static final int MPARTP = 9;
    public static final int ULAWRTP = 10;
    public static final int GSMRTP = 11;
    public static final int DVIRTP = 12;
    public static final int G723RTP = 13;
    public static final int CINEPAK = 14;
    public static final int H263 = 15;
    public static final int JPEGRTP = 16;
    public static final int MPEGRTP = 17;
    public static final int JPEG = 18;
    */

    // Decoder
    public static final int MPAJD = 0;
    public static final int ULAWJD = 1;
    public static final int MSGSMJD = 2;
    public static final int GSMJD  = 3;
    public static final int ALAWJD  = 4;
    public static final int MSIMA4JD = 5;
    public static final int IMA4JD = 6;
    public static final int DVIJD = 7;
    public static final int G723JD = 8;
    public static final int MSADPCMJD = 9;
    public static final int MPAND = 10;
    public static final int MSGSMND = 11;
    public static final int GSMND = 12;
    public static final int G723ND = 13;
    public static final int ACMND = 14;
    /////
    public static final int CINEPAKJD = 15 ;
    public static final int H263JD = 16;
    public static final int CINEPAKND = 17;
    public static final int H263ND = 18;
    public static final int H261ND = 19;
    public static final int JPEGND = 20;
    public static final int MPEGND = 21;
    public static final int IV32ND = 22;
    public static final int MPEGPLY = 23;
    public static final int VCMND = 24;

    // Encoder
    public static final int ULAWJE = 25;
    public static final int MSGSMJE = 26;
    public static final int GSMJE = 27;
    public static final int MSIMA4JE = 28;
    public static final int IMA4JE = 29;
    public static final int DVIJE = 30;
    public static final int MPANE = 31;
    public static final int MSGSMNE = 32;
    public static final int GSMNE = 33;
    public static final int G723NE = 34;
    public static final int ACMNE = 35;
    //////
    public static final int CINEPAKPRONE = 36;
    public static final int H263NE = 37;
    public static final int JPEGNE = 38;
    public static final int VCMNE = 39;

    //DePacketizer
    public static final int MPADRTP = 40;
    public static final int ULAWDRTP = 41;
    public static final int GSMDRTP = 42;
    public static final int DVIDRTP = 43;
    public static final int G723DRTP = 44;
    ///
    public static final int H263DRTP = 45;
    public static final int H261DRTP = 46;
    public static final int JPEGDRTP = 47;
    public static final int MPEGDRTP = 48;

    // Packetizer
    public static final int MPAPRTP = 49;
    public static final int ULAWPRTP = 50;
    public static final int GSMPRTP = 51;
    public static final int DVIPRTP = 52;
    public static final int G723PRTP = 53;
    ////
    public static final int H263PRTP = 54;
    public static final int JPEGPRTP = 55;
    public static final int MPEGPRTP = 56;

    JCheckBox[] codecs = new JCheckBox[57];
    boolean[] resultCodec = new boolean[57];

    // even though we support 12 media formats, but only
    // 8 of them have the codecs
    Vector[] knowledge = new Vector[9];

    public CodecPane() {
	codecs[MPAJD] = new JCheckBox(I18N.getResource("CodecPane.MPAJD"), false);
	codecs[ULAWJD] = new JCheckBox(I18N.getResource("CodecPane.ULAWJD"), false);
	codecs[MSGSMJD] = new JCheckBox(I18N.getResource("CodecPane.MSGSMJD"), false);
	codecs[GSMJD] = new JCheckBox(I18N.getResource("CodecPane.GSMJD"), false);
	codecs[ALAWJD] = new JCheckBox(I18N.getResource("CodecPane.ALAWJD"), false);
	codecs[MSIMA4JD] = new JCheckBox(I18N.getResource("CodecPane.MSIMA4JD"), false);
	codecs[IMA4JD] = new JCheckBox(I18N.getResource("CodecPane.IMA4JD"), false);
	codecs[DVIJD] = new JCheckBox(I18N.getResource("CodecPane.DVIJD"), false);
	codecs[G723JD] = new JCheckBox(I18N.getResource("CodecPane.G723JD"), false);
	codecs[MSADPCMJD] = new JCheckBox(I18N.getResource("CodecPane.MSADPCMJD"), false);
	codecs[MPAND] = new JCheckBox(I18N.getResource("CodecPane.MPAND"), false);
	codecs[MSGSMND] = new JCheckBox(I18N.getResource("CodecPane.MSGSMND"), false);
	codecs[GSMND] = new JCheckBox(I18N.getResource("CodecPane.GSMND"), false);
	codecs[G723ND] = new JCheckBox(I18N.getResource("CodecPane.G723ND"), false);
	codecs[ACMND] = new JCheckBox(I18N.getResource("CodecPane.ACMND"), false);
	codecs[CINEPAKJD] = new JCheckBox(I18N.getResource("CodecPane.CINEPAKJD"), false);
	codecs[H263JD] = new JCheckBox(I18N.getResource("CodecPane.H263JD"), false);
	codecs[CINEPAKND] = new JCheckBox(I18N.getResource("CodecPane.CINEPAKND"), false);
	codecs[H263ND] = new JCheckBox(I18N.getResource("CodecPane.H263ND"), false);
	codecs[H261ND] = new JCheckBox(I18N.getResource("CodecPane.H261ND"), false);
	codecs[JPEGND] = new JCheckBox(I18N.getResource("CodecPane.JPEGND"), false);
	codecs[MPEGND] = new JCheckBox(I18N.getResource("CodecPane.MPEGND"), false);
	codecs[IV32ND] = new JCheckBox(I18N.getResource("CodecPane.IV32ND"), false);
	codecs[VCMND] = new JCheckBox(I18N.getResource("CodecPane.VCMND"), false);
	codecs[MPEGPLY] = new JCheckBox(I18N.getResource("CodecPane.MPEGPLY"), false);
	codecs[ULAWJE] = new JCheckBox(I18N.getResource("CodecPane.ULAWJE"), false);
	codecs[MSGSMJE] = new JCheckBox(I18N.getResource("CodecPane.MSGSMJE"), false);
	codecs[GSMJE] = new JCheckBox(I18N.getResource("CodecPane.GSMJE"), false);
	codecs[MSIMA4JE] = new JCheckBox(I18N.getResource("CodecPane.MSIMA4JE"), false);
	codecs[IMA4JE] = new JCheckBox(I18N.getResource("CodecPane.IMA4JE"), false);
	codecs[DVIJE] = new JCheckBox(I18N.getResource("CodecPane.DVIJE"), false);
	codecs[MPANE] = new JCheckBox(I18N.getResource("CodecPane.MPANE"), false);
	codecs[MSGSMNE] = new JCheckBox(I18N.getResource("CodecPane.MSGSMNE"), false);
	codecs[GSMNE] = new JCheckBox(I18N.getResource("CodecPane.GSMNE"), false);
	codecs[G723NE] = new JCheckBox(I18N.getResource("CodecPane.G723NE"), false);
	codecs[ACMNE] = new JCheckBox(I18N.getResource("CodecPane.ACMNE"), false);
	codecs[CINEPAKPRONE] = new JCheckBox(I18N.getResource("CodecPane.CINEPAKPRONE"), false);
	codecs[H263NE] = new JCheckBox(I18N.getResource("CodecPane.H263NE"), false);
	codecs[JPEGNE] = new JCheckBox(I18N.getResource("CodecPane.JPEGNE"), false);
	codecs[VCMNE] = new JCheckBox(I18N.getResource("CodecPane.VCMNE"), false);
	codecs[MPADRTP] = new JCheckBox(I18N.getResource("CodecPane.MPADRTP"), false);
	codecs[ULAWDRTP] = new JCheckBox(I18N.getResource("CodecPane.ULAWDRTP"), false);
	codecs[GSMDRTP] = new JCheckBox(I18N.getResource("CodecPane.GSMDRTP"), false);
	codecs[DVIDRTP] = new JCheckBox(I18N.getResource("CodecPane.DVIDRTP"), false);
	codecs[G723DRTP] = new JCheckBox(I18N.getResource("CodecPane.G723DRTP"), false);
	codecs[H263DRTP] = new JCheckBox(I18N.getResource("CodecPane.H263DRTP"), false);
	codecs[H261DRTP] = new JCheckBox(I18N.getResource("CodecPane.H261DRTP"), false);
	codecs[JPEGDRTP] = new JCheckBox(I18N.getResource("CodecPane.JPEGDRTP"), false);
	codecs[MPEGDRTP] = new JCheckBox(I18N.getResource("CodecPane.MPEGDRTP"), false);
	codecs[MPAPRTP] = new JCheckBox(I18N.getResource("CodecPane.MPAPRTP"), false);
	codecs[ULAWPRTP] = new JCheckBox(I18N.getResource("CodecPane.ULAWPRTP"), false);
	codecs[GSMPRTP] = new JCheckBox(I18N.getResource("CodecPane.GSMPRTP"), false);
	codecs[DVIPRTP] = new JCheckBox(I18N.getResource("CodecPane.DVIPRTP"), false);
	codecs[G723PRTP] = new JCheckBox(I18N.getResource("CodecPane.G723PRTP"), false);
	codecs[H263PRTP] = new JCheckBox(I18N.getResource("CodecPane.H263PRTP"), false);
	codecs[JPEGPRTP] = new JCheckBox(I18N.getResource("CodecPane.JPEGPRTP"), false);
	codecs[MPEGPRTP] = new JCheckBox(I18N.getResource("CodecPane.MPEGPRTP"), false);

	// create the tabbed pane
	JTabbedPane tabPane = new JTabbedPane();
	JPanel decoderPane = makeDecoderPane();
	JPanel encoderPane = makeEncoderPane();
	JPanel depacPane = makeDepacPane();
	JPanel pacPane = makePacPane();
	tabPane.addTab("Decoder", decoderPane);
	tabPane.addTab("Encoder", encoderPane);
	tabPane.addTab("DePacketizer", depacPane);
	tabPane.addTab("Packetizer", pacPane);

	this.setLayout(new GridLayout(1,1));
	this.add(tabPane);

	buildKnowledge();
    }

    JPanel makeDecoderPane() {
	JPanel apane = new JPanel ( new GridLayout(4,4));
	for ( int i = MPAJD; i <= ACMND; i++) {
	    codecs[i].setEnabled(false);
	    apane.add(codecs[i]);
	}
	JPanel vpane = new JPanel( new GridLayout(3,4));
	for ( int i = CINEPAKJD; i <= VCMND; i++) {
	    codecs[i].setEnabled(false);
	    vpane.add(codecs[i]);
	}

	apane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("CodecPane.ADECODER")));
	vpane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("CodecPane.VDECODER")));
	    
	JPanel ret = new JPanel();
	ret.setLayout(new GridLayout(2,1));
	ret.add(apane);
	ret.add(vpane);
	
	return ret;
    }
    
    JPanel makeEncoderPane() {
	JPanel apane = new JPanel ( new GridLayout(4,3));
	for ( int i = ULAWJE; i <= ACMNE; i++) {
	    codecs[i].setEnabled(false);
	    apane.add(codecs[i]);
	}
	JPanel vpane = new JPanel( new GridLayout(2,2));
	for ( int i = CINEPAKPRONE; i <= VCMNE; i++) {
	    codecs[i].setEnabled(false);
	    vpane.add(codecs[i]);
	}

	apane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("CodecPane.AENCODER")));
	vpane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("CodecPane.VENCODER")));
	    
	JPanel ret = new JPanel();
	ret.setLayout(new GridLayout(2,1));
	ret.add(apane);
	ret.add(vpane);
	
	return ret;
    }

    JPanel makePacPane() {
	JPanel apane = new JPanel ( new GridLayout(3,2));
	for ( int i = MPAPRTP; i <= G723PRTP; i++) {
	    codecs[i].setEnabled(false);
	    apane.add(codecs[i]);
	}
	JPanel vpane = new JPanel( new GridLayout(2,2));
	for ( int i = H263PRTP; i <= MPEGPRTP; i++) {
	    codecs[i].setEnabled(false);
	    vpane.add(codecs[i]);
	}

	apane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("CodecPane.APAC")));
	vpane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("CodecPane.VPAC")));
	    
	JPanel ret = new JPanel();
	ret.setLayout(new GridLayout(2,1));
	ret.add(apane);
	ret.add(vpane);
	
	return ret;
    }

    JPanel makeDepacPane() {
	JPanel apane = new JPanel ( new GridLayout(3,2));
	for ( int i = MPADRTP; i <= G723DRTP; i++) {
	    codecs[i].setEnabled(false);
	    apane.add(codecs[i]);
	}
	JPanel vpane = new JPanel( new GridLayout(2,2));
	for ( int i = H263DRTP; i <= MPEGDRTP; i++) {
	    codecs[i].setEnabled(false);
	    vpane.add(codecs[i]);
	}

	apane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("CodecPane.ADEPAC")));
	vpane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("CodecPane.VDEPAC")));
	    
	JPanel ret = new JPanel();
	ret.setLayout(new GridLayout(2,1));
	ret.add(apane);
	ret.add(vpane);
	
	return ret;
    }

    
    public boolean[] getState() {
	for (int i = 0; i < 57; i++) {
	    if (codecs[i].isEnabled() && codecs[i].isSelected())
		resultCodec[i] = true;
	    else 
		resultCodec[i] = false;
	} 
	return (resultCodec);
    }

    public void setHighlight(boolean[] funcs, boolean[] protocols, boolean[] mformats, boolean[] muxs, int release) {
	
	boolean spp = (release == 2);
	boolean wpp = (release == 3);
	boolean notaj = (release >= 2);
	boolean value;
	Integer vv;

	// if the codec chain is too complicated, maybe has problems.
	// Decoders
	if ( funcs[GeneralPane.MFILE] ) {
	    for ( int i = MPAJD; i<= MSADPCMJD; i++) {
		vv = new Integer(i);
		value = false;

		for ( int j = 0; j < 9; j++)
		    if ( knowledge[j].contains(vv) )
			value |= mformats[j];
		codecs[i].setEnabled(value);
	    }

	    for ( int i = CINEPAKJD; i <= H263JD; i++) {
		vv = new Integer(i);
		value = false;

		for ( int j = 0; j < 9; j++)
		    if ( knowledge[j].contains(vv) )
			value |= mformats[j];
		codecs[i].setEnabled(value);
	    }
	    
	    // native decoders
	    if ( !notaj ) {
		for ( int i = MPAND; i <= ACMND; i++) 
		    codecs[i].setEnabled(false);
		for ( int i = CINEPAKND; i <= VCMND; i++)
		    codecs[i].setEnabled(false);
	    } else {
		for ( int i = MPAND; i<= G723ND; i++) {
		    vv = new Integer(i);
		    value = false;
		    
		    for ( int j = 0; j < 9; j++)
			if ( knowledge[j].contains(vv) )
			    value |= mformats[j];
		    codecs[i].setEnabled(value);
		}
	    
		for ( int i = CINEPAKND; i<= MPEGPLY; i++) {
		    vv = new Integer(i);
		    value = false;
		    for ( int j = 0; j < 9; j++) 
			if (knowledge[j].contains(vv) )
			    value |= mformats[j];
		    codecs[i].setEnabled(value);
		}

		value = wpp && (mformats[MFormatPane.AVI] || mformats[MFormatPane.WAV]);
		codecs[ACMND].setEnabled(value);
		codecs[VCMND].setEnabled(value);
		if ( spp )
		    codecs[IV32ND].setEnabled(false);
	    }
	}

	// Encoders
	boolean value1 = funcs[GeneralPane.RTPTRANS] && 
	                 protocols[ProtocolPane.RTP];
	if ( value1 ) { // rtp transmitting case, enable almost all the endcoders
	    for ( int i = ULAWJE; i <= DVIJE; i++) 
		codecs[i].setEnabled(true);
	    if ( !notaj ) { // all-java case
		for ( int i = MPANE; i <= VCMNE ; i++)
		    codecs[i].setEnabled(false);
	    } else {
		for ( int i = MPANE; i <= VCMNE ; i++)
		    codecs[i].setEnabled(true);
		//codecs[CINEPAKPRONE].setEnabled(false);
		//codecs[VCMNE].setEnabled(false);
		//codecs[ACMNE].setEnabled(false);
	    }
	} else if ( funcs[GeneralPane.TRANSCODE]) {
	    // java encoders
	    for ( int i = ULAWJE; i <= DVIJE; i++) {
		vv = new Integer(i);
		value = false;
		for ( int j = 0; j < 8; j++) 
		    if (knowledge[j].contains(vv) )
			value |= muxs[j];
		codecs[i].setEnabled(value);
	    }
	    
	    // native encoders
	    if ( !notaj ) {
		for ( int i = MPANE; i <= VCMNE ; i++)
		    codecs[i].setEnabled(false);
	    } else {
		for ( int i = MPANE; i <= VCMNE; i++) {
		    vv = new Integer(i);
		    value = false;
		    for ( int j = 0; j < 8; j++) 
			if (knowledge[j].contains(vv) )
			    value |= muxs[j];
		    codecs[i].setEnabled(value);
		}
		
		value = codecs[CINEPAKPRONE].isEnabled() && spp;
		codecs[CINEPAKPRONE].setEnabled(value);
		value = codecs[VCMNE].isEnabled() && wpp;
		codecs[VCMNE].setEnabled(value);
		codecs[ACMNE].setEnabled(value);
	    }
	} 
	
	// decoders case for capture source or rtp source
        value = protocols[ProtocolPane.JAVASOUND] ||
	    protocols[ProtocolPane.RTP] ||
	    protocols[ProtocolPane.VFW] ||
	    protocols[ProtocolPane.SVDO] ||
	    protocols[ProtocolPane.SVDOPLS];
	
	if ( value ) {
	    // audio
	    for ( int i = MPAJD; i <= MSADPCMJD; i++)
		codecs[i].setEnabled(true);
	    for ( int i = MPAND; i<= G723ND; i++)
		codecs[i].setEnabled(notaj);
	    codecs[ACMND].setEnabled(wpp);
	    
	    //video
	    for ( int i = CINEPAKJD; i <= H263JD; i++)
		codecs[i].setEnabled(true);
	    for ( int i = CINEPAKND; i <= IV32ND; i++)
		codecs[i].setEnabled(notaj);
	    codecs[VCMND].setEnabled(wpp);
	    if ( spp )
		codecs[IV32ND].setEnabled(false);
	}
	
	
	// DePacketizer and Packetizer
	for (int i = MPADRTP; i <= MPEGPRTP; i++)
	    codecs[i].setEnabled(protocols[ProtocolPane.RTP]);
	
    }

    /**
     *  This method the knowledge about which media format support which
     * codecs
     */
    private void buildKnowledge() {
	knowledge[MFormatPane.AU] = new Vector();
	knowledge[MFormatPane.AU].addElement(new Integer(ULAWJD));
	knowledge[MFormatPane.AU].addElement(new Integer(ULAWJE));
	knowledge[MFormatPane.AU].addElement(new Integer(ULAWDRTP));
	knowledge[MFormatPane.AU].addElement(new Integer(ULAWPRTP));
	knowledge[MFormatPane.AU].addElement(new Integer(ALAWJD));

	knowledge[MFormatPane.AIFF] = new Vector();
	knowledge[MFormatPane.AIFF].addElement(new Integer(ULAWJD));
	knowledge[MFormatPane.AIFF].addElement(new Integer(ULAWJE));
	knowledge[MFormatPane.AIFF].addElement(new Integer(ULAWDRTP));
	knowledge[MFormatPane.AIFF].addElement(new Integer(ULAWPRTP));
	knowledge[MFormatPane.AIFF].addElement(new Integer(ALAWJD));
	knowledge[MFormatPane.AIFF].addElement(new Integer(MSADPCMJD));

	knowledge[MFormatPane.GSM] = new Vector();
	knowledge[MFormatPane.GSM].addElement(new Integer(GSMJD));
	knowledge[MFormatPane.GSM].addElement(new Integer(GSMND));
	knowledge[MFormatPane.GSM].addElement(new Integer(GSMJE));
	knowledge[MFormatPane.GSM].addElement(new Integer(GSMNE));
	knowledge[MFormatPane.GSM].addElement(new Integer(GSMDRTP));
	knowledge[MFormatPane.GSM].addElement(new Integer(GSMPRTP));
	
	knowledge[MFormatPane.MP2] = new Vector();
	knowledge[MFormatPane.MP2].addElement(new Integer(MPAJD));
	knowledge[MFormatPane.MP2].addElement(new Integer(MPAND));
	knowledge[MFormatPane.MP2].addElement(new Integer(MPANE));
	knowledge[MFormatPane.MP2].addElement(new Integer(MPADRTP));
	knowledge[MFormatPane.MP2].addElement(new Integer(MPAPRTP));


	knowledge[MFormatPane.MP3] = new Vector();
	knowledge[MFormatPane.MP3].addElement(new Integer(MPAJD));
	knowledge[MFormatPane.MP3].addElement(new Integer(MPAND));
	knowledge[MFormatPane.MP3].addElement(new Integer(MPANE));
	knowledge[MFormatPane.MP3].addElement(new Integer(MPADRTP));
	knowledge[MFormatPane.MP3].addElement(new Integer(MPAPRTP));

	knowledge[MFormatPane.WAV] = new Vector();
	knowledge[MFormatPane.WAV].addElement(new Integer(ULAWJD));
	knowledge[MFormatPane.WAV].addElement(new Integer(ULAWJE));
	knowledge[MFormatPane.WAV].addElement(new Integer(ULAWDRTP));
	knowledge[MFormatPane.WAV].addElement(new Integer(ULAWPRTP));
	knowledge[MFormatPane.WAV].addElement(new Integer(ALAWJD));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSGSMJD));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSGSMND));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSGSMJE));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSGSMNE));
	knowledge[MFormatPane.WAV].addElement(new Integer(DVIJD));
	knowledge[MFormatPane.WAV].addElement(new Integer(DVIJE));
	knowledge[MFormatPane.WAV].addElement(new Integer(DVIDRTP));
	knowledge[MFormatPane.WAV].addElement(new Integer(DVIPRTP));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSADPCMJD));
	knowledge[MFormatPane.WAV].addElement(new Integer(MPAJD));
	knowledge[MFormatPane.WAV].addElement(new Integer(MPAND));
	knowledge[MFormatPane.WAV].addElement(new Integer(MPANE));
	knowledge[MFormatPane.WAV].addElement(new Integer(MPADRTP));
	knowledge[MFormatPane.WAV].addElement(new Integer(MPAPRTP));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSIMA4JD));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSIMA4JE));
	knowledge[MFormatPane.WAV].addElement(new Integer(ACMND));
	knowledge[MFormatPane.WAV].addElement(new Integer(ACMNE));

	knowledge[MFormatPane.MOV] = new Vector();
	knowledge[MFormatPane.MOV].addElement(new Integer(ULAWJD));
	knowledge[MFormatPane.MOV].addElement(new Integer(ULAWJE));
	knowledge[MFormatPane.MOV].addElement(new Integer(ULAWDRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(ULAWPRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(ALAWJD));
	knowledge[MFormatPane.MOV].addElement(new Integer(GSMJD));
	knowledge[MFormatPane.MOV].addElement(new Integer(GSMND));
	knowledge[MFormatPane.MOV].addElement(new Integer(GSMJE));
	knowledge[MFormatPane.MOV].addElement(new Integer(GSMNE));
	knowledge[MFormatPane.MOV].addElement(new Integer(GSMDRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(GSMPRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(IMA4JD));
	knowledge[MFormatPane.MOV].addElement(new Integer(IMA4JE));
	knowledge[MFormatPane.MOV].addElement(new Integer(CINEPAKJD));
	knowledge[MFormatPane.MOV].addElement(new Integer(CINEPAKND));
	knowledge[MFormatPane.MOV].addElement(new Integer(CINEPAKPRONE));
	knowledge[MFormatPane.MOV].addElement(new Integer(H263JD));
	knowledge[MFormatPane.MOV].addElement(new Integer(H263ND));
	knowledge[MFormatPane.MOV].addElement(new Integer(H263NE));
	knowledge[MFormatPane.MOV].addElement(new Integer(H263DRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(H263PRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(JPEGND));
	knowledge[MFormatPane.MOV].addElement(new Integer(JPEGNE));
	knowledge[MFormatPane.MOV].addElement(new Integer(JPEGDRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(JPEGPRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(H261ND));
	knowledge[MFormatPane.MOV].addElement(new Integer(H261DRTP));
	knowledge[MFormatPane.MOV].addElement(new Integer(IV32ND));

	knowledge[MFormatPane.AVI] = new Vector();
	knowledge[MFormatPane.AVI].addElement(new Integer(DVIJD));
	knowledge[MFormatPane.AVI].addElement(new Integer(DVIJE));
	knowledge[MFormatPane.AVI].addElement(new Integer(DVIDRTP));
	knowledge[MFormatPane.AVI].addElement(new Integer(DVIPRTP));
	knowledge[MFormatPane.AVI].addElement(new Integer(ULAWJD));
	knowledge[MFormatPane.AVI].addElement(new Integer(ULAWJE));
	knowledge[MFormatPane.AVI].addElement(new Integer(ULAWDRTP));
	knowledge[MFormatPane.AVI].addElement(new Integer(ULAWPRTP));
	knowledge[MFormatPane.AVI].addElement(new Integer(ALAWJD));
	knowledge[MFormatPane.AVI].addElement(new Integer(MSGSMJD));
	knowledge[MFormatPane.AVI].addElement(new Integer(MSGSMND));
	knowledge[MFormatPane.AVI].addElement(new Integer(MSGSMJE));
	knowledge[MFormatPane.AVI].addElement(new Integer(MSGSMNE));
	knowledge[MFormatPane.AVI].addElement(new Integer(MPAJD));
	knowledge[MFormatPane.AVI].addElement(new Integer(MPAND));
	knowledge[MFormatPane.AVI].addElement(new Integer(MPANE));
	knowledge[MFormatPane.AVI].addElement(new Integer(MPADRTP));
	knowledge[MFormatPane.AVI].addElement(new Integer(MPAPRTP));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSIMA4JD));
	knowledge[MFormatPane.WAV].addElement(new Integer(MSIMA4JE));
	knowledge[MFormatPane.AVI].addElement(new Integer(ACMND));
	knowledge[MFormatPane.AVI].addElement(new Integer(ACMNE));
	knowledge[MFormatPane.AVI].addElement(new Integer(CINEPAKJD));
	knowledge[MFormatPane.AVI].addElement(new Integer(CINEPAKND));
	knowledge[MFormatPane.AVI].addElement(new Integer(CINEPAKPRONE));
	knowledge[MFormatPane.AVI].addElement(new Integer(JPEGND));
	knowledge[MFormatPane.AVI].addElement(new Integer(JPEGNE));
	knowledge[MFormatPane.AVI].addElement(new Integer(JPEGDRTP));
	knowledge[MFormatPane.AVI].addElement(new Integer(JPEGPRTP));
	knowledge[MFormatPane.AVI].addElement(new Integer(VCMND));
	knowledge[MFormatPane.AVI].addElement(new Integer(VCMNE));
	knowledge[MFormatPane.AVI].addElement(new Integer(IV32ND));

	// mpeg ??
	knowledge[MFormatPane.MPEG] = new Vector();
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPAJD));
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPAND));
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPANE));
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPADRTP));
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPAPRTP));
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPEGND));
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPEGPLY));
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPEGDRTP));
	knowledge[MFormatPane.MPEG].addElement(new Integer(MPEGPRTP));

    }


}

