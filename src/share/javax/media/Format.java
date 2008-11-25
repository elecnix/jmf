/*
 * @(#)Format.java	1.40 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import java.lang.Class;

/**
 * A <code>Format</code> abstracts an exact media format.
 * It carries no encoding-specific parameters 
 * or timing information global to the presentation.
 * <p>
 * <h3> Comparing different formats </h3>
 * Not all of the attributes in a <code>Format</code> object have to be specified.
 * This enables selected attributes to be specified, making it possible to 
 * locate a supported <CODE>Format</CODE> that meets certain requirements without needing to
 * find an exact match. 
 * <p>
 * Two methods are provided for comparing <code>Formats</code>.
 * The <code>equals</code> method returns <CODE>true</CODE> if two <CODE>Format</CODE> 
 * objects are exactly the same--they're the same type and all of their attributes are the
 * same.  The <code>matches</code> method relaxes the comparison, comparing
 * only the attributes that are explicitly specified in the <CODE>Format</CODE> you are comparing.
 * @since JMF 2.0
 */

public class Format implements java.lang.Cloneable, java.io.Serializable {

    public static final int NOT_SPECIFIED = -1;
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    
    protected String encoding;

    /**
     * The  data object required by the <CODE>Format</CODE> is an integer array.
     */
    public static final Class intArray = (new int[0]).getClass();

    /**
     * The data object required by the <CODE>Format</CODE> is a short array.
     */
    public static final Class shortArray = (new short[0]).getClass();

    /**
     * The data object required by the <CODE>Format</CODE> is a byte array.
     */
    public static final Class byteArray = (new byte[0]).getClass();

    /**
     * The data object required by the <CODE>Format</CODE> is an array of <CODE>Format</CODE> objects.
     */
    public static final Class formatArray = (new Format[0]).getClass();
    
    protected Class dataType = byteArray;
    protected Class clz = getClass();	// Cache the to optimize on
					// equals, matches & intersect.

    private long encodingCode = 0;
    
    /**
     * Constructs a <CODE>Format</CODE> that has the specified encoding type.
     * @param encoding A <CODE>String</CODE> that contains the encoding 
     * type of the <CODE>Format</CODE> to be constructed.
     */
    public Format(String encoding) {
	this.encoding = encoding;	
    }

    /**
     * Constructs a <CODE>Format</CODE> that has the specified encoding and data types.
     * @param encoding A <CODE>String</CODE> that contains the encoding 
     * type of the <CODE>Format</CODE> to be constructed.
     * @param dataType The type of data object required by the <CODE>Format</CODE> to be constructed,
     * such as:
     * <CODE>byteArray</CODE>, <CODE>intArray</CODE>, or <CODE>shortArray</CODE>.
     * For example, for a byte array the data type would be "<CODE>Format.byteArray</CODE>".
     */
    public Format(String encoding, Class dataType) {
	this(encoding);
	this.dataType = dataType;
    }

    /**
     * Gets the uniquely-qualified encoding name for this <CODE>Format</CODE>.
     * <p>
     * In the reference implementation of JMF, these strings follow the QuickTime 
     * codec strings.
     *
     * @return The encoding of the <CODE>Format</CODE>. 
     */
    public String getEncoding() {
	return encoding;
    }

    /**
     * Gets the type of the data that this <CODE>Format</CODE> requires.
     * For example, for byte array it returns "<CODE>byte[].class</CODE>".
     * @return The data type of this <CODE>Format</CODE>.
     */
    public Class getDataType() {
	return dataType;
    }

    /**
     * Checks whether or not the specified <CODE>Format</CODE> is the same as this <CODE>Format</CODE>.
     * To be equal, the two <CODE>Formats</CODE> must be of the same type and all of their attributes must be the same.
     * @param format The <CODE>Format</CODE> to compare with this one.
     * @return <CODE>true</CODE> if the specified <CODE>Format</CODE> is the same as this one, <CODE>false</CODE> if it is not.
     */
    public boolean equals(Object format) {
	if (format == null || clz != ((Format)format).clz)
	    return false;
	
	String otherEncoding = ((Format)format).encoding;
	Class otherType = ((Format)format).dataType;
	
	return (dataType == otherType) &&
	    (encoding == otherEncoding ||
	     ((encoding != null && otherEncoding != null) &&
	      isSameEncoding((Format)format)));
    }

    /**
     * Checks whether or not the specified <CODE>Format</CODE> <EM>matches</EM> this <CODE>Format</CODE>.
     * Matches only compares the attributes that are defined in the specified <CODE>Format</CODE>, 
     * unspecified attributes are ignored.
     * <p>
     * The two <CODE>Format</CODE> objects do not have to be of the same class to 
     * match.  For example, if "A" are "B" are being compared, a
     * match is possible if "A" is derived from "B"
     * or "B" is derived from "A". (The compared attributes must still match, or <CODE>matches</CODE> fails.)  
     * @param format The <CODE>Format</CODE> to compare with this one.
     * @return <CODE>true</CODE> if the specified <CODE>Format</CODE> matches this one, <CODE>false</CODE> if it does not.
     */
    public boolean matches(Format format) {
	if( format == null) return false;
		
	return	(format.encoding == null || encoding == null ||
		 isSameEncoding(format)) &&
		(format.dataType == null || dataType == null ||
		 format.dataType == dataType) &&
		(clz.isAssignableFrom(format.clz) ||
		 format.clz.isAssignableFrom(clz));
    }

