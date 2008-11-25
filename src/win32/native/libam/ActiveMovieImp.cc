/*
 * @(#)ActiveMovieImp.cc	1.8 03/03/10
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

//#define DEBUG
#include <windows.h>
#include <WCHAR.h>
#include <stdio.h>
#include <streams.h>
#include <errors.h>
#include <mmsystem.h>
#include "com_sun_media_amovie_ActiveMovie.h"
#include "jni-util.h"
#include "JStream.h"
#include "JMSourceFilter.h"
#include "NullAudioFilter.h"


#define UNICODE
#include "errors.h"

static int  getStreamType(unsigned char *peekBuffer, int size);
static int  renderStream(JNIEnv *env, jobject amovie, JStream *astream, int streamType,
			 int tryNullAudio);
static BOOL renderFile(JNIEnv *env, jobject amovie, const jchar *filename, int length);
static char * szActiveMovieError =
              "Please verify that Active Movie is installed correctly\n";

static void
HandleError(char* msg)
{
    printf("Active Movie Error: %s\n", msg);
    return;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    openFile
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_amovie_ActiveMovie_openFile(JNIEnv *env,
					       jobject amovie,
					       jstring filename)
{
    // Get the file name from java
    char szFile[MAX_PATH];
    jboolean ok;
    
    //char *cfilename = (char*) env->GetStringUTFChars(filename, NULL);
    //strcpy(szFile, cfilename);
    //env->ReleaseStringUTFChars(filename, (const char *) cfilename);
    
    const jchar *cname = env->GetStringChars(filename, JNI_FALSE);

    int length = env->GetStringLength(filename);

    ok = renderFile( env, amovie, cname, length);

    env->ReleaseStringChars(filename, cname);
    
    return ok;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    openStream
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_amovie_ActiveMovie_openStream(JNIEnv *env,
						 jobject amovie,
						 jboolean seekable,
						 jboolean randomAccess,
						 jint streamType,
						 jlong contentLength)
{
    // Create a stream for the file. TODO: Change it to a stream
    JStream *astream = new JStream(env, amovie, seekable,
				   randomAccess, contentLength);
    SetIntField(env, amovie, "aStream", (jint)astream);

    // Try to create the filter graph
    int retVal =  renderStream(env, amovie, astream, (int) streamType, 1);

    if (retVal != 0) {
	printf("Error value: %x\n", retVal);
	return FALSE;
    } else
	return TRUE;
}


JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_setNSeekable(JNIEnv *env,
						  jobject amovie,
						  jboolean seekable)
{
    JStream *jstream = (JStream *) GetIntField(env, amovie, "aStream");
    if (jstream) {
	jstream->setSeekable((int) seekable);
    }
}


JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_doNRequest(JNIEnv *env,
						 jobject amovie,
						 jbyteArray jbuffer)
{
    JStream *jstream = (JStream *) GetIntField(env, amovie, "aStream");
    //printf("Native code calling jstream->goSpin()\n");
    if (jstream != NULL)
	jstream->doNRequest(env, amovie, jbuffer);
}

/**
 * Render a SourceStream by creating a source filter (JMSource) and calling
 *   Render() on it.
 */
