/*
 * @(#)Owned.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

/**
 * The Owned interface is implemented by Control objects that wish to advertise
 * which media component owns the Control. A Control is typically owned by a
 * PlugIn, Player, Processor, DataSource or DataSink object.
 * @since JMF 2.0
 * @see Control
 */

package javax.media;

public interface Owned {

    /**
     * Returns the object that owns this control.
     */
    Object getOwner();
}
