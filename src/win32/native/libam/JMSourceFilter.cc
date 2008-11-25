/*
 * @(#)JMSourceFilter.cc	1.15 98/09/24
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
#include <errors.h>
#include <stdio.h>
#include <io.h>
#include <streams.h>
#include <olectl.h>
#include <initguid.h>
#include <strmif.h>
#include <objidl.h>
#include <unknwn.h>
#include <cguid.h>
#include "guid.h"
#include "JStream.h"
#include "JMSourceFilter.h"

// Macros for debug messages

#ifdef DEBUG
#define PRINT(s) printf(s)
#define PRINT2(s, n) printf(s, n);
#else
#define PRINT(s) 
#define PRINT2(s, n) 
#endif

int
headerToFooter(unsigned char *peekBuffer, int size)
{
    int index = 0;
    unsigned char ch;
    int streamType = 0;
    unsigned int sequence = 0xFFFFFFFF;
    while (index < size - 4) {
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
		
		// This chunk of code is a hack to get ActiveMovie
		// to think that the movie is really large.
		// The time stamps of the video frames in the first
		// 64K are changed to a large value and the data is
		// reused when AM asks for the footer. AM seems to
		// want to find the length of the movie by getting
		// the time stamp on the last frame. This happens
		// only in video-only streams.
		{
		    int i = 0;
		    unsigned char c0, c1, c2, c3, c4, c5, c6;
		    while (i < 1000 && (index + i) < size) {
			ch = peekBuffer[index+i];
			//printf("%03d = %02X\n", i, ch);
			c6 = c5;
			c5 = c4;
			c4 = c3;
			c3 = c2;
			c2 = c1;
			c1 = c0;
			c0 = ch;
			
			if (c6 == 0) {
			    if (c5 == 0)
				if (c4 == 0x01)
				    if (c3 == 0xB8) {
					peekBuffer[index+i-2] |= 0x7C;
					i = 1001;  // Done
				    }
			}
			
			i++;
		    }
		}
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

/*************************************************************************
 * MPEG SOURCE class derived from CSource
 *************************************************************************/
void __cdecl DbgAssert(char const * a, char const * b, int c) {
}
// Constructor

JMSource::JMSource(LPUNKNOWN lpunk, HRESULT *phr, JStream *stream, int streamType) :
    CSource(NULL,
            lpunk,
            CLSID_JMSource,
            phr)
{
    PRINT("JMSource::JMSource\n");
    CAutoLock cAutoLock(&m_cStateLock);
    // m_filterPosition = NULL;
    m_paStreams    = (CSourceStream **) new JMSourceStream*[1];
    if (m_paStreams == NULL) {
        *phr = E_OUTOFMEMORY;
	return;
    }
    m_paStreams[0] = new JMSourceStream(phr, this, L"JMSourceStream", stream,
					streamType);
    if (m_paStreams[0] == NULL) {
        *phr = E_OUTOFMEMORY;
	return;
    }

} // End constructor

CSourceStream *
JMSource::getOutputPin()
{
    PRINT("JMSource::getOutputPin\n");
    return m_paStreams[0];
}

// CreateInstance

CUnknown *
JMSource::CreateInstance(LPUNKNOWN lpunk, HRESULT *phr)
{
    PRINT("JMSource::CreateInstance\n");
    
    CUnknown *punk = new JMSource(lpunk, phr, NULL, 3);
    if (punk == NULL) {
	*phr = E_OUTOFMEMORY;
    }
    return punk;
} // CreateInstance

STDMETHODIMP
JMSource::NonDelegatingQueryInterface(REFIID id, void **ppv)
{
    HRESULT result;
    /*
    if (m_filterPosition == NULL) {
	m_filterPosition = new JMFilterPosition(NULL, NULL, &result,
						m_pLock);
    }
    */
    /*
    if (0) {//id == IID_IMediaPosition) {
	PRINT("Returning JMFilterPosition\n");
	*ppv = m_filterPosition;
	return S_OK;
	//return m_filterPosition->NonDelegatingQueryInterface(id, ppv);
    } else
    */
    return CSource::NonDelegatingQueryInterface(id, ppv);
}


