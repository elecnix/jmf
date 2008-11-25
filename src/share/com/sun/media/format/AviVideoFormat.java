/*
 * @(#)AviVideoFormat.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.format;

import java.awt.Dimension;
import javax.media.Format;
import javax.media.format.VideoFormat;

/**
 * An extended video format class that contains additional information from
 * the video header of an AVI file. This includes codec specific information
 * required by certain codecs.
 */
public class AviVideoFormat extends VideoFormat {

    protected int planes        = NOT_SPECIFIED;
    protected int bitsPerPixel  = NOT_SPECIFIED;
    protected int imageSize     = NOT_SPECIFIED;
    protected int xPelsPerMeter = NOT_SPECIFIED;
    protected int yPelsPerMeter = NOT_SPECIFIED;
    protected int clrUsed       = NOT_SPECIFIED;
    protected int clrImportant  = NOT_SPECIFIED;

    protected byte [] codecSpecificHeader = null;

    public AviVideoFormat(String encoding) {
	super(encoding);
    }
    
    public AviVideoFormat(String encoding, Dimension size, int maxDataLength, 
			  Class dataType, float frameRate, int planes, int bitsPerPixel,
			  int imageSize, int xPelsPerMeter, int yPelsPerMeter,
			  int clrUsed, int clrImportant, byte[] codecHeader) {

	super(encoding, size, maxDataLength, dataType, frameRate);
	
	this.planes = planes;
	this.bitsPerPixel = bitsPerPixel;
	this.imageSize = imageSize;
	this.xPelsPerMeter = xPelsPerMeter;
	this.yPelsPerMeter = yPelsPerMeter;
	this.clrUsed = clrUsed;
	this.clrImportant = clrImportant;
	this.codecSpecificHeader = codecHeader;
    }

    public int getPlanes() {
	return planes;
    }

    public int getBitsPerPixel() {
	return bitsPerPixel;
    }
    
    public int getImageSize() {
	return imageSize;
    }

    public int getXPelsPerMeter() {
	return xPelsPerMeter;
    }

    public int getYPelsPerMeter() {
	return yPelsPerMeter;
    }

    public int getClrUsed() {
	return clrUsed;
    }

    public int getClrImportant() {
	return clrImportant;
    }

    public byte [] getCodecSpecificHeader() {
	return codecSpecificHeader;
    }

    /**
     * Return a clone of this format.
     */
    public Object clone() {
	AviVideoFormat f = new AviVideoFormat(encoding);
	f.copy(this);
	return f;
    }

    /**
     * Copy the attributes from the given object.
     */
    protected void copy(Format f) {
	super.copy(f);
	if (f instanceof AviVideoFormat) {
	    AviVideoFormat other = (AviVideoFormat) f;
	    planes = other.planes;
	    bitsPerPixel = other.bitsPerPixel;
	    imageSize = other.imageSize;
	    xPelsPerMeter = other.xPelsPerMeter;
	    yPelsPerMeter = other.yPelsPerMeter;
	    clrUsed = other.clrUsed;
	    clrImportant = other.clrImportant;
	    codecSpecificHeader = other.codecSpecificHeader;
	}
    }

    /**
     * @return True if the given format is the same as this one.
     */
    public boolean equals(Object format) {
	if (format instanceof AviVideoFormat) {
	    AviVideoFormat other = (AviVideoFormat) format;
	    
	    boolean result = super.equals(format) &&
		planes == other.planes &&
		bitsPerPixel == other.bitsPerPixel &&
		imageSize == other.imageSize &&
		xPelsPerMeter == other.xPelsPerMeter &&
		yPelsPerMeter == other.yPelsPerMeter &&
		clrUsed == other.clrUsed &&
		clrImportant == other.clrImportant;
	    if (result == false)
		return false;
	    else {
		if (codecSpecificHeader == other.codecSpecificHeader)
		    return true;
		if (codecSpecificHeader == null ||
		    other.codecSpecificHeader == null)
		    return false;
		if (codecSpecificHeader.length != other.codecSpecificHeader.length)
		    return false;
		for (int i = 0; i < codecSpecificHeader.length; i++)
		    if (codecSpecificHeader[i] != other.codecSpecificHeader[i])
			return false;
		return true;
	    }
	} else
	    return false;
    }

