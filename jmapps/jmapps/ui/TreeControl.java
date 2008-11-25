/*
 * @(#)TreeControl.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;


public class TreeControl extends JMPanel implements ComponentListener,
                                    MouseListener, KeyListener, FocusListener,
                                    ActionListener, AdjustmentListener {

    private TreeNode    nodeRoot = null;
    private TreeNode    nodeTop = null;
    private TreeNode    nodeCurrent = null;
    private Vector      vectorTreeElList = new Vector ();
    private	boolean     boolLayoutDone = false;
    private boolean     boolRootVisible = true;

    private Scrollbar   scrollBarVert = null;
    private Scrollbar   scrollBarHorz = null;

    private int         nScrollVertVisible;
    private int         nScrollHorzMax;
    private int         nScrollHorzVisible;
    private int         nScrollHorzLeft = 0;
    private int         nScrollHorzCell = TreeNode.MARGIN_HORZ;

	private Image       imageDraw = null;
    

    public TreeControl () {
        this ( null );
    }

    public TreeControl ( String stringRootName ) {
		super ( (LayoutManager)null );

        setLoweredBorder ();
		setBackground ( TreeNode.colorBg );
        
        this.addComponentListener ( this );
        this.addMouseListener ( this );
        this.addKeyListener ( this );
        this.addFocusListener ( this );

		if ( stringRootName != null )
            createRootElement ( stringRootName );
    }

    public boolean isFocusTraversable () {
        return ( true );
    }

    public TreeNode createRootElement ( String stringRootName ) {
        nodeRoot = new TreeNode ( stringRootName, this );
        nodeRoot.addActionListener ( this );
        vectorTreeElList = new Vector ();
        nodeRoot.addToTreeElList ( vectorTreeElList );
        nodeTop = nodeRoot;
        nodeRoot.setCurrent ( true );
        return ( nodeRoot );
    }

    public TreeNode createSubElement ( TreeNode nodeParent, String stringNodeName ) {
        TreeNode	nodeChild;

        nodeChild = nodeParent.addSubElement ( stringNodeName );
        nodeChild.addActionListener ( this );
        if ( this.isShowing() ) {
            recomputeLayout ();
            recomputeScrollbars ();
            repaint ();
        }
        return ( nodeChild );
    }

    public void destroySubElement ( TreeNode nodeParent, TreeNode node ) {
        TreeNode    nodeCurrent;

        nodeCurrent = this.getCurrentElement ();
        if ( nodeCurrent != null  &&  node.isRecursiveSubElement(nodeCurrent) )
            nodeParent.setCurrent ( true );

        nodeParent.destroySubElement ( node );
        if ( this.isShowing() ) {
            recomputeLayout ();
            recomputeScrollbars ();
            repaint ();
        }
    }

    public boolean isRootVisible () {
        return ( boolRootVisible );
    }

    public void setRootVisible ( boolean boolVisible ) {
        if ( this.boolRootVisible == boolVisible )
            return;

        boolRootVisible = boolVisible;
        nodeRoot.setVisible ( boolRootVisible );
        recomputeLayout ();
        recomputeScrollbars ();
        repaint ();
    }

    public TreeNode getRootElement () {
        return ( nodeRoot );
    }

    public TreeNode findElement ( String stringFullPath ) {
        TreeNode    node;

        node = nodeRoot.findElement ( stringFullPath );
        return ( node );
    }

    public TreeNode getCurrentElement () {
        return ( nodeRoot.getCurrent() );
    }

    public void setCurrentElement ( TreeNode node ) {
        if ( node == null )
            return;
        if ( !(nodeRoot.isRecursiveSubElement (node)) )
            return;
        node.setCurrent ( true );
    }

    public boolean SetElementImage ( TreeNode node, Image image ) {
        boolean     boolResult;

        boolResult = nodeRoot.isRecursiveSubElement ( node );
        if ( boolResult == false )
            return ( false );
        node.setImage ( image );

        if ( vectorTreeElList.contains (node) == false )
            return ( true );

        recomputeLayout ();
        recomputeScrollbars ();
        repaint ();
        return ( true );
    }

    public boolean setElementImageCur ( TreeNode node, Image image ) {
        boolean     boolResult;

        boolResult = nodeRoot.isRecursiveSubElement ( node );
        if ( boolResult == false )
            return ( false );
        node.setImageCur ( image );

        if ( vectorTreeElList.contains (node) == false )
            return ( true );

        recomputeLayout ();
        recomputeScrollbars ();
        repaint ();
        return ( true );
    }

    public Dimension getPreferredSize () {
        Dimension   dim;
        Rectangle   rect;
        Insets      insets;

        rect = nodeRoot.getNodeBoundsFull ();
        insets = getInsets ();
        dim = new Dimension ( rect.width + insets.left + insets.right,
                                rect.height + insets.top + insets.bottom );
        return ( dim );
    }

    public Rectangle getClientArea () {
        Rectangle   rect;
        Rectangle   rectScrollbar;
        Insets      insets;

        rect = new Rectangle ( getSize() );
        insets = getInsets ();
        rect.x = insets.left;
        rect.y = insets.top;
        rect.width -= insets.left + insets.right;
        rect.height -= insets.top + insets.bottom;

        if ( scrollBarVert != null  &&  scrollBarVert.isVisible() ) {
            rectScrollbar = scrollBarVert.getBounds ();
            rect.width -= rectScrollbar.width;
        }
        if ( scrollBarHorz != null  &&  scrollBarHorz.isVisible() ) {
            rectScrollbar = scrollBarHorz.getBounds ();
            rect.height -= rectScrollbar.height;
        }

        if ( rect.width < 0 )
            rect.width = 0;
        if ( rect.height < 0 )
            rect.height = 0;
        return ( rect );
    }

    public void update (Graphics graphics) {
        if ( isShowing() )
            paint ( graphics );
    }


    public void paint ( Graphics graphics ) {
        int         i;
        int         nIndexTop;
        int         nSize;
        Rectangle   rect;
        Rectangle   rectTop;
        TreeNode    node;
        Rectangle   rectClientArea;
        Dimension   dim;
        Graphics    graphicsImage;


        dim = this.getSize ();
        if ( imageDraw == null  ||  imageDraw.getWidth(this) < dim.width
                                ||  imageDraw.getHeight(this) < dim.height )
		    imageDraw = createImage ( dim.width, dim.height );
        graphicsImage = imageDraw.getGraphics ();
        graphicsImage.setColor ( this.getBackground() );
        graphicsImage.fillRect ( 0, 0, dim.width, dim.height );

        if ( boolLayoutDone == false ) {
            recomputeLayout ( graphicsImage );
            recomputeScrollbars ();
        }
        rectClientArea = getClientArea ();

        graphicsImage.setFont ( getFont() );

        nSize = vectorTreeElList.size ();
        nIndexTop = vectorTreeElList.indexOf ( nodeTop );
        rectTop = nodeTop.getNodeBounds ();

        if ( boolRootVisible == false ) {
            rect = nodeRoot.getNodeBounds ();
            Rectangle   rectFirst = nodeRoot.getSubElement(0).getNodeBounds();
            Rectangle   rectLast = nodeRoot.getSubElement(nodeRoot.size()-1).getNodeBounds();
            int nX = rectFirst.x - nScrollHorzLeft * nScrollHorzCell + TreeNode.MARGIN_HORZ + TreeNode.BOXSIZE / 2;
            int nY = rectFirst.y + rectFirst.height / 2 - rectTop.y;
            int nY2 = rectLast.y + rectLast.height / 2 - rectTop.y;
            nodeRoot.drawDottedLine ( graphicsImage, nX, nY, nX, nY2 );
        }
        for ( i = 0;  i < nSize;  i++ )  {
            node = (TreeNode) vectorTreeElList.elementAt ( i );
            node.drawElement ( graphicsImage, nScrollHorzLeft * nScrollHorzCell, rectTop.y );
            rect = node.getNodeBounds ();
            if ( rect.y + rect.height >= rectTop.y + rectClientArea.height )
                break;
        }

        super.paint ( graphicsImage );
        graphics.drawImage ( imageDraw, 0, 0, this );
	}

	public void addNotify () {
		super.addNotify ();

		addScrollbarVert ();
		addScrollbarHorz ();
	}


    public void componentResized ( ComponentEvent event ) {
        recomputeLayout ();
        recomputeScrollbars ();
    }

    public void componentMoved ( ComponentEvent event ) {
    }

    public void componentShown ( ComponentEvent event ) {
    }

    public void componentHidden ( ComponentEvent event ) {
    }

    public void mouseClicked ( MouseEvent event ) {
        Rectangle   rect;
        Point       point;

        this.requestFocus ();
        if ( event.getClickCount() % 2 == 1 )
            return;

        rect = nodeTop.getNodeBounds ();
        point = new Point ( event.getPoint() );
        point.y += rect.y;
        point.x += nScrollHorzLeft * nScrollHorzCell;

        nodeRoot.onMouseDoubleclick ( point );
    }

    public void mousePressed ( MouseEvent event ) {
        Rectangle   rect;
        Point       point;
        int         nModifiers;

        rect = nodeTop.getNodeBounds ();
        point = new Point ( event.getPoint() );
        point.y += rect.y;
        point.x += nScrollHorzLeft * nScrollHorzCell;

        nModifiers = event.getModifiers ();
        if ( (nModifiers & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK )
            nodeRoot.onMousePressedLeft ( point, event.isShiftDown(),
                                    event.isControlDown(), event.isAltDown() );
        else if ( (nModifiers & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK )
            nodeRoot.onMousePressedRight ( point, event.isShiftDown(),
                                    event.isControlDown(), event.isAltDown() );
    }

    public void mouseReleased ( MouseEvent event ) {
    }

    public void mouseEntered ( MouseEvent event ) {
    }

    public void mouseExited ( MouseEvent event ) {
    }

    public void keyPressed ( KeyEvent event ) {
        char    cKeyChar;
        int     nKeyCode;
        int     nModifiers;
        int     nIndexCurrent;
        int     nIndexCurrentNew;

        cKeyChar = event.getKeyChar ();
        nKeyCode = event.getKeyCode ();
        nModifiers = event.getModifiers ();

        if ( nKeyCode == KeyEvent.VK_ADD ) {
            nodeCurrent.setExpanded ( true );
            return;
        }
        else if ( nKeyCode == KeyEvent.VK_SUBTRACT ) {
            nodeCurrent.setExpanded ( false );
            return;
        }

        nIndexCurrent = vectorTreeElList.indexOf ( nodeCurrent );
        nIndexCurrentNew = nIndexCurrent;

        if ( nKeyCode == KeyEvent.VK_DOWN )
            nIndexCurrentNew++;
        else if ( nKeyCode == KeyEvent.VK_PAGE_DOWN )
            nIndexCurrentNew += nScrollVertVisible;
        else if ( nKeyCode == KeyEvent.VK_UP )
            nIndexCurrentNew--;
        else if ( nKeyCode == KeyEvent.VK_PAGE_UP )
            nIndexCurrentNew -= nScrollVertVisible;
        else if ( nKeyCode == KeyEvent.VK_END )
            nIndexCurrentNew = vectorTreeElList.size () - 1;
        else if ( nKeyCode == KeyEvent.VK_HOME )
            nIndexCurrentNew = 0;

        if ( nIndexCurrentNew > vectorTreeElList.size () - 1 )
            nIndexCurrentNew = vectorTreeElList.size () - 1;
        if ( nIndexCurrentNew < 0 )
            nIndexCurrentNew = 0;

        if ( nIndexCurrentNew != nIndexCurrent ) {
            nodeCurrent = (TreeNode) vectorTreeElList.elementAt ( nIndexCurrentNew );
            nodeCurrent.setCurrent ( true );
        }
    }

    public void keyReleased ( KeyEvent event ) {
    }

    public void keyTyped ( KeyEvent event ) {
    }

    public void focusGained ( FocusEvent event ) {
        repaint ();
    }

    public void focusLost ( FocusEvent event ) {
        repaint ();
    }

    public void adjustmentValueChanged ( AdjustmentEvent event ) {
        Adjustable      adjustable;
        int             nType;
        int             nValue;
        int             nIndexTop;

        adjustable = event.getAdjustable ();
        if ( adjustable == null  ||  !(adjustable instanceof Scrollbar) )
            return;

        nType = event.getAdjustmentType ();
        nValue = event.getValue ();
        if ( nValue != adjustable.getValue() )
            adjustable.setValue ( nValue );

        if ( ((Scrollbar)adjustable).getOrientation() == Scrollbar.VERTICAL ) {
            nIndexTop = vectorTreeElList.indexOf ( nodeTop );
            if ( nIndexTop != nValue ) {
                nIndexTop = nValue;
                nodeTop = (TreeNode) vectorTreeElList.elementAt ( nIndexTop );
                recomputeScrollbars ();
                repaint ();
            }
        }
        else { // if ( ((Scrollbar)adjustable).getOrientation() == Scrollbar.HORIZONTAL )
            if ( nScrollHorzLeft != nValue ) {
                nScrollHorzLeft = nValue;
                repaint ();
            }
        }
    }

    public void actionPerformed ( ActionEvent event ) {
        Object  objSource;
        String  strAction;

        objSource = event.getSource ();
        if ( objSource == null  ||  !(objSource instanceof TreeNode) )
            return;

        strAction = event.getActionCommand ();
        if ( strAction.equals(TreeNode.ACTION_NODE_ADDED) ) {
            recomputeTreeElList ();
            if ( vectorTreeElList.contains (objSource) == true ) {
                recomputeLayout ();
                recomputeScrollbars ();
            }
            repaint ();
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_REMOVED) ) {
            if ( vectorTreeElList.contains (objSource) == true ) {
                recomputeTreeElList ();
                recomputeLayout ();
                recomputeScrollbars ();
            }
            repaint ();
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_EXPANDED) ) {
            recomputeTreeElList ();
            recomputeLayout ();
            recomputeScrollbars ();
            repaint ();
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_COLLAPSED) ) {
            recomputeTreeElList ();
            recomputeLayout ();
            recomputeScrollbars ();
            repaint ();
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_SETCURRENT) ) {
            nodeCurrent = (TreeNode)objSource;
            scrollCurrentIntoView ();
            repaint ();
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_RESETCURRENT) ) {
        }
    }

    private void recomputeTreeElList () {
        boolean     boolResult;
        TreeNode    nodeCurrentOld;


        if ( nodeRoot == null )
            return;
			
        vectorTreeElList.removeAllElements ();
        nodeRoot.addToTreeElList ( vectorTreeElList );

        while ( nodeTop != null ) {
            boolResult = vectorTreeElList.contains ( nodeTop );
            if ( boolResult == true )
                break;
            nodeTop = (TreeNode) nodeTop.getOwner ();
        }
        if ( nodeTop == null  &&  vectorTreeElList.size() > 0 )
            nodeTop = (TreeNode) vectorTreeElList.elementAt ( 0 );
        if ( nodeTop == null )
            nodeTop = nodeRoot;

        nodeCurrentOld = nodeCurrent;
        while ( nodeCurrent != null ) {
            boolResult = vectorTreeElList.contains ( nodeCurrent );
            if ( boolResult == true )
                break;
            nodeCurrent = (TreeNode) nodeCurrent.getOwner ();
        }
        if ( nodeCurrent == null  &&  vectorTreeElList.size() > 0 )
            nodeCurrent = (TreeNode) vectorTreeElList.elementAt ( 0 );
        if ( nodeCurrent == null )
            nodeCurrent = nodeRoot;

        if ( nodeCurrentOld != nodeCurrent ) {
            if ( nodeCurrentOld != null )
                nodeCurrentOld.setCurrent ( false );
            if ( nodeCurrent != null )
                nodeCurrent.setCurrent ( true );
        }
    }

    private void recomputeLayout () {
        Graphics    graphics;

        graphics = getGraphics ();
        if ( graphics != null )
            recomputeLayout ( graphics );
    }

    private void recomputeLayout ( Graphics graphics ) {
        int             nIndexTop;
        Rectangle       rect;
        Rectangle       rectTop;
        Rectangle       rectClientArea;
        Font            font;
        FontMetrics     fontMetrics;


        font = this.getFont ();
        fontMetrics = graphics.getFontMetrics ( font );
        rect = new Rectangle ( 0, 0, 0, 0 );
        rect = nodeRoot.recomputeLayout ( rect, fontMetrics );

        rectClientArea = this.getClientArea ();
        nIndexTop = vectorTreeElList.indexOf ( nodeTop );
        rectTop = nodeTop.getNodeBounds ();
        while ( nIndexTop > 0   &&  rectClientArea.height > rect.height - (rectTop.y - rect.y) ) {
            nIndexTop--;
            nodeTop = (TreeNode) vectorTreeElList.elementAt ( nIndexTop );
            rectTop = nodeTop.getNodeBounds ();
        }
        boolLayoutDone = true;
    }

    public void addScrollbarVert () {
        scrollBarVert = new Scrollbar ( Scrollbar.VERTICAL );
        scrollBarVert.addAdjustmentListener ( this );
        scrollBarVert.setUnitIncrement ( 1 );
        add ( scrollBarVert );
    }

    public void addScrollbarHorz () {
        scrollBarHorz = new Scrollbar ( Scrollbar.HORIZONTAL );
        scrollBarHorz.addAdjustmentListener ( this );
        scrollBarHorz.setUnitIncrement ( 1 );
        add ( scrollBarHorz );
    }

    public void removeScrollbarVert () {
        scrollBarVert.removeAdjustmentListener ( this );
        remove ( scrollBarVert );
        scrollBarVert = null;
    }

    public void removeScrollbarHorz () {
        scrollBarHorz.removeAdjustmentListener ( this );
        remove ( scrollBarHorz );
        scrollBarHorz = null;
    }

    private void recomputeScrollbars () {
        int         i;
        int         nIndexTop;
        int         nSize;
        Rectangle   rect;
        Rectangle   rectTop;
        Rectangle   rectClientArea;
        TreeNode    node;

        if ( boolLayoutDone == false )
            return;

        nSize = vectorTreeElList.size ();
        nIndexTop = vectorTreeElList.indexOf ( nodeTop );
        rectTop = nodeTop.getNodeBounds ();

        rectClientArea = getClientArea ();
        for ( i = nIndexTop;  i < nSize;  i++ ) {
            node = (TreeNode) vectorTreeElList.elementAt ( i );
            rect = node.getNodeBounds ();
            if ( rect.y + rect.height - rectTop.y > rectClientArea.height )
                break;
        }
        nScrollVertVisible = i - nIndexTop;
        setScrollbarVertValues ( nIndexTop, nScrollVertVisible, 0, nSize );

        rect = nodeRoot.getNodeBoundsFull ();
        nScrollHorzMax = (rect.width + nScrollHorzCell - 1) / nScrollHorzCell;
        nScrollHorzVisible = rectClientArea.width / nScrollHorzCell;
        setScrollbarHorzValues ( nScrollHorzLeft, nScrollHorzVisible, 0, nScrollHorzMax );

        rect = getClientArea ();
        if ( !(rectClientArea.equals(rect)) ) {
            recomputeScrollbars ();
        }
    }

    private void scrollCurrentIntoView () {
        int         nSize;
        int         nIndexCurrent;
        int         nIndexTop;
        int         nIndexTopNew;
        Rectangle   rectCurrent;
        Rectangle   rectTop;
        Rectangle   rectClientArea;


        if ( boolLayoutDone == false )
            return;

        nSize = vectorTreeElList.size ();
        nIndexCurrent = vectorTreeElList.indexOf ( nodeCurrent );
        if ( nIndexCurrent < 0 )
            return;
			
        nIndexTop = vectorTreeElList.indexOf ( nodeTop );
        nIndexTopNew = nIndexTop;

        if ( nIndexCurrent >= nIndexTopNew + nScrollVertVisible ) {
            rectClientArea = getClientArea ();
            rectCurrent = nodeCurrent.getNodeBounds ();
            while ( nIndexTopNew < nSize - 1 ) {
                nIndexTopNew++;
                nodeTop = (TreeNode) vectorTreeElList.elementAt ( nIndexTopNew );
                rectTop = nodeTop.getNodeBounds ();
                if ( rectCurrent.y + rectCurrent.height - rectTop.y > rectClientArea.height );
                    break;
            }
        }
        if ( nIndexCurrent < nIndexTopNew ) {
            nIndexTopNew = nIndexCurrent;
            nodeTop = (TreeNode) vectorTreeElList.elementAt ( nIndexTopNew );
        }

        if ( nIndexTopNew != nIndexTop )
            recomputeScrollbars ();
	}

    public boolean setScrollbarVertValues ( int nValue, int nVisibleCount, int nMinValue, int nMaxValue ) {
        boolean     boolResult;
        boolean     boolVisible;

		
        if ( scrollBarVert == null )
            return ( false );

        boolResult = scrollBarVert.isVisible ();
        scrollBarVert.setValues ( nValue, nVisibleCount, nMinValue, nMaxValue );
        if ( nVisibleCount - 1 < 1 )
            scrollBarVert.setBlockIncrement ( 1 );
        else
            scrollBarVert.setBlockIncrement ( nVisibleCount - 1 );

        if ( nMaxValue - nMinValue > nVisibleCount )
            boolVisible = true;
        else
            boolVisible = false;

        scrollBarVert.setVisible ( boolVisible );
        positionScrollbars ();

        scrollBarVert.setValues ( nValue, nVisibleCount, nMinValue, nMaxValue );
        if ( nVisibleCount - 1 < 1 )
            scrollBarVert.setBlockIncrement ( 1 );
        else
            scrollBarVert.setBlockIncrement ( nVisibleCount - 1 );

        return ( boolResult != boolVisible );
    }

    public boolean setScrollbarHorzValues ( int nValue, int nVisibleCount, int nMinValue, int nMaxValue ) {
        boolean     boolResult;
        boolean     boolVisible;


        if ( scrollBarHorz == null )
            return ( false );

        boolResult = scrollBarHorz.isVisible ();
        scrollBarHorz.setValues ( nValue, nVisibleCount, nMinValue, nMaxValue );
        if ( nVisibleCount - 1 < 1 )
            scrollBarHorz.setBlockIncrement ( 1 );
        else
            scrollBarHorz.setBlockIncrement ( nVisibleCount - 1 );

        if ( nMaxValue - nMinValue > nVisibleCount )
            boolVisible = true;
        else
            boolVisible = false;

        scrollBarHorz.setVisible ( boolVisible );
        positionScrollbars ();

        scrollBarHorz.setValues ( nValue, nVisibleCount, nMinValue, nMaxValue );
        if ( nVisibleCount - 1 < 1 )
            scrollBarHorz.setBlockIncrement ( 1 );
        else
            scrollBarHorz.setBlockIncrement ( nVisibleCount - 1 );

        return ( boolResult != boolVisible );
    }

    private void positionScrollbars () {
        Dimension   dimVert;
        Dimension   dimHorz;
        Rectangle   rect;
        Insets      insets;

        rect = getBounds ();
        insets = this.getInsets ();
        rect.x = insets.left;
        rect.y = insets.top;
        rect.width = rect.width - insets.left - insets.right;
        rect.height = rect.height - insets.top - insets.bottom;

        if ( scrollBarVert != null  &&  scrollBarVert.isVisible() ) {
            dimVert = scrollBarVert.getPreferredSize ();
            dimVert.width += 2;
        }
        else {
            dimVert = new Dimension ( 0, 0 );
        }

        if ( scrollBarHorz != null  &&  scrollBarHorz.isVisible() ) {
            dimHorz = scrollBarHorz.getPreferredSize ();
            dimHorz.height += 2;
        }
        else {
            dimHorz = new Dimension ( 0, 0 );
        }

        if ( scrollBarVert != null )
            scrollBarVert.setBounds ( rect.x + rect.width - dimVert.width, rect.y, dimVert.width, rect.height - dimHorz.height );
        if ( scrollBarHorz != null )
            scrollBarHorz.setBounds ( rect.x, rect.y + rect.height - dimHorz.height, rect.width - dimVert.width, dimHorz.height );
    }

}


