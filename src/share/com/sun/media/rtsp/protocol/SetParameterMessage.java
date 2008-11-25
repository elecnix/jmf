/*
 * @(#)SetParameterMessage.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class SetParameterMessage extends RequestMessage {
    public SetParameterMessage(byte data[]) {
        super(data);
    }
}

