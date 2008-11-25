/*
 * @(#)NoProcessorException.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>NoProcessorException</code>  is thrown when <code>Manager</code> 
 * can't find a <code>Processor</code> for a
 * particular <CODE>URL, MediaLocator</code> or <code>ProcessorModel</CODE>.
 *
 * @see Manager
 * @see NoPlayerException
 * @version 2.0, 98/05/18.
 * @since JMF 2.0
 */

public class NoProcessorException extends NoPlayerException {

   public NoProcessorException() {
       super();
   }
    
    public NoProcessorException(String reason) {
	super(reason);
    }
}
