/*
 * @(#)BasicSourceStream.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol;

import java.io.IOException;
import javax.media.protocol.SourceStream;
import javax.media.protocol.ContentDescriptor;

public class BasicSourceStream implements SourceStream {

    protected ContentDescriptor contentDescriptor = null;
    protected long              contentLength     = LENGTH_UNKNOWN;
    protected Object []         controls          = new Object[0];

    static public final int LENGTH_DISCARD = -2;

    public BasicSourceStream(){}
    
    public BasicSourceStream(ContentDescriptor cd, long contentLength) {
	this.contentDescriptor = cd;
	this.contentLength = contentLength;
    }
    
    public ContentDescriptor getContentDescriptor() {
	return contentDescriptor;
    }

    public long getContentLength() {
	return contentLength;
    }

    /** Must override this class */
    public boolean endOfStream() {
	return false;
    }

    public Object [] getControls() {
	return controls;
    }
    
    public Object getControl(String controlType) {
       try {
          Class  cls = Class.forName(controlType);
          Object cs[] = getControls();
          for (int i = 0; i < cs.length; i++) {
             if (cls.isInstance(cs[i]))
                return cs[i];
          }
          return null;

       } catch (Exception e) {   // no such controlType or such control
         return null;
       }
    }
}
