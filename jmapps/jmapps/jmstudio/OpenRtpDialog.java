/*
 * @(#)OpenRtpDialog.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.jmstudio;

import java.awt.*;
import java.awt.event.*;
import javax.media.rtp.*;
import javax.media.control.BufferControl;

import com.sun.media.util.JMFI18N;

import jmapps.util.*;
import jmapps.ui.*;


public class OpenRtpDialog extends JMDialog
implements TextListener, FocusListener {

    private TextField           fieldAddress0;
    private TextField           fieldAddress1;
    private TextField           fieldAddress2;
    private TextField           fieldAddress3;
    private TextField           fieldPort;
    private Choice              choiceTtl;

    private JMAppsCfg           cfgJMApps = null;

    private String              strAddress = null;
    private String              strPort = null;
    private String              strTtl = null;


    public OpenRtpDialog ( Frame frame, JMAppsCfg cfgJMApps ) {
        super ( frame, JMFI18N.getResource("jmstudio.openrtp.title"), true );

        this.cfgJMApps = cfgJMApps;
        init ();
    }

    public String getAddress () {
        return ( strAddress );
    }

    public String getPort () {
        return ( strPort );
    }

    public String getTtl () {
        return ( strTtl );
    }

    private void init () {
        JMPanel     panel;
        JMPanel     panelButtons;
        JMPanel     panelContent;

        this.setLayout ( new BorderLayout() );
        panelContent = new JMPanel ( new BorderLayout(6,6) );
        panelContent.setEmptyBorder ( 6, 6, 6, 6 );
        this.add ( panelContent, BorderLayout.CENTER );
        panelContent.setBackground ( Color.lightGray );

        panel = createPanelSource ();
        panelContent.add ( panel, BorderLayout.CENTER );

        panel = new JMPanel ( new FlowLayout(FlowLayout.CENTER) );
        this.add ( panel, BorderLayout.SOUTH );
        panelButtons = createButtonPanel ( new String[] { ACTION_OPEN, ACTION_CANCEL } );
        panel.add ( panelButtons );

        pack ();
        setResizable (false);
    }

    private JMPanel createPanelSource () {
    	JMPanel                 panelSource;
    	JMPanel	                panelLabels;
    	JMPanel	                panelParams;
    	JMPanel	                panelAddress;
        Label                   label;
        JMAppsCfg.RtpData     dataRtp;


        dataRtp = cfgJMApps.getLastOpenRtpData();

    	panelSource = new JMPanel ( new BorderLayout(6,6) );

    	panelLabels = new JMPanel ( new GridLayout(0,1,6,6) );
    	panelSource.add ( panelLabels, BorderLayout.WEST );
    	panelParams = new JMPanel ( new GridLayout(0,1,6,6) );
    	panelSource.add ( panelParams, BorderLayout.CENTER );

        label = new Label ( JMFI18N.getResource("jmstudio.openrtp.label.address") );
    	panelLabels.add ( label );
    	panelAddress = new JMPanel ( new GridLayout(1,0,2,2) );
    	panelParams.add ( panelAddress );

    	fieldAddress0 = new TextField ( 3 );
        if ( dataRtp != null )
            fieldAddress0.setText ( dataRtp.strAddress0 );
        fieldAddress0.addTextListener ( this );
	fieldAddress0.addFocusListener(this);
    	panelAddress.add ( fieldAddress0 );

    	fieldAddress1 = new TextField ( 3 );
        if ( dataRtp != null )
            fieldAddress1.setText ( dataRtp.strAddress1 );
        fieldAddress1.addTextListener ( this );
	fieldAddress1.addFocusListener(this);
    	panelAddress.add ( fieldAddress1 );

    	fieldAddress2 = new TextField ( 3 );
        if ( dataRtp != null )
            fieldAddress2.setText ( dataRtp.strAddress2 );
        fieldAddress2.addTextListener ( this );
	fieldAddress2.addFocusListener(this);
    	panelAddress.add ( fieldAddress2 );

    	fieldAddress3 = new TextField ( 3 );
        if ( dataRtp != null )
            fieldAddress3.setText ( dataRtp.strAddress3 );
        fieldAddress3.addTextListener ( this );
	fieldAddress3.addFocusListener(this);
    	panelAddress.add ( fieldAddress3 );

        label = new Label ( JMFI18N.getResource("jmstudio.openrtp.label.port") );
    	panelLabels.add ( label );
    	fieldPort = new TextField ( 5 );
        if ( dataRtp != null )
            fieldPort.setText ( dataRtp.strPort );
	fieldPort.addFocusListener(this);
    	panelParams.add ( fieldPort );

        label = new Label ( JMFI18N.getResource("jmstudio.openrtp.label.ttl") );
    	panelLabels.add ( label );
    	choiceTtl = new Choice ();
    	panelParams.add ( choiceTtl );

    	choiceTtl.addItem ( "1" );
    	choiceTtl.addItem ( "2" );
    	choiceTtl.addItem ( "3" );
    	choiceTtl.addItem ( "4" );
    	choiceTtl.addItem ( "8" );
    	choiceTtl.addItem ( "16" );
    	choiceTtl.addItem ( "32" );
    	choiceTtl.addItem ( "64" );
    	choiceTtl.addItem ( "128" );
    	choiceTtl.addItem ( "255" );
        if ( dataRtp != null )
            choiceTtl.select ( dataRtp.strTtl );

    	return ( panelSource );
    }

    public void setVisible ( boolean show ) {
        super.setVisible ( show );
        fieldAddress0.requestFocus ();
    }

    public void actionPerformed ( ActionEvent event ) {
    	String		strAction;

    	strAction = event.getActionCommand ();
        if ( strAction.equals(ACTION_OPEN) ) {
            if ( validateData() == true ) {
                this.setAction ( strAction );
                this.dispose ();
            }
        }
        else if ( strAction.equals(ACTION_CANCEL) ) {
            this.setAction ( strAction );
            this.dispose ();
        }
    }

    public void textValueChanged ( TextEvent event ) {
    	Object		objectSource;
    	TextField	textField;
    	String		strValue;

    	objectSource = event.getSource ();
    	if ( objectSource instanceof TextField ) {
    	    textField = (TextField) objectSource;
    	    strValue = textField.getText ();
    	    if ( strValue.length() > 2 )
    	    	textField.transferFocus ();
    	}
    }

    public boolean validateData () {
        String                  strValue;
        int                     nValue;
        JMAppsCfg.RtpData     dataRtp;


        dataRtp = cfgJMApps.createRtpDataObject ();

        strValue = fieldAddress0.getText ();
        dataRtp.strAddress0 = strValue;
        try {
            nValue = Integer.valueOf(strValue).intValue();
        }
        catch ( Exception exception ) {
            nValue = -1;
        }
        if ( nValue < 0  ||  nValue > 255 ) {
            MessageDialog.createErrorDialog ( this.frameOwner, JMFI18N.getResource("jmstudio.error.sessionaddr") );
            return ( false );
        }
        strAddress = new String ( strValue );

        strValue = fieldAddress1.getText ();
        dataRtp.strAddress1 = strValue;
        try {
            nValue = Integer.valueOf(strValue).intValue();
        }
        catch ( Exception exception ) {
            nValue = -1;
        }
        if ( nValue < 0  ||  nValue > 255 ) {
            MessageDialog.createErrorDialog ( this.frameOwner, JMFI18N.getResource("jmstudio.error.sessionaddr") );
            return ( false );
        }
        strAddress = strAddress + "." + strValue;

        strValue = fieldAddress2.getText ();
        dataRtp.strAddress2 = strValue;
        try {
            nValue = Integer.valueOf(strValue).intValue();
        }
        catch ( Exception exception ) {
            nValue = -1;
        }
        if ( nValue < 0  ||  nValue > 255 ) {
            MessageDialog.createErrorDialog ( this.frameOwner, JMFI18N.getResource("jmstudio.error.sessionaddr") );
            return ( false );
        }
        strAddress = strAddress + "." + strValue;

        strValue = fieldAddress3.getText ();
        dataRtp.strAddress3 = strValue;
        try {
            nValue = Integer.valueOf(strValue).intValue();
        }
        catch ( Exception exception ) {
            nValue = -1;
        }
        if ( nValue < 0  ||  nValue > 255 ) {
            MessageDialog.createErrorDialog ( this.frameOwner, JMFI18N.getResource("jmstudio.error.sessionaddr") );
            return ( false );
        }
        strAddress = strAddress + "." + strValue;

        strPort = fieldPort.getText ();
        dataRtp.strPort = strPort;
        try {
            nValue = Integer.valueOf(strPort).intValue();
        }
        catch ( Exception exception ) {
            nValue = -1;
        }
        if ( nValue < 0  ||  (nValue & 0x0001) == 0x0001 ) {
            MessageDialog.createErrorDialog ( this.frameOwner, JMFI18N.getResource("jmstudio.error.port") );
            return ( false );
        }

        strTtl = choiceTtl.getSelectedItem ();
        dataRtp.strTtl = strTtl;

        if ( cfgJMApps != null )
            cfgJMApps.setLastOpenRtpData ( dataRtp );
        return ( true );
    }

    public void focusGained(FocusEvent fe) {
	Object source = fe.getSource();
	// Select the text in the field
	if (source instanceof TextField)
	    ((TextField)source).selectAll();
    }

    public void focusLost(FocusEvent fe) {
	Object source = fe.getSource();
	// Deselect the text in the field
	if (source instanceof TextField)
	    ((TextField)source).select(1, 0);
    }

}


