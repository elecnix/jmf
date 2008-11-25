/*
 * @(#)XILCapture.c	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
#define SUNXIL_WARNING_DISABLE 1
#include <malloc.h>
#include <sys/utsname.h>
#include <xil/xil.h>
#include "com_sun_media_protocol_sunvideo_XILCapture.h"
#include <jni-util.h>

#ifdef DEBUG
#define PRINT(s) printf(s)
#else
#define PRINT(s)
#endif

#define UNKNOWN_SIGNAL (0)

/* The following are in XILRenderer, we share a common state. */
extern XilSystemState * jmf_xil_state;
extern void createXilState();

static int formatUnspec = -1;

enum compressor_type {
    RAW, JPEG, MPEG, CELLB
};

typedef struct {

    XilSystemState xil_state;
    XilImage       rtvc_image;
    XilImage       scaled_image;
    XilImage       colored_image;
    XilCis	   xil_cis;

    int            started;
    int            firstRawRead;

    int            port;
    int            signal;
    enum compressor_type do_cis;
    char	  *cis_type;
    int            scale;
    int            skip;
    int            quality;
    uint	   inWidth;
    uint	   inHeight;
    uint	   outWidth;
    uint	   outHeight;
    uint	   inBands;
    int            inStrideX;
    float          sx;
    float          sy;

} InstanceState;


static void updateXilCis(InstanceState *inst);


/*************************************************************************
 * Local Methods
 *************************************************************************/

static int
tryPort(JNIEnv *env, jobject jxil, InstanceState *inst)
{
    XilDataType datatype;
    int i;

    inst->signal = 0;
    inst->inWidth = 0;
    inst->inHeight = 0;

    xil_set_device_attribute(inst->rtvc_image, "PORT_V", (void *)inst->port);
    /*    Debug Message */
    PRINT("XILCapture xil_set_device_attribute() completed \n");
    /*
     * On a busy system attempts to get the signal format fail so try
     * multiple times. In addition, some video sources (like VCRs)
     * have a poor signal unless actively transmitting so signal format
     * may report unknown (0).
     */
    for (i = 0; i < 10; i++) {
	if (xil_get_device_attribute(inst->rtvc_image, "FORMAT_V",
		(void **) &inst->signal) == XIL_SUCCESS)
	    break;
    }
    /*    Debug Message */
    if (inst->signal == 0) {
	/* signal format unknown, may not be a camera or video source */
	PRINT("XILCapture xil_get_device_attribute() == unknown signal \n");
    }
    xil_get_info(inst->rtvc_image, &inst->inWidth, &inst->inHeight,
		    &inst->inBands, &datatype);
    /*    Debug Message */
    PRINT("XILCapture xil_get_info() completed \n");

    return 1;
}

