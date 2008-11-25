/*
 * @(#)DataSource.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.http;

import java.io.IOException;
import javax.media.protocol.SourceCloneable;

public class DataSource extends com.sun.media.protocol.DataSource implements SourceCloneable {

    public javax.media.protocol.DataSource createClone() {
	DataSource ds = new com.sun.media.protocol.http.DataSource();
	ds.setLocator(getLocator());
	if (connected) {
	    try {
		ds.connect();
	    } catch (IOException e) {
		return null;
	    }
	}
	return ds;
    }
}
