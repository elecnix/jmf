/*
 * @(#)XLibRenderer.c	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <malloc.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
/* For shared memory */
#include <X11/extensions/XShm.h>
#include <sys/shm.h>

#include <sys/types.h>
#include <math.h>
#include <stdlib.h>
#include "jni-util.h"
#include "com_sun_media_renderer_video_XLibRenderer.h"

typedef struct {
  Display *display;					    /* X display */
  Visual  *visual;					    /* X Visual */
  void    *imageData;					    /* image data */
  int      inWidth;					    /* input image width */
  int      inHeight;					    /*  "     "   height */
  int      inStride;					    /* bytes per line */
  int      outWidth;					    /* output size (zoom?) */
  int      outHeight;					    /*  */
  int      screen;					    /* screen no. */
  Drawable  xid;					    /* window to draw to */
  GC       gc;					    /* graphics context */
  jobject  component;					    /* AWT component */
  jobject  surface;					    /* Drawing Surface */
  XImage  *ximage;					    /* X Image */
  void    *scaledData;				    /* buffer to scale into */
  char    *xincs;					    /* scaling arrays */
  char    *yincs;
  int      depth;					    /* visual depth */
  int      bitsPerPixel;				    /* includes padding */
  
  int      shmAvailable;
  int      shmActive;
  int      shmComplete;
  int      shmOpcode;
  XShmSegmentInfo shmInfo;
  int      awtObj;                                        /* pointer to JAWT*/
  int      drawingSurface;                                /* pointer to JAWT_DrawingSurface*/
} XlibBlitter;

static int jawt = 0;
/*************************************************************************
 * Helper functions
 *************************************************************************/

static void awtLock(JNIEnv *env, XlibBlitter *blt) {
  jmethodID mid;
  jclass cls;
  
  if ( jawt == 1 ) {
    cls = (jclass)(*env)->FindClass(env, "com/sun/media/DrawingSurfaceJAWT");
    if ( cls == NULL ) {
	printf("in awtLock cls is null\n");
	return;
    }
    mid = (*env)->GetStaticMethodID(env, cls, "lockAWT", "(I)Z");
    if ( mid == NULL ) {
	printf("in awtLock mid is null\n");
	return;
    }

    (*env)->CallStaticBooleanMethod(env, cls, mid, blt->drawingSurface);
    if((*env)->ExceptionOccurred(env)) {
	(*env)->ExceptionDescribe(env);
	(*env)->ExceptionClear(env);
    }

    return;
  } else {
    CallIntMethod(env, blt->surface, "lock", "()I", NULL);
  }
}

static void awtUnlock(JNIEnv *env, XlibBlitter *blt) {
  jmethodID mid;
  jclass cls;
  if ( jawt == 1 ) {
    cls = (jclass)(*env)->FindClass(env, "com/sun/media/DrawingSurfaceJAWT");
    if ( cls == NULL ) {
	printf("in awtUnlock cls is null\n");
	return;
    }
    
    mid = (*env)->GetStaticMethodID(env, cls, "unlockAWT", "(I)V");
    if ( mid == NULL ) {
	printf("in awtUnlock mid is null\n");
	return;
    }

    (*env)->CallStaticVoidMethod(env, cls, mid, blt->drawingSurface);
    if((*env)->ExceptionOccurred(env)) {
	(*env)->ExceptionDescribe(env);
	(*env)->ExceptionClear(env);
    }

    return;
  } else {
    CallVoidMethod(env, blt->surface, "unlock", "()V", NULL);
    }
}

