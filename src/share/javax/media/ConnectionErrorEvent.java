/*
 * @(#)ConnectionErrorEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <CODE>ConnectionErrorEvent</CODE> is posted when an error occurs within a
 <CODE>DataSource</CODE>
 * when obtaining data or communicating with a server.
 **/

public class ConnectionErrorEvent extends ControllerErrorEvent {


    public ConnectionErrorEvent(Controller from) {
        super(from);
    }

    public ConnectionErrorEvent(Controller from, String why) {
        super(from, why);
    }

}
