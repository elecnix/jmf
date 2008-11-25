/*
 * "@(#)TabControl.java	1.17 02/08/21
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

import java.util.*;
import java.awt.*;
import java.awt.event.*;


public class TabControl extends Panel implements MouseListener, FocusListener, KeyListener, ComponentListener {

    public static final int    ALIGN_TOP = 0;
    public static final int    ALIGN_LEFT = 1;

//    private Vector      vectorPages = new Vector ();
//    private Vector      vectorTitles = new Vector ();
//    private Vector      vectorImages = new Vector ();
    private Panel       panelPageContainer;
    private CardLayout  layoutCard = new CardLayout ();
    private int         nCurrentPage = -1;
    private Button      buttonFocus;
    private boolean     boolFocus = false;
    private int         nAlignment = ALIGN_TOP;

    private int         MARGIN_PAGE_VERT = 6;
    private int         MARGIN_PAGE_HORZ = 6;

    private String      strPageToShowAfterPaint = null;
    private Cursor      cursorNormal = new Cursor ( Cursor.DEFAULT_CURSOR );
    private Cursor      cursorWait = new Cursor ( Cursor.WAIT_CURSOR );

    private Vector      vectorTabs = new Vector (); // of type TabField
    private int         nTabHeightMax = 1;
    private int         nTabWidthMax = 1;
//    private Vector      vectorDimTabs = new Vector ();
//    private Vector      vectorRectTabs = new Vector ();
    private int         nRowCount = 1;


    public TabControl () {
        this ( ALIGN_TOP );
    }

    public TabControl ( int nAlignment ) {
        super ();

        this.nAlignment = nAlignment;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init () throws Exception {
        Panel    panel;
        Font     font;
        Font     fontOld;

        this.setLayout ( new BorderLayout() );
        this.addComponentListener ( this );
        this.addMouseListener ( this );

        buttonFocus = new Button ( "Focus" );
        this.add ( buttonFocus );
        buttonFocus.addKeyListener ( this );
        buttonFocus.addFocusListener ( this );

        panelPageContainer = new Panel ( layoutCard );
        this.add ( panelPageContainer, BorderLayout.CENTER );

        fontOld = panelPageContainer.getFont ();
        if ( fontOld == null ) {
            fontOld = new Font ( "Dialog", Font.PLAIN, 12 );
        }
        font = new Font ( "Dialog", Font.PLAIN, 12 );
        this.setFont ( font );
        panelPageContainer.setFont ( fontOld );

        this.setBackground ( TabField.COLOR_BG );
        panelPageContainer.setBackground ( TabField.COLOR_BG );
    }

    public int addPage ( Panel panelPage, String strTitle ) {
        int    nIndex;

        nIndex = addPage ( panelPage, strTitle, null );
        return ( nIndex );
    }

    public int addPage ( Panel panelPage, String strTitle, Image image ) {
        int          nIndex;
        Dimension    dim;
        TabField     tabField;


        nIndex = vectorTabs.size ();
        tabField = new TabField ( this, panelPage, strTitle, image  );
        vectorTabs.addElement ( tabField );

        panelPageContainer.add ( panelPage, strTitle );
        if ( nIndex == 0 ) {
            nCurrentPage = 0;
            layoutCard.show ( panelPageContainer, strTitle );
        }

        tabField.calculateTabDimension ( getFontMetrics(getFont()) );
        nTabHeightMax = Math.max ( tabField.dim.height, nTabHeightMax );
        nTabWidthMax = Math.max ( tabField.dim.width, nTabWidthMax );

        recalculateTabs ();
        repaint ();

        return ( nIndex );
    }

    public int setPageImage ( Panel panelPage, Image imageTab ) {
        int          i;
        int          nCount;
        TabField     tabField;
        int          nIndex;
        Dimension    dim;


        nIndex = findPage ( panelPage );
        if ( nIndex < 0  ||  nIndex >= vectorTabs.size() )
            return ( nIndex );

        tabField = (TabField) vectorTabs.elementAt ( nIndex );
        if ( tabField.image == imageTab )
            return ( nIndex );
        tabField.image = imageTab;

        nTabHeightMax = 1;
        nTabWidthMax = 1;
        nCount = vectorTabs.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            tabField = (TabField) vectorTabs.elementAt ( i );
            tabField.calculateTabDimension ( getFontMetrics(getFont()) );
            nTabHeightMax = Math.max ( tabField.dim.height, nTabHeightMax );
            nTabWidthMax = Math.max ( tabField.dim.width, nTabWidthMax );
        }

        recalculateTabs ();
        repaint ();
        return ( nIndex );
    }

    public Dimension getPreferredSize () {
        int          i;
        TabField     tabField;
        Dimension    dim;
        int          nRowWidth;


        dim = super.getPreferredSize ();

        if ( nAlignment == ALIGN_LEFT )
            dim.height = Math.max ( dim.height, nTabHeightMax * vectorTabs.size() + 1 );
        else { // if ( nAlignment == ALIGN_TOP )
            nRowWidth = 0;
            for ( i = 0;  i < vectorTabs.size();  i++ ) {
                tabField = (TabField) vectorTabs.elementAt ( i );
                nRowWidth += tabField.dim.width;
            }
            dim.width = Math.max ( dim.width, nRowWidth + 1 );
        }
        return ( dim );
    }

    public Insets getInsets () {
        Insets    insets;

        insets = super.getInsets ();
        if ( nAlignment == ALIGN_LEFT )
            insets = new Insets ( insets.top + MARGIN_PAGE_VERT,
                                  insets.left + nRowCount * nTabWidthMax - 2 + MARGIN_PAGE_HORZ,
                                  insets.bottom + MARGIN_PAGE_VERT,
                                  insets.right + MARGIN_PAGE_HORZ );
        else // if ( nAlignment == ALIGN_TOP )
            insets = new Insets ( insets.top + nRowCount * nTabHeightMax - 2 + MARGIN_PAGE_VERT,
                                  insets.left + MARGIN_PAGE_HORZ,
                                  insets.bottom + MARGIN_PAGE_VERT,
                                  insets.right + MARGIN_PAGE_HORZ );
        return ( insets );
    }

    public void update ( Graphics g ) {
	Rectangle       rectClient;
        Image           image;
        Graphics        graphics;

	rectClient = this.getBounds ();
	// Safeguard to prevent an exception on 8-bit displays
	if (rectClient.width < 1 || rectClient.height < 1)
	    return;
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
	int		i;
	int		nSize;
	Rectangle       rect;
        TabField        tabField;
	Rectangle       rectClient;
	Rectangle       rectString;
	String		stringTabName;
        Font            fontNormal;
        Font            fontBold;
	FontMetrics	fontMetrics;
	int		nX, nY;
	int		nWidth, nHeight;


        super.paint ( graphics );

	rectClient = this.getBounds ();
        rectClient.x = 0;
        rectClient.y = 0;

	rect = new Rectangle ( rectClient );
        if ( nAlignment == ALIGN_LEFT ) {
            rect.x += nTabWidthMax * nRowCount - 2;
            rect.width -= nTabWidthMax * nRowCount - 2;
        }
        else { // if ( nAlignment == ALIGN_TOP )
            rect.y += nTabHeightMax * nRowCount - 2;
            rect.height -= nTabHeightMax * nRowCount - 2;
        }
        rect.width--;
        rect.height--;

	graphics.setColor ( TabField.COLOR_SHADOW_BOTTOM );
	graphics.drawRect ( rect.x, rect.y, rect.width, rect.height );
	graphics.setColor ( TabField.COLOR_SHADOW_TOP );
	graphics.drawLine ( rect.x + 1, rect.y + 1, rect.x + 1, rect.y + rect.height - 2 );
	graphics.drawLine ( rect.x + 1, rect.y + 1, rect.x + rect.width - 2, rect.y + 1 );

        fontNormal = this.getFont ();
//        fontBold = new Font ( fontNormal.getName(), Font.BOLD, fontNormal.getSize() );
        fontBold = fontNormal;
	nSize = vectorTabs.size ();
	for ( i = nSize - 1;   i >= 0;   i-- ) {
            tabField = (TabField) vectorTabs.elementAt ( i );
            if ( i == nCurrentPage ) {
                if ( nAlignment == ALIGN_LEFT )
                    tabField.drawCurrentTabLeft ( graphics );
                else // if ( nAlignment == ALIGN_TOP )
                    tabField.drawCurrentTabTop ( graphics );
            }
            else {
                if ( nAlignment == ALIGN_LEFT )
                    tabField.drawTabLeft ( graphics );
                else // if ( nAlignment == ALIGN_TOP )
                    tabField.drawTabTop ( graphics );
            }

            graphics.setColor ( this.getForeground() );
	    rectString = new Rectangle ( tabField.rect );
            if ( nAlignment == ALIGN_LEFT )
                rectString.width = nTabWidthMax;
            else // if ( nAlignment == ALIGN_TOP )
                rectString.height = nTabHeightMax;

            if ( tabField.image != null ) {
                nWidth = tabField.image.getWidth ( this );
                rectString.x += nWidth + tabField.MARGIN_TAB_HORZ;
                rectString.width -= nWidth + tabField.MARGIN_TAB_HORZ;
            }
	    rectString.y++;
	    if ( i == nCurrentPage ) {
	    	graphics.setFont ( fontBold );
	    	fontMetrics = graphics.getFontMetrics ( fontBold );
	    	if ( boolFocus == true ) {
	    	    nWidth = fontMetrics.stringWidth ( tabField.strTitle ) + 6;
	    	    nHeight = fontMetrics.getHeight () + 1;
	    	    nX = rectString.x + (rectString.width - nWidth) / 2;
	    	    nY = rectString.y + (nTabHeightMax - 2 - nHeight) / 2 + 1;
	    	    drawDottedRectangle ( graphics, nX, nY, nWidth, nHeight );
	    	}
	    }
	    else {
	    	graphics.setFont ( fontNormal );
	    	fontMetrics = graphics.getFontMetrics ( fontNormal );
	    }
	    nX = rectString.x + (rectString.width - fontMetrics.stringWidth(tabField.strTitle)) / 2;
	    nY = rectString.y + rectString.height - (rectString.height - fontMetrics.getHeight()) / 2 - fontMetrics.getMaxDescent ();
            nY--;
	    if ( i != nCurrentPage ) {
	    	nX++;
	    	nY++;
	    }
	    graphics.drawString ( tabField.strTitle, nX, nY );

            if ( tabField.image != null ) {
                nHeight = tabField.image.getHeight ( this );
                nX = tabField.rect.x + tabField.MARGIN_TAB_HORZ;
                nY = tabField.rect.y + (nTabHeightMax - nHeight) / 2;
                if ( i != nCurrentPage ) {
                    nX++;
                    nY++;
                }
                graphics.drawImage ( tabField.image, nX, nY, this );
            }

	}

        if ( strPageToShowAfterPaint != null ) {
            layoutCard.show ( panelPageContainer, strPageToShowAfterPaint );
            strPageToShowAfterPaint = null;
            this.setCursor ( cursorNormal );
        }
    }


    private void drawDottedRectangle ( Graphics graphics, int nX, int nY, int nWidth, int nHeight ) {
	drawDottedLine ( graphics, nX, nY, nX + nWidth - 1, nY );
	drawDottedLine ( graphics, nX + nWidth - 1, nY, nX + nWidth - 1, nY + nHeight - 1 );
	drawDottedLine ( graphics, nX + nWidth - 1, nY + nHeight - 1, nX, nY + nHeight - 1 );
	drawDottedLine ( graphics, nX, nY + nHeight - 1, nX, nY );
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

    public void mouseClicked ( MouseEvent event ) {
    }

    public void mousePressed ( MouseEvent event ) {
        int          i;
        int          nTabCount;
        TabField     tabField;
        int          x, y;

        x = event.getX ();
        y = event.getY ();

        nTabCount = vectorTabs.size ();
        for ( i = 0;  i < nTabCount;  i++ ) {
            tabField = (TabField) vectorTabs.elementAt ( i );
            if ( tabField.rect.contains (x,y) ) {
                buttonFocus.requestFocus ();
                nCurrentPage = i;
                strPageToShowAfterPaint = tabField.strTitle;
                this.setCursor ( cursorWait );
                repaint ();

                break;
            }
        }
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
        int          nKeyCode;
        int          nIndex;
        TabField     tabField;

        nIndex = nCurrentPage;
        nKeyCode = event.getKeyCode ();
        if ( nKeyCode == KeyEvent.VK_DOWN  ||  nKeyCode == KeyEvent.VK_RIGHT ) {
            nIndex++;
        }
        else if ( nKeyCode == KeyEvent.VK_UP  ||  nKeyCode == KeyEvent.VK_LEFT ) {
            nIndex--;
        }

        if ( nIndex >= vectorTabs.size() )
            nIndex = vectorTabs.size() - 1;
        if ( nIndex < 0 )
            nIndex = 0;

        if ( nCurrentPage != nIndex ) {
            nCurrentPage = nIndex;
            tabField = (TabField) vectorTabs.elementAt ( nIndex );
            strPageToShowAfterPaint = tabField.strTitle;
            this.setCursor ( cursorWait );
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
        recalculateTabs ();
        this.doLayout ();
        panelPageContainer.validate ();
    }

    public void componentMoved ( ComponentEvent event ) {
    }

    public void componentShown ( ComponentEvent event ) {
    }

    public void componentHidden ( ComponentEvent event ) {
    }

    private void recalculateTabs () {
        int          i, j;
        int          nTabCount;
        int          nRowSize = 1;
        int          nRowIndex;
        Rectangle    rectClient;
        int          nOffsetX;
        int          nOffsetY;
        int          nRowWidth;
        TabField     tabField;


        rectClient = this.getBounds ();
        rectClient.x = 0;
        rectClient.y = 0;
        if ( rectClient.width < 1  ||  rectClient.height < 1 )
            return;

        nTabCount = vectorTabs.size ();
        if ( nAlignment == ALIGN_LEFT ) {
            nRowSize = rectClient.height / nTabHeightMax;
            nRowCount = (nTabCount + nRowSize - 1) / nRowSize;
            nOffsetX = nRowCount * nTabWidthMax;
            nOffsetY = 0;
            for ( i = 0;  i < nTabCount;  i++ ) {
                if ( (i % nRowSize) == 0 ) {
                    nOffsetX -= nTabWidthMax;
                    nOffsetY = 0;
                }
                tabField = (TabField) vectorTabs.elementAt ( i );
                tabField.rect.x = nOffsetX;
                tabField.rect.y = nOffsetY;
                tabField.rect.width = nTabWidthMax * (i / nRowSize + 1);
                tabField.rect.height = nTabHeightMax;
                tabField.nRowIndex = i / nRowSize;
                if ( tabField.nRowIndex > 0 )
                    tabField.rect.width -= 2;

                nOffsetY += nTabHeightMax;
            }
        }
        else { // if ( nAlignment == ALIGN_TOP )
            nRowCount = 1;
            nRowWidth = 0;
            for ( i = 0;  i < nTabCount;  i++ ) {
                tabField = (TabField) vectorTabs.elementAt ( i );
                if ( nRowWidth + tabField.dim.width > rectClient.width ) {
                    nRowWidth = 0;
                    nRowCount++;
                }
                nRowWidth += tabField.dim.width;
            }
            nOffsetX = 0;
            nOffsetY = nRowCount * nTabHeightMax;
            nRowIndex = 0;
            j = 0;
            for ( i = 0;  i < nTabCount;  i++ ) {
                if ( i == j ) { // start of the row
                    nOffsetX = 0;
                    nOffsetY -= nTabHeightMax;
                    nRowWidth = 0;
                    for ( j = i;  j < nTabCount;  j++ ) {
                        tabField = (TabField) vectorTabs.elementAt ( j );
                        if ( j > i  &&  nRowWidth + tabField.dim.width > rectClient.width )
                            break;

                        nRowWidth += tabField.dim.width;
                        tabField.nRowIndex = nRowIndex;
                    }
                    nRowSize = j - i;
                    nRowIndex++;
                }

                tabField = (TabField) vectorTabs.elementAt ( i );
                tabField.rect.x = nOffsetX;
                tabField.rect.y = nOffsetY;
                tabField.rect.width = tabField.dim.width;
                if ( nRowCount > 1  &&  nRowIndex < nRowCount ) {
                    tabField.rect.width += (rectClient.width - nRowWidth - 1) / nRowSize;
                    tabField.rect.width += (j - i > (rectClient.width - nRowWidth - 1) % nRowSize) ? 0 : 1;
                }
                tabField.rect.height = nTabHeightMax * nRowIndex;
                if ( tabField.nRowIndex > 0 )
                    tabField.rect.height -= 2;
                nOffsetX += tabField.rect.width;
            }
        }
	repaint();
    }

    private int findPage ( Panel panelPage ) {
        int          i;
        TabField     tabField;


        i = vectorTabs.size() - 1;
        while ( i >= 0 ) {
            tabField = (TabField) vectorTabs.elementAt ( i );
            if ( tabField.panelPage == panelPage )
                break;
            i--;
        }
        return ( i );
    }

}


class TabField {

    Panel       panelPage = null;
    String      strTitle = null;
    Image       image = null;
    Dimension   dim = new Dimension ();
    Rectangle   rect = new Rectangle ();
    int         nRowIndex = 0;
    Component   compOwner;

    int         MARGIN_TAB_VERT = 5;
    int         MARGIN_TAB_HORZ = 8;

    public static final Color    COLOR_BG = Color.lightGray;
    public static final Color    COLOR_FG = Color.black;
    public static final Color    COLOR_SHADOW_TOP = Color.white;
    public static final Color    COLOR_SHADOW_BOTTOM = Color.darkGray;
    public static final Color    COLOR_TAB_BG = new Color ( 128, 128, 128 );
    public static final Color    COLOR_TAB_FG = Color.black;
    public static final Color    COLOR_TAB_SHADOW_TOP = Color.lightGray;
    public static final Color    COLOR_TAB_SHADOW_BOTTOM = Color.darkGray;


    public TabField ( Component compOwner, Panel panelPage, String strTitle, Image image ) {
        this.compOwner = compOwner;
        this.panelPage = panelPage;
        this.strTitle = strTitle;
        this.image = image;
    }

    public void calculateTabDimension ( FontMetrics fontMetrics ) {

        dim.width = fontMetrics.stringWidth ( strTitle );
        dim.height = fontMetrics.getHeight ();

        if ( image != null ) {
            dim.width += image.getWidth ( compOwner ) + MARGIN_TAB_HORZ;
            dim.height = Math.max ( image.getHeight(compOwner), dim.height );
        }

        dim.width += 2 * MARGIN_TAB_HORZ;
        dim.height += 2 * MARGIN_TAB_VERT;
        rect.width = dim.width;
        rect.height = dim.height;
    }

    public void drawTabTop ( Graphics graphics ) {
        int    arrX [] = new int [5];
        int    arrY [] = new int [5];

	graphics.setColor ( COLOR_TAB_BG );
        arrX[0] = rect.x + 6;
        arrY[0] = rect.y + 2;
        arrX[1] = rect.x + rect.width - 0;
        arrY[1] = rect.y + 2;
        arrX[2] = rect.x + rect.width - 0;
        arrY[2] = rect.y + rect.height - 2;
        arrX[3] = rect.x + 2;
        arrY[3] = rect.y + rect.height - 2;
        arrX[4] = rect.x + 2;
        arrY[4] = rect.y + 6;
	graphics.fillPolygon ( arrX, arrY, 5 );

	graphics.setColor ( COLOR_TAB_SHADOW_BOTTOM );
	graphics.drawLine ( rect.x, rect.y + rect.height - 2, rect.x, rect.y + 6 );
	graphics.drawLine ( rect.x, rect.y + 6, rect.x + 6, rect.y );
	graphics.drawLine ( rect.x + 6, rect.y, rect.x + rect.width - 1, rect.y );
	graphics.drawLine ( rect.x + rect.width, rect.y + 1, rect.x + rect.width, rect.y + rect.height - 2 );

	graphics.setColor ( COLOR_TAB_SHADOW_TOP );
	graphics.drawLine ( rect.x + 1, rect.y + rect.height - 3, rect.x + 1, rect.y + 6 );
	graphics.drawLine ( rect.x + 1, rect.y + 6, rect.x + 6, rect.y + 1 );
	graphics.drawLine ( rect.x + 6, rect.y + 1, rect.x + rect.width - 1, rect.y + 1 );
    }

    public void drawTabLeft ( Graphics graphics ) {
        int    arrX [] = new int [5];
        int    arrY [] = new int [5];

	graphics.setColor ( COLOR_TAB_BG );
        arrX[0] = rect.x + 2;
        arrY[0] = rect.y + 6;
        arrX[1] = rect.x + 2;
        arrY[1] = rect.y + rect.height - 0;
        arrX[2] = rect.x + rect.width - 2;
        arrY[2] = rect.y + rect.height - 0;
        arrX[3] = rect.x + rect.width - 2;
        arrY[3] = rect.y + 2;
        arrX[4] = rect.x + 6;
        arrY[4] = rect.y + 2;
	graphics.fillPolygon ( arrX, arrY, 5 );

	graphics.setColor ( COLOR_TAB_SHADOW_BOTTOM );
	graphics.drawLine ( rect.x + rect.width - 2, rect.y, rect.x + 6, rect.y );
	graphics.drawLine ( rect.x + 6, rect.y, rect.x, rect.y + 6 );
	graphics.drawLine ( rect.x, rect.y + 6, rect.x, rect.y + rect.height - 1 );
	graphics.drawLine ( rect.x + 1, rect.y + rect.height, rect.x + rect.width - 2, rect.y + rect.height );

	graphics.setColor ( COLOR_TAB_SHADOW_TOP );
	graphics.drawLine ( rect.x + rect.width - 3, rect.y + 1, rect.x + 6, rect.y + 1 );
	graphics.drawLine ( rect.x + 6, rect.y + 1, rect.x + 1, rect.y + 6 );
	graphics.drawLine ( rect.x + 1, rect.y + 6, rect.x + 1, rect.y + rect.height - 1 );
    }

    public void drawCurrentTabTop ( Graphics graphics ) {
        int    arrX [] = new int [5];
        int    arrY [] = new int [5];

	graphics.setColor ( COLOR_BG );
        arrX[0] = rect.x + 6;
        arrY[0] = rect.y + 2;
        arrX[1] = rect.x + rect.width - 0;
        arrY[1] = rect.y + 2;
        arrX[2] = rect.x + rect.width - 0;
        arrY[2] = rect.y + rect.height - 0; //((nRowIndex==0)?0:2);
        arrX[3] = rect.x + 2;
        arrY[3] = rect.y + rect.height - 0; //((nRowIndex==0)?0:2);
        arrX[4] = rect.x + 2;
        arrY[4] = rect.y + 6;

	graphics.fillPolygon ( arrX, arrY, 5 );

	graphics.setColor ( COLOR_SHADOW_BOTTOM );
	graphics.drawLine ( rect.x, rect.y + rect.height - 2, rect.x, rect.y + 6 );
	graphics.drawLine ( rect.x, rect.y + 6, rect.x + 6, rect.y );
	graphics.drawLine ( rect.x + 6, rect.y, rect.x + rect.width - 1, rect.y );
	graphics.drawLine ( rect.x + rect.width, rect.y + 1, rect.x + rect.width, rect.y + rect.height - 2 );

	graphics.setColor ( COLOR_SHADOW_TOP );
	graphics.drawLine ( rect.x + 1, rect.y + rect.height - 2, rect.x + 1, rect.y + 6 );
	graphics.drawLine ( rect.x + 1, rect.y + 6, rect.x + 6, rect.y + 1 );
	graphics.drawLine ( rect.x + 6, rect.y + 1, rect.x + rect.width - 1, rect.y + 1 );
    }

    public void drawCurrentTabLeft ( Graphics graphics ) {
        int    arrX [] = new int [5];
        int    arrY [] = new int [5];

	graphics.setColor ( COLOR_BG );
        arrX[0] = rect.x + 2;
        arrY[0] = rect.y + 6;
        arrX[1] = rect.x + 2;
        arrY[1] = rect.y + rect.height - 0;
        arrX[2] = rect.x + rect.width - 0; //((nRowIndex==0)?0:2);
        arrY[2] = rect.y + rect.height - 0;
        arrX[3] = rect.x + rect.width - 0; //((nRowIndex==0)?0:2);
        arrY[3] = rect.y + 2;
        arrX[4] = rect.x + 6;
        arrY[4] = rect.y + 2;
	graphics.fillPolygon ( arrX, arrY, 5 );

	graphics.setColor ( COLOR_SHADOW_BOTTOM );
	graphics.drawLine ( rect.x + rect.width - 2, rect.y, rect.x + 6, rect.y );
	graphics.drawLine ( rect.x + 6, rect.y, rect.x, rect.y + 6 );
	graphics.drawLine ( rect.x, rect.y + 6, rect.x, rect.y + rect.height - 1 );
	graphics.drawLine ( rect.x + 1, rect.y + rect.height, rect.x + rect.width - 2, rect.y + rect.height );

	graphics.setColor ( COLOR_SHADOW_TOP );
	graphics.drawLine ( rect.x + rect.width - 2, rect.y + 1, rect.x + 6, rect.y + 1 );
	graphics.drawLine ( rect.x + 6, rect.y + 1, rect.x + 1, rect.y + 6 );
	graphics.drawLine ( rect.x + 1, rect.y + 6, rect.x + 1, rect.y + rect.height - 1 );
    }

}


