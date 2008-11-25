/*
 * @(#)filter2stream.h	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <jni.h>

class CFilter2StreamInputPin;
class CFilter2Stream;
class CFilter2StreamFilter;

// declare another interface to be supported by filter2stream

class IJMFStream : public IUnknown 
{

 public:
    STDMETHOD_(void, setBuffer(signed char *buf, int bufLength)) PURE;
    STDMETHOD_(void, setStopped(BOOL isStopped)) PURE;
    STDMETHOD_(BOOL, isCopied()) PURE;

};


// Main filter object

class CFilter2StreamFilter : public CBaseFilter
{
    CFilter2Stream * const m_pFilter2Stream;

 public:

    // Constructor
    CFilter2StreamFilter(CFilter2Stream *pFilter2Stream,
			 LPUNKNOWN pUnk,
			 CCritSec *pLock,
			 HRESULT *phr);

    // Pin enumeration
    CBasePin * GetPin(int n);
    int GetPinCount();

};


//  Pin object

class CFilter2StreamInputPin : public CRenderedInputPin
{
    CFilter2Stream    * const m_pFilter2Stream;           // Main renderer object
    CCritSec * const m_pReceiveLock;    // Sample critical section
    REFERENCE_TIME m_tLast;             // Last sample receive time

 public:

    CFilter2StreamInputPin(CFilter2Stream *pFilter2Stream,
			   LPUNKNOWN pUnk,
			   CBaseFilter *pFilter,
			   CCritSec *pLock,
			   CCritSec *pReceiveLock,
			   HRESULT *phr);

    // Do something with this media sample
    STDMETHODIMP Receive(IMediaSample *pSample);
    STDMETHODIMP EndOfStream(void);
    STDMETHODIMP ReceiveCanBlock();

    // Check if the pin can support this specific proposed type and format
    HRESULT CheckMediaType(const CMediaType *);

    // Break connection
    HRESULT BreakConnect();

    // Track NewSegment
    STDMETHODIMP NewSegment(REFERENCE_TIME tStart,
                            REFERENCE_TIME tStop,
                            double dRate);
};


//  CFilter2Stream object which has filter and pin members

class CFilter2Stream : public CUnknown, public IJMFStream
{
    friend class CFilter2StreamFilter;
    friend class CFilter2StreamInputPin;

    CFilter2StreamFilter *m_pFilter;         // Methods for filter interfaces
    CFilter2StreamInputPin *m_pPin;          // A simple rendered input pin
    CCritSec m_Lock;                // Main renderer critical section
    CCritSec m_ReceiveLock;         // Sublock for received samples
    CPosPassThru *m_pPosition;      // Renderer position controls

 public:

    DECLARE_IUNKNOWN

	CFilter2Stream(LPUNKNOWN pUnk, HRESULT *phr);
    ~CFilter2Stream();

    static CUnknown * WINAPI CreateInstance(LPUNKNOWN punk, HRESULT *phr);

    STDMETHOD_(void, setBuffer(signed char *buf, int bufLength));
    STDMETHOD_(void, setStopped(BOOL isStopped));
    STDMETHOD_(BOOL, isCopied());

 private:

    // Overriden to say what interfaces we support where
    STDMETHODIMP NonDelegatingQueryInterface(REFIID riid, void ** ppv);

};

