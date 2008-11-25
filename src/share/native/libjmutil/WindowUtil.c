/*
 * @(#)WindowUtil.c	1.7 00/09/14
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

#include <jni.h>
#include "com_sun_media_util_WindowUtil.h"
#include "com_sun_media_util_Registry.h"

#ifdef NOT_USED
#ifdef WIN32
#include <windows.h>
#else
#include <X11.h>
#endif /* WIN32 */
#endif /* NOT_USED */

#ifndef WIN32
#include <unistd.h>
#include <sys/utsname.h>
#else
#include <windows.h>
#endif

JNIEXPORT jint JNICALL
Java_com_sun_media_util_WindowUtil_getWindowHandle(JNIEnv *env,
						   jclass jwindowutil,
						   jobject jcomponent,
                                                   jint jawtAvail)
{
    jmethodID mid;
    jobject peer, surface;
    int returnVal;
    jclass clPeer;
    jclass clSurface;
    jclass clComponent;

    if ( jawtAvail == 1 ) {
	/*printf("in WindowUtil using jawt \n");*/
      /*return jawt_getWindowHandle(env, jcomponent); */
      clComponent = (jclass)(*env)->FindClass(env, "com/sun/media/DrawingSurfaceJAWT");
      if ( clComponent == NULL) 
	  printf("can't find dsJAWT class \n");
      mid = (*env)->GetStaticMethodID(env, clComponent, "getWindowHandle", 
			      "(Ljava/awt/Component;)I");
      if ( mid == NULL ) 
	  printf("can't find method getWindowHandle \n");
      /*printf("in WindowUtil after get mid \n");*/
      returnVal = (*env)->CallStaticIntMethod(env, clComponent, mid, jcomponent);
      if((*env)->ExceptionOccurred(env)) {
	  (*env)->ExceptionDescribe(env);
	  (*env)->ExceptionClear(env);
      }
      return returnVal;
    }

    /*printf("in WindowUtil using old way \n");*/
    clComponent = (jclass) (*env)->FindClass(env,"java/awt/Component");
    

    mid = (*env)->GetMethodID(env, clComponent, "getPeer", 
			      "()Ljava/awt/peer/ComponentPeer;");

    peer = (*env)->CallObjectMethod(env, jcomponent, mid, NULL); 
    if (peer == NULL)
	return 0;
    
    clPeer = (*env)->GetObjectClass(env, peer);
    if (clPeer == NULL)
	return 0;
    mid = (*env)->GetMethodID(env, clPeer, "gethwnd", "()I");

    if (mid == NULL) {
	(*env)->ExceptionClear(env);
	mid = (*env)->GetMethodID(env, clPeer, "getDrawingSurfaceInfo",
				  "()Lsun/awt/DrawingSurfaceInfo;");
    } else {
	int mswinid;
	mswinid = (*env)->CallIntMethod(env, peer, mid, NULL);
	return mswinid;
    }
    surface = (*env)->CallObjectMethod(env, peer, mid, NULL);

    clSurface = (*env)->GetObjectClass(env, surface);
    if (clSurface == NULL)
	return 0;
    
    /* Lock the drawing surface data */
    mid = (*env)->GetMethodID(env, clSurface, "lock", "()I");

    (*env)->CallIntMethod(env, surface, mid);

#ifdef WIN32
    /* Get the HWND */
    mid = (*env)->GetMethodID(env, clSurface, "getHWnd", "()I");
#else /* UNIX */
    /* Get the XID */
    mid = (*env)->GetMethodID(env, clSurface, "getDrawable", "()I");
#endif
    
    returnVal =   (int) (*env)->CallIntMethod(env, surface, mid, NULL);
    /* Unlock the drawing surface */
    mid = (*env)->GetMethodID(env, clSurface, "unlock", "()V");

    (*env)->CallVoidMethod(env, surface, mid, NULL);
    return (jint) returnVal;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_util_WindowUtil_canUseXIL(JNIEnv *env,
					     jclass jwindowutil,
					     jboolean greenOnlyVM)
{
#ifdef WIN32
    return (jboolean) 1;
#else

    /*
     * Get the environment variable THREADS_FLAG and the OS version.
     * XIL cannot be used with GREEN threads on Solaris 2.6
     */

    char *thread;
    int greenThreads = greenOnlyVM;
    int solaris26 = 0;
    struct utsname osDetails;
    /* get the THREADS_FLAG variable */
    thread = getenv("THREADS_FLAG");
    if (thread == NULL || strcasecmp(thread, "green") == 0)
	greenThreads = 1;
    
    /* get the OS details */
    uname(&osDetails);

    osDetails.release[3] = 0;
    
    if ( strcmp(osDetails.release, "2.6") == 0 ||
	 strcmp(osDetails.release, "5.6") == 0 ||
	 strcmp(osDetails.release, "2.7") == 0 ||
	 strcmp(osDetails.release, "5.7") == 0 ||
	 strcmp(osDetails.release, "2.8") == 0 ||
	 strcmp(osDetails.release, "5.8") == 0) {
	solaris26 = 1;
    }

    if (solaris26 && greenThreads)
	return (jboolean) 0;
    else
	return (jboolean) 1;

#endif
}
    
/*
 * Class:     com_sun_media_util_Registry
 * Method:    nGetUserHome
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_sun_media_util_Registry_nGetUserHome(JNIEnv *env,
					      jclass jRegistryClass)
{
    jstring userhome = NULL;
#ifdef WIN32
    char strHome[1024];
    int result = GetEnvironmentVariable("windir", strHome, sizeof(strHome));
    if (result > 0 && result < sizeof(strHome)) {
	strHome[result] = 0;
	userhome = (*env)->NewStringUTF(env, strHome);
    }
    
#endif
    return userhome;
}
