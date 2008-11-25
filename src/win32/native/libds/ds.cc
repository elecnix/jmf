/*
 * @(#)ds.cc	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>

#include <windows.h>
#include <commdlg.h>
#include <streams.h>
#include "com_ibm_media_protocol_ds_DataSource.h"
#include "com_ibm_media_protocol_ds_DSSourceStream.h"
#include "filter2stream.h"
#include "filter2streamuids.h"


IBaseFilter *pVCap;
IBaseFilter *pFilter2Stream;
IGraphBuilder *pFg;
IJMFStream *pJMFStream;
AM_MEDIA_TYPE *pMediaType;

void FreeFilters() {
  if (pFg) 
    pFg->Release();
  pFg = NULL;
  if (pVCap) 
    pVCap->Release();
  pVCap = NULL;
  if (pFilter2Stream) 
    pFilter2Stream->Release();
  pFilter2Stream = NULL;
  if (pJMFStream) 
    pJMFStream->Release();
  pJMFStream = NULL;
}

// Make a graph object we can use for capture graph building
//
BOOL MakeGraph(IGraphBuilder **ppFg)
{
    HRESULT hr = CoCreateInstance(CLSID_FilterGraph, NULL, CLSCTX_INPROC, 
				  IID_IGraphBuilder, (void**)ppFg);
    return (hr == NOERROR) ? TRUE : FALSE;
}

BOOL InitCapFilter(int deviceNo) {

    HRESULT hr;
    BOOL rc;
    ICreateDevEnum *pCreateDevEnum;
    IEnumMoniker *pEm;
    ULONG cFetched;
    IMoniker *pM;
    UINT uIndex = 0;
    IAMVfwCaptureDialogs *pVFWCaptureDialogs;

    hr = CoCreateInstance(CLSID_SystemDeviceEnum, NULL, CLSCTX_INPROC, 
			  IID_ICreateDevEnum, (void**)&pCreateDevEnum);
    if (hr != NOERROR) {
	printf("Error %x: Can't enumerate video caputre devices\n", hr);
	FreeFilters();
	return FALSE;
    }
	    
    hr = pCreateDevEnum->CreateClassEnumerator(CLSID_VideoInputDeviceCategory, &pEm, 0);
    pCreateDevEnum->Release();
    if (hr != NOERROR) {
	printf("Error %x: Can't enumerate video caputre devices\n", hr);
	FreeFilters();
	return FALSE;
    }
		
    pEm->Reset();

    while (hr = pEm->Next(1, &pM, &cFetched), hr == S_OK) {
      printf("Found device no. %d\n", uIndex);
      if ((int)uIndex == deviceNo) {
	pVCap = NULL;
	hr = pM->BindToObject(0, 0, IID_IBaseFilter, (void**)&pVCap);
	pM->Release();
	break;
      }
      pM->Release();
      uIndex++;
    }
    
    pEm->Release();
    if (pVCap == NULL) {
	printf("Error %x: Cannot create video capture filter for device %d\n", hr, deviceNo);
	FreeFilters();
	return FALSE;
    }

    pFg = NULL;
    rc = MakeGraph(&pFg);
    if (rc == FALSE) {
	printf("Cannot instantiate filtergraph");
	FreeFilters();
	return FALSE;
    }

    hr = pFg->AddFilter(pVCap, NULL);
    if (hr != NOERROR) {
	printf("Error %x: Cannot add vidcap to filtergraph", hr);
	FreeFilters();
	return FALSE;
    }

    hr = pVCap->QueryInterface(IID_IAMVfwCaptureDialogs, (void**)&pVFWCaptureDialogs);
    if (hr != NOERROR) {
	printf("Error %x: Cannot find IAMVFWCapture Interface", hr);
	FreeFilters();
	return FALSE;
    }
	
    hr = pVFWCaptureDialogs->HasDialog(VfwCaptureDialog_Source);
    if (hr == S_OK)
      pVFWCaptureDialogs->ShowDialog(VfwCaptureDialog_Source, NULL);
    else 
      printf("No source dialog\n");

    hr = pVFWCaptureDialogs->HasDialog(VfwCaptureDialog_Format);
    if (hr == S_OK)
	pVFWCaptureDialogs->ShowDialog(VfwCaptureDialog_Format, NULL);
    else
      printf("No format dialog\n");

    pVFWCaptureDialogs->Release();
	    
    return TRUE;
}

BOOL InitFilter2Stream() {

    HRESULT hr;

    // create the actual filter
    hr = CoCreateInstance((REFCLSID)CLSID_Filter2Stream, NULL, CLSCTX_INPROC, 
			  (REFIID)IID_IBaseFilter, (void**)&pFilter2Stream);
		
    if (hr != NOERROR) {
	printf("Error %x: creating Filter2Stream\n", hr);
	FreeFilters();
	return FALSE;
    }
	
    // add it to the filter graph
    hr = pFg->AddFilter(pFilter2Stream, NULL);
    if (hr != NOERROR) {
	printf("Error %x: Cannot add Filter2Stream to filtergraph", hr);
	FreeFilters();
	return FALSE;
    }

    hr = pFilter2Stream->QueryInterface(IID_IJMFStream, (void**)&pJMFStream);
    if (hr != NOERROR) {
	printf("Error %x: Cannot find IJMFStream", hr);
	FreeFilters();
	return FALSE;
    }

    return TRUE;
}

AM_MEDIA_TYPE* GetFormat(IPin *pCapPin) {

    // find out what is the output format of the capture pin
    IAMStreamConfig *pVSC;
    AM_MEDIA_TYPE *pmt = NULL;

    HRESULT hr = pCapPin->QueryInterface(IID_IAMStreamConfig, (void**)&pVSC);
    if (hr != NOERROR) {
	printf("Error %x: Cannot find AMStreamConfig", hr);
	FreeFilters();
	return NULL;
    }

    // default capture format
    if (pVSC) {
	pVSC->GetFormat(&pmt); 
    	pVSC->Release();
    }
		
    return pmt;
}

BOOL ConnectGraph() {
    ULONG fetched;
    IPin *pCapPin;
    IPin *pInputPin;
    IEnumPins *pEnumPins;
    PIN_INFO pinInfo;
    HRESULT hr;

    // find the capture pin
    hr = pVCap->EnumPins(&pEnumPins);
    if (hr != NOERROR) {
	printf("Cannot enum pins");
	FreeFilters();
	return FALSE;
    }

    while(pEnumPins->Next((ULONG)1, &pCapPin, &fetched) == NOERROR) {
	pCapPin->QueryPinInfo(&pinInfo);
	if (wcscmp(pinInfo.achName, L"~Capture") == 0) {
	    if (pinInfo.pFilter)
		pinInfo.pFilter->Release();
	    break;
	}
	pCapPin->Release();
	if (pinInfo.pFilter)
	    pinInfo.pFilter->Release();
	
    }

    pEnumPins->Release();
    pEnumPins = NULL;

    // find the input pin
    hr = ((IBaseFilter*)pFilter2Stream)->EnumPins(&pEnumPins);
    if (hr != NOERROR) {
	printf("Cannot enum pins");
	FreeFilters();
	return FALSE;
    }

    while(pEnumPins->Next((ULONG)1, &pInputPin, &fetched) == NOERROR) {
	pInputPin->QueryPinInfo(&pinInfo);
	if (wcscmp(pinInfo.achName, L"Input") == 0) {
	    if (pinInfo.pFilter)
		pinInfo.pFilter->Release();
		
	    break;
	}
	pInputPin->Release();
	if (pinInfo.pFilter)
	    pinInfo.pFilter->Release();
    }

    pEnumPins->Release();
			
    // try to connect them
    hr = pFg->ConnectDirect(pCapPin, pInputPin, NULL);
    if (hr != NOERROR) {
	printf("Cannot connect pins");
	pCapPin->Release();
	pInputPin->Release();
	FreeFilters();
	return FALSE;
    }

    pMediaType = GetFormat(pCapPin); // we generate the capture format before we release the capture pin
	
    pCapPin->Release();
    pInputPin->Release();
    
    return TRUE;
}

/*
 * Class:     com_ibm_media_protocol_ds_DSSourceStream
 * Method:    getFrameRate
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_ibm_media_protocol_ds_DSSourceStream_getFrameRate(JNIEnv *env, jobject obj) {

  REFERENCE_TIME avgTimePerFrame = ((VIDEOINFOHEADER*)pMediaType->pbFormat)->AvgTimePerFrame;
  return (float)(10000000L)/avgTimePerFrame;
}

/*
 * Class:     com_ibm_media_protocol_ds_DSSourceStream
 * Method:    getVideoFormat
 * Signature: (Lcom/sun/media/vfw/BitMapInfo;)V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_protocol_ds_DSSourceStream_getVideoFormat(JNIEnv *env, jobject obj, jobject jbmi) {

    BITMAPINFOHEADER *bmi = HEADER(pMediaType->pbFormat);
    int size = pMediaType->cbFormat;
    char fourcc[5];
    
    jclass jBitMapInfo = env->FindClass("com/sun/media/vfw/BitMapInfo");
    jfieldID bmi_biWidth = env->GetFieldID(jBitMapInfo, "biWidth", "I");
    jfieldID bmi_biHeight = env->GetFieldID(jBitMapInfo, "biHeight", "I");
    jfieldID bmi_biPlanes = env->GetFieldID(jBitMapInfo, "biPlanes", "I");
    jfieldID bmi_biBitCount = env->GetFieldID(jBitMapInfo, "biBitCount", "I");
    jfieldID bmi_biSizeImage = env->GetFieldID(jBitMapInfo, "biSizeImage", 
					       "I");
    jfieldID bmi_biXPelsPerMeter = env->GetFieldID(jBitMapInfo, 
						   "biXPelsPerMeter", "I");
    jfieldID bmi_biYPelsPerMeter = env->GetFieldID(jBitMapInfo, 
						   "biYPelsPerMeter", "I");
    jfieldID bmi_biClrUsed = env->GetFieldID(jBitMapInfo, "biClrUsed", "I");
    jfieldID bmi_biClrImportant = env->GetFieldID(jBitMapInfo, 
						  "biClrImportant", "I");
    jfieldID bmi_fourcc = env->GetFieldID(jBitMapInfo, "fourcc",
					  "Ljava/lang/String;");
    
    env->SetIntField(jbmi, bmi_biWidth, bmi->biWidth);
    env->SetIntField(jbmi, bmi_biHeight, bmi->biHeight);
    env->SetIntField(jbmi, bmi_biPlanes, bmi->biPlanes);
    env->SetIntField(jbmi, bmi_biBitCount, bmi->biBitCount);
    env->SetIntField(jbmi, bmi_biSizeImage, bmi->biSizeImage);
    env->SetIntField(jbmi, bmi_biXPelsPerMeter, bmi->biXPelsPerMeter);
    env->SetIntField(jbmi, bmi_biYPelsPerMeter, bmi->biYPelsPerMeter);
    env->SetIntField(jbmi, bmi_biClrUsed, bmi->biClrUsed);
    env->SetIntField(jbmi, bmi_biClrImportant, bmi->biClrImportant);
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
      signed char* extraData;
      jbyteArray jExtraBytes = env->NewByteArray(diff);
      jclass jbmiClass = env->GetObjectClass(jbmi); 
      jfieldID extraBytesField = env->GetFieldID(jbmiClass, "extraBytes", 
						 "[B");
      env->SetObjectField(jbmi, extraBytesField, jExtraBytes);
      jfieldID extraSizeField = env->GetFieldID(jbmiClass, "extraSize", "I");
      env->SetIntField(jbmi, extraSizeField, diff);
      extraData = (signed char*)env->GetByteArrayElements(jExtraBytes, 0);
      for (int i = 0; i < diff; i++) {
	extraData[i] = ((signed char*)bmi)[i + sizeof(BITMAPINFOHEADER)];
      }
      env->ReleaseByteArrayElements(jExtraBytes, extraData, 0);
    }
    /*
      for (i = 0; i < size; i++) {
      printf("%d = %d\n", i, ((unsigned char *)bmi)[i]);
      }
    */
    jstring js = env->NewStringUTF(fourcc);
    env->SetObjectField(jbmi, bmi_fourcc, (jobject) js);
}

