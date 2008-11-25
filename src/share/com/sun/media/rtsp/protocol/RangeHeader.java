/*
 * @(#)RangeHeader.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class RangeHeader {
    private long startPos;

    public RangeHeader(String str) {
        int start = str.indexOf('=') + 1;
        int end = str.indexOf('-');

        String startPosStr = str.substring(start, end);

        startPos = new Long(startPosStr).longValue();
    }

    public long getStartPos() {
        return startPos;
    }
}
