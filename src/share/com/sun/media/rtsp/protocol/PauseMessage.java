/*
 * @(#)PauseMessage.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class PauseMessage extends RequestMessage {
    public PauseMessage(byte data[]) {
        super(data);
    }

    public PauseMessage(String url, int sequenceNumber, int sessionId) {
        String msg = "PAUSE " + url + "RTSP/1.0" + "\r\n" + "CSeq: " +
                sequenceNumber + "\r\n" + "Session: " + sessionId + "\r\n";
    }
}


