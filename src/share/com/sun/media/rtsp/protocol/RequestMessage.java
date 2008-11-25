/*
 * @(#)RequestMessage.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class RequestMessage {
    private byte data[];
    private Request request;

    public RequestMessage() {

    }

    public RequestMessage(byte data[]) {
        this.data = data;

        parseRequest();
    }

    private void parseRequest() {
        request = new Request(new ByteArrayInputStream(data));
    }

    public Request getRequest() {
        return request;
    }
}


