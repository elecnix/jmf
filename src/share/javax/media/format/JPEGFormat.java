/*
 * @(#)JPEGFormat.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.format;

import javax.media.Format;
import javax.media.format.VideoFormat;

import java.awt.Dimension;


/**
 * Describes JPEG compressed video data.
 * 
 *
 * @since JMF 2.0
 */

public class JPEGFormat extends VideoFormat {

    /**
     * JPEG 422 decimation.
     */
    static final public int DEC_422 = 0;

    /**
     * JPEG 420 decimation.
     */
    static final public int DEC_420 = 1;

    /**
     * JPEG 444 decimation.
     */
    static final public int DEC_444 = 2;

    /**
     * JPEG 402 decimation.
     */
    static final public int DEC_402 = 3;

    /**
     * JPEG 411 decimation.
     */
    static final public int DEC_411 = 4;

    /** 
     * JPEG encoding parameter quality factor.
     **/
    int qFactor = NOT_SPECIFIED;

    /** 
     * Indicates whether or not JPEG decimation is used. 
     **/
    int decimation = NOT_SPECIFIED;


    /**
     * Constructs a default <CODE>JPEGFormat</CODE> object.
     */
    public JPEGFormat() {
	super(VideoFormat.JPEG);
    }

    /**
     * Constructs a <CODE>JPEGFormat</CODE>  with the specified parameters.
     *
     * @param size A <CODE>Dimension</CODE> that specifies the frame size.
     * @param maxDataLength The maximum size of the compressed data array.
     * @param dataType The class type of the data.
     * @param frameRate The frame rate of the video.
     * @param q The JPEG quality factor.
     * This is a value from 0 to 100, where 0 is the lowest
     * quality and 100 is the highest.
     * @param dec The JPEG decimation used.
     */
    public JPEGFormat(Dimension size, int maxDataLength,
		       Class dataType, float frameRate, int q, int dec) {
	super(VideoFormat.JPEG, size, maxDataLength, dataType, frameRate);
	this.qFactor = q;
	this.decimation = dec;
    }

    /**
     * Gets the JPEG quality factor for this <CODE>JPEGFormat</CODE>.
     * @return An integer in the range 0 to 100, where 0 is the lowest
     * quality and 100 is the highest.
     */
    public int getQFactor() {
	return qFactor;
    }

    /**
     * Gets the decimation of the video.
     * @return An integer that indicates the decimation of this
     * <code>Format</code>:
       DEC_422, DEC_420, DEC_444, DEC_402, or DEC_411.
     */
    public int getDecimation() {
	return decimation;
    }

    /**
     * Creates a clone of this <CODE>JPEGFormat</CODE> by copying each format
     * attribute to the clone.
     * @return A clone of this <CODE>JPEGFormat</CODE>.
     */
    public Object clone() {
	JPEGFormat f = new JPEGFormat(getSize(), getMaxDataLength(),
				getDataType(), getFrameRate(),
				qFactor, decimation);
	f.copy(this);
	return f;
    }

    /**
     * Copies  the attributes from the specified <CODE>Format</CODE> into 
     * this <CODE>JPEGFormat</CODE>.
     * @param f The <CODE>Format</CODE> to copy the attributes from.     
     */
    protected void copy(Format f) {
	super.copy(f);
	JPEGFormat jf = (JPEGFormat)f;
	qFactor = jf.qFactor;
	decimation = jf.decimation;
    }

    /**
     * Gets a <CODE>String</CODE> representation of the attributes of this 
     * <code>JPEGFormat</code>. For example: "JPEG, 352x240, ...".
     * @return A <CODE>String</CODE> that describes the <code>JPEGFormat</code> 
     * attributes.
     */
    public String toString() {
	String s = getEncoding() + " video format:";
	if (size != null)
	    s += " size = " + size.width + "x" + size.height;
	if (frameRate != NOT_SPECIFIED)
	    s += " FrameRate = " + frameRate;
	if (maxDataLength != NOT_SPECIFIED)
	    s += " maxDataLength = " + maxDataLength;
	if (dataType != null)
	    s += " dataType = " + dataType;
	if (qFactor != NOT_SPECIFIED)
	    s += " q factor = " + qFactor;
	if (decimation != NOT_SPECIFIED)
	    s += " decimation = " + decimation;
	return s;
    }

    /**
     * Compares the specified <CODE>Format</CODE> with this <code>JPEGFormat</code>. 
     * Returns <CODE>true</CODE> 
     * only if the specified <CODE>Format</CODE> is an <CODE>JPEGFormat</CODE> and 
     * all of its attributes are
     * identical to this <code>JPEGFormat</code>.
     * @param format  The <CODE>Format</CODE> to compare with this one.
     * @return <CODE>true</CODE> if the specified <CODE>Format</CODE> is the same, 
     * <CODE>false</CODE> if it is not.
     */
    public boolean equals(Object format) {
	if (format instanceof JPEGFormat) {
	    JPEGFormat vf = (JPEGFormat)format;

	    return super.equals(format) &&
		qFactor == vf.qFactor &&
		decimation == vf.decimation;
	}
	return false;
    }

    /**
     * Checks whether or not the specified <CODE>Format</CODE> <EM>matches</EM> 
     * this <CODE>JPEGFormat</CODE>.
     * Matches only compares the attributes that are defined in the specified 
     * <CODE>Format</CODE>,  unspecified attributes are ignored.
     * <p>
     * The two <CODE>Format</CODE> objects do not have to be of the same class to 
     * match.  For example, if "A" are "B" are being compared, a
     * match is possible if "A" is derived from "B"
     * or "B" is derived from "A". (The compared attributes must still match, or 
     * <CODE>matches</CODE> fails.)  
     * @param format The <CODE>Format</CODE> to compare with this one.
     * @return <CODE>true</CODE> if the specified <CODE>Format</CODE> matches this one, 
     * <CODE>false</CODE> if it does not.
     */
    public boolean matches(Format format) {
	if (!super.matches(format))
	    return false;
	if (!(format instanceof JPEGFormat))
	    return true;

	JPEGFormat vf = (JPEGFormat)format;

	return 
	    (qFactor == NOT_SPECIFIED || vf.qFactor == NOT_SPECIFIED ||
	     qFactor == vf.qFactor) &&
	    (decimation == NOT_SPECIFIED || vf.decimation == NOT_SPECIFIED ||
	     decimation == vf.decimation);
    }

    /**
     * Finds the attributes shared by two matching <CODE>Format</CODE> objects.
     * If the specified <CODE>Format</CODE> does not match this one, the result is
     * undefined.  
     * @param The matching <CODE>Format</CODE> to intersect with this 
     * <CODE>JPEGFormat</CODE>.
     * @return A <CODE>Format</CODE> object
     * with its attributes set to those attributes common to both 
     * <CODE>Format</CODE> objects. 
     * @see #matches
     */
    public Format intersects(Format format) {
	Format fmt;
	if ((fmt = super.intersects(format)) == null)
	    return null;
	if (!(format instanceof JPEGFormat))
	    return fmt;
	JPEGFormat other = (JPEGFormat)format;
	JPEGFormat res = (JPEGFormat)fmt;
	res.qFactor = (qFactor != NOT_SPECIFIED ?
			 qFactor : other.qFactor);
	res.decimation = (decimation != NOT_SPECIFIED ?
			 decimation : other.decimation);
	return res;
    }
}

