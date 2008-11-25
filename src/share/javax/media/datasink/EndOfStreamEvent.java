/*
 * @(#)EndOfStreamEvent.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.datasink;

import javax.media.DataSink;

/**
 * <code>EndOfStreamEvent </code> indicates that the DataSource connected
 * to the DataSink has flagged and end-of-stream. The application can now
 * assume that no more data is expected from the DataSource and can
 * close the DataSink.
 *
 * These events are used by <CODE>DataSinkListener</CODE>.
 *
 * @see javax.media.DataSink
 * @see DataSinkListener
 * @see javax.media.MediaEvent
 * @since JMF 2.0
 */

public class EndOfStreamEvent extends DataSinkEvent {

    public EndOfStreamEvent(DataSink from) {
	super(from);
    }

    public EndOfStreamEvent(DataSink from, String reason) {
	super(from, reason);
    }
}
