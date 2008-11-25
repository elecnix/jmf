/*
 * @(#)DataSinkErrorEvent.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.datasink;

import javax.media.DataSink;

/**
 * <code>DataSinkErrorEvent</code> indicates that an error has
 * occurred while the DataSink was writing data to the destination.
 * 
 *
 * These events are used by <CODE>DataSinkListener</CODE>.
 *
 * @see javax.media.DataSink
 * @see DataSinkListener
 * @see javax.media.MediaEvent
 * @since JMF 2.0
 */

public class DataSinkErrorEvent extends DataSinkEvent { 

    public DataSinkErrorEvent(DataSink from) {
	super(from);
    }

    public DataSinkErrorEvent(DataSink from, String reason) {
	super(from, reason);
    }

}