STDMETHODIMP
JMSource::Run(REFERENCE_TIME tStart)
{
    PRINT("JMSource::Run\n");
    return CSource::Run(tStart);
}

STDMETHODIMP
JMSource::Pause()
{
    CAutoLock cObjectLock(m_pLock);
    PRINT("JMSource::Pause\n");
    //printf("In CSource::Pause()\n");
    // notify all pins of the change to active state
    if (m_State == State_Stopped) {
	int cPins = GetPinCount();
	for (int c = 0; c < cPins; c++) {

	    CBasePin *pPin = GetPin(c);

            // Disconnected pins are not activated - this saves pins
            // worrying about this state themselves
	    //printf("Inside loop of CSource::Pause()\n");    
            if (pPin->IsConnected()) {
	        HRESULT hr = pPin->Active();
		//printf("Called pPin->Active()\n");
	        if (FAILED(hr)) {
		    return hr;
	        }
            }
	}
    }
    //printf("Out CSource::Pause()\n");
    m_State = State_Paused;
    return S_OK;
    /*    PRINT("Calling CSource::Pause()\n");*/
    /*    return CSource::Pause();*/
}


/*************************************************************************
 * JMSourceStream class
 *************************************************************************/

// Constructor

JMSourceStream::JMSourceStream(HRESULT *phr,
			       JMSource *pParent,
			       LPCWSTR pPinName,
			       JStream *stream,
			       int streamType) :
    CSourceStream(NULL, phr, pParent, pPinName)
{
    // unsigned char peekBuffer[65536];
    CAutoLock cAutoLock(&m_cSharedState);
    
    PRINT("JMSourceStream::JMSourceStream\n");
    m_discontinuity = 1;
    m_astream = stream;
    m_seekCount = 0;
    m_sourcePosition = NULL;
    m_firstRead = 1;
    if (!m_astream->getLength(&m_mediaLength))
	m_mediaLength = 0;
    PRINT2(" - Media Length is %d\n", m_mediaLength);
    m_readingHeader = 1;

    m_streamType = streamType;
    m_totalHeaderRead = 0;
    m_DontPush = 0;
}

// Destructor

JMSourceStream::~JMSourceStream()
{
    CAutoLock cAutoLock(&m_cSharedState);
}

HRESULT
JMSourceStream::FillBuffer(IMediaSample *pms)
{
    REFERENCE_TIME startTime, stopTime;
    PRINT("JMSourceStream::FillBuffer\n");
    BYTE *pData;
    long lDataLen;
    long actualRead;
    CAutoLock cAutoLock(&m_cSharedState);

    if (m_readingHeader) {
	m_readingHeader = 0;
	m_astream->restart();  // Should we do this for http streams
	m_discontinuity = 1;
    }
    
    pms->GetPointer(&pData);
    lDataLen = pms->GetSize();
    
    if (m_discontinuity) {
	pms->SetDiscontinuity(TRUE);
	//printf("Set Discontinuity\n");
	m_discontinuity = 0;
    }
    // printf("StartTime = %f, StopTime = %f\n",(double)startTime,(double)stopTime);
    
    int res = m_astream->read(pData, lDataLen, &actualRead);
    if (m_astream->getDiscontinuity())
	pms->SetDiscontinuity(TRUE);
    
    if (!res)
 	actualRead = 0;

    pms->SetActualDataLength(actualRead);

    return S_OK;
}