static int
renderStream(JNIEnv *env, jobject amovie, JStream *astream, int streamType,
	     int tryNullAudio)
{
    HRESULT hr;
    IGraphBuilder *pGraph = (IGraphBuilder*)GetIntField(env, amovie, "pGraph");

    // Initialize ActiveMovie
    if (pGraph == NULL) {
	CoInitialize(NULL);
	hr = CoCreateInstance(CLSID_FilterGraph,
			      NULL,
			      CLSCTX_INPROC_SERVER,
			      IID_IGraphBuilder,
			      (void **)&pGraph);
	if (FAILED(hr)) {
	    printf("Error initializing Active Movie. Make sure Active Movie is correctly installed and try again.\n");
	    return -1;
	}
    }

    // Create the source filter and add it to the graph
    JMSource *sourceFilter = (JMSource *) new JMSource(NULL, &hr, astream,
						       streamType);
    CSourceStream *mpegPin = sourceFilter->getOutputPin();
    SetIntField(env, amovie, "filterPin", (jint)mpegPin);
    JMSourceStream * stream = (JMSourceStream *) mpegPin;
    
    if (sourceFilter != NULL && hr == NOERROR) {
	hr = pGraph->AddFilter(sourceFilter, NULL);
	if (hr == S_OK)
	    ; //printf("Added Filter!!\n");
	else
	    return -2;
    }


#define DEBUGAM_NO
#ifdef DEBUGAM
    HANDLE hfile = GetStdHandle(STD_OUTPUT_HANDLE);
    if (hfile == INVALID_HANDLE_VALUE) { 
	printf("Could not create log file\n");
    } else {
        HRESULT h = pGraph->SetLogFile(hfile);
	printf("SetLogFile() returned %x\n", h);
    }
#endif

    // If it is a system stream and there is no audio device, we need to
    // fool the graph builder into using a NullAudioFilter (renderer) so that
    // it can complete the graph.
    if (tryNullAudio && streamType == MPEGSYSTEM && waveOutGetNumDevs() < 1) {
	/* Try creating a Null Audio Filter */
	CNullAudioFilter *naf = (CNullAudioFilter *) new CNullAudioFilter(NULL, &hr);
	// Register the filter
	if (naf != NULL) {
	    // printf("Created CNullAudioFilter\n");
	    naf->Register();
	}
	
	hr = pGraph->AddFilter(naf, NULL);
	
	if (hr == S_OK)
	    ;//printf("Added CNullAudioFilter!!\n");
	else
	    ;//printf("Could not add CNullAudioFilter\n"); 
	/* End Null Audio Filter */
    }
    
    hr = pGraph->Render(mpegPin);

    if (FAILED(hr)) {
	return hr;
    }

    // Set the default synchronization source
    hr = pGraph->SetDefaultSyncSource();
    
    if (FAILED(hr)) {
	printf("Error setting default sync source\n");
        return -3;
    }

    SetIntField(env, amovie, "streamType", streamType);

    /*
    // Testing - Amith
    if (0) {
	IEnumFilters * pEnum;
	IFilter * pFilter;
	ULONG nFilters;
	IAsyncReader *iar;
	
	hr = pGraph->EnumFilters(&pEnum);
	while (1) {
	    pEnum->Next(1, &pFilter, &nFilters);
	    if (nFilters == 0)
		break;
	    if (hr = pFilter->QueryInterface(IID_IAsyncReader, (void **)&iar))
		if (hr != 0)
		    printf("Couldn't get IAsyncReader\n");
	    CLSID clsid;
	    pFilter->GetClassID(&clsid);
	    printf("Filter-------------------  %s\n", GuidNames[clsid]);
	    // Enumerate the filter pins
	    {
		IEnumPins *enumPins;
		ULONG nPins;
		IPin *pPin;
		
		pFilter->EnumPins(&enumPins);
		while (1) {
		    enumPins->Next(1, &pPin, &nPins);
		    if (nPins == 0)
			break;
		    printf("Pins------------\n");
		    // Enumerate the media types
		    {
			AM_MEDIA_TYPE *pMedia;
			IEnumMediaTypes *enumMedia;
			ULONG nMedia;
			printf("Media------\n");
			pPin->EnumMediaTypes(&enumMedia);
			while (1) {
			    enumMedia->Next(1, &pMedia, &nMedia);
			    if (nMedia == 0)
				break;
			    printf("Major   = %s\n", GuidNames[pMedia->majortype]);
			    printf("SubType = %s\n", GuidNames[pMedia->subtype]);
			    printf("Format  = %s\n", GuidNames[pMedia->formattype]);
			}
		    }
		    pPin->Release();
		}
	    }
	    pFilter->Release();
	    printf("Found filter\n");
	}
	pEnum->Release();
    }
    */
    // Try to hide the video window at startup
    IVideoWindow *pivw;
    hr = pGraph->QueryInterface(IID_IVideoWindow, (void **)&pivw);
    if (FAILED(hr)) {
	HandleError( "can't get IVideoWindow interface");
    } else {
	pivw->put_Visible(OAFALSE);
	pivw->put_AutoShow(OAFALSE);
	pivw->put_BackgroundPalette(OATRUE);
	/* pivw->put_WindowStyle(WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS);*/
	pivw->Release();
    }

    // Store the graph object in a java field
    SetIntField(env, amovie, "pGraph", (jint) pGraph);
    return 0; // Realize succeeded
}

