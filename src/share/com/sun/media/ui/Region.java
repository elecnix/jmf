/*
 * @(#)Region.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.Rectangle;
import java.util.Vector;
import java.util.Enumeration;
import java.lang.Cloneable;
import java.io.Serializable;

class Region implements Cloneable, Serializable {

    /**
     * List of rectangles that make up the region.
     */
    protected Vector rects;

    /**
     * Create a new region with no rectangles.
     */
    public Region() {
        rects = new Vector();
    }

    /**
     * Create a region with one rectangle in it.
     */
    public Region(Rectangle r) {
        rects = new Vector();
        addRectangle(r);
    }

    /**
     * Create a region with two rectangles. Useful for moving-sprites
     * which need to invalidate old and new positions.
     */
    public Region(Rectangle r1, Rectangle r2) {
        rects = new Vector();
        addRectangle(r1);
        addRectangle(r2);
    }

    /**
     * Returns true if the region contains no rectangles.
     */
    public boolean isEmpty() {
        return rects.isEmpty();
    }

    public int getNumRectangles() {
	return rects.size();
    }

    /**
     * Returns an Enumeration of the list of rectangles.
     */
    public java.util.Enumeration rectangles() {
        return rects.elements();
    }

    /**
     * Returns a clone of the region.
     */
    public Object clone() {
        Region r = new Region();
        
        r.rects = (Vector)rects.clone();

        return r;
    }

    public Rectangle getBounds() {
	Rectangle r = new Rectangle();
	
	for (int i = 0; i < rects.size(); i++) {
	    r = r.union((Rectangle)rects.elementAt(i));
	}
	return r;
    }


    /**
     * Adds a rectangle to the region. This method checks for intersecting
     * rectangles and merges them into one.
     */
    public void addRectangle(Rectangle r) {

        Rectangle current;
        int position = 0;
        
        while (position < rects.size())
        {
            current = (Rectangle)rects.elementAt(position);

            // First check to see if the current rectangle
            // already includes the new one
            if ((r.x > current.x) && (r.y > current.y) &&
                (right(r) <= right(current)) && 
                (bottom(r) <= bottom(current)))
                return;

            if (r.intersects(current))
            {
                r = r.union(current);
                rects.removeElementAt(position);
            } else {
                position++;
            }
        }
        // Add it to the end of the list
        rects.addElement(r);
    }

    /**
     * Checks if the rectangle <b>r</b> intersects any of the rectangles
     * in the region.
     */
    public boolean intersects(Rectangle r) {

	Rectangle rect;
	int position = 0;
	
	while (position < rects.size()) {
	    rect = (Rectangle)rects.elementAt(position);
	    if (rect.intersects(r)) {
		return true;
	    }
	    position++;
	}
	return false;
    }

    /**
     * Intersects all the rectangles in the region with rectangle <b>r</b>
     * and throws away any that dont intersect.
     */
    public void intersect(Rectangle r) {

        Rectangle rect;
        int position = 0;

        while (position < rects.size()) {
            rect = (Rectangle)rects.elementAt(position);
            rect = rect.intersection(r);
            if (rect.isEmpty()) {
                rects.removeElementAt(position);
            } else {
                rects.setElementAt(rect, position);
                position++;
            }
        }
    }

    /**
     * Adds a region <b>r</b> to the current region.
     */
    public void addRegion(Region r) {

        for (Enumeration e = r.rectangles(); e.hasMoreElements(); )
        {
            addRectangle((Rectangle)e.nextElement());
        }
    }

    /**
     * Translates all rectangles in the region by (dx,dy).
     */
    public void translate(int dx, int dy) {

        Rectangle r;

        for (int p = 0; p < rects.size(); p++) {
            r = (Rectangle)rects.elementAt(p);
            r.translate(dx, dy);
        }
    }

    /**
     * Converts the list of rectangles to a displayable string.
     */
    public String toString() {
        String s = getClass().getName() + " = [\n";

        for (Enumeration e = rectangles(); e.hasMoreElements(); )
        {
            s += "(" + (Rectangle)e.nextElement() + ")\n";
        }
        return s + "]";
    }

    /**
     * Returns the x-coordinate of the right edge of a rectangle.
     */
    public static int right(Rectangle r) {
        return r.x + r.width - 1;
    }

    /**
     * Returns the y-coordinate of the bottom edge of a rectangle.
     */
    public static int bottom(Rectangle r) {
        return r.y + r.height - 1;
    }
}
