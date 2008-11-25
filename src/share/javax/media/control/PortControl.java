/*
 * @(#)PortControl.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;		  


/**
 * The <code>PortControl</code> interface represents a control to
 * access the input or output ports of a device.  A device in
 * this case could be an capture device (<code>CaptureDevice</code>)
 * or a renderer (<code>Renderer</code>).
 * Methods are provided to find out what
 * ports the device includes, to find out which ports are currently
 * turned on, and to turn a port on or off.
 *
 */
public interface PortControl extends javax.media.Control {

    /**
     * Specifies a microphone port.
     */
    public static final int MICROPHONE = (1 << 0);

    /**
     * Specifies a line in port.
     */
    public static final int LINE_IN = (1 << 1);

    /**
     * Specifies a speaker port.
     */
    public static final int SPEAKER = (1 << 2);

    /**
     * Specifies a headphone port.
     */
    public static final int HEADPHONE = (1 << 3); 

    /**
     * Specifies a line out port.
     */
    public static final int LINE_OUT = (1 << 4); 

    /**
     * Specifies a compact disc port.
     */
    public static final int COMPACT_DISC = (1 << 5); 

    /**
     * Specifies a S-Video port.
     */
    public static final int SVIDEO = (1 << 6); 

    /**
     * Specifies a composite video port.
     */
    public static final int COMPOSITE_VIDEO = (1 << 7);

    /**
     * Specifies a TV-Tuner input port.
     */
    public static final int TV_TUNER = (1 << 8);

    /**
     * Specifies a second composite video port.
     */
    public static final int COMPOSITE_VIDEO_2 = (1 << 9);
    
    /**
     * Set the enabled ports.  The enabled ports are given as the logical "OR"
     * of the bit mask value of the desired ports.
     * <p>
     * For example, to enable microphone and line in,
     * use <code>setPorts(MICROPHONE | LINE_IN)</code>.
     *
     * @param ports the logical "OR" of the bit mask value of the ports.
     * @return the integer bit mask of the all the ports that are enabled.
     */
    public int setPorts(int ports);


    /**
     * Obtains the set of ports currently enabled.
     * @return the set of enabled ports.  The value returned is the logical
     * "OR" of the bit mask value of the enabled ports.
     */
    public int getPorts();


    /**
     * Obtains the set of ports supported by the device that is controlled
     * by this <code>PortControl</code> object.
     * @return the set of supported ports.  The value returned is the logical
     * "OR" of the bit mask value of the enabled ports.
     */
    public int getSupportedPorts();
}


