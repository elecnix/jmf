/*
 * @(#)SlowPlugIn.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import javax.media.*;


/**
 * An interface to denote a particularly slow plugin.
 */
public interface SlowPlugIn {

    /**
     * Force to use this plugin even though there may be another better
     * alternative.
     */
    public void forceToUse();

}

