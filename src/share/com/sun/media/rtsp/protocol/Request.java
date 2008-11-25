/*
 * @(#)Request.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;
import com.sun.media.rtsp.*;

public class Request extends Parser {
    public RequestLine requestLine;
    public Vector headers;

    public Request(ByteArrayInputStream bin) {
        String line = getLine(bin);

        requestLine = new RequestLine(line);

        headers = new Vector();

        line = getLine(bin);

        while (line.length() > 0) {
            if (line.length() > 0) {
                Header header = new Header(line);

                headers.addElement(header);

                line = getLine(bin);
            }
        }
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }

    public Header getHeader(int type) {
        Header header = null;

        for (int i = 0; i < headers.size(); i++) {
            Header tmpHeader = (Header) headers.elementAt(i);

            if (tmpHeader.type == type) {
                header = tmpHeader;

                break;
            }
        }

        return header;
    }
}
