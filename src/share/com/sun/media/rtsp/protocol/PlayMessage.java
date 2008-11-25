/*
 * @(#)PlayMessage.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class PlayMessage extends RequestMessage {
    public PlayMessage(byte data[]) {
        super(data);
    }

    public PlayMessage(String url, int sequenceNumber, int sessionId,
            int range_lo, int range_hi) {
        String msg = "PLAY " + url + "RTSP/1.0" + "\r\n" + "CSeq: " +
                sequenceNumber + "\r\n" + "Session: " + sessionId + "\r\n" +
                "Range: npt=" + range_lo + "-" + range_hi;
    }
}
