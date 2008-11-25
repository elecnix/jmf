/*
 * @(#)RtspPort.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp;

abstract public class RtspPort {
    final public static int RTSP_DEFAULT_PORT = 1554;

    public static int port = RTSP_DEFAULT_PORT;

    public static void setPort(int current_port) {
        port = current_port;
    }

    public static int getPort() {
        return port;
    }
}