    /**
     * Test to see if the given format matches this format.
     * Matches compares attributes that are defined and ignore attributes
     * that are unspecified.
     * Two formats do not have to be of the same class to be considered
     * a match.  Say "A" are "B" are the two classes.  If "A" derives "B"
     * or "B" derives "A", then a match is possible (after comparing
     * individual attributes.  Otherwise, matches fails.  This is to
     * prevent matching VideoFormat and AudioFormat, for example.
     * @return true if the given format matches this one.
     */
    public boolean matches(Format format) {
	if (!super.matches(format))
	    return false;
	if (!(format instanceof AviVideoFormat))
	    return true;

	AviVideoFormat other = (AviVideoFormat) format;

	boolean returnVal = 
	    (planes == NOT_SPECIFIED || other.planes == NOT_SPECIFIED ||
	     planes == other.planes) &&
	    (bitsPerPixel == NOT_SPECIFIED || other.bitsPerPixel == NOT_SPECIFIED ||
	     bitsPerPixel == other.bitsPerPixel) &&
	    (imageSize == NOT_SPECIFIED || other.imageSize == NOT_SPECIFIED ||
	     imageSize == other.imageSize) &&
	    (xPelsPerMeter == NOT_SPECIFIED || other.xPelsPerMeter == NOT_SPECIFIED ||
	     xPelsPerMeter == other.xPelsPerMeter) &&
	    (yPelsPerMeter == NOT_SPECIFIED || other.yPelsPerMeter == NOT_SPECIFIED ||
	     yPelsPerMeter == other.yPelsPerMeter) &&
	    (clrUsed == NOT_SPECIFIED || other.clrUsed == NOT_SPECIFIED ||
	     clrUsed == other.clrUsed) &&
	    (clrImportant == NOT_SPECIFIED || other.clrImportant == NOT_SPECIFIED ||
	     clrImportant == other.clrImportant)
		&&
	    (codecSpecificHeader == null || other.codecSpecificHeader == null ||
	     codecSpecificHeader == other.codecSpecificHeader ||
	     codecSpecificHeader.equals(codecSpecificHeader));
	    
	return returnVal;
    }

    /**
     * Find the common attributes of two matching formats.
     * If the given format does not match this one, the result is
     * undefined.  Otherwise, it returns a format object 
     * with its attributes set to the common attributes of the two. 
     * @return a format object with its attributes set to the common
     * attributes of the two.
     * @see matches
     */
    public Format intersects(Format format) {
	Format fmt;
	if ((fmt = super.intersects(format)) == null)
	    return null;
	if (!(format instanceof AviVideoFormat))
	    return fmt;
	AviVideoFormat other = (AviVideoFormat)format;
	AviVideoFormat res = (AviVideoFormat)fmt;
	res.planes = (planes != NOT_SPECIFIED ?
		      planes : other.planes);
	res.bitsPerPixel = (bitsPerPixel != NOT_SPECIFIED ?
			    bitsPerPixel : other.bitsPerPixel);
	res.imageSize = (imageSize != NOT_SPECIFIED ?
			 imageSize : other.imageSize);
	res.xPelsPerMeter = (xPelsPerMeter != NOT_SPECIFIED ?
			     xPelsPerMeter : other.xPelsPerMeter);
	res.yPelsPerMeter = (yPelsPerMeter != NOT_SPECIFIED ?
			     yPelsPerMeter : other.yPelsPerMeter);
	res.clrUsed = (clrUsed != NOT_SPECIFIED ?
		       clrUsed : other.clrUsed);
	res.clrImportant = (clrImportant != NOT_SPECIFIED ?
			    clrImportant : other.clrImportant);
	res.codecSpecificHeader = (codecSpecificHeader != null ?
				   codecSpecificHeader : other.codecSpecificHeader);
	return res;
    }

    public Format relax() {
	AviVideoFormat fmt;
	if ((fmt = (AviVideoFormat)super.relax()) == null)
	    return null;

	fmt.imageSize = NOT_SPECIFIED;
	return fmt;
    }

    public String toString() {
	String s = super.toString() + " " + (codecSpecificHeader != null ? codecSpecificHeader.length : 0) + " extra bytes";
	return s;
    }    
}

