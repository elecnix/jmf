/*
 * @(#)JStream.cc	1.15 98/09/22
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

#include <stdio.h>
#include <string.h>
#include <windows.h>
#include "jni-util.h"
#include "JStream.h"

#ifdef DEBUG
#define PRINT(s) printf(s)
#else
#define PRINT(s)
#endif

/**
 * Constructor.
 */
JStream::JStream(JNIEnv *env, jobject amovie,
		 jboolean seekable,
		 jboolean randomAccess,
		 jlong contentLength)
{
    PRINT("JStream::JStream\n");
    m_mid_read = NULL;
    m_mid_seek = NULL;
    m_classAM  = NULL;
    m_seekable = (int) seekable;
    m_randomAccess = (int) randomAccess;
    m_bytesRequired = 0;
    m_seekTo = -1;
    m_closed = 0;
    m_length = (jlong) contentLength;

    // Cache the method IDs
    m_classAM  = env->GetObjectClass(amovie);
    m_mid_read = env->GetMethodID( m_classAM, "read", "([BII)I" );
    m_mid_seek = env->GetMethodID( m_classAM, "seek", "(J)J"    );
}

/**
 * A Java Thread frequently comes down to native land to check for
 * seek or read requests from ActiveMovie.
 */
void
JStream::doNRequest(JNIEnv *env, jobject amovie, jbyteArray jbuffer)
{
    PRINT("JStream::doNRequest\n");
    // If Active Movie needs to seek, do so.
    if ((m_seekTo > 0 && m_randomAccess) || (m_seekTo == 0 && m_seekable)) {
	
	doJSeek(env, amovie, m_seekTo);

	m_seekTo = -1;

    }
    
    // If Active Movie needs a buffer, fill it up.
    if (m_bytesRequired) {

	doJRead(env, amovie, jbuffer, m_bytesRequired, &m_bytesRead);

	if (m_bytesRead > 0 && m_bytesRead <= 65536) {
	    char *cdata = (char*) (env)->GetByteArrayElements(jbuffer, NULL);
	    memcpy(m_data, (void*)cdata, m_bytesRead);
	    env->ReleaseByteArrayElements(jbuffer, (signed char*) cdata, 0);
	}

	m_bytesRequired = 0;

    }
}

void
JStream::doJSeek(JNIEnv *env, jobject amovie, long seekTo)
{
    PRINT("JStream::doJSeek\n");
    
    //printf("In doJSeek %d\n", seekTo);
    env->CallLongMethod(amovie, m_mid_seek, seekTo, NULL);
}

void
JStream::doJRead(JNIEnv *env, jobject amovie, jbyteArray buffer, long size,
		long *actualRead)
{
    long aRead;
    long totalRead = 0;

    PRINT("JStream::doJRead\n");

    // printf("In doJRead() start reading %d\n", totalRead);
    
    // Loop till we get data or end-of-stream
    // A -2 return value indicates that there was a discontinuity in
    // the incoming data. MPEG decoder needs to reset
    m_discontinuity = 0;
    do {
	aRead = env->CallIntMethod(amovie, m_mid_read,
				   buffer,
				   totalRead, size - totalRead, NULL);
	if (aRead == -1)
	    totalRead = 0;
	else if (aRead == -2) {
	    m_discontinuity = 1;
	} else
	    totalRead = aRead;
    } while (aRead == -2);

    PRINT("JStream::doJRead() done reading %d\n", totalRead);

    if (actualRead != NULL)
	*actualRead = totalRead;
    return;
}

int
JStream::restart()
{
    PRINT("JStream::restart\n");
    seek(0, NULL);
    return 1;
}

int
JStream::getLength(jlong *length)
{
    *length = m_length;
    return 1;
}

int
JStream::getSeekable()
{
    PRINT("JStream::getSeekable\n");
    
    return m_randomAccess;
}

int
JStream::seek(long position, long *actualPosition)
{
    PRINT("JStream::seek\n");
    if (m_closed) {
	return 0;
    }
    
    if (m_randomAccess || (position == 0 && m_seekable)) {
	m_seekTo = position;
	while (m_seekTo >= 0) {
	    Sleep(10);
	}
	if (actualPosition)
	    *actualPosition = position;
	return position;
    } else
	return 0;
}

int
JStream::read(void *data, long size, long *actualRead)
{
    char *dataBuffer = (char*) data;
    //printf("In JStream::read\n");
    PRINT("JStream::read\n");
    
    if (m_closed) {
	*actualRead = 0;
	return 1;
    }
    
    m_data = dataBuffer;
    m_bytesRequired = size;
    while (m_bytesRequired)
	Sleep(10);
    *actualRead = m_bytesRead;
    return 1;
}

void
JStream::close()
{
    PRINT("JStream::close\n");
    m_closed = 1;
}

JStream::~JStream()
{
}