HRESULT
JMSourceStream::GetMediaType(CMediaType *pmt)
{
    CAutoLock cAutoLock2(m_pFilter->pStateLock());
    CAutoLock cAutoLock(&m_cSharedState);

    PRINT("JMSourceStream::GetMediaType\n");

    const GUID *sub = (m_streamType == MPEGSYSTEM)? &MEDIASUBTYPE_MPEG1System :
	((m_streamType == MPEGVIDEO) ? &MEDIASUBTYPE_MPEG1Video :
	                               &MEDIASUBTYPE_MPEG1Audio);
    //printf("Media Type is %d\n", *(pmt->Type()));
    pmt->SetType(&MEDIATYPE_Stream);
    
    pmt->SetSubtype(sub);
    //pmt->SetSubtype(&MEDIASUBTYPE_NULL);
    pmt->SetFormatType(&TIME_FORMAT_NONE);
    pmt->SetSampleSize(1);
    pmt->SetTemporalCompression(TRUE);
    
    return S_OK;
}

HRESULT
JMSourceStream::CheckMediaType(const CMediaType *cmt)
{
    CAutoLock cAutoLock(m_pFilter->pStateLock());
    PRINT("JMSourceStream::CheckMediaType\n");
    PRINT2(" Media Type = %s\n", GuidNames[*(cmt->Type())]);
    PRINT2(" Media SubType = %s\n", GuidNames[*(cmt->Subtype())]);
    PRINT2(" Media FormatType = %s\n", GuidNames[*(cmt->FormatType())]);
    PRINT2(" Format = %d\n", cmt->Format());
    PRINT2(" IsFixedSize = %d\n", cmt->IsFixedSize());
    PRINT2(" SampleSize = %d\n\n", cmt->GetSampleSize());
    //HRESULT result = CSourceStream::CheckMediaType(cmt);
    //printf("..............Returning result %d\n", result);
    const GUID *sub = (m_streamType == MPEGSYSTEM)? &MEDIASUBTYPE_MPEG1System :
	((m_streamType == MPEGVIDEO) ? &MEDIASUBTYPE_MPEG1Video :
	                               &MEDIASUBTYPE_MPEG1Audio);
    if (*(cmt->Type()) == MEDIATYPE_Stream &&
	*(cmt->Subtype()) == *sub)
	return S_OK;
    else
	return E_INVALIDARG;
}

//
// DecideBufferSize
//
// This will always be called after the format has been sucessfully
// negotiated. So we have a look at m_mt to see what size image we agreed.
// Then we can ask for buffers of the correct size to contain them.
//
HRESULT
JMSourceStream::DecideBufferSize(IMemAllocator *pAlloc,
				   ALLOCATOR_PROPERTIES *pProperties)
{
    PRINT("JMSourceStream::DecideBufferSize\n");
    CAutoLock cAutoLock2(m_pFilter->pStateLock());
    ASSERT(pAlloc);
    ASSERT(pProperties);
    HRESULT hr = NOERROR;
    pProperties->cBuffers = 1;
    pProperties->cbBuffer = 65536;

    ASSERT(pProperties->cbBuffer);

    // Ask the allocator to reserve us some sample memory, NOTE the function
    // can succeed (that is return NOERROR) but still not have allocated the
    // memory that we requested, so we must check we got whatever we wanted

    ALLOCATOR_PROPERTIES Actual;
    hr = pAlloc->SetProperties(pProperties,&Actual);
    if (FAILED(hr)) {
	PRINT("FAILED(hr) DecideBufferSize\n");
        return hr;
    }

    // Is this allocator unsuitable
    /*
    if (Actual.cbBuffer < pProperties->cbBuffer) {
	PRINT("buffer not allocated in DecideBufferSize\n");
        return E_FAIL;
    }
    */

    // PRINT("Done DecideBufferSize\n");
    return S_OK;

} // DecideBufferSize

STDMETHODIMP
JMSourceStream::NonDelegatingQueryInterface(REFIID id, void **ppObject)
{
    HRESULT result;
    PRINT("JMSourceStream::NonDelegatingQueryInterface\n - 1\n");
    if (m_sourcePosition == NULL) {
	m_sourcePosition = new JMSourcePosition(NULL, NULL, &result,
						&m_crit);
	PRINT2(" - sourcePosition = %d\n", m_sourcePosition);
	m_sourcePosition->setSourceStream(this);
    }
    
    if (id == IID_IStream) {
	PRINT(" - 3\n");
	return GetInterface((IStream *) this, ppObject);
    } else if (id == IID_IMediaPosition) {
	PRINT2(" - 4 sourcePosition = %d\n", m_sourcePosition);
	return m_sourcePosition->NonDelegatingQueryInterface(id, ppObject);
    } else {
	PRINT(" - 5\n");
	return CSourceStream::NonDelegatingQueryInterface(id, ppObject);
    }
}

