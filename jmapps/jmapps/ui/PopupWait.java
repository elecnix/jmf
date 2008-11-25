/*
 * @(#)PopupWait.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.awt.*;
import java.awt.event.*;


public class PopupWait extends Window {

    private JMPanel         panelOuter;
    private JMPanel         panelInner;
    private JMPanel         panelImage;
    private ImageArea       fieldImage;
    private TextView        fieldText;

    private Frame           frameOwner;
    private String          strText;
    private UpdateThread    threadUpdate = new UpdateThread ();
    private Cursor          cursorOld;
    private Cursor          cursorWait = new Cursor ( Cursor.WAIT_CURSOR );
    private Cursor          cursorOldOwner;
    private Cursor          cursorWaitOwner = new Cursor ( Cursor.WAIT_CURSOR );


    public PopupWait ( Frame frameOwner, String strText ) {
        super ( frameOwner );

        this.frameOwner = frameOwner;
        this.strText = strText;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init () throws Exception {
        Image       image;
        Dimension   dimPopup;
        Dimension   dimScreen;
        Point       point;

        this.setLayout ( new BorderLayout(6,6) );

        panelOuter = new JMPanel ( new BorderLayout(6,6) );
        panelOuter.setBackground ( Color.lightGray );
        panelOuter.setRaisedBorder ();
        this.add ( panelOuter, BorderLayout.CENTER );

        panelInner = new JMPanel ( new BorderLayout(6,6) );
        panelInner.setEmptyBorder ( 6, 6, 6, 6 );
        panelOuter.add ( panelInner, BorderLayout.CENTER );

        panelImage = new JMPanel ( new FlowLayout() );
        panelInner.add ( panelImage, BorderLayout.WEST );

        image = ImageArea.loadImage ( "logo.gif", this, true );
        fieldImage = new ImageArea ( image );
        fieldImage.setInsets ( 0, 0, 0, 0 );
        panelImage.add ( fieldImage );

        fieldText = new TextView ( strText );
        fieldText.setPreferredWidth ( 256 );
        panelInner.add ( fieldText, BorderLayout.CENTER );

        pack ();
        dimPopup = this.getSize ();
        dimScreen = this.getToolkit().getScreenSize();
        point = new Point ( (dimScreen.width - dimPopup.width) / 2, (dimScreen.height - dimPopup.height) / 2 );
        this.setLocation ( point );
    }

    public void setVisible ( boolean boolVisible ) {
        super.setVisible ( boolVisible );

        if ( boolVisible == true ) {
            cursorOld = this.getCursor ();
            this.setCursor ( cursorWait );

            cursorOldOwner = frameOwner.getCursor ();
            frameOwner.setCursor ( cursorWaitOwner );

            threadUpdate.start ();
        }
        else {
            threadUpdate.terminateThread ();

            this.setCursor ( cursorOld );
            frameOwner.setCursor ( cursorOldOwner );
        }
    }

    private class UpdateThread extends Thread {
        private boolean     boolTerminate = false;

        public UpdateThread () {
            super ();
        }

        public void terminateThread () {
            boolTerminate = true;
        }

        public void run() {
            boolTerminate = false;
            while ( boolTerminate == false ) {
                try {
                    sleep ( 1000 );
                }
                catch ( Exception exception ) {
                }
//                updateComponent ( PopupWait.this );
                updateComponent ( PopupWait.this.panelOuter );
                updateComponent ( PopupWait.this.panelInner );
                updateComponent ( PopupWait.this.panelImage );
                updateComponent ( PopupWait.this.fieldImage );
                updateComponent ( PopupWait.this.fieldText );
            }
        }

        private void updateComponent ( Component component ) {
            Graphics    graphics;

            if ( component == null )
                return;
            if ( !component.isShowing() )
                return;
            graphics = component.getGraphics ();
            if ( graphics == null )
                return;

            component.update ( graphics );
        }

    }

}


