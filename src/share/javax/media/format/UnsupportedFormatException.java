/*
 * @(#)UnsupportedFormatException.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package javax.media.format;

import javax.media.Format;

/**
 * An <code>UnsupportedFormatException</code> is thrown when a format change request fails
 * because the requested <code>Format</code> is not supported.
 *
 */

public class UnsupportedFormatException extends javax.media.MediaException {

    Format failedFormat;

    /**
     * Constructs a new <code>UnsupportedFormatException</code>.
     * @param unsupportedFormat  The <code>Format</code> that is not supported.
     */
    public UnsupportedFormatException(Format unsupportedFormat) {
      
	failedFormat = unsupportedFormat;
    }

    /** 
     * Constructs a new <code>UnsupportedFormatException</code> with the specified parameters.
     * @param message  A <code>String</code> that contains a  message associated with the exception.
     * @param unsupportedFormat  The <code>Format</code> that is not supported.
     */
    public UnsupportedFormatException(String message, Format unsupportedFormat) {
        super(message);
	failedFormat = unsupportedFormat;
    }

    /**
     * Gets the unsupported <code>Format</code> reported by this exception.
     * @return The unsupported <code>Format</code>.
     */
    public Format getFailedFormat() {
	return failedFormat;
    }
}
