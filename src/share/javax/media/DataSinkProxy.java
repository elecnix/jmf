/*
 * @(#)DataSinkProxy.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.MediaLocator;
import javax.media.MediaProxy;

/**
 * A <CODE>DataSinkProxy</CODE> is a <CODE>MediaProxy</CODE> that provides
 * the content type of the <CODE>DataSink</CODE> to be created based on its
 * destination <CODE>MediaLocator</CODE>. 
 * <CODE>DataSinkProxy</CODE> is part of the 
 * factory mechanism for creating a <CODE>DataSink</CODE>.
 * @since JMF 2.0
 */
public abstract interface DataSinkProxy extends MediaProxy {

    /**
     * Gets the content type of the <CODE>DataSink</CODE> to be created for
     * the specified destination <CODE>MediaLocator</CODE>. 
	 * A <CODE>DataSink</CODE> destination <CODE>MediaLocator</CODE> 
     * typically contains information such as the file extension, which
     * the <CODE>DataSinkProxy</CODE> uses to deduce the content type.    
	 * The specified <CODE>MediaLocator</CODE> can have any format 
     * as long as the <CODE>DataSinkProxy</CODE> is
	 * capable of parsing the locator and returning its type. 
     * 
     * @param destination The destination <CODE>MediaLocator</CODE> for the
     * <CODE>DataSink</CODE> to be created.
     * @returns A <CODE>String</CODE> that contains content type of the <CODE>DataSink</CODE> or
     * null if the content type cannot be
     * determined from the specified <CODE>MediaLocator</CODE>. 
     * 
     */
    public String getContentType(MediaLocator destination);
}
