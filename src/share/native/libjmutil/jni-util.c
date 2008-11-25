/*
 * @(#)jni-util.c	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include "jni-util.h"

JNIEXPORT jint 
GetIntField(JNIEnv *env, jobject obj, char *field)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "I");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "GetIntField() failed: %s\n", field);
	return -1;
    }
    return (*env)->GetIntField(env, obj, fid);
}


JNIEXPORT void 
SetIntField(JNIEnv *env, jobject obj, char *field, int val)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "I");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "SetIntField() failed: %s\n", field);
	return;
    }
    (*env)->SetIntField(env, obj, fid, val);
}


JNIEXPORT jlong 
GetLongField(JNIEnv *env, jobject obj, char *field)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "J");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "GetLongField() failed: %s\n", field);
	return -1;
    }
    return (*env)->GetLongField(env, obj, fid);
}


JNIEXPORT void 
SetLongField(JNIEnv *env, jobject obj, char *field, jlong val)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "J");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "SetLongField() failed: %s\n", field);
	return;
    }
    (*env)->SetLongField(env, obj, fid, val);
}


JNIEXPORT jfloat
GetFloatField(JNIEnv *env, jobject obj, char *field)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "F");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "GetFloatField() failed: %s\n", field);
	return -1;
    }
    return (*env)->GetFloatField(env, obj, fid);
}


JNIEXPORT void 
SetFloatField(JNIEnv *env, jobject obj, char *field, jfloat val)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "F");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "SetFloatField() failed: %s\n", field);
	return;
    }
    (*env)->SetFloatField(env, obj, fid, val);
}


JNIEXPORT jobject 
GetObjectField(JNIEnv *env, jobject obj, char *field, char *type)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, type);
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "GetObjectField() failed: %s, %s\n", field, type);
	return NULL;
    }
    return (*env)->GetObjectField(env, obj, fid);
}


JNIEXPORT void 
SetObjectField(JNIEnv *env, jobject obj, char *field, char *type, jobject val)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, type);
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "SetObjectField() failed: %s, %s\n", field, type);
	return;
    }
    (*env)->SetObjectField(env, obj, fid, val);
}


JNIEXPORT jmethodID
GetMethodID(JNIEnv *env, jobject obj, char *name, char *sig)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid = (*env)->GetMethodID(env, cls, name, sig);
    (*env)->DeleteLocalRef(env, cls);
    if (mid == NULL) {
	fprintf(stderr, "GetMethodID() failed: %s, %s\n", name, sig);
	return NULL;
    }
    return mid;
}


JNIEXPORT jobject
CallObjectMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid;
    va_list args;
    jobject result = NULL;

    mid = (*env)->GetMethodID(env, cls, name, sig);
    (*env)->DeleteLocalRef(env, cls);
    if (mid == 0) {
	fprintf(stderr, "CallObjectMethod() failed: %s, %s\n", name, sig);
	return NULL;
    }

    va_start(args, sig);
    result = (*env)->CallObjectMethodV(env, obj, mid, args); 
    va_end(args);

    return result;    
}


JNIEXPORT jboolean
CallBooleanMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid;
    va_list args;
    jboolean result = 0;

    mid = (*env)->GetMethodID(env, cls, name, sig);
    (*env)->DeleteLocalRef(env, cls);
    if (mid == 0) {
	fprintf(stderr, "CallBooleanMethod() failed: %s, %s\n", name, sig);
	return NULL;
    }

    va_start(args, sig);
    result = (*env)->CallBooleanMethodV(env, obj, mid, args); 
    va_end(args);
        
    if((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    return result;    
}


JNIEXPORT jint
CallIntMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid;
    va_list args;
    jint result = 0;

    mid = (*env)->GetMethodID(env, cls, name, sig);
    (*env)->DeleteLocalRef(env, cls);
    if (mid == 0) {
	fprintf(stderr, "CallIntMethod() failed: %s, %s\n", name, sig);
	return NULL;
    }

    va_start(args, sig);
    result = (*env)->CallIntMethodV(env, obj, mid, args); 
    va_end(args);
        
    if((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    return result;    
}


JNIEXPORT jlong
CallLongMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid;
    va_list args;
    jint result = 0;

    mid = (*env)->GetMethodID(env, cls, name, sig);
    (*env)->DeleteLocalRef(env, cls);
    if (mid == 0) {
	fprintf(stderr, "CallLongMethod() failed: %s, %s\n", name, sig);
	return NULL;
    }

    va_start(args, sig);
    result = (*env)->CallLongMethodV(env, obj, mid, args); 
    va_end(args);
        
    if((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    return result;    
}


JNIEXPORT jdouble
CallDoubleMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid;
    va_list args;
    jint result = 0;

    mid = (*env)->GetMethodID(env, cls, name, sig);
    (*env)->DeleteLocalRef(env, cls);
    if (mid == 0) {
	fprintf(stderr, "CallDoubleMethod() failed: %s, %s\n", name, sig);
	return 0;
    }

    va_start(args, sig);
    result = (*env)->CallDoubleMethodV(env, obj, mid, args); 
    va_end(args);
        
    if((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    return result;    
}


JNIEXPORT void 
CallVoidMethod(JNIEnv *env, jobject obj, char *name, char *sig, ...)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid;
    va_list args;
    jint result = 0;

    mid = (*env)->GetMethodID(env, cls, name, sig);
    (*env)->DeleteLocalRef(env, cls);
    if (mid == 0) {
	fprintf(stderr, "CallVoidMethod() failed: %s, %s\n", name, sig);
	return;
    }

    va_start(args, sig);
    (*env)->CallVoidMethodV(env, obj, mid, args); 
    va_end(args);
        
    if((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
}


JNIEXPORT void 
CallStaticVoidMethod(JNIEnv *env, jclass cls, char *name, char *sig, ...)
{
    jmethodID mid;
    va_list args;
    jint result = 0;

    mid = (*env)->GetStaticMethodID(env, cls, name, sig);
    if (mid == 0) {
	fprintf(stderr, "CallStaticVoidMethod() failed: %s, %s\n", name, sig);
	return;
    }

    va_start(args, sig);
    (*env)->CallStaticVoidMethodV(env, cls, mid, args); 
    va_end(args);
        
    if((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
}


JNIEXPORT char *
GetObjectClassName(JNIEnv *env, jobject obj)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid = (*env)->GetMethodID(env, cls, "toString",
				"()Ljava/lang/String;");
    jobject name = (*env)->CallObjectMethod(env, obj, mid); 
    char *str = (char *)(*env)->GetStringUTFChars(env, name, 0);
    (*env)->DeleteLocalRef(env, cls);
    return str;
}


JNIEXPORT jboolean
IsInstanceOf(JNIEnv *env, jobject obj, char *type) 
{
    jclass cls = (*env)->FindClass(env, type);
    if (cls == NULL) {
	fprintf(stderr, "IsInstanceOf() failed: no such class: %s\n", type);
	return 0;
    }
    return (*env)->IsInstanceOf(env, obj, cls);
}

