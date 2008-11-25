/*
 * @(#)jni-util.h	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef _JNI_UTIL_H_
#define _JNI_UTIL_H_

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

    JNIEXPORT jint GetIntField(JNIEnv *env, jobject obj, char *field);
    JNIEXPORT void SetIntField(JNIEnv *env, jobject obj, char *field, int val);
    JNIEXPORT jlong GetLongField(JNIEnv *env, jobject obj, char *field);
    JNIEXPORT void SetLongField(JNIEnv *env, jobject obj, char *field, jlong val);
    JNIEXPORT jfloat GetFloatField(JNIEnv *env, jobject obj, char *field);
    JNIEXPORT void SetFloatField(JNIEnv *env, jobject obj, char *field, jfloat val);
    JNIEXPORT jobject GetObjectField(JNIEnv *env, jobject obj, char *field, char *type);
    JNIEXPORT void SetObjectField(JNIEnv *env, jobject obj, char *field, char *type, jobject val);
    JNIEXPORT jmethodID GetMethodID(JNIEnv *env, jobject obj, char *name, char *sig);
    JNIEXPORT jobject CallObjectMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...);
    JNIEXPORT jint CallIntMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...);
    JNIEXPORT jlong CallLongMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...);
    JNIEXPORT jdouble CallDoubleMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...);
    JNIEXPORT void CallVoidMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...);
    JNIEXPORT void CallStaticVoidMethod(JNIEnv *env, jclass cls, char *name, char *sig, ...);
    JNIEXPORT char *GetObjectClassName(JNIEnv *env, jobject obj);
    JNIEXPORT jboolean IsInstanceOf(JNIEnv *env, jobject obj, char *cls);

#ifdef __cplusplus
}
#endif

#endif /* _JNI_UTIL_H_ */