static BOOL
renderFile(JNIEnv *env, jobject amovie, const jchar *filename, int length)
{
    HRESULT hr;
    WCHAR wPath[MAX_PATH];
    IGraphBuilder *pGraph = (IGraphBuilder*)GetIntField(env, amovie, "pGraph");
    
    if (pGraph == NULL) {
	CoInitialize(NULL);
	hr = CoCreateInstance(CLSID_FilterGraph,
			      NULL,
			      CLSCTX_INPROC_SERVER,
			      IID_IGraphBuilder,
			      (void **)&pGraph);
	if (FAILED(hr)) {
	    printf("Error initializing Active Movie. Make sure Active Movie is correctly installed and try again.\n");
	    return FALSE;
	}
	// Store the graph object in a java field
	SetIntField(env, amovie, "pGraph", (jint) pGraph);
    }
    
    // MultiByteToWideChar(CP_ACP, 0, filename, -1, wPath, MAX_PATH );
    // hr = pGraph->RenderFile((char *)filename, NULL);
    
    memcpy( wPath, filename, length * sizeof( WCHAR));
    
    hr = pGraph->RenderFile(wPath, NULL);
    
    if (FAILED(hr)) {
	printf("Error value: %x\n", hr);
        return FALSE;
    }

    int streamType = 3;
    // Read the file header and determine the stream type
    {
	FILE *fp;
	fp = _wfopen(filename, (const wchar_t *)"rb");
	if (fp != NULL) {
	    unsigned char header[65536];
	    int nRead = fread(header, 1, 65536, fp);
	    if (nRead == 65536) {
		streamType = ::getStreamType(header, 65536);
	    }
	    fclose(fp);
	}
    }
    if (streamType == 0)
	streamType = 3;
    SetIntField(env, amovie, "streamType", streamType);
    
    // Set the default synchronization source - Audio Renderer if available,
    // else use the system clock
    hr = pGraph->SetDefaultSyncSource();
    
    if (FAILED(hr)) {
	printf("Error setting default sync source\n");
        return FALSE;
    }
    
    IVideoWindow *pivw;
    hr = pGraph->QueryInterface(IID_IVideoWindow, (void **)&pivw);
    if (FAILED(hr)) {
	HandleError( "can't get IVideoWindow interface");
    } else {
	pivw->put_Visible(OAFALSE);
	pivw->put_AutoShow(OAFALSE);
	pivw->put_BackgroundPalette(OATRUE);
	/* pivw->put_WindowStyle(WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS);*/
	pivw->Release();
    }
    return TRUE; // Realize succeeded
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    run
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_amRun(JNIEnv *env,
					  jobject amovie)
{
    //IGraphBuilder *pGraph = (IGraphBuilder*)(unhand(self)->pGraph);
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    JMSourceStream *filterPin = (JMSourceStream *) GetIntField(env, amovie,
							       "filterPin");
    IMediaControl *pimc;
    HRESULT hr;
    OAFilterState state;
    //printf("In run\n");

    if (pGraph == NULL) {
        HandleError( "Error initializing Active Movie. Make sure Active Movie is correctly installed and try again.");
        return;
    }
    hr = pGraph->QueryInterface(IID_IMediaControl, (void **)&pimc);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaControl interface");
        return;
    }
    hr = pimc->Run();
    if (filterPin != NULL)
	filterPin->Restart();
    if (FAILED(hr)) {
	printf("IMediaControl->Run() failed\n");
    }
    pimc->Release();
    //printf("Called IMediaControl->Run()\n");
}

JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_stopDataFlow(JNIEnv *env,
						   jobject amovie,
						   jint jfilterPin,
						   jboolean stop)
{
    JMSourceStream *filterPin = (JMSourceStream *) jfilterPin;
    if (filterPin != NULL)
	filterPin->stopDataFlow((int) stop);
}
    
