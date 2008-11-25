/*
 * @(#)CSeqHeader.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp.protocol;

import java.io.*;
import java.util.*;

public class CSeqHeader {
    private String sequence_number;

    public CSeqHeader(String number) {
        this.sequence_number = number;
    }

    public String getSequenceNumber() {
        return sequence_number;
    }
}



