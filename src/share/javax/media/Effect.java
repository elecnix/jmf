/*
 * @(#)Effect.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.format.*;

/**
 * An <code>Effect</code> is a media processing unit that takes an input <CODE>Buffer</CODE> of data,
 * performs some special-effects processing on the data, and then outputs the
 * data to an output <CODE>Buffer</CODE>. It has the same input and output structure
 * as a <CODE>Codec</CODE>. The difference
 * is that an <CODE>Effect</CODE> does not modify the <CODE>Format</CODE> of the data, it 
 * manipulates the contents.
 * @since JMF 2.0
 */
public interface Effect extends Codec {
}