/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    pause
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_amPause(JNIEnv *env,
					    jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*)GetIntField(env, amovie, "pGraph");
    IMediaControl *pimc;
    HRESULT hr;
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    hr = pGraph->QueryInterface(IID_IMediaControl, (void **)&pimc);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaControl interface");
        return;
    }
    // printf("Calling pimc->Pause()\n");
    pimc->Pause();
    OAFilterState fstate;
    long count = 0;
    do {
	hr = pimc->GetState(20, &fstate);
	if (hr == S_OK && fstate == State_Paused) {
	    break;
	}
	if (hr != S_OK && hr != VFW_S_STATE_INTERMEDIATE)
	    break;
	Sleep(20);
	count += 40;
	if (count > 10000)
	    break;
    } while (1);

    pimc->Release();
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    amStopWhenReady
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_amStopWhenReady(JNIEnv *env,
						      jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaControl *pimc;
    HRESULT hr;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    
    hr = pGraph->QueryInterface(IID_IMediaControl, (void **)&pimc);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaControl interface");
        return;
    }
    
    pimc->StopWhenReady();
    OAFilterState fstate;
    long count = 0;
    do {
	hr = pimc->GetState(20, &fstate);
	if (hr == S_OK && fstate == State_Stopped) {
	    break;
	}
	if (hr != S_OK && hr != VFW_S_STATE_INTERMEDIATE)
	    break;
	Sleep(20);
	count += 40;
	if (count > 10000)
	    break;
    } while (1);
    pimc->Release();
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    amStop
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_amStop(JNIEnv *env,
					   jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaControl *pimc;
    HRESULT hr;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    
    hr = pGraph->QueryInterface(IID_IMediaControl, (void **)&pimc);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaControl interface");
        return;
    }
    
    pimc->Stop();
    pimc->Release();
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getDuration
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_com_sun_media_amovie_ActiveMovie_getDuration(JNIEnv *env,
						  jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaPosition *pimp = NULL;
    HRESULT hr;
    REFTIME ref = 1E+12;

    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }
    
    hr = pGraph->QueryInterface(IID_IMediaPosition, (void**)&pimp);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaPosition interface");
        return 0;
    }

    hr = pimp->get_Duration(&ref);

    pimp->Release();
    return (jdouble) ref;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getCurrentPosition
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_com_sun_media_amovie_ActiveMovie_getCurrentPosition(JNIEnv *env,
							 jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaPosition *pimp;
    HRESULT hr;
    REFTIME ref;
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }
    hr = pGraph->QueryInterface(IID_IMediaPosition, (void**)&pimp);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaPosition interface");
        return 0;
    }
    hr = pimp->get_CurrentPosition(&ref);
    
    pimp->Release();
    return (jdouble) ref;
}


/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    setCurrentPosition
 * Signature: (D)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_setCurrentPosition(JNIEnv *env,
							 jobject amovie,
							 jdouble pos)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaPosition *pimp;
    HRESULT hr;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    
    hr = pGraph->QueryInterface(IID_IMediaPosition, (void**)&pimp);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaPosition interface");
        return;
    }
    //printf("Calling put_CurrentPosition()\n");
    hr = pimp->put_CurrentPosition((REFTIME)pos);
    //printf("Called put_CurrentPosition()\n");
    pimp->Release();
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    setRate
 * Signature: (D)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_setRate(JNIEnv *env,
					      jobject amovie,
					      jdouble rate)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaPosition *pimp;
    HRESULT hr;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    
    hr = pGraph->QueryInterface(IID_IMediaPosition, (void**)&pimp);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaPosition interface");
        return;
    }
    hr = pimp->put_Rate((double)rate);

    {
      double d;
      pimp->get_Rate(&d);
      //printf("Actual rate set is %f\n", d);
    }
    
    pimp->Release();
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getRate
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_com_sun_media_amovie_ActiveMovie_getRate(JNIEnv *env,
					      jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaPosition *pimp;
    double rate;
    HRESULT hr;

    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }

    hr = pGraph->QueryInterface(IID_IMediaPosition, (void **)&pimp);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaPosition interface");
        return 1.0;
    }
    pimp->get_Rate(&rate);
    pimp->Release();
    
    return (jdouble) rate;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getTime
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL
Java_com_sun_media_amovie_ActiveMovie_getTime(JNIEnv *env,
					      jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IReferenceClock *refClock;
    IMediaFilter *mediaFilter;
    jlong time;
    HRESULT hr;
    REFERENCE_TIME rtime;
    //printf("AM: GetTime\n");
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }

    hr = pGraph->QueryInterface(IID_IMediaFilter, (void **)&mediaFilter);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaFilter interface");
        return 0;
    }
    
    mediaFilter->GetSyncSource(&refClock);
    refClock->GetTime(&rtime);
    refClock->Release();
    mediaFilter->Release();
    time = (jlong) (rtime / 10);
    //printf("AM: GetTIme returning %u\n", time);
    return time;
}



