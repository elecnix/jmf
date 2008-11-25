/*
 * @(#)JMSourceFilter.h	1.10 98/09/18
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

#include <limits.h>

class JMSourceStream;
class JMSourcePosition;
// class JMFilterPosition;

/////////////////////////////////////////////////////////////////////////////////
// This class implements the IFilter and IAMovieSetup interfaces.
// It is the source filter for the active movie graph

class JMSource : public CSource
{
  public:
    DECLARE_IUNKNOWN
    
    // Method to create an instance. Constructor is not used directly.

    static CUnknown * CreateInstance(LPUNKNOWN lpunk, HRESULT *phr);

    // Setup helper

    //LPAMOVIESETUP_FILTER GetSetupData();

    // Constructor...
    
    JMSource(LPUNKNOWN lpunk, HRESULT *phr, JStream *stream, int streamType);

    CSourceStream * getOutputPin();

    STDMETHODIMP NonDelegatingQueryInterface(REFIID id, void **ppv);
    STDMETHODIMP Run(REFERENCE_TIME tStart);
    STDMETHODIMP Pause(void);
    
};

/////////////////////////////////////////////////////////////////////////////////
// This class implements the IPin interface to feed the raw mpeg system stream

class JMSourceStream : public CSourceStream , public IStream
{
  public:
    DECLARE_IUNKNOWN
    // Constructor
    JMSourceStream(HRESULT *, JMSource *, LPCWSTR, JStream *, int streamType);

    // Destructor
    ~JMSourceStream();
    
    // CSourceStream methods
    HRESULT FillBuffer(IMediaSample *);
    virtual HRESULT GetMediaType(CMediaType *);
    virtual HRESULT CheckMediaType(const CMediaType *);
    HRESULT DecideBufferSize(IMemAllocator *pIMemAlloc,
			     ALLOCATOR_PROPERTIES *pProperties);
    STDMETHODIMP NonDelegatingQueryInterface(REFIID id, void **ppObject);
    STDMETHODIMP Run(void);

    // IStream Methods
    STDMETHODIMP Read(void *pv, ULONG cb, ULONG *pcbRead);
    STDMETHODIMP Write(const void *pv, ULONG cb, ULONG *pcbWritten);
    STDMETHODIMP Seek(LARGE_INTEGER dlibMove,
		      DWORD dwOrigin,
		      ULARGE_INTEGER *plibNewPos);
    STDMETHODIMP SetSize(ULARGE_INTEGER libNewSize);
    STDMETHODIMP CopyTo(IStream *pstm,
			ULARGE_INTEGER cb,
			ULARGE_INTEGER *pcbRead,
			ULARGE_INTEGER *pcbWritten);
    STDMETHODIMP Commit(DWORD grfCommitFlags);
    STDMETHODIMP Revert(void);
    STDMETHODIMP LockRegion(ULARGE_INTEGER libOffset,
			    ULARGE_INTEGER cb,
			    DWORD dwLockType);
    STDMETHODIMP UnlockRegion(ULARGE_INTEGER libOffset,
			      ULARGE_INTEGER cb,
			      DWORD dwLockType);
    STDMETHODIMP Stat(STATSTG *pstatstg,
		      DWORD grfStatFlag);
    STDMETHODIMP Clone(IStream **ppStream);
    
    HRESULT Active() { return CSourceStream::Active(); }
    HRESULT Inactive() { return CSourceStream::Inactive(); }

    CCritSec *GetSharedState() { return &m_cSharedState; }
    HRESULT DoBufferProcessingLoop(void);


    HRESULT ChangePosition(long position);		    // Local function
    HRESULT Restart(void);				    // Local function
    JMSource *GetFilter() { return (JMSource *)m_pFilter; }
    int     getStreamType() { return m_streamType; }
    void    stopDataFlow(int stop);

    int m_inactive;

  private:
    CCritSec m_cSharedState;
    CCritSec m_crit;
    JStream *m_astream;					    // Stream to read from
    __int64 m_mediaLength;					    // Length of stream
    int  m_readingHeader;				    // AM is reading header
    int  m_discontinuity;
    int  m_seekCount;
    int  m_streamType;
    int  m_firstRead;
    int  m_DontPush;
    JMSourcePosition *m_sourcePosition;			    // ISourcePosition
							    // int.
    int  m_totalHeaderRead;
    unsigned char m_peekBuffer[65536];
};

class JMSourcePosition: public CSourcePosition {
public:
    JMSourcePosition(TCHAR *pName,
		       LPUNKNOWN pUnk,
		       HRESULT * phr,
		       CCritSec * pCritSec);
    void setSourceStream(JMSourceStream *stream) {
	pStream = stream;
    }
    
    DECLARE_IUNKNOWN

    STDMETHODIMP put_CurrentPosition(REFTIME llTime);
    HRESULT ChangeRate()  { printf("In ChangeRate()\n");  return S_OK; }
    HRESULT ChangeStart();
    HRESULT ChangeStop()  { printf("In ChangeStop()\n");  return S_OK; }
    STDMETHODIMP get_Duration(REFTIME * plength);
    JMSourceStream* pStream;
};

/*
class JMFilterPosition: public CSourcePosition {
public:
    JMFilterPosition(TCHAR *pName,
		     LPUNKNOWN pUnk,
		     HRESULT * phr,
		     CCritSec * pCritSec)
	: CSourcePosition(pName, pUnk, phr, pCritSec)
    {
	// Set the duration
	m_Duration = CRefTime((LONGLONG)_I64_MAX);

    }
    
    DECLARE_IUNKNOWN

    HRESULT ChangeRate()  { printf("In JMFP::ChangeRate()\n");  return S_OK; }
    HRESULT ChangeStart() { printf("In JMFP::ChangeStart()\n"); return S_OK; }
    HRESULT ChangeStop()  { printf("In JMFP::ChangeStop()\n");  return S_OK; }
    
    STDMETHODIMP get_Duration(REFTIME *plength) {
	printf("<<<<<<<< Getting duration\n");
	return CSourcePosition::get_Duration(plength);
    }
    
};
*/

#define MPEGSYSTEM 3
#define MPEGVIDEO 2
#define MPEGAUDIO 1
