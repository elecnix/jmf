/*
 * @(#)ViewReceiverReport.java	1.3 02/08/21
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


public class ViewReceiverReport extends JMPanel {

    private ReceiverReport  reportReceiver;

    private ViewSourceDescription   panelSrcDescr = null;
    private JMPanel                 panelLabels = null;
    private JMPanel                 panelData = null;
    private Label                   fieldSSRC;

    private static final String LABEL_SSRC  = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.report.ssrc" );


    public ViewReceiverReport ( ReceiverReport reportReceiver ) {
	    super ();

        this.reportReceiver = reportReceiver;
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

        panel = new JMPanel ( new BorderLayout(6,6) );
        this.add ( panel, BorderLayout.CENTER );
        panelSrcDescr = new ViewSourceDescription ( null );
        panel.add ( panelSrcDescr, BorderLayout.NORTH );

        updateFields ( reportReceiver );
    }

    public void updateFields ( ReceiverReport reportReceiver ) {
        if ( reportReceiver == null )
            return;

        this.reportReceiver = reportReceiver;
        fieldSSRC.setText ( "" + Signed.UnsignedInt((int)reportReceiver.getSSRC()) );
        panelSrcDescr.updateFields ( reportReceiver.getSourceDescription() );
    }

}


