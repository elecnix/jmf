/*
 * @(#)JMUtil.cc	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef WIN32
#include <unistd.h>
#include <sys/utsname.h>
#endif
/*#include <native.h>*/
// #include "bsd-endian.h"
#include "jni-util.h"
#include "com_sun_media_protocol_CachedPullSourceStream.h"
#include "com_sun_media_NBA.h"
#include "RGBScaler.h"

#ifdef LINUX
#define INT int
#else
#define INT long
#endif


JNIEXPORT void JNICALL
Java_com_sun_media_protocol_CachedPullSourceStream_nativeCreatFile
(JNIEnv *env,jobject cacheFile, jstring fname) {
    const char *f = env->GetStringUTFChars(fname, (jboolean *)NULL);
    //fprintf(stderr, "creating file %s\n", f);
    FILE *wFd = fopen(f, "wb");
    FILE *rFd = fopen(f, "rb");
    SetIntField(env, cacheFile, "wFd", (jint) wFd);
    //fprintf(stderr, "write: 0x%08x, read 0x%08x\n", (int) wFd, (int) rFd);
    SetIntField(env, cacheFile, "rFd", (jint) rFd);
    env->ReleaseStringUTFChars(fname, f);
    return;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_CachedPullSourceStream_checkAccess
(JNIEnv *env,jclass cls, jstring fname) {
    const char *f = env->GetStringUTFChars(fname, (jboolean *)NULL);
    int status;

    FILE *wFd = fopen(f, "wb");
    FILE *rFd = fopen(f, "rb");

    if ( (wFd == NULL) || (rFd == NULL) ) {
      status = 0; /* false */
    } else {
      status = 1; /* true */
    }
    env->ReleaseStringUTFChars(fname, f);
    if (rFd != NULL) {
      fclose(rFd);
    }
    if (wFd != NULL) {
      fclose(wFd);
    }
    
    return status;
}

JNIEXPORT void JNICALL
Java_com_sun_media_protocol_CachedPullSourceStream_nativeDelete
(JNIEnv *env,jobject cacheFile, jstring fname) {
    const char *f = env->GetStringUTFChars(fname, (jboolean *)NULL);
    //fprintf(stderr, "deleting file %s\n", f);
#ifdef WIN32
    _unlink(f);
#else
    unlink(f);
#endif
    env->ReleaseStringUTFChars(fname, f);
    return;
}

JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_CachedPullSourceStream_nativeWrite
(JNIEnv *env,jobject cacheFile, jint fd, jbyteArray buffer, jint offset, jint length) {
    char *cbuf = (char *)env->GetByteArrayElements(buffer, 0);
    FILE *f = (FILE*)fd;
    //fprintf(stderr, "write: 0x%08x\n", (int) f);
    fflush(f);
    int ret = fwrite(cbuf+offset, sizeof(char), length, f);
    fflush(f);
    env->ReleaseByteArrayElements(buffer, (signed char *)cbuf, JNI_ABORT);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_CachedPullSourceStream_nativeRead
(JNIEnv *env,jobject cacheFile, jint fd, jbyteArray buffer, jint offset, jint length) {
    char *cbuf = (char *)env->GetByteArrayElements(buffer, 0);
    FILE *f = (FILE*)fd;
    //fprintf(stderr, "read: 0x%08x\n", (int) f);
    int ret = fread(cbuf+offset, sizeof(char), length, f);
    env->ReleaseByteArrayElements(buffer, (signed char *) cbuf, 0);
    return ret;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_CachedPullSourceStream_nativeSeek
(JNIEnv *env,jobject cacheFile, jint fd, jint offset) {
    FILE *f = (FILE*)fd;
    int ret = fseek(f, offset, SEEK_SET);
    if (ret < 0)
	return 0;
    return 1;
}

JNIEXPORT void JNICALL
Java_com_sun_media_protocol_CachedPullSourceStream_nativeClose
(JNIEnv *env,jobject cacheFile, jint fd) {
    FILE *f = (FILE*)fd;
    if (f != NULL)
      fclose(f);
}

JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_CachedPullSourceStream_nativeTell
(JNIEnv *env,jobject cacheFile, jint fd) {
    FILE *f = (FILE*)fd;
    return ftell(f);
}

#ifdef NATIVERGBTORGB

JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_colorspace_NativeRGBToRGB_initConverter(JNIEnv *env, 
								       jobject rgb2rgb,
								       jint rm1,
								       jint gm1,
								       jint bm1,
								       jint byteDepth1,
								       jint rm2,
								       jint gm2,
								       jint bm2,
								       jint byteDepth2
								       ) {
    ms MS;
    MS = compute_ms((unsigned int) rm1, (unsigned int) rm2);
    SetIntField(env, rgb2rgb, "rMask", MS.mask);
    SetIntField(env, rgb2rgb, "rShift", MS.shift);
    MS = compute_ms((unsigned int) gm1, (unsigned int) gm2);
    SetIntField(env, rgb2rgb, "gMask", MS.mask);
    SetIntField(env, rgb2rgb, "gShift", MS.shift);
    MS = compute_ms((unsigned int) bm1, (unsigned int) bm2);
    SetIntField(env, rgb2rgb, "bMask", MS.mask);
    SetIntField(env, rgb2rgb, "bShift", MS.shift);
    
    if (byteDepth1 == 4) {
	if (byteDepth2 == 2) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert32_16);
	} else if (byteDepth2 == 3) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert32_24);
	} else if (byteDepth2 == 4) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert32_32);
	}
    } else if (byteDepth1 == 3) {
	if (byteDepth2 == 2) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert24_16);
	} else if (byteDepth2 == 3) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert24_24);
	} else if (byteDepth2 == 4) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert24_32);
	}
    } else if (byteDepth1 == 2) {
	if (byteDepth2 == 2) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert16_16);
	} else if (byteDepth2 == 3) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert16_24);
	} else if (byteDepth2 == 4) {
	    SetIntField(env, rgb2rgb, "conversionFun", (int) convert16_32);
	}
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_colorspace_NativeRGBToRGB_convert(JNIEnv *env, 
								 jobject rgb2rgb,
								 jobject inData,
								 jint aes1,
								 jint inStride,
								 jobject outData,
								 jint aes2,
								 jint outStride,
								 jint width,
								 jint height)
{
    unsigned int rmask, gmask, bmask;
    int rshift, bshift, gshift;
    rgb2rgb_conversion_fun converter;
    unsigned char *inBuf, *outBuf;
    
    rmask = (unsigned int) GetIntField(env, rgb2rgb, "rMask");
    gmask = (unsigned int) GetIntField(env, rgb2rgb, "gMask");
    bmask = (unsigned int) GetIntField(env, rgb2rgb, "bMask");

    rshift = (int) GetIntField(env, rgb2rgb, "rShift");
    gshift = (int) GetIntField(env, rgb2rgb, "gShift");
    bshift = (int) GetIntField(env, rgb2rgb, "bShift");
    
    if (inStride < width)
	inStride = width;
    if (outStride < width) 
	outStride = width;
    
    converter = (rgb2rgb_conversion_fun) GetIntField(env, rgb2rgb, "conversionFun");
    if (aes1 == 1) 
	inBuf = (unsigned char *)env->GetByteArrayElements((jbyteArray) inData,0);
    else if (aes1 == 2) 
	inBuf = (unsigned char *)env->GetShortArrayElements((jshortArray)inData,0);
    else if (aes1 == 4)
	inBuf =  (unsigned char *) env->GetIntArrayElements((jintArray)inData, 0);

    if (aes2 == 1) 
	outBuf = (unsigned char *)env->GetByteArrayElements((jbyteArray) outData,0);
    else if (aes2 == 2)
	outBuf = (unsigned char *)env->GetShortArrayElements((jshortArray) outData,0);
    else if (aes2 == 4)
	outBuf =  (unsigned char *) env->GetIntArrayElements((jintArray) outData, 0);

    // Do the conversion
    converter(inBuf, outBuf, rmask, gmask, bmask, 
	      rshift, gshift, bshift, width, height, inStride, outStride);

    if (aes1 == 1)
	env->ReleaseByteArrayElements((jbyteArray) inData, (signed char *) inBuf, JNI_ABORT);
    else if (aes1 == 2)
	env->ReleaseShortArrayElements((jshortArray) inData, (short *) inBuf, JNI_ABORT);
    else if (aes1 == 4)
	env->ReleaseIntArrayElements((jintArray) inData, (INT *) inBuf, JNI_ABORT);

    if (aes2 == 1)
	env->ReleaseByteArrayElements((jbyteArray) outData, (signed char *) outBuf, 0);
    else if (aes2 == 2)
	env->ReleaseShortArrayElements((jshortArray) outData, (short *) outBuf, 0);
    else if (aes2 == 4)
	env->ReleaseIntArrayElements((jintArray) outData, (INT *) outBuf, 0);
    
    return 1;
}

