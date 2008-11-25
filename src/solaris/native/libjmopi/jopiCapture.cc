/*
 * @(#)jopiCapture.cc	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/utsname.h>
#include "OPICapture.h"
#include "com_sun_media_protocol_sunvideoplus_OPICapture.h"
#include <jni-util.h>

#ifdef DEBUG
#define PRINT(s) printf(s)
#else
#define PRINT(s)
#endif

static int formatUnspec = -1;


/*************************************************************************
 * Local Methods
 *************************************************************************/

static int
formatRGB(JNIEnv *env, jobject jopi, OPICapture *inst, jobject jStream)
{
    int stride = inst->outWidth * 2;	/* 16 bit RGB565 */

    if (stride <= 0)
	stride = formatUnspec;
    CallVoidMethod(env, jStream, "setRGBFormat", "(IIIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			stride, inst->scale);
    return TRUE;
}

static int
formatYUV(JNIEnv *env, jobject jopi, OPICapture *inst, jobject jStream)
{
    int stride = inst->outWidth * 2;	/* 16 bit YUV422 */

    if (stride <= 0)
	stride = formatUnspec;
    CallVoidMethod(env, jStream, "setYUVFormat", "(IIIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			stride, inst->scale);
    return TRUE;
}

static int
formatH261(JNIEnv *env, jobject jopi, OPICapture *inst, jobject jStream)
{
    CallVoidMethod(env, jStream, "setH261Format", "(IIIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			inst->quality, inst->scale);
    return TRUE;
}

static int
formatH263(JNIEnv *env, jobject jopi, OPICapture *inst, jobject jStream)
{
    CallVoidMethod(env, jStream, "setH263Format", "(IIIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			inst->quality, inst->scale);
    return TRUE;
}

static int
formatJpeg(JNIEnv *env, jobject jopi, OPICapture *inst, jobject jStream)
{
    CallVoidMethod(env, jStream, "setJpegFormat", "(IIIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			inst->quality, inst->scale);
    return TRUE;
}

static int
formatMpeg(JNIEnv *env, jobject jopi, OPICapture *inst, jobject jStream)
{
    CallVoidMethod(env, jStream, "setMpegFormat", "(IIIIII)V",
			inst->inWidth, inst->inHeight,
			inst->outWidth, inst->outHeight,
			inst->quality, inst->scale);
    return TRUE;
}

static int
setFormat(JNIEnv *env, jobject jopi, OPICapture *inst)
{
    jboolean isInstance;
    jobject jStream;

    /*    Debug Message */
    PRINT("jopiCapture in setFormat \n");

    jStream = GetObjectField(env, jopi, "stream",
				"Ljavax/media/protocol/SourceStream;");
    if (!jStream) {
	/*    Debug Message */
	PRINT("jopiCapture setFormat failed to obtain stream \n");
	return FALSE;
    }
    isInstance = IsInstanceOf(env, jStream, 
			"com/sun/media/protocol/sunvideoplus/OPISourceStream");
    if (!isInstance) {
	/*    Debug Message */
	PRINT("jopiCapture setFormat stream not valid instance \n");
	return FALSE;
    }
    if (inst->do_compress == RGB) {
	return formatRGB(env, jopi, inst, jStream);
    } else if (inst->do_compress == YUV) {
	return formatYUV(env, jopi, inst, jStream);
    } else if (inst->do_compress == H261) {
	return formatH261(env, jopi, inst, jStream);
    } else if (inst->do_compress == H263) {
	return formatH263(env, jopi, inst, jStream);
    } else if (inst->do_compress == JPEG) {
	return formatJpeg(env, jopi, inst, jStream);
    } else if (inst->do_compress == MPEG) {
	return formatMpeg(env, jopi, inst, jStream);
    } else {
	return FALSE;
    }
}

static void
freeOPICapture(JNIEnv *env, jobject jopi, OPICapture *inst)
{

    /*    Debug Message */
    PRINT("In freeOPICapture\n");
    
    if(inst) {
	SetLongField(env, jopi, "peer", (jlong) 0);
	delete inst;
    }
}

 
/*************************************************************************
 * Java Native methods
 *************************************************************************/

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiInitialize
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiInitialize(
							    JNIEnv *env,
							    jclass jopiclass)
{
    struct utsname osDetails;

    /*    Debug Message */
    PRINT("jopiCapture in opiInitialize \n");

    /* get the OS details */
    uname(&osDetails);

    osDetails.release[3] = 0;
    
    if (strcmp(osDetails.release, "2.4") == 0 ||
	strcmp(osDetails.release, "5.4") == 0) {
	/*    Debug Message */
	PRINT("jopiCapture in opiInitialize, failed, Solaris 2.4 \n");
	return (jboolean) FALSE;
    } else
	return (jboolean) TRUE;
}


/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	cacheFieldIDs
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_cacheFieldIDs(
							    JNIEnv *env,
							    jclass jopiclass)
{
    jclass jFormatClass;
    jfieldID jFormatUnspecID;

    /*    Debug Message */
    PRINT("jopiCapture in cacheFieldIDs \n");
    jFormatClass = env->FindClass("javax/media/Format");
    if (jFormatClass) {
	jFormatUnspecID = env->GetStaticFieldID(jFormatClass,
					"NOT_SPECIFIED", "I");
	if (jFormatUnspecID) {
	    formatUnspec = env->GetStaticIntField(jFormatClass,
					jFormatUnspecID);
	}
    }

    if(env->ExceptionOccurred()) {
	env->ExceptionDescribe();
	env->ExceptionClear();
    }
    return (jboolean) TRUE;
}


/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiLittleEndian
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiLittleEndian(
						       JNIEnv *env,
						       jobject jopi)
{
    int one = 1;
    if (*((char *) &one) == 1)
	return (jboolean) TRUE;
    else
	return (jboolean) FALSE;
}


/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiConnect
 * Signature:	(II)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiConnect(
						      JNIEnv *env,
						      jobject jopi,
						      jint devnum,
						      jint port)
{
    boolean res = FALSE;

    /* Debug message   */
    PRINT("jopiCapture in opiConnect\n");

    /* Allocate a new structure */
    OPICapture * inst = new OPICapture((int)devnum, (int)port);

    if (inst) {
	/* Store the pointer to the capture class in the java variable "peer" */
	SetLongField(env, jopi, "peer", (int) inst);
	res = inst->opiConnect();
	if (!res)
	    freeOPICapture(env, jopi, inst);
    }
    return res;
}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiSetPort
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiSetPort(
						        JNIEnv *env,
							jobject jopi,
							jint port)
{
    OPICapture * inst = (OPICapture *) GetLongField(env, jopi, "peer");
    
    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiSetPort\n");

    if (inst->opiSetPort((int)port))
	setFormat(env, jopi, inst);
    else
	return FALSE;

    return TRUE;
}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiSetScale
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiSetScale(
						      JNIEnv *env,
						      jobject jopi,
						      jint jscale)
{
    boolean res;

    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiSetScale\n");

    if (inst->opiSetScale((int) jscale))
	setFormat(env, jopi, inst);
    else
	return FALSE;

    return TRUE;

}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiSetFrameRate
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiSetFrameRate(
						      JNIEnv *env,
						      jobject jopi,
						      jint jfps)
{
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiSetFrameRate\n");

    return inst->opiSetFrameRate((int)jfps);
}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiSetBitRate
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiSetBitRate(
						      JNIEnv *env,
						      jobject jopi,
						      jint jbitrate)
{
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiSetBitRate\n");

    /* TODO - may want to do a sanity check */
    return inst->opiSetBitRate((int)jbitrate);
}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiSetQuality
 * Signature:	(I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiSetQuality(
						      JNIEnv *env,
						      jobject jopi,
						      jint jquality)
{
    int quality = (int)jquality;
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiSetQuality\n");

    if(inst->opiSetQuality(quality))
	setFormat(env, jopi, inst);
    else
	return FALSE;

    return TRUE;
}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiSetCompress
 * Signature:	(Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiSetCompress(
						      JNIEnv *env,
						      jobject jopi,
						      jstring jcompress)
{
    boolean res;
    const char *compress;

    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiSetCompress\n");

    compress = env->GetStringUTFChars(jcompress, 0);
    if (compress == NULL) {
	PRINT("jopiCapture opiSetCompress got NULL instead of string\n");
	return FALSE;
    }

    res = inst->opiSetCompress(compress);
 
    env->ReleaseStringUTFChars(jcompress, compress);

    if (res)
	setFormat(env, jopi, inst);

    return res;
}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiSetFormat
 * Signature:	(Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiSetSignal(
						      JNIEnv *env,
						      jobject jopi,
						      jstring jsignal)
{
    boolean res;
    const char *signal;

    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiSetSignal\n");

    signal = env->GetStringUTFChars(jsignal, 0);

    res = inst->opiSetSignal(signal);
    env->ReleaseStringUTFChars(jsignal, signal);

    if (res)
	setFormat(env, jopi, inst);

    return res;
}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiStart
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiStart(
						      JNIEnv *env,
						      jobject jopi)
{
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return FALSE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiStart\n");

    return inst->opiStart();

}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiRead
 * Signature:	([BI)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiRead(
						      JNIEnv *env,
						      jobject jopi,
						      jbyteArray jbuf,
						      jint jlen)
{
    void *buf;
    int len = -1;

    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return -1;

    /*    Debug Message*/
    /*	PRINT("jopiCapture in opiRead\n");	*/

    buf = (void *) env->GetByteArrayElements(jbuf, 0);

    len = inst->opiRead(buf, (int)jlen);

    env->ReleaseByteArrayElements(jbuf, (signed char *)buf, 0);

    return len;

}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiGetWidth
 * Signature:	()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiGetWidth(
						      JNIEnv *env,
						      jobject jopi)
{
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return -1;

    /*    Debug Message*/
    PRINT("jopiCapture in opiGetWidth\n");

    return (jint) inst->opiGetWidth();

}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiGetHeight
 * Signature:	()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiGetHeight(
						      JNIEnv *env,
						      jobject jopi)
{
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return -1;

    /*    Debug Message*/
    PRINT("jopiCapture in opiGetHeight\n");

    return (jint) inst->opiGetHeight();

}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiGetLineStride
 * Signature:	()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiGetLineStride(
						      JNIEnv *env,
						      jobject jopi)
{
    int w;
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return -1;

    /*    Debug Message*/
    PRINT("jopiCapture in opiGetLineStride\n");

    return (jint) inst->opiGetLineStride();

}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiStop
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiStop(
						      JNIEnv *env,
						      jobject jopi)
{
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    if (inst == NULL)
	return TRUE;

    /*    Debug Message*/
    PRINT("jopiCapture in opiStop\n");

    return inst->opiStop();
}

/*
 * Class:	com_sun_media_protocol_sunvideoplus_OPICapture
 * Method:	opiDisconnect
 * Signature:	()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_protocol_sunvideoplus_OPICapture_opiDisconnect(
						      JNIEnv *env,
						      jobject jopi)
{
    boolean res = TRUE;
    OPICapture *inst = (OPICapture *) GetLongField(env, jopi, "peer");

    /*    Debug Message */
    PRINT("jopiCapture in opiDisconnect\n");
    
    if (inst) {
	res = inst->opiDisconnect();
	freeOPICapture(env, jopi, inst);
    }
    return res;
}

