/*
 * @(#)StringControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import com.sun.media.*;

/**
 * A StringControl holds a string value and can be used to display status
 * information pertaining to the player. In most cases this will be a
 * read-only control.
 */
public interface StringControl extends AtomicControl {

    /**
     * ???  Sets the string value for this control. Returns the actual string
     * that was set.
     */
    String setValue(String value);

    /**
     * Returns the string value for this control.
     */
    String getValue();

    String setTitle(String title);

    String getTitle();
}    
