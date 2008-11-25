/*
 * @(#)RtspUrl.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp;

import java.net.*;

public class RtspUrl {
    private String url;

    public RtspUrl(String url) throws MalformedURLException {
        this.url = url;

	if( url.length() < 7) {
	    throw new MalformedURLException();
	}

	if( !url.startsWith( "rtsp://")) {
	    throw new MalformedURLException();
	}
    }

    public String getFile() {
        String str = url.substring(7);

        int start = str.indexOf('/');

	String file= "";
	
	if( start != -1) {
            file = str.substring( start + 1);
	}
	
        return file;
    }

    public String getHost() {
	String host= null;
	
        String str = url.substring(7);

        int end = str.indexOf(':');

        if (end == -1) {
            end = str.indexOf('/');

	    if( end == -1) {
		host= str;
	    } else {
		host= str.substring( 0, end);
	    }
        } else {
            host = str.substring(0, end);
	}

        return host;
    }
    
    public int getPort() {
        int port = 554; // default port for RTSP

        String str = url.substring(7);

        int start = str.indexOf(':');

        if (start != -1) {
            int end = str.indexOf('/');

            port = new Integer(str.substring(start + 1, end)).intValue();
        }

        return port;
    }
}