#endif

struct ScalerData {
    int * offsets;
    int wIn, wOut, hIn, hOut;
};

JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_colorspace_RGBScaler_nativeScale(JNIEnv *env, 
								jobject jscaler,
								jobject jinData,
								jlong inBytes,
								jobject joutData,
								jlong outBytes,
								jint psIn,
								jint lsIn,
								jint wIn,
								jint hIn,
								jint psOut,
								jint lsOut,
								jint wOut,
								jint hOut)
{
    struct ScalerData * nativeData = (struct ScalerData *) GetIntField(env, jscaler,
								       "nativeData");
    char *inData = (char*) inBytes, *outData = (char*) outBytes;
    int oldY = -1, inY;
    int x, y;
    char *inPtr;
    char *outPtr;
    
    float verRatio = (float) hIn / hOut;
    
    if (nativeData == NULL) {
	nativeData = (struct ScalerData *) malloc(sizeof(struct ScalerData));
	SetIntField(env, jscaler, "nativeData", (jint) nativeData);
	nativeData->wIn = 0;
	nativeData->wOut = 0;
	nativeData->hIn = 0;
	nativeData->hOut = 0;
	nativeData->offsets = NULL;
    }

    // Check to see if scaling parameters have changed
    if (  nativeData->wIn != wIn ||
	  nativeData->wOut != wOut ||
	  nativeData->hIn != hIn ||
	  nativeData->hOut != hOut   ) {

	int i;
	
	if (nativeData->offsets)
	    free(nativeData->offsets);
	nativeData->offsets = (int *) malloc(wOut * sizeof(int));
	for (i = 0; i < wOut; i++) {
	    nativeData->offsets[i] = (int) ((float) (i * wIn) / wOut) * psIn;
	}

	nativeData->wIn = wIn; nativeData->wOut = wOut;
	nativeData->hIn = hIn; nativeData->hOut = hOut;
    }
    
    if (inBytes == 0)
	inData = (char *) env->GetByteArrayElements((jbyteArray) jinData, 0);
    if (outBytes == 0)
	outData = (char *) env->GetByteArrayElements((jbyteArray) joutData, 0);

    inPtr = inData;
    outPtr = outData;
    
    for (y = 0; y < hOut; y++) {
	inY = (int) (y * verRatio);
	inPtr = inData + (inY * lsIn);
	if (inY == oldY) {
	    memcpy(outPtr, outPtr - lsOut, lsOut);
	    outPtr += lsOut;
	} else {
	    if (psOut == 3 && psIn == 3 &&((wOut % 4) == 0)) {
		for (x = 0; x < wOut; x+=4) {
		    *outPtr++ = inPtr[nativeData->offsets[x+0] + 0];
		    *outPtr++ = inPtr[nativeData->offsets[x+0] + 1];
		    *outPtr++ = inPtr[nativeData->offsets[x+0] + 2];
		    *outPtr++ = inPtr[nativeData->offsets[x+1] + 0];
		    *outPtr++ = inPtr[nativeData->offsets[x+1] + 1];
		    *outPtr++ = inPtr[nativeData->offsets[x+1] + 2];
		    *outPtr++ = inPtr[nativeData->offsets[x+2] + 0];
		    *outPtr++ = inPtr[nativeData->offsets[x+2] + 1];
		    *outPtr++ = inPtr[nativeData->offsets[x+2] + 2];
		    *outPtr++ = inPtr[nativeData->offsets[x+3] + 0];
		    *outPtr++ = inPtr[nativeData->offsets[x+3] + 1];
		    *outPtr++ = inPtr[nativeData->offsets[x+3] + 2];
		}
	    } else {
		for (x = 0; x < wOut; x++) {
		    *outPtr++ = inPtr[nativeData->offsets[x]];
		    *outPtr++ = inPtr[nativeData->offsets[x] + 1];
		    *outPtr++ = inPtr[nativeData->offsets[x] + 2];
		    outPtr += (psOut - 3);
		}
	    }
	    outPtr += lsOut - (wOut * psOut); 
	}
	oldY = inY;
    }

    if (inBytes == 0)
	env->ReleaseByteArrayElements((jbyteArray) jinData, (jbyte*) inData,
				      JNI_ABORT);
    if (outBytes == 0)
	env->ReleaseByteArrayElements((jbyteArray) joutData, (jbyte*) outData, 0);
}

JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_colorspace_RGBScaler_nativeClose(JNIEnv *env, 
								jobject jscaler)
{
    struct ScalerData * nativeData = (struct ScalerData *) GetIntField(env, jscaler,
								       "nativeData");

    if (nativeData != NULL) {
	if (nativeData->offsets != NULL)
	    free(nativeData->offsets);
	free(nativeData);
	SetIntField(env, jscaler, "nativeData", 0);
    }
}

/*
 * Class:     com_sun_media_NBA
 * Method:    nAllocate
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL
Java_com_sun_media_NBA_nAllocate(JNIEnv *env,
				 jobject nba, jint size)
{
    return (jlong) malloc(size);
}

/*
 * Class:     com_sun_media_NBA
 * Method:    nDeallocate
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_NBA_nDeallocate(JNIEnv *env,
				   jobject nba, jlong data)
{
    if (data != 0)
	free((void *) data);
}

/*
 * Class:     com_sun_media_NBA
 * Method:    nCopyToJava
 * Signature: (JLjava/lang/Object;)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_NBA_nCopyToJava(JNIEnv *env,
				   jobject nba,
				   jlong source,
				   jobject dest,
				   jint size,
				   jint type)
{
    if (type == 1) {
	env->SetByteArrayRegion((jbyteArray) dest, 0, size,
				(jbyte*) source);
    } else if (type == 2) {
	env->SetShortArrayRegion((jshortArray) dest, 0, size,
				(jshort*) source);
    } else if (type == 4) {
	env->SetIntArrayRegion((jintArray) dest, 0, size,
				(jint*) source);
    } else if (type == 8) {
	env->SetLongArrayRegion((jlongArray) dest, 0, size,
				(jlong*) source);
    }
}

/*
 * Class:     com_sun_media_NBA
 * Method:    nCopyToNative
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_NBA_nCopyToNative(JNIEnv *env,
				     jobject nba,
				     jlong source,
				     jlong dest,
				     jint size)
{
    if (source != 0 && dest != 0)
	memcpy((void*)dest, (void*)source, size);
}
