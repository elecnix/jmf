/*
 * @(#)SessionControlDialog.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.rtp;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.rtp.*;
import javax.media.control.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.ui.TabControl;

import jmapps.ui.*;


public class SessionControlDialog extends JMDialog {

    private SessionManager      mngrSession;


    public SessionControlDialog ( Frame frame, SessionManager mngrSession ) {
	    super ( frame, JMFI18N.getResource("jmstudio.rtpsessionctrl.title"), false );

        this.mngrSession = mngrSession;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init () throws Exception {
        JMPanel     panel;
        JMPanel     panelContent;
        JMPanel     panelButtons;
        TabControl  tabControl;


        this.setLayout ( new BorderLayout(6,6) );
        panelContent = new JMPanel ( new BorderLayout(6,6) );
        panelContent.setEmptyBorder ( 6, 6, 6, 6 );
        this.add ( panelContent, BorderLayout.CENTER );

        tabControl = new TabControl ( TabControl.ALIGN_TOP );
        panelContent.add ( tabControl, BorderLayout.CENTER );

        panel = createOverallStatsPanel ();
        tabControl.addPage ( panel, JMFI18N.getResource("jmstudio.rtpsessionctrl.panel.overall") );
        panel = createParticipantsPanel ();
        tabControl.addPage ( panel, JMFI18N.getResource("jmstudio.rtpsessionctrl.panel.participants") );
        panel = createBufferControlPanel ();
        tabControl.addPage ( panel, JMFI18N.getResource("jmstudio.rtpsessionctrl.panel.bufferctrl") );

        panel = new JMPanel ( new BorderLayout(6,6) );
        panelContent.add ( panel, BorderLayout.SOUTH );
        panelButtons = createButtonPanel ( new String[] { ACTION_CLOSE } );
        panel.add ( panelButtons, BorderLayout.EAST );

        this.pack ();
        this.setResizable ( false );
    }

    private JMPanel createOverallStatsPanel () {
        JMPanel     panelStats;

        panelStats = new PanelOverallRtpStats ( mngrSession );
        return ( panelStats );
    }

    private JMPanel createParticipantsPanel () {
        JMPanel     panelParticipants;

        panelParticipants = new PanelParticipants ( mngrSession );
        return ( panelParticipants );
    }

    private JMPanel createBufferControlPanel () {
        JMPanel         panelBufferControl;
        BufferControl   bufferControl;
        Component       component = null;

        panelBufferControl = new JMPanel ( new FlowLayout() );

        bufferControl = (BufferControl) mngrSession.getControl ( "javax.media.control.BufferControl" );
        if ( bufferControl != null )
            component = bufferControl.getControlComponent ();

        if ( component != null )
            panelBufferControl.add ( component );

        return ( panelBufferControl );
    }

    public void actionPerformed ( ActionEvent event ) {
        String  strAction;

        strAction = event.getActionCommand ();
        if ( strAction.equals(ACTION_CLOSE) ) {
            setAction ( ACTION_CLOSE );
            this.setVisible ( false );
        }
    }

    public void windowClosing ( WindowEvent event ) {
        setAction ( ACTION_CLOSE );
        this.setVisible ( false );
    }

}


