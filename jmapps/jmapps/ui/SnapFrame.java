/*
 * @(#)SnapFrame.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.awt.*;
import java.awt.event.*;

public class SnapFrame extends JMFrame {

    private Dimension   dimImage;
    private Canvas      canvasImage;
    private Image       image;


    public SnapFrame ( Image image ) {
        this ( image, null );
    }

    public SnapFrame ( Image image, Frame frameOwner ) {
        super ( frameOwner );

        setLayout ( new BorderLayout() );

        canvasImage = new Canvas() {
            public Dimension getPreferredSize () {
                return ( dimImage );
            }
            public void paint ( Graphics g ) {
                super.paint ( g );
                g.drawImage ( getImage(), 0, 0, this );
            }
        };
        add ( canvasImage, BorderLayout.CENTER );
        setImage ( image );
    }

    public void setImage ( Image image ) {

        this.image = image;
        dimImage = new Dimension ( image.getWidth(this), image.getHeight(this) );
	    pack();
        setVisible(true);
	    canvasImage.repaint();
    }

    public Image getImage () {
        return ( image );
    }

    public void windowClosing ( WindowEvent event ) {
        this.setVisible ( false );
    }


}

