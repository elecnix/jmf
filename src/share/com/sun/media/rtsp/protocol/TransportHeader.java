/*
 * @(#)TransportHeader.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class TransportHeader {
    private String transportProtocol;
    private String profile;
    private String lowerTransport;
    private int server_data_port;
    private int server_control_port;

    public TransportHeader(String str) {
        // Debug.println("TransportHeader: " + str);

        int end = str.indexOf('/');

        transportProtocol = str.substring(0, end);


	// client port:
	int start = str.indexOf( "client_port");

	if( start > 0)
	{
	}

	// server port:
	start = str.indexOf( "server_port");

	if( start > 0)
	{
	    // data port:
	    start = str.indexOf("=", start) + 1;

	    end = str.indexOf("-", start);

            String data_str = str.substring(start, end);

            server_data_port = new Integer(data_str).intValue();

	    // control port:
	    start = end + 1;

	    end = str.indexOf(";", start);

	    String control_str;

	    if( end > 0)
	    {
		control_str = str.substring( start, end);
	    }
	    else
	    {		
                control_str = str.substring(start);	    
	    }
	    
            server_control_port = new Integer(control_str).intValue();		
	}
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    public int getServerDataPort() {
        return server_data_port;
    }

    public int getServerControlPort() {
        return server_control_port;
    }
}
