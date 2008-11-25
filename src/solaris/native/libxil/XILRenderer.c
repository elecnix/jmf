/*
 * @(#)XILRenderer.c	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
#define SUNXIL_WARNING_DISABLE 1
#include <malloc.h>
#include <sys/utsname.h>
#include <xil/xil.h>
#include "com_sun_media_renderer_video_XILRenderer.h"
#include <jni-util.h>

#ifdef DEBUG
#define PRINT(s) printf(s)
#else
#define PRINT(s)
#endif

/* jmf_xil_state made global to be shared with XILCapture */
XilSystemState * jmf_xil_state = NULL;

typedef struct {

    XID            xid;
    Display *      xdisplay;
    XilSystemState xil_state;
    XilImage       xil_src;
    XilImage       xil_src_child;
    XilImage       xil_dst;
    XilLookup      colormap;
    XilDitherMask  dithermask;
    XilKernel      fskernel;

    jobject        component;
    jobject        surface;
    int            screen;
    
    int            inWidth;
    int            inHeight;
    int            outWidth;
    int            outHeight;
    int            inStrideX;
    int            offsetX;
    int            offsetY;
    int            bpp;
    float          sx;
    float          sy;

} Blitter;


/*************************************************************************
 * Local Methods
 *************************************************************************/

/* createXilState made global to be shared with XILCapture */
void
createXilState()
{
    if (jmf_xil_state == NULL) {
	jmf_xil_state = (XilSystemState *) malloc(sizeof(XilSystemState));
	*jmf_xil_state = xil_open();
    }
}

