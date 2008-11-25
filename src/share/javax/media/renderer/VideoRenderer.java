/*
 * @(#)VideoRenderer.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.renderer;

import java.awt.Component;
import java.awt.Rectangle;
import javax.media.Renderer;

/**
 * An interface for renderers that render video to a display or
 * any other such device.
 * @since JMF 2.0
 */
public interface VideoRenderer extends javax.media.Renderer {

    /**
     * Returns an AWT component that it will render to. Returns null
     * if it is not rendering to an AWT component.
     * @return  the component that will used for rendering.
     */
    public java.awt.Component getComponent();

    /**
     * Requests the renderer to draw into a specified AWT component.
     * @param comp the component to be used for rendering.
     * @return false if the renderer cannot draw into the specified
     * component.
     */
    public boolean setComponent(java.awt.Component comp);

    /**
     * Sets the region in the component where the video is to be
     * rendered to. Video is to be scaled if necessary. If <code>rect</code>
     * is null, then the video occupies the entire component.
     * @param rect the rect that defines the region to be rendered to.
     */
    public void setBounds(java.awt.Rectangle rect);

    /**
     * Returns the region in the component where the video will be
     * rendered to. Returns null if the entire component is being used.
     * @return the region where the video will be rendered.
     */
    public java.awt.Rectangle getBounds();
}

