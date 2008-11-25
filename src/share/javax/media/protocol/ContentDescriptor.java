/*
 * @(#)ContentDescriptor.java	1.16 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

import javax.media.Format;

/**
 * A <CODE>ContentDescriptor</CODE> identifies media data containers.
 *
 * @see SourceStream
 * @see javax.media.Format
 * @since JMF 1.0, extends Format in JMF 2.0
 */

public class ContentDescriptor extends Format {

    /**
     * RAW content type. This signifies content that's contained in
     * individual buffers of type <code>Buffer</code> and carries any
     * format of media as specified by the <code>format</code> attribute
     * of the buffer.
     * @see javax.media.Buffer
     * @see javax.media.Format
     * @see javax.media.protocol.PullBufferDataSource
     * @see javax.media.protocol.PushBufferDataSource
     */
    static public final String RAW	 	= "raw";

    /**
     * RAW RTP content type. This is similar to the RAW content type
     * but only carries buffers that contain packetized data formats
     * supported by RTP. 
     */
    static public final String RAW_RTP          = "raw.rtp";

    /**
     * Mixed content type.  This signifies content that contains
     * other elementary content types.  e.g., when more than one
     * <code>DataSources</code> is merged to formed one 
     * <code>DataSource</code>, the resulting <code>DataSource</code>
     * may contain mixed contents.
     */
    static public final String MIXED		= "application.mixed-data";

    /** Unknown content type */
    static public final String CONTENT_UNKNOWN 	= "UnknownContent";


    /**
     * Obtain a string that represents the content-name
     * for this descriptor.
     *
     * @return The content-type name.
     */
    public String getContentType() {
	return getEncoding();
    }

    /** 
     * Create a content descriptor with the specified name.
     * <p>
     * To create a <CODE>ContentDescriptor</CODE> from a MIME type, use
     * the <code>mimeTypeToPackageName</code> static member.
     *
     * @param cdName The name of the content-type.
     */
    public ContentDescriptor(String cdName) {
	super(cdName);
    }

    /**
     * Returns a string which describes the format parameters.
     * @return a string which describes the format parameters.
     */
    public String toString() {
	if (encoding.equalsIgnoreCase(RAW))
	    return "RAW";
	else if (encoding.equalsIgnoreCase(RAW_RTP))
	    return "RAW/RTP";
	else if (encoding.equalsIgnoreCase("audio.cdaudio"))
	    return "CD Audio";
	else
	    return encoding;	
    }

    /**
     * Map a MIME content-type to an equivalent string
     * of class-name components.
     * <p>
     * The MIME type is mapped to a string by:
     * <ol>
     * <li>Replacing all slashes with a period.
     * <li>Converting all alphabetic characters to lower case.
     * <li>Converting all non-alpha-numeric characters other than periods
     * to underscores (_).
     * </ol>
     * <p>
     * For example, "text/html" would
     * be converted to "text.html"
     *
     * @param mimeType The MIME type to map to a string.
     */
    static final public  String mimeTypeToPackageName(String mimeType) {

	if (mimeType == null)
	    return null;

	// All to lower case ...
	mimeType = mimeType.toLowerCase();

	// ... run through each char and convert
	//            '/'                ->  '.'
	//             !([A-Za-z0--9])   ->  '_'
	int len = mimeType.length();
	char nm[] = new char[len];
	mimeType.getChars(0, len, nm, 0);
	for (int i = 0; i < len; i++) {
	    char c = nm[i];
	    if (c == '/') {
		nm[i] = '.';
	    } else if (!(c == '.' ||
			'A' <= c && c <= 'Z' ||
		        'a' <= c && c <= 'z' ||
		        '0' <= c && c <= '9')) {
		nm[i] = '_';
	    }
	}
	
	return new String(nm);
    }

}
