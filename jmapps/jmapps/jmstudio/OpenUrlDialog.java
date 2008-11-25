/*
 * @(#)OpenUrlDialog.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.jmstudio;

import java.awt.*;
import java.awt.event.*;

import com.sun.media.util.JMFI18N;

import jmapps.ui.*;


public class OpenUrlDialog extends JMDialog {

    public static final String     LABEL = JMFI18N.getResource ( "jmstudio.openurl.label" );

    private TextField   fieldUrl;
    private String      nameUrlDefault = null;


    public OpenUrlDialog ( Frame frame, String nameUrlDefault ) {
        super ( frame, JMFI18N.getResource("jmstudio.openurl.title"), true );

        this.nameUrlDefault = nameUrlDefault;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUrl () {
        String  nameUrl;

        nameUrl = fieldUrl.getText ();
        return ( nameUrl );
    }


    protected void init () throws Exception {
        JMPanel     panel;
        JMPanel     panelButtons;
        Label       label;

	    setLayout ( new BorderLayout() );

        panel = new JMPanel ( new BorderLayout() );
        panel.setEmptyBorder ( 6, 6, 6, 6 );
        this.add ( panel, BorderLayout.CENTER );

        label = new Label ( LABEL );
        panel.add ( label, BorderLayout.WEST );

        if ( nameUrlDefault == null )
            nameUrlDefault = "";
	    fieldUrl = new TextField ( nameUrlDefault, 30 );
        fieldUrl.addActionListener ( this );
        panel.add ( fieldUrl, BorderLayout.CENTER );

        panel = new JMPanel ( new FlowLayout(FlowLayout.CENTER) );
        this.add ( panel, BorderLayout.SOUTH );
        panelButtons = createButtonPanel ( new String[] { ACTION_OPEN, ACTION_CANCEL } );
        panel.add ( panelButtons );

        this.pack ();
        this.setResizable ( false );
    }

    public void actionPerformed ( ActionEvent event ) {
        String      strAction;
        Object      objSource;

        strAction = event.getActionCommand ();
        objSource = event.getSource ();
        if ( strAction.equals(ACTION_OPEN) ) {
            this.setAction ( strAction );
            this.dispose ();
        }
        else if ( strAction.equals(ACTION_CANCEL) ) {
            this.setAction ( strAction );
            this.dispose ();
        }
        else if ( objSource == fieldUrl ) {
            this.setAction ( ACTION_OPEN );
            this.dispose ();
        }
    }


}


