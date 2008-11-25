/*
 * @(#)OPICapture.h	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef opicapture_h
#define opicapture_h

#include "opi.h"

enum compressor_type {
    RGB, YUV, H261, H263, JPEG, MPEG
};

#define boolean int

class OPICapture {
    public:
	int		devnum;
	int		port;
	compressor_type do_compress;
	char*		compression;
	char*		signal;
	int		scale;
	int		fps;
	int		bitrate;
	int		quality;
	uint		inWidth;
	uint		inHeight;
	uint		outWidth;
	uint		outHeight;

			OPICapture(int devnum, int port);
			~OPICapture();
	boolean		opiConnect();
	boolean		opiSetPort(int port);
	boolean		opiSetScale(int scale);
	boolean		opiSetFrameRate(int fps);
	boolean		opiSetBitRate(int bitrate);
	boolean		opiSetQuality(int quality);
	boolean		opiSetCompress(const char* compress);
	boolean		opiSetSignal(const char* signal);
	boolean		opiStart();
	int		opiRead(void* buf, int len);
	int		opiGetWidth();
	int		opiGetHeight();
	int		opiGetLineStride();
	boolean		opiStop();
	boolean		opiDisconnect();

    private:

	OPISystem*	opiSys;
	OPIDevice*	opiDevice;
	OPIPipeline*	pipeline;
	OPIStream*	opiStream;
	OPICaptureDeviceVideo* captureDevice;
	OPICompressionDeviceVideo* compressDevice;

	int		started;

	void		printFormat(char *fnm, OPIFormatVideo *fmt);
	void		setOutSize();
	void		setDeviceAttribute(char* name, int value);
	void		setDeviceQuality();
	void		setDeviceBitRate();
	void		setDeviceFrameRate();
	void		freePipeline();
	void		freeOPIState();

};

#endif

