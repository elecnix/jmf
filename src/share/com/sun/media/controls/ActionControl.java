/*
 * @(#)ActionControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import com.sun.media.*;

/**
 * ActionControl can be used to perform an action, similar to the behaviour
 * of a push button.
 */
public interface ActionControl extends AtomicControl {

    /**
     * Returns true if the action could be performed.
     */
    boolean performAction();
}