    /**
     * Intersects the attributes of this format and the specified format to create
     * a new <CODE>Format</code> object. The two objects being intersected should either be of the
     * same type or one should be a subclass of the other. The resulting object will be
     * the same type as the subclass. <P>
     * Common attributes are intersected as follows: If both objects have NOT_SPECIFIED
     * values for an attribute, the result will also have a NOT_SPECIFIED value. If one
     * of them has a NOT_SPECIFIED value then the result will have the value that is
     * specified in the other object. If both objects have specified values then the value
     * in this object will be used. <P>
     * Attributes that are specific to the subclass will be carried forward to the result.
     * @param other The <CODE>Format</CODE> object to intersect with this 
     * <CODE>Format</CODE>.
     * @return A <CODE>Format</CODE> object 
     * with its attributes set to those attributes common to both <CODE>Format</CODE> objects. 
     * @see #matches
     */
    public Format intersects(Format other) {
	Format res;
	if (clz.isAssignableFrom(other.clz))
	    res = (Format)other.clone();
	else if (other.clz.isAssignableFrom(clz))
	    res = (Format)clone();
	else
	    return null;
	if (res.encoding == null)
	    res.encoding = (encoding != null ? encoding : other.encoding);
	if (res.dataType == null)
	    res.dataType = (dataType != null ? dataType : other.dataType);
	return res;
    }

    /**
     * Checks if the encodings of both format objects are the same. Its
     * faster than calling String.equalsIgnoreCase to compare the two
     * encodings.
     * @return true if the encodings are the same, false otherwise.
     */
    public boolean isSameEncoding(Format other) {
	if (encoding == null || other == null || other.encoding == null)
	    return false;
	// Quick checks
	if (encoding == other.encoding)
	    return true;
	if (encodingCode > 0 && other.encodingCode > 0)
	    return encodingCode == other.encodingCode;
	
	// Works faster only for shorter strings of 10 chars or less.
	if (encoding.length() > 10)
	    return encoding.equalsIgnoreCase(other.encoding);
	if (encodingCode == 0) {
	    encodingCode = getEncodingCode(encoding);
	}
	// If the encoding code cannot be computed (out of bounds chars)
	// or in the off chance that its all spaces.
	if (encodingCode <= 0)
	    return encoding.equalsIgnoreCase(other.encoding);
	
	if (other.encodingCode == 0)
	    return other.isSameEncoding(this);
	else
	    return encodingCode == other.encodingCode;
    }

    /**
     * Checks if the encoding of this format is same as the parameter. Its
     * faster than calling String.equalsIgnoreCase to compare the two
     * encodings.
     * @return true if the encodings are the same, false otherwise.
     */
    public boolean isSameEncoding(String encoding) {
	if (this.encoding == null || encoding == null)
	    return false;
	// Quick check
	if (this.encoding == encoding)
	    return true;
	// Works faster only for shorter strings of 10 chars or less.
	if (this.encoding.length() > 10)
	    return this.encoding.equalsIgnoreCase(encoding);
	// Compute encoding code only once
	if (encodingCode == 0) {
	    encodingCode = getEncodingCode(this.encoding);
	}
	// If the encoding code cannot be computed (out of bounds chars)
	if (encodingCode < 0)
	    return this.encoding.equalsIgnoreCase(encoding);
	long otherEncodingCode = getEncodingCode(encoding);
	return encodingCode == otherEncodingCode;
    }

    private long getEncodingCode(String enc) {
 	byte chars[] = enc.getBytes();
	byte b;
	long code = 0;
	for (int i = 0; i < enc.length(); i++) {
	    b = chars[i];
	    if (b > 96 && b < 123)
		b -= 32; // lower to upper
	    b -= 32;
	    if (b > 63)
		return -1;
	    code = (code << 6) | (long) b;
	}
	return code;
    }
    
    /**
     * Generate a format that's less restrictive than this format but
     * contains the basic attributes that will make this resulting format 
     * useful for format matching.
     * @return A <CODE>Format</CODE> that's less restrictive than the
     * this format.
     */
    public Format relax() {
	return (Format)clone();
    }

    /**
     * Creates a clone of this <CODE>Format</CODE>.
     * @return A clone of this format.
     */
    public Object clone() {
	Format f = new Format(encoding);
	f.copy(this);
	return f;
    }

    /**
     * Copies the attributes from  the specified <CODE>Format</CODE> into this <CODE>Format</CODE>.
     * @param f The <CODE>Format</CODE> to copy the attributes from.
     */
    protected void copy(Format f) {
	dataType = f.dataType;
    }

    /**
     * Gets a <CODE>String</CODE> representation of the <CODE>Format</CODE> attributes.
     * For example: "PCM, 44.1 KHz, Stereo, Signed".
     * @return A <CODE>String</CODE> that describes the <CODE>Format</CODE> attributes.
     */
    public String toString() {
	return getEncoding();
    }
}
