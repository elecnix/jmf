/*
 * @(#)ViewSenderReport.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.rtp;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;
import javax.media.rtp.event.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.rtp.util.Signed;

import jmapps.ui.*;


public class ViewSenderReport extends JMPanel {

    private SessionManager          mngrSession;
    private SenderReport            reportSender;

    private ViewSourceDescription   panelSrcDescr = null;
    private JMPanel                 panelLabels = null;
    private JMPanel                 panelData = null;
    private Label                   fieldSSRC;
    private Label                   fieldNtpTimestampLsw;
    private Label                   fieldNtpTimestampMsw;
    private Label                   fieldRtpTimestamp;
    private Label                   fieldBytes;
    private Label                   fieldPackets;

    private static final String LABEL_SSRC              = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.report.ssrc" );
    private static final String LABEL_NTP_TIMESTAMP_LSW = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.report.ntptimestamplsw" );
    private static final String LABEL_NTP_TIMESTAMP_MSW = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.report.ntptimestampmsw" );
    private static final String LABEL_RTP_TIMESTAMP     = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.report.rtptimestamp" );
    private static final String LABEL_BYTES             = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.report.bytecount" );
    private static final String LABEL_PACKETS           = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.report.packetcount" );


    public ViewSenderReport ( SessionManager mngrSession, SenderReport reportSender ) {
	    super ();

        this.mngrSession = mngrSession;
        this.reportSender = reportSender;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init () throws Exception {
        JMPanel     panel;
        Label       label;

        this.setLayout ( new BorderLayout(6,6) );
        panel = new JMPanel ( new BorderLayout(6,6) );
        this.add ( panel, BorderLayout.NORTH );

        panelLabels = new JMPanel ( new GridLayout(0,1,0,0) );
        panel.add ( panelLabels, BorderLayout.WEST );
        panelData = new JMPanel ( new GridLayout(0,1,0,0) );
        panel.add ( panelData, BorderLayout.CENTER );

        label = new Label ( LABEL_SSRC );
        panelLabels.add ( label );
        fieldSSRC = new Label ();
        panelData.add ( fieldSSRC );

        label = new Label ( LABEL_NTP_TIMESTAMP_LSW );
        panelLabels.add ( label );
        fieldNtpTimestampLsw = new Label ();
        panelData.add ( fieldNtpTimestampLsw );

        label = new Label ( LABEL_NTP_TIMESTAMP_MSW );
        panelLabels.add ( label );
        fieldNtpTimestampMsw = new Label ();
        panelData.add ( fieldNtpTimestampMsw );

        label = new Label ( LABEL_RTP_TIMESTAMP );
        panelLabels.add ( label );
        fieldRtpTimestamp = new Label ();
        panelData.add ( fieldRtpTimestamp );

        label = new Label ( LABEL_BYTES );
        panelLabels.add ( label );
        fieldBytes = new Label ();
        panelData.add ( fieldBytes );

        label = new Label ( LABEL_PACKETS );
        panelLabels.add ( label );
        fieldPackets = new Label ();
        panelData.add ( fieldPackets );

        panel = new JMPanel ( new BorderLayout(6,6) );
        this.add ( panel, BorderLayout.CENTER );
        panelSrcDescr = new ViewSourceDescription ( reportSender.getSourceDescription() );
        panel.add ( panelSrcDescr, BorderLayout.NORTH );

        updateFields ( reportSender );
    }

    public void updateFields ( SenderReport reportSender ) {
        long    lValue;

        if ( reportSender == null )
            return;

        this.reportSender = reportSender;

        lValue = reportSender.getSSRC ();
        fieldSSRC.setText ( longToString(lValue) );

        lValue = reportSender.getNTPTimeStampLSW ();
        fieldNtpTimestampLsw.setText ( longToString(lValue) );

        lValue = reportSender.getNTPTimeStampMSW ();
        fieldNtpTimestampMsw.setText ( longToString(lValue) );

        lValue = reportSender.getRTPTimeStamp ();
        fieldRtpTimestamp.setText ( longToString(lValue) );

        lValue = reportSender.getSenderByteCount ();
        fieldBytes.setText ( longToString(lValue) );

        lValue = reportSender.getSenderPacketCount ();
        fieldPackets.setText ( longToString(lValue) );
    }

    private String longToString ( long lValue ) {
        if ( lValue < 0 )
            lValue = Signed.UnsignedInt((int)lValue);
        return ( "" + lValue );
    }

}


