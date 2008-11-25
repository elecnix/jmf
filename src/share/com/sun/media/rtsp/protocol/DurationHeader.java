/*
 * @(#)DurationHeader.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class DurationHeader {
    private long duration;

    public DurationHeader(String str) {
        duration = new Long(str).longValue();
    }

    public long getDuration() {
        return duration;
    }
}


