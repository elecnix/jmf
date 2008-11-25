/*
 * @(#)ViewRtpStreamInfo.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.rtp;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.rtp.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.rtp.util.Signed;

import jmapps.ui.*;


public class ViewRtpStreamInfo extends JMPanel {

    private SessionManager          mngrSession;
    private RTPStream               stream;
    private UpdateThread            threadUpdate = null;

    private JMPanel     panelContent = null;
    private JMPanel     panelLabels = null;
    private JMPanel     panelData = null;
    private Label       fieldOwner;
    private Label       fieldSsrc;
    private Label       fieldLostPdu;
    private Label       fieldProcessedPdu;
    private Label       fieldMisorderedPdu;
    private Label       fieldInvalidPdu;
    private Label       fieldDuplicatePdu;

    private static final String LABEL_NAME              = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.stream.owner" );
    private static final String LABEL_SSRC              = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.stream.ssrc" );
    private static final String LABEL_LOST_PDU          = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.stream.lostpdu" );
    private static final String LABEL_PROCESSED_PDU     = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.stream.processedpdu" );
    private static final String LABEL_MISORDERED_PDU    = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.stream.misorderedpdu" );
    private static final String LABEL_INVALID_PDU       = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.stream.invalidpdu" );
    private static final String LABEL_DUPLICATE_PDU     = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.stream.duplicatepdu" );


    public ViewRtpStreamInfo ( SessionManager mgrSession, RTPStream stream ) {
	    super ();

        this.mngrSession = mngrSession;
        this.stream = stream;
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
        Label       label;

        this.setLayout ( new BorderLayout(6,6) );

        panelContent = new JMPanel ( new BorderLayout(6,6) );
        this.add ( panelContent, BorderLayout.NORTH );

        panelLabels = new JMPanel ( new GridLayout(0,1,0,0) );
        panelContent.add ( panelLabels, BorderLayout.WEST );
        panelData = new JMPanel ( new GridLayout(0,1,0,0) );
        panelContent.add ( panelData, BorderLayout.CENTER );

        label = new Label ( LABEL_NAME );
        panelLabels.add ( label );
        fieldOwner = new Label ();
        panelData.add ( fieldOwner );

        label = new Label ( LABEL_SSRC );
        panelLabels.add ( label );
        fieldSsrc = new Label ();
        panelData.add ( fieldSsrc );

        if ( stream instanceof ReceiveStream ) {
            label = new Label ( LABEL_LOST_PDU );
            panelLabels.add ( label );
            fieldLostPdu = new Label ();
            panelData.add ( fieldLostPdu );

            label = new Label ( LABEL_PROCESSED_PDU );
            panelLabels.add ( label );
            fieldProcessedPdu = new Label ();
            panelData.add ( fieldProcessedPdu );

            label = new Label ( LABEL_MISORDERED_PDU );
            panelLabels.add ( label );
            fieldMisorderedPdu = new Label ();
            panelData.add ( fieldMisorderedPdu );

            label = new Label ( LABEL_INVALID_PDU );
            panelLabels.add ( label );
            fieldInvalidPdu = new Label ();
            panelData.add ( fieldInvalidPdu );

            label = new Label ( LABEL_DUPLICATE_PDU );
            panelLabels.add ( label );
            fieldDuplicatePdu = new Label ();
            panelData.add ( fieldDuplicatePdu );
        }

        updateFields ();
    }

    private void updateFields () {
        ReceptionStats  stats;

        fieldOwner.setText ( stream.getParticipant().getCNAME() );
        fieldSsrc.setText ( "" + Signed.UnsignedInt((int)stream.getSSRC()) );

        if ( stream instanceof ReceiveStream ) {
            stats = ((ReceiveStream)stream).getSourceReceptionStats ();

            fieldLostPdu.setText ( "" + stats.getPDUlost() );
            fieldProcessedPdu.setText ( "" + stats.getPDUProcessed() );
            fieldMisorderedPdu.setText ( "" + stats.getPDUMisOrd() );
            fieldInvalidPdu.setText ( "" + stats.getPDUInvalid() );
            fieldDuplicatePdu.setText ( "" + stats.getPDUDuplicate() );
        }
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


