/*
 * @(#)Debug.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp;

import java.io.*;

abstract class Debug {
    static boolean debug_enabled = false;

    public static void println(Object object) {
        if (debug_enabled) {
            System.out.println(object);
        }
    }

    public static void dump(byte data[]) {
        if (debug_enabled) {
            for (int i = 0; i < data.length; i++) {
                int value = (data[i] & 0xff);

                System.out.println(i + ": " + Integer.toHexString(value));
            }
        }
    }
}
