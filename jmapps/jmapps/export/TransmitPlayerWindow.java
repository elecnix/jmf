/*
 * @(#)TransmitPlayerWindow.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.export;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.media.*;
//import javax.media.format.*;
//import javax.media.control.*;
//import javax.media.protocol.*;
//import javax.media.datasink.*;
import javax.media.rtp.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.ui.PlayerWindow;

//import jmapps.ui.*;
//import jmapps.util.*;
//import jmapps.jmstudio.*;
import jmapps.rtp.*;


class TransmitPlayerWindow extends PlayerWindow implements ActionListener, WindowListener {

    private Vector      vectorMngrSessions = new Vector ();
    private Vector      vectorStreams = new Vector ();
    private Vector      vectorStreamLabels = new Vector ();

    private MenuBar     menuBar = null;
    private Menu        menuStats = null;
    private MenuItem    menuItemStats = null;

    private TransmissionStatsDialog dlgTransmissionStats = null;

    private static final String MENU_STATS              = JMFI18N.getResource ( "jmstudio.menu.stats" );
    private static final String MENU_STATS_TRANSMISSION = JMFI18N.getResource ( "jmstudio.menu.player.transmission" );


    public TransmitPlayerWindow ( Processor processor ) {
        super ( processor, JMFI18N.getResource("jmstudio.playerwindow.transcoding"), true );

        this.addWindowListener ( this );
    }

    public void addNotify () {
        createMenuStats ();
        super.addNotify ();
    }

    public void addSessionManager ( SessionManager mngrSession,
                        SendStream streamSend, String strStreamLabel ) {

        vectorMngrSessions.addElement ( mngrSession );
        vectorStreams.addElement ( streamSend );
        vectorStreamLabels.addElement ( strStreamLabel );
        createMenuStats ();
    }


    public void actionPerformed ( ActionEvent event ) {
        String                  strCmd;
        String                  strAction;

        strCmd = event.getActionCommand ();
        if ( strCmd.equals(MENU_STATS_TRANSMISSION) ) {
            if ( dlgTransmissionStats == null )
                dlgTransmissionStats = new TransmissionStatsDialog ( this,
                                    vectorMngrSessions, vectorStreamLabels );
            dlgTransmissionStats.setVisible ( true );
            dlgTransmissionStats.toFront ();
        }
    }

    public void windowOpened ( WindowEvent event ) {
    }

    public void windowClosing ( WindowEvent event ) {
    }

    public void windowClosed ( WindowEvent event ) {
        int                     i, j;
        int                     nCount;
        SendStream              streamSend;
        SessionManager          mngrSession;

        if ( dlgTransmissionStats != null ) {
            dlgTransmissionStats.dispose ();
            dlgTransmissionStats = null;
        }

        nCount = vectorStreams.size();
        for ( i = 0;  i < nCount;  i++ ) {
            streamSend = (SendStream) vectorStreams.elementAt ( i );
            streamSend.close ();
        }
        nCount = vectorMngrSessions.size();
        for ( i = 0;  i < nCount;  i++ ) {
            mngrSession = (SessionManager) vectorMngrSessions.elementAt ( i );
            mngrSession.closeSession ( "Transmission terminated" );
        }
        vectorMngrSessions.removeAllElements ();
        vectorStreams.removeAllElements ();
        vectorStreamLabels.removeAllElements ();
    }

    public void windowIconified ( WindowEvent event ) {
    }

    public void windowDeiconified ( WindowEvent event ) {
    }

    public void windowActivated ( WindowEvent event ) {
    }

    public void windowDeactivated ( WindowEvent event ) {
    }

    private synchronized void createMenuStats () {
        if ( menuItemStats != null )
            return;

        createMenuBar ();
        if ( menuStats == null ) {
            menuStats = new Menu ( MENU_STATS );
            menuBar.add ( this.menuStats );
        }

        menuItemStats = new MenuItem ( MENU_STATS_TRANSMISSION );
        menuStats.add ( menuItemStats );
        menuItemStats.setActionCommand ( MENU_STATS_TRANSMISSION );
        menuItemStats.addActionListener ( this );
    }

    private synchronized void createMenuBar () {
        menuBar = this.getMenuBar ();
        if ( menuBar == null ) {
            menuBar = new MenuBar ();
            setMenuBar ( menuBar );
        }
    }

}


