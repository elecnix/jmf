/*
 * @(#)ResponseMessage.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class ResponseMessage {
    private byte data[];
    private Response response;

    public ResponseMessage(byte data[]) {
        this.data = data;

        parseResponse();
    }

    private void parseResponse() {
        response = new Response(new ByteArrayInputStream(data));
    }

    public Response getResponse() {
        return response;
    }
}
