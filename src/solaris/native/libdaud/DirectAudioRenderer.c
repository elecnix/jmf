/*
 * @(#)DirectAudioRenderer.c	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <malloc.h>
#include <sys/utsname.h>
#include "com_sun_media_renderer_audio_DirectAudioRenderer.h"
#include <jni-util.h>

#include <sys/fcntl.h>
#include <sys/audioio.h>
#include <sys/stropts.h>

#ifdef DEBUG
#define PRINT(s) printf(s)
#else
#define PRINT(s)
#endif


#define DEV_AUDIO	"/dev/audio"
#define DEV_AUDIOCTL	"/dev/audioctl"
#define AUD_BUFSIZ	1024
#define TRUE 		1
#define FALSE		0

/* hack solaris8 mixer function, in order not to include mixer.h */
#define MY_MIOC ('M'<<8)
#define MY_AUDIO_MIXER_MULTIPLE_OPEN (MY_MIOC|10)

typedef audio_info_t AudioInfo;

/**
 * Audio Device structure.
 */
typedef struct {
    int dev;
    int ctl;
} AudioDevice;



/*************************************************************************
 * Java Native methods
 *************************************************************************/

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nOpen(JNIEnv *env,
							jobject jad,
							jint rate,
							jint sizeInBits,
							jint channels,
							jint bufSize)
{
    /* Allocate a new structure */
    AudioDevice * ad = (AudioDevice *) malloc(sizeof(AudioDevice));
    AudioInfo info;
    char *devName = (char *)getenv("AUDIODEV");
    struct utsname osDetails;
    char sol8 = 0;
    int ret = 0;

    /* Store the pointer to the native output device in the java 
       variable "nativeData" */
    SetIntField(env, jad, "nativeData", (int)ad);

    /* fprintf(stderr, "DirectAudioRenderer: nOpen()\n"); */

    if (devName == NULL)
	devName = DEV_AUDIO;
    
    /* Open the device as non-blocking to check if the device is
       available.  If we open it as blocking device, the open call
       will block if the device is unavailable. */
    ad->dev = open(devName, O_WRONLY | O_NONBLOCK);

    if (ad->dev < 0) {
	/*fprintf(stderr, "DirectAudioRenderer non-blocking open: cannot open: %s\n", devName);*/
	return FALSE;
    }

    /* Close the device and re-open it as blocking. */
    close(ad->dev);
    ad->dev = open(devName, O_WRONLY);

    /* for solaris 8+ */
    uname(&osDetails);
    osDetails.release[3] = 0;
    if (osDetails.release[0] == '2' || osDetails.release[0] == '5') {
	if (osDetails.release[2] >= '8')
	   sol8 = 1;
	else
	   sol8 = 0;
    } else {
	sol8 = 1;
    }

    if ( sol8 == 1 ) {
	if ( ioctl(ad->dev, MY_AUDIO_MIXER_MULTIPLE_OPEN) < 0 ) {
	    fprintf(stderr, "dev/audio failed to set to multiple open mode\n");
	}
   }

    /* Set the output format */
    AUDIO_INITINFO(&info);
    info.play.encoding = AUDIO_ENCODING_LINEAR;
    info.play.sample_rate = (int)rate;
    info.play.channels = (int)channels;
    info.play.precision = (int)sizeInBits;
    info.play.buffer_size = bufSize;

    if ((ret = ioctl(ad->dev, AUDIO_SETINFO, &info)) < 0) {
        fprintf(stderr, "audio return value = %d \n", ret);
	fprintf(stderr, "audio DirectAudioRenderer: failed to set format: rate = %d, size = %d, # of channels = %d, buffer size = %d\n", rate, sizeInBits, channels, bufSize);
	close(ad->dev);
	return FALSE;
    }

    return TRUE;
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nClose(JNIEnv *env,
							jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /* fprintf(stderr, "DirectAudioRenderer: nClose()\n"); */

    if (ad != 0) {
	close(ad->dev);
	free(ad);
	SetIntField(env, jad, "nativeData", (int)0);
    }
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nPause(JNIEnv *env,
							jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    AudioInfo info;

    /* fprintf(stderr, "DirectAudioRenderer: nPause()\n"); */

    if (ad == 0)
	return;

    AUDIO_INITINFO(&info);
    info.play.pause = 1;
    ioctl(ad->dev, AUDIO_SETINFO, &info);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nResume(JNIEnv *env,
							jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    AudioInfo info;

    /* fprintf(stderr, "DirectAudioRenderer: nResume()\n"); */

    if (ad == 0)
	return;

    AUDIO_INITINFO(&info);
    info.play.pause = 0;
    ioctl(ad->dev, AUDIO_SETINFO, &info);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nDrain(JNIEnv *env,
							jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /* fprintf(stderr, "DirectAudioRenderer: nDrain()\n"); */

    if (ad == 0)
	return;

    ioctl(ad->dev, AUDIO_DRAIN);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nFlush(JNIEnv *env,
							jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /* fprintf(stderr, "DirectAudioRenderer: nFlush()\n"); */

    if (ad == 0)
	return;

    ioctl(ad->dev, I_FLUSH, FLUSHW);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nSetGain(JNIEnv *env,
							jobject jad,
							jfloat gain)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    AudioInfo info;

    /* fprintf(stderr, "DirectAudioRenderer: nSetGain(): %f\n", gain); */

    if (ad == 0)
	return;

    AUDIO_INITINFO(&info);
    info.play.gain = (int)(gain * 255.0);   /* range from 0..255 */
    ioctl(ad->dev, AUDIO_SETINFO, &info);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nSetMute(JNIEnv *env,
							jobject jad,
							jboolean mute)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    AudioInfo info;

    /* fprintf(stderr, "DirectAudioRenderer: nSetMute()\n"); */

    if (ad == 0)
	return;

    AUDIO_INITINFO(&info);
    info.output_muted = (int)mute;
    ioctl(ad->dev, AUDIO_SETINFO, &info);
}


JNIEXPORT jlong JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nGetSamplesPlayed(JNIEnv *env,
							jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    AudioInfo info;

    if (ad == 0 || ioctl(ad->dev, AUDIO_GETINFO, &info) < 0)
	return -1;

    /* fprintf(stderr, "DirectAudioRenderer: nGetSamplesPlayed(): %d\n", info.play.samples); */

    return (jlong) info.play.samples;
}


JNIEXPORT jint JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nBufferAvailable(JNIEnv *env,
							jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /* fprintf(stderr, "DirectAudioRenderer: nBufferAvailable()\n"); */

    return (jlong) 0;
}


JNIEXPORT jint JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nWrite(JNIEnv *env,
							jobject jad,
							jbyteArray jdata,
							jint off,
							jint len)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    int olen;
    char *data;

    data = (char *) (*env)->GetByteArrayElements(env, jdata, 0);

    olen = write(ad->dev, data + off, len);

    /* fprintf(stderr, "DirectAudioRenderer: nWrite(): %d\n", olen); */

    (*env)->ReleaseByteArrayElements(env, jdata, (jbyte *)data, JNI_ABORT);

    return (jlong)olen;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nCanMix(JNIEnv *env,
							      jclass jad)
{
    struct utsname osDetails;
    uname(&osDetails);
    osDetails.release[3] = 0;
    if (osDetails.release[0] == '2' ||
	osDetails.release[0] == '5') {
	if (osDetails.release[2] >= '8')
	    return (jboolean) 1;
	else
	    return (jboolean) 0;
    } else {
	return (jboolean) 1;
    }
}
    
