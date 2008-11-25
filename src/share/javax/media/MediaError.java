/*
 * @(#)MediaError.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
  * A <code>MediaError</code> indicates an error condition that
  * occurred through incorrect usage of the API.
  * You should not check for <code>MediaErrors</code>.
 * @version 1.15, 98/06/23.
 */

public class MediaError extends Error {

    public MediaError() {
	super();
    }
    
    public MediaError(String reason) {
       super(reason);
    }
}
