/*
 * @(#)ViewParticipantInfo.java	1.3 02/08/21
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

import jmapps.ui.*;


public class ViewParticipantInfo extends JMPanel {

    private SessionManager          mngrSession;
    private Participant             participant;
    private ViewSourceDescription   panelSrcDescr = null;

    public ViewParticipantInfo ( SessionManager mngrSession, Participant participant ) {
	    super ();

        this.mngrSession = mngrSession;
        this.participant = participant;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init () throws Exception {
        this.setLayout ( new BorderLayout() );

        panelSrcDescr = new ViewSourceDescription ( participant.getSourceDescription() );
        this.add ( panelSrcDescr, BorderLayout.NORTH );
    }

    public void updateFields ( Report report ) {
        if ( report.getParticipant() != this.participant )
            return;

        panelSrcDescr.updateFields ( report.getSourceDescription() );
    }

}


