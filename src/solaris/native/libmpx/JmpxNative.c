/*
 * @(#)JmpxNative.c	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdlib.h>
#include <stdio.h>
#ifndef WIN32
#include <unistd.h>
#endif
#include "mpx.h"
#include "mp_mpd.h"
#include "mp_aud.h"
#include "ln_lnd.h"
#include "com_sun_media_codec_video_jmpx_Jmpx.h"
#include <jni-util.h>

static int MMX = -1;

extern int getMediaDuration(char * filename);

long
windowID(JNIEnv *env, jobject component)
{
    jobject peer, surface;
    
    peer = CallObjectMethod(env, component, "getPeer", 
			    "()Ljava/awt/peer/ComponentPeer;"); 
    surface = CallObjectMethod(env, peer, "getDrawingSurfaceInfo", 
			       "()Lsun/awt/DrawingSurfaceInfo;");
    return CallIntMethod(env, surface, "getDrawable", "()I");
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_jmpx_Jmpx_bigEndian(JNIEnv *env, jobject jmpx)
{
#ifdef JM_BIG_ENDIAN
    return (jboolean) 1;
#else
    return (jboolean) 0;
#endif
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_jmpx_Jmpx_initMPX(JNIEnv *env,
						 jobject jmpx,
						 jobject component)
{
#define ERRMPX	"Failed to initialize mpx\n"

    MpxCntl *cntl;
    if (!uvtablecreated) {
	createUVTable();
	uvtablecreated = 1;
    }

#ifdef WIN32
    /* Check for MMX technology bit in the Pentium Processor */
    if (MMX == -1) {
	MMX = 0;
	__asm {
	    mov eax, 1;
	    _emit 0fh;
	    _emit 0a2h;
	    test edx, 0x0800000;
	    jz NOTFOUND;
	    mov MMX, 1;
	NOTFOUND:
	    nop;
	}
    }
#endif
    cntl = mpxCreate(NULL,
		     MpxNerrorSilent, TRUE,
		     MpxNuiType, MpxNoGUI,
		     MpxNwinXid, component,
		     MpxNwinDepth, 24,
		     MpxNvideoInterleave, TRUE,
		     MpxNvideoZoom, 1,
		     MpxNjniEnv, env,
		     MpxNjavaClient, (*env)->NewGlobalRef(env, jmpx),
		     NULL);
    
    if (!cntl) {
	fprintf(stderr, ERRMPX);
	return 0;
    }
    
    SetLongField(env, jmpx, "peer", (jlong)cntl);
    
    return 1;
    
#undef ERRMPX
}


JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_jmpx_Jmpx_sendMPX(JNIEnv *env, jobject jmpx, jbyteArray cmd)
{
    MpxCntl *cntl;
    u_char *data;
    
    cntl = (MpxCntl *)GetLongField(env, jmpx, "peer");
    
    /* This shouldn't happen normally.  But somehow, when running in
       HotJava, it got garbage collected twice.  This handles the case
       when this is called the second time. */
    if (cntl == NULL)
	return;
    data = (u_char *)(*env)->GetByteArrayElements(env, cmd, 0);
    proc_extcmd(env, cntl->gb, (u_int *)data, 512);
    (*env)->ReleaseByteArrayElements(env, cmd, (jbyte *)data, JNI_ABORT);
}


JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_jmpx_Jmpx_broadcastMPX(JNIEnv *env, jobject jmpx, jbyteArray cmd)
{
    u_char *data = (u_char *)(*env)->GetByteArrayElements(env, cmd, 0);
    proc_cmd_broadcast(env, (u_int *)data, 512);
    (*env)->ReleaseByteArrayElements(env, cmd, (jbyte *)data, JNI_ABORT);
}


JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_jmpx_Jmpx_setStream(JNIEnv *env, jobject jmpx, jobject strm)
{
    MpxCntl *cntl = (MpxCntl *)GetLongField(env, jmpx, "peer");
    typ_mpgbl *gb = (typ_mpgbl *)cntl->gb;	
    
    if (gb->dsrc.jstrm)
	(*env)->DeleteGlobalRef(env, gb->dsrc.jstrm);
    if (strm != NULL) {
	gb->dsrc.jstrm = (*env)->NewGlobalRef(env, strm);
    } else
	gb->dsrc.jstrm = NULL;
}


JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_jmpx_Jmpx_checkMpegFile(JNIEnv *env, jobject jmpx, jobject path)
{
    jint returnVal;
    int width = -1, height = -1;
    char *fpath = (char *)(*env)->GetStringUTFChars(env, path, 0);
    int possibleLength = getMediaDuration(fpath);
    SetIntField(env, jmpx, "possibleLength", possibleLength); 
    returnVal =  (jint)isMpegFile(fpath, &width, &height);
    SetIntField(env, jmpx, "possibleWidth", width);
    SetIntField(env, jmpx, "possibleHeight", height);
    return returnVal;
}

JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_jmpx_Jmpx_getMediaDuration(JNIEnv *env, jobject jmpx, jobject path)
{
    char *fpath = (char *)(*env)->GetStringUTFChars(env, path, 0);
    int possibleLength = getMediaDuration(fpath);
    SetIntField(env, jmpx, "possibleLength", possibleLength); 
    return (jint) 0;
}

JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_jmpx_Jmpx_checkMpegStream(JNIEnv *env, jobject jmpx, jobject jstrm)
{
    jint returnVal;
    int width = -1, height = -1;
    returnVal =  (jint)isMpegStream(env, jstrm, &width, &height);
    SetIntField(env, jmpx, "possibleWidth", width);
    SetIntField(env, jmpx, "possibleHeight", height);
    return returnVal;
}

jobject
Jmpx_getImage(typ_mpgbl *gb, int width, int height, jimage_t *im)
{
    /* Call the getImage method of Jmpx */
    jobject comp = CallObjectMethod(gb->mpx_env, gb->jmpx,
				    "getImage", "(II)Ljava/awt/Component;", width, height);
    /* Do we need a pass up a YUV buffer? */
    im->needYUVBuffer = (int) GetIntField(gb->mpx_env, gb->jmpx, 
					  "needYUVBuffer");
    im->XBGR = (int) GetIntField(gb->mpx_env, gb->jmpx, 
					  "needXBGR");
    im->outWidth = (int) GetIntField(gb->mpx_env, gb->jmpx, 
					  "outWidth");
    im->outHeight = (int) GetIntField(gb->mpx_env, gb->jmpx, 
					  "outHeight");
    im->jarray = (jarray)GetObjectField(gb->mpx_env, gb->jmpx, 
					"rgbBuffer", "Ljava/lang/Object;");
    if (im->jarray == NULL) {
	im->buf = NULL;
	return 0;
    }
    if (im->needYUVBuffer) {
	im->buf = (uint32 *)(*gb->mpx_env)->GetByteArrayElements(gb->mpx_env,
	        				 (jbyteArray)im->jarray, 0);
    } else {
	jboolean isCopy;
	im->buf = (uint32 *)(*gb->mpx_env)->GetIntArrayElements(gb->mpx_env,
						(jintArray)im->jarray, &isCopy);
    }
    /* Release the component because its not being used */
    (*gb->mpx_env)->DeleteLocalRef(gb->mpx_env, comp);
    return 0;
}


void
Jmpx_releaseImage(typ_mpgbl *gb, jimage_t *im)
{
    if (im->needYUVBuffer)
	(*gb->mpx_env)->ReleaseByteArrayElements(gb->mpx_env, 
				 im->jarray, (jbyte *)im->buf, 0);
    else
	(*gb->mpx_env)->ReleaseIntArrayElements(gb->mpx_env, 
				im->jarray, (jint *)im->buf, 0);
    (*gb->mpx_env)->DeleteLocalRef(gb->mpx_env, im->jarray);
}


void
Jmpx_displayImage(typ_mpgbl *gb)
{
    CallVoidMethod(gb->mpx_env, gb->jmpx, "displayImage", "()V"); 
}

long
Jmpx_getContentLength(JNIEnv *env, jobject jmpx)
{
    return (long) CallIntMethod(env, jmpx, "getContentLength", "()I");
}

JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_jmpx_MpxThread_run(JNIEnv *env, jobject thread)
{
    extern void mpxDestroy(MpxCntl *);
    typ_mpgbl *gb = (typ_mpgbl *)GetLongField(env, thread, "mpxData");
    gb->mpx_env = env;
    gb->mpx_thread = (*env)->NewGlobalRef(env, thread);
    mpxThread(gb);
    /* Final clean up. */
    if (gb->mpxCntl != 0)
	mpxDestroy(gb->mpxCntl);
    free(gb);
}


JNIEXPORT void JNICALL
Java_com_sun_media_codec_video_jmpx_DataThread_run(JNIEnv *env, jobject thread)
{
    typ_mpgbl *gb = (typ_mpgbl *)GetLongField(env, thread, "mpxData");
    gb->ds_env = env;
    gb->ds_thread = (*env)->NewGlobalRef(env, thread);
    dserver(gb);
}