/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    setStopTime
 * Signature: (D)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_setStopTime(JNIEnv *env,
						  jobject amovie,
						  jdouble time)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaPosition *pimp;
    HRESULT hr;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    
    hr = pGraph->QueryInterface(IID_IMediaPosition, (void**)&pimp);
    if (FAILED(hr)) {
        HandleError( "can't get IMediaPosition interface");
        return;
    }
    hr = pimp->put_StopTime((REFTIME)time);
    pimp->Release();
}


/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getVolume
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_amovie_ActiveMovie_getVolume(JNIEnv *env,
						jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IBasicAudio *piba;
    HRESULT hr;
    long volume;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }
    
    hr = pGraph->QueryInterface(IID_IBasicAudio, (void **)&piba);
    if (FAILED(hr)) {
        HandleError( "can't get IBasicAudio interface");
        return 0;
    }
    piba->get_Volume(&volume);
    piba->Release();
    return volume;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    setVolume
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_setVolume(JNIEnv *env,
						jobject amovie,
						jint volume)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IBasicAudio *piba;
    HRESULT hr;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    
    hr = pGraph->QueryInterface(IID_IBasicAudio, (void **)&piba);
    if (FAILED(hr)) {
        HandleError( "can't get IBasicAudio interface");
        return;
    }
    piba->put_Volume(volume);
    piba->Release();
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getBitRate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_amovie_ActiveMovie_getBitRate(JNIEnv *env,
						    jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IBasicVideo *pibv;
    HRESULT hr;
    long bitrate = 0;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }
    
    hr = pGraph->QueryInterface(IID_IBasicVideo, (void **)&pibv);
    if (FAILED(hr)) {
        HandleError( "can't get IBasicVideo interface");
        return 0;
    }
    pibv->get_BitRate(&bitrate);
    pibv->Release();
    return (jint) bitrate;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getFrameRate
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_com_sun_media_amovie_ActiveMovie_getFrameRate(JNIEnv *env,
						    jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IBasicVideo *pibv;
    HRESULT hr;
    long avgTimePerFrame = 0;
    double frameRate;
    REFTIME rt = (double) 0;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }
    
    hr = pGraph->QueryInterface(IID_IBasicVideo, (void **)&pibv);
    if (FAILED(hr)) {
        HandleError( "can't get IBasicVideo interface");
        return 0;
    }
    pibv->get_AvgTimePerFrame(&rt);
    pibv->Release();
    avgTimePerFrame = (double) rt;
    //printf("Avg time per frame = %f\n", avgTimePerFrame);
    avgTimePerFrame /= 10000; // Convert to millisecs
    if (avgTimePerFrame != 0)
	frameRate = 1000.0 / (double)avgTimePerFrame;
    else
	frameRate = 0.0;
    return (jdouble) frameRate;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getVideoWidth
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_amovie_ActiveMovie_getVideoWidth(JNIEnv *env,
						    jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IBasicVideo *pibv;
    HRESULT hr;
    long width = 0;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }
    
    hr = pGraph->QueryInterface(IID_IBasicVideo, (void **)&pibv);
    if (FAILED(hr)) {
        HandleError( "can't get IBasicVideo interface");
        return 0;
    }
    pibv->get_VideoWidth(&width);
    pibv->Release();
    return (jint) width;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    getVideoHeight
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_amovie_ActiveMovie_getVideoHeight(JNIEnv *env,
						     jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IBasicVideo *pibv;
    HRESULT hr;
    long height = 0;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return 0;
    }
    
    hr = pGraph->QueryInterface(IID_IBasicVideo, (void **)&pibv);
    if (FAILED(hr)) {
        HandleError( "can't get IBasicVideo interface");
        return 0;
    }
    pibv->get_VideoHeight(&height);
    pibv->Release();
    return (jint) height;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    setVisible
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_setVisible(JNIEnv *env,
						 jobject amovie,
						 jint value)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IVideoWindow *pivw;
    HRESULT hr;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    
    hr = pGraph->QueryInterface(IID_IVideoWindow, (void **)&pivw);
    if (FAILED(hr)) {
        HandleError( "can't get IVideoWindow interface");
        return;
    }
    
    if (value)
	pivw->put_Visible(OATRUE);
    else
	pivw->put_Visible(OAFALSE);
    
    /*    pivw->put_WindowStyle(WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS);*/
    pivw->Release();
}


