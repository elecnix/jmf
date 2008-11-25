/*
 * @(#)MonitorControl.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

import javax.media.Control;


/**
 * Some capture devices or encoders may have a monitor to view/listen to
 * the capture or encoding progress.
 * The MonitorControl allows you to enable/disable monitoring and in
 * the case of a video monitor, also control the preview rate. Setting
 * a lower preview rate will result in lesser resource consumption.
 * @since JMF 2.0
 */
public interface MonitorControl extends javax.media.Control {

    /**
     * Enable or disable the monitor.
     * @param on true to enable monitoring.
     * @return true if the monitor is enabled.
     */
    public boolean setEnabled(boolean on);

    /**
     * Sets the frame rate at which the video is to be previewed.
     * This does not affect the actual capture or encoding rate.
     * @param rate the frame rate used for previewing.
     * @return the actual rate that it was set to. Returns -1 if
     * this is not a video monitor. 
     */
    public float setPreviewFrameRate(float rate);
}
