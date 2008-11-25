/*
 * @(#)TreeNode.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;


public class TreeNode extends Vector {

    public static final int     MARGIN_HORZ = 6;
    public static final int     MARGIN_VERT = 2;
    public static final int     BOXSIZE = 8;

    public static final String  ACTION_NODE_ADDED = "Node Added";
    public static final String  ACTION_NODE_REMOVED = "Node Removed";
    public static final String  ACTION_NODE_EXPANDED = "Node Expanded";
    public static final String  ACTION_NODE_COLLAPSED = "Node Collapsed";
    public static final String  ACTION_NODE_SETCURRENT = "Node Set Current";
    public static final String  ACTION_NODE_RESETCURRENT = "Node Reset Current";

    public static final Color   colorBg = Color.lightGray;
    public static final Color   colorCurBg = new Color ( 0, 0, 128 );
    public static final Color   colorFg = Color.black;
    public static final Color   colorCurFg = Color.yellow;
    public static final Color   colorLines = Color.darkGray;

    private Component           componentOwner;
    private Vector              vectorActionListeners = new Vector ();

    private boolean     boolExpanded = false;
    private boolean     boolCurrent = false;
    private boolean     boolVisible = true;
    private Image       imageTreeElement = null;
    private Image       imageTreeElementCur = null;
    private int         nWidthImage = 0;
    private int         nHeightImage = 0;
    private Rectangle   rectElement = null;
    private Rectangle   rectElementFull = null;

    private String      strName;
    private Object      objUserData = null;
    private TreeNode    nodeOwner;


    public TreeNode ( String stringName, Component clWindowOwner ) {
        super ();

        this.strName = new String ( stringName );
        this.componentOwner = clWindowOwner;
        rectElement = new Rectangle ( 0, 0, 0, 0 );
        rectElementFull = new Rectangle ( 0, 0, 0, 0 );

        setImage ( ImageArea.loadImage ( "treeNode.gif", clWindowOwner, true ) );
        setImageCur ( ImageArea.loadImage ( "treeNodeCur.gif", clWindowOwner, true ) );
	}

    public void addActionListener ( ActionListener listener ) {
        if ( listener == null )
            return;
        vectorActionListeners.addElement ( listener );
    }

    public void rewmoveActionListener ( ActionListener listener ) {
        if ( listener == null )
            return;
        vectorActionListeners.removeElement ( listener );
    }

    public String getName () {
        return ( strName );
    }

    public String getFullPathName () {
        String  stringName;

        if ( nodeOwner != null )
            stringName = nodeOwner.getFullPathName ();
        else
            stringName = new String ();
        stringName += "/" + strName;
        return ( stringName );
    }

    public TreeNode getOwner () {
        return ( nodeOwner );
    }

    public TreeNode getRoot () {
        TreeNode    nodeRoot;

        nodeRoot = this;
        while ( nodeRoot.getOwner() != null )
            nodeRoot = nodeRoot.getOwner ();
        return ( nodeRoot );
    }

    public Object getUserData () {
        return ( objUserData );
    }

    public TreeNode getSubElement ( int nIndex ) {
        int         nSize;
        TreeNode    node;

        nSize = size ();
        if ( nIndex >= nSize  ||  nIndex < 0 )
            return ( null );
        node = (TreeNode) elementAt ( nIndex );
        return ( node );
    }

    public TreeNode getSubElement ( String stringName ) {
        int         i;
        int         nSize;
        TreeNode    node;
        String      stringElementName = null;


        nSize = size ();
        for ( i = 0;  i < nSize;  i++ ) {
            node = (TreeNode) elementAt ( i );
            stringElementName = node.getName ();
            if ( stringElementName.equals (stringName) )
                return ( node );
        }
        return ( null );
    }

    public boolean isSubElement ( String stringName ) {
        int         i;
        int         nSize;
        TreeNode    node;
        String      stringElementName = null;


        nSize = size ();
        for ( i = 0;  i < nSize;  i++ ) {
            node = (TreeNode) elementAt ( i );
            stringElementName = node.getName ();
            if ( stringElementName.equals(stringName) )
                return ( true );
        }
        return ( false );
    }

    public TreeNode findElement ( String stringFullPath ) {
        int         i;
        int         nSize;
        TreeNode    node = null;


        if ( stringFullPath.equals("/" + strName) )
            return ( this );
        if ( !(stringFullPath.startsWith("/"+strName)) )
            return ( null );

        stringFullPath = stringFullPath.substring ( strName.length() + 1 );
        nSize = size ();
        for ( i = 0;  i < nSize  &&  node == null;  i++ ) {
            node = (TreeNode) elementAt ( i );
            node = node.findElement ( stringFullPath );
        }
        return ( node );
    }

    public TreeNode addSubElement ( TreeNode node ) {
        ActionEvent     event;

        if ( node.nodeOwner != null )
            node.nodeOwner.removeSubElement ( node );
        addElement ( node );
        node.nodeOwner = this;

        event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_NODE_ADDED );
        fireActionEvent ( event );

        return ( node );
    }

    public TreeNode addSubElement ( String stringName ) {
        TreeNode    node;

        node = new TreeNode ( stringName, componentOwner );
        addSubElement ( node );
        return ( node );
    }

    public TreeNode insertSubElement ( int nIndex, TreeNode node ) {
        int     nSize;

        if ( node.nodeOwner != null )
            node.nodeOwner.removeSubElement ( node );

        nSize = size ();
        if ( nIndex < 0 )
            nIndex = 0;
        if ( nIndex > nSize )
            nIndex = nSize;
        insertElementAt ( node, nIndex );
        node.nodeOwner = this;
        return ( node );
    }

    public TreeNode insertSubElement ( int nIndex, String stringName ) {
        TreeNode    node;

        node = new TreeNode ( stringName, componentOwner );
        insertSubElement ( nIndex, node );
        return ( node );
    }

    public void removeSubElement ( TreeNode node ) {
        boolean         boolResult;
        ActionEvent     event;

        boolResult = removeElement ( node );
        if ( boolResult == true ) {
            node.nodeOwner = null;
            event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_NODE_REMOVED );
            fireActionEvent ( event );
        }
    }

    public void destroySubElement ( TreeNode node ) {
        node.destroyAllSubElements ();
        node.nodeOwner = null;
        removeSubElement ( node );
    }

    public void destroySubElement ( int nIndex ) {
        int         nSize;
        TreeNode    node;

        nSize = size ();
        if ( nIndex >= nSize  ||  nIndex < 0 )
            return;
        node = (TreeNode) elementAt ( nIndex );
        destroySubElement ( node );
    }

    public void destroyAllSubElements () {
        int         nSize;
        TreeNode    node;

        nSize = size ();
        while ( nSize > 0 ) {
            nSize--;
            destroySubElement ( nSize );
        }
    }

    public void sortSubElements () {
        int         i, j;
        int         nSize;
        TreeNode    node1;
        TreeNode    node2;
        String      stringName1;
        String      stringName2;
        int         nResult;


        nSize = size ();
        for ( i = 0;  i < nSize;  i++ ) {
            node1 = (TreeNode) elementAt ( i );
            stringName1 = node1.getName ();
            removeElementAt ( i );
            for ( j = 0;  j < i;  j++ ) {
                node2 = (TreeNode) elementAt ( j );
                stringName2 = node2.getName ();
                nResult = stringName1.compareTo ( stringName2 );
                if ( nResult < 0 )
                    break;
            }
            insertElementAt ( node1, j );
            node1.sortSubElements ();
        }
    }

    public int getImmediateSubElementsCount () {
        int	nSize;

        nSize = size ();
        return ( nSize );
    }

    public int getRecursiveSubElementsCount () {
        int     i;
        int     nCount;
        int     nSize;
        Object  object;

        nSize = size ();
        nCount = nSize;
        for ( i = 0;  i < nSize;  i++ ) {
            object = elementAt ( i );
            if ( object != null  &&  object instanceof TreeNode )
                nCount += ((TreeNode)object).getRecursiveSubElementsCount ();
        }
        return ( nCount );
    }

    public int getExpandedSubElementsCount () {
        int     i;
        int     nCount;
        int     nSize;
        Object  object;

        nSize = size ();
        nCount = nSize;
        for ( i = 0;  i < nSize;  i++ ) {
            object = elementAt ( i );
            if ( object != null  &&  object instanceof TreeNode  &&  ((TreeNode)object).isExpanded() )
                nCount += ((TreeNode)object).getExpandedSubElementsCount ();
        }
        return ( nCount );
    }

    public boolean isRecursiveSubElement ( Object object ) {
        boolean     boolResult = false;
        int         i;
        int         nSize;
        Object      objectSub;

        if ( object == this )
            return ( true );
        nSize = size ();
        for ( i = 0;  i < nSize  &&  boolResult == false;  i++ ) {
            objectSub = elementAt ( i );
            if ( object == objectSub )
                return ( true );
            if ( objectSub != null  &&  objectSub instanceof TreeNode )
                boolResult = ((TreeNode)objectSub).isRecursiveSubElement ( object );
        }
        return ( boolResult );
	}

    public void setUserData ( Object objectData ) {
        this.objUserData = objectData;
    }

    public boolean isVisible () {
        return ( boolVisible );
    }

    public void setVisible ( boolean boolVisible ) {
        if ( this.boolVisible == boolVisible )
			return;

        this.boolVisible = boolVisible;
        if ( boolVisible == false  &&  boolExpanded == false )
            switchExpanded ();
    }

    public boolean isExpanded () {
        return ( boolExpanded );
    }

    public void setExpanded ( boolean boolExpanded ) {
        if ( this.boolExpanded != boolExpanded )
            switchExpanded ();
    }

    public boolean switchExpanded () {
        ActionEvent     event;

        if ( boolExpanded == true ) {
            boolExpanded = false;
            event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_NODE_COLLAPSED );
        }
        else {
            boolExpanded = true;
            event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_NODE_EXPANDED );
        }
        fireActionEvent ( event );
        return ( boolExpanded );
    }

    public void setCurrent ( boolean boolCurrent ) {
        TreeNode	node;
        ActionEvent event;

        if ( this.boolCurrent == boolCurrent )
            return;

        if ( boolCurrent == true ) {
            node = (TreeNode) getRoot ();
            node = node.getCurrent ();
            if ( node != null )
                node.setCurrent ( false );

            node = (TreeNode) nodeOwner;
            while ( node != null  &&  node instanceof TreeNode ) {
                if ( node.isExpanded() == false )
                    node.setExpanded ( true );
                node = (TreeNode) node.nodeOwner;
            }
            event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_NODE_SETCURRENT );
        }
        else {
            event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_NODE_RESETCURRENT );
        }

        this.boolCurrent = boolCurrent;
        computeImageSize ();
        fireActionEvent ( event );
    }

    public TreeNode getCurrent () {
        int         i;
        int         nSize;
        TreeNode    node;
        TreeNode    nodeCurrent = null;

        if ( boolCurrent == true )
            return ( this );
        nSize = getImmediateSubElementsCount ();
        for ( i = 0;  i < nSize  &&  nodeCurrent == null;  i++ ) {
            node = (TreeNode) getSubElement ( i );
            nodeCurrent = node.getCurrent ();
        }
        return ( nodeCurrent );
    }

    public void setImage ( Image imageNew ) {
        imageTreeElement = imageNew;
        computeImageSize ();
    }

    public void setImageCur ( Image imageNew ) {
        imageTreeElementCur = imageNew;
        computeImageSize ();
    }

    public Rectangle getNodeBounds () {
        return ( new Rectangle(rectElement) );
    }

    public Rectangle getNodeBoundsFull () {
        return ( new Rectangle(rectElementFull) );
    }

    public void onMouseDoubleclick ( Point pointMouse ) {
        int         i;
        int         nSize;
        TreeNode    node;

        nSize = getImmediateSubElementsCount ();
        if ( !(rectElementFull.contains (pointMouse)) )
            return;
        if ( rectElement.contains (pointMouse)
                    &&  pointMouse.x >= rectElement.x + 2 * MARGIN_HORZ + BOXSIZE
                    &&  nSize > 0 ) {
            switchExpanded ();
            return;
        }
        if ( boolExpanded == false )
            return;

        for ( i = 0;  i < nSize;  i++ ) {
            node = (TreeNode) getSubElement ( i );
            node.onMouseDoubleclick ( pointMouse );
        }
    }

    public void onMousePressedLeft ( Point pointMouse, boolean boolShift, boolean boolCtrl, boolean boolAlt ) {
        int         i;
        int         nSize;
        TreeNode    node;

        nSize = getImmediateSubElementsCount ();
        if ( !(rectElementFull.contains (pointMouse)) )
            return;
        if ( rectElement.contains (pointMouse) ) {
            if ( pointMouse.x >= rectElement.x + 2 * MARGIN_HORZ + BOXSIZE )
                setCurrent ( true );
            else if ( nSize > 0 )
                switchExpanded ();
            return;
        }
        if ( boolExpanded == false )
            return;

        for ( i = 0;  i < nSize;  i++ ) {
            node = (TreeNode) getSubElement ( i );
            node.onMousePressedLeft ( pointMouse, boolShift, boolCtrl, boolAlt );
        }
    }

    public void onMousePressedRight ( Point pointMouse, boolean boolShift, boolean boolCtrl, boolean boolAlt ) {
    }

    public void drawElement ( Graphics graphics, int nOffsetX, int nOffsetY ) {
        int             nX;
        int             nY;
        int             nWidth;
        int             nHeight;
        int             nSize;
        Rectangle       rect;
        Rectangle       rectLastSubElement;
        FontMetrics     fontMetrics;
        TreeNode        node;
        Image           imageDraw;



        nSize = getImmediateSubElementsCount ();
        rect = new Rectangle ( rectElement );
        rect.x -= nOffsetX;
        rect.y -= nOffsetY;
        fontMetrics = graphics.getFontMetrics ();

        graphics.setColor ( colorLines );

        if ( nSize > 0 ) {
            nX = rect.x + MARGIN_HORZ;
            nY = rect.y + (rect.height - BOXSIZE) / 2;
            nWidth = BOXSIZE;
            nHeight = BOXSIZE;
            graphics.setColor ( colorBg );
            graphics.fillRect ( nX, nY, nWidth, nHeight );
            graphics.setColor ( colorLines );
            graphics.drawRect ( nX, nY, nWidth, nHeight );

            nX = rect.x + MARGIN_HORZ + BOXSIZE;
            nY = rect.y + rect.height / 2;
            drawDottedLine ( graphics, nX, nY, nX + MARGIN_HORZ, nY );
        }
        else {
            nX = rect.x + MARGIN_HORZ + BOXSIZE / 2;
            nY = rect.y + rect.height / 2;
            drawDottedLine ( graphics, nX, nY, nX + MARGIN_HORZ, nY );
        }

        if ( nSize > 0  &&  boolExpanded == true ) {
            node = (TreeNode) getSubElement ( nSize - 1 );
            rectLastSubElement = node.getNodeBounds ();

            nX = rect.x + 2 * MARGIN_HORZ + BOXSIZE + nWidthImage / 2;
            nY = rect.y + (rect.height - nHeightImage) / 2 + nHeightImage + 1;
            nHeight = rectLastSubElement.y - nOffsetY - nY + rectLastSubElement.height / 2;
            drawDottedLine ( graphics, nX, nY, nX, nY + nHeight );
        }

        graphics.setColor ( colorFg );

        if ( nSize > 0 ) {
            nX = rect.x + MARGIN_HORZ + 2;
            nY = rect.y + rect.height / 2;
            nWidth = BOXSIZE - 2 * 2;
            graphics.drawLine ( nX, nY, nX + nWidth, nY );
            if ( boolExpanded == false ) {
                nX = rect.x + MARGIN_HORZ + BOXSIZE / 2;
                nY = rect.y + (rect.height - BOXSIZE) / 2 + 2;
                nHeight = BOXSIZE - 2 * 2;
                graphics.drawLine ( nX, nY, nX, nY + nHeight );
            }
        }

        nX = rect.x + 2 * MARGIN_HORZ + BOXSIZE;
        nY = rect.y + (rect.height - nHeightImage) / 2;
		
        if ( boolCurrent == true )
            imageDraw = imageTreeElementCur;
        else
            imageDraw = imageTreeElement;

        if ( imageDraw != null )
            graphics.drawImage ( imageDraw, nX, nY, colorBg, componentOwner );

        if ( nWidthImage < 0  ||  nHeightImage < 0 ) {
            nWidthImage = imageDraw.getWidth ( componentOwner );
            nHeightImage = imageDraw.getHeight ( componentOwner );
        }

        nX = rect.x + 3 * MARGIN_HORZ + BOXSIZE + nWidthImage - MARGIN_HORZ / 2;
        nY = rect.y + 2;
        nWidth = fontMetrics.stringWidth ( strName ) + MARGIN_HORZ;
        nHeight = rect.height - 3;
        if ( boolCurrent == true ) {
            graphics.setColor ( colorCurBg );
            graphics.fillRect ( nX, nY, nWidth, nHeight );
            graphics.setColor ( colorCurFg );
        }
        else {
            graphics.setColor ( colorFg );
        }

        if ( boolCurrent == true  &&  hasOwnerFocus() )
            drawDottedRectangle ( graphics, nX, nY, nWidth, nHeight );

        nHeight = fontMetrics.getAscent ();
        nX = rect.x + 3 * MARGIN_HORZ + BOXSIZE + nWidthImage;
        nY = rect.y + (rect.height - nHeight) / 2 + nHeight;
        graphics.drawString ( strName, nX, nY );
    }

    public void drawDottedRectangle ( Graphics graphics, int nX, int nY, int nWidth, int nHeight ) {
        drawDottedLine ( graphics, nX, nY, nX + nWidth - 1, nY );
        drawDottedLine ( graphics, nX + nWidth - 1, nY, nX + nWidth - 1, nY + nHeight - 1 );
        drawDottedLine ( graphics, nX + nWidth - 1, nY + nHeight - 1, nX, nY + nHeight - 1 );
        drawDottedLine ( graphics, nX, nY + nHeight - 1, nX, nY );
    }

    public void drawDottedLine ( Graphics graphics, int nX1, int nY1, int nX2, int nY2 ) {
        int     nX, nY;
        double  dDiv;

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

    public void drawDot ( Graphics graphics, int nX, int nY ) {
        if ( (nX + nY) % 2 == 0 )
            graphics.drawLine ( nX, nY, nX, nY );
    }


    public void computeImageSize () {
        if ( boolCurrent == false  &&  imageTreeElement != null ) {
            nWidthImage = imageTreeElement.getWidth ( componentOwner );
            nHeightImage = imageTreeElement.getHeight ( componentOwner );
        }
        else if ( boolCurrent == true  &&  imageTreeElementCur != null ) {
            nWidthImage = imageTreeElementCur.getWidth ( componentOwner );
            nHeightImage = imageTreeElementCur.getHeight ( componentOwner );
        }
        else {
            nWidthImage = 0;
            nHeightImage = 0;
        }
    }

    public Rectangle recomputeLayout ( Rectangle rect, FontMetrics fontMetrics ) {
        int         i;
        int         nSize;
        int         nOffsetX;
        int         nOffsetY;
        int         nHeight;
        TreeNode    node;
        Rectangle   rectSubElement;


        computeImageSize ();

        rectElement.x = rect.x;
        rectElement.y = rect.y;

        if ( boolVisible == true ) {
            rectElement.height = Math.max ( fontMetrics.getHeight(), BOXSIZE + 2 * MARGIN_VERT );
            rectElement.height = Math.max ( rectElement.height, nHeightImage + 2 * MARGIN_VERT );
            rectElement.width = BOXSIZE + nWidthImage + fontMetrics.stringWidth(strName) + 4 * MARGIN_HORZ;
        }
        else {
            rectElement.height = 0;
            rectElement.width = 0;
        }
        rectElementFull.setBounds ( rectElement );

        if ( boolExpanded == true ) {
            if ( boolVisible == true )
                nOffsetX = BOXSIZE / 2 + MARGIN_HORZ + nWidthImage / 2;
            else
                nOffsetX = 0;
            nOffsetY = rectElement.height;

            rectSubElement = new Rectangle ( rect );
            rectSubElement.x += nOffsetX;
            rectSubElement.y += nOffsetY;

            nSize = getImmediateSubElementsCount ();
            for ( i = 0;  i < nSize;  i++ ) {
                node = (TreeNode) getSubElement ( i );
                rectSubElement = node.recomputeLayout ( rectSubElement, fontMetrics );
                rectElementFull.height += rectSubElement.height;
                rectSubElement.y += rectSubElement.height;
                if ( rectElementFull.width < nOffsetX + rectSubElement.width )
                    rectElementFull.width = nOffsetX + rectSubElement.width;
            }
        }
        return ( new Rectangle(rectElementFull) );
    }

    public void addToTreeElList ( Vector cw_vectorTreeElList ) {
        int         i;
        int         nSize;
        TreeNode    node;

        if ( boolVisible == true )
            cw_vectorTreeElList.addElement ( this );

        if ( boolExpanded == false )
            return;

        nSize = getImmediateSubElementsCount ();
        for ( i = 0;  i < nSize;  i++ ) {
            node = (TreeNode) getSubElement ( i );
            node.addToTreeElList ( cw_vectorTreeElList );
        }
    }

    private boolean hasOwnerFocus () {
        boolean     boolResult = false;
        Component   component;
        Window      window;

        if ( componentOwner == null )
            return ( false );

        component = componentOwner;
        while ( component != null ) {
            if ( component instanceof Window ) {
                boolResult = (((Window)component).getFocusOwner() == componentOwner);
                break;
            }
            component = component.getParent ();
        }
        return ( boolResult );
    }

    private void fireActionEvent ( ActionEvent event ) {
        int             i;
        int             nCount;
        ActionListener  listener;

        nCount = vectorActionListeners.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            listener = (ActionListener) vectorActionListeners.elementAt ( i );
            listener.actionPerformed ( event );
        }
    }

}


