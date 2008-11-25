/*
 * @(#)DirectAudioRenderer.c	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <unistd.h>
#include <stdarg.h>
#include <stdlib.h>
#include <memory.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <signal.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <errno.h>
#include <linux/soundcard.h>
#include <malloc.h>
#include <string.h>
#include "com_sun_media_renderer_audio_DirectAudioRenderer.h"
#include <jni-util.h>

#ifdef DEBUG
#define PRINT(s) printf(s)
#else
#define PRINT(s)
#endif


#define DEV_DSP	"/dev/dsp"
#define DEV_MIXER "/dev/mixer"
#define AUD_BUFSIZ	1024
#define TRUE 		1
#define FALSE		0

typedef struct {
    int dev;
    int mixer;
    int format;
    int rate;
    int bufSize;
    int channels;
    int sampleSize;
    int volume;
} AudioDevice;


jboolean
openDev(AudioDevice * ad)
{
    int error;
    int format = ad->format;
    int stereo = (ad->channels == 2) ? 1 : 0;
    int rate = ad->rate;
    
    ad->dev = open(DEV_DSP, O_WRONLY);
    ad->mixer = open(DEV_MIXER, O_RDWR);
    if (ad->mixer == 0)
	printf("No mixer\n");

    /* Set the format - 16bit or 8 bit */
    error = ioctl(ad->dev, SNDCTL_DSP_SETFMT, &format);
    /* Set the channels to mono or stereo */
    if (error >= 0) {
	error = ioctl(ad->dev, SNDCTL_DSP_STEREO, &stereo);
    }
    /* Set the sample rate */
    if (error >= 0) {
	error = ioctl(ad->dev, SNDCTL_DSP_SPEED, &rate);
    }
    if (error < 0)
	return FALSE;
    else
	return TRUE;
}

void
closeDev(AudioDevice * ad)
{
    close(ad->dev);
    if (ad->mixer)
	close(ad->mixer);
}



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

    /* Store the pointer to the native output device in the java 
       variable "nativeData" */
    SetIntField(env, jad, "nativeData", (int)ad);

    fprintf(stderr, "DirectAudioRenderer: nOpen() \n\trate = %d,"
	    "\n\tsizeInBits = %d, \n\tchannels = %d\n",
	    rate, sizeInBits, channels);

    /* Open the device as non-blocking to check if the device is
       available.  If we open it as blocking device, the open call
       will block if the device is unavailable. */
    ad->volume = 0;
    
    /* Set the format to LINEAR 8/16 bit */
    if (sizeInBits == 16)
	ad->format = AFMT_S16_LE;
    else
	ad->format = AFMT_U8;
    
    ad->channels = channels;
    ad->sampleSize = (sizeInBits / 8) * channels;
    ad->rate = rate;
    
    if (!openDev(ad)) {
	fprintf(stderr, "DirectAudioRenderer: failed to set format: rate = %d, size = %d, # of channels = %d, buffer size = %d\n", rate, sizeInBits, channels, bufSize);
	closeDev(ad);
	return FALSE;
    }

    return TRUE;
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nClose(JNIEnv *env,
							     jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /*fprintf(stderr, "DirectAudioRenderer: nClose()\n");*/

    if (ad != 0) {
	closeDev(ad);
	free(ad);
	SetIntField(env, jad, "nativeData", (int)0);
    }
}

static void
drain(int dev)
{
    ioctl(dev, SNDCTL_DSP_SYNC, 0);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nPause(JNIEnv *env,
							     jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /*fprintf(stderr, "DirectAudioRenderer: nPause()\n");*/

    if (ad == 0)
	return;

    /* TODO */
    /*drain(ad->dev);*/

}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nResume(JNIEnv *env,
							      jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /*fprintf(stderr, "DirectAudioRenderer: nResume()\n");*/

    if (ad == 0)
	return;

    /* TODO */
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nDrain(JNIEnv *env,
							     jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /*fprintf(stderr, "DirectAudioRenderer: nDrain()\n");*/

    if (ad == 0)
	return;

    drain(ad->dev);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nFlush(JNIEnv *env,
							     jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /*fprintf(stderr, "DirectAudioRenderer: nFlush()\n");*/

    if (ad == 0)
	return;

    /* TODO */
    /* drain(ad->dev); */
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nSetGain(JNIEnv *env,
							       jobject jad,
							       jfloat gain)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    int volume;
    
    fprintf(stderr, "DirectAudioRenderer: nSetGain(): %f\n", gain);

    if (ad == 0)
	return;

    if (gain > 1.0)
	gain = 1.0;
    if (gain < 0.0)
	gain = 0.0;
    volume = gain * 100;
    volume = volume | (volume << 8);
    
    if (ad->mixer != 0)
	ioctl(ad->mixer, SOUND_MIXER_WRITE_PCM, &volume);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nSetMute(JNIEnv *env,
							       jobject jad,
							       jboolean mute)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    /* fprintf(stderr, "DirectAudioRenderer: nSetMute()\n"); */

    if (ad == 0)
	return;

    if (ad->mixer != 0) {
	if (mute) {
	    /* Get the old volume */
	    int volume;
	    ioctl(ad->mixer, SOUND_MIXER_READ_PCM, &volume);
	    if (volume != 0)
		ad->volume = volume;
	    volume = 0;
	    ioctl(ad->mixer, SOUND_MIXER_WRITE_PCM, &volume);
	} else {
	    if (ad->volume > 0) {
		ioctl(ad->mixer, SOUND_MIXER_WRITE_PCM, &(ad->volume));
	    }
	}
    }
}


JNIEXPORT jlong JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nGetSamplesPlayed(JNIEnv *env,
									jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    count_info audio_info;

    if (ad == 0 || ioctl(ad->dev, SNDCTL_DSP_GETOPTR, &audio_info) < 0)
	return -1;

    /*fprintf(stderr, "DirectAudioRenderer: nGetSamplesPlayed(): %d\n", audio_info.bytes);*/

    return (jlong) (audio_info.bytes / ad->sampleSize);
}


JNIEXPORT jint JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nBufferAvailable(JNIEnv *env,
							jobject jad)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");

    //fprintf(stderr, "DirectAudioRenderer: nBufferAvailable()\n");

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

    //fprintf(stderr, "DirectAudioRenderer: nWrite(): len=%d, olen=%d\n", len, olen);

    (*env)->ReleaseByteArrayElements(env, jdata, (jbyte *)data, JNI_ABORT);

    return (jlong)olen;
}
