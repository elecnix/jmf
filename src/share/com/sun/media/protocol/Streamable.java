/*
 * @(#)Streamable.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol;

import javax.media.*;


/**
 * This is a special tagging interface to specify whether a DataSource
 * is intended to be used as a streamable DataSource.  In such case,
 * it needs to report some properties that the Handler will need to know.
 */ 
public interface Streamable {

    /**
     * Returns true if the DataSource can be prefetched.
     */
    public boolean isPrefetchable();
}
