/*
 * @(#)InternalErrorEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * An <code>InternalErrorEvent</code> indicates that a <code>Controller</code> failed
 * for implementation-specific reasons.
 * This event indicates that there are problems with the implementation of the <code>Controller</code>.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21
 */
public class InternalErrorEvent extends ControllerErrorEvent {


    public InternalErrorEvent(Controller from) {
        super(from);
    }

    public InternalErrorEvent(Controller from, String message) {
        super(from, message);
    }

}
