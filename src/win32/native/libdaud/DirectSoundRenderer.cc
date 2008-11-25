/*
 * @(#)DirectSoundRenderer.cc	1.17 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <malloc.h>
#include <fcntl.h>
#include <io.h>
#include "com_sun_media_renderer_audio_DirectAudioRenderer.h"
#include <jni-util.h>

#include <windows.h>
#include <mmsystem.h>
#include <dsound.h>


#ifdef DEBUG
#define PRINT(s) printf(s)
#else
#define PRINT(s)
#endif

#define TRUE 	1
#define FALSE 	0

#define NUM_BUFFERS	20

#define GAP 32

LPDIRECTSOUND directSound = NULL;
static LPDIRECTSOUNDBUFFER primaryBuffer = NULL;

/**
 * Audio Device structure.
 */
typedef struct {
    LPDIRECTSOUNDBUFFER dsBuffer;
    int nBlockAlign;
    INT64 bytesWrittenSoFar;
    int bufSize;
    int writePos;
    DWORD writeCursor;
    DWORD playCursor;
    int started;
    int prefetchBufSize;
    int bytesPrefetched;
    LONG volume;
    int mute;
    int bytesPer10ms;
    int bytesOfSilence;
    INT64 lastTimeStamp;
    float rate;
    int sampleRate;
    int sampleSize;
    unsigned char silence;
    boolean justFlushed;
} AudioDevice;


// LOCAL FUNCTION
DWORD
getAvailable(AudioDevice *ad)
{
    DWORD available = 0;

    ad->dsBuffer->GetCurrentPosition(&(ad->playCursor), &(ad->writeCursor));
    //printf("playc = %d, ad->writePos = %d\n", playCursor, ad->writePos);
    if (ad->playCursor > ad->writePos)
        available =  ad->playCursor - ad->writePos;
    else
        available = ad->bufSize - (ad->writePos - ad->playCursor);
    if (available < 0)
	available = 0;
    //fprintf(stderr, "buffer available = %d\n", available);
    return available;
}

DWORD
getQueued(AudioDevice *ad)
{
    int queued = (ad->bufSize - getAvailable(ad));
    if (queued + ad->lastTimeStamp > ad->bytesWrittenSoFar) {
	//printf("WrapAround during underflow\n");
	return 0;
    } else
	return queued;
}

void
backupSilence(AudioDevice * ad)
{
    DWORD readPos, writePos;
    DWORD distance;

    ad->dsBuffer->GetCurrentPosition(&readPos, &writePos);

    if (ad->writePos > writePos)
	distance = ad->writePos - writePos;
    else
	distance = ad->bufSize - (writePos - ad->writePos);

    if (distance > ad->bytesOfSilence)
	distance = ad->bytesOfSilence;
    //printf("---   backing up %d\n", distance);
    ad->writePos = (ad->writePos - distance + ad->bufSize) % ad->bufSize;
    ad->bytesOfSilence = 0;
}

void
fillInSilence(AudioDevice * ad, int howMuch)
{
    void * writePtr1;
    void * writePtr2;
    DWORD writeSize1;
    DWORD writeSize2;
    HRESULT result;
    int written = 0;
    DWORD available;

    char *data;

    //fprintf(stderr, "DirectAudioRenderer: nWrite(): %d\n", len);

    if (ad == NULL)
	return;

    result = ad->dsBuffer->Lock(ad->writePos, howMuch,
				&writePtr1, &writeSize1,
				&writePtr2, &writeSize2,
				0);
    //fprintf(stderr, "available = %d, wp1 = %d, ws1 = %d, wpos = %d, result = %x\n",
    //	    available, writePtr1, writeSize1, ad->writePos, result);

    if (result == DS_OK) {

	if (writeSize1 > 0 && writePtr1 != NULL) {
	    memset(writePtr1, ad->silence, writeSize1);
	    written += writeSize1;
	}
	if (writeSize2 > 0 && writePtr2 != NULL) {
	    memset(writePtr2, ad->silence, writeSize2);
	    written += writeSize2;
	}
	ad->bytesOfSilence += written;
	//printf("---   Inserting %d\n", written);
	result = ad->dsBuffer->Unlock(writePtr1, writeSize1, writePtr2, writeSize2);
    } else if (result == DSERR_BUFFERLOST) {
	printf("DSERR_BUFFERLOST\n");
    } else if (result == DSERR_INVALIDPARAM) {
	printf("DSERR_INVALIDPARAM\n");
    } else if (result == DSERR_INVALIDCALL) {
	printf("DSERR_INVALIDCALL\n");
    } else if (result == DSERR_PRIOLEVELNEEDED) {
	printf("DSERR_PRIOLEVELNEEDED\n");
    }
}

