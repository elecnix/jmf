/*
 * @(#)IndexedColorFormat.java	1.15 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.format;

import java.awt.Dimension;
import javax.media.Format;

/**
 * Describes indexed color video data.
 * 
 * @since JMF 2.0
 */
public class IndexedColorFormat extends VideoFormat {

    /** Increment value of the array index from the first pixel on line n
      to the first pixel on line n+1. */
    protected int lineStride = NOT_SPECIFIED;

    protected byte [] redValues = null;
    protected byte [] greenValues = null;
    protected byte [] blueValues = null;

    protected int mapSize = NOT_SPECIFIED;
    
    private static String ENCODING = VideoFormat.IRGB;

  /**
   * Constructs an <CODE>IndexedColorFormat</CODE> object with the specified parameters.
   * @param size  A <CODE>Dimension</CODE> that specifies the frame size.
   * @param maxDataLength  The maximum size of a data chunk.
   * @param dataType  The type of data. For example, byte array.
   * @param frameRate  The frame rate.
   * @param lineStride  The number elements between adjacent rows of pixels.
   * @param mapSize   The number of bits required per pixel.
   * @param red   The mask for the red component.
   * @param green  The mask for the green component.
   * @param blue  The mask for the blue component.
   */
    public IndexedColorFormat(Dimension size, int maxDataLength, Class dataType,
			      float frameRate,
			      int lineStride,
			      int mapSize,
			      byte [] red, byte [] green, byte [] blue) {
	super(ENCODING, size, maxDataLength, dataType, frameRate);
	this.lineStride = lineStride;
	this.redValues = red;
	this.greenValues = green;
	this.blueValues = blue;
	this.mapSize = mapSize;
    }

    /** 
     * Gets the number of bits required per pixel.
     * @return An integer representing the number of bits per pixel.
     */
    public int getMapSize() {
	return mapSize;
    }

    /** 
     * Gets the mask for the red component. 
     * @return A byte array containing the mask for the red component.
     */
    public byte [] getRedValues() {
	return redValues;
    }

    /** 
     * Gets the mask for the green component. 
     * @return A byte array containing the mask for the green component.
     */
    public byte [] getGreenValues() {
	return greenValues;
    }

    /** 
     * Gets the mask for the blue component. 
     * @return A byte array containing the mask for the blue component.
     */
    public byte [] getBlueValues() {
	return blueValues;
    }

    /** 
     * Gets the line stride--the number of array elements between adjacent rows of pixels. 
     * @return An integer representing the line stride. 
     */
    public int getLineStride() {
	return lineStride;
    }

    /**
     * Creates a clone of this <CODE>IndexedColorFormat</CODE> by copying each format
     * attribute to the clone.
     * @return A clone of this <CODE>IndexedColorFormat</CODE>.
     */
    public Object clone() {
	IndexedColorFormat f = new IndexedColorFormat(size,
						      maxDataLength,
						      dataType,
						      frameRate,
						      lineStride,
						      mapSize,
						      redValues,
						      greenValues,
						      blueValues);
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
	if (f instanceof IndexedColorFormat) {
	    IndexedColorFormat other = (IndexedColorFormat) f;
	    mapSize = other.mapSize;
	    redValues = other.redValues;
	    greenValues = other.greenValues;
	    blueValues = other.blueValues;
	    lineStride = other.lineStride;
	}
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
	if (format instanceof IndexedColorFormat) {
	    IndexedColorFormat other = (IndexedColorFormat) format;
	    
	    return super.equals(format) &&
		mapSize == other.mapSize &&
		redValues == other.redValues &&
		greenValues == other.greenValues &&
		blueValues == other.blueValues &&
		lineStride == other.lineStride;
	} else
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
	if (!(format instanceof IndexedColorFormat))
	    return true;

	IndexedColorFormat other = (IndexedColorFormat) format;
	
	return 
	    (mapSize == NOT_SPECIFIED || other.mapSize == NOT_SPECIFIED ||
	     mapSize == other.mapSize) &&
	    
	    (redValues == null || other.redValues == null ||
	     redValues.equals(other.redValues)) &&
	    (greenValues == null || other.greenValues == null ||
	     greenValues.equals(other.greenValues)) &&
	    (blueValues == null || other.blueValues == null ||
	     blueValues.equals(other.blueValues)) &&
	    
	    (lineStride == NOT_SPECIFIED || other.lineStride == NOT_SPECIFIED ||
	     lineStride == other.lineStride) ;
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
	if (!(format instanceof IndexedColorFormat))
	    return fmt;
	IndexedColorFormat other = (IndexedColorFormat)format;
	IndexedColorFormat res = (IndexedColorFormat)fmt;
	res.mapSize = (mapSize != NOT_SPECIFIED ?
		       mapSize : other.mapSize);
	res.redValues = (redValues != null ?
			 redValues : other.redValues);
	res.greenValues = (greenValues != null ?
			 greenValues : other.greenValues);
	res.blueValues = (blueValues != null ?
			 blueValues : other.blueValues);

	res.lineStride = (lineStride != NOT_SPECIFIED ?
			  lineStride : other.lineStride);
	
	return res;
    }


    /**
     * Generate a format that's less restrictive than this format but
     * contains the basic attributes that will make this resulting format 
     * useful for format matching.
     * @return A <CODE>Format</CODE> that's less restrictive than the
     * this format.
     */
    public Format relax() {
	IndexedColorFormat fmt;
	if ((fmt = (IndexedColorFormat)super.relax()) == null)
	    return null;

	fmt.lineStride = NOT_SPECIFIED;

	return fmt;
    }

}
