/*
 * @(#)PanelMediaTargetType.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.export;

import java.awt.*;

import com.sun.media.util.JMFI18N;

import jmapps.ui.*;


public class PanelMediaTargetType extends JMPanel {

    public static final String		TYPE_FILE = JMFI18N.getResource("jmstudio.export.type.file");
    public static final String		TYPE_NETWORK = JMFI18N.getResource("jmstudio.export.type.network");
    public static final String		TYPE_SCREEN = JMFI18N.getResource("jmstudio.export.type.screen");
    public static final String		TYPE_OTHER = JMFI18N.getResource("jmstudio.export.type.other");

    private CheckboxGroup	groupTargetType;
    private Checkbox		radioFile;
    private Checkbox		radioNetwork;
    private Checkbox		radioScreen;


    public PanelMediaTargetType () {
    	super ();

    	try {
    	    init ();
    	}
    	catch ( Exception exception ) {
    	    exception.printStackTrace ();
    	}
    }

    public String getType () {
    	String		strType;

    	if ( radioFile.getState() )
    	    strType = TYPE_FILE;
    	else if ( radioNetwork.getState() )
    	    strType = TYPE_NETWORK;
    	else if ( radioScreen.getState() )
    	    strType = TYPE_SCREEN;
    	else
    	    strType = TYPE_OTHER;

    	return ( strType );
    }

    private void init () throws Exception {
    	Panel	panel;
    	Panel	panelDescription;
    	Panel	panelChoice;
        Panel   panelTemp;
    	Label	label;


    	this.setLayout ( new BorderLayout(12,12) );

    	panelDescription = new Panel ( new GridLayout(0,1) );
    	this.add ( panelDescription, BorderLayout.NORTH );

    	panelDescription.add ( new Label(JMFI18N.getResource("jmstudio.export.type.label1")) );
    	panelDescription.add ( new Label(JMFI18N.getResource("jmstudio.export.type.label2")) );

    	panel = new Panel ( new BorderLayout(6,6) );
    	this.add ( panel, BorderLayout.CENTER );

    	panelTemp = new Panel ( new BorderLayout(6,6) );
    	panel.add ( panelTemp, BorderLayout.NORTH );

    	panelChoice = new Panel ( new GridLayout(0,1,6,6) );
    	panelTemp.add ( panelChoice, BorderLayout.WEST );

    	groupTargetType = new CheckboxGroup ();

    	panelTemp = new Panel ( new BorderLayout(6,6) );
    	panelChoice.add ( panelTemp );
    	radioFile = new Checkbox ( TYPE_FILE, groupTargetType, true );
    	panelTemp.add ( radioFile, BorderLayout.WEST );

    	panelTemp = new Panel ( new BorderLayout(6,6) );
    	panelChoice.add ( panelTemp );
    	radioNetwork = new Checkbox ( TYPE_NETWORK, groupTargetType, false );
    	panelTemp.add ( radioNetwork, BorderLayout.WEST );

    	panelTemp = new Panel ( new BorderLayout(6,6) );
    	panelChoice.add ( panelTemp );
    	radioScreen = new Checkbox ( TYPE_SCREEN, groupTargetType, false );
    	panelTemp.add ( radioScreen, BorderLayout.WEST );

    }

}


