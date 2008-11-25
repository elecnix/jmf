/*
 * @(#)vfwcapture.c	1.20 03/04/25
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <windows.h>
#include <vfw.h>
#include <jni-util.h>
#include "com_sun_media_protocol_vfw_VFWCapture.h"

static jfieldID
    cp_dwRequestMicroSecPerFrame,
    cp_fMakeUserHitOKToCapture,
    cp_wPercentDropForError, 
    cp_fYield, 
    cp_dwIndexSize, 
    cp_wChunkGranularity, 
    cp_fUsingDOSMemory, 
    cp_wNumVideoRequested, 
    cp_fCaptureAudio, 
    cp_wNumAudioRequested, 
    cp_vKeyAbort, 
    cp_fAbortLeftMouse, 
    cp_fAbortRightMouse, 
    cp_fLimitEnabled, 
    cp_wTimeLimit, 
    cp_fMCIControl, 
    cp_fStepMCIDevice, 
    cp_dwMCIStartTime, 
    cp_dwMCIStopTime, 
    cp_fStepCaptureAt2x, 
    cp_wStepCaptureAverageFrames, 
    cp_dwAudioBufferSize, 
    cp_fDisableWriteCache, 
    cp_AVStreamMaster,
    wfe_wFormatTag,
    wfe_nChannels,
    wfe_nSamplesPerSec,
    wfe_nAvgBytesPerSec,
    wfe_nBlockAlign,
    wfe_wBitsPerSample,
    wfe_cbSize,
    cdc_wDeviceIndex,
    cdc_fHasOverlay,
    cdc_fHasDlgVideoSource,
    cdc_fHasDlgVideoFormat,
    cdc_fHasDlgVideoDisplay,
    cdc_fCaptureInitialized,
    cdc_fDriverSuppliesPalettes,
    cs_uiImageWidth,
    cs_uiImageHeight,
    cs_dwCurrentVideoFrame,
    cs_dwCurrentVideoFramesDropped,
    cs_dwCurrentWaveSamples,
    cs_dwCurrentTimeElapsedMS,
    cs_hPalCurrent,
    cs_dwReturn,
    cs_wNumVideoAllocated,
    cs_wNumAudioAllocated,
    cs_fLiveWindow,
    cs_fOverlayWindow,
    cs_fScale,
    cs_fAudioHardware,
    cs_fUsingDefaultPalette,
    cs_fCapFileExists,
    cs_fCapturingNow,
    cs_ptScrollX,
    cs_ptScrollY,
    bmi_biWidth,
    bmi_biHeight,
    bmi_biPlanes,
    bmi_biBitCount,
    bmi_biSizeImage,
    bmi_biXPelsPerMeter,
    bmi_biYPelsPerMeter,
    bmi_biClrUsed,
    bmi_biClrImportant,
    bmi_fourcc;

typedef struct {
    int dataAvailable;
    int timeStamp;
    int keyFrame;
    void *data;
    int capture;
} Transfer;



JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_cacheFieldIDs(JNIEnv *env,
						jclass jvfw)
{
    jclass jCaptureParms = (*env)->FindClass(env, "com/sun/media/protocol/vfw/CaptureParms");
    jclass jBitMapInfo = (*env)->FindClass(env, "com/sun/media/vfw/BitMapInfo");
    jclass jWaveFormatEx = (*env)->FindClass(env, "com/sun/media/vfw/WaveFormatEx");
    jclass jCapStatus = (*env)->FindClass(env, "com/sun/media/protocol/vfw/CapStatus");
    jclass jCaptureDriverCaps = (*env)->FindClass(env, "com/sun/media/protocol/vfw/CapDriverCaps");
    
    if (jCaptureParms) {
	cp_dwRequestMicroSecPerFrame = (*env)->GetFieldID(env, jCaptureParms,
						  "dwRequestMicroSecPerFrame",
						  "I");
	cp_fMakeUserHitOKToCapture   = (*env)->GetFieldID(env, jCaptureParms,
						  "fMakeUserHitOKToCapture",
						  "Z");
	cp_wPercentDropForError      = (*env)->GetFieldID(env, jCaptureParms,
						  "wPercentDropForError",
						  "I");
	cp_fYield                    = (*env)->GetFieldID(env, jCaptureParms,
						  "fYield",
						  "Z");
	cp_dwIndexSize               = (*env)->GetFieldID(env, jCaptureParms,
						  "dwIndexSize",
						  "I");
	cp_wChunkGranularity         = (*env)->GetFieldID(env, jCaptureParms,
						  "wChunkGranularity",
						  "I");
	cp_fUsingDOSMemory           = (*env)->GetFieldID(env, jCaptureParms,
						  "fUsingDOSMemory",
						  "Z");
	cp_wNumVideoRequested        = (*env)->GetFieldID(env, jCaptureParms,
						  "wNumVideoRequested",
						  "I");
	cp_fCaptureAudio = (*env)->GetFieldID(env, jCaptureParms,
				      "fCaptureAudio",
				      "Z");
	cp_wNumAudioRequested = (*env)->GetFieldID(env, jCaptureParms,
					   "wNumAudioRequested",
					   "I");
	cp_vKeyAbort = (*env)->GetFieldID(env, jCaptureParms,
				  "vKeyAbort",
				  "I");
	cp_fAbortLeftMouse = (*env)->GetFieldID(env, jCaptureParms,
					"fAbortLeftMouse",
					"Z");
	cp_fAbortRightMouse = (*env)->GetFieldID(env, jCaptureParms,
					 "fAbortRightMouse",
					 "Z");
	cp_fLimitEnabled = (*env)->GetFieldID(env, jCaptureParms,
				      "fLimitEnabled",
				      "Z");
	cp_wTimeLimit = (*env)->GetFieldID(env, jCaptureParms,
				   "wTimeLimit",
				   "I");
	cp_fMCIControl = (*env)->GetFieldID(env, jCaptureParms,
				    "fMCIControl",
				    "Z");
	cp_fStepMCIDevice = (*env)->GetFieldID(env, jCaptureParms,
				       "fStepMCIDevice",
				       "Z");
	cp_dwMCIStartTime = (*env)->GetFieldID(env, jCaptureParms,
				       "dwMCIStartTime",
				       "I");
	cp_dwMCIStopTime = (*env)->GetFieldID(env, jCaptureParms,
				      "dwMCIStopTime",
				      "I");
	cp_fStepCaptureAt2x = (*env)->GetFieldID(env, jCaptureParms,
					 "fStepCaptureAt2x",
					 "Z");
	cp_wStepCaptureAverageFrames = (*env)->GetFieldID(env, jCaptureParms,
						  "wStepCaptureAverageFrames",
						  "I");
	cp_dwAudioBufferSize = (*env)->GetFieldID(env, jCaptureParms,
					  "dwAudioBufferSize",
					  "I");
	cp_fDisableWriteCache = (*env)->GetFieldID(env, jCaptureParms,
					   "fDisableWriteCache",
					   "Z");
	cp_AVStreamMaster = (*env)->GetFieldID(env, jCaptureParms,
				       "AVStreamMaster",
				       "I");
    }

    if (jBitMapInfo != NULL) {
	bmi_biWidth = (*env)->GetFieldID(env, jBitMapInfo,
				 "biWidth",
				 "I");
	bmi_biHeight = (*env)->GetFieldID(env, jBitMapInfo,
				  "biHeight",
				  "I");
	bmi_biPlanes = (*env)->GetFieldID(env, jBitMapInfo,
				  "biPlanes",
				  "I");
	bmi_biBitCount = (*env)->GetFieldID(env, jBitMapInfo,
				    "biBitCount",
				    "I");
	bmi_biSizeImage = (*env)->GetFieldID(env, jBitMapInfo,
				     "biSizeImage",
				     "I");
	bmi_biXPelsPerMeter = (*env)->GetFieldID(env, jBitMapInfo,
					 "biXPelsPerMeter",
					 "I");
	bmi_biYPelsPerMeter = (*env)->GetFieldID(env, jBitMapInfo,
					 "biYPelsPerMeter",
					 "I");
	bmi_biClrUsed = (*env)->GetFieldID(env, jBitMapInfo,
				   "biClrUsed",
				   "I");
	bmi_biClrImportant = (*env)->GetFieldID(env, jBitMapInfo,
					"biClrImportant",
					"I");
	bmi_fourcc = (*env)->GetFieldID(env, jBitMapInfo,
					"fourcc",
					"Ljava/lang/String;");
    }

    if (jWaveFormatEx != NULL) {

	wfe_wFormatTag = (*env)->GetFieldID(env, jWaveFormatEx,
				    "wFormatTag",
				    "I");
	wfe_nChannels = (*env)->GetFieldID(env, jWaveFormatEx,
				   "nChannels",
				   "I");
	wfe_nSamplesPerSec = (*env)->GetFieldID(env, jWaveFormatEx,
					"nSamplesPerSec",
					"I");
	wfe_nAvgBytesPerSec = (*env)->GetFieldID(env, jWaveFormatEx,
					 "nAvgBytesPerSec",
					 "I");
	wfe_nBlockAlign = (*env)->GetFieldID(env, jWaveFormatEx,
				     "nBlockAlign",
				     "I");
	wfe_wBitsPerSample = (*env)->GetFieldID(env, jWaveFormatEx,
					"wBitsPerSample",
					"I");
	wfe_cbSize = (*env)->GetFieldID(env, jWaveFormatEx,
				"cbSize",
				"I");
    }

    if (jCapStatus != NULL) {
	
	cs_uiImageWidth = (*env)->GetFieldID(env, jCapStatus,
				     "uiImageWidth",
				     "I");
	cs_uiImageHeight = (*env)->GetFieldID(env, jCapStatus,
				      "uiImageHeight",
				      "I");
	cs_dwCurrentVideoFrame = (*env)->GetFieldID(env, jCapStatus,
					    "dwCurrentVideoFrame",
					    "I");
	cs_dwCurrentVideoFramesDropped = (*env)->GetFieldID(env, jCapStatus,
						    "dwCurrentVideoFramesDropped",
						    "I");
	cs_dwCurrentWaveSamples = (*env)->GetFieldID(env, jCapStatus,
					     "dwCurrentWaveSamples",
					     "I");
	cs_dwCurrentTimeElapsedMS = (*env)->GetFieldID(env, jCapStatus,
					       "dwCurrentTimeElapsedMS",
					       "I");
	cs_hPalCurrent = (*env)->GetFieldID(env, jCapStatus,
				    "hPalCurrent",
				    "I");
	cs_dwReturn = (*env)->GetFieldID(env, jCapStatus,
				 "dwReturn",
				 "I");
	cs_wNumVideoAllocated = (*env)->GetFieldID(env, jCapStatus,
					   "wNumVideoAllocated",
					   "I");
	cs_wNumAudioAllocated = (*env)->GetFieldID(env, jCapStatus,
					   "wNumAudioAllocated",
					   "I");
	cs_fLiveWindow = (*env)->GetFieldID(env, jCapStatus,
				    "fLiveWindow",
				    "Z");
	cs_fOverlayWindow = (*env)->GetFieldID(env, jCapStatus,
				       "fOverlayWindow",
				       "Z");
	cs_fScale = (*env)->GetFieldID(env, jCapStatus,
			       "fScale",
			       "Z");
	cs_fAudioHardware = (*env)->GetFieldID(env, jCapStatus,
				       "fAudioHardware",
				       "Z");
	cs_fUsingDefaultPalette = (*env)->GetFieldID(env, jCapStatus,
					     "fUsingDefaultPalette",
					     "Z");
	cs_fCapFileExists = (*env)->GetFieldID(env, jCapStatus,
				       "fCapFileExists",
				       "Z");
	cs_fCapturingNow = (*env)->GetFieldID(env, jCapStatus,
				      "fCapturingNow",
				      "Z");
	cs_ptScrollX = (*env)->GetFieldID(env, jCapStatus,
				  "ptScrollX",
				  "I");
	cs_ptScrollY = (*env)->GetFieldID(env, jCapStatus,
				  "ptScrollY",
				  "I");
    }

    if (jCaptureDriverCaps != NULL) {
	cdc_wDeviceIndex = (*env)->GetFieldID(env, jCaptureDriverCaps,
				      "wDeviceIndex",
				      "I");
	cdc_fHasOverlay = (*env)->GetFieldID(env, jCaptureDriverCaps,
				     "fHasOverlay",
				     "Z");
	cdc_fHasDlgVideoSource = (*env)->GetFieldID(env, jCaptureDriverCaps,
					    "fHasDlgVideoSource",
					    "Z");
	cdc_fHasDlgVideoFormat = (*env)->GetFieldID(env, jCaptureDriverCaps,
					    "fHasDlgVideoFormat",
					    "Z");
	cdc_fHasDlgVideoDisplay = (*env)->GetFieldID(env, jCaptureDriverCaps,
					     "fHasDlgVideoDisplay",
					     "Z");
	cdc_fCaptureInitialized = (*env)->GetFieldID(env, jCaptureDriverCaps,
					     "fCaptureInitialized",
					     "Z");
	cdc_fDriverSuppliesPalettes = (*env)->GetFieldID(env, jCaptureDriverCaps,
						 "fDriverSuppliesPalettes",
						 "Z");
    }
    return 1;
}

static int
getWindowHandle(JNIEnv *env, jobject comp)
{
    jmethodID mid;
    jobject peer, surface;
    int returnVal;
    jclass clComponent = (jclass) (*env)->FindClass(env,
						    "java/awt/Component");
    jclass clPeer = (jclass) (*env)->FindClass(env,
					       "sun/awt/windows/WCanvasPeer");
    jclass clSurface = (jclass) (*env)->FindClass(env,
						  "sun/awt/windows/WDrawingSurfaceInfo");

    if (clComponent == NULL || clPeer == NULL || clSurface == NULL)
	printf("Couldn't find all classes\n");
    
    mid = (*env)->GetMethodID(env, clComponent, "getPeer", 
			      "()Ljava/awt/peer/ComponentPeer;");
    peer = (*env)->CallObjectMethod(env, comp, mid, NULL); 
    mid = (*env)->GetMethodID(env, clPeer, "getDrawingSurfaceInfo",
			      "()Lsun/awt/DrawingSurfaceInfo;");
    surface = (*env)->CallObjectMethod(env, peer, mid, NULL);
    
    /* Lock the drawing surface data */
    mid = (*env)->GetMethodID(env, clSurface, "lock", "()I");
    (*env)->CallIntMethod(env, surface, mid);
    /* Get the HWND */
    mid = (*env)->GetMethodID(env, clSurface, "getHWnd", "()I");
    returnVal =   (int) (*env)->CallIntMethod(env, surface, mid, NULL);
    /* Unlock the drawing surface */
    mid = (*env)->GetMethodID(env, clSurface, "unlock", "()V");
    (*env)->CallVoidMethod(env, surface, mid, NULL);
    return returnVal;
}

