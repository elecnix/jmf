/*
 * @(#)DynamicPlugIn.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import javax.media.Format;

public interface DynamicPlugIn {

    /**
     * An array of format objects that cover the generic input formats
     * that this plugin supports. For example, a VideoRenderer may not
     * know the exact RGBFormat it supports, so it returns a dummy RGBFormat
     * with mostly unspecified values.
     */
    Format [] getBaseInputFormats();

    /**
     * An array of format objects that cover the generic output formats
     * that this plugin supports. For example, a Codec may not
     * know the exact output RGBFormat it supports, so it returns a dummy
     * RGBFormat with mostly unspecified values.
     */
    Format [] getBaseOutputFormats();
}
