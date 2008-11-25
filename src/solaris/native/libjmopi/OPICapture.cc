/*
 * @(#)OPICapture.cc	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "opi.h"
#include "OPICapture.h"

#ifdef DEBUG
#define PRINT(s) printf(s)
#else
#define PRINT(s)
#endif


// THIS IS THE H263 RTP PACKET STRUCTURE
typedef struct {
    unsigned long int offset  : 20;
    unsigned long int m       :  4;
    unsigned long int tempref :  8;
    unsigned long int vmvd    :  5;
    unsigned long int hmvd    :  5;
    unsigned long int quant   :  5;
    unsigned long int mbap    :  5;
    unsigned long int gobn    :  4;
    unsigned long int v       :  1;
    unsigned long int i       :  1;
    unsigned long int ebit    :  3;
    unsigned long int sbit    :  3;
} H263PacketInfoFrameType;


/*************************************************************************
 * Private Methods
 *************************************************************************/

 
void
OPICapture::printFormat(char *fnm, OPIFormatVideo *fmt)
{
#ifdef DEBUG
    fprintf(stderr, "%s.formatType = \'%d\'\n", fnm, fmt->formatType);
    if (fmt->compression)
	fprintf(stderr, "%s.compression = \'%s\'\n", fnm, fmt->compression);
    else
	fprintf(stderr, "%s.compression = <NULL>\n", fnm);
    if (fmt->param)
	fprintf(stderr, "%s.param = 0x%x \n", fnm, fmt->param);
    else
	fprintf(stderr, "%s.param = <NULL>\n", fnm);
    if (fmt->deviceName)
	fprintf(stderr, "%s.deviceName = \'%s\'\n", fnm, fmt->deviceName);
    else
	fprintf(stderr, "%s.deviceName = <NULL>\n", fnm);
    fprintf(stderr, "%s.externalInput = \'%d\'\n", fnm, fmt->externalInput);
    fprintf(stderr, "%s.externalOutput = \'%d\'\n", fnm, fmt->externalOutput);
    fprintf(stderr, "%s.width = \'%d\'\n", fnm, fmt->width);
    fprintf(stderr, "%s.height = \'%d\'\n", fnm, fmt->height);
    fprintf(stderr, "%s.depth = \'%d\'\n", fnm, fmt->depth);
#endif

}


void
OPICapture::setDeviceAttribute(char* name, int value)
{
    /*    Debug Message*/
    PRINT("In setDeviceAttribute\n");

    OPIAttribute val_set;
    val_set.i = value;

    if (opiDevice) {
	OPIDeviceInstance* di = opiDevice->getInstanceByIndex(devnum);
	di->setAttribute(name, val_set);
    }

}

void
OPICapture::setDeviceQuality()
{
    /*    Debug Message*/
    PRINT("In setDeviceQuality\n");

    if (compressDevice) {
	// quality ranges from 0 to 100, adjust to the OPI range of 1 to 31
	int qual = ( quality * 31 + 50 ) / 100;

	if (qual < 1) qual = 1;	/* force to minimum quality */
	if (qual > 31) qual = 31;	/* limit to maximum quality */

	// For Jpeg, 1 is low quality, 31 is high quality
	// For H261 & H263, 1 is high quality, 31 is low quality
	if ((do_compress == H261) || (do_compress == H263)) {
	    qual = 32 - qual;	// invert the value
	}
	compressDevice->setQuality(qual);
#ifdef DEBUG
	fprintf(stderr, "OPI Capture set quality to %d.\n", qual);
#endif
    }

}

void
OPICapture::setDeviceBitRate()
{
    /*    Debug Message*/
    PRINT("In setDeviceBitRate\n");

    if (compressDevice) {
	compressDevice->setBitRate(bitrate);

#ifdef DEBUG
	fprintf(stderr, "OPICapture bit rate %d\n", bitrate);
#endif
    }

}

