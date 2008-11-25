/*
 * @(#)SessionHeader.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;


public class SessionHeader {
    private String sessionId;
    private long   timeout;

    public SessionHeader(String str) {
        int index= str.indexOf( ';');

        if( index > 0) {
            sessionId= str.substring( 0, index);

            str= str.substring( index);

            index= str.indexOf( '=');

            String seconds= str.substring( index + 1);

            try {
                timeout= new Long( seconds).longValue();
	    } catch( NumberFormatException e) {
	        timeout= 60; // default is 60 seconds
	    }          
	} else {
            sessionId= str;
	}
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getTimeoutValue() {
        return timeout;
    }
}
