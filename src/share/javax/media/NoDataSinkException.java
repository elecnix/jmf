/*
 *
 * @(#)NoDataSinkException.java	1.2 02/08/21 
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */


package javax.media;

/**
 * A <code>NoDataSinkException</code>  is thrown when <code>Manager</code> 
 * can't find a <code>DataSink</code> for a
 * particular <code>MediaLocator and DataSource</code>.
 *
 * @see Manager
 * @see NoDataSinkException
 * @since JMF 2.0
 */

public class NoDataSinkException extends MediaException {

    public NoDataSinkException() {
	super();
    }
    
    public NoDataSinkException(String reason) {
	super(reason);
    }
}