/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    setOwner
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_setOwner(JNIEnv *env,
					       jobject amovie,
					       jint hwnd)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IVideoWindow *pivw;
    HRESULT hr;
    
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    
    hr = pGraph->QueryInterface(IID_IVideoWindow, (void **)&pivw);
    if (FAILED(hr)) {
        HandleError( "can't get IVideoWindow interface");
        return;
    }
    pivw->put_Owner((OAHWND)hwnd);
    pivw->put_WindowStyle(WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS);
    pivw->put_MessageDrain((OAHWND)hwnd);
    pivw->Release();
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    setWindowPosition
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_setWindowPosition(JNIEnv *env,
							jobject amovie,
							jint left, jint top,
							jint right, jint bottom)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IVideoWindow *pivw;
    HRESULT hr;
    if (pGraph == NULL) {
        HandleError( szActiveMovieError );
        return;
    }
    hr = pGraph->QueryInterface(IID_IVideoWindow, (void **)&pivw);
    if (FAILED(hr)) {
        HandleError( "can't get IVideoWindow interface");
        return;
    }
    pivw->SetWindowPosition(left, top, right, bottom);
    pivw->Release();
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    dispose0
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_dispose0(JNIEnv *env,
					       jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IVideoWindow *pivw;

    HRESULT hr;
    if (pGraph == NULL) {
        return;
    }
    hr = pGraph->QueryInterface(IID_IVideoWindow, (void **)&pivw);
    if (SUCCEEDED(hr)) {
      pivw->put_Visible(OAFALSE);
      pivw->put_Owner(NULL);
      pivw->Release();
    }

    // Destroy the stream if necessary
    JStream * astream = (JStream *) GetIntField(env, amovie, "aStream");
    if (astream)
	astream->close();
    SetIntField(env, amovie, "aStream", NULL);
    
    pGraph->Release();
    SetIntField(env, amovie, "pGraph", NULL);
    
    CoUninitialize();
}

typedef struct _WndData {
    char name[MAX_PATH];
    HWND hWnd;
} WndData;

static
BOOL CALLBACK MyEnumChildWindowsProc(HWND hWnd, LPARAM lParam)
{
    char text[MAX_PATH];
    WndData* pData = (WndData*)lParam;
    GetWindowText(hWnd, text, sizeof(text));
    if (strcmp(text, pData->name) == 0) {
        pData->hWnd = hWnd;
        return FALSE;
    }
    return TRUE;
}

