/*
 * "@(#)ToolTip.java	1.5 99/05/23
 *
 * Copyright (c) 1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

package com.sun.media.ui;

import java.awt.*;
import java.awt.event.*;

/**
* This class...
*/
public class ToolTip extends Window
{
    private static final int	MARGIN_HORZ = 4;
    private static final int	MARGIN_VERT = 2;

    private String		arrStrings[] = null;

    /**
    * Constructs the ToolTip object.
    * @param	strText      text to display
    */
    public ToolTip ( String strText ) {
        super ( new Frame() );

        Font    font;

    	arrStrings = new String [1];
        arrStrings[0] = new String ( strText );
        font = new Font ( "Helvetica", Font.PLAIN, 10 );
        this.setFont ( font );
    	resizePopup ();
    }

    /**
    * Constructs the ToolTip object.
    * @param	arrStrings	array of strings
    */
    public ToolTip ( String arrStrings[] ) {
        super ( new Frame() );

    	int     i;
        Font    font;

    	arrStrings = new String [arrStrings.length];
    	for ( i = 0;   i < arrStrings.length;   i++ )
            arrStrings[i] = new String ( arrStrings[i] );
        font = new Font ( "Helvetica", Font.PLAIN, 10 );
        this.setFont ( font );
    	resizePopup ();
    }

    public void setText ( String strText ) {
    	arrStrings = new String [1];
        arrStrings[0] = new String ( strText );
    	resizePopup ();
        repaint ();
    }

    /**
    * This method is called, when the window needs redrawing.
    * @param	graphics	the specified Graphics window
    */
    public void paint ( Graphics graphics ) {
    	int		i;
    	Rectangle	rect;
    	int		nX, nY;
    	int		nHeight;
        Font            font;
    	FontMetrics	fontMetrics;


    	rect = getBounds ();
        font = getFont ();
    	fontMetrics = getFontMetrics ( font );

    	graphics.setColor ( new Color(255,255,192) );
    	graphics.fillRect ( 0, 0, rect.width, rect.height );

    	graphics.setColor ( Color.black );
    	graphics.drawRect ( 0, 0, rect.width-1, rect.height-1 );

    	nX = MARGIN_HORZ;
    	nY = MARGIN_VERT + fontMetrics.getAscent ();
    	nHeight = fontMetrics.getHeight ();
    	for ( i = 0;   i < arrStrings.length;   i++ ) {
            graphics.drawString ( arrStrings[i], nX, nY );
            nY += nHeight;
    	}
    }

    /**
    * This method resizes window to fit all strings.
    */
    private void resizePopup () {
    	int		i;
    	int		nWidth = 0;
    	int		nHeight = 0;
    	int		nWidthText;
    	Rectangle	rect;
        Font            font;
    	FontMetrics	fontMetrics;
        Dimension       dim;


    	rect = getBounds ();
        font = getFont ();
    	fontMetrics = getFontMetrics ( font );

    	for ( i = 0;   i < arrStrings.length;   i++ ) {
            nWidthText = fontMetrics.stringWidth ( arrStrings[i] );
            nWidth = Math.max ( nWidth, nWidthText );
    	}
    	nHeight = fontMetrics.getHeight() * arrStrings.length;

    	rect.width = nWidth + 2 * MARGIN_HORZ;
    	rect.height = nHeight + 2 * MARGIN_VERT;
        dim = this.getSize ();
        if ( dim.height != rect.height  ||  rect.width > dim.width  ||  rect.width < dim.width / 2 )
    	    setBounds ( rect );
    }

}

