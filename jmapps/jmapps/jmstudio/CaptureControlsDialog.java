/*
 * @(#)CaptureControlsDialog.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.jmstudio;

import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import com.sun.media.util.JMFI18N;

import jmapps.ui.*;


public class CaptureControlsDialog extends JMDialog {

    private DataSource      dataSource;
    private int             nControlCount = 0;


    public CaptureControlsDialog ( Frame parentFrame, DataSource dataSource ) {
        super ( parentFrame, JMFI18N.getResource("jmstudio.capturecontrols.title"), false );

        this.dataSource = dataSource;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isEmpty () {
        return ( nControlCount < 1 );
    }

    private void init () throws Exception {
        int             i;
        int             nCount = 0;
        Object          arrControls [] = null;
        Component       componentControl;
        JMPanel         panelContent;
        Panel           panel;
        Panel           panelNext;
        Panel           panelButtons;


        this.setLayout ( new BorderLayout() );

        panelContent = new JMPanel ( new BorderLayout() );
        panelContent.setEmptyBorder ( 6, 6, 6, 6 );
        this.add ( panelContent, BorderLayout.CENTER );

        if ( dataSource != null )
            arrControls = dataSource.getControls ();
        if ( arrControls != null )
            nCount = arrControls.length;
        panel = panelContent;
        nControlCount = 0;
        for ( i = 0;  i < nCount;  i++ ) {
            if ( arrControls[i] == null )
                continue;
            if ( !(arrControls[i] instanceof Control) )
                continue;
            componentControl = ((Control)arrControls[i]).getControlComponent ();
            if ( componentControl == null )
                continue;
            nControlCount++;
            panelNext = new Panel ( new BorderLayout(6,6) );
            panelNext.add ( componentControl, BorderLayout.NORTH );
            panel.add ( panelNext, BorderLayout.CENTER );
            panel = panelNext;
        }

        panel = new JMPanel ( new FlowLayout(FlowLayout.CENTER) );
        panelContent.add ( panel, BorderLayout.SOUTH );
        panelButtons = createButtonPanel ( new String[] { ACTION_CLOSE } );
        panel.add ( panelButtons );

        this.pack ();
        this.setResizable ( false );
    }

    public void actionPerformed ( ActionEvent event ) {
        String               strCmd;


        strCmd = event.getActionCommand ();
        if ( strCmd.equals(ACTION_CLOSE) ) {
            setAction ( ACTION_CLOSE );
            setVisible ( false );
        }
    }

    public void windowClosing ( WindowEvent event ) {
        setAction ( ACTION_CLOSE );
        setVisible ( false );
    }


}


