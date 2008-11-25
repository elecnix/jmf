/*
 * @(#)BasicDataSink.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.datasink;

import java.util.Vector;
import java.util.Enumeration;
import javax.media.datasink.*;

public abstract class BasicDataSink implements javax.media.DataSink {

    protected Vector listeners = new Vector(1);
    
    public void addDataSinkListener(DataSinkListener dsl) {
	if (dsl != null)
	    if (!listeners.contains(dsl))
		listeners.addElement(dsl);
    }

    public void removeDataSinkListener(DataSinkListener dsl) {
	if (dsl != null)
	    listeners.removeElement(dsl);
    }

    protected void sendEvent(DataSinkEvent event) {
	if (!listeners.isEmpty()) {
	    synchronized (listeners) {
		Enumeration list = listeners.elements();
		while (list.hasMoreElements()) {
		    DataSinkListener listener = (DataSinkListener)list.nextElement();
		    listener.dataSinkUpdate(event);
		}
	    }
	}
    }

    protected void removeAllListeners() {
	listeners.removeAllElements();
    }
	    
    protected final void sendEndofStreamEvent() {
	sendEvent(new EndOfStreamEvent(this));
    }

    protected final void sendDataSinkErrorEvent(String reason) {
	sendEvent(new DataSinkErrorEvent(this, reason));
    }
}
	
