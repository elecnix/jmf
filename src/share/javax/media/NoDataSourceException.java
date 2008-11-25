/*
 * @(#)NoDataSourceException.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
  * A <code>NoDataSourceException</code> is thrown when a <code>DataSource</code> 
  * can't be found for a particular <CODE>URL</CODE> or <CODE>MediaLocator</CODE>.
  *
 * @version 1.2, 02/08/21.
 */

public class NoDataSourceException extends MediaException {

   public NoDataSourceException() {
       super();
   }
    
    public NoDataSourceException(String reason) {
	super(reason);
    }
}
