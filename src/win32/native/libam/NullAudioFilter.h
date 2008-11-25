/*
 * @(#)NullAudioFilter.h	1.1 98/09/02
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

DEFINE_GUID(CLSID_NullAudio,
0x737e8ff0, 0x41fb, 0x11d2, 0xa4, 0xad, 0x0, 0x80, 0x5f, 0x31, 0x07, 0xc0);

class CNullAudioFilter : public CBaseRenderer
{
public:

    static CUnknown * WINAPI CreateInstance(LPUNKNOWN pUnk, HRESULT *phr);

    CNullAudioFilter(LPUNKNOWN pUnk,HRESULT *phr);
    ~CNullAudioFilter();
    STDMETHODIMP NonDelegatingQueryInterface(REFIID, void **);

    LPAMOVIESETUP_FILTER GetSetupData();
    STDMETHODIMP Pause();
    HRESULT CheckMediaType(const CMediaType *pmt);
    HRESULT DoRenderSample(IMediaSample *pMediaSample);

}; // CNullAudioFilter

