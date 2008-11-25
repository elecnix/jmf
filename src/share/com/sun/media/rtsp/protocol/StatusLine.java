/*
 * @(#)StatusLine.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class StatusLine extends Parser {
    private String protocol;
    private int code;
    private String reason;

    public StatusLine(String input) {
        ByteArrayInputStream bin = new ByteArrayInputStream(input.getBytes());

        String protocol = getToken(bin);

        Debug.println("protocol : " + protocol);

        code = new Integer(getToken(bin)).intValue();

        Debug.println("code     : " + code);

        reason = getStringToken(bin);

        Debug.println("reason   : " + reason);
    }

    public String getReason() {
        return reason;
    }

    public int getCode() {
        return code;
    }
}
