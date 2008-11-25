/*
 * @(#)Header.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

import com.sun.media.rtsp.*;

public class Header extends Parser {
    public int type; // the header type
    public Object parameter; // parameter entries
    public int contentLength;

    public final static int TRANSPORT = 1;
    public final static int CSEQ = 2;
    public final static int SESSION = 3;
    public final static int DURATION = 4;
    public final static int RANGE = 5;
    public final static int DATE = 6;
    public final static int SERVER = 7;
    public final static int CONTENT_TYPE = 8;
    public final static int CONTENT_BASE = 9;
    public final static int CONTENT_LENGTH = 10;

    public Header(String input) {
        ByteArrayInputStream bin = new ByteArrayInputStream(input.getBytes());

        String id = getToken(bin);

        if (id.equalsIgnoreCase("CSeq:")) {
            type = CSEQ;

            String number = getStringToken(bin).trim();

            parameter = new CSeqHeader(number);
        } else if (id.equalsIgnoreCase("Transport:")) {
            type = TRANSPORT;

            String tx = getToken(bin);

            parameter = new TransportHeader(tx);
        } else if (id.equalsIgnoreCase("Session:")) {
            type = SESSION;

            String tx = getToken(bin);

            parameter = new SessionHeader(tx);
        } else if (id.equalsIgnoreCase("Duration:")) {
            type = DURATION;

            String tx = getToken(bin);

            Debug.println("Duration : " + tx);

            parameter = new DurationHeader(tx);
        } else if (id.equalsIgnoreCase("Range:")) {
            type = RANGE;

            String tx = getToken(bin);

            parameter = new RangeHeader(tx);
        } else if (id.equalsIgnoreCase("Date:")) {
            type = DATE;

            String date = getStringToken(bin);

            // Debug.println( "Date : " + date);
        } else if (id.equalsIgnoreCase("Allow:")) {
            type = DATE;

            String entries = getStringToken(bin);

            // Debug.println( "Allow : " + entries);
        } else if (id.equalsIgnoreCase("Server:")) {
            type = SERVER;

            String server = getStringToken(bin);

            // Debug.println ( "Server   : " + server);
        } else if (id.equalsIgnoreCase("Content-Type:")) {
            type = CONTENT_TYPE;

            String content_type = getStringToken(bin);

            // Debug.println( "Content-Type : " + content_type);
        } else if (id.equalsIgnoreCase("Content-Base:")) {
            type = CONTENT_BASE;
            String content_base = getStringToken(bin);

            parameter = new ContentBaseHeader(content_base);
        } else if (id.equalsIgnoreCase("Content-Length:")) {
            type = CONTENT_LENGTH;

            String content_length = getStringToken(bin);

            // System.out.println( "Content-Length : " + content_length);

            contentLength = new Integer(content_length).intValue();
        } else if (id.equalsIgnoreCase("Last-Modified:")) {
            String date = getStringToken(bin);

            // Debug.println( "Last-Modified: " + date);
        } else if (id.equalsIgnoreCase("RTP-Info:")) {
            String rtpInfo = getStringToken(bin);

            // Debug.println( "RTP-Info: " + rtpInfo);
        } else if (id.length() > 0) {
            Debug.println("unknown id : <" + id + ">");

            String tmp = getStringToken(bin);
        }
    }
}







