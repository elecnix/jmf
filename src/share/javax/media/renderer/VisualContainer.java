/*
 * @(#)VisualContainer.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.renderer;

/**
 * Players that need to export multiple visual components in one
 * container will implement VisualContainer on the visual component.
 * You can cast the visual component to a java.awt.Container and use the
 * usual java.awt.Container methods to retrieve the individual
 * components in the container. The components can then be placed
 * as desired in a custom GUI.
 * @since JMF 2.0
 */
public interface VisualContainer {
    // No methods.
}
