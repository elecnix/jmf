/*
 * @(#)Control.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import java.awt.Component;

/**
 * The base interface for processing <CODE>Control</CODE> objects.
 *
 * @version 1.2, 02/08/21
 */

public interface Control {
    
    /**
     * Get the <code>Component</code> associated with this
     * <code>Control</code> object.
     * For example, this method might return
     * a slider for volume control or a panel containing radio buttons for 
     * CODEC control.
     * The <code>getControlComponent</code> method can return
     * <CODE>null</CODE> if there is no GUI control for
     * this <code>Control</code>.
     */
    public Component getControlComponent();
}
