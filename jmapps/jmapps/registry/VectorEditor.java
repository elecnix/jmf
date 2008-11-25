/*
 * @(#)VectorEditor.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.registry;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

public interface VectorEditor {

    Vector getList(int code);
    void setList(int code, Vector v);
    void commit(int code);
    void selectedIndex(int code, int index);
    boolean addToList(int code, String value);
}


