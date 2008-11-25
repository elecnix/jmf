/*
 * @(#)FormatChangeEvent.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package javax.media.format;

/**
 *
 * A <CODE>FormatChangeEvent</CODE> is generated when the <CODE>Format</CODE> 
 * of an object changes.
 *
 * @version 1.4, 98/06/23.
 */
import javax.media.Controller;
import javax.media.Format;

    /** 
     * A <CODE>FormatChangeEvent</CODE> is posted by a <code>Controller</code> when
     * the <CODE>Format</CODE> of its media changes.
     */

public class FormatChangeEvent extends javax.media.ControllerEvent {

    protected Format oldFormat;
    protected Format newFormat;

    /** 
     * Constructs a <CODE>FormatChangeEvent</CODE>.
     * @param source The <CODE>Controller</CODE> that generated this event.
     */
    public FormatChangeEvent(Controller source) {
	super(source);
    }
     
    /**
     * Constructs a <CODE>FormatChangeEvent</CODE>, indicating the old <CODE>Format</CODE> and the
     * new <CODE>Format</CODE>. 
     * @param source  The <CODE>Controller</CODE> that generated this event.
     * @param oldFormat  The <code>Controller</code> object's old <CODE>Format</CODE>.
     * @param newFormat  The <code>Controller</code> object's new <CODE>Format</CODE>.
     */
    public FormatChangeEvent(Controller source, Format oldFormat, Format newFormat) {
	super(source);
	this.oldFormat = oldFormat;
	this.newFormat = newFormat;
    }

    /**
     * Gets the object's old <CODE>Format</CODE>.
     * @return A <CODE>Format</CODE> that describes the <code>Controller</code> object's original format.
     */
    public Format getOldFormat() {
	return oldFormat;
    }

    /**
     * Gets the object's new <CODE>Format</CODE>.
     * @return A <CODE>Format</CODE> that describes the <code>Controller</code> object's new format.
     */
    public Format getNewFormat() {
	return newFormat;
    }
}
