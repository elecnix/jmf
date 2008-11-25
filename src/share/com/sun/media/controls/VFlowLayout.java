/*
 * @(#)VFlowLayout.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import java.awt.*;
import java.util.Vector;
/*
 * Vertical FlowLayout manager. it accepts components and put them one under the other.
 * it saves the common solution which is recursive BorderLayout.
 *
 * @author Shay Ben-David (bendavid@haifa.ibm.com)
 *
 */
public class VFlowLayout implements LayoutManager {
   /** <FONT COLOR="#FF0000">
    *  Licensed Materials - Property of IBM                         <br><br>
    *  "Restricted Materials of IBM"                                <br><br>
    *  5648-B81                                                     <br><br>
    *  (c) Copyright IBM Corporation 1997,1999 All Rights Reserved  <br><br>
    *  US Government Users Restricted Rights - Use, duplication or
    *  disclosure restricted by GSA ADP Schedule Contract with
    *  IBM Corporation.</FONT>
    *
    **/
    public static final String a_copyright_notice="(c) Copyright IBM Corporation 1997,1999.";

    private int gap;
    private int minWidth = 0, minHeight = 0;
    private int preferredWidth = 0, preferredHeight = 0;
    private boolean sizeUnknown = true;

    /** flow layout without gap **/
    public VFlowLayout() {
        this(0);
    }

    /** flow layout with the specified gap **/
    public VFlowLayout(int v) {
        gap = v;
    }

    /* Required by LayoutManager. */
    public void addLayoutComponent(String name, Component comp) {
    }

    /* Required by LayoutManager. */
    public void removeLayoutComponent(Component comp) {
    }
    /** calculates minimum size and preffered size **/
    private void setSizes(Container parent) {
        int nComps = parent.countComponents();
        Dimension d = null;

        //Reset preferred/minimum width and height.
        preferredWidth = 0;
        preferredHeight = 0;
        minWidth = 0;
        minHeight = 0;

        for (int i = 0; i < nComps; i++) {
            Component c = parent.getComponent(i);
            if (c.isVisible()) {
                d = c.preferredSize();

                minWidth       = Math.max(c.minimumSize().width, minWidth);
                preferredWidth = Math.max(c.preferredSize().width, preferredWidth);

                minHeight       += (c.minimumSize().height + gap);
                preferredHeight += (c.preferredSize().height + gap);

            }
        }
    }


    /* Required by LayoutManager. */
    public Dimension preferredLayoutSize(Container parent) {
        Dimension dim = new Dimension(0, 0);
        int nComps = parent.countComponents();

        setSizes(parent);

        //Always add the container's insets!
        Insets insets = parent.insets();
        dim.width = preferredWidth + insets.left + insets.right;
        dim.height = preferredHeight + insets.top + insets.bottom;

        sizeUnknown = false;

        return dim;
    }

    /* Required by LayoutManager. */
    public Dimension minimumLayoutSize(Container parent) {
        Dimension dim = new Dimension(0, 0);
        int nComps = parent.countComponents();

        setSizes(parent);

        //Always add the container's insets!
        Insets insets = parent.insets();
        dim.width = minWidth + insets.left + insets.right;
        dim.height = minHeight + insets.top + insets.bottom;

        sizeUnknown = false;

        return dim;
    }

    /* Required by LayoutManager. */
    /* This is called when the panel is first displayed,
     * and every time its size changes.
     * Note: You CAN'T assume preferredLayoutSize() or minimumLayoutSize()
     * will be called -- in the case of applets, at least, they probably
     * won't be. */
    public void layoutContainer(Container parent) {
        Insets insets = parent.insets();
        int maxWidth = parent.size().width
                       - (insets.left + insets.right);
        int maxHeight = parent.size().height
                        - (insets.top + insets.bottom);
        int nComps = parent.countComponents();

        // Go through the components' sizes, if neither preferredLayoutSize()
        // nor minimumLayoutSize() has been called.
        if (sizeUnknown) {
            setSizes(parent);
        }

        int previousWidth = 0, previousHeight = 0;
        int x = 0, y = insets.top + gap/2;
        int rowh = 0, start = 0;
        int yFudge = 0;
        boolean oneColumn = false;

        // Go through the components' sizes, if neither preferredLayoutSize()
        // nor minimumLayoutSize() has been called.
        if (sizeUnknown) {
            setSizes(parent);
        }

        if (maxHeight > preferredHeight) {
            yFudge = (maxHeight - preferredHeight)/(nComps);
        }

        for (int i = 0 ; i < nComps ; i++) {
            Component c = parent.getComponent(i);
            if (c.isVisible()) {
                Dimension d = c.preferredSize();

                if (i!=0)
                  y+=(previousHeight + yFudge + gap);
                else
                  y+=(previousHeight + (yFudge + gap)/2 );

                // Set the component's size and position.
                c.reshape(0, y, maxWidth, d.height);

                previousWidth = d.width;
                previousHeight = d.height;
            }
        }
    }

    public String toString() {
        return getClass().getName() + "[gap=" + gap +"]";
    }
}
