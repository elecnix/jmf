/*
 * @(#)FileTypeDescriptor.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * This sub-class of ContentDescriptor enumerates those content descriptors
 * that are file formats. That is, data of this content type can be saved to
 * a file. It helps differentiate from content types that are not file formats.<P>
 * When a Processor advertises a set of supported content descriptors, you can
 * check if it's a FileTypeDescriptor before trying to save the stream to a file.
 */
public class FileTypeDescriptor extends ContentDescriptor {

    /** Quicktime content type */
    static public final String QUICKTIME 	= "video.quicktime";
    /** AVI content type */
    static public final String MSVIDEO 		= "video.x_msvideo";
    /** MPEG video and system stream content type */
    static public final String MPEG		= "video.mpeg";
    /** VIVO content type */
    static public final String VIVO		= "video.vivo";
    /** Basic Audio (.au) content type */
    static public final String BASIC_AUDIO	= "audio.basic";
    /** WAV content type */
    static public final String WAVE		= "audio.x_wav";
    /** AIFF content type */
    static public final String AIFF		= "audio.x_aiff";
    /** MIDI content type */
    static public final String MIDI		= "audio.midi";
    /** RMF content type */
    static public final String RMF		= "audio.rmf";
    /** GSM content type */
    static public final String GSM		= "audio.x_gsm";
    /** MPEG audio stream content type */
    static public final String MPEG_AUDIO	= "audio.mpeg";

    /**
     * Constructs a FileTypeDescriptor object for the specified content type string.
     */
    public FileTypeDescriptor(String contentType) {
	super(contentType);
    }
    
    public String toString() {
	if (encoding.equalsIgnoreCase(QUICKTIME))
	    return "QuickTime";
	else if (encoding.equalsIgnoreCase(MSVIDEO))
	    return "AVI";
	else if (encoding.equalsIgnoreCase(MPEG))
	    return "MPEG Video";
	else if (encoding.equalsIgnoreCase(VIVO))
	    return "Vivo";
	else if (encoding.equalsIgnoreCase(BASIC_AUDIO))
	    return "Basic Audio (au)";
	else if (encoding.equalsIgnoreCase(WAVE))
	    return "WAV";
	else if (encoding.equalsIgnoreCase(AIFF))
	    return "AIFF";
	else if (encoding.equalsIgnoreCase(MIDI))
	    return "MIDI";
	else if (encoding.equalsIgnoreCase(RMF))
	    return "RMF";
	else if (encoding.equalsIgnoreCase(GSM))
	    return "GSM";
	else if (encoding.equalsIgnoreCase(MPEG_AUDIO))
	    return "MPEG Audio";
	else
	    return encoding;	
    }
}
