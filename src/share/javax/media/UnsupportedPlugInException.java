/*
 * @(#)UnsupportedPlugInException.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * An <CODE>UnsupportedPlugInException</CODE> is thrown
 * by a <CODE>TrackControl</CODE> if a <code>PlugIn<code>
 * cannot be set on the <code>Processor</code>.
 * @see Processor
 * @since JMF 2.0
 */

public class UnsupportedPlugInException extends MediaException {

    public UnsupportedPlugInException() {
	super();
    }
    
    public UnsupportedPlugInException(String reason) {
	super(reason);
    }
}
