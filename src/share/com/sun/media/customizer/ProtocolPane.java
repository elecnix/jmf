/*
 * @(#)ProtocolPane.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  This class defines the protocol (DataSource) page
 *
 *  @version 2.0
 */

public class ProtocolPane extends JPanel {
    public static final int FILE = 0;
    public static final int HTTP = 1;
    public static final int FTP = 2;
    public static final int RTP = 3;
    public static final int RTSP = 4;
    public static final int JAVASOUND = 5;
    public static final int VFW = 6;
    public static final int SVDO = 7;
    public static final int SVDOPLS = 8;
    public static final int HTTPS = 9;
    public static final int CLNMGDS = 10;
    public static final int DSOUND = 11;

    JCheckBox[] protocols = new JCheckBox[12];
    boolean[] resultProtocol = new boolean[12];

    public ProtocolPane() {
	super();

	protocols[FILE] = new JCheckBox(I18N.getResource("ProtocolPane.FILE"), false);
	protocols[HTTP] = new JCheckBox(I18N.getResource("ProtocolPane.HTTP"), false);
	protocols[FTP] = new JCheckBox(I18N.getResource("ProtocolPane.FTP"), false);
	protocols[RTP] = new JCheckBox(I18N.getResource("ProtocolPane.RTP"), false);
	protocols[RTSP] = new JCheckBox(I18N.getResource("ProtocolPane.RTSP"), false);
	protocols[JAVASOUND] = new JCheckBox(I18N.getResource("ProtocolPane.JAVASOUND"), false);

	protocols[VFW] = new JCheckBox(I18N.getResource("ProtocolPane.VFW"), false);
	protocols[SVDO] = new JCheckBox(I18N.getResource("ProtocolPane.SVDO"), false);
	protocols[SVDOPLS] = new JCheckBox(I18N.getResource("ProtocolPane.SVDOPLS"), false);
	protocols[HTTPS] = new JCheckBox(I18N.getResource("ProtocolPane.HTTPS"), false);
	protocols[CLNMGDS] = new JCheckBox(I18N.getResource("ProtocolPane.CLNMGDS"), false);
	protocols[DSOUND] = new JCheckBox(I18N.getResource("ProtocolPane.DSOUND"), false);

        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),I18N.getResource("ProtocolPane.PROTOCOL") ));
	this.setLayout(new BorderLayout());
	JPanel pp1 = new JPanel();
	pp1.setLayout(new GridLayout(6,2));
	this.add("Center", pp1);
	for (int i = 0; i < 12; i++) {
	    protocols[i].setEnabled(false);
	    pp1.add(protocols[i]);
	}
	
	protocols[CLNMGDS].setEnabled(true);

	//this.add("South", protocols[10]);
    }

    public boolean[] getState() {
	for (int i = 0; i < 12; i++) {
	    if (protocols[i].isEnabled() && protocols[i].isSelected())
		resultProtocol[i] = true;
	    else 
		resultProtocol[i] = false;
	} 
	return (resultProtocol);
    }
	
    // media file -- file/http/ftp
    // RTP recv --- rtp/rtsp
    // capture --- javasound in jdk1.3
    // RTP transmit -- rtp/rtsp
    // transcode --- dst is a file, src could be one of file/http/fpt/rtp/capture
    public void setHighlight(boolean[] funcs, int release) {
	boolean value;

	value = funcs[GeneralPane.MFILE];
	protocols[HTTP].setEnabled(value);
	protocols[HTTPS].setEnabled(value);
	protocols[FTP].setEnabled(value);
	value = value || funcs[GeneralPane.TRANSCODE];
	protocols[FILE].setEnabled(value);

	value = funcs[GeneralPane.RTPREC] || funcs[GeneralPane.RTPTRANS];
	protocols[RTSP].setEnabled(value);
	protocols[RTP].setEnabled(value);

	value = funcs[GeneralPane.CAPTURE];
	protocols[JAVASOUND].setEnabled(value);

	value = funcs[GeneralPane.CAPTURE] && ( release == 2 ); // SPP
	protocols[SVDO].setEnabled(value);
	protocols[SVDOPLS].setEnabled(value);
	
	value = funcs[GeneralPane.CAPTURE] && ( release == 3 ); // WPP
	protocols[VFW].setEnabled(value);
	protocols[DSOUND].setEnabled(value);
    }
}

