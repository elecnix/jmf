/*
 * @(#)CaptureDevice.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

import java.io.IOException;
import javax.media.control.FormatControl;

/**
 * <code>CaptureDevice</code> is the interface for all capture devices.
 * <p>
 * A capture device is a <code>DataSource</code> of the type 
 * <code>PullDataSource</code>, <code>PullBufferDataSource</code>,
 * <code>PushDataSource</code> or <code>PushBufferDataSource</code>.
 * It also implements the <code>CaptureDevice</code> interface which
 * further specializes the <code>DataSource</code> to handle data capturing.
 * <p>
 * A capture <code>DataSource</code> contains an array of 
 * <code>SourceStream<code>'s.  These <code>SourceStreams</code> provide
 * the interface for the captured data streams to be read.
 * <p>
 * The formats of the captured data can be manipulated by the
 * <code>FormatControl</code> objects.  There is one <code>FormatControl</code>
 * per each capture stream.  These controls can be obtained by the
 * <code>getFormatControls</code> method.
 * <p>
 * A few methods from the <code>DataSource</code> are re-defined
 * to support semantics of capture devices.
 * 
 * @see DataSource
 * @see SourceStream
 * @see FormatControl
 * @since JMF 2.0
 */
public interface CaptureDevice {

    /**
     * Return the <code>CaptureDeviceInfo</code> object that describes 
     * this device.
     * @return The <code>CaptureDeviceInfo</code> object that describes 
     * this device.
     */
    public javax.media.CaptureDeviceInfo getCaptureDeviceInfo();

    /**
     * Returns an array of <code>FormatControl</code> objects.  Each of
     * them can be used to set and get the format of each capture stream. 
     * This method can be used before connect to set and get the
     * capture formats. 
     * @return an array for FormatControls.
     */
    public FormatControl[] getFormatControls(); 

    /**
     * Initialize the capture device.  The device will be initialized to
     * the formats specified by using the <code>FormatControl</code>. 
     * The default will be used if no formats were specified.
     * <p>
     * After connect, the resources required by the device will be
     * allocated.  If the device is an exclusive device, connect from
     * other capture <code>DataSource</code>'s referring to the same
     * device will fail. 
     *
     * @exception IOException Thrown if there are IO problems
     * when <CODE>connect</CODE> is called.
     */
    public void connect() throws IOException; 

    /**
     * Close the capture device.  It frees the resources held by the device.
     * <p>
     * If <CODE>stop</CODE> hasn't already been called,
     * calling <CODE>disconnect</CODE> implies a stop.
     *
     * <code>connect</code> may be called again to re-open the device.
     */
    public void disconnect();

    /**
     * Start the data capture.
     */
    public void start() throws IOException;

    /**
     * Stop the data capture.
     */
    public void stop() throws IOException;
}

