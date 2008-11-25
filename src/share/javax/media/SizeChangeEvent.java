/*
 * @(#)SizeChangeEvent.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;
import javax.media.format.FormatChangeEvent;

/**
 * Event which indicates that the input video has changed in size and the video
 * renderer needs to be resized to specified size. Also includes the scale to
 * which the video is going to be zoomed.
 * @since JMF 2.0
 */
public class SizeChangeEvent extends FormatChangeEvent {

    protected int width;
    protected int height;
    protected float scale;

    public SizeChangeEvent (Controller from, int width, int height, float scale) {
	super(from);
	this.width = width;
	this.height = height;
	this.scale = scale;
    }

    /** Get the new width of the video input. */
    public int getWidth() {
	return width;
    }

    /** Get the new height of the video input. */
    public int getHeight() {
	return height;
    }

    /** Get the new scale to which the video is being zoomed. */
    public float getScale() {
	return scale;
    }
}