static
BOOL CALLBACK MyEnumWindowsProc(HWND hWnd, LPARAM lParam)
{
    BOOL bRet;
    WndData* pData = (WndData*)lParam;
    bRet = EnumChildWindows(hWnd, (WNDENUMPROC)MyEnumChildWindowsProc, lParam);
    if (pData->hWnd != NULL) {
        return FALSE;
    }
    return TRUE;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    findWindow
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_amovie_ActiveMovie_findWindow(JNIEnv *env,
						 jclass classAM,
						 jstring jname)
{
    BOOL bRet;
    WndData data;
    HWND hWndParent = NULL;
    data.hWnd = NULL;

    // Copy the java string to a C string
    char *cname = (char*) env->GetStringUTFChars(jname, NULL);
    strcpy(data.name, cname);
    env->ReleaseStringUTFChars(jname, (const char *)cname);

    bRet = EnumWindows((WNDENUMPROC)MyEnumWindowsProc, (LPARAM)(&data));
    if (data.hWnd != NULL) {
        hWndParent = GetParent(data.hWnd);
    }
    return (long)hWndParent;
}


/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    waitForCompletion
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_amovie_ActiveMovie_waitForCompletion(JNIEnv *env,
							jobject amovie)
{
    IGraphBuilder *pGraph = (IGraphBuilder*) GetIntField(env, amovie, "pGraph");
    IMediaEvent *pME;
    HRESULT hr;
    
    if (pGraph == NULL) {
	HandleError( szActiveMovieError );
	return (jboolean) 0; // FALSE
    }
    hr = pGraph->QueryInterface(IID_IMediaEvent, (void **)&pME);
    if (FAILED(hr)) {
	HandleError( "can't get IMediaEvent interface");
	return (jboolean) 0; // FALSE
    }
    long l;
    
    hr = pME->WaitForCompletion(0, &l);
    //printf("After wait for completion\n");
    /*
    if (FAILED(hr)) {
	HandleError( "Error in WaitForCompletion");
	return (jboolean) 0; // FALSE
    }
    */
    pME->Release();
    if (l == 0)
	return (jboolean) 0;
    else
	return (jboolean) 1;
}


static int
getStreamType(unsigned char *peekBuffer, int size)
{
    int index = 0;
    unsigned char ch;
    int streamType = 0;
    unsigned int sequence = 0xFFFFFFFF;

    while (index < size - 4 && streamType != MPEGSYSTEM) {
	ch = peekBuffer[index++];
	sequence = (sequence << 8) | ch;
	if ((sequence & 0xFFFFFF00) == 0x00000100) {
	    /* Found start code. */
	    switch (ch) {
	    case 0xBA:
	    case 0xBB:
		streamType = MPEGSYSTEM;
		//PRINT("************  Found system stream\n");
		break;
	    case 0xB3:
		streamType = MPEGVIDEO;
		//PRINT("************  Found Video stream\n");
		break;
	    default:
		//PRINT("************  Found Audio stream?\n");
		if (streamType == 0)
		    streamType = MPEGAUDIO;
	    }
	}
    }
    
    // If no stream type was detected
    if (streamType == 0)
	streamType = MPEGAUDIO;
    
    // If the first 13 bits are "1111 1111 1111 1" then its an audio stream
    // This is needed because some audio streams have a System stream start
    // code embedded in the data.
    
    if (peekBuffer[0] == 0xFF &&
	(peekBuffer[1] & 0xF8) == 0xF8)
	streamType = MPEGAUDIO;
    return streamType;
    
}

JNIEXPORT jint JNICALL
Java_com_sun_media_amovie_ActiveMovie_getStreamType(JNIEnv *env,
						    jobject amovie,
						    jbyteArray jBuffer,
						    jint size)
{
    int streamType;
    unsigned char *peekBuffer = (unsigned char *)
	(env)->GetByteArrayElements(jBuffer, NULL);

    streamType = ::getStreamType(peekBuffer, (int) size);
    
    env->ReleaseByteArrayElements(jBuffer, (signed char*) peekBuffer, JNI_ABORT);
    return streamType;
}


/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    nCreateCache
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_amovie_ActiveMovie_nCreateCache(JNIEnv *env,
						   jobject amovie,
						   jint size)
{
    char * array = new char[size];
    if (array != NULL)
	return (jint) (int) array;
    else
	return 0;
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    nAddToCache
 * Signature: (II[BII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_nAddToCache(JNIEnv *env,
						  jobject amovie,
						  jint cacheBuffer,
						  jint cacheOffset,
						  jbyteArray jbuffer,
						  jint bufOffset,
						  jint size)
{
    char *array = (char *) env->GetByteArrayElements(jbuffer, 0);
    char *src = array + bufOffset;
    memcpy((char*)cacheBuffer + cacheOffset, src, (int) size);
    env->ReleaseByteArrayElements(jbuffer, (signed char *) array, 0);
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    nGetFromCache
 * Signature: (II[BII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_nGetFromCache(JNIEnv *env,
						    jobject amovie,
						    jint cacheBuffer,
						    jint cacheOffset,
						    jbyteArray jbuffer,
						    jint bufOffset,
						    jint size)
{
    char *array = (char *) env->GetByteArrayElements(jbuffer, 0);
    char *dst = array + bufOffset;
    memcpy(dst, (char*)cacheBuffer + cacheOffset, (int) size);
    env->ReleaseByteArrayElements(jbuffer, (signed char*) array, 0);    
}

/*
 * Class:     com_sun_media_amovie_ActiveMovie
 * Method:    nFreeCache
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_com_sun_media_amovie_ActiveMovie_nFreeCache(JNIEnv *env,
						 jobject amovie,
						 jint cacheBuffer)
{
    if (cacheBuffer != 0)
	delete [] (char *) cacheBuffer;
}

