/*
 * @(#)TransportHeader.java	1.6 00/05/09
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class ContentBaseHeader {
    private String contentBase;

    public ContentBaseHeader(String contentBase) {
        // Debug.println( "Content-Base : " + contentBase);
    
        this.contentBase= contentBase;
    }

    public String getContentBase() {
        return contentBase;
    }
}