void
OPICapture::setDeviceFrameRate()
{
    /*    Debug Message*/
    PRINT("In setDeviceFrameRate\n");

    if (captureDevice) {
	// frame rate is measured in msec per frame
	captureDevice->setFrameRate(1000/fps);
	captureDevice->setOverlayRate(1000/fps);   // Set rate for RGB, YUV
#ifdef DEBUG
	fprintf(stderr, "OPICapture frame rate %d overlay rate %d\n",
				captureDevice->getFrameRate(),
				captureDevice->getOverlayRate());
#endif
    }

}

void
OPICapture::setOutSize()
{
    /*    Debug Message*/
    PRINT("In setOutSize\n");

    int iw = inWidth;
    int ih = inHeight;
    if ((do_compress == H261) || (do_compress == H263)) {
	iw = 704;
	ih = 576;
    }

    if (scale != 1) {
	outWidth = (int) (((float) iw / scale) + 0.5);
	outHeight = (int) (((float) ih / scale) + 0.5);
    } else {
	outWidth = iw;
	outHeight = ih;
    }

}

void
OPICapture::freePipeline()
{

    /*    Debug Message */
    PRINT("In freePipeline\n");
    
    if (pipeline) {
	if (started) {
	    pipeline->stop();
	    started = 0;
	}
	delete pipeline;
	pipeline = NULL;
	captureDevice = NULL;
	compressDevice = NULL;
#ifdef CONSTRUCT_STREAM
	if (opiStream) {
	    delete opiStream;
	    opiStream = NULL;
	}
#endif
	opiStream = NULL;
    }
}

void
OPICapture::freeOPIState()
{

    /*    Debug Message */
    PRINT("In freeOPIState\n");
    
    freePipeline();
    if (opiSys) {
	delete opiSys;
	opiSys = NULL;
	opiDevice = NULL;
    }
}

/*************************************************************************
 * Public methods
 *************************************************************************/

/*
 * Class:	OPICapture
 * Method:	OPICapture
 */
OPICapture::OPICapture(int dev, int p)
{
    /* Debug message   */
    PRINT("In opiCapture constructor\n");

    /* Copy the parameters */
    devnum = dev;
    port = p;
    opiSys = NULL;
    pipeline = NULL;
    opiStream = NULL;
    opiDevice = NULL;
    captureDevice = NULL;
    compressDevice = NULL;
    scale = 2;
    quality = 50;
    do_compress = YUV;
    compression = "YUV422";
    signal = "NTSC";
    fps = 30;
    bitrate = 2000;
    inWidth = 640;
    inHeight = 480;
    outWidth = 0;
    outHeight = 0;

    started = 0;

}

/*
 * Class:	OPICapture
 * Method:	~OPICapture
 */
OPICapture::~OPICapture()
{

    /*    Debug Message */
    PRINT("In ~OPICapture\n");
    
    freeOPIState();
}

/*
 * Class:	OPICapture
 * Method:	opiConnect
 */
boolean
OPICapture::opiConnect()
{
    /* Debug message   */
    PRINT("In opiConnect\n");

    opiSys = new OPISystem();
    // Currently the only effect of setting debug level is to
    // disable detection of streaming timeouts if passed a
    // non-zero value.
    opiSys->setDebugLevel( 1);

    if (opiSys == NULL) {
	fprintf(stderr, "OPI Capture unable to open opi library\n");
	freeOPIState();
	return FALSE;
    }

    /*    Debug Message */
    PRINT("OPICapture new OPISystem() succeeded \n");

    opiDevice = opiSys->getDeviceByName("o1k");
    if (opiDevice == NULL) {
	fprintf(stderr, "OPI Capture unable to obtain opi device\n");
	freeOPIState();
	return FALSE;
    }

    int num_instances = opiDevice->instances.getNumItems();
    if (devnum > num_instances -1) {
	fprintf(stderr, "OPI Capture device %d not installed\n", devnum);
	freeOPIState();
	return FALSE;
    }

    /*    Debug Message */
    PRINT("OPICapture getDeviceByName() succeeded \n");

    setDeviceAttribute("Video Port", port);

    return TRUE;
}