/*
 * Class:     com_ibm_media_protocol_ds_DSSourceStream
 * Method:    setBuffer
 * Signature: ([BII)V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_protocol_ds_DSSourceStream_setBuffer
(JNIEnv *env, jobject obj, jbyteArray buffer, jint offset) {

    jsize len = env->GetArrayLength(buffer);
    jbyte *array = env->GetByteArrayElements(buffer, NULL);
    pJMFStream->setBuffer(array, len);
}

/*
 * Class:     com_ibm_media_protocol_ds_DSSourceStream
 * Method:    isFilled
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_media_protocol_ds_DSSourceStream_isFilled
(JNIEnv *env, jobject obj) {
	
    return pJMFStream->isCopied();
}


/*
 * Class:     com_ibm_media_protocol_ds_DataSource
 * Method:    startDSGraph
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_media_protocol_ds_DataSource_startDSGraph
(JNIEnv *env, jobject obj) {

    // start the engine
    IMediaControl *pMC = NULL;
    HRESULT hr = pFg->QueryInterface(IID_IMediaControl, (void **)&pMC);
    if (hr != NOERROR) {
	printf("Error %x: Cannot get IMediaControl", hr);
	if (pMC)
	    pMC->Release();
	FreeFilters();
	return FALSE;
    }

    pJMFStream->setStopped(FALSE);

    hr = pMC->Run();
    if (hr != NOERROR) {
	printf("Error %x: Cannot Run DirectShow Filter Graph", hr);
	hr = pMC->Stop();
	if (pMC)
	    pMC->Release();
	FreeFilters();
	return FALSE;
    }
    if (pMC)
	pMC->Release();

    return TRUE;
}

/*
 * Class:     com_ibm_media_protocol_ds_DataSource
 * Method:    stopDSGraph
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_media_protocol_ds_DataSource_stopDSGraph
(JNIEnv *env, jobject obj) {

    IMediaControl *pMC = NULL;
    HRESULT hr = pFg->QueryInterface(IID_IMediaControl, (void **)&pMC);
    if (hr != NOERROR) {
	printf("Error %x: Cannot get IMediaControl", hr);
	if (pMC)
	    pMC->Release();
	FreeFilters();
	return FALSE;
    }

    pJMFStream->setStopped(TRUE);

    pMC->Stop();
    if (hr != NOERROR) {
	printf("Error %x: Cannot Run", hr);
	if (pMC)
	    pMC->Release();
	FreeFilters();
	return FALSE;
    }

    if (pMC)
	pMC->Release();

    return TRUE;
}

/*
 * Class:     com_ibm_media_protocol_ds_DataSource
 * Method:    buildDSGraph
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_media_protocol_ds_DataSource_buildDSGraph
(JNIEnv *env, jobject obj, jint deviceNo) {
	
    CoInitialize(NULL); // initialize COM
    if (!InitCapFilter(deviceNo))
      return FALSE;
    if (!InitFilter2Stream())
      return FALSE;
    if (!ConnectGraph())
      return FALSE;

    return TRUE;
}

/*
 * Class:     com_ibm_media_protocol_ds_DataSource
 * Method:    destroyDSGraph
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_protocol_ds_DataSource_destroyDSGraph
(JNIEnv *env, jobject obj) {

    FreeFilters();
}

