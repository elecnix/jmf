/*
 * @(#)DescribeMessage.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class DescribeMessage extends RequestMessage {
    public DescribeMessage(byte data[]) {
        super(data);
    }

    public DescribeMessage(String url, int sequenceNumber) {
        String msg = "DESCRIBE " + url + "RTSP/1.0" + "\r\n" + "CSeq: " +
                sequenceNumber + "\r\n\r\n";
    }
}





