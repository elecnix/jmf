/*
 * @(#)ContentType.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

// TODO: choose a better name
public class ContentType {
    public  static String getCorrectedContentType(String contentType,
						  String fileName) {
	if (contentType != null) {
	    if (contentType.startsWith("text")) {

		// Browser returns text/* for some uncommon types
		// like hotmedia or flash.  We'll handle this case here.

		int i = fileName.lastIndexOf(".");
		if (i != -1) {
		    String ext = fileName.substring(i+1).toLowerCase();
		    String type = com.sun.media.MimeManager.getMimeType(ext);
		    if (type != null)
			return type;
		}

		// TODO: Move this warning to protocol/DataSource.java
		com.sun.media.Log.error("Warning: The URL may not exist. Please check URL");
		return contentType;
	    }
	    if (contentType.equals("audio/wav"))
		return "audio/x-wav";
	    else if (contentType.equals("audio/aiff"))
		return "audio/x-aiff";
	    else if (contentType.equals("application/x-troff-msvideo"))
		// $$ WORKAROUND DUE TO WRONG MIME TYPE GIVEN FOR AVI
		//System.out.print("MIME TYPE BUG: ");
		//System.out.println("application/x-troff-msvideo ==> video/x-msvideo");
		return "video/x-msvideo";
	    else if (contentType.equals("video/msvideo"))
		return "video/x-msvideo";
            else if (contentType.equals("video/avi"))
                return "video/x-msvideo";
	    else if (contentType.equals("audio/x-mpegaudio"))
		return "audio/mpeg";
	}

	// Catch a few well known types even if they are not defined
	// in the system MIME table.
	String type = null;
 	int i = fileName.lastIndexOf(".");
 	if (i != -1) {
 	    String ext = fileName.substring(i+1).toLowerCase();
	    type = com.sun.media.MimeManager.getMimeType(ext);
	}

	if (type != null)
	    return type;
	
	if (contentType != null)
	    return contentType;
	else
	    return "content/unknown";
    }
}
