/*
 * @(#)H263Format.java	1.17 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.format;

import javax.media.Format;

import java.awt.Dimension;

/**
 * Describes
 * H.263 compressed video data.
 *
 * @since JMF 2.0
 */

public class H263Format extends VideoFormat {

    /** the video encoding string **/
    private static String ENCODING = "h263";
   /**
     * Indicates if advanced prediction is used.
     * Can take values NOT_SPECIFIED, TRUE, or FALSE.
     */
    protected int advancedPrediction = NOT_SPECIFIED;
   /**
     * Indicates if arithmetic coding is used.
     * Can take values NOT_SPECIFIED, TRUE, or FALSE.
     */
    protected int arithmeticCoding = NOT_SPECIFIED;
   /**
     * Indicates if error compensation is used.
     * Can take values NOT_SPECIFIED, TRUE, or FALSE.
     */
    protected int errorCompensation = NOT_SPECIFIED;
   /**
     * The size of Hypothetical Reference decoder buffer.
     */
    protected int hrDB = NOT_SPECIFIED;
   /**
     * Indicates if PB frames mode  is used in this bitstream.
     * Can take values NOT_SPECIFIED, TRUE, or FALSE.
     */
    protected int pbFrames = NOT_SPECIFIED;
   /**
     * Indicates if unrestricted motion estimation is used.
     * Can take values NOT_SPECIFIED, TRUE, or FALSE.
     */
    protected int unrestrictedVector = NOT_SPECIFIED;

    /**
     * Constructs an <CODE>H263Format</CODE> object with default attributes.
     */
    public H263Format() {
	super(ENCODING);
    }
    /**
     * Constructs an H263Format object with the specified attributes.
     *
     * @param size A <CODE>Dimension</CODE> that specifies the frame size.
     * @param maxDataLength The maximum size of the compressed data array.
     * @param dataType The class type of the data.
     * @param frameRate The frame rate of the video.
     * @param advancedPrediction Specifies whether or not the H.263
     *        advanced prediction capability (H.263 Annex F) is used.
     * @param arithmeticCoding Specifies whether or not the H.263
     *        arithmetic coding capability (H.263 Annex E) is used.
     * @param errorCompensation Specifies whether or not the H.263
     *        error compensation capability is used.
     * @param hrDB Specifies the H.263 Hypothetical Reference decoder buffer.
     * @param pbFrames Specifies whether or not the H.263
     *        PB Frames capability (H.263 Annex G) is used.
     * @param unrestrictedVector Specifies whether or not the H.263
     *        unrestricted motion vector capability (H.263 Annex D) is used.
     */
     public H263Format(Dimension size, int maxDataLength,
		      Class dataType,
		      float frameRate,
		      int advancedPrediction,
		      int arithmeticCoding,
		      int errorCompensation,
		      int hrDB,
		      int pbFrames,
		      int unrestrictedVector) {
	super(ENCODING, size, maxDataLength, dataType, frameRate);
	this.advancedPrediction = advancedPrediction;
	this.arithmeticCoding = arithmeticCoding;
	this.errorCompensation = errorCompensation;
	this.hrDB = hrDB;
	this.pbFrames = pbFrames;
	this.unrestrictedVector = unrestrictedVector;
    }

   /**
     * Gets the advanced prediction setting for this <CODE>Format</CODE>. 
     * @return An integer that indicates the advanced prediction setting: NOT_SPECIFIED, TRUE, or FALSE.
     */
    public int getAdvancedPrediction() {
	return advancedPrediction;
    }

   /**
     * Gets the arithmetic coding setting for this <CODE>Format</CODE>. 
     * @return An integer that indicates the arithmetic coding setting: NOT_SPECIFIED, TRUE, or FALSE.     
     */
    public int getArithmeticCoding() {
	return arithmeticCoding;
    }

   /**
     * Gets the error compensation setting for this <CODE>Format</CODE>. 
     * @return An integer that indicates the error compensation setting: NOT_SPECIFIED, TRUE, or FALSE.     
     */
    public int getErrorCompensation() {
	return errorCompensation;
    }
   /**
     * Gets the size of Hypothetical Reference decoder buffer.
     * @return The size of the Hypothetical Reference decoder buffer, as an integer. Returns
     * NOT_SPECIFIED if the decoder buffer size is not specified.
     */
    public int getHrDB() {
	return hrDB;
    }

