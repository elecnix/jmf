/*
 * @(#)filter2stream.cc	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>

#include <windows.h>
#include <commdlg.h>
#include <streams.h>
#include <initguid.h>
#include "filter2stream.h"
#include "filter2streamuids.h"

signed char *pBuffer = NULL;;
int bufferLength = 0;
BOOL copied = FALSE;
BOOL stopped = TRUE;

// Setup data

const AMOVIESETUP_MEDIATYPE sudPinTypes =
{
    &MEDIATYPE_NULL,            // Major type
    &MEDIASUBTYPE_NULL          // Minor type
};

const AMOVIESETUP_PIN sudPins =
{
    L"Input",                   // Pin string name
    FALSE,                      // Is it rendered
    FALSE,                      // Is it an output
    FALSE,                      // Allowed none
    FALSE,                      // Likewise many
    &CLSID_NULL,                // Connects to filter
    L"Output",                  // Connects to pin
    1,                          // Number of types
    &sudPinTypes                // Pin information
};

const AMOVIESETUP_FILTER sudFilter2Stream =
{
    &CLSID_Filter2Stream,                // Filter CLSID
    L"Filter2Stream",                    // String name
    MERIT_DO_NOT_USE,           // Filter merit
    1,                          // Number pins
    &sudPins                    // Pin details
};


//
//  Object creation stuff
//
CFactoryTemplate g_Templates[]= {
    L"Filter2Stream", &CLSID_Filter2Stream, CFilter2Stream::CreateInstance, 
    NULL, &sudFilter2Stream
};
int g_cTemplates = 1;


// Constructor

CFilter2StreamFilter::CFilter2StreamFilter(CFilter2Stream *pFilter2Stream,
					   LPUNKNOWN pUnk,
					   CCritSec *pLock,
					   HRESULT *phr) :
    CBaseFilter(NAME("CFilter2StreamFilter"), pUnk, pLock, CLSID_Filter2Stream),
    m_pFilter2Stream(pFilter2Stream)
{
}


//
// GetPin
//
CBasePin * CFilter2StreamFilter::GetPin(int n)
{
    if (n == 0) {
        return m_pFilter2Stream->m_pPin;
    } else {
        return NULL;
    }
}


//
// GetPinCount
//
int CFilter2StreamFilter::GetPinCount()
{
    return 1;
}



//
//  Definition of CFilter2StreamInputPin
//
CFilter2StreamInputPin::CFilter2StreamInputPin(CFilter2Stream *pFilter2Stream,
					       LPUNKNOWN pUnk,
					       CBaseFilter *pFilter,
					       CCritSec *pLock,
					       CCritSec *pReceiveLock,
					       HRESULT *phr) :

    CRenderedInputPin(NAME("CFilter2StreamInputPin"),
		      pFilter,                   // Filter
		      pLock,                     // Locking
		      phr,                       // Return code
		      L"Input"),                 // Pin name
    m_pReceiveLock(pReceiveLock),
    m_pFilter2Stream(pFilter2Stream),
    m_tLast(0)
{
}


//
// CheckMediaType
//
// Check if the pin can support this specific proposed type and format
//
HRESULT CFilter2StreamInputPin::CheckMediaType(const CMediaType *)
{
    return S_OK;
}


//
// BreakConnect
//
// Break a connection
//
HRESULT CFilter2StreamInputPin::BreakConnect()
{
    if (m_pFilter2Stream->m_pPosition != NULL) {
        m_pFilter2Stream->m_pPosition->ForceRefresh();
    }
    return CRenderedInputPin::BreakConnect();
}


//
// ReceiveCanBlock
//
// We don't hold up source threads on Receive
//
STDMETHODIMP CFilter2StreamInputPin::ReceiveCanBlock()
{
    // we are going to block in the Receive method
    return S_OK;  
}


//
// Receive
//
// Do something with this media sample
//
STDMETHODIMP CFilter2StreamInputPin::Receive(IMediaSample *pSample)
{
    CAutoLock lock(m_pReceiveLock);
    PBYTE pbData;
    //	int copySize;

    while (pBuffer == NULL) { // no buffer to copy to - sleep
	if (stopped)
	    return S_OK;
	Sleep(50);
    }

    int sampleLength = pSample->GetActualDataLength();

    //printf("the sample size is:%d\n", sampleLength);
	
    if (sampleLength != bufferLength) 
	printf("!!! Buffer passed by Processor doesn't fit\n");
		

    HRESULT hr = pSample->GetPointer(&pbData);
    if (FAILED(hr)) {
        return hr;
    }

    REFERENCE_TIME tStart, tStop;
    pSample->GetTime(&tStart, &tStop);
    
    m_tLast = tStart;

    // Copy the data
    memcpy(pBuffer, pbData, sampleLength);
		
    pBuffer = NULL;
    copied = TRUE;

    return S_OK;
}



//
// EndOfStream
//
STDMETHODIMP CFilter2StreamInputPin::EndOfStream(void)
{
    CAutoLock lock(m_pReceiveLock);
    return CRenderedInputPin::EndOfStream();

} // EndOfStream


//
// NewSegment
//
// Called when we are seeked
//
STDMETHODIMP CFilter2StreamInputPin::NewSegment(REFERENCE_TIME tStart,
						REFERENCE_TIME tStop,
						double dRate)
{
    m_tLast = 0;
    return S_OK;

} // NewSegment



//
//  CFilter2Stream class
//
CFilter2Stream::CFilter2Stream(LPUNKNOWN pUnk, HRESULT *phr) :
    CUnknown(NAME("CFilter2Stream"), pUnk),
    m_pFilter(NULL),
    m_pPin(NULL),
    m_pPosition(NULL)
{
    m_pFilter = new CFilter2StreamFilter(this, GetOwner(), &m_Lock, phr);
    if (m_pFilter == NULL) {
        *phr = E_OUTOFMEMORY;
        return;
    }

    m_pPin = new CFilter2StreamInputPin(this,GetOwner(),
					m_pFilter,
					&m_Lock,
					&m_ReceiveLock,
					phr);
    if (m_pPin == NULL) {
        *phr = E_OUTOFMEMORY;
        return;
    }
}


// Destructor

CFilter2Stream::~CFilter2Stream()
{
    delete m_pPin;
    delete m_pFilter;
    delete m_pPosition;

}


//
// CreateInstance
//
// Provide the way for COM to create a dump filter
//
CUnknown * WINAPI CFilter2Stream::CreateInstance(LPUNKNOWN punk, HRESULT *phr)
{
    CFilter2Stream *pNewObject = new CFilter2Stream(punk, phr);
    if (pNewObject == NULL) {
        *phr = E_OUTOFMEMORY;
    }
    return pNewObject;

} // CreateInstance


//
// NonDelegatingQueryInterface
//
// Override this to say what interfaces we support where
//
STDMETHODIMP CFilter2Stream::NonDelegatingQueryInterface(REFIID riid, void ** ppv)
{
    CheckPointer(ppv,E_POINTER);
    CAutoLock lock(&m_Lock);

    // Do we have this interface

    if (riid == IID_IBaseFilter || riid == IID_IMediaFilter || riid == IID_IPersist) {
	return m_pFilter->NonDelegatingQueryInterface(riid, ppv);
    } else if (riid == IID_IMediaPosition || riid == IID_IMediaSeeking) {
        if (m_pPosition == NULL) {

            HRESULT hr = S_OK;
            m_pPosition = new CPosPassThru(NAME("Filter2Stream Pass Through"),
                                           (IUnknown *) GetOwner(),
                                           (HRESULT *) &hr, m_pPin);
            if (m_pPosition == NULL) {
                return E_OUTOFMEMORY;
            }

            if (FAILED(hr)) {
                delete m_pPosition;
                m_pPosition = NULL;
                return hr;
            }
        }
        return m_pPosition->NonDelegatingQueryInterface(riid, ppv);
    } else if (riid == IID_IJMFStream) {
	return GetInterface((IJMFStream*)this, ppv);
    }
    else return CUnknown::NonDelegatingQueryInterface(riid, ppv);
    

} // NonDelegatingQueryInterface


//
// IJMFStream implementation
//

STDMETHODIMP_(void) CFilter2Stream::setBuffer(signed char *buf, int bufLength) {
	
    copied = FALSE;
    pBuffer = buf;
    bufferLength = bufLength;
}

STDMETHODIMP_(void) CFilter2Stream::setStopped(BOOL isStopped) {
	
    stopped = isStopped;
}

STDMETHODIMP_(BOOL) CFilter2Stream::isCopied() {
	
    return copied;
}


//
// DllRegisterSever
//
// Handle the registration of this filter
//
STDAPI DllRegisterServer()
{
    return AMovieDllRegisterServer2( TRUE );

} // DllRegisterServer


//
// DllUnregisterServer
//
STDAPI DllUnregisterServer()
{
    return AMovieDllRegisterServer2( FALSE );

} // DllUnregisterServer
