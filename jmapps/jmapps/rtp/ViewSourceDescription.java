/*
 * @(#)ViewSourceDescription.java	1.3 02/08/21
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

import jmapps.ui.*;


public class ViewSourceDescription extends JMPanel {

    private Vector  vectorData = null;

    private JMPanel     panelLabels = null;
    private JMPanel     panelData = null;
    private Label       fieldCname;
    private Label       fieldName;
    private Label       fieldEmail;
    private Label       fieldPhone;
    private Label       fieldLocation;
    private Label       fieldTool;
    private Label       fieldNote;
    private Label       fieldPrivate;

    private static final String LABEL_CNAME     = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.cname" );
    private static final String LABEL_NAME      = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.name" );
    private static final String LABEL_EMAIL     = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.email" );
    private static final String LABEL_PHONE     = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.phone" );
    private static final String LABEL_LOCATION  = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.location" );
    private static final String LABEL_TOOL      = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.tool" );
    private static final String LABEL_NOTE      = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.note" );
    private static final String LABEL_PRIVATE   = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.private" );
    private static final String LABEL_NONE      = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.srcdescr.none" );


    public ViewSourceDescription ( Vector vectorData ) {
	    super ();

        this.vectorData = vectorData;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init () throws Exception {
        Label       label;

        this.setLayout ( new BorderLayout(6,6) );

        panelLabels = new JMPanel ( new GridLayout(0,1,0,0) );
        this.add ( panelLabels, BorderLayout.WEST );
        panelData = new JMPanel ( new GridLayout(0,1,0,0) );
        this.add ( panelData, BorderLayout.CENTER );

        label = new Label ( LABEL_CNAME );
        panelLabels.add ( label );
        fieldCname = new Label ( LABEL_NONE );
        panelData.add ( fieldCname );

        label = new Label ( LABEL_NAME );
        panelLabels.add ( label );
        fieldName = new Label ( LABEL_NONE );
        panelData.add ( fieldName );

        label = new Label ( LABEL_EMAIL );
        panelLabels.add ( label );
        fieldEmail = new Label ( LABEL_NONE );
        panelData.add ( fieldEmail );

        label = new Label ( LABEL_PHONE );
        panelLabels.add ( label );
        fieldPhone = new Label ( LABEL_NONE );
        panelData.add ( fieldPhone );

        label = new Label ( LABEL_LOCATION );
        panelLabels.add ( label );
        fieldLocation = new Label ( LABEL_NONE );
        panelData.add ( fieldLocation );

        label = new Label ( LABEL_TOOL );
        panelLabels.add ( label );
        fieldTool = new Label ( LABEL_NONE );
        panelData.add ( fieldTool );

        label = new Label ( LABEL_NOTE );
        panelLabels.add ( label );
        fieldNote = new Label ( LABEL_NONE );
        panelData.add ( fieldNote );

        label = new Label ( LABEL_PRIVATE );
        panelLabels.add ( label );
        fieldPrivate = new Label ( LABEL_NONE );
        panelData.add ( fieldPrivate );

        updateFields ( vectorData );
    }

    public void updateFields ( Vector vectorData ) {
        int                 i;
        int                 nCount;
        SourceDescription   srcDescr;

        if ( vectorData == null )
            return;

        this.vectorData = vectorData;

        nCount = vectorData.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            srcDescr = (SourceDescription) vectorData.elementAt ( i );
            switch ( srcDescr.getType() ) {
                case SourceDescription.SOURCE_DESC_CNAME:
                    fieldCname.setText ( srcDescr.getDescription() );
                    break;
                case SourceDescription.SOURCE_DESC_NAME:
                    fieldName.setText ( srcDescr.getDescription() );
                    break;
                case SourceDescription.SOURCE_DESC_EMAIL:
                    fieldEmail.setText ( srcDescr.getDescription() );
                    break;
                case SourceDescription.SOURCE_DESC_PHONE:
                    fieldPhone.setText ( srcDescr.getDescription() );
                    break;
                case SourceDescription.SOURCE_DESC_LOC:
                    fieldLocation.setText ( srcDescr.getDescription() );
                    break;
                case SourceDescription.SOURCE_DESC_TOOL:
                    fieldTool.setText ( srcDescr.getDescription() );
                    break;
                case SourceDescription.SOURCE_DESC_NOTE:
                    fieldNote.setText ( srcDescr.getDescription() );
                    break;
                case SourceDescription.SOURCE_DESC_PRIV:
                    fieldPrivate.setText ( srcDescr.getDescription() );
                    break;
                default:
                    break;
            }
        }
    }

}


