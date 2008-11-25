/*
 * @(#)DataSinkEvent.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.datasink;

import javax.media.*;

/**
 * <code>DataSinkEvent</code> is the base class for events generated
 * by the <code>DataSink</code>. 
 *
 * These events are used by <CODE>DataSinkListener</CODE>.
 *
 * @see javax.media.DataSink
 * @see DataSinkListener
 * @see javax.media.MediaEvent
 * @version 1.7, 02/08/21
 * @since JMF 2.0
 */

public class DataSinkEvent extends javax.media.MediaEvent {

    private String message;

    /**
     * Constructs a DataSinkEvent object for the specified DataSink.
     */
    public DataSinkEvent(DataSink from) {
	super(from);
	message = new String("");
    }

    /**
     * Constructs a DataSinkEvent object for the specified DataSink
     * and reason string.
     */
    public DataSinkEvent(DataSink from, String reason) {
	super(from);
	message = new String(reason);
    }

    /**
     * Get the <CODE>DataSink</CODE> that posted this event.  
     * The returned <CODE>DataSink</CODE> has at least one active
     * listener. (The 
     * <CODE>addDataSinkListener</CODE> method has been called on
     * the <CODE>DataSink</CODE>).
     * 
     * @return The <CODE>DataSink</CODE> that posted this event.
     */
    public DataSink getSourceDataSink() {
	return (DataSink) getSource();
    }

    /**
     * Returns the String representation of this event's values.
     */
    public String toString() {
	return getClass().getName() + "[source=" + getSource() + "] message: " + message;
    }
}

