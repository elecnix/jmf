/*
 * @(#)PanelOverallRtpStats.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.rtp;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.rtp.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.ui.TabControl;

import jmapps.ui.*;


public class PanelOverallRtpStats extends JMPanel {

    private SessionManager  mngrSession;
    private UpdateThread    threadUpdate = null;

    private Label   fieldTotalRtpPackets;
    private Label   fieldTotalBytes;
    private Label   fieldBadRtpPackets;
    private Label   fieldLocalCollisions;
    private Label   fieldRemoteCollisions;
    private Label   fieldPacketsLooped;
    private Label   fieldFailedTransmissions;
    private Label   fieldRtcpPackets;
    private Label   fieldSrPackets;
    private Label   fieldBadRtcpPackets;
    private Label   fieldUnknownRtcpTypes;
    private Label   fieldMalformedRr;
    private Label   fieldMalformedSdes;
    private Label   fieldMalformedBye;
    private Label   fieldMalformedSr;

    private static final String     LABEL_TOTAL_PACKETS         = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.totalpackets" );
    private static final String     LABEL_TOTAL_BYTES           = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.totalbytes" );
    private static final String     LABEL_BAD_RTP_PACKETS       = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.badrtppackets" );
    private static final String     LABEL_LOCAL_COLLISIONS      = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.localcollisions" );
    private static final String     LABEL_REMOTE_COLLISIONS     = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.remotecollisions" );
    private static final String     LABEL_PACKETS_LOOPED        = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.packetslooped" );
    private static final String     LABEL_FAILED_TRANSMISSIONS  = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.failedtransmissions" );
    private static final String     LABEL_RTCP_PACKETS          = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.rtcppackets" );
    private static final String     LABEL_SR_PACKETS            = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.srpackets" );
    private static final String     LABEL_BAD_RTCP_PACKETS      = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.badrtcppackets" );
    private static final String     LABEL_UNKNOWN_RTCP_TYPES    = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.unknownrtcptypes" );
    private static final String     LABEL_MALFORMED_RR          = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.malformedrr" );
    private static final String     LABEL_MALFORMED_SDES        = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.malformedsdes" );
    private static final String     LABEL_MALFORMED_BYE         = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.malformedbye" );
    private static final String     LABEL_MALFORMED_SR          = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.overall.malformedsr" );


    public PanelOverallRtpStats ( SessionManager mngrSession ) {
	    super ();

        this.mngrSession = mngrSession;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addNotify () {
        super.addNotify ();

        if ( threadUpdate == null )
            threadUpdate = new UpdateThread ();
        threadUpdate.start ();
    }

    public void removeNotify () {
        if ( threadUpdate != null ) {
            threadUpdate.terminate ();
            threadUpdate = null;
        }

        super.removeNotify ();
    }

    private void init () throws Exception {
        JMPanel     panelColumn1;
        JMPanel     panelColumn2;
        JMPanel     panelLabels;
        JMPanel     panelData;
        JMPanel     panel;
        Label       label;


        this.setLayout ( new GridLayout(1,0,24,6) );

        panelColumn1 = new JMPanel ( new BorderLayout(6,6) );
        this.add ( panelColumn1 );
        panelColumn2 = new JMPanel ( new BorderLayout(6,6) );
        this.add ( panelColumn2 );

        panel = new JMPanel ( new BorderLayout(6,6) );
        panelColumn1.add ( panel, BorderLayout.NORTH );
        panelLabels = new JMPanel ( new GridLayout(0,1,0,0) );
        panel.add ( panelLabels, BorderLayout.WEST );
        panelData = new JMPanel ( new GridLayout(0,1,0,0) );
        panel.add ( panelData, BorderLayout.CENTER );

        label = new Label ( LABEL_TOTAL_PACKETS );
        panelLabels.add ( label );
        fieldTotalRtpPackets = new Label ( "000000000000" );
        panelData.add ( fieldTotalRtpPackets );

        label = new Label ( LABEL_TOTAL_BYTES );
        panelLabels.add ( label );
        fieldTotalBytes = new Label ();
        panelData.add ( fieldTotalBytes );

        label = new Label ( LABEL_BAD_RTP_PACKETS );
        panelLabels.add ( label );
        fieldBadRtpPackets = new Label ();
        panelData.add ( fieldBadRtpPackets );

        label = new Label ( LABEL_LOCAL_COLLISIONS );
        panelLabels.add ( label );
        fieldLocalCollisions = new Label ();
        panelData.add ( fieldLocalCollisions );

        label = new Label ( LABEL_REMOTE_COLLISIONS );
        panelLabels.add ( label );
        fieldRemoteCollisions = new Label ();
        panelData.add ( fieldRemoteCollisions );

        label = new Label ( LABEL_PACKETS_LOOPED );
        panelLabels.add ( label );
        fieldPacketsLooped = new Label ();
        panelData.add ( fieldPacketsLooped );

        label = new Label ( LABEL_FAILED_TRANSMISSIONS );
        panelLabels.add ( label );
        fieldFailedTransmissions = new Label ();
        panelData.add ( fieldFailedTransmissions );

        panel = new JMPanel ( new BorderLayout(6,6) );
        panelColumn2.add ( panel, BorderLayout.NORTH );
        panelLabels = new JMPanel ( new GridLayout(0,1,0,0) );
        panel.add ( panelLabels, BorderLayout.WEST );
        panelData = new JMPanel ( new GridLayout(0,1,0,0) );
        panel.add ( panelData, BorderLayout.CENTER );

        label = new Label ( LABEL_RTCP_PACKETS );
        panelLabels.add ( label );
        fieldRtcpPackets = new Label ();
        panelData.add ( fieldRtcpPackets );

        label = new Label ( LABEL_SR_PACKETS );
        panelLabels.add ( label );
        fieldSrPackets = new Label ();
        panelData.add ( fieldSrPackets );

        label = new Label ( LABEL_BAD_RTCP_PACKETS );
        panelLabels.add ( label );
        fieldBadRtcpPackets = new Label ();
        panelData.add ( fieldBadRtcpPackets );

        label = new Label ( LABEL_UNKNOWN_RTCP_TYPES );
        panelLabels.add ( label );
        fieldUnknownRtcpTypes = new Label ();
        panelData.add ( fieldUnknownRtcpTypes );

        label = new Label ( LABEL_MALFORMED_RR );
        panelLabels.add ( label );
        fieldMalformedRr = new Label ();
        panelData.add ( fieldMalformedRr );

        label = new Label ( LABEL_MALFORMED_SDES );
        panelLabels.add ( label );
        fieldMalformedSdes = new Label ();
        panelData.add ( fieldMalformedSdes );

        label = new Label ( LABEL_MALFORMED_BYE );
        panelLabels.add ( label );
        fieldMalformedBye = new Label ();
        panelData.add ( fieldMalformedBye );

        label = new Label ( LABEL_MALFORMED_SR );
        panelLabels.add ( label );
        fieldMalformedSr = new Label ();
        panelData.add ( fieldMalformedSr );

        updateFields ();
    }

    private void updateFields () {
        GlobalReceptionStats    stats;

        stats = mngrSession.getGlobalReceptionStats ();

        fieldTotalRtpPackets.setText ( "" + stats.getPacketsRecd() );
        fieldTotalBytes.setText ( "" + stats.getBytesRecd() );
        fieldBadRtpPackets.setText ( "" + stats.getBadRTPkts() );
        fieldLocalCollisions.setText ( "" + stats.getLocalColls() );
        fieldRemoteCollisions.setText ( "" + stats.getRemoteColls() );
        fieldPacketsLooped.setText ( "" + stats.getPacketsLooped() );
        fieldFailedTransmissions.setText ( "" + stats.getTransmitFailed() );
        fieldRtcpPackets.setText ( "" + stats.getRTCPRecd() );
        fieldSrPackets.setText ( "" + stats.getSRRecd() );
        fieldBadRtcpPackets.setText ( "" + stats.getBadRTCPPkts() );
        fieldUnknownRtcpTypes.setText ( "" + stats.getUnknownTypes() );
        fieldMalformedRr.setText ( "" + stats.getMalformedRR() );
        fieldMalformedSdes.setText ( "" + stats.getMalformedSDES() );
        fieldMalformedBye.setText ( "" + stats.getMalformedBye() );
        fieldMalformedSr.setText ( "" + stats.getMalformedSR() );
    }


    private class UpdateThread extends Thread {

        private boolean     boolTerminate = false;

        public UpdateThread () {
        }

        public void terminate () {
            boolTerminate = true;
        }


        public void run () {
            while ( boolTerminate == false ) {
                try {
                    sleep ( 1000 );
                    updateFields ();
                }
                catch ( Exception exception ) {
                }
            }
        }
    }


}


