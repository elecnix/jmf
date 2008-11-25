/*
 * @(#)SetupMessage.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class SetupMessage extends RequestMessage {
    public SetupMessage(byte data[]) {
        super(data);
    }

    public SetupMessage(String url, int sequenceNumber, int port_lo, int port_hi) {
        String msg = "SETUP " + url + "RTSP/1.0" + "\r\n" + "CSeq: " +
                sequenceNumber + "\r\n" +
                "Transport: RTP/AVP;unicast;client_port=" + port_lo + "-" + port_hi;
    }
}