   /**
     * Gets the PB frames setting for this <CODE>Format</CODE>. 
     * @return An integer that indicates the PB frames setting: NOT_SPECIFIED, TRUE, or FALSE.     
     */
    public int getPBFrames() {
	return pbFrames;
    }

   /**
     * Gets the unrestricted motion vector setting for this <CODE>Format</CODE>. 
     * @return An integer that indicates the unrestricted motion vector setting: NOT_SPECIFIED, TRUE, or FALSE.     
     */
    public int getUnrestrictedVector() {
	return unrestrictedVector;
    }

    /**
     * Gets a string representation of the attributes of this 
     * <CODE>H263Format</CODE>.
     * For example: "H.263, 352x240, ...".
     * @return A <CODE>String</CODE> that describes the format attributes.
     */
    public String toString() {
	return "H.263 video format";
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
	if (format instanceof H263Format) {
	    H263Format other = (H263Format) format;
	    return super.equals(format) &&
		advancedPrediction == other.advancedPrediction &&
		arithmeticCoding == other.arithmeticCoding &&
		errorCompensation == other.errorCompensation &&
		hrDB == other.hrDB &&
		pbFrames == other.pbFrames &&
		unrestrictedVector == other.unrestrictedVector;
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
	if (!(format instanceof H263Format))
	    return true;

	H263Format other = (H263Format) format;

	return
	    (advancedPrediction == NOT_SPECIFIED || other.advancedPrediction == NOT_SPECIFIED ||
	     advancedPrediction == other.advancedPrediction) &&
	    (arithmeticCoding == NOT_SPECIFIED || other.arithmeticCoding == NOT_SPECIFIED ||
	     arithmeticCoding == other.arithmeticCoding) &&
	    (errorCompensation == NOT_SPECIFIED || other.errorCompensation == NOT_SPECIFIED ||
	     errorCompensation == other.errorCompensation) &&
	    (hrDB == NOT_SPECIFIED || other.hrDB == NOT_SPECIFIED ||
	     hrDB == other.hrDB) &&
	    (pbFrames == NOT_SPECIFIED || other.pbFrames == NOT_SPECIFIED ||
	     pbFrames == other.pbFrames) &&
	    (unrestrictedVector == NOT_SPECIFIED || other.unrestrictedVector == NOT_SPECIFIED ||
	     unrestrictedVector == other.unrestrictedVector);
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
	if (!(format instanceof H263Format))
	    return fmt;
	H263Format other = (H263Format)format;
	H263Format res = (H263Format)fmt;
	res.advancedPrediction = (advancedPrediction != NOT_SPECIFIED ?
				 advancedPrediction : other.advancedPrediction);
	res.arithmeticCoding = (arithmeticCoding != NOT_SPECIFIED ?
				arithmeticCoding : other.arithmeticCoding);
	res.errorCompensation = (errorCompensation != NOT_SPECIFIED ?
				 errorCompensation : other.errorCompensation);
	res.hrDB = (hrDB != NOT_SPECIFIED ?
		    hrDB : other.hrDB);
	res.pbFrames = (pbFrames != NOT_SPECIFIED ?
			pbFrames : other.pbFrames);
	res.unrestrictedVector = (unrestrictedVector != NOT_SPECIFIED ?
				  unrestrictedVector : other.unrestrictedVector);
	
	return res;
    }

    /**
     * Creates a clone of this <CODE>H263Format</CODE>.
     * @return A clone of this <CODE>H263Format</CODE>.     
     */
    public Object clone() {
	H263Format f = new H263Format();
	f.copy(this);
	return f;
    }

    /**
     * Copies  the attributes from the specified <CODE>Format</CODE> into 
     * this <CODE>H263Format</CODE>.
     * @param f The <CODE>Format</CODE> to copy the attributes from.     
     */
    protected void copy(Format f) {
	super.copy(f);
	H263Format other = (H263Format) f;
	advancedPrediction = other.advancedPrediction;
	arithmeticCoding = other.arithmeticCoding;
	errorCompensation = other.errorCompensation;
	hrDB = other.hrDB;
	pbFrames = other.pbFrames;
	unrestrictedVector = other.unrestrictedVector;
    }

}

