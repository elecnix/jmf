/*
 * @(#)JStream.h	1.8 98/09/07
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */


#ifndef __JSTREAM_H__
#define __JSTREAM_H__

#include <jni.h>

class JStream;

/**
 * Java Stream reader for the JMSourceStream
 */
class JStream {
  public:
    JStream() { }
    JStream(JNIEnv *env, jobject amovie,
	    jboolean seekable,
	    jboolean randomAccess,
	    jlong contentLength);
    ~JStream();

    int  getLength(jlong *length);
    int  read(void *data, long size, long *actualRead);
    int  getSeekable();
    int  seek(long position, long *actualPosition);
    int  restart();
    void close();
    
    void setSeekable(int seekable) {
	m_randomAccess = seekable;
	m_seekable = seekable;
    }

    int  getDiscontinuity() {
	return m_discontinuity;
    }
    
    void doNRequest(JNIEnv *env, jobject amovie, jbyteArray buffer);
    void doJRead(JNIEnv *env, jobject amovie, jbyteArray, long size,
		 long *actualRead);
    void doJSeek(JNIEnv *env, jobject amovie, long seekTo);
    

  private:
    jlong  m_length;
    jmethodID m_mid_read;
    jmethodID m_mid_seek;
    jclass    m_classAM;
    int   m_seekable;
    int   m_randomAccess;
    long  m_bytesRequired;
    long  m_bytesRead;
    void *m_data;
    int   m_closed;
    long  m_seekTo;
    int   m_goAway;
    int   m_discontinuity;
};

#endif
