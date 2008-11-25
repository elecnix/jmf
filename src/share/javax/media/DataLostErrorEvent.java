/*
 * @(#)DataLostErrorEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <CODE>DataLostErrorEvent</CODE> is posted when a <code>Controller</code> 
 * has lost data.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21
 */
public class DataLostErrorEvent extends ControllerClosedEvent {
    public DataLostErrorEvent(Controller from) {
        super(from);
    }

    public DataLostErrorEvent(Controller from, String why) {
        super(from, why);
    }
}