/*************************************************************************
 * Java Native methods
 *************************************************************************/

JNIEXPORT jint JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nOpen(JNIEnv *env,
							jobject jad,
							jint rate,
							jint sizeInBits,
							jint channels,
							jint bufSize)
{
    /* Allocate a new structure */
    AudioDevice *ad = (AudioDevice *) malloc(sizeof(AudioDevice));
    HRESULT result;
    HWND hwnd = NULL;
    DSBUFFERDESC desc;
    WAVEFORMATEX pcmwf;

    memset(&desc, 0, sizeof(DSBUFFERDESC));
    desc.dwSize = sizeof(DSBUFFERDESC);

    pcmwf.wFormatTag = WAVE_FORMAT_PCM;
    pcmwf.nChannels = 2;
    pcmwf.nSamplesPerSec = 44100;
    pcmwf.nAvgBytesPerSec = 44100 * 2 * 2;
    pcmwf.nBlockAlign = 4;
    pcmwf.wBitsPerSample = 16;
    pcmwf.cbSize = 0;
    
    //fprintf(stderr, "DirectAudioRenderer: nOpen(): rate = %d, size = %d, channels = %d, bufSize = %d\n", rate, sizeInBits, channels, bufSize);

    // Create the direct sound object
    if (directSound == NULL) {
	result = DirectSoundCreate(NULL, &directSound, NULL);
	if (result != DS_OK) {
	    printf("Could not create DirectSound\n");
	    return 0;
	}
	// Set cooperative level
	hwnd = GetForegroundWindow();
	if (hwnd == NULL)
	    hwnd = GetDesktopWindow();
	result = directSound->SetCooperativeLevel(hwnd, DSSCL_PRIORITY);
	if (result != DS_OK) {
	    printf("Error getting CooperativeLevel\n");
	}
	/*
 	desc.dwFlags = DSBCAPS_CTRLFREQUENCY |
 	               DSBCAPS_GETCURRENTPOSITION2 |
 	               DSBCAPS_CTRLVOLUME |
 	               DSBCAPS_PRIMARYBUFFER  | DSBCAPS_GLOBALFOCUS; 
	*/

	
	desc.dwFlags = DSBCAPS_PRIMARYBUFFER;
	desc.dwBufferBytes = 0;
	desc.lpwfxFormat = NULL;
	
	//desc.guid3DAlgorithm = 0;
	
	// Create the primary buffer
	result = directSound->CreateSoundBuffer(&desc, &primaryBuffer, NULL);
	if (primaryBuffer == NULL) {
	    printf("Could not create primary buffer %8x\n", result);
	    return 0;
	}
	// Set the primary buffer format to 44100, 16bit stereo
	result = primaryBuffer->SetFormat(&pcmwf);
	if (result != DS_OK) {
	    printf("Couldn't set format for primary buffer\n");
	    return 0;
	}
	
    }

    pcmwf.nChannels = channels;
    pcmwf.nSamplesPerSec = rate;
    pcmwf.nAvgBytesPerSec = rate * (sizeInBits / 8) * channels;
    pcmwf.nBlockAlign = (sizeInBits / 8 ) * channels;
    pcmwf.wBitsPerSample = sizeInBits;

    ad->bytesPer10ms = pcmwf.nAvgBytesPerSec / 100;
    ad->nBlockAlign = pcmwf.nBlockAlign;
    ad->bytesWrittenSoFar = 0;
    ad->lastTimeStamp = 0;
    ad->bufSize = pcmwf.nAvgBytesPerSec * 2; // 2000 millisecs buffer
    ad->writePos = 0;
    ad->prefetchBufSize = (pcmwf.nAvgBytesPerSec / 8) * 2;
    ad->bytesPrefetched = 0;
    ad->sampleRate = rate;
    ad->sampleSize = sizeInBits;
    ad->volume = 0;
    ad->mute = FALSE;
    ad->started = FALSE;
    ad->rate = 1.0;
    ad->justFlushed = TRUE;
    ad->bytesOfSilence = 0;
    ad->silence = (sizeInBits == 16) ? 0 : (unsigned char) 0x80;
    
    desc.dwFlags =
	DSBCAPS_CTRLFREQUENCY |
	DSBCAPS_CTRLVOLUME |
	DSBCAPS_GETCURRENTPOSITION2 |
	DSBCAPS_GLOBALFOCUS;
    desc.dwBufferBytes = ad->bufSize;
    desc.lpwfxFormat = &pcmwf;
    
    result = directSound->CreateSoundBuffer(&desc, &(ad->dsBuffer), NULL);
    if (result != DS_OK) {
	printf("Couldnt create secondary sound buffer\n");
	return 0;
    }
    ad->dsBuffer->SetCurrentPosition(0);
    //printf("Using DirectSound\n");
    return (jint) ad;
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nClose(JNIEnv *env,
							     jobject jad,
							     jint jpeer)
{
    AudioDevice *ad = (AudioDevice *) jpeer;

    //fprintf(stderr, "DirectAudioRenderer: nClose()\n");
        
    if (ad == 0)
	return;
    ad->dsBuffer->Release();
    free(ad);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nPause(JNIEnv *env,
							     jobject jad,
							     jint jpeer)
{
    AudioDevice *ad = (AudioDevice *) jpeer;

    //fprintf(stderr, "DirectAudioRenderer: nPause()\n");

    if (ad == 0)
	return;

    ad->started = FALSE;
    ad->dsBuffer->Stop();
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nResume(JNIEnv *env,
							      jobject jad,
							      jint jpeer)
{
    AudioDevice *ad = (AudioDevice *) jpeer;

    //fprintf(stderr, "DirectAudioRenderer: nResume()\n");

    if (ad == 0)
	return;

    if (ad->started)
	return;

    ad->started = TRUE;
    //printf("play = %d\n",
    ad->dsBuffer->Play(0, 0, DSBPLAY_LOOPING);
    //primaryBuffer->Play(0, 0, DSBPLAY_LOOPING);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nDrain(JNIEnv *env,
							     jobject jad,
							     jint jpeer)
{
    AudioDevice *ad = (AudioDevice *) jpeer;
    DWORD available, availableNow;
    //fprintf(stderr, "DirectAudioRenderer: nDrain()\n");

    if (ad == 0)
	return;
    if (ad->bytesWrittenSoFar != 0) {
	available = getAvailable(ad);
	fillInSilence(ad, available);
	//printf("available = %d\n", available);
	if (available < ad->bufSize && ad->started) {
	    do {
		Sleep(10);
		availableNow = getAvailable(ad);
		if (availableNow < available)
		    break;
		available = availableNow;
		//printf("availableNow = %d\n", availableNow);
	    } while (true);
	}
    }
    //printf("Done drain.\n");
    ad->started = FALSE;
    ad->dsBuffer->Stop();
    ad->dsBuffer->SetCurrentPosition(ad->writePos);
    ad->bytesPrefetched = 0;
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nFlush(JNIEnv *env,
							     jobject jad,
							     jint jpeer)
{
    AudioDevice *ad = (AudioDevice *) jpeer;

    //fprintf(stderr, "DirectAudioRenderer: nFlush()\n");

    if (ad == 0)
	return;

    ad->started = FALSE;
    ad->dsBuffer->Stop();
    ad->dsBuffer->SetCurrentPosition(ad->writePos);
    ad->bytesPrefetched = 0;
    ad->justFlushed = TRUE;
    ad->bytesWrittenSoFar = 0;
    ad->lastTimeStamp = 0;
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nSetGain(JNIEnv *env,
							       jobject jad,
							       jint jpeer,
							       jfloat gain)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    LONG volume;
    
    //fprintf(stderr, "DirectAudioRenderer: nSetGain(): %f\n", gain);

    if (ad == 0)
	return;

    volume = gain * 100;
    if (volume > DSBVOLUME_MAX)
	volume = DSBVOLUME_MAX;
    else if (volume < DSBVOLUME_MIN)
	volume = DSBVOLUME_MIN;
    ad->volume = volume;
    if (!ad->mute)
	ad->dsBuffer->SetVolume(volume);
}


JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nSetMute(JNIEnv *env,
							       jobject jad,
							       jint jpeer,
							       jboolean mute)
{
    AudioDevice *ad = (AudioDevice *) jpeer;

    //fprintf(stderr, "DirectAudioRenderer: nSetMute()\n");

    if (ad == 0)
	return;

    if (mute) {
	ad->mute = TRUE;
	ad->dsBuffer->GetVolume(&(ad->volume));
	ad->dsBuffer->SetVolume(DSBVOLUME_MIN);
    } else {
	ad->mute = FALSE;
	ad->dsBuffer->SetVolume(ad->volume);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nSetFrequency(
							       JNIEnv *env,
							       jobject jad,
							       jint jpeer,
							       jint frequency)
{
    AudioDevice *ad = (AudioDevice *) jpeer;
    HRESULT result;
    
    //fprintf(stderr, "DirectAudioRenderer: nSetMute()\n");

    if (ad == 0)
	return (jboolean) 0;

    result = ad->dsBuffer->SetFrequency(frequency);
    if (result == DS_OK) {
	ad->rate = (float) frequency / ad->sampleRate;
	return (jboolean) 1;
    } else
	return (jboolean) 0;
}


jlong
getSamplesPlayed(JNIEnv *env,
		 jobject jad,
		 jint jpeer, int correctUnderflow)
{
    AudioDevice *ad = (AudioDevice *) jpeer;
    DWORD available;
    INT64 bytesPlayed;
    
    if (ad == NULL)
	return (jlong) -1;
    // Get the empty space in buffer and subtract from total buffer size
    // This gives the bytes yet to be played
    // Bytes written so far minus bytes yet to be played gives the
    // current play position
    available = getAvailable(ad);
    bytesPlayed = ad->bytesWrittenSoFar - (INT64) (ad->bufSize - available);
    if (bytesPlayed > ad->bytesWrittenSoFar) {
	//printf("==V Underflow\n");
	bytesPlayed = ad->bytesWrittenSoFar;
    }
    if (bytesPlayed < ad->lastTimeStamp) {
	if (ad->lastTimeStamp != 0 && correctUnderflow) {
	    ad->writePos = ad->writeCursor;
	    //printf("== Corrected Underflow av=%d pc=%d writePos=%d\n", available, ad->playCursor, ad->writePos);
	}
	bytesPlayed = ad->lastTimeStamp;
    }
    ad->lastTimeStamp = bytesPlayed;
    return (jlong) bytesPlayed / (INT64) ad->nBlockAlign;
}

JNIEXPORT jlong JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nGetSamplesPlayed(JNIEnv *env,
									jobject jad,
									jint jpeer)
{
    return getSamplesPlayed(env, jad, jpeer, 0);
}


JNIEXPORT jint JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nBufferAvailable(JNIEnv *env,
								       jobject jad,
								       jint jpeer)
{
    AudioDevice *ad = (AudioDevice *) jpeer;
    if (ad == NULL)
	return 0;
    else {
	DWORD available = getAvailable(ad);
	if (available < GAP)
	    available = 0;
	else
	    available -= GAP;
	//available = 0;
	if (!ad->started) {
	    // Prefetching
	    if (available > ad->prefetchBufSize - ad->bytesPrefetched)
		available = ad->prefetchBufSize - ad->bytesPrefetched;
	}
	return available;
    }
}

JNIEXPORT void JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nCheckUnderflow(JNIEnv *env,
								      jobject jad,
								      jint jpeer)
{
    AudioDevice *ad = (AudioDevice *) GetIntField(env, jad, "nativeData");
    DWORD queued;
    DWORD silenceToFill;

    //fprintf(stderr, "DirectAudioRenderer: nCheckUnderflow()\n");

    if (ad == 0)
	return;
    if (!ad->started)
	return;

    // Fill 100 ms of silence
    silenceToFill = (DWORD) (ad->bytesPer10ms * 5.0 * ad->rate);
    silenceToFill &= ~3;

    queued = getQueued(ad);
    
    if (queued < silenceToFill) {
	fillInSilence(ad, ad->bufSize - silenceToFill);
	//fprintf(stderr, "---Inserted silence\n");
	ad->writePos = (ad->writePos + silenceToFill) %
	                ad->bufSize;
    }
}

void
doSwapBytes(char * dst, char * src, int size)
{
    int i;
    for (i = 0; i < size; i+=2) {
	dst[i] = src[i + 1];
	dst[i + 1] = src[i];
    }
}

void
doSignChange(char * dst, char * src, int size)
{
    int i;
    for (i = 0; i < size; i++) {
	dst[i] = src[i] + (char)(-128);
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_media_renderer_audio_DirectAudioRenderer_nWrite(JNIEnv *env,
							     jobject jad,
							     jint jpeer,
							     jbyteArray jdata,
							     jint off,
							     jint len,
							     jboolean swapBytes,
							     jboolean signChange)
{
    AudioDevice *ad = (AudioDevice *) jpeer;
    char * writePtr1;
    char * writePtr2;
    DWORD writeSize1;
    DWORD writeSize2;
    HRESULT result;
    int written = 0;
    DWORD available;

    char *data;

    //fprintf(stderr, "DirectAudioRenderer: nWrite(): %d\n", len);

    if (ad == NULL)
	return 0;

    if (ad->justFlushed) {
	DWORD playCursor, writeCursor;
	ad->dsBuffer->GetCurrentPosition(&playCursor, &writeCursor);
	// Force the write pointer to just after the committed portion
	// of the circular buffer
	ad->writePos = writeCursor;
	ad->justFlushed = FALSE;
	//Java_com_sun_media_renderer_audio_DirectAudioRenderer_nCheckUnderflow(env, jad, jpeer);
    } else if (ad->bytesOfSilence > 0) {
	//backupSilence(ad);
    }

    // Correct Underflows
    getSamplesPlayed(env, jad, jpeer, 1);
    
    available = getAvailable(ad);
    if (available < GAP)
	available = 0;
    else
	available -= GAP;
    //printf("available = %d\n", available);
    
    if (available < ad->bufSize / 4) {
	//fprintf(stderr, "sleeping 10\n");
	Sleep(100);
	return 0;
    }

    if (available > len)
	available = len;
    if (swapBytes) {
	available = available - (available & 1);
    }

    if (available == 0)
	return 0;
    
    if (!ad->started) {
	// Prefetching
	if (ad->bytesPrefetched >= ad->prefetchBufSize) {
	    //printf("ret 0, prefetched\n");
	    return 0;
	}
	if (available > ad->prefetchBufSize - ad->bytesPrefetched)
	    available = ad->prefetchBufSize - ad->bytesPrefetched;
	ad->bytesPrefetched += available;
	//printf("bytesPrefetched = %d\n", ad->bytesPrefetched);
    } else {
	ad->bytesPrefetched = 0;
    }
    
    data = (char *) env->GetByteArrayElements(jdata, 0);

    result = ad->dsBuffer->Lock(ad->writePos, available,
				(void **) &writePtr1, &writeSize1,
				(void **) &writePtr2, &writeSize2,
				0);
    //fprintf(stderr, "available = %d, wp1 = %d, ws1 = %d, wpos = %d, result = %x\n",
    //    available, writePtr1, writeSize1, ad->writePos, result);
    
    if (result == DS_OK) {
	if (writeSize1 > 0 && writePtr1 != NULL) {
	    if (!swapBytes && !signChange) { 
		memcpy(writePtr1, data + off, writeSize1);
	    } else {
		if (swapBytes)
		    doSwapBytes(writePtr1, data + off, writeSize1);
		else if (signChange)
		    doSignChange(writePtr1, data + off, writeSize1);
	    }
	    written += writeSize1;
	    ad->bytesWrittenSoFar += writeSize1;
	    ad->writePos += writeSize1;
	    if (ad->writePos >= ad->bufSize)
		ad->writePos = 0;
	}
	if (writeSize2 > 0 && writePtr2 != NULL) {
	    if (!swapBytes && !signChange) {
		memcpy(writePtr2, data + off + written, writeSize2);
	    } else {
		if (swapBytes)
		    doSwapBytes(writePtr2, data + off + written, writeSize2);
		else if (signChange)
		    doSignChange(writePtr2, data + off + written, writeSize2);
	    }
	    written += writeSize2;
	    ad->bytesWrittenSoFar += writeSize2;
	    ad->writePos += writeSize2;
	    if (ad->writePos >= ad->bufSize)
		ad->writePos = 0;
	}
	ad->bytesOfSilence = 0;
	result = ad->dsBuffer->Unlock(writePtr1, writeSize1, writePtr2, writeSize2);
	//fprintf(stderr, "started = %d, result = %d, written %d\n", ad->started, result, written);
	//ad->dsBuffer->Play(0, 0, DSBPLAY_LOOPING);
    } else if (result == DSERR_BUFFERLOST) {
	printf("DSERR_BUFFERLOST\n");
    } else if (result == DSERR_INVALIDPARAM) {
	printf("DSERR_INVALIDPARAM\n");
    } else if (result == DSERR_INVALIDCALL) {
	printf("DSERR_INVALIDCALL\n");
    } else if (result == DSERR_PRIOLEVELNEEDED) {
	printf("DSERR_PRIOLEVELNEEDED\n");
    }
    
    env->ReleaseByteArrayElements(jdata, (jbyte *)data, JNI_ABORT);
    
    return (jlong) written;
}

