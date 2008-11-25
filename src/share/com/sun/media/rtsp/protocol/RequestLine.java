/*
 * @(#)RequestLine.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

import com.sun.media.rtsp.*;

public class RequestLine extends Parser {
    private String url;
    private String version;

    public RequestLine(String input) {
        ByteArrayInputStream bin = new ByteArrayInputStream(input.getBytes());

        String method = getToken(bin);

        Debug.println("method  : " + method);

        url = getToken(bin);

        Debug.println("url     : " + url);

        version = getToken(bin);

        Debug.println("version : " + version);
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }
}
