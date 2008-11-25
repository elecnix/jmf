#include <stdio.h>
#include <windows.h>
#include <mmsystem.h>
#include <jni.h>

#include "com_ibm_media_protocol_device_DataSource.h"
#include "com_ibm_media_protocol_device_DevicePushSourceStream.h"

/* pointer to handler of device */
HWAVEIN hwi; 

/* WAVEHDR to be used as the buffer holder */
WAVEHDR waveHeader;

/* Indicates if the buffer sent to the device was filled */
boolean isBufferFilled = TRUE; /* we initialize it to TRUE for bootstraping
									of the transferData call from Java */

/* Indicates that the device was started */
boolean isStarted = FALSE;

/*
 * callback function for events handling
 */
void CALLBACK waveInProc(HWAVEIN hwi, UINT uMsg, DWORD dwInstance, DWORD dwParam1, 
						 DWORD dwParam2) {
	
	if (uMsg = WIM_DATA)
		isBufferFilled = TRUE;
}

/*
 * Class:     com_ibm_media_protocol_device_DataSource
 * Method:    connectDevice
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_protocol_device_DataSource_connectDevice
(JNIEnv* env, jobject obj) {

	MMRESULT result;
	/* we current use PCM, 22050, mono, 16bit */
	WAVEFORMATEX format = { WAVE_FORMAT_PCM, 1, 22050, 44100 , 2, 16, 0 };
		
	result = waveInOpen(&hwi, WAVE_MAPPER, &format, waveInProc, NULL, 
		CALLBACK_FUNCTION);
	/* DEBUG */
	if (result == MMSYSERR_NOERROR)
		printf("Device opened\n");
	else 
		printf("ERROR opening device !");
	/* end DEBUG */

	printf("no. of devices %d\n", waveInGetNumDevs());
}

/*
 * Class:     com_ibm_media_protocol_device_DataSource
 * Method:    disconnectDevice
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_protocol_device_DataSource_disconnectDevice
(JNIEnv* env, jobject obj) {

	MMRESULT result;

	/* first mark all pending buffers as done */
	result = waveInReset(hwi); 
	
	/* DEBUG */
	if (result == MMSYSERR_NOERROR)
		printf("Device reset\n");
	else
		printf("ERROR reseting device !\n");
	/* end DEBUG */

	result = waveInClose(hwi);

	/* DEBUG */
	if (result == MMSYSERR_NOERROR)
		printf("Device closed\n");
	else
		printf("ERROR closing device !\n");
	/* end DEBUG */

}

/*
 * Class:     com_ibm_media_protocol_device_DataSource
 * Method:    stopDevice
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_protocol_device_DataSource_stopDevice
(JNIEnv* env, jobject obj) {
	
	MMRESULT result = waveInStop(hwi); 

	/* DEBUG */
	if (result == MMSYSERR_NOERROR)
		printf("Device stoped\n");
	else
		printf("ERROR stoping device !\n");
	/* end DEBUG */

	isStarted = FALSE;
}

/*
 * start the actual device 
 */
void startDevice() {

	MMRESULT result = waveInStart(hwi); 

	/* DEBUG */
	if (result == MMSYSERR_NOERROR)
		printf("Device started\n");
	else
		printf("ERROR starting device !\n");
	/* end DEBUG */

	isStarted = TRUE;
}

/*
 * Class:     com_ibm_media_protocol_device_DevicePushSourceStream
 * Method:    read
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_media_protocol_device_DevicePushSourceStream_read
(JNIEnv* env, jobject obj, jbyteArray arr, jint offset, jint length) {
	
	MMRESULT result;

	jclass cls;
	jmethodID mid;
	jsize len = (*env)->GetArrayLength(env, arr);
	jbyte* body = (*env)->GetByteArrayElements(env, arr, JNI_FALSE);

	/* fill the buffer struct */
	waveHeader.lpData = &body[offset];
	waveHeader.dwBufferLength = length;
	waveHeader.dwBytesRecorded = 0; 
	waveHeader.dwUser = NULL;
	waveHeader.dwFlags = NULL;
	waveHeader.dwLoops = NULL;
	waveHeader.lpNext = NULL;
	waveHeader.reserved = NULL;

	isBufferFilled = FALSE;

	/* preper the buffer */
	result = waveInPrepareHeader(hwi, &waveHeader, sizeof(WAVEHDR)); 
	/* DEBUG */
	if (result != MMSYSERR_NOERROR)
		printf("ERROR while adding buffer to device !\n %d\n", result);
	/* end DEBUG */

	result = waveInAddBuffer(hwi, &waveHeader, sizeof(WAVEHDR));
	/* DEBUG */
	if (result != MMSYSERR_NOERROR)
		printf("ERROR while adding buffer to device !\n %d\n", result);
	/* end DEBUG */
	
	if (!isStarted) 
		startDevice();

	/* wait until buffer was filled */
	while (!isBufferFilled)
		Sleep(10);

	(*env)->ReleaseByteArrayElements(env, arr, body, 0);
    
	/* notify DevicePushSourceStream it can generate another transferData call */
	if (isStarted) {
		cls = (*env)->GetObjectClass(env, obj);
		mid = (*env)->GetMethodID(env, cls, "notify", "()V");
		if (mid == 0) {
			printf("Error indentifying native method\n");
			return;
		}
		(*env)->CallVoidMethod(env, obj, mid);
	}
	
	return waveHeader.dwBytesRecorded;
}

/*
 * Class:     com_ibm_media_protocol_device_DevicePushSourceStream
 * Method:    isBufferFilled
 * Signature: ()Z
 */
/*
 * Class:     com_ibm_media_protocol_device_DevicePushSourceStream
 * Method:    isBufferFilled
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_media_protocol_device_DevicePushSourceStream_isBufferFilled
(JNIEnv* env, jobject obj) {

	return isBufferFilled;
}
