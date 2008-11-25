/*
 * @(#)H261Format.java	1.18 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.format;

import javax.media.Format;

import java.awt.Dimension;

/**
 * Describes H.261 compressed video data.
 *
 * @since JMF 2.0
 */

public class H261Format extends VideoFormat {

    /**
     * Used to indicate whether or not still image transmission is used.
     * Can be set to NOT_SPECIFIED, TRUE, or FALSE.
     */
    protected int stillImageTransmission = NOT_SPECIFIED;

    /** the video encoding string **/
    private static String ENCODING = "h261";

    /**
     * Constructs an <CODE>H261Format</CODE> object with default parameters.
     */
    public H261Format() {
 	super(ENCODING);
    }

    /**
     * Constructs an <CODE>H261Format</CODE> object with the specified parameters.
     *
     * @param size A <CODE>Dimension</CODE> that specifies the frame size.
     * @param maxDataLength The maximum size of the compressed data array.
     * @param dataType The class type of the data.
     * @param frameRate The frame rate of the video.
     * @param stillImageTransmission Specifies whether or not H.261 still image
     * transmission is used (H.261 Annex D).
     */
    public H261Format(Dimension size, int maxDataLength, Class dataType,
		      float frameRate,
		      int stillImageTransmission) {
	super(ENCODING, size, maxDataLength, dataType, frameRate);
	this.stillImageTransmission = stillImageTransmission;
    }

    /**
     * Gets the still image transmission setting for this <code>Format</code>.
     * @return An integer that indicates whether or not still image
     * transmission is used: TRUE, FALSE, or NOT_SPECIFIED. 
     */
    public int getStillImageTransmission() {
	return stillImageTransmission;
    }

    /**
     * Creates a clone of this <CODE>H261Format</CODE>.
     * @return A clone of this <CODE>H261Format</CODE>.
     */
    public Object clone() {
	H261Format f = new H261Format();
	f.copy(this);
	return f;
    }

    /**
     * Copies  the attributes from the specified <CODE>Format</CODE> into 
     * this <CODE>H261Format</CODE>.
     * @param f The <CODE>Format</CODE> to copy the attributes from.     
     */
    protected void copy(Format f) {
	super.copy(f);
	stillImageTransmission = ((H261Format)f).stillImageTransmission;
    }

    /**
     * Gets a <CODE>String</CODE> representation of the attributes of this 
     * <CODE>H261Format</CODE>.
     * For example: "H261, 352x240, ...".
     * @return A <CODE>String</CODE> that describes the format attributes.
     */
    public String toString() {
	return "H.261 video format";
    }

    /**
     * Compares the specified <CODE>Format</CODE> with this <code>H261Format</code>. 
     * Returns <CODE>true</CODE> only if the specified <CODE>Format</CODE>
     * is a <CODE>H261Format</CODE> object and all of 
     * its attributes are identical to 
     * the attributes in this <code>H261Format</code>.
     * @param format  The <CODE>Format</CODE> to compare.
     * @return true if the specified <CODE>Format</CODE> is the same as this one.
     */
    public boolean equals(Object format) {
	if (format instanceof H261Format) {
	    return super.equals(format) &&
		stillImageTransmission == ((H261Format)format).stillImageTransmission;
	}
	return false;
    }

    /**
     * Checks whether or not the specified <CODE>Format</CODE> <EM>matches</EM> 
     * this <CODE>H261Format</CODE>.
     * Matches only compares the attributes that are defined in the specified 
     * <CODE>Format</CODE>, 
     * unspecified attributes are ignored.
     * <p>
     * The two <CODE>Format</CODE> objects do not have to be of the same class 
     * to match.  For example, if "A" are "B" are being compared, a
     * match is possible if "A" is derived from "B"
     * or "B" is derived from "A". (The compared attributes must still match, 
     * or <CODE>matches</CODE> fails.)  
     * @param format The <CODE>Format</CODE> to compare with this one.
     * @return <CODE>true</CODE> if the specified <CODE>Format</CODE> 
     * matches this one, <CODE>false</CODE> if it does not.
     */
    public boolean matches(Format format) {
	if (!super.matches(format))
	    return false;
	if (!(format instanceof H261Format))
	    return true;

	H261Format f = (H261Format)format;

	return
	    (stillImageTransmission == NOT_SPECIFIED || f.stillImageTransmission == NOT_SPECIFIED ||
	     stillImageTransmission == f.stillImageTransmission);
    }

    /**
     * Finds the attributes shared by two matching <CODE>Format</CODE> objects.
     * If the specified <CODE>Format</CODE> does not match this one, the result 
     * is undefined.  
     * @param The matching <CODE>Format</CODE> to intersect with this 
     * <CODE>H261Format</CODE>.
     * @return A <CODE>Format</CODE> object 
     * with its attributes set to those attributes common to both 
     * <CODE>Format</CODE> objects. 
     * @see #matches
     */
    public Format intersects(Format format) {
	Format fmt;
	if ((fmt = super.intersects(format)) == null)
	    return null;
	if (!(format instanceof H261Format))
	    return fmt;
	H261Format other = (H261Format)format;
	H261Format res = (H261Format)fmt;
	res.stillImageTransmission = (stillImageTransmission != NOT_SPECIFIED ?
				      stillImageTransmission : other.stillImageTransmission);

	return res;
    }
}

