/*
 * @(#)TeardownMessage.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class TeardownMessage extends RequestMessage {
    public TeardownMessage(byte data[]) {
        super(data);
    }

    public TeardownMessage(String url, int sequenceNumber, int sessionId) {
        String msg = "TEARDOWN " + url + "RTSP/1.0" + "\r\n" + "CSeq: " +
                sequenceNumber + "\r\n" + "Session: " + sessionId + "\r\n";
    }
}


