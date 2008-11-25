/*
 * @(#)Reparentable.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import javax.media.Owned;

/**
 * Interface that extends Owned and allows the owner to be changed.
 */

public interface Reparentable extends Owned {

    void setOwner(Object newOwner);
}
