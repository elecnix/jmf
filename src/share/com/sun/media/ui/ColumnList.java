/*
 * @(#)ColumnList.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;


public class ColumnList extends Canvas implements MouseListener, FocusListener, KeyListener, ComponentListener {

    public static final int       TYPE_INTEGER = 1;
    public static final int       TYPE_DOUBLE = 2;
    public static final int       TYPE_STRING = 3;
    public static final int       TYPE_DATE = 4;

    private static final int      MARGIN_VERT = 2;
    private static final int      MARGIN_HORZ = 6;

    private static final Color    COLOR_HEADER_BG = Color.lightGray;
    private static final Color    COLOR_HEADER_FG = Color.black;
    private static final Color    COLOR_SHADOW_TOP = Color.white;
    private static final Color    COLOR_SHADOW_BOTTOM = Color.darkGray;
//    private static final Color    COLOR_SEL_BG = new Color ( 0, 0, 128 );
//    private static final Color    COLOR_SEL_FG = Color.yellow;
    private static final Color    COLOR_SEL_BG = Color.white;
    private static final Color    COLOR_SEL_FG = Color.black;

    private Vector     vectorColumns = new Vector (); // array of ColumnData
    private Vector     vectorRows = new Vector (); // array of RowData

    private boolean    boolFocus = false;
    private boolean    boolSetColumnWidthAsPreferred = false;
    private int        nScrollPosHorz = 0;
    private int        nScrollPosVert = 0;
    private int        nCurrentIndex = 0;
    private int        nVisibleRows = 1;

    private Font       fontHeader = new Font ( "Dialog", Font.PLAIN, 12 );
    private Font       fontItem = new Font ( "Dialog", Font.PLAIN, 12 );
    private int        nHeightHeader;
    private int        nHeightRow;

    public ColumnList ( String arrColumnNames[] ) {
        super ();

        int           i;
        int           nCount;
        ColumnData    column;

        nCount = arrColumnNames.length;
        for ( i = 0;  i < nCount;  i++ ) {
            column = new ColumnData ( arrColumnNames[i], TYPE_STRING );
            vectorColumns.addElement ( column );
        }

        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addRow ( Object arrValues[] ) {
        RowData   rowData;

        rowData = new RowData ( arrValues );
        vectorRows.addElement ( rowData );
        repaint ();
    }

    public void removeRow ( int nRowIndex ) {
        vectorRows.removeElementAt ( nRowIndex );
        repaint ();
    }

    public void setCellValue ( Object value, int nRowIndex, int nColumnIndex ) {
        RowData   rowData;

        rowData = (RowData) vectorRows.elementAt ( nRowIndex );
        rowData.setValue ( value, nColumnIndex );
        repaint ();
    }

    public void setColumnWidth ( int nWidth, int nColumnIndex ) {
        ColumnData   columnData;

        columnData = (ColumnData) vectorColumns.elementAt ( nColumnIndex );
        columnData.nWidth = nWidth;
        repaint ();
    }

    public void setColumnWidth ( int nWidth ) {
        int          i;
        int          nCount;
        ColumnData   columnData;


        nCount = vectorColumns.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            columnData = (ColumnData) vectorColumns.elementAt ( i );
            columnData.nWidth = nWidth;
        }
        repaint ();
    }

    public void setColumnWidthAsPreferred ( int nColumnIndex ) {
        int          nWidth;
        ColumnData   columnData;

        columnData = (ColumnData) vectorColumns.elementAt ( nColumnIndex );
        nWidth = getPreferredColumnWidth ( nColumnIndex );
        columnData.nWidth = nWidth;
        repaint ();
    }

    public void setColumnWidthAsPreferred () {
        int          i;
        int          nCount;
        int          nWidth;
        int          nWidthTotal;
        ColumnData   columnData;
        Rectangle    rect;
        int          nWidthExtra;


        nCount = vectorColumns.size ();
        nWidthTotal = 0;
        for ( i = 0;  i < nCount;  i++ ) {
            columnData = (ColumnData) vectorColumns.elementAt ( i );
            nWidth = getPreferredColumnWidth ( i );
            columnData.nWidth = nWidth;
            nWidthTotal += nWidth;
        }
        rect = getBounds ();
        if ( rect.width < 1 )
            boolSetColumnWidthAsPreferred = true;
        rect.width -= 2;
        if ( rect.width > nWidthTotal ) {
            nWidthExtra = (rect.width - nWidthTotal) / nCount;
            nWidthTotal = rect.width;
            for ( i = 0;  i < nCount;  i++ ) {
                columnData = (ColumnData) vectorColumns.elementAt ( i );
                if ( i < nCount - 1 )
                    columnData.nWidth += nWidthExtra;
                else
                    columnData.nWidth = nWidthTotal;
                nWidthTotal -= columnData.nWidth;
            }
        }
        repaint ();
    }

    public Dimension getPreferredSize () {
        int          i;
        int          nCount;
        Dimension    dim;
        int          nWidth;
        int          nHeight;


        dim = new Dimension ();

        dim.height += this.nHeightHeader;
        nHeight = this.nHeightRow;
        nHeight *= vectorRows.size ();
        dim.height += nHeight; 

        // columns
        nCount = vectorColumns.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            nWidth = this.getPreferredColumnWidth ( i );
            dim.width += nWidth;
        }

        dim.width += 3; // for shadow lines
        dim.height += 3; // for shadow lines
        return ( dim );
    }

    public boolean isFocusTraversable () {
        return ( true );
    }

    public void update ( Graphics g ) {
	Rectangle       rectClient;
        Image           image;
        Graphics        graphics;

	rectClient = this.getBounds ();
        image = createImage ( rectClient.width, rectClient.height );
        if ( image != null )
            graphics = image.getGraphics ();
        else
            graphics = g;

        paint ( graphics );

        if ( image != null )
            g.drawImage ( image, 0, 0, this );
    }

    public void paint ( Graphics graphics ) {
        int             i, j;
        int             nColCount;
        int             nRowCount;
	Rectangle       rect;
	Rectangle       rectClient;
        Font            font;
        FontMetrics     fontMetrics;
        ColumnData      columnData;
        RowData         rowData;
        int             nStartX;
        String          strValue;
        int             nX;
        int             nY;
        int             nWidth;
        int             nHeight;
        int             nLength;


	rectClient = this.getBounds ();
        rectClient.x = 0;
        rectClient.y = 0;
	rect = new Rectangle ( rectClient );

        super.paint ( graphics );

	graphics.setColor ( COLOR_SHADOW_BOTTOM );
        graphics.drawRect ( rect.x, rect.y, rect.width - 2, rect.height - 2 );
	graphics.setColor ( COLOR_SHADOW_TOP );
	graphics.drawLine ( rect.x + rect.width - 1, rect.y + 1, rect.x + rect.width - 1, rect.y + rect.height - 1 );
	graphics.drawLine ( rect.x + 1, rect.y + rect.height - 1, rect.x + rect.width - 1, rect.y + rect.height - 1 );

        nColCount = vectorColumns.size ();
        nRowCount = vectorRows.size ();

        rect.x += 1;
        rect.y += 1;
        nStartX = rect.x;

        // draw header
        fontMetrics = this.getFontMetrics ( fontHeader );
        graphics.setFont ( this.fontHeader );
        nHeight = fontMetrics.getHeight ();
        rect.height = nHeightHeader;
        for ( i = nScrollPosHorz;  i < nColCount;  i++ ) {
            columnData = (ColumnData) vectorColumns.elementAt ( i );
            rect.width = columnData.nWidth;
            if ( rect.x + rect.width > rectClient.x + rectClient.width - 1 )
                rect.width = rectClient.x + rectClient.width - 1 - rect.x;

            // draw header
            graphics.setColor ( COLOR_HEADER_BG );
            graphics.fillRect ( rect.x, rect.y, rect.width, rect.height );
            graphics.setColor ( COLOR_SHADOW_TOP );
            graphics.drawLine ( rect.x, rect.y, rect.x, rect.y + rect.height - 2 );
            graphics.drawLine ( rect.x, rect.y, rect.x + rect.width - 2, rect.y );
            graphics.setColor ( COLOR_SHADOW_BOTTOM );
            graphics.drawLine ( rect.x + rect.width - 1, rect.y + 1, rect.x + rect.width - 1, rect.y + rect.height - 1 );
            graphics.drawLine ( rect.x + 1, rect.y + rect.height - 1, rect.x + rect.width - 1, rect.y + rect.height - 1 );

            strValue = columnData.strName;
            nLength = strValue.length ();
            nWidth = fontMetrics.stringWidth ( strValue );
            while ( nWidth > rect.width - 2 * MARGIN_HORZ  &&  nLength > 0 ) {
                nLength--;
                strValue = strValue.substring ( 0, nLength ) + "...";
                nWidth = fontMetrics.stringWidth ( strValue );
            }
            nX = rect.x + (rect.width - nWidth) / 2;
            nY = rect.y + rect.height - (rect.height - nHeight) / 2 - fontMetrics.getMaxDescent();
            graphics.setColor ( this.getForeground() );
	    graphics.drawString ( strValue, nX, nY );

            rect.x += rect.width;
        }

        // draw rows
        font = this.getFont ();
        fontMetrics = this.getFontMetrics ( font );
        graphics.setFont ( font );
        nHeight = fontMetrics.getHeight ();

        rect.y += rect.height;
        rect.height = nHeightRow;
        for ( j = nScrollPosVert;  j < nRowCount;  j++ ) {
            rect.x = nStartX;
            if ( j == nCurrentIndex ) {
                rect.width = rectClient.width - 3;
                graphics.setColor ( COLOR_SEL_BG );
                graphics.fillRect ( rect.x, rect.y, rect.width, rect.height );
                graphics.setColor ( COLOR_SEL_FG );
                if ( boolFocus == true ) {
                    drawDottedLine ( graphics, rect.x, rect.y, rect.x + rect.width - 1, rect.y );
                    drawDottedLine ( graphics, rect.x, rect.y + rect.height - 1, rect.x + rect.width - 1, rect.y + rect.height - 1 );
                }
            }
            else
                graphics.setColor ( this.getForeground() );

            rowData = (RowData) vectorRows.elementAt ( j );
            for ( i = nScrollPosHorz;  i < nColCount;  i++ ) {
                columnData = (ColumnData) vectorColumns.elementAt ( i );
                rect.width = columnData.nWidth;
                if ( rect.x + rect.width > rectClient.x + rectClient.width - 1 )
                    rect.width = rectClient.x + rectClient.width - 1 - rect.x;
                strValue = rowData.getValue(i).toString();
                nLength = strValue.length ();
                nWidth = fontMetrics.stringWidth ( strValue );
                while ( nWidth > rect.width - 2 * MARGIN_HORZ  &&  nLength > 0 ) {
                    nLength--;
                    strValue = strValue.substring ( 0, nLength ) + "...";
                    nWidth = fontMetrics.stringWidth ( strValue );
                }
                nX = rect.x + MARGIN_HORZ;
                nY = rect.y + rect.height - (rect.height - nHeight) / 2 - fontMetrics.getMaxDescent();
                graphics.drawString ( strValue, nX, nY );
                rect.x += rect.width;
            }
            rect.y += rect.height;
        }

    }

    public void mouseClicked ( MouseEvent event ) {
    }

    public void mousePressed ( MouseEvent event ) {
        int             x, y;
        int             nIndex;

        x = event.getX ();
        y = event.getY ();
        y -= 1 + nHeightHeader;
        if ( y >= 0 ) {
            nIndex = y / nHeightRow;
            if ( nIndex >= 0  &&  nIndex < vectorRows.size() - nScrollPosVert )
                nCurrentIndex = nIndex + nScrollPosVert;
        }

        this.requestFocus ();
        repaint ();
    }

    public void mouseReleased ( MouseEvent event ) {
    }

    public void mouseEntered ( MouseEvent event ) {
    }

    public void mouseExited ( MouseEvent event ) {
    }

    public void keyTyped ( KeyEvent event ) {
    }

    public void keyPressed ( KeyEvent event ) {
        int       nKeyCode;
        int       nIndex;

        nKeyCode = event.getKeyCode ();
        nIndex = nCurrentIndex;
        if ( nKeyCode == KeyEvent.VK_DOWN )
            nIndex++;
        else if ( nKeyCode == KeyEvent.VK_UP )
            nIndex--;
        else if ( nKeyCode == KeyEvent.VK_HOME )
            nIndex = 0;
        else if ( nKeyCode == KeyEvent.VK_END )
            nIndex = vectorRows.size() - 1;
        else if ( nKeyCode == KeyEvent.VK_PAGE_UP )
            nIndex -= nVisibleRows;
        else if ( nKeyCode == KeyEvent.VK_PAGE_DOWN )
            nIndex += nVisibleRows;

        if ( nIndex > vectorRows.size() - 1 )
            nIndex = vectorRows.size() - 1;
        if ( nIndex < 0 )
            nIndex = 0;

        if ( nIndex != nCurrentIndex ) {
            nCurrentIndex = nIndex;
            if ( nScrollPosVert + nVisibleRows < nCurrentIndex )
                nScrollPosVert = nCurrentIndex - nVisibleRows + 1;
            if ( nScrollPosVert > nCurrentIndex )
                nScrollPosVert = nCurrentIndex;
            repaint ();
        }
    }

    public void keyReleased ( KeyEvent event ) {
    }

    public void focusGained ( FocusEvent event ) {
        if ( boolFocus == true )
            return;
        boolFocus = true;
        repaint ();
    }

    public void focusLost ( FocusEvent event ) {
        if ( boolFocus == false )
            return;
        boolFocus = false;
        repaint ();
    }

    public void componentResized ( ComponentEvent event ) {
        Rectangle    rect;

        if ( boolSetColumnWidthAsPreferred == true ) {
            boolSetColumnWidthAsPreferred = false;
            this.setColumnWidthAsPreferred ();
        }
        rect = getBounds ();
        rect.height -= 3 + nHeightHeader;
        nVisibleRows = rect.height / nHeightRow;
        if ( nVisibleRows < 1 )
            nVisibleRows = 1;
    }

    public void componentMoved ( ComponentEvent event ) {
    }

    public void componentShown ( ComponentEvent event ) {
    }

    public void componentHidden ( ComponentEvent event ) {
    }


    private void init () throws Exception {
        this.setFont ( fontItem );
        this.computeHeights ();
        this.setBackground ( Color.white );

        this.addMouseListener ( this );
        this.addKeyListener ( this );
        this.addFocusListener ( this );
        this.addComponentListener ( this );
    }

    private int getPreferredColumnWidth ( int nColumnIndex ) {
        int          i;
        int          nCount;
        int          nWidth;
        int          nWidthMax;
        Font         font;
        FontMetrics  fontMetrics;
        String       strValue;
        ColumnData   columnData;
        RowData      rowData;

        columnData = (ColumnData) vectorColumns.elementAt ( nColumnIndex );

        // header
        fontMetrics = this.getFontMetrics ( fontHeader );
        strValue = columnData.strName;
        nWidthMax = fontMetrics.stringWidth ( strValue ) + 2 * MARGIN_HORZ + 2;

        // rows
        font = this.getFont ();
        fontMetrics = this.getFontMetrics ( font );
        nCount = vectorRows.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            rowData = (RowData) vectorRows.elementAt ( i );
            strValue = rowData.getValue(nColumnIndex).toString();
            nWidth = fontMetrics.stringWidth ( strValue ) + 2 * MARGIN_HORZ;
            nWidthMax = Math.max ( nWidthMax, nWidth );
        }
        
        return ( nWidthMax );
    }

    private void computeHeights () {
        Font            font;
        FontMetrics     fontMetrics;

        fontMetrics = this.getFontMetrics ( fontHeader );
        nHeightHeader = fontMetrics.getHeight ();
        nHeightHeader += 2;  // shadow lines
        nHeightHeader += 2 * MARGIN_VERT;

        font = this.getFont ();
        fontMetrics = this.getFontMetrics ( font );
        nHeightRow = fontMetrics.getHeight ();
        nHeightRow += 2 * MARGIN_VERT;
    }

    private void drawDottedLine ( Graphics graphics, int nX1, int nY1, int nX2, int nY2 ) {
	int	nX, nY;
	double	dDiv;

	if ( nX1 == nX2  &&  nY1 == nY2 ) {
	    drawDot ( graphics, nX1, nY1 );
            return;
	}
	if ( nX1 > nX2 ) {
	    nX = nX1;
	    nX1 = nX2;
	    nX2 = nX;
	}
	if ( nY1 > nY2 ) {
	    nY = nY1;
	    nY1 = nY2;
	    nY2 = nY;
	}
	if ( nX2 - nX1 > nY2 - nY1 ) {
	    dDiv = (double)(nY2 - nY1) / (nX2 - nX1);
	    for ( nX = nX1;   nX <= nX2;   nX++ ) {
	    	nY = (int)Math.rint ( nY1 + (nX - nX1) * dDiv );
	    	drawDot ( graphics, nX, nY );
	    }
	}
	else {
	    dDiv = (nX2 - nX1) / (nY2 - nY1);
	    for ( nY = nY1;   nY <= nY2;   nY++ ) {
	    	nX = (int)Math.rint ( nX1 + (nY - nY1) * dDiv );
	    	drawDot ( graphics, nX, nY );
	    }
	}
    }

    private void drawDot ( Graphics graphics, int nX, int nY )
    {
    	if ( (nX + nY) % 2 == 0 )
            graphics.drawLine ( nX, nY, nX, nY );
    }


}


class ColumnData {
    String   strName;
    int      nType;
    int      nWidth = 120;

    public ColumnData ( String strName, int nType ) {
        this.strName = strName;
        this.nType = nType;
    }

    public String toString () {
        return ( strName );
    }
}

class RowData {
    private Vector   vectorValues = new Vector ();

    public RowData ( Object arrValues[] ) {
        int       i;
        int       nCount;

        if ( arrValues != null ) {
            nCount = arrValues.length;
            for ( i = 0;  i < nCount;  i++ )
                vectorValues.addElement ( arrValues[i] );
        }
    }

    void setValue ( Object value, int nColumn ) {
        vectorValues.setElementAt ( value, nColumn );
    }

    Object getValue ( int nColumn ) {
        Object    value;

        value = vectorValues.elementAt ( nColumn );
        return ( value );
    }
}
