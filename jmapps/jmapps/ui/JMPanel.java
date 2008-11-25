/*
 * @(#)JMPanel.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.awt.*;


public class JMPanel extends Panel {

    private static final int TYPE_EMPTY = 1;
    private static final int TYPE_ETCHED = 2;
    private static final int TYPE_RAISED = 3;
    private static final int TYPE_LOWERED = 4;

    private Insets      insetsBorder = new Insets (0,0,0,0);
    private int         nType = TYPE_EMPTY;


    public JMPanel () {
        super ();
    }

    public JMPanel ( LayoutManager managerLayout ) {
        super ( managerLayout );
    }

    public Insets getInsets () {
        return ( insetsBorder );
    }

    public void doLayout () {
        int         i;
        int         nCount;
        Component   component;

        super.doLayout ();

        nCount = this.getComponentCount ();
        for ( i = 0;  i < nCount;  i++ ) {
            component = this.getComponent ( i );
            if ( component != null  &&  component instanceof Container )
                ((Container)component).doLayout();
        }
    }

    public void setEmptyBorder ( int nLeft, int nTop, int nRight, int nBottom ) {
        insetsBorder.left = nLeft;
        insetsBorder.top = nTop;
        insetsBorder.right = nRight;
        insetsBorder.bottom = nBottom;

        nType = TYPE_EMPTY;
    }

    public void setEtchedBorder () {
        insetsBorder.left = 8;
        insetsBorder.top = 8;
        insetsBorder.right = 8;
        insetsBorder.bottom = 8;

        nType = TYPE_ETCHED;
    }

    public void setRaisedBorder () {
        insetsBorder.left = 2;
        insetsBorder.top = 2;
        insetsBorder.right = 2;
        insetsBorder.bottom = 2;

        nType = TYPE_RAISED;
    }

    public void setLoweredBorder () {
        insetsBorder.left = 1;
        insetsBorder.top = 1;
        insetsBorder.right = 1;
        insetsBorder.bottom = 1;

        nType = TYPE_LOWERED;
    }

    public void paint ( Graphics graphics ) {
        Color           colorBg;
        Color           colorTs;
        Color           colorBs;
        Dimension       dim;

        super.paint ( graphics );

        colorBg = this.getBackground ();
        colorTs = colorBg.brighter ();
        colorBs = colorBg.darker ();

        dim = this.getSize ();

        if ( nType == TYPE_ETCHED ) {
            graphics.setColor ( colorBs );
            graphics.drawRect ( 0, 0, dim.width - 2, dim.height - 2 );
            graphics.setColor ( colorTs );
            graphics.drawRect ( 1, 1, dim.width - 2, dim.height - 2 );
        }
        else if ( nType == TYPE_RAISED ) {
            graphics.setColor ( colorTs );
            graphics.drawLine ( 0, 0, dim.width - 1, 0 );
            graphics.drawLine ( 1, 1, dim.width - 2, 1 );
            graphics.drawLine ( 0, 0, 0, dim.height - 1 );
            graphics.drawLine ( 1, 1, 1, dim.height - 2 );

            graphics.setColor ( colorBs );
            graphics.drawLine ( 2, dim.height - 2, dim.width - 1, dim.height - 2 );
            graphics.drawLine ( 1, dim.height - 1, dim.width - 2, dim.height - 1 );
            graphics.drawLine ( dim.width - 2, 2, dim.width - 2, dim.height - 2 );
            graphics.drawLine ( dim.width - 1, 1, dim.width - 1, dim.height - 1 );
        }
        else if ( nType == TYPE_LOWERED ) {
            graphics.setColor ( colorBs );
            graphics.drawLine ( 0, 0, dim.width - 1, 0 );
            graphics.drawLine ( 0, 0, 0, dim.height - 1 );

            graphics.setColor ( colorTs );
            graphics.drawLine ( 1, dim.height - 1, dim.width - 2, dim.height - 1 );
            graphics.drawLine ( dim.width - 1, 1, dim.width - 1, dim.height - 1 );
        }
    }

    protected Frame getFrame() {
    	Frame		frame = null;
    	Component	comp;

    	comp = this;
    	while ( comp != null ) {
    	    if ( comp instanceof Frame ) {
    	    	frame = (Frame) comp;
    	    	break;
    	    }
    	    comp = comp.getParent ();
    	}
    	return ( frame );
    }


}


