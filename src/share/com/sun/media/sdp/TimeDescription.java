/*
 * @(#)TimeDescription.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.sdp;

import java.io.*;
import java.util.*;

public class TimeDescription extends Parser {
    // Values:
    public String timeActive;
    public Vector repeatTimes;

    public TimeDescription(ByteArrayInputStream bin) {
        // Time the session is active:
        timeActive = getLine(bin);

        // Repeat Times:
        repeatTimes = new Vector();

        boolean found = getToken(bin, "r=", false);

        while (found) {
            String repeatTime = getLine(bin);

            repeatTimes.addElement(repeatTime);

            found = getToken(bin, "r=", false);
        }
    }
}
