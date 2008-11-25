/*
 * @(#)ViewReportBlock.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.rtp;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.rtp.util.Signed;

import jmapps.ui.*;


public class ViewReportBlock extends JMPanel {

    private Feedback    feedback;

    private Label   fieldDlsr;
    private Label   fieldFractionLost;
    private Label   fieldLostPackets;
    private Label   fieldJitter;
    private Label   fieldLsr;
    private Label   fieldSsrc;
    private Label   fieldXtndSeqNum;

    private static final String LABEL_DLSR          = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.feedback.dlsr" );
    private static final String LABEL_FRACTION_LOST = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.feedback.fractionlost" );
    private static final String LABEL_LOST_PACKETS  = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.feedback.lostpackets" );
    private static final String LABEL_JITTER        = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.feedback.jitter" );
    private static final String LABEL_LSR           = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.feedback.lsr" );
    private static final String LABEL_SSRC          = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.feedback.ssrc" );
    private static final String LABEL_XTNDSEQNUM    = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.feedback.xtndseqnum" );


    public ViewReportBlock ( Feedback feedback ) {
	    super ();

        this.feedback = feedback;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init () throws Exception {
        JMPanel     panelContent;
        JMPanel     panelLabels;
        JMPanel     panelData;
        Label       label;


        this.setLayout ( new BorderLayout(6,6) );

        panelContent = new JMPanel ( new BorderLayout(6,6) );
        this.add ( panelContent, BorderLayout.NORTH );
        panelLabels = new JMPanel ( new GridLayout(0,1,0,0) );
        panelContent.add ( panelLabels, BorderLayout.WEST );
        panelData = new JMPanel ( new GridLayout(0,1,0,0) );
        panelContent.add ( panelData, BorderLayout.CENTER );

        label = new Label ( LABEL_DLSR );
        panelLabels.add ( label );
        fieldDlsr = new Label ();
        panelData.add ( fieldDlsr );

        label = new Label ( LABEL_FRACTION_LOST );
        panelLabels.add ( label );
        fieldFractionLost = new Label ();
        panelData.add ( fieldFractionLost );

        label = new Label ( LABEL_LOST_PACKETS );
        panelLabels.add ( label );
        fieldLostPackets = new Label ();
        panelData.add ( fieldLostPackets );

        label = new Label ( LABEL_JITTER );
        panelLabels.add ( label );
        fieldJitter = new Label ();
        panelData.add ( fieldJitter );

        label = new Label ( LABEL_LSR );
        panelLabels.add ( label );
        fieldLsr = new Label ();
        panelData.add ( fieldLsr );

        label = new Label ( LABEL_SSRC );
        panelLabels.add ( label );
        fieldSsrc = new Label ();
        panelData.add ( fieldSsrc );

        label = new Label ( LABEL_XTNDSEQNUM );
        panelLabels.add ( label );
        fieldXtndSeqNum = new Label ();
        panelData.add ( fieldXtndSeqNum );

        updateFields ( feedback );
    }

    public void updateFields ( Feedback feedback ) {
        if ( feedback == null )
            return;
        this.feedback = feedback;

        fieldDlsr.setText ( "" + (feedback.getDLSR()/65536.0) );
        fieldFractionLost.setText ( "" + (feedback.getFractionLost()/256.0) );
        fieldLostPackets.setText ( "" + feedback.getNumLost() );
        fieldJitter.setText ( "" + feedback.getJitter() );
        fieldLsr.setText ( "" + feedback.getLSR() );
        fieldSsrc.setText ( "" + Signed.UnsignedInt((int)feedback.getSSRC()) );
        fieldXtndSeqNum.setText ( "" + feedback.getXtndSeqNum() );
    }

}


