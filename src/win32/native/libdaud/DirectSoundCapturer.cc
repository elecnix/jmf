/*
 * @(#)DirectSoundCapturer.cc	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <malloc.h>
#include <fcntl.h>
#include <io.h>
#include "com_sun_media_protocol_dsound_DSound.h"
#include <jni-util.h>

#include <windows.h>
#include <mmsystem.h>
#include <dsound.h>

typedef struct _DSound {
    LPDIRECTSOUNDCAPTUREBUFFER dsBuffer;
    WAVEFORMATEX wfe;
    int readPos;
    int bufSize;
    int started;
} DSound;

LPDIRECTSOUNDCAPTURE directSoundCapture = NULL;

#define NBUFFERS 4

static int available(DSound *lpdsound)
{
    DWORD readPos, capturePos;
    HRESULT result;

    result = lpdsound->dsBuffer->GetCurrentPosition(&capturePos, &readPos);
    //printf("capturePos = %d, readPos = %d\n", capturePos, readPos);
    if (lpdsound->readPos <= readPos) {
	return readPos - lpdsound->readPos;
    } else {
	return readPos + ((lpdsound->bufSize * NBUFFERS) - lpdsound->readPos);
    }
}

/*
 * Class:     com_sun_media_protocol_dsound_DSound
 * Method:    nOpen
 * Signature: (IIII)J
 */
JNIEXPORT jlong JNICALL
Java_com_sun_media_protocol_dsound_DSound_nOpen(JNIEnv *env,
						jobject dsound,
						jint sampleRate,
						jint sampleSize,
						jint channels,
						jint bufferSize)
{
    HRESULT result;
    HWND hwnd;
    WAVEFORMATEX wfe;
    DSCBUFFERDESC dscbd;
    LPDIRECTSOUNDCAPTUREBUFFER lpdscb;
    DSound * lpdsound;
    /*
    if (directSound == NULL) {
	result = DirectSoundCreate(NULL, &directSound, NULL);
	if (result != DS_OK) {
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
    }
    */
    if (directSoundCapture == NULL) {
	result = DirectSoundCaptureCreate(NULL, &directSoundCapture, NULL);
	if (result != DS_OK) {
	    return 0;
	}
    }

    wfe.wFormatTag = WAVE_FORMAT_PCM;
    wfe.nChannels = channels;
    wfe.nSamplesPerSec = sampleRate;
    wfe.nBlockAlign = channels * sampleSize / 8;
    wfe.nAvgBytesPerSec = sampleRate * wfe.nBlockAlign;
    wfe.wBitsPerSample = sampleSize;

    dscbd.dwSize = sizeof(DSCBUFFERDESC);
    
    dscbd.dwFlags = 0;
    dscbd.dwBufferBytes = bufferSize * NBUFFERS;
    dscbd.dwReserved = 0;
    dscbd.lpwfxFormat = &wfe;

    result = directSoundCapture->CreateCaptureBuffer(&dscbd, &lpdscb, NULL);
    if (result == DS_OK) {
	lpdsound = (DSound *) malloc(sizeof(DSound));
	lpdsound->dsBuffer = lpdscb;
	lpdsound->bufSize = bufferSize;
	lpdsound->wfe = wfe;
	lpdsound->readPos = 0;
	lpdsound->started = FALSE;
	return (jlong) lpdsound;
    } else {
	return 0;
    }
    
}

/*
 * Class:     com_sun_media_protocol_dsound_DSound
 * Method:    nStart
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_dsound_DSound_nStart(JNIEnv *env,
						 jobject dsound,
						 jlong peer)
{
    DSound * lpdsound = (DSound *) peer;
    
    if (peer == 0)
	return;

    lpdsound->started = TRUE;
    lpdsound->readPos = 0;
    lpdsound->dsBuffer->Start(DSCBSTART_LOOPING);
}

/*
 * Class:     com_sun_media_protocol_dsound_DSound
 * Method:    nStop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_dsound_DSound_nStop(JNIEnv *env,
						jobject dsound,
						jlong peer)
{
    DSound * lpdsound = (DSound *) peer;
    
    if (peer == 0)
	return;
    lpdsound->started = FALSE;

    lpdsound->dsBuffer->Stop();
}

/*
 * Class:     com_sun_media_protocol_dsound_DSound
 * Method:    nFlush
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_dsound_DSound_nFlush(JNIEnv *env,
						 jobject dsound,
						 jlong jdsBuffer)
{

}

/*
 * Class:     com_sun_media_protocol_dsound_DSound
 * Method:    nRead
 * Signature: (J[BII)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_dsound_DSound_nRead(JNIEnv *env,
						jobject dsound,
						jlong peer,
						jbyteArray jdata,
						jint offset,
						jint len)
{
    DSound * lpdsound = (DSound *) peer;
    jbyte * data;
    HRESULT result;
    void *ptr1, *ptr2;
    DWORD size1, size2;
    int sizeToRead;

    if (lpdsound == NULL)
	return -1;

    if (!lpdsound->started)
	return 0;
    
    while (available(lpdsound) < lpdsound->bufSize) {
	Sleep(15);
    }

    data = (jbyte *) env->GetByteArrayElements(jdata, 0);
    sizeToRead = lpdsound->bufSize & ~0x3;
    if (sizeToRead > len)
	sizeToRead = len;
    result = lpdsound->dsBuffer->Lock(lpdsound->readPos,
				      sizeToRead,
				      &ptr1, &size1,
				      &ptr2, &size2,
				      0);
    if (result != DS_OK)
	return -1;
    len = 0;
    if (ptr1 != NULL && size1 > 0) {
	memcpy(data + offset, ptr1, size1);
	offset += size1;
	len = size1;
	if (ptr2 != NULL && size2 > 0) {
	    memcpy(data + offset, ptr2, size2);
	    offset += size2;
	    len += size2;
	}
    }
    lpdsound->readPos = (lpdsound->readPos + len) %
	                (lpdsound->bufSize * NBUFFERS);
    result = lpdsound->dsBuffer->Unlock(ptr1, size1, ptr2, size2);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, 0);
    
    return len;
}

/*
 * Class:     com_sun_media_protocol_dsound_DSound
 * Method:    nClose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_dsound_DSound_nClose(JNIEnv *env,
						 jobject dsound,
						 jlong peer)
{
    DSound * lpdsound = (DSound *) peer;
    if (lpdsound == NULL)
	return;
    if (lpdsound->dsBuffer) {
	lpdsound->dsBuffer->Release();
	lpdsound->dsBuffer = NULL;
    }
    
    if (directSoundCapture != NULL) {
	directSoundCapture->Release();
	directSoundCapture = NULL;
    }
    
    free(lpdsound);
}