/*************************************************************************
 * Java Native methods
 *************************************************************************/

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XILRenderer_xilInitialize(JNIEnv *env,
							    jobject jxil)
{
    struct utsname osDetails;

    /* get the OS details */
    uname(&osDetails);

    osDetails.release[3] = 0;
    
    if (strcmp(osDetails.release, "2.4") == 0 ||
	strcmp(osDetails.release, "5.4") == 0)
	return (jboolean) 0;
    else
	return (jboolean) 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XILRenderer_xilLittleEndian(JNIEnv *env,
						       jobject jxil)
{
    int one = 1;
    if (*((char *) &one) == 1)
	return (jboolean) 1;
    else
	return (jboolean) 0;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XILRenderer_xilSetComponent(JNIEnv *env,
							      jobject jxil,
							      jint handle,
							      jint bitsPerPixel)
{
    /* Allocate a new structure */
    Blitter * blt = (Blitter *) malloc(sizeof(Blitter));
    /* Store the pointer to the blitter in the java variable "nativeData" */
    SetIntField(env, jxil, "nativeData", (int) blt);

    /* Debug message   */
    PRINT("In xilSetComponent\n");

    /* Copy the two parameters */
    blt->bpp = (int) bitsPerPixel;
    blt->xil_state = NULL;
    blt->xil_src = NULL;
    blt->xil_src_child = NULL;
    /*    blt->xil_dithered = NULL;*/
    blt->xil_dst = NULL;
    blt->colormap = NULL;
    blt->outWidth = -1;
    blt->outHeight = -1;
    createXilState();
    blt->xil_state = *jmf_xil_state;
    blt->xid = (XID) handle;
    blt->xdisplay = XOpenDisplay(NULL);
    blt->surface = NULL;
    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XILRenderer_xilSetInputFormat(JNIEnv *env,
								jobject jxil,
								jint width, jint height,
								jint strideX)
{
    Blitter * blt = (Blitter *) GetIntField(env, jxil, "nativeData");
    int one = 1;
    int byte1 = 1;
    if (*((char*)&one) == 1) /* Little endian - x86 */
	byte1 = 0;
    
    if (blt == NULL)
	return;

    /*    Debug Message*/
    PRINT("In xilSetInputSize\n");
    
    if (blt->xil_state == 0) {
	createXilState();
	blt->xil_state = *jmf_xil_state;
    }

    if (blt->xil_src)
	xil_destroy(blt->xil_src);
    if (blt->xil_src_child)
	xil_destroy(blt->xil_src_child);

    blt->inWidth = width;
    blt->inHeight = height;

    if (blt->inWidth == 0 || blt->inHeight == 0)
	return;
    
    blt->inStrideX = strideX;
    blt->sx = (float)(blt->outWidth) / blt->inWidth;
    blt->sy = (float)(blt->outHeight) / blt->inHeight;
    
    if (blt->bpp == 8) {
	blt->xil_src = xil_create(blt->xil_state, width, height, 1, XIL_BYTE);
	if (blt->xil_src == NULL)
	    printf("xil/Blitter: xil_src is null\n");
    } else {
	blt->xil_src = xil_create(blt->xil_state, width, height, 4, XIL_BYTE);
	if (blt->xil_src == NULL)
	    printf("xil/Blitter: xil_src is null\n");
	
	blt->xil_src_child = xil_create_child(blt->xil_src, 0, 0, width, height, byte1, 3);
	if (blt->xil_src_child == NULL)
	    printf("xil/Blitter: xil_src_child is null\n");
    }

    if (blt->xil_src == 0)
	return 0;
    else
	return 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XILRenderer_xilSetOutputSize(JNIEnv *env,
							       jobject jxil,
							       jint width, jint height)
{
    Blitter * blt = (Blitter *) GetIntField(env, jxil, "nativeData");
    if (blt == NULL)
	return;

    /*    Debug Message */
    PRINT("In xilSetOutputSize\n");
    
    blt->offsetX = GetIntField(env, jxil, "offsetX");
    blt->offsetY = GetIntField(env, jxil, "offsetY");

    if (blt->xil_state == 0) {
	createXilState();
	blt->xil_state = *jmf_xil_state;
    }

    blt->outWidth = width;
    blt->outHeight = height;
    blt->sx = (float)(blt->outWidth) / blt->inWidth;
    blt->sy = (float)(blt->outHeight) / blt->inHeight;

    if (blt->xil_dst) {
	xil_destroy(blt->xil_dst);
    }
    blt->xil_dst = xil_create_from_window(blt->xil_state, blt->xdisplay,
					  blt->xid);
    if (blt->xil_dst == NULL)
	printf("xil/Blitter: xil_dst is null\n");
    xil_set_origin(blt->xil_dst, blt->offsetX, blt->offsetY);

    if (blt->xil_dst == 0)
	return 0;
    else
	return 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XILRenderer_xilDraw(JNIEnv *env,
						      jobject jxil,
						      jint x, jint y,
						      jint outWidth, jint outHeight,
						      jobject jbuffer,
						      jlong dataBytes)
{
    Blitter *blt = (Blitter *) GetIntField(env, jxil, "nativeData");
    jarray array = (jarray) jbuffer;
    int *imageData = (int *) dataBytes;
    XilMemoryStorage storage;
    int nBands;
    XilImage dstImage;

    if (blt == NULL)
	return 1;

    nBands = (blt->bpp == 8)? 1:4;
    //    if (blt->outWidth <= 0 || blt->outHeight <= 0)
    //return 1;

    if (blt->outWidth != outWidth || blt->outHeight != outHeight) {
	Java_com_sun_media_renderer_video_XILRenderer_xilSetOutputSize(env, jxil,
								       outWidth, outHeight);
    }
    
    /*    Debug Message */
    PRINT("In xilDraw\n");

    dstImage = blt->xil_dst;
    xil_set_origin(blt->xil_dst, x, y);
    if (dataBytes == 0) {
	if (nBands == 4)
	    imageData = (int*) (*env)->GetIntArrayElements(env, (jintArray) array, 0);
	else
	    imageData = (int*) (*env)->GetByteArrayElements(env, (jbyteArray) array, 0);
    }
    
    if (imageData == NULL)
	return 1;
    xil_export(blt->xil_src);
    storage.byte.data = (Xil_unsigned8 *) imageData;
    storage.byte.pixel_stride = nBands;
    storage.byte.scanline_stride = nBands * blt->inStrideX;
    xil_set_memory_storage(blt->xil_src, &storage);
    xil_import(blt->xil_src, 1);

    if (blt->bpp == 24 || blt->bpp == 32) {
	/* TrueColor blitting */
	if (blt->inWidth == blt->outWidth && blt->inHeight == blt->outHeight)
	    xil_copy(blt->xil_src_child, dstImage);
	else
	    xil_scale(blt->xil_src_child, dstImage, "nearest", blt->sx, blt->sy);
    } else {
	/* PseudoColor blitting */
	if (blt->inWidth == blt->outWidth && blt->inHeight == blt->outHeight)
	    xil_copy(blt->xil_src, dstImage);
	else
	    xil_scale(blt->xil_src, dstImage, "nearest", blt->sx, blt->sy);
    }
    
    /* Release the input array */
    if (dataBytes == 0) {
	if (nBands == 4)
	    (*env)->ReleaseIntArrayElements(env, (jintArray) array,
					    (jint*) imageData, JNI_ABORT);
	else
	    (*env)->ReleaseByteArrayElements(env, (jbyteArray) array,
					     (jbyte*) imageData, JNI_ABORT);
    }
    return 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XILRenderer_xilFree(JNIEnv *env,
						      jobject jxil)
{
    Blitter *blt = (Blitter *) GetIntField(env, jxil, "nativeData");

    /*    Debug Message */
    PRINT("In xilFree\n");
    
    if (blt) {
	if (blt->xil_src)
	    xil_destroy(blt->xil_src);
	if (blt->xil_src_child)
	    xil_destroy(blt->xil_src_child);
	if (blt->xil_dst)
	    xil_destroy(blt->xil_dst);
	/*	if (blt->xil_state)*/
	/*	    xil_close(blt->xil_state);*/
	if (blt->xdisplay)
	    XCloseDisplay(blt->xdisplay);
	/*	if (blt->surface) {*/
	/*	    (*env)->DeleteGlobalRef(env, blt->surface);*/
	/*	    printf("Deleted global reference of surface\n");*/
	/*	}*/
	free(blt);
	SetIntField(env, jxil, "nativeData", 0);
    }
}

