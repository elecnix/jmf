/*
 * @(#)OptionsMessage.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class OptionsMessage extends RequestMessage {
    public OptionsMessage(byte data[]) {
        super(data);
    }
}