/*
 * Class:	OPICapture
 * Method:	opiSetPort
 */
boolean
OPICapture::opiSetPort(int p)
{
    /*    Debug Message*/
    PRINT("In opiSetPort\n");

    port = p;
    setDeviceAttribute("Video Port", port);

    return TRUE;
}

/*
 * Class:	OPICapture
 * Method:	opiSetScale
 */
boolean
OPICapture::opiSetScale(int sc)
{
    /*    Debug Message*/
    PRINT("In opiSetScale\n");

    /* Don't allow full size for some compressions */
    if (sc == 1 && (do_compress == JPEG || do_compress == H261 ||
				do_compress == H263 || do_compress == MPEG))
	return FALSE;

    /* For now, only support 1 (full), 2 (1/2), 4 (1/4) */
    if (sc == 1 || sc == 2 || sc == 4) {
	scale = sc;
	setOutSize();
    } else {
	return FALSE;
    }

    return TRUE;

}

/*
 * Class:	OPICapture
 * Method:	opiSetFrameRate
 */
boolean
OPICapture::opiSetFrameRate(int f)
{
    /*    Debug Message*/
    PRINT("In opiSetFrameRate\n");

    /* TODO - may want to do a sanity check */
    fps = f;
    setDeviceFrameRate();

    return TRUE;
}

/*
 * Class:	OPICapture
 * Method:	opiSetBitRate
 */
boolean
OPICapture::opiSetBitRate(int br)
{
    /*    Debug Message*/
    PRINT("In opiSetBitRate\n");

    /* TODO - may want to do a sanity check */
    bitrate = br;
    setDeviceBitRate();

    return TRUE;
}

/*
 * Class:	OPICapture
 * Method:	opiSetQuality
 */
boolean
OPICapture::opiSetQuality(int q)
{
    /*    Debug Message*/
    PRINT("In opiSetQuality\n");

    quality = q;
    setDeviceQuality();

    return TRUE;
}

/*
 * Class:	OPICapture
 * Method:	opiSetCompress
 */
boolean
OPICapture::opiSetCompress(const char* compress)
{
    /*    Debug Message*/
    PRINT("In opiSetCompress\n");

    if (strcasecmp(compress, "rgb") == 0) {
	do_compress = RGB;
	compression = "RGB565";
    } else if (strcasecmp(compress, "yuv") == 0) {
	do_compress = YUV;
	compression = "YUV422";
    } else if (strcasecmp(compress, "h261") == 0) {
	do_compress = H261;
	compression = "IP64";
	if (scale == 1)
	    scale = 2;		// Full size not supported
    } else if (strcasecmp(compress, "h263") == 0) {
	do_compress = H263;
#ifdef USE_H263P
	compression = "H263P";
#else
	compression = "H263";
#endif
	if (scale == 1)
	    scale = 2;		// Full size not supported
    } else if (strcasecmp(compress, "jpeg") == 0) {
	do_compress = JPEG;
	compression = "JPEG";
	if (scale == 1)
	    scale = 2;		// Full size not supported
    } else if ((strcasecmp(compress, "mpeg") == 0) ||
		(strcasecmp(compress, "mpeg1") == 0)) {
	do_compress = MPEG;
	compression = "MPEG";
	if (scale == 1)
	    scale = 2;		// Full size not supported
    } else {
	/*    Debug Message */
	PRINT("OPI Capture invalid compress format specified %s ");
	PRINT(compress);
	PRINT("\n");
    }
    setOutSize();

    return TRUE;
}

/*
 * Class:	OPICapture
 * Method:	opiSetSignal
 */
