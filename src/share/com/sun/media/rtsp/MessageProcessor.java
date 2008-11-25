/*
 * @(#)MessageProcessor.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp;

import com.sun.media.Log;
import com.sun.media.rtsp.protocol.*;

public class MessageProcessor {
    private int connectionId;
    private RtspManager rtspManager;
    private byte buffer[];

    public MessageProcessor(int connectionId, RtspManager rtspManager) {
        this.connectionId = connectionId;
        this.rtspManager = rtspManager;

        buffer = new byte[0];
    }

    public void processMessage(byte data[]) {
        Log.comment("incoming msg:");
        Log.comment(new String(data));

        Message message = new Message(data);

        rtspManager.dataIndication(connectionId, message);
    }
}




