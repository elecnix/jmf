/*
 * @(#)RtspControl.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package javax.media.control;

/**
 * This interface is a Control for obtaining a handle on 
 * RTPManager objects, for example from the RTSP Player.
 * 
 * @since JMF 2.1
 */

import javax.media.rtp.*;

public interface RtspControl extends javax.media.Control {

    /* returns a list of RTPManager objects */
    public RTPManager[] getRTPManagers();
}