boolean
OPICapture::opiSetSignal(const char* sig)
{
    /*    Debug Message*/
    PRINT("In opiSetSignal\n");

    if (strcasecmp(sig, "ntsc") == 0) {
	signal = "NTSC";
	inWidth = 640;
	inHeight = 480;
    } else if (strcasecmp(sig, "pal") == 0) {
	signal = "PAL";
	inWidth = 768;
	inHeight = 576;
    } else {
	/*    Debug Message */
	PRINT("OPI Capture invalid signal specified %s ");
	PRINT(sig);
	PRINT("\n");
    }
    setOutSize();

    return TRUE;
}

/*
 * Class:	OPICapture
 * Method:	opiStart
 */
boolean
OPICapture::opiStart()
{
    /*    Debug Message*/
    PRINT("In opiStart\n");

    /*
     * Get the device instance to capture.
     */
    OPIDeviceInstance* di = opiDevice->getInstanceByIndex(devnum);
    char* selected_device = di->getName();

    /*
     * Start constructing the pipeline.
     */
    if ((pipeline = new OPIPipeline(opiSys)) == NULL) {
	/*    Debug Message */
	PRINT("OPICapture opiStart() new OPIPipeline() failed \n");
	return FALSE;
    }

    // Setting up the external format variables for the first pipeline stage.
    OPIFormatVideo externalFmt;
    externalFmt.compression   = signal;
    externalFmt.deviceName    = selected_device;

    printFormat("externalFmt", &externalFmt);

    // Setting up uncompressed format variables for the first pipeline stage.
    OPIFormatVideo uncompressedFmt;
    uncompressedFmt.width       = outWidth;
    uncompressedFmt.height      = outHeight;
    uncompressedFmt.depth       = 16;
    if (do_compress == RGB)
	uncompressedFmt.compression = "RGB565";
    else
	uncompressedFmt.compression = "YUV422";
    uncompressedFmt.deviceName  = externalFmt.deviceName;

    printFormat("uncompressedFmt", &uncompressedFmt);

    // Creating the first pipeline stage which converts the
    // captured image into RGB565.
    OPIStage* sh;
    sh = pipeline->firstStage(CAP_VID_CAPTURE,
					&externalFmt, &uncompressedFmt);

    if (!sh) {
	/*    Debug Message */
	PRINT("OPICapture opiStart() firstStage() failed \n");
	freePipeline();
	return FALSE;
    }

    OPIStage* videoCompressionStage;
    if ((do_compress != RGB) && (do_compress != YUV)) {
	// Set up the compression format variables for the compression stage.
	OPIFormatVideo compressedFmt;
	compressedFmt.compression = compression;
	compressedFmt.width       = outWidth;
	compressedFmt.height      = outHeight;
	compressedFmt.depth       = 16;
	compressedFmt.deviceName  = externalFmt.deviceName;

	printFormat("compressedFmt", &compressedFmt);

	// Creating the compression stage of the pipeline.
	videoCompressionStage = pipeline->addStage(sh,
					CAP_VID_COMPRESSION,
					&uncompressedFmt, &compressedFmt);
	if (!videoCompressionStage) {
	    /*    Debug Message */
	    PRINT("OPICapture opiStart() addStage() failed \n");
	    freePipeline();
	    return FALSE;
	}
    }

    // Assigning the actual hardware and software to
    // each of the required actions. If it returns false
    // then delete the pipeline and window.
    if (!pipeline->mapToDevices()) {
	fprintf(stderr, "OPI Capture could not map to pipeline.\n");
	freePipeline();
	return FALSE;
    }


    // Getting the specific capture device and
    // setting the framerate and the overlay rate.
    captureDevice = (OPICaptureDeviceVideo *)
				pipeline->getDeviceFromHandle(sh);
    setDeviceFrameRate();

    if ((do_compress != RGB) && (do_compress != YUV)) {
	// Getting the specific compression device and setting some of the
	// capabilities of the compression device.
	compressDevice = (OPICompressionDeviceVideo *)
		pipeline->getDeviceFromHandle(videoCompressionStage);
	setDeviceBitRate();
	setDeviceQuality();
    }

#ifdef CONSTRUCT_STREAM
    opiStream = new OPIStream(OUTSTREAM, 3, outWidth * outHeight * 3);
    pipeline->setOutStream(opiStream);
#endif

    // Finish the construction of the pipeline.
    if (!pipeline->finishConstruction()) {
	fprintf(stderr, "OPI Capture could not finish pipeline.\n");
	freePipeline();
	return FALSE;
    }

       
#ifndef CONSTRUCT_STREAM
    opiStream = pipeline->getOutStream();
#endif

    // Start the pipeline.
    pipeline->start();

    started = 1;
    return TRUE;

}