HRESULT
JMSourceStream::Restart()
{
    PRINT("JMSourceStream::Restart\n");
    return Run();
}

STDMETHODIMP
JMSourceStream::Run(void)
{
    PRINT("JMSourceStream::Run\n");
    m_DontPush = 0;
    return CSourceStream::Run();
}

// IStream methods for JMSourceStream

STDMETHODIMP
JMSourceStream::Read(void *pv, ULONG cb, ULONG *pcbRead)
{
    PRINT("JMSourceStream::Read\n");
    long bytesRead;
    CAutoLock lock(m_pFilter->pStateLock());
    CAutoLock cAutoLock(&m_cSharedState);
    m_readingHeader = 1;

    // If the stream cant seek, then return S_FALSE on second read
    if (m_astream->getSeekable() == 0) {
	// If its a video-only stream, we need to give it a footer
	if (m_streamType == MPEGVIDEO) {
	    if (m_seekCount == 2) {
		if (pcbRead != NULL)
		    *pcbRead = cb;
		// copy the modified footer to the data buffer

		memcpy(pv, m_peekBuffer, cb);
		m_seekCount++;
		return S_OK;
	    } else if (m_seekCount == 3) { // Already sent dummy bytes
		if (pcbRead != NULL)
		    *pcbRead = 0;
		return S_FALSE;
	    }
	} else if (m_seekCount > 1) {
	    if (pcbRead != NULL)
		*pcbRead = 0;
	    return S_FALSE;
	}
    }

    int res = m_astream->read(pv, cb, &bytesRead);
    
    if (m_firstRead && (m_streamType == MPEGVIDEO)) {
	m_firstRead = 0;
	memcpy(m_peekBuffer, pv, bytesRead);
	headerToFooter(m_peekBuffer, bytesRead);
    }
    
    //printf("Bytes read = %d\n", bytesRead);    
    if (pcbRead != NULL)
	*pcbRead = bytesRead;

    if (bytesRead == 0) {
	PRINT("##### EOF\n");
	m_discontinuity = 1;
	// m_astream->restart();
	return S_FALSE;
    }
    return S_OK;
}

STDMETHODIMP
JMSourceStream::Write(const void *pv, ULONG cb, ULONG *pcbWritten)
{
    PRINT("JMSourceStream::Write\n");
    return S_OK;
}

HRESULT
JMSourceStream::ChangePosition(long position)
{
    PRINT2("JMSourceStream::ChangePosition()  %d \n", position);
    CAutoLock cAutoLock(&m_cSharedState);
    CAutoLock lock2(m_pFilter->pStateLock());
    //m_discontinuity = 1;
    // PRINT2(" - ChangePosition()  %d \n", position);
    if (m_astream->getSeekable() && !m_DontPush) {
	m_discontinuity = 1;
	m_astream->seek(position, NULL);
	PRINT(" - 2\n");
    } else if (position == 0 && !m_DontPush) {
	PRINT(" - 3\n");
	m_discontinuity = 1;
	m_astream->restart();
    }
    PRINT(" - 4\n");
    return S_OK;
}

