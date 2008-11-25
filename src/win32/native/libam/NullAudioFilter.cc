/*
 * @(#)NullAudioFilter.cc	1.2 98/09/02
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

#include <stdio.h>
#include <streams.h>
#include <initguid.h>
#include "NullAudioFilter.h"

// Setup data

AMOVIESETUP_MEDIATYPE sudIpPinTypes =
{
    &MEDIATYPE_Audio,             // MajorType
    &MEDIASUBTYPE_NULL            // MinorType
};

AMOVIESETUP_PIN sudIpPin =
{
    L"Input",                     // The Pins name
    FALSE,                        // Is rendered
    FALSE,                        // Is an output pin
    FALSE,                        // Allowed none
    FALSE,                        // Allowed many
    &CLSID_NULL,                  // Connects to filter
    NULL,                         // Connects to pin
    1,                            // Number of types
    &sudIpPinTypes                // Pin details
};

AMOVIESETUP_FILTER sudNullAudioAx =
{
    &CLSID_NullAudio,             // Filter CLSID
    L"Null Audio",                // String name
    MERIT_UNLIKELY,               // Filter merit
    1,                            // Number of pins
    &sudIpPin                     // Pin details
};


//
// Constructor
//
// Constructor for the text out renderer filter. After initialising the base
// renderer class and our nested window handling class we have to pass our
// input pin we have to the window class. The base class uses this to check
// that the filter has a valid pin connection before allowing IVideoWindow
// methods to be called (this is a stipulation of the interface set mainly
// because most filters can't do anything before they know what data they
// will be dealing with - an example being video renderers who can't really
// support IVideoWindow fully until they know the size/format of the video)
//
#pragma warning(disable:4355)
//

CNullAudioFilter::CNullAudioFilter(LPUNKNOWN pUnk,HRESULT *phr) :
    CBaseRenderer(CLSID_NullAudio, NAME("NullAudio Filter"), pUnk, phr)
    
{

} // (Constructor)


//
// Destructor
//
CNullAudioFilter::~CNullAudioFilter()
{
}

 
//
// CreateInstance
//
// This goes in the factory template table to create new instances
//
CUnknown * WINAPI CNullAudioFilter::CreateInstance(LPUNKNOWN pUnk, HRESULT *phr)
{
    CNullAudioFilter *pNullAudioFilter = new CNullAudioFilter(pUnk,phr);

    if (pNullAudioFilter == NULL) {
        return NULL;
    }
    
    return (CBaseMediaFilter *) pNullAudioFilter;

} // CreateInstance


//
// NonDelegatingQueryInterface
//
// Overriden to say what interfaces we support and where
//
STDMETHODIMP
CNullAudioFilter::NonDelegatingQueryInterface(REFIID riid,void **ppv)
{
    CheckPointer(ppv,E_POINTER);
    return CBaseRenderer::NonDelegatingQueryInterface(riid,ppv);

} // NonDelegatingQueryInterface


//
// Pause
//
// Overriden to show the text renderer window
//
STDMETHODIMP CNullAudioFilter::Pause()
{
    BOOL fStopToPause = (m_State == State_Stopped);

    HRESULT hr = CBaseRenderer::Pause();
    if(FAILED(hr)) {
        return hr;
    }

    return hr;

} // Pause


LPAMOVIESETUP_FILTER CNullAudioFilter::GetSetupData()
{
    return &sudNullAudioAx;
}

//
// CheckMediaType
//
// Check that we can support a given proposed type
//
HRESULT CNullAudioFilter::CheckMediaType(const CMediaType *pmt)
{
    // Reject non-Audio type
    if (pmt->majortype != MEDIATYPE_Audio) {
	return E_INVALIDARG;
    }
    return NOERROR;

} // CheckMediaType


//
// DoRenderSample
//
// This is called when a sample is ready for rendering
//
HRESULT CNullAudioFilter::DoRenderSample(IMediaSample *pMediaSample)
{
    ASSERT(pMediaSample);
    //printf("In CNullAudioFilter::DoRenderSample\n");

    return NOERROR;

} // DoRenderSample