/*
 * Class:	OPICapture
 * Method:	opiRead
 */
int
OPICapture::opiRead(void* buf, int len)
{
    OPIBuffer* opiBuf;
    void* data;
    int clen = -1;

    if (!started) {
	/*    Debug Message */
	PRINT("OPICapture opiRead() not started \n");
	return -1;
    }

    /*    Debug Message*/
    /*	PRINT("In opiRead\n");	*/

    if ((opiBuf = opiStream->getData()) != NULL) {
	data = (void *)opiBuf->data;
	clen = opiBuf->fsize;
#ifdef USE_H263P
	// If H263 using compression "H263P", drop off the RTP header at the end
	if (do_compress == H263)
	    clen -= sizeof(H263PacketInfoFrameType);
#endif
	if (clen > len) {
	    /*    Debug Message */
	    PRINT("opiRead: buffer too short\n");
	    clen = -1;
	} else {
	    // Catch a special case -- a bug in the OPI logic
	    // It breaks the h263 decoders.
	    if (clen == 1) {
		PRINT("OPICapture opiRead length = 1\n");
		clen = 0;	// act like there was no buffer
	    } else if ((do_compress == JPEG) && clen < 100) {
		PRINT("OPICapture opiRead short Jpeg frame\n");
		clen = 0;	// act like there was no buffer
	    } else {
		/*************************************************
		* NOTE: not using any RTP formats today. If they
		* are added, OPI puts the RTP header at the end
		* of the buffer and JMF decoders expect it at
		* the front so this would have to become two
		* memcpy()s to re-order them.
		**************************************************/
		memcpy(buf, data, clen);
	    }
	}
	opiStream->returnBuffer(opiBuf);
    } else {
	return 0;
    }

    return clen;

}

/*
 * Class:	OPICapture
 * Method:	opiGetWidth
 */
int
OPICapture::opiGetWidth()
{
    /*    Debug Message*/
    PRINT("In opiGetWidth\n");

    return outWidth;

}

/*
 * Class:	OPICapture
 * Method:	opiGetHeight
 */
int
OPICapture::opiGetHeight()
{
    /*    Debug Message*/
    PRINT("In opiGetHeight\n");

    return outHeight;

}

/*
 * Class:	OPICapture
 * Method:	opiGetLineStride
 */
int
OPICapture::opiGetLineStride()
{
    int w;

    /*    Debug Message*/
    PRINT("In opiGetLineStride\n");

    if ((do_compress != RGB) && (do_compress != YUV)) {
	/*    Debug Message */
	PRINT("OPICapture opiLineStride() not valid for compressed streams \n");
    }

    return (int) outWidth * 2;

}

/*
 * Class:	OPICapture
 * Method:	opiStop
 */
boolean
OPICapture::opiStop()
{
    /*    Debug Message*/
    PRINT("In opiStop\n");
    freePipeline();
    started = 0;

    return TRUE;
}

/*
 * Class:	OPICapture
 * Method:	opiDisconnect
 */
boolean
OPICapture::opiDisconnect()
{
    /*    Debug Message */
    PRINT("In opiDisconnect\n");
    
    freeOPIState();
    return TRUE;
}

