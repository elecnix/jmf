/*
 * @(#)ProgressBar.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.awt.*;


public class ProgressBar extends Canvas {

    private int    nMinPos;
    private int    nMaxPos;
    private int    nCurPos;

    public ProgressBar ( int nMin, int nMax ) {
        super ();

        nMinPos = nMin;
        nMaxPos = nMax;
        if ( nMaxPos <= nMinPos )
            nMaxPos = nMinPos + 1;
        nCurPos = nMinPos;
        this.setBackground ( Color.lightGray );
    }

    public int getMinPos () {
        return ( nMinPos );
    }

    public int getMaxPos () {
        return ( nMaxPos );
    }

    public int getCurPos () {
        return ( nCurPos );
    }

    public void setCurPos ( int nPos ) {
        nCurPos = nPos;
        if ( nCurPos > nMaxPos )
            nCurPos = nMaxPos;
        if ( nCurPos < nMinPos )
            nCurPos = nMinPos;
        repaint ();
    }

    public int getCurPercent () {
        return ( 100 * (nCurPos - nMinPos) / (nMaxPos - nMinPos) );
    }


    public void paint ( Graphics graphics ) {
        Rectangle    rect;

        rect = this.getBounds ();
        rect.x = 0;
        rect.y = 0;

        graphics.setColor ( Color.darkGray );
        graphics.drawLine ( rect.x, rect.y, rect.x, rect.y + rect.height - 2 );
        graphics.drawLine ( rect.x, rect.y, rect.x + rect.width - 2, rect.y );

        graphics.setColor ( Color.white );
        graphics.drawLine ( rect.x + rect.width - 1, rect.y + rect.height - 1, rect.x + rect.width - 1, rect.y + 1 );
        graphics.drawLine ( rect.x + rect.width - 1, rect.y + rect.height - 1, rect.x + 1, rect.y + rect.height - 1 );

        rect.x++;
        rect.y++;
        rect.width -= 2;
        rect.height -= 2;

        graphics.setColor ( Color.blue );
        rect.width = rect.width * (nCurPos - nMinPos) / (nMaxPos - nMinPos);
        graphics.fillRect ( rect.x, rect.y, rect.width, rect.height );
    }

    public Dimension getPreferredSize () {
        Dimension    dim;

        dim = new Dimension ( 128, 22 );
        return ( dim );
    }

}


