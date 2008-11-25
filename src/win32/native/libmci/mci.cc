/*
 * @(#)mci.cc	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <windows.h>
#include <mmsystem.h>
#include <stdio.h>
#include "com_sun_media_amovie_MCI.h"
#include "jni-util.h"

JNIEXPORT jint JNICALL
Java_com_sun_media_amovie_MCI_getDeviceId
(JNIEnv *env, jobject mci, jstring name) {
    char *devName = (char *)env->GetStringUTFChars(name, NULL);
    int ret = mciGetDeviceID(devName);
    env->ReleaseStringUTFChars(name, devName);
    return ret;
}

JNIEXPORT jstring JNICALL
Java_com_sun_media_amovie_MCI_getErrorString
(JNIEnv *env, jobject mci, jint errId) {
    char errString[300];
    if (mciGetErrorString(errId, errString, 300)) {
	return env->NewStringUTF(errString);
    }
    return (jstring) 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_amovie_MCI_sendString
(JNIEnv *env, jobject mci, jstring command) {
    char errStr[300];
    char * comm =(char *) env->GetStringUTFChars(command, NULL);
    int ret;
    HANDLE hwnd = (HANDLE) GetIntField(env, mci, "hwndCallback");
    if (ret = mciSendString(comm, errStr, 300, (HWND) hwnd)) { //some error occured
	mciGetErrorString(ret, errStr, 300);
	jstring err = env->NewStringUTF(errStr);
	SetObjectField(env, mci, "errString", "Ljava/lang/String;", err);
#ifdef DEBUG
	fprintf(stderr, "ERROR: %d %s\n", ret, errStr);
#endif 
	return 0;
    }
    jstring err = env->NewStringUTF(errStr);
    SetObjectField(env, mci, "retString", "Ljava/lang/String;", err);
#ifdef DEBUG
    fprintf(stderr, "RETURN: %d %s\n", ret, errStr);
#endif
    return 1;
}
