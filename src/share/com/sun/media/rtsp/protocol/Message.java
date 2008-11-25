/*
 * @(#)Message.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.util.*;

public class Message {
    private byte data[];
    private int type;
    private Object parameter;

    public Message(int type, Object parameter) {
    }

    public Message(byte data[]) {
        this.data = data;

        parseData();
    }

    private void parseData() {
        StringTokenizer st = new StringTokenizer(new String(data));

        type = new MessageType(st.nextToken()).getType();

        switch (type) {
            case MessageType.DESCRIBE:
                parameter = (Object) new DescribeMessage(data);
                break;
            case MessageType.SETUP:
                parameter = (Object) new SetupMessage(data);
                break;
            case MessageType.PLAY:
                parameter = (Object) new PlayMessage(data);
                break;
            case MessageType.PAUSE:
                parameter = (Object) new PauseMessage(data);
                break;
            case MessageType.TEARDOWN:
                parameter = (Object) new TeardownMessage(data);
                break;
            case MessageType.OPTIONS:
                parameter = (Object) new OptionsMessage(data);
                break;
            case MessageType.RESPONSE:
                parameter = (Object) new ResponseMessage(data);
                break;
            case MessageType.SET_PARAMETER:
                parameter = (Object) new SetParameterMessage(data);
                break;
            default:
                Debug.println("Unknown msg type: " + type);
                Debug.println("Unknown msg type: " + new String(data));
                break;
        }
    }

    public int getType() {
        return type;
    }

    public Object getParameter() {
        return parameter;
    }
}