static int
formatRaw(JNIEnv *env, jobject jxil, InstanceState *inst, jobject jStream)
{
    int stride = inst->inStrideX;

    if (stride <= 0)
	stride = formatUnspec;
    CallVoidMethod(env, jStream, "setRGBFormat", "(IIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			stride);
    return 1;
}

static int
formatJpeg(JNIEnv *env, jobject jxil, InstanceState *inst, jobject jStream)
{
    CallVoidMethod(env, jStream, "setJpegFormat", "(IIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			inst->quality);
    return 1;
}

static int
formatMpeg(JNIEnv *env, jobject jxil, InstanceState *inst, jobject jStream)
{
    CallVoidMethod(env, jStream, "setMpegFormat", "(IIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			inst->quality);
    return 1;
}

static int
setFormat(JNIEnv *env, jobject jxil, InstanceState *inst)
{
    jboolean isInstance;
    jobject jStream;

    /*    Debug Message */
    PRINT("XILCapture in setFormat \n");

    jStream = GetObjectField(env, jxil, "stream",
				"Ljavax/media/protocol/SourceStream;");
    if (!jStream) {
	/*    Debug Message */
	PRINT("XILCapture setFormat failed to obtain stream \n");
	return 0;
    }
    isInstance = IsInstanceOf(env, jStream, 
			"com/sun/media/protocol/sunvideo/SunVideoSourceStream");
    if (!isInstance) {
	/*    Debug Message */
	PRINT("XILCapture setFormat stream not valid instance \n");
	return 0;
    }
    if (inst->do_cis == RAW) {
	return formatRaw(env, jxil, inst, jStream);
    } else if (inst->do_cis == JPEG) {
	return formatJpeg(env, jxil, inst, jStream);
    } else {
	return formatMpeg(env, jxil, inst, jStream);
    }
}

static int
readRaw(InstanceState *inst, void *buf, int len)
{
    XilImage image;
    XilMemoryStorage storage;
    int clen;

    /*    Debug Message */
    /*	PRINT("In readRaw\n");	*/
    
    if (inst == NULL) {
	return -1;
    }
    if (inst->scaled_image) {
	xil_scale(inst->rtvc_image, inst->scaled_image, "nearest",
			inst->sx, inst->sy);
	image = inst->scaled_image;
    } else {
	image = inst->rtvc_image;
    }
    xil_color_convert(image, inst->colored_image);
    xil_toss(image);
    xil_export(inst->colored_image);
    xil_get_memory_storage(inst->colored_image, &storage);
    inst->inStrideX = storage.byte.scanline_stride;
    clen = storage.byte.scanline_stride * inst->outHeight;
    if (clen > len) {
	/*    Debug Message */
	PRINT("readRaw: buffer too short\n");
	clen = -1;
    } else {
	memcpy(buf, (void *)storage.byte.data, clen);
    }
    xil_import(inst->colored_image, 0);
    xil_toss(inst->colored_image);

    return clen;
}

static int
readCompressed(InstanceState *inst, void *buf, int len)
{
    int clen, frames;
    void *data = NULL;

    /*    Debug Message */
    /*	PRINT("In readCompressed\n");	*/

    if (inst == NULL) {
	return -1;
    }
    if (inst->xil_cis == NULL) {
	return -1;
    }
    if (inst->scaled_image) {
	xil_scale(inst->rtvc_image, inst->scaled_image, "nearest",
			inst->sx, inst->sy);
	xil_compress(inst->scaled_image, inst->xil_cis);
	xil_cis_sync(inst->xil_cis);
	if (xil_cis_get_write_invalid(inst->xil_cis)) {
	    updateXilCis(inst);
	    return -1;
	}
	xil_toss(inst->rtvc_image);
	xil_toss(inst->scaled_image);
    } else {
	xil_compress(inst->rtvc_image, inst->xil_cis);
	xil_cis_sync(inst->xil_cis);
	if (xil_cis_get_write_invalid(inst->xil_cis)) {
	    updateXilCis(inst);
	    return -1;
	}
	xil_toss(inst->rtvc_image);
    }
    if (xil_cis_has_frame(inst->xil_cis)) {
	data = (void *)xil_cis_get_bits_ptr(inst->xil_cis, &clen, &frames);
    }
    if (data) {
	if (clen > len) {
	    /*    Debug Message */
	    PRINT("readCompressed: buffer too short\n");
	    clen = -1;
	} else {
	    memcpy(buf, data, clen);
	}
    } else {
	clen = -1;
    }
    return clen;

}



static void
updateXilImages(InstanceState *inst)
{

    /*    Debug Message */
    PRINT("In updateXilImages\n");
    
    if (!inst || !inst->started)
	return;

    if (inst->scaled_image) {
	xil_destroy(inst->scaled_image);
	inst->scaled_image = NULL;
    }
    if (inst->colored_image) {
	xil_destroy(inst->colored_image);
	inst->colored_image = NULL;
    }
    /*
     * At this point, rtvc_image is valid but nothing else is.
     * Construct the rest of the images needed.
     */
    if (inst->scale != 1) {
	inst->outWidth = (int) (((float) inst->inWidth / inst->scale) + 0.5);
	inst->outHeight = (int) (((float) inst->inHeight / inst->scale) + 0.5);
	inst->sx = 1.0 / (float) inst->scale;
	inst->sy = 1.0 / (float) inst->scale;
	inst->scaled_image = xil_create(inst->xil_state,
					inst->outWidth, inst->outHeight,
					inst->inBands, XIL_BYTE);
    } else {
	inst->outWidth = inst->inWidth;
	inst->outHeight = inst->inHeight;
    }
    if (inst->do_cis == RAW) {
	XilColorspace xil_cspace_ycc601;	/* YCC CCIR 601 colorspace */
	XilColorspace xil_cspace_rgb709;	/* RGB CCIR 709 colorspace */
	xil_cspace_ycc601 = xil_colorspace_get_by_name(inst->xil_state,
								"ycc601");
	xil_cspace_rgb709 = xil_colorspace_get_by_name(inst->xil_state,
								"rgb709");
	inst->colored_image = xil_create(inst->xil_state,
					inst->outWidth, inst->outHeight,
					inst->inBands, XIL_BYTE);
	xil_set_colorspace(inst->rtvc_image, xil_cspace_ycc601);
	if (inst->scaled_image)
	    xil_set_colorspace(inst->scaled_image, xil_cspace_ycc601);
	xil_set_colorspace(inst->colored_image, xil_cspace_rgb709);
    } else {
	updateXilCis(inst);
    }
}

static void
updateXilCis(InstanceState *inst)
{

    /*    Debug Message */
    PRINT("In updateXilCis\n");
    
    if (inst) {
	xil_cis_destroy(inst->xil_cis);
	inst->xil_cis = NULL;
	inst->xil_cis = xil_cis_create(inst->xil_state, inst->cis_type);
	if (inst->do_cis == MPEG) {
	    XilMpeg1Pattern mpattern;
	    char *pattern = "IIII";
	    mpattern.pattern = pattern;
	    mpattern.repeat_count = 0;
	    xil_cis_set_keep_frames(inst->xil_cis, 6);
	    xil_cis_set_max_frames(inst->xil_cis, 6);
	    xil_cis_set_attribute(inst->xil_cis,
				"COMPRESSOR_PATTERN", (void *)&mpattern);
	    xil_cis_set_attribute(inst->xil_cis,
				"COMPRESSOR_INSERT_VIDEO_SEQUENCE_END",
				(void *)TRUE);

	} else {
	    xil_cis_set_keep_frames(inst->xil_cis, 1);
	    xil_cis_set_max_frames(inst->xil_cis, 1);
	}
	if (inst->do_cis == JPEG) {
	    xil_cis_set_attribute(inst->xil_cis, "ENCODE_411_INTERLEAVED",
							(void *) TRUE);
	    xil_cis_set_attribute(inst->xil_cis, "IGNORE_HISTORY",
							(void *) TRUE);
	    xil_cis_set_attribute(inst->xil_cis, "COMPRESSED_DATA_FORMAT",
							(void *) INTERCHANGE);
	    if (inst->quality != 0) {
		xil_cis_set_attribute(inst->xil_cis,
				"COMPRESSION_QUALITY", (void *)inst->quality);
	    }
	}
    }
}

static void
freeXilCis(InstanceState *inst)
{

    /*    Debug Message */
    PRINT("In freeXilCis\n");
    
    if (inst) {
	if (inst->xil_cis) {
	    xil_cis_flush(inst->xil_cis);
	    xil_cis_destroy(inst->xil_cis);
	    inst->xil_cis = NULL;
	}
    }
}

static void
freeXilImages(InstanceState *inst)
{

    /*    Debug Message */
    PRINT("In freeXilImages\n");
    
    if (inst) {
	if (inst->scaled_image) {
	    xil_destroy(inst->scaled_image);
	    inst->scaled_image = NULL;
	}
	if (inst->colored_image) {
	    xil_destroy(inst->colored_image);
	    inst->colored_image = NULL;
	}
	freeXilCis(inst);
    }
}

static void
freeXilState(JNIEnv *env, jobject jxil, InstanceState *inst)
{

    /*    Debug Message */
    PRINT("In freeXilState\n");
    
    if (inst) {
	if (inst->rtvc_image) {
	    xil_destroy(inst->rtvc_image);
	    inst->rtvc_image = NULL;
	}
	freeXilImages(inst);
	if (inst->xil_state) {
	    /*	xil_close(inst->xil_state);	*/
	    inst->xil_state = NULL;
	}
	SetLongField(env, jxil, "peer", (int) 0);
	free(inst);
    }
}

/*************************************************************************
 * Java Native methods
 *************************************************************************/

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilInitialize
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilInitialize(JNIEnv *env,
							    jclass jxilclass)
{
    struct utsname osDetails;

    /*    Debug Message */
    PRINT("XILCapture in xilInitialize \n");

    /* get the OS details */
    uname(&osDetails);

    osDetails.release[3] = 0;
    
    if (strcmp(osDetails.release, "2.4") == 0 ||
	strcmp(osDetails.release, "5.4") == 0) {
	/*    Debug Message */
	PRINT("XILCapture in xilInitialize, failed, Solaris 2.4 \n");
	return (jboolean) 0;
    } else
	return (jboolean) 1;
}


/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	cacheFieldIDs
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_cacheFieldIDs(JNIEnv *env,
							    jclass jxilclass)
{
    jclass jFormatClass;
    jfieldID jFormatUnspecID;

    /*    Debug Message */
    PRINT("XILCapture in cacheFieldIDs \n");
    jFormatClass = (*env)->FindClass(env, "javax/media/Format");
    if (jFormatClass) {
	jFormatUnspecID = (*env)->GetStaticFieldID(env, jFormatClass,
					"NOT_SPECIFIED", "I");
	if (jFormatUnspecID) {
	    formatUnspec = (*env)->GetStaticIntField(env, jFormatClass,
					jFormatUnspecID);
	}
    }

    if((*env)->ExceptionOccurred(env)) {
	(*env)->ExceptionDescribe(env);
	(*env)->ExceptionClear(env);
    }
    return (jboolean) 1;
}


/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilLittleEndian
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilLittleEndian(JNIEnv *env,
						       jobject jxil)
{
    int one = 1;
    if (*((char *) &one) == 1)
	return (jboolean) 1;
    else
	return (jboolean) 0;
}


/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilConnect
 * Signature:	(II)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilConnect(JNIEnv *env,
						      jobject jxil,
						      jint devnum,
						      jint port)
{
    XilDevice device;
    char *devname = NULL;
    char devnamestr[256];

    /* Allocate a new structure */
    InstanceState * inst = (InstanceState *) malloc(sizeof(InstanceState));
    /* Store the pointer to the instance state in the java variable "peer" */
    SetLongField(env, jxil, "peer", (int) inst);

    /* Debug message   */
    PRINT("In xilConnect\n");

    /* Copy the parameters */
    inst->port = (int) port;
    inst->xil_state = NULL;
    inst->rtvc_image = NULL;
    inst->scaled_image = NULL;
    inst->colored_image = NULL;
    inst->xil_cis = NULL;
    inst->scale = 1;
    inst->quality = 0;
    inst->do_cis = RAW;
    inst->cis_type = "Raw";
    inst->skip = 0;
    inst->inWidth = 0;
    inst->inHeight = 0;
    inst->outWidth = 0;
    inst->outHeight = 0;
    inst->inStrideX = 0;

    inst->started = 0;
    inst->firstRawRead = 1;

    /* inst->xil_state = xil_open(); */
    createXilState();
    inst->xil_state = *jmf_xil_state;
    if (inst->xil_state == NULL) {
	fprintf(stderr, "SunVideo Capture unable to open xil library\n");
	freeXilState(env, jxil, inst);
	return 0;
    }

    /*    Debug Message */
    PRINT("XILCapture open_xil() succeeded \n");
    if (devnum > 0) {
	sprintf(devnamestr, "/dev/rtvc%d", devnum);
	devname = devnamestr;
	/*    Debug Message */
	PRINT("Attempting to open xil device ");
	PRINT(devname);
	PRINT("\n");
    }

    if (! (device = xil_device_create(inst->xil_state, "SUNWrtvc"))) {
	/*    Debug Message */
	PRINT("Unable to create a xil device object\n");
	freeXilState(env, jxil, inst);
	return 0;
    }
    /*    Debug Message */
    PRINT("XILCapture xil_device_create() succeeded \n");
    xil_device_set_value(device, "DEVICE_NAME", devname);
    /*    Debug Message */
    PRINT("XILCapture xil_device_set_value() succeeded \n");
    inst->rtvc_image = xil_create_from_device(inst->xil_state, "SUNWrtvc",
						device);
    /*    Debug Message */
    PRINT("XILCapture xil_create_from_device() completed \n");
    xil_device_destroy(device);
    /*    Debug Message */
    PRINT("XILCapture xil_device_destroy() completed \n");
    if (inst->rtvc_image == NULL) {
	/*    Debug Message */
	PRINT("Unable to open xil device\n");
	freeXilState(env, jxil, inst);
	return 0;
    }

    tryPort(env, jxil, inst);
    return 1;
}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilSetPort
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilSetPort(JNIEnv *env,
							jobject jxil,
							jint port)
{
    int res;
    InstanceState * inst = (InstanceState *) GetLongField(env, jxil, "peer");
    
    if (inst == NULL)
	return TRUE;

    /*    Debug Message*/
    PRINT("In xilSetPort\n");

    if ((int)port == inst->port && inst->signal != UNKNOWN_SIGNAL)
	return TRUE;

    inst->port = (int)port;
    res = tryPort(env, jxil, inst);

    if (res) {
	setFormat(env, jxil, inst);
	updateXilImages(inst);
    }

    return res;
}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilSetScale
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilSetScale(JNIEnv *env,
						      jobject jxil,
						      jint jscale)
{
    int scale;

    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return;

    /*    Debug Message*/
    PRINT("In xilSetScale\n");

    /* For now, only support 1 (full), 2 (1/2), 4 (1/4) */
    scale = (int) jscale;
    if (scale == 1 || scale == 2 || scale == 4) {
	inst->scale = scale;
	inst->outWidth = 0;
	inst->outHeight = 0;
	inst->inStrideX = 0;
	setFormat(env, jxil, inst);
    } else {
	return 0;
    }

    return 1;

}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilSetSkip
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilSetSkip(JNIEnv *env,
						      jobject jxil,
						      jint jskip)
{
    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return;

    /*    Debug Message*/
    PRINT("In xilSetSkip\n");

    /* TODO - may want to do a sanity check */
    inst->skip = (int)jskip;
    xil_set_device_attribute(inst->rtvc_image,
				"IMAGE_SKIP", (void *)inst->skip);

}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilSetQuality
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilSetQuality(JNIEnv *env,
						      jobject jxil,
						      jint jquality)
{
    int quality = (int)jquality;
    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("In xilSetQuality\n");

    if (quality < 1) quality = 50;	/* force to default quality */
    if (quality > 100) quality = 100;	/* limit to maximum quality */
    if (quality == inst->quality)
	return TRUE;			/* if no change, return */
    inst->quality = quality;
    if (inst->started && inst->xil_cis != NULL) {
	xil_cis_set_attribute(inst->xil_cis,
				"COMPRESSION_QUALITY", (void *)inst->quality);
    }

    setFormat(env, jxil, inst);

    return TRUE;
}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilSetCompress
 * Signature:	(Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilSetCompress(JNIEnv *env,
						      jobject jxil,
						      jstring jcompress)
{
    const char *compress;

    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return;

    /*    Debug Message*/
    PRINT("In xilSetCompress\n");

    compress = (*env)->GetStringUTFChars(env, jcompress, 0);

    if (strcasecmp(compress, "rgb") == 0) {
	inst->do_cis = RAW;
	inst->cis_type = "Raw";
    } else if (strcasecmp(compress, "jpeg") == 0) {
	inst->do_cis = JPEG;
	inst->cis_type = "Jpeg";
    } else if ((strcasecmp(compress, "mpeg") == 0) ||
		(strcasecmp(compress, "mpeg1") == 0)) {
	inst->do_cis = MPEG;
	inst->cis_type = "Mpeg1";
    } else if (strcasecmp(compress, "cellb") == 0) {
	inst->do_cis = CELLB;
	inst->cis_type = "CellB";
    } else {
	/*    Debug Message */
	PRINT("Invalid compress format specified %s ");
	PRINT(compress);
	PRINT("\n");
    }
    (*env)->ReleaseStringUTFChars(env, jcompress, compress);

    setFormat(env, jxil, inst);

    return TRUE;
}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilStart
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilStart(JNIEnv *env,
						      jobject jxil)
{
    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return 0;

    /*    Debug Message*/
    PRINT("In xilStart\n");

    /*
     * Check scale and compression to eliminate invalid combinations.
     */
    if (inst->do_cis == JPEG && inst->scale == 1) {
	return 0;
    }

    /*
     * At this point, rtvc_image is valid but nothing else is.
     * Construct the rest of the images needed.
     */
    if (inst->inWidth == 0 || inst->signal == UNKNOWN_SIGNAL) {
	/* Don't know the input dimensions yet, get them. */
	if (!tryPort(env, jxil, inst)) {
	    fprintf(stderr,
			"SunVideo Capture xilStart() port not responding \n");
	    return 0;
	}
    }
    inst->started = 1;
    updateXilImages(inst);
    return 1;

}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilRead
 * Signature:	([BI)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilRead(JNIEnv *env,
						      jobject jxil,
						      jbyteArray jbuf,
						      jint jlen)
{
    void *buf;
    int len;

    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return -1;
    if (!inst->started) {
	/*    Debug Message */
	PRINT("XILCapture xilRead() not started \n");
	return -1;
    }

    /*    Debug Message*/
    /*	PRINT("In xilRead\n");	*/

    buf = (void *) (*env)->GetByteArrayElements(env, jbuf, 0);

    if (inst->xil_cis) {
	len = readCompressed(inst, buf, (int)jlen);
    } else {
	len = readRaw(inst, buf, (int)jlen);
	if (inst->firstRawRead) {
	    setFormat(env, jxil, inst);
	    inst->firstRawRead = 0;
	}
    }
    (*env)->ReleaseByteArrayElements(env, jbuf, buf, 0);

    return len;

}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilGetWidth
 * Signature:	()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilGetWidth(JNIEnv *env,
						      jobject jxil)
{
    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return -1;
    if (!inst->started) {
	/*    Debug Message */
	PRINT("XILCapture xilGetWidth() not started \n");
	return (jint) (((float) inst->inWidth / inst->scale) + 0.5);
    }

    /*    Debug Message*/
    PRINT("In xilGetWidth\n");

    return (jint) inst->outWidth;

}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilGetHeight
 * Signature:	()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilGetHeight(JNIEnv *env,
						      jobject jxil)
{
    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return -1;
    if (!inst->started) {
	/*    Debug Message */
	PRINT("XILCapture xilGetHeight() not started \n");
	return (jint) (((float) inst->inHeight / inst->scale) + 0.5);
    }

    /*    Debug Message*/
    PRINT("In xilGetHeight\n");

    return (jint) inst->outHeight;

}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilGetLineStride
 * Signature:	()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilGetLineStride(JNIEnv *env,
						      jobject jxil)
{
    int w;
    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return -1;
    if (!inst->started || (inst->do_cis != RAW)) {
	/*    Debug Message */
	PRINT("XILCapture xilLineStride() not started \n");
	/* return a guess at what it will be */
	return (jint) (inst->inWidth * 3);
    }

    /*    Debug Message*/
    PRINT("In xilGetLineStride\n");

    if (inst->firstRawRead) {
	XilMemoryStorage storage;
	int stride = 0;
	xil_export(inst->colored_image);
	xil_get_memory_storage(inst->colored_image, &storage);
	stride = storage.byte.scanline_stride;
	xil_import(inst->colored_image, 0);
	/*    Debug Message */
	PRINT("XILCapture xilLineStride() no read done \n");
	return (jint) stride;
    }

    return (jint) inst->inStrideX;

}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilStop
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilStop(JNIEnv *env,
						      jobject jxil)
{
    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    if (inst == NULL)
	return;

    /*    Debug Message*/
    PRINT("In xilStop\n");
    inst->started = 0;
    inst->firstRawRead = 1;

    freeXilImages(inst);

}

/*
 * Class:	com_sun_media_protocol_sunvideo_XILCapture
 * Method:	xilDisconnect
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideo_XILCapture_xilDisconnect(JNIEnv *env,
						      jobject jxil)
{
    InstanceState *inst = (InstanceState *) GetLongField(env, jxil, "peer");

    /*    Debug Message */
    PRINT("In xilFree\n");
    
    if (inst) {
	freeXilState(env, jxil, inst);
    }
}

