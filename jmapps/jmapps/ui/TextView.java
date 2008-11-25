/*
 * @(#)TextView.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.awt.*;
import java.awt.event.*;


public class TextView extends Canvas {

    private String      strText;
    private Insets      insetsBorder = new Insets ( 4, 12, 4, 12 );
    private int         nPreferredWidth = 0;

    static private Image    imageBuffer = null;


    public TextView ( String strText ) {
        this.strText = strText;
    }

    public void setText ( String strText ) {
        this.strText = strText;
        this.repaint();
    }

    public boolean isDoubleBuffered () {
        return ( true );
    }

    public void update ( Graphics graphics ) {
        int         nWidth = 0;
        int         nHeight = 0;
        Dimension   dim;
        Graphics    graphicsNew;


        dim = this.getSize ();
        if ( imageBuffer != null ) {
            nWidth = imageBuffer.getWidth ( this );
            nHeight = imageBuffer.getHeight ( this );
        }
        if ( imageBuffer == null  ||  nWidth < dim.width  ||  nHeight < dim.height ) {
            nWidth = Math.max ( nWidth, dim.width );
            nHeight = Math.max ( nHeight, dim.height );
            imageBuffer = this.createImage ( nWidth, nHeight );
        }

        graphicsNew = imageBuffer.getGraphics ();
        if ( graphicsNew == null )
            graphicsNew = graphics;

        super.update ( graphicsNew );
        
        if ( graphicsNew != graphics )
            graphics.drawImage ( imageBuffer, 0, 0, dim.width, dim.height, this );
    }

    public void paint ( Graphics g ) {
        int             i;
        int             nLength;
        char            character;
        int             nBegin;
        int             nEnd;
        String          strLine;
        int             nPreferredWidth;
        int             nLineWidth;
        int             nLineHeight;
        int             nOffsetY;
        Font            font;
        FontMetrics     fontMetrics;
        Dimension       dim;


        dim = this.getSize ();
        nPreferredWidth = dim.width - (insetsBorder.left + insetsBorder.right);
        font = this.getFont ();
        fontMetrics = this.getFontMetrics ( font );
        nLineHeight = fontMetrics.getHeight();
        nOffsetY = insetsBorder.top + fontMetrics.getMaxAscent();

        nLength = strText.length ();
        nEnd = 0;
        for ( i = 0;  i < nLength;  i++ ) {
            nBegin = nEnd;
            do {
                nEnd = i;
                do {
                    i++;
                    character = strText.charAt (i);
                } while ( i < nLength - 2  &&  Character.isLetterOrDigit(character) );

                strLine = strText.substring ( nBegin, i );
                nLineWidth = fontMetrics.stringWidth ( strLine );
                if ( i >= nLength - 1  &&  nLineWidth < nPreferredWidth )
                    nEnd = i - 1;
                if ( character == '\n'  ||  character == Character.LINE_SEPARATOR )
                    nEnd = i;
            } while ( i < nLength - 1  &&  nLineWidth < nPreferredWidth
                                    &&  character != '\n'
                                    &&  character != Character.LINE_SEPARATOR );

            do {
                nEnd++;
                character = strText.charAt ( nEnd );
            } while ( nEnd < nLength - 1  &&  Character.isSpaceChar(character) );
            i = nEnd;
            if ( i == nLength - 1 )
                i++;

            character = strText.charAt ( i - 1 );
            if ( character == '\n'  ||  character == Character.LINE_SEPARATOR )
                strLine = strText.substring ( nBegin, i - 1 );
            else
                strLine = strText.substring ( nBegin, i );
            g.drawString ( strLine, insetsBorder.left, nOffsetY );
            nOffsetY += nLineHeight;
        }
    }


    public Dimension getPreferredSize () {
        Dimension      dim;

        dim = new Dimension ( nPreferredWidth, 0 );
        dim.width += insetsBorder.left + insetsBorder.right;
//        dim.height += insetsBorder.top + insetsBorder.bottom;
        if ( nPreferredWidth > 0 )
            dim = getPreferredSize ( dim.width );
        return ( dim );
    }


    public void setPreferredWidth ( int nPreferredWidth ) {
        this.nPreferredWidth = nPreferredWidth;
    }

    public Dimension getPreferredSize ( int nPreferredWidth ) {
        int             i;
        int             nLength;
        char            character;
        int             nBegin;
        int             nEnd;
        String          strLine;
        int             nLineWidth;
        int             nLineCount;
        Dimension       dim;
        Font            font;
        FontMetrics     fontMetrics;


        dim = new Dimension ( 0, 0 );
        nPreferredWidth -= insetsBorder.left + insetsBorder.right;
        font = this.getFont ();
        fontMetrics = this.getFontMetrics ( font );

        nLength = strText.length ();
        nEnd = 0;
        nLineCount = 0;
        for ( i = 0;  i < nLength;  i++ ) {
            nBegin = nEnd;
            do {
                nEnd = i;
                do {
                    i++;
                    character = strText.charAt (i);
                } while ( i < nLength - 1  &&  Character.isLetterOrDigit(character) );

                strLine = strText.substring ( nBegin, i );
                nLineWidth = fontMetrics.stringWidth ( strLine );
                if ( i >= nLength - 1  &&  nLineWidth < nPreferredWidth )
                    nEnd = i - 1;
                if ( character == '\n'  ||  character == Character.LINE_SEPARATOR )
                    nEnd = i;
            } while ( i < nLength - 1  &&  nLineWidth < nPreferredWidth
                                    &&  character != '\n'
                                    &&  character != Character.LINE_SEPARATOR );

            strLine = strText.substring ( nBegin, nEnd );
            nLineWidth = fontMetrics.stringWidth ( strLine );
            dim.width = Math.max ( dim.width, nLineWidth );

            nLineCount++;

            do {
                nEnd++;
                character = strText.charAt ( nEnd );
            } while ( nEnd < nLength - 1  &&  Character.isSpaceChar(character) );
            i = nEnd;
        }

        dim.height = nLineCount * fontMetrics.getHeight();
        dim.height += insetsBorder.top + insetsBorder.bottom;
        dim.width += insetsBorder.left + insetsBorder.right + 2;
        return ( dim );
    }
/*
    public Insets getInsets () {
        return ( insetsBorder );
    }
*/
}