static void freeJAWT(JNIEnv *env, XlibBlitter *blt) {
  jmethodID mid;
  jclass cls;
  
  /*printf("in freeJAWT \n");*/
  cls = (jclass)(*env)->FindClass(env, "com/sun/media/DrawingSurfaceJAWT");
  if ( cls == NULL ) {
      printf("in freeJAWT cls is null\n");
      return;
  }
  
  mid = (*env)->GetStaticMethodID(env, cls, "freeResource", "(II)V");
  if ( mid == NULL ) {
      printf("in freeJAWT mid is null\n");
      return;
  }

  (*env)->CallStaticVoidMethod(env, cls, mid, blt->awtObj, blt->drawingSurface );
  if((*env)->ExceptionOccurred(env)) {
      (*env)->ExceptionDescribe(env);
      (*env)->ExceptionClear(env);
  }
  
  return;
}

void freeDisplay(XlibBlitter blitter) {
}

static int getComponentInfo(JNIEnv *env, XlibBlitter *blt) {    
  jmethodID mid;
  jobject peer, surface;
  jclass jds, jwinfo;
  jmethodID cid;
  jobject insDS;
  jintArray winfos;
  jint buf[5];
  
  if ( jawt == 1 ) {
    // new instance of DrawingSurfaceJAWT
    // invoke getWindowInfo
    // get winfo field and set blt filed
    jds = (*env)->FindClass(env, "com/sun/media/DrawingSurfaceJAWT");
    if ( jds == NULL )
      return 0;
    
    cid = (*env)->GetMethodID(env, jds, "<init>", "()V");
    if ( cid == NULL ) 
      return 0;

    insDS = (*env)->NewObject(env, jds, cid, NULL);
    
    if ( insDS == NULL ) {
	printf("in Xlibrenderer getComponentInfo insDS is NULL \n");
	return 0;
    }
    mid = (*env)->GetMethodID(env, jds, "getWindowInfo", "(Ljava/awt/Component;)[I");

    if ( mid == NULL) {
	printf("in xlibRenderer getComponentInfo mid is null\n");
	return 0;
    }
    winfos = (*env)->CallObjectMethod(env, insDS, mid, blt->component);
    
    /*printf("after invoke jni getWinowInfo \n");*/

    if ( winfos == NULL ) {
	printf("getWinowInfo call failed \n");
	return 0;
    }

    if((*env)->ExceptionOccurred(env)) {
	(*env)->ExceptionDescribe(env);
	(*env)->ExceptionClear(env);
	return 0;
    }

    (*env)->GetIntArrayRegion(env, winfos, 0, 5, buf);
    if ( buf[0] == 0 )
      return 0;
    
    blt->awtObj = buf[1];
    blt->drawingSurface = buf[2];
    blt->xid =(Drawable)buf[3];
    blt->display = (Display *)buf[4];

    return 1;
    
  }      
  mid = GetMethodID(env, blt->component, "getPeer", 
		    "()Ljava/awt/peer/ComponentPeer;");
  if (!mid)
      return 0;
  peer = (*env)->CallObjectMethod(env, blt->component, mid); 
  mid = GetMethodID(env, peer, "getDrawingSurfaceInfo",
		    "()Lsun/awt/DrawingSurfaceInfo;");
  if (!mid)
      return 0;
  surface = (*env)->CallObjectMethod(env, peer, mid);
  /* NOTE: The global reference needs to be freed after we're done with it. */
  surface = (*env)->NewGlobalRef(env, surface);
  blt->surface = surface;
  /* Get the XID */
  mid = GetMethodID(env, surface, "getDrawable", "()I");
  if (!mid)
      return 0;
  blt->xid = (XID) (*env)->CallIntMethod(env, surface, mid);
  /* Get the xdisplay */
  mid = GetMethodID(env, surface, "getDisplay", "()I");
  if (!mid)
      return 0;
  blt->display = (Display *) (*env)->CallIntMethod(env, surface, mid);
  return 1;
}

/*************************************************************************
 * Native methods
 *************************************************************************/
JNIEXPORT void JNICALL Java_com_sun_media_renderer_video_XLibRenderer_xlibSetJAWT(JNIEnv *env, jobject blitter, jint jawtAvail) {
    jawt = jawtAvail;
}