JNIEXPORT LRESULT WINAPI JNICALL
MyWindowProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {

    switch( uMsg ) {
	case WM_COMMAND:
	    break;
	    
	case WM_DESTROY:
	    PostQuitMessage( 0 );
	    break;
	default:
	    return( DefWindowProc( hWnd, uMsg, wParam, lParam ));
    }

    return 0;
}

int instance = 0;

JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_createWindow(JNIEnv *env,
					       jclass jvfw,
					       jstring title)
{
    WNDCLASS  wc;
    HWND      hWnd;
    HINSTANCE hInstance;
    char szAppName[100];
    MSG msg;

    instance++;
    sprintf(szAppName, "Capture Monitor %d", instance);

    if (1/*wc.style == 0*/) {
	// Fill in window class structure with parameters that describe
	// the main window.
	wc.style         = CS_NOCLOSE;
	wc.lpfnWndProc   = (WNDPROC)MyWindowProc;
	wc.cbClsExtra    = 0;
	wc.cbWndExtra    = 0;
	wc.hInstance     = GetModuleHandle(NULL);
	hInstance        = wc.hInstance;
	wc.hIcon         = NULL;
	wc.hCursor       = LoadCursor(NULL, IDC_ARROW);
	wc.hbrBackground = (HBRUSH)(COLOR_WINDOW+1);
	wc.lpszMenuName  = NULL;
	wc.lpszClassName = szAppName;
	
	if (!RegisterClass(&wc))
	    return NULL;
    }
    
    hWnd = CreateWindow(szAppName,
			szAppName,
			WS_OVERLAPPEDWINDOW,
			0, 0, 5, 5,
			NULL, NULL, hInstance, NULL);
    if (!hWnd) {
	printf("Couldn't create window\n");
	return NULL;
    }
    
    ShowWindow(hWnd, SW_HIDE);
    UpdateWindow(hWnd);

    return hWnd;
}

JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_peekWindowLoop(JNIEnv *env,
					       jclass jvfw,
					       jint window)
{
    MSG msg;
    if (PeekMessage( &msg, NULL, 0, 0, PM_NOREMOVE) ) {
	if (GetMessage( &msg, NULL, 0, 0 )) {
	    TranslateMessage( &msg );
	    DispatchMessage( &msg );
	} else {
	    /*printf("PeekWindow returning 0\n");*/
	    return 0;
	}
    }

    return 1;
}


/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capCreateCaptureWindow
 * Signature: (Ljava/lang/String;IIIII)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capCreateCaptureWindow(JNIEnv *env,
							 jclass jvfw,
							 jstring title,
							 jint jparent,
							 jint x,
							 jint y,
							 jint width,
							 jint height,
							 jint nID)
{
    char *szTitle = (*env)->GetStringUTFChars(env, title, 0);
    HWND rootWindow = GetDesktopWindow();
    HWND parentWindow;
    HWND hwndc;
    
    if (jparent == NULL) {
	parentWindow = GetDesktopWindow();
    } else {
	parentWindow = (HWND) jparent;
	if (parentWindow == NULL) {
	    parentWindow = GetDesktopWindow();
	}
    }

    hwndc = capCreateCaptureWindow(szTitle,
				   WS_CHILD | WS_VISIBLE,
				   (int)x, (int)y,
				   (int)width, (int)height,
				   parentWindow,
				   (int)nID);
    (*env)->ReleaseStringUTFChars(env, title, szTitle);
    return (jint)hwndc;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capSetWindowPos
 * Signature: (IIIII)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capSetWindowPos(JNIEnv *env,
						  jclass jvfw,
						  jint hWnd,
						  jint x,
						  jint y,
						  jint width,
						  jint height)
{
    SetWindowPos((HWND) hWnd, NULL, (int) x, (int) y,
		 (int) width, (int) height, 0);
    return hWnd;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    showWindow
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_showWindow(JNIEnv *env,
					     jclass jvfw,
					     jint hWnd,
					     jint showCode,
					     jint width, jint height)
{
    RECT rectWindow;
    RECT rectClient;
    switch (showCode) {
    case 0:
	ShowWindow((HWND) hWnd, SW_HIDE);
	break;
    case 1:
	ShowWindow((HWND) hWnd, SW_SHOWMINIMIZED);
	break;
    case 2:
	ShowWindow((HWND) hWnd, SW_SHOWNORMAL);
	if (width > 0 && height > 0) {
	    GetClientRect((HWND) hWnd, &rectClient);
	    GetWindowRect((HWND) hWnd, &rectWindow);
	    width += rectWindow.right - rectWindow.left - rectClient.right;
	    height += rectWindow.bottom - rectWindow.top - rectClient.bottom;
	    SetWindowPos((HWND) hWnd, HWND_TOP,
			 0, 0, width, height, SWP_NOMOVE);
	}
	break;
    }
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capGetDriverDescriptionName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capGetDriverDescriptionName(JNIEnv *env,
							      jclass jvfw,
							      jint driverID)
{
    char name[1024];
    char desc[1024];

    int result = capGetDriverDescription((int)driverID,
					 name, 1024,
					 desc, 1024);
    if (result) {
	jstring jname = (*env)->NewStringUTF(env, name);
	return jname;
    } else
	return (jstring)NULL;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capGetDriverDescriptionDesc
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capGetDriverDescriptionDesc(JNIEnv *env,
							      jclass jvfw,
							      jint driverID)
{
    char name[1024];
    char desc[1024];

    int result = capGetDriverDescription((int)driverID,
					 name, 1024,
					 desc, 1024);
    if (result) {
	jstring jdesc = (*env)->NewStringUTF(env, desc);
	return jdesc;
    } else
	return (jstring)NULL;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capCaptureAbort
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capCaptureAbort(JNIEnv *env,
						  jclass jvfw,
						  jint hWnd)
{
    return (jboolean) capCaptureAbort((HWND)hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capCaptureGetSetup
 * Signature: (ILcom/sun/media/vfw/CaptureParms;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capCaptureGetSetup(JNIEnv *env,
						     jclass jvfw,
						     jint hWnd,
						     jobject jcaps)
{
    CAPTUREPARMS cp;
    int size = sizeof(cp);
    int result = capCaptureGetSetup((HWND)hWnd, &cp, size);
    if (!result)
	return (jboolean) 0;

    (*env)->SetIntField(env, jcaps, cp_dwRequestMicroSecPerFrame,
		cp.dwRequestMicroSecPerFrame);
    (*env)->SetBooleanField(env, jcaps, cp_fMakeUserHitOKToCapture,
		    cp.fMakeUserHitOKToCapture);
    (*env)->SetIntField(env, jcaps, cp_wPercentDropForError,
		cp.wPercentDropForError);
    (*env)->SetBooleanField(env, jcaps, cp_fYield,
		    cp.fYield);
    (*env)->SetIntField(env, jcaps, cp_dwIndexSize,
		cp.dwIndexSize);
    (*env)->SetIntField(env, jcaps, cp_wChunkGranularity,
		cp.wChunkGranularity);
    (*env)->SetBooleanField(env, jcaps, cp_fUsingDOSMemory,
		    cp.fUsingDOSMemory);
    (*env)->SetIntField(env, jcaps, cp_wNumVideoRequested,
		cp.wNumVideoRequested);
    (*env)->SetBooleanField(env, jcaps, cp_fCaptureAudio,
		    cp.fCaptureAudio);
    (*env)->SetIntField(env, jcaps, cp_wNumAudioRequested,
		cp.wNumAudioRequested);
    (*env)->SetIntField(env, jcaps, cp_vKeyAbort,
		cp.vKeyAbort);
    (*env)->SetBooleanField(env, jcaps, cp_fAbortLeftMouse,
		    cp.fAbortLeftMouse);
    (*env)->SetBooleanField(env, jcaps, cp_fAbortRightMouse,
		    cp.fAbortRightMouse);
    (*env)->SetBooleanField(env, jcaps, cp_fLimitEnabled,
		    cp.fLimitEnabled);
    (*env)->SetIntField(env, jcaps, cp_wTimeLimit,
		cp.wTimeLimit);
    (*env)->SetBooleanField(env, jcaps, cp_fMCIControl,
		    cp.fMCIControl);
    (*env)->SetBooleanField(env, jcaps, cp_fStepMCIDevice,
		    cp.fStepMCIDevice);
    (*env)->SetIntField(env, jcaps, cp_dwMCIStartTime,
		cp.dwMCIStartTime);
    (*env)->SetIntField(env, jcaps, cp_dwMCIStopTime,
		cp.dwMCIStopTime);
    (*env)->SetBooleanField(env, jcaps, cp_fStepCaptureAt2x,
		    cp.fStepCaptureAt2x);
    (*env)->SetIntField(env, jcaps, cp_wStepCaptureAverageFrames,
		cp.wStepCaptureAverageFrames);
    (*env)->SetIntField(env, jcaps, cp_dwAudioBufferSize,
		cp.dwAudioBufferSize);
    (*env)->SetBooleanField(env, jcaps, cp_fDisableWriteCache,
		    cp.fDisableWriteCache);
    (*env)->SetIntField(env, jcaps, cp_AVStreamMaster,
		cp.AVStreamMaster);

    return (jboolean) 1 ;    
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capCaptureSetSetup
 * Signature: (ILcom/sun/media/vfw/CaptureParms;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capCaptureSetSetup(JNIEnv *env,
						     jclass jvfw,
						     jint hWnd,
						     jobject jcaps)
{
    CAPTUREPARMS cp;
    cp.dwRequestMicroSecPerFrame = (*env)->GetIntField(env, jcaps,
						       cp_dwRequestMicroSecPerFrame);
    cp.wPercentDropForError = (*env)->GetIntField(env, jcaps,
						  cp_wPercentDropForError);
    cp.dwIndexSize = (*env)->GetIntField(env, jcaps,
					 cp_dwIndexSize);
    cp.wChunkGranularity = (*env)->GetIntField(env, jcaps,
					       cp_wChunkGranularity);
    cp.wNumVideoRequested = (*env)->GetIntField(env, jcaps,
						cp_wNumVideoRequested);
    cp.wNumAudioRequested = (*env)->GetIntField(env, jcaps,
						cp_wNumAudioRequested);
    cp.wTimeLimit = (*env)->GetIntField(env, jcaps,
					cp_wTimeLimit);
    cp.dwMCIStartTime = (*env)->GetIntField(env, jcaps,
					    cp_dwMCIStartTime);
    cp.dwMCIStopTime = (*env)->GetIntField(env, jcaps,
					   cp_dwMCIStopTime);
    cp.wStepCaptureAverageFrames = (*env)->GetIntField(env, jcaps,
						       cp_wStepCaptureAverageFrames);
    cp.dwAudioBufferSize = (*env)->GetIntField(env, jcaps,
					       cp_dwAudioBufferSize);
    cp.AVStreamMaster = (*env)->GetIntField(env, jcaps,
					    cp_AVStreamMaster);
    cp.fMakeUserHitOKToCapture = (BOOL) (*env)->GetBooleanField(env, jcaps,
								cp_fMakeUserHitOKToCapture);
    cp.fYield = (BOOL) (*env)->GetBooleanField(env, jcaps,
					       cp_fYield);
    cp.fUsingDOSMemory = (BOOL) (*env)->GetBooleanField(env, jcaps,
							cp_fUsingDOSMemory);
    cp.fCaptureAudio = (BOOL) (*env)->GetBooleanField(env, jcaps,
						      cp_fCaptureAudio);
    cp.fAbortLeftMouse = (BOOL) (*env)->GetBooleanField(env, jcaps,
							cp_fAbortLeftMouse);
    cp.fAbortRightMouse = (BOOL) (*env)->GetBooleanField(env, jcaps,
							 cp_fAbortRightMouse);
    cp.fLimitEnabled = (BOOL) (*env)->GetBooleanField(env, jcaps,
						      cp_fLimitEnabled);
    cp.fMCIControl = (BOOL) (*env)->GetBooleanField(env, jcaps,
						    cp_fMCIControl);
    cp.fStepMCIDevice = (BOOL) (*env)->GetBooleanField(env, jcaps,
						       cp_fStepMCIDevice);
    cp.fStepCaptureAt2x = (BOOL) (*env)->GetBooleanField(env, jcaps,
							 cp_fStepCaptureAt2x);
    cp.fDisableWriteCache = (BOOL) (*env)->GetBooleanField(env, jcaps,
							   cp_fDisableWriteCache);

    return capCaptureSetSetup((HWND) hWnd, &cp, sizeof(cp));
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capCaptureSequence
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capCaptureSequence(JNIEnv *env,
						     jclass jvfw,
						     jint hWnd)
{
    return (jboolean) capCaptureSequence((HWND) hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capCaptureSequenceNoFile
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capCaptureSequenceNoFile(JNIEnv *env,
							   jclass jvfw,
							   jint hWnd)
{
    return (jboolean) capCaptureSequenceNoFile((HWND)hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capCaptureStop
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capCaptureStop(JNIEnv *env,
						 jclass jvfw,
						 jint hWnd)
{
    return (jboolean) capCaptureStop((HWND) hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDlgVideoCompression
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDlgVideoCompression(JNIEnv *env,
							 jclass jvfw,
							 jint hWnd)
{
    return (jboolean) capDlgVideoCompression((HWND) hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDlgVideoDisplay
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDlgVideoDisplay(JNIEnv *env,
						     jclass jvfw,
						     jint hWnd)
{
    return (jboolean) capDlgVideoDisplay((HWND) hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDlgVideoFormat
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDlgVideoFormat(JNIEnv *env,
						    jclass jvfw,
						    jint hWnd)
{
    return (jboolean) capDlgVideoFormat((HWND) hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDlgVideoSource
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDlgVideoSource(JNIEnv *env,
						    jclass jvfw,
						    jint hWnd)
{
    return (jboolean) capDlgVideoSource((HWND) hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDriverConnect
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDriverConnect(JNIEnv *env,
						   jclass jvfw,
						   jint hWnd,
						   jint driverID)
{
    return (jboolean) capDriverConnect((HWND) hWnd,
				       (int) driverID);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDriverDisconnect
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDriverDisconnect(JNIEnv *env,
						      jclass jvfw,
						      jint hWnd)
{
    return (jboolean) capDriverDisconnect((HWND) hWnd);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDriverGetCaps
 * Signature: (ILcom/sun/media/vfw/CapDriverCaps;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDriverGetCaps(JNIEnv *env,
						   jclass jvfw,
						   jint hWnd,
						   jobject jcaps)
{
    CAPDRIVERCAPS caps;
    int result = capDriverGetCaps((HWND) hWnd,
				  &caps,
				  sizeof(caps));
    if (!result)
	return (jboolean) 0;

    (*env)->SetIntField(env, jcaps, cdc_wDeviceIndex, caps.wDeviceIndex);
    (*env)->SetBooleanField(env, jcaps, cdc_fHasOverlay, caps.fHasOverlay);
    (*env)->SetBooleanField(env, jcaps, cdc_fHasDlgVideoSource, caps.fHasDlgVideoSource);
    (*env)->SetBooleanField(env, jcaps, cdc_fHasDlgVideoFormat, caps.fHasDlgVideoFormat);
    (*env)->SetBooleanField(env, jcaps, cdc_fHasDlgVideoDisplay,
		    caps.fHasDlgVideoDisplay);
    (*env)->SetBooleanField(env, jcaps, cdc_fCaptureInitialized,
		    caps.fCaptureInitialized);
    (*env)->SetBooleanField(env, jcaps, cdc_fDriverSuppliesPalettes,
		    caps.fDriverSuppliesPalettes);
    return (jboolean) 1;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDriverGetName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDriverGetName(JNIEnv *env,
						   jclass jvfw,
						   jint hWnd)
{
    char name[1024];

    int result = capDriverGetName((HWND) hWnd,
				  name, 1024);
    if (result) {
	jstring jname = (*env)->NewStringUTF(env, name);
	return jname;
    } else
	return (jstring)NULL;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capDriverGetVersion
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capDriverGetVersion(JNIEnv *env,
						      jclass jvfw,
						      jint hWnd)
{
    return NULL;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capGetStatus
 * Signature: (ILcom/sun/media/vfw/CapStatus;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capGetStatus(JNIEnv *env,
					       jclass jvfw,
					       jint hWnd,
					       jobject jcaps)
{
    CAPSTATUS cs;
    int result = capGetStatus((HWND) hWnd,
			      &cs, sizeof(cs));
    if (!result)
	return (jboolean) 0;

    (*env)->SetIntField(env, jcaps, cs_uiImageWidth,
		cs.uiImageWidth);
    (*env)->SetIntField(env, jcaps, cs_uiImageHeight,
		cs.uiImageHeight);
    (*env)->SetIntField(env, jcaps, cs_dwCurrentVideoFrame,
		cs.dwCurrentVideoFrame);
    (*env)->SetIntField(env, jcaps, cs_dwCurrentVideoFramesDropped,
		cs.dwCurrentVideoFramesDropped);
    (*env)->SetIntField(env, jcaps, cs_dwCurrentWaveSamples,
		cs.dwCurrentWaveSamples);
    (*env)->SetIntField(env, jcaps, cs_dwCurrentTimeElapsedMS,
		cs.dwCurrentTimeElapsedMS);
    (*env)->SetIntField(env, jcaps, cs_hPalCurrent,
		cs.hPalCurrent);
    (*env)->SetIntField(env, jcaps, cs_dwReturn,
		cs.dwReturn);
    (*env)->SetIntField(env, jcaps, cs_wNumVideoAllocated,
		cs.wNumVideoAllocated);
    (*env)->SetIntField(env, jcaps, cs_wNumAudioAllocated,
		cs.wNumAudioAllocated);
    (*env)->SetBooleanField(env, jcaps, cs_fLiveWindow,
		    cs.fLiveWindow);
    (*env)->SetBooleanField(env, jcaps, cs_fOverlayWindow,
		    cs.fOverlayWindow);
    (*env)->SetBooleanField(env, jcaps, cs_fScale,
		    cs.fScale);
    (*env)->SetBooleanField(env, jcaps, cs_fUsingDefaultPalette,
		    cs.fUsingDefaultPalette);
    (*env)->SetBooleanField(env, jcaps, cs_fAudioHardware,
		    cs.fAudioHardware);
    (*env)->SetBooleanField(env, jcaps, cs_fCapFileExists,
		    cs.fCapFileExists);
    (*env)->SetBooleanField(env, jcaps, cs_fCapturingNow,
		    cs.fCapturingNow);
    (*env)->SetIntField(env, jcaps, cs_ptScrollX,
		cs.ptScroll.x);
    (*env)->SetIntField(env, jcaps, cs_ptScrollY,
		cs.ptScroll.y);

    return (jboolean) 1;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capPreview
 * Signature: (IZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capPreview(JNIEnv *env,
					     jclass jvfw,
					     jint hWnd,
					     jboolean preview)
{
    return capPreview((HWND) hWnd,
		      (preview)? TRUE:FALSE);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capOverlay
 * Signature: (IZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capOverlay(JNIEnv *env,
					     jclass jvfw,
					     jint hWnd,
					     jboolean overlay)
{
    return capOverlay((HWND) hWnd,
		      (overlay)? TRUE:FALSE);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capPreviewRate
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capPreviewRate(JNIEnv *env,
						 jclass jvfw,
						 jint hWnd,
						 jint rate)
{
    return capPreviewRate((HWND) hWnd,
			  (int) rate);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capPreviewScale
 * Signature: (IZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capPreviewScale(JNIEnv *env,
						  jclass jvfw,
						  jint hWnd,
						  jboolean scale)
{
    return capPreviewScale((HWND) hWnd,
			   (BOOL) scale);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capGetAudioFormat
 * Signature: (ILcom/sun/media/vfw/WaveFormatEx;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capGetAudioFormat(JNIEnv *env,
						    jclass jvfw,
						    jint hWnd,
						    jobject jwave)
{
    int size;
    WAVEFORMATEX *wfe;
    size = capGetAudioFormatSize((HWND) hWnd);
    wfe = (WAVEFORMATEX *) malloc(size);
    capGetAudioFormat((HWND) hWnd,
		      wfe,
		      size);
    (*env)->SetIntField(env, jwave, wfe_wFormatTag,
		wfe->wFormatTag);
    (*env)->SetIntField(env, jwave, wfe_nChannels,
		wfe->nChannels);
    (*env)->SetIntField(env, jwave, wfe_nSamplesPerSec,
		wfe->nSamplesPerSec);
    (*env)->SetIntField(env, jwave, wfe_nAvgBytesPerSec,
		wfe->nAvgBytesPerSec);
    (*env)->SetIntField(env, jwave, wfe_nBlockAlign,
		wfe->nBlockAlign);
    (*env)->SetIntField(env, jwave, wfe_wBitsPerSample,
		wfe->wBitsPerSample);
    (*env)->SetIntField(env, jwave, wfe_cbSize,
		wfe->cbSize);
    free(wfe);
    return (jboolean) 1;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capSetAudioFormat
 * Signature: (ILcom/sun/media/vfw/WaveFormatEx;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capSetAudioFormat(JNIEnv *env,
						    jclass jvfw,
						    jint hWnd,
						    jobject jwfe)
{
    WAVEFORMATEX wfe;

    wfe.wFormatTag = (*env)->GetIntField(env, jwfe, wfe_wFormatTag);
    wfe.nChannels  = (*env)->GetIntField(env, jwfe, wfe_nChannels);
    wfe.nSamplesPerSec = (*env)->GetIntField(env, jwfe, wfe_nSamplesPerSec);
    wfe.nAvgBytesPerSec = (*env)->GetIntField(env, jwfe, wfe_nAvgBytesPerSec);
    wfe.nBlockAlign = (*env)->GetIntField(env, jwfe, wfe_nBlockAlign);
    wfe.wBitsPerSample = (*env)->GetIntField(env, jwfe, wfe_wBitsPerSample);
    wfe.cbSize = (*env)->GetIntField(env, jwfe, wfe_cbSize);

    return capSetAudioFormat((HWND) hWnd, &wfe, sizeof(wfe));
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capGetVideoFormat
 * Signature: (ILcom/sun/media/vfw/BitMapInfo;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capGetVideoFormat(JNIEnv *env,
						    jclass jvfw,
						    jint hWnd,
						    jobject jbmi)
{
    BITMAPINFOHEADER *bmi;
    int size = capGetVideoFormatSize((HWND) hWnd);
    int i;
    char fourcc[5];
    jstring js;
    bmi = (BITMAPINFOHEADER*) malloc(size);
    capGetVideoFormat((HWND) hWnd,
		      bmi,
		      size);
    (*env)->SetIntField(env, jbmi, bmi_biWidth, bmi->biWidth);
    (*env)->SetIntField(env, jbmi, bmi_biHeight, bmi->biHeight);
    (*env)->SetIntField(env, jbmi, bmi_biPlanes, bmi->biPlanes);
    (*env)->SetIntField(env, jbmi, bmi_biBitCount, bmi->biBitCount);
    (*env)->SetIntField(env, jbmi, bmi_biSizeImage, bmi->biSizeImage);
    (*env)->SetIntField(env, jbmi, bmi_biXPelsPerMeter, bmi->biXPelsPerMeter);
    (*env)->SetIntField(env, jbmi, bmi_biYPelsPerMeter, bmi->biYPelsPerMeter);
    (*env)->SetIntField(env, jbmi, bmi_biClrUsed, bmi->biClrUsed);
    (*env)->SetIntField(env, jbmi, bmi_biClrImportant, bmi->biClrImportant);
    if (bmi->biCompression == BI_RGB)
	strcpy(fourcc, "RGB");
    else if (bmi->biCompression == BI_RLE8)
	strcpy(fourcc, "RLE8");
    else if (bmi->biCompression == BI_RLE4)
	strcpy(fourcc, "RLE4");
    else if (bmi->biCompression == BI_BITFIELDS)
	strcpy(fourcc, "RGB");
    else
	*(int*)fourcc = bmi->biCompression;
    fourcc[4] = 0;

    /*printf("size = %d, biSizeImage = %d, fourcc = %s\n", size, bmi->biSizeImage, fourcc);*/

    if (size > sizeof(BITMAPINFOHEADER)) {
	int diff = size - sizeof(BITMAPINFOHEADER);
	char * extraData;
	jbyteArray jExtraBytes = (*env)->NewByteArray(env, diff);
	SetObjectField(env, jbmi, "extraBytes", "[B", jExtraBytes);
	SetIntField(env, jbmi, "extraSize", diff);
	extraData = (char *) (*env)->GetByteArrayElements(env, jExtraBytes, 0);
	for (i = 0; i < diff; i++) {
	    extraData[i] = ((char*)bmi)[i + sizeof(BITMAPINFOHEADER)];
	}
	(*env)->ReleaseByteArrayElements(env, jExtraBytes, extraData, 0);
    }
    /*
    for (i = 0; i < size; i++) {
	printf("%d = %d\n", i, ((unsigned char *)bmi)[i]);
    }
    */
    js = (*env)->NewStringUTF(env, fourcc);
    (*env)->SetObjectField(env, jbmi, bmi_fourcc, (jobject) js);
    free(bmi);
    return (jboolean) 1;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    capSetVideoFormat
 * Signature: (ILcom/sun/media/vfw/BitMapInfo;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_capSetVideoFormat(JNIEnv *env,
						    jclass jvfw,
						    jint hWnd,
						    jobject jbmi)
{
    BITMAPINFOHEADER bmi;
    char *fourcc;
    jstring jfourcc = (*env)->GetObjectField(env, jbmi, bmi_fourcc);
    fourcc = (*env)->GetStringUTFChars(env, jfourcc, 0);

    if (strcmp(fourcc, "RGB") == 0)
	bmi.biCompression = BI_RGB;
    else
	bmi.biCompression = *(int *)fourcc;

    bmi.biSize = sizeof(BITMAPINFOHEADER);
    bmi.biWidth = (*env)->GetIntField(env, jbmi, bmi_biWidth);
    bmi.biHeight = (*env)->GetIntField(env, jbmi, bmi_biHeight);
    bmi.biPlanes = (*env)->GetIntField(env, jbmi, bmi_biPlanes);
    bmi.biBitCount = (*env)->GetIntField(env, jbmi, bmi_biBitCount);
    bmi.biSizeImage = (*env)->GetIntField(env, jbmi, bmi_biSizeImage);
    bmi.biXPelsPerMeter = (*env)->GetIntField(env, jbmi, bmi_biXPelsPerMeter);
    bmi.biYPelsPerMeter = (*env)->GetIntField(env, jbmi, bmi_biYPelsPerMeter);
    bmi.biClrUsed = (*env)->GetIntField(env, jbmi, bmi_biClrUsed);
    bmi.biClrImportant = (*env)->GetIntField(env, jbmi, bmi_biClrImportant);

    (*env)->ReleaseStringUTFChars(env, jfourcc, fourcc);
    /* TODO: set the extra bytes */
    
    return capSetVideoFormat((HWND) hWnd, &bmi, sizeof(bmi));
}

/****************************************************************
 * Capture callback
 ****************************************************************/

LRESULT JNIEXPORT CALLBACK videoStreamCallback(HWND handle, LPVIDEOHDR videoHeader)
{
    /* SetWindowLong(handle, GWL_USERDATA, longData); */
    int time;
    Transfer *userData = (Transfer *) GetWindowLong(handle, GWL_USERDATA);
    if (userData) {
	if (userData->capture == 0)
	    return (LRESULT) TRUE;
	/* Signal availability of data. */
	userData->data = (void *) videoHeader->lpData;
	userData->timeStamp = videoHeader->dwTimeCaptured;
	userData->keyFrame = videoHeader->dwFlags & VHDR_KEYFRAME;
	userData->dataAvailable = videoHeader->dwBytesUsed;
	/* Wait until java land reads the data and sets dataAvailable to zero */
	time = 0;
	while (userData->capture && userData->dataAvailable && time < 2000) {
	    Sleep(10);
	    time += 10;
	}
	userData->dataAvailable = 0;
    }
    return (LRESULT) TRUE;
}


LRESULT JNIEXPORT CALLBACK errorCallback(HWND handle, int errID, LPSTR errText)
{
    if (errID != 0)
	printf("Error is %s\n", errText);
    return TRUE;
}

LRESULT JNIEXPORT CALLBACK statusCallback(HWND handle, int statusID, LPCSTR message)
{
    //printf("Status callback %d = %s\n", statusID, message);
    return TRUE;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    createFrameCallback
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_createFrameCallback(JNIEnv *env,
						     jclass jvfw,
						     jint jhandle)
{
    Transfer *cbHandle = (Transfer*) malloc(sizeof(Transfer));
    int result;
    
    cbHandle->data = NULL;
    cbHandle->dataAvailable = 0;
    cbHandle->capture = 0;
    
    SetWindowLong((HWND)jhandle, GWL_USERDATA, (long) cbHandle);
    return (jint) cbHandle;
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    startFrameCallback
 * Signature: (II)
 */
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_startFrameCallback(JNIEnv *env,
						     jclass jvfw,
						     jint jhandle,
						     jint cbHandle)
{
    int result;
    
    if (cbHandle) {
	Transfer *transfer = (Transfer *) cbHandle;
	transfer->capture = 1;
    }
    
    result = capSetCallbackOnVideoStream((HWND) jhandle, videoStreamCallback);
    capSetCallbackOnError((HWND) jhandle, errorCallback);
    capSetCallbackOnStatus((HWND) jhandle, statusCallback);
    /*printf("result of capSetCallbackOnVideoStream = %d\n", result);*/
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    stopFrameCallback
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_stopFrameCallback(JNIEnv *env,
						    jclass jvfw,
						    jint jhandle,
						    jint cbHandle)
{
    if (cbHandle) {
	Transfer *transfer = (Transfer *) cbHandle;
	transfer->capture = 0;
    }
    capSetCallbackOnVideoStream((HWND) jhandle, NULL);
    capSetCallbackOnError((HWND) jhandle, NULL);
    capSetCallbackOnStatus((HWND) jhandle, NULL);
}

/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    destroyFrameCallback
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_destroyFrameCallback(JNIEnv *env,
						    jclass jvfw,
						    jint jhandle,
						    jint cbHandle)
{
    if (cbHandle != 0) {
	free((Transfer*)cbHandle);
    }
}


/*
 * Class:     com_sun_media_protocol_vfw_VFWCapture
 * Method:    getAvailableData
 * Signature: (II[B)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_getAvailableData(JNIEnv *env,
						   jclass jvfw,
						   jint jhandle,
						   jint cbHandle,
						   jobject jdata,
						   jlong dataBytes,
						   jint dataLength,
						   jlongArray jtimestamp)
{
    Transfer *transfer;
    void *data = (void *) dataBytes;
    int size;
    jlong *resultTimeStamp;
    
    if (cbHandle == 0)
	return 0;

    transfer = (Transfer*) cbHandle;

    if (transfer->dataAvailable != 0) {
	if (dataLength < transfer->dataAvailable) 
	    return -(transfer->dataAvailable);
	/* fall through and copy data */
    } else
	return 0;

    size = transfer->dataAvailable;

    if (dataBytes == 0)
	data = (void *) (*env)->GetByteArrayElements(env, (jbyteArray) jdata, 0);

    memcpy(data, transfer->data, size);
    transfer->dataAvailable = 0;

    /* Copy the time stamp into the result array */
    resultTimeStamp = (jlong *) (*env)->GetLongArrayElements(env, jtimestamp, 0);
    *resultTimeStamp = transfer->timeStamp;
    (*env)->ReleaseLongArrayElements(env, jtimestamp, resultTimeStamp, 0);

    if (dataBytes == 0)
	(*env)->ReleaseByteArrayElements(env, (jbyteArray) jdata, data, 0);

    return size;
}
    
JNIEXPORT void JNICALL
Java_com_sun_media_protocol_vfw_VFWCapture_destroyWindow(JNIEnv *env,
						jclass jvfw,
						jint jhandle)
{
    DestroyWindow((HWND) jhandle);
}
