/*
 * @(#)JMD.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import javax.media.Buffer;

public interface JMD extends javax.media.Control {
    
    void setVisible(boolean visible);
    
    void initGraph(BasicModule bm);
    void moduleIn(BasicModule bm, int index, Buffer in, boolean here);
    void moduleOut(BasicModule bm, int index, Buffer out, boolean here);

}
