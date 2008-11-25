/*
 * @(#)ProgressDialog.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.awt.*;
import java.awt.event.*;

import com.sun.media.util.JMFI18N;


public class ProgressDialog extends JMDialog {

    public static final String    ACTION_ABORT = JMFI18N.getResource("jmstudio.saveprogress.abort");
    public static final String    ACTION_STOP  = JMFI18N.getResource("jmstudio.saveprogress.stop");
    public static final String    ACTION_PAUSE = JMFI18N.getResource("jmstudio.saveprogress.pause");
    public static final String    ACTION_RESUME = JMFI18N.getResource("jmstudio.saveprogress.resume");

    private int               nMinPos = 0;
    private int               nMaxPos = 0;
    private String            strMessage = null;
    private ActionListener    listener;
    private ProgressBar       progressBar = null;
    private Label             labelProgress;
    private Button            buttonPause;
    private Component         component = null;


    public ProgressDialog ( Frame frame, String strTitle, int nMin, int nMax, ActionListener listener ) {
        super ( frame, strTitle, false );

        nMinPos = nMin;
        nMaxPos = nMax;
        this.listener = listener;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ProgressDialog ( Frame frame, String strTitle, String strMessage, Component component, ActionListener listener ) {
        super ( frame, strTitle, false );

        this.strMessage = strMessage;
        this.listener = listener;
        this.component = component;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCurPos ( int nPos ) {
        if ( progressBar != null ) {
            progressBar.setCurPos ( nPos );
            labelProgress.setText ( "" + progressBar.getCurPercent() + "%" );
            repaint ();
        }
        else {
            labelProgress.setText ( JMFI18N.getResource("jmstudio.saveprogress.saved")
                                + " " + nPos + " "
                                + JMFI18N.getResource("jmstudio.saveprogress.seconds") + "...");
        }
    }

    public void setPauseButtonText ( String strButton ) {
        buttonPause.setLabel ( strButton );
        this.repaint ();
    }

    private void init () throws Exception {
        Panel          panel;
        Panel          panelGrid;
        Panel          panelComp;
        Button         button;
        Dimension      dimDialog;
        Dimension      dimScreen;


        this.setLayout ( new BorderLayout() );
        this.setBackground ( Color.lightGray );

        panel = new JMPanel ( new BorderLayout(6,6) );
        ((JMPanel)panel).setEmptyBorder ( 6, 6, 6, 6 );
        if ( nMaxPos > nMinPos ) {
            this.add ( panel, BorderLayout.CENTER );
            progressBar = new ProgressBar ( nMinPos, nMaxPos );
            panel.add ( progressBar, BorderLayout.CENTER );
            labelProgress = new Label ( "100%" );
            panel.add ( labelProgress, BorderLayout.EAST );
        }
        else if ( strMessage != null ) {
            this.add(panel, BorderLayout.CENTER);
            labelProgress = new Label ( strMessage );
            panel.add ( labelProgress, BorderLayout.NORTH );
            if (component != null) {
                panelComp = new Panel( new FlowLayout() );
                panel.add(panelComp, BorderLayout.CENTER);
                panelComp.add(component);
            }
        }

        panel = new Panel ( new FlowLayout(FlowLayout.CENTER) );
        this.add ( panel, BorderLayout.SOUTH );
        panelGrid = new Panel ( new GridLayout(1,0,6,6) );
        panel.add ( panelGrid );
        buttonPause = new Button ( ACTION_PAUSE );
        buttonPause.addActionListener ( listener );
        panelGrid.add ( buttonPause );
        if (strMessage == null)
            button = new Button ( ACTION_ABORT );
        else
            button = new Button ( ACTION_STOP );
        button.addActionListener ( listener );
        panelGrid.add ( button );

        panel = new Panel ();
        this.add ( panel, BorderLayout.NORTH );

        this.pack ();
        dimDialog = this.getPreferredSize ();
        this.setSize ( dimDialog );
        dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation ( (dimScreen.width - dimDialog.width) / 2, (dimScreen.height - dimDialog.height) / 2 );
        this.setResizable ( false );

        if (progressBar != null)
            labelProgress.setText ( "" + progressBar.getCurPercent() + "%" );
        repaint ();
    }

    public void windowClosing ( WindowEvent event ) {
        ActionEvent     eventAction;

        eventAction = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_ABORT );
        listener.actionPerformed ( eventAction );
    }

}