JNIEXPORT jboolean JNICALL Java_com_sun_media_renderer_video_XLibRenderer_xlibInitialize(JNIEnv *env,
											 jobject blitter)
{
    XlibBlitter *xblitter = (XlibBlitter*) malloc(sizeof(XlibBlitter));
    SetIntField(env, blitter, "blitter", (int)xblitter);
    
    /* Initialize fields */
    xblitter->display   = NULL;
    xblitter->visual    = NULL;
    xblitter->imageData = NULL;
    xblitter->component = NULL;
    xblitter->scaledData= NULL;
    xblitter->xincs     = NULL;
    xblitter->yincs     = NULL;
    xblitter->ximage    = NULL;
    xblitter->outWidth  = -1;
    xblitter->outHeight = -1;
    xblitter->surface   = NULL;
    
    xblitter->shmAvailable = 0;
    xblitter->shmActive = 0;
    xblitter->awtObj = 0;
    xblitter->drawingSurface = 0;
    return 1;
}


JNIEXPORT jboolean JNICALL
    Java_com_sun_media_renderer_video_XLibRenderer_xlibSetComponent(JNIEnv *env,
								    jobject blitter,
								    jobject component)
{
    XWindowAttributes xwa;
    int error;
    XlibBlitter *xblitter = (XlibBlitter*) GetIntField(env, blitter, "blitter");
    
    if (xblitter->display) {
	freeDisplay(*xblitter);
    }
    
    if (xblitter->surface) {
	(*env)->DeleteGlobalRef(env, xblitter->surface);
	xblitter->surface = NULL;
    }
    
    if ( jawt == 1 && xblitter->awtObj != 0) {
	freeJAWT(env, xblitter);
	xblitter->awtObj = 0;
	xblitter->drawingSurface = 0;
    }
    
    xblitter->component = component;
    getComponentInfo(env, xblitter);
    
    awtLock(env, xblitter);
    
    xblitter->screen = DefaultScreen(xblitter->display);
    XGetWindowAttributes(xblitter->display, xblitter->xid, &xwa);
    xblitter->visual = xwa.visual;
    xblitter->depth  = xwa.depth;
    xblitter->bitsPerPixel = (xwa.depth == 8)? 8 : ((xwa.depth < 24)? 16 : 32);
    xblitter->gc = XCreateGC(xblitter->display, xblitter->xid, 0, 0);
    
    xblitter->shmAvailable = XQueryExtension(xblitter->display, "MIT-SHM",
					     &(xblitter->shmOpcode),
					     &(xblitter->shmComplete),
					     &error);
    
    awtUnlock(env, xblitter);
    
    return 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XLibRenderer_xlibSetInputFormat(JNIEnv *env,
								   jobject blitter,
								   jint width,
								   jint height,
								   jint stride)
{
    XlibBlitter *xblitter = (XlibBlitter*) GetIntField(env, blitter, "blitter");
    /*
    awtLock(env, xblitter);
    bitsPerPixel = (xblitter->depth == 8) ? 8 : 32;
    if (xblitter->scaledData)
	free(xblitter->scaledData);
    if (xblitter->ximage)
    XDestroyImage(xblitter->ximage);
    xblitter->ximage = XCreateImage(xblitter->display, xblitter->visual,
    xblitter->depth, ZPixmap, 0,
    (char *)data, width, height, bitsPerPixel,
    stride * (bitsPerPixel >> 3));
    */
    xblitter->inWidth = width;
    xblitter->inHeight = height;
    xblitter->inStride = stride;
    /*
      if (aes == 4)
      (*env)->ReleaseIntArrayElements(env, (jintArray)dataArray, data, 0);
      else
      (*env)->ReleaseByteArrayElements(env, (jbyteArray)dataArray, data, 0);
      if (xblitter->ximage == 0)
      printf("Couldn't create X Image \n");
      else
      printf("Created X Image \n");
      
      awtUnlock(env, xblitter);
    */
    return 1;
}

Bool imageComplete(Display *display, XEvent *event, char *arg) {
    if (event->type == *((int *)arg))
	return 1;
    else
	return 0;
}


void destroySharedMemory(XlibBlitter *xblitter) {
    if (xblitter->shmActive) {
	XShmDetach(xblitter->display, &xblitter->shmInfo);
	shmdt(xblitter->shmInfo.shmaddr);
	shmctl(xblitter->shmInfo.shmid, IPC_RMID, 0);
	xblitter->scaledData = NULL;
    }
}

static struct {
	int		attachFailed;
	int		major_code;
	XErrorHandler	oldHandler;
} Attachment;

void
createSharedMemory(XlibBlitter *xblitter, int outWidth, int outHeight)
{
    int imageSize;
    imageSize = outHeight * outWidth * xblitter->bitsPerPixel / 8;
    xblitter->shmActive = 0;
    if (xblitter->shmAvailable) {
	/* Create the shared memory image */
	xblitter->ximage = XShmCreateImage(xblitter->display, xblitter->visual,
					   xblitter->depth, ZPixmap, 0,
					   &xblitter->shmInfo, outWidth,
					   outHeight);
	if (!xblitter->ximage)
	    return;

	/* Allocate shared memory */
	xblitter->shmInfo.shmid = shmget(IPC_PRIVATE, imageSize,
					 IPC_CREAT | 0777);
	if (xblitter->shmInfo.shmid < 0) {
	    XDestroyImage(xblitter->ximage);
	    xblitter->ximage = NULL;
	    return;
	}


	xblitter->scaledData = (void *) shmat(xblitter->shmInfo.shmid, 0, 0);
	if (xblitter->scaledData == (void *) 0xFFFFFFFF) {
	    XDestroyImage(xblitter->ximage);
	    xblitter->ximage = NULL;
	    xblitter->scaledData = NULL;
	    shmctl(xblitter->shmInfo.shmid, IPC_RMID, 0);
	    return;
	}

	xblitter->ximage->data = (char *) xblitter->scaledData;
	xblitter->shmInfo.shmaddr = (char *) xblitter->scaledData;
	xblitter->shmInfo.readOnly = True;
	XSync(xblitter->display, False);
	
	if (XShmAttach(xblitter->display, &xblitter->shmInfo) == 0) {
	    XDestroyImage(xblitter->ximage);
	    xblitter->ximage = NULL;
	    xblitter->scaledData = NULL;
	    shmctl(xblitter->shmInfo.shmid, IPC_RMID, 0);
	    return;
	}
	/*	printf("Got shared memory\n");*/
	xblitter->shmActive = 1;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XLibRenderer_xlibSetOutputSize(JNIEnv *env,
								  jobject blitter,
								  jint outWidth,
								  jint outHeight)
{
    /* We assume that this function is called only if there is a size change */
    XlibBlitter *xblitter = (XlibBlitter*) GetIntField(env, blitter, "blitter");
    int inWidth = xblitter->inWidth;
    int inHeight = xblitter->inHeight;
    int stride   = xblitter->inStride;

    awtLock(env, xblitter);
    
    /* Get rid of the old image and arrays */
    if (xblitter->ximage) {
	/* Clean up shared memory */
	if (xblitter->shmActive) {
	    destroySharedMemory(xblitter);
	}
	xblitter->ximage->data = NULL;
	XDestroyImage(xblitter->ximage);
    }

    if (xblitter->scaledData) {
	free(xblitter->scaledData);
	xblitter->scaledData = NULL;
    }
    
    if (xblitter->xincs) {
	free(xblitter->xincs);
	xblitter->xincs = NULL;
    }

    if (xblitter->yincs) {
	free(xblitter->yincs);
	xblitter->yincs = NULL;
    }
    
    xblitter->outWidth = outWidth;
    xblitter->outHeight = outHeight;

    if (  outWidth != xblitter->inWidth ||
	  outHeight != xblitter->inHeight) {
	/* We need to scale */
	xblitter->xincs = (char *) malloc(outWidth * sizeof(char));
	xblitter->yincs = (char *) malloc(outHeight * sizeof(char));

	{
	    int x, y, oldValue, newValue, xtotal;
	    oldValue = 0; xtotal = 0;
	    for (x = 1; x < xblitter->outWidth; x++) {
		newValue = (int)((double)(x * xblitter->inWidth) / outWidth);
		xblitter->xincs[x-1] = newValue - oldValue;
		xtotal += newValue - oldValue;
		oldValue = newValue;
	    }
	    xblitter->xincs[x - 1] = xblitter->inStride - xtotal;
	    oldValue = 0;
	    for (y = 1; y < xblitter->outHeight; y++) {
		newValue = (int)((double)(y * inHeight) / outHeight);
		xblitter->yincs[y-1] = newValue - oldValue;
		oldValue = newValue;
	    }
	}

	stride = outWidth;
    } else {
	/* No scaling required */
	xblitter->xincs = NULL;
	xblitter->yincs = NULL;
	xblitter->scaledData = NULL;
    }

    /* Try to allocate a shared memory image */
    if (xblitter->shmAvailable) {
	createSharedMemory(xblitter, outWidth, outHeight);
    }
    
    /* If a shared memory image could not be created and we need to scale, use XLib */
    if (xblitter->shmActive == 0) {
	if ( outWidth != xblitter->inWidth || outHeight != xblitter->inHeight) {
	    xblitter->scaledData = (void *) malloc(outWidth * outHeight *
						   (xblitter->bitsPerPixel >> 3));
	}

	xblitter->ximage = XCreateImage(xblitter->display, xblitter->visual,
					xblitter->depth, ZPixmap, 0,
					(char *)0, outWidth, outHeight,
					xblitter->bitsPerPixel,
					stride * (xblitter->bitsPerPixel >> 3));
    }

    awtUnlock(env, xblitter);
    
    return 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XLibRenderer_xlibDraw(JNIEnv *env,
							jobject blitter,
							jobject dataArray,
							jlong dataBytes,
							jint aes)
{
    XlibBlitter *xblitter = (XlibBlitter*) GetIntField(env, blitter, "blitter");
    void *data = (void *) dataBytes;
    if (dataBytes == 0) {
	if (aes == 4)
	    data = (void *) (*env)->GetIntArrayElements(env, (jintArray)dataArray, 0);
	else if (aes == 2)
	    data = (void *) (*env)->GetShortArrayElements(env, (jshortArray)dataArray, 0);
	else
	    data = (void *) (*env)->GetByteArrayElements(env, (jbyteArray)dataArray, 0);
    }
    
    if (xblitter->ximage) {
	if (xblitter->scaledData && xblitter->xincs && xblitter->yincs) {
	    /* Scale the video to (outWidth, outHeight). */
	    int x, y;
	    char *xincs = xblitter->xincs, *yincs = xblitter->yincs;
#define SCALE								\
	    *scaledData++ = *inData;					\
	    for (x = 0; x < xblitter->outWidth-1; x++) {  		\
		inData += xincs[x];					\
		*scaledData++ = *inData;				\
	    }								\
 	    inData += xblitter->xincs[x];                		\
	    for (y = 0; y < xblitter->outHeight-1; y++) {		\
		char yinc = yincs[y];					\
		if (yinc == 0) {					\
		    memcpy(scaledData, scaledData - xblitter->outWidth,	\
			   xblitter->outWidth * aes);			\
		    scaledData += xblitter->outWidth;                   \
		} else {						\
		    inData += (yinc-1) * xblitter->inStride;             \
		    *scaledData++ = *inData;				\
		    for (x = 0; x < xblitter->outWidth - 1; x++) {      \
			inData += xincs[x];				\
			*scaledData++ = *inData;			\
		    }							\
		    inData += xblitter->xincs[x];                       \
		}							\
	    }
	    
	    if (xblitter->bitsPerPixel == 8) {
		/* For 8 bit data */
		char *scaledData = (char*)xblitter->scaledData;
		char  *inData    = (char*)data;
		SCALE
	    } else if (xblitter->bitsPerPixel == 16) {
		/* For 16 bit data */
		short *scaledData  = (short *)xblitter->scaledData;
		short *inData      = (short *)data;
		SCALE
	    } else {
		/* For 32 bit data */
		int *scaledData  = (int *)xblitter->scaledData;
		int *inData      = (int *)data;
		SCALE
	    }
	    xblitter->ximage->data = (char*) xblitter->scaledData;
	} else  { /* No scaling required */
	    if (xblitter->shmActive) {
		int y;
		char * scaledData = xblitter->scaledData;
		char * origData = (void *) data;
		int aes = xblitter->bitsPerPixel / 8;
		
		for (y = 0; y < xblitter->outHeight; y++) {
		    memcpy(scaledData, (void *)origData, xblitter->outWidth * aes);
		    scaledData += xblitter->outWidth * aes;
		    origData += xblitter->inStride * aes;
		}
		
	    } else 
		xblitter->ximage->data = (char*) data;
	}
	
	awtLock(env, xblitter);
	if (xblitter->shmActive == 0) {
	    XPutImage(xblitter->display, xblitter->xid, xblitter->gc,
		      xblitter->ximage, 0, 0, 0, 0,
		      xblitter->outWidth, xblitter->outHeight);
	} else {
	    XEvent	event;
	    XShmPutImage(xblitter->display, xblitter->xid, xblitter->gc,
			 xblitter->ximage,
			 0, 0, 0, 0, xblitter->outWidth, xblitter->outHeight, True);
	    XIfEvent(xblitter->display, &event,
		     imageComplete, (char *) &xblitter->shmComplete);
	}
	awtUnlock(env, xblitter);
    }
    if (dataBytes == 0) {
	if (aes == 4)
	    (*env)->ReleaseIntArrayElements(env, (jintArray)dataArray, data, JNI_ABORT);
	else if (aes == 2)
	    (*env)->ReleaseShortArrayElements(env, (jshortArray)dataArray, data, JNI_ABORT);
	else
	    (*env)->ReleaseByteArrayElements(env, (jbyteArray)dataArray, data, JNI_ABORT);
    }
    return 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XLibRenderer_xlibLittleEndian(JNIEnv *env,
								 jobject blitter)
{
    int one = 1;
    if (*((char *) &one) == 1)
	return (jboolean) 1;
    else
	return (jboolean) 0;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_XLibRenderer_xlibFree(JNIEnv *env,
							jobject blitter)
{
  XlibBlitter *xblitter = (XlibBlitter*) GetIntField(env, blitter, "blitter");
  if (xblitter) {
    if (xblitter->shmActive)
      destroySharedMemory(xblitter);
    /*	Clean up intermediate buffer */
    if (xblitter->scaledData) {
      free(xblitter->scaledData);
      xblitter->scaledData = NULL;
    }
    /*	Clean up scaling arrays  */
    if (xblitter->xincs) {
      free(xblitter->xincs);
      xblitter->xincs = NULL;
	}
    
    if (xblitter->yincs) {
      free(xblitter->yincs);
      xblitter->yincs = NULL;
    }
    /*	Release global references to java objects  */
    if (xblitter->surface) {
      (*env)->DeleteGlobalRef(env, xblitter->surface);
      xblitter->surface = NULL;
    }
    
    if ( jawt == 1 && xblitter->awtObj != 0) {
      freeJAWT(env, xblitter);
      xblitter->awtObj = 0;
      xblitter->drawingSurface = 0;
    }
    /*	Free the XLibBlitter structure */
    free(xblitter);
    xblitter = NULL;
    SetIntField(env, blitter, "blitter", 0);
    
  }
}