STDMETHODIMP
JMSourceStream::Seek(LARGE_INTEGER dlibMove,
		       DWORD dwOrigin,
		       ULARGE_INTEGER *plibNewPos)
{
    PRINT("JMSourceStream::Seek\n");
    PRINT2(" - Offset = %d\n", dlibMove);
    CAutoLock cAutoLock(&m_cSharedState);
    long actualPos;

    if (!m_DontPush)
	switch (dwOrigin) {
	case STREAM_SEEK_SET:
	    if (m_astream->getSeekable()) {
		PRINT(" - Seeking\n");
		m_astream->seek(dlibMove.LowPart, &actualPos);
	    } else {
		m_seekCount++;
		actualPos = dlibMove.LowPart;
		/*
		  if (m_seekCount > 1)
		  return S_FALSE;
		  else
		  actualPos = 0;
		*/
	    }
	    //fseek(my_fp, dlibMove.LowPart, SEEK_SET);
	    break;
	case STREAM_SEEK_CUR:
	    PRINT("Cannot SEEK_CUR\n");
	    //fseek(my_fp, dlibMove.LowPart, SEEK_CUR);
	    break;
	case STREAM_SEEK_END:
	    PRINT("Cannot SEEK_END\n");
	    //fseek(my_fp, dlibMove.LowPart, SEEK_END);
	    break;
	}
    
    if (plibNewPos) {
	(*plibNewPos).LowPart = actualPos;
	(*plibNewPos).HighPart = 0;
    }
    
    m_readingHeader = 1;
    PRINT(" - After Seek \n");
    return S_OK;
}

STDMETHODIMP
JMSourceStream::SetSize(ULARGE_INTEGER libNewSize)
{
    PRINT("JMSourceStream::SetSize\n");
    return S_OK;
}

STDMETHODIMP
JMSourceStream::CopyTo(IStream *pstrm,
			 ULARGE_INTEGER cb,
			 ULARGE_INTEGER *pcbRead,
			 ULARGE_INTEGER *pcbWritten)
{
    PRINT("JMSourceStream::CopyTo\n");
    return S_OK;
}

STDMETHODIMP
JMSourceStream::Commit(DWORD grfCommitFlags)
{
    PRINT("JMSourceStream::Commit\n");
    return S_OK;
}

STDMETHODIMP
JMSourceStream::Revert(void)
{
    PRINT("JMSourceStream::Revert\n");
    return S_OK;
}

STDMETHODIMP
JMSourceStream::LockRegion(ULARGE_INTEGER libOffset,
			     ULARGE_INTEGER cb,
			     DWORD dwLockType)
{
    PRINT("JMSourceStream::LockRegion\n");
    return S_OK;
}

STDMETHODIMP
JMSourceStream::UnlockRegion(ULARGE_INTEGER libOffset,
			       ULARGE_INTEGER cb,
			       DWORD dwLockType)
{
    PRINT("JMSourceStream::UnlockRegion\n");
    return S_OK;
}

STDMETHODIMP
JMSourceStream::Stat(STATSTG *pstatstg,
		       DWORD grfStatFlag)
{
    PRINT("JMSourceStream::Stat\n");
    CAutoLock lock(m_pFilter->pStateLock());
    CAutoLock cAutoLock(&m_cSharedState);

    if (m_mediaLength >= 0) {
	pstatstg->cbSize.LowPart = m_mediaLength & 0xFFFFFFFF;
	pstatstg->cbSize.HighPart = (m_mediaLength >> 32) & 0xFFFFFFFF;
    } else {
	pstatstg->cbSize.LowPart = 0;
	pstatstg->cbSize.HighPart = 0x7FFFFFFF;
    }

    return S_OK;
}



STDMETHODIMP
JMSourceStream::Clone(IStream **ppstm)
{
    PRINT("JMSourceStream::Clone\n");
    return S_OK;
}

void
JMSourceStream::stopDataFlow(int stop)
{
    PRINT("JMSourceStream::stopDataFlow\n");
    m_DontPush = stop;
}

