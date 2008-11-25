/*
 * @(#)RGBScaler.h	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_colorspace_RGBScaler_nativeScale(JNIEnv *env, 
								jobject jscaler,
								jobject jinData,
								jlong jinBytes,
								jobject joutData,
								jlong joutBytes,
								jint psIn,
								jint lsIn,
								jint wIn,
								jint hIn,
								jint psOut,
								jint lsOut,
								jint wOut,
								jint hOut);

JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_colorspace_RGBScaler_nativeClose(JNIEnv *env, 
								jobject jscaler);
    
#ifdef __cplusplus
}
#endif
