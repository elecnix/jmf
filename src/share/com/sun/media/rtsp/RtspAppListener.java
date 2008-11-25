/*
 * @(#)RtspAppListener.java	1.4 02/08/21
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

package com.sun.media.rtsp;

import java.io.*;
import java.net.*;

import com.sun.media.rtsp.protocol.*;

/**
 * The listener interface for receiving RTSP events.
 */

public interface RtspAppListener
{
    void streamsReceivedEvent();

    void postStatusMessage( int    code,
                            String message);
}