//
// DoBufferProcessingLoop
//
// Grabs a buffer and calls the users processing function.
// Overridable, so that different delivery styles can be catered for.
HRESULT
JMSourceStream::DoBufferProcessingLoop(void) {

    Command com;

    m_DontPush = 0;
    PRINT("JMSourceStream::DoBufferProcessingLoop\n");
    OnThreadStartPlay();
    //printf("Entering Buffer Loop\n");
    do {
	//printf("%%%%%%%%%%%% Buffer Loop - outer loop\n");
	while (!CheckRequest(&com)) {

	    if (m_DontPush)
		continue;
	    
	    // printf("%%%%%%%%%%%% Buffer Loop - inner loop %d\n", com);
	    IMediaSample *pSample;
	    
	    HRESULT hr = GetDeliveryBuffer(&pSample,NULL,NULL,0);
	    if (FAILED(hr)) {
                Sleep(1);
		continue;	// go round again. Perhaps the error will go away
			    // or the allocator is decommited & we will be asked to
			    // exit soon.
	    }
	    
	    // Virtual function user will override.
	    hr = FillBuffer(pSample);
	    PRINT("After FillBuffer()\n");
	    if (m_DontPush) {
		pSample->Release();
	    } else if (hr == S_OK) {
		hr = Deliver(pSample);
                pSample->Release();

                // downstream filter returns S_FALSE if it wants us to
                // stop or an error if it's reporting an error.
                if (hr != S_OK /*&& hr != S_FALSE */) {
		    DbgLog((LOG_TRACE, 2, TEXT("Deliver() returned %08x; stopping"),
			    hr));
		    PRINT2("%%%%%% Buffer Loop - DownStream filter returned an error %d\n", hr);
		    
		    return S_OK;
                }
		
	    } else if (hr == S_FALSE) {
                // derived class wants us to stop pushing data
		pSample->Release();
		DeliverEndOfStream();
		return S_OK;
	    } else {
                // derived class encountered an error
                pSample->Release();
		DbgLog((LOG_ERROR, 1, TEXT("Error %08lX from FillBuffer!!!"), hr));
                DeliverEndOfStream();
                m_pFilter->NotifyEvent(EC_ERRORABORT, hr, 0);
                return hr;
	    }

            // all paths release the sample
	}

        // For all commands sent to us there must be a Reply call!
	// printf("DoBufferProcessingLoop() .... Request = %d\n", com);
	if (com == CMD_RUN || com == CMD_PAUSE) {
	    Reply(NOERROR);
	} else if (com != CMD_STOP) {
	    Reply(E_UNEXPECTED);
	    DbgLog((LOG_ERROR, 1, TEXT("Unexpected command!!!")));
	}
    } while (com != CMD_STOP);

    return S_FALSE;
}

/*************************************************************************
 * JMSourcePosition
 *************************************************************************/

JMSourcePosition::JMSourcePosition(TCHAR *pName,
				       LPUNKNOWN pUnk,
				       HRESULT *phr,
				       CCritSec *pCritSec)
    : CSourcePosition(pName, pUnk, phr, pCritSec)
{
    PRINT("JMSourcePosition::JMSourcePosition\n");
}

HRESULT
JMSourcePosition::ChangeStart()
{
    HRESULT hr;
    PRINT("JMSourcePosition::ChangeStart\n");
    CAutoLock lock(m_pLock);
    CAutoLock lock2(pStream->GetSharedState());
    
    //pStream->Run();

    /*    if ((pStream->GetFilter())->IsActive()) {*/
	hr = pStream->DeliverBeginFlush();
	if (FAILED(hr))
	    PRINT(" - DeliverBeginFlush() failed!!!!!\n");
	hr = pStream->DeliverEndFlush();
	if (FAILED(hr))
	    PRINT(" - DeliverEndFlush() failed!!!!!\n");
	/*    }*/

    return ((JMSourceStream*)pStream)->ChangePosition((long)(double)m_Start);
}

STDMETHODIMP
JMSourcePosition::put_CurrentPosition(REFTIME time)
{
    PRINT("JMSourcePosition::put_CurrentPosition\n");
    return CSourcePosition::put_CurrentPosition(time);
}

STDMETHODIMP
JMSourcePosition::get_Duration(REFTIME * plength)
{
    CSourcePosition::get_Duration(plength);
    PRINT2("JMSourcePosition::Duration is %d\n", *plength);
    return S_OK;
}

