/*
 * @(#)ImageArea.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;

public class ImageArea extends Canvas {

    private Image    image = null;
    private Insets   insetsBorder = new Insets (6,6,6,6);

    public ImageArea ( Image image ) {
	this.image = image;
    }

    public void setImage ( Image image ) {
	this.image = image;
    }

    public void setInsets ( int nLeft, int nTop, int nRight, int nBottom ) {
        insetsBorder.left = nLeft;
        insetsBorder.top = nTop;
        insetsBorder.right = nRight;
        insetsBorder.bottom = nBottom;
    }

    public void paint ( Graphics g ) {
        Dimension       dim;

        dim = this.getSize ();
        dim.width -= insetsBorder.left + insetsBorder.right;
        dim.height -= insetsBorder.top + insetsBorder.bottom;
        g.drawImage ( image, insetsBorder.left, insetsBorder.top, dim.width, dim.height, this );
    }

    public Dimension getPreferredSize () {
        Dimension      dim;

        if ( image == null )
            dim = new Dimension ( 0, 0 );
        else
            dim = new Dimension ( image.getWidth(this), image.getHeight(this) );
        dim.width += insetsBorder.left + insetsBorder.right;
        dim.height += insetsBorder.top + insetsBorder.bottom;
        return ( dim );
    }

    public static Image loadImage ( String strFileName ) {
        Image               image = null;
        URL                 url;
        Toolkit             toolkit;
        Class               classObject;
        Object              objImageProducer;
        ImageProducer       imageProducer;
        BufferedInputStream streamImage;
        byte                arrImageBytes [];

        toolkit = Toolkit.getDefaultToolkit ();

        if ( strFileName == null )
            return ( null );
        if ( !strFileName.startsWith("/jmapps/images/") )
            strFileName = "/jmapps/images/" + strFileName;

        try {
            classObject = Class.forName ( "jmapps.ui.ImageArea" );
            url = classObject.getResource ( strFileName );
            if ( url != null ) {
                objImageProducer = url.getContent ();
                if ( objImageProducer instanceof ImageProducer ) {
                    imageProducer = (ImageProducer) objImageProducer;
                    image = toolkit.createImage ( imageProducer );
                }
                else if ( objImageProducer instanceof BufferedInputStream ) {
                    streamImage = (BufferedInputStream) objImageProducer;
                    arrImageBytes = new byte [streamImage.available()];
                    streamImage.read ( arrImageBytes );
                    image = toolkit.createImage ( arrImageBytes );
                }
            }
        }
        catch ( Exception exception ) {
//            exception.printStackTrace ();
        }

        return ( image );
    }

    public static Image loadImage ( String strFileName, Component component, boolean boolWait ) {
        Image          image;
        MediaTracker   trackerMedia;

        image = loadImage ( strFileName );

        if ( image != null  &&  boolWait == true ) {
            trackerMedia = new MediaTracker ( component );
            trackerMedia.addImage ( image, 1001 );
            try {
                trackerMedia.waitForID ( 1001 );
            }
            catch ( Exception exception ) {
//                exception.printStackTrace ();
            }
        }
        return ( image );
    }

}


