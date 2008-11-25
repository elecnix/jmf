/*
 * @(#)SystemTimeBase.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * SystemTimeBase is the implementation of the default <CODE>TimeBase</CODE> that ships
 * with JMF.
 *
 * @see TimeBase
 *
 * @version 1.5, 02/08/21.
 *
*/ 
final public class SystemTimeBase implements TimeBase {

    // Pick some offset (start-up time) so the system time won't be
    // so huge.  The huge numbers overflow floating point operations
    // in some cases.
    static long offset = System.currentTimeMillis() * 1000000L;
  
    public Time getTime() {
	return new Time(getNanoseconds());
    }

    public long getNanoseconds() {
	return (System.currentTimeMillis() * 1000000L) - offset;
    }
}
