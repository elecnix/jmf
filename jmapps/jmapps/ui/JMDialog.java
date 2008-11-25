/*
 * @(#)JMDialog.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.awt.*;
import java.awt.event.*;

import com.sun.media.util.JMFI18N;


public class JMDialog extends Dialog  implements ActionListener, WindowListener {

    public static final String     ACTION_OK = JMFI18N.getResource ( "jmstudio.dlg.ok" );
    public static final String     ACTION_CANCEL = JMFI18N.getResource ( "jmstudio.dlg.cancel" );
    public static final String     ACTION_CLOSE = JMFI18N.getResource ( "jmstudio.dlg.close" );
    public static final String     ACTION_OPEN = JMFI18N.getResource ( "jmstudio.dlg.open" );
    public static final String     ACTION_SAVE = JMFI18N.getResource ( "jmstudio.dlg.save" );
    public static final String     ACTION_YES = JMFI18N.getResource ( "jmstudio.dlg.yes" );
    public static final String     ACTION_NO = JMFI18N.getResource ( "jmstudio.dlg.no" );

    protected Frame         frameOwner = null;
    private String          strAction = ACTION_CANCEL;
    private boolean         boolFirstTimeVisible = true;


    public JMDialog ( Frame frame, String strTitle, boolean boolModal ) {
        super ( frame, strTitle, boolModal );

        frameOwner = frame;
    }

    public void addNotify () {
	    this.setBackground ( Color.lightGray );
        this.addWindowListener ( this );

        super.addNotify ();
        autoPosition ();
    }

    public void setVisible ( boolean boolVisible ) {

        if ( boolVisible == true  &&  !this.isVisible()  &&  boolFirstTimeVisible == true ) {
            boolFirstTimeVisible = false;
            autoPosition ();
        }
        super.setVisible ( boolVisible );
    }

    public void autoPosition () {
        Dimension       dim;
        Dimension       dimFrame;
        Dimension       dimDialog;
        Dimension       dimScreen;
        Point           point;
        Insets          insets;

        if ( frameOwner != null  &&  frameOwner.isShowing() ) {
            point = frameOwner.getLocationOnScreen ();
            if ( this.isModal() ) {
                insets = frameOwner.getInsets ();
                point.x += insets.left;
                point.y += insets.top;
            }
            else {
                dimFrame = frameOwner.getSize ();
                point.y += dimFrame.height;
            }
            dim = this.getPreferredSize ();
            dimDialog = this.getSize ();
            dimDialog.width = Math.max ( dim.width, dimDialog.width );
            dimDialog.height = Math.max ( dim.height, dimDialog.height );
            dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
            if ( point.x + dimDialog.width > dimScreen.width )
                point.x = dimScreen.width - dimDialog.width;
            if ( point.y + dimDialog.height > dimScreen.height )
                point.y = dimScreen.height - dimDialog.height;
            this.setLocation ( point );
        }
        else {
            setLocationCenter ();
        }
    }

    public void setLocationCenter () {
        Dimension       dimDialog;
        Dimension       dimScreen;
        Point           point;

        dimDialog = this.getSize ();
        dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
        point = new Point ( (dimScreen.width - dimDialog.width) / 2,
                                (dimScreen.height - dimDialog.height) / 2 );
        this.setLocation ( point );
    }

    public String getAction () {
        return ( strAction );
    }

    protected void setAction ( String strAction ) {
        this.strAction = strAction;
    }

    protected Frame getOwnerFrame () {
        return ( frameOwner );
    }

    protected JMPanel createButtonPanel ( String arrActions[] ) {
        int         i;
        int         nCount;
        JMPanel     panelButtons;
        Button      button;

        panelButtons = new JMPanel ( new GridLayout(1,0,12,6) );
        nCount = arrActions.length;
        for ( i = 0;  i < nCount;  i++ ) {
            button = new Button ( arrActions[i] );
            button.addActionListener ( this );
            panelButtons.add ( button );
        }
        return ( panelButtons );
    }

    public void actionPerformed ( ActionEvent event ) {
    }

    public void windowOpened ( WindowEvent event ) {
    }

    public void windowClosing ( WindowEvent event ) {
        this.dispose ();
    }

    public void windowClosed ( WindowEvent event ) {
    }

    public void windowIconified ( WindowEvent event ) {
    }

    public void windowDeiconified ( WindowEvent event ) {
    }

    public void windowActivated ( WindowEvent event ) {
    }

    public void windowDeactivated ( WindowEvent event ) {
    }

}


