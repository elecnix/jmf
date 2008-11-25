/*
 * @(#)ResourceUnavailableEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>ResourceUnavailableEvent</code> indicates that a <code>Controller</code> was
 * unable to allocate a resource that it requires for operation.
 *
 * @see Controller
 * @see ControllerListener
 * @version 1.2, 02/08/21
 */
public class ResourceUnavailableEvent extends ControllerErrorEvent{


    public ResourceUnavailableEvent(Controller from) {
        super(from);
    }

    public ResourceUnavailableEvent(Controller from, String message) {
        super(from, message);
    }

}
