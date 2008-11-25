/*
 * @(#)ln_disp.c	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifdef WIN32
#include <windows.h>
#endif

#include <stdio.h>
#include "mp_mpd.h"
#include "ln_lnd.h"
#include "yrc.h"
#include <jni-util.h>

extern jobject Jmpx_getImage(typ_mpgbl *gb, int width, int height, jimage_t *im);
extern void Jmpx_releaseImage(typ_mpgbl *gb, jimage_t *im);
extern void Jmpx_displayImage(typ_mpgbl *gb);

typedef int (*BltCreate)(void **);
typedef int (*BltGetDisplayFormat)(void *, int *, int *, int *, int *);
typedef int (*BltSetWindow)(void *, int);
typedef int (*BltSetInputSize)(void *, int, int, int);
typedef int (*BltSetOutputSize)(void *, int, int);
typedef int (*BltGetSurface)(void *, void **, int *);
typedef int (*BltReleaseSurface)(void *);
typedef int (*BltClose)(void *);

BltCreate bltCreate = NULL;
BltGetDisplayFormat bltGetDisplayFormat = NULL;
BltSetWindow bltSetWindow = NULL;
BltSetInputSize bltSetInputSize = NULL;
BltSetOutputSize bltSetOutputSize = NULL;
BltGetSurface bltGetSurface = NULL;
BltReleaseSurface bltReleaseSurface = NULL;
BltClose bltClose = NULL;

static jmethodID mid_getPeer = NULL;
static jmethodID mid_getDrawingSurfaceInfo = NULL;
static jmethodID mid_lock = NULL;
static jmethodID mid_unlock = NULL;
static jmethodID mid_getHWnd = NULL;

static SEMA displayLock = {NULL, 0};

void
awtLock(JNIEnv *env, jobject component, jobject *objSurface)
{   
    jobject peer, surface;

    if (mid_getPeer == NULL)
	mid_getPeer = GetMethodID(env, component, "getPeer", 
				  "()Ljava/awt/peer/ComponentPeer;");
    if (mid_getPeer) {
	peer = (*env)->CallObjectMethod(env, component, mid_getPeer, NULL);
	if (mid_getDrawingSurfaceInfo == NULL)
	    mid_getDrawingSurfaceInfo = GetMethodID(env, peer,
					"getDrawingSurfaceInfo",
				        "()Lsun/awt/DrawingSurfaceInfo;");
	if (mid_getDrawingSurfaceInfo) {
	    surface = (*env)->CallObjectMethod(env, peer,
					       mid_getDrawingSurfaceInfo,
					       NULL);
	    *objSurface = surface;
	    /* Lock the drawing surface data */
	    if (mid_lock == NULL)
		mid_lock = GetMethodID(env, surface, "lock", "()I");
	    if (mid_unlock == NULL)
		mid_unlock = GetMethodID(env, surface, "unlock", "()V");
	    if (mid_lock)
		(*env)->CallIntMethod(env, surface, mid_lock, NULL);
	}
    }
}

void
awtUnlock(JNIEnv *env, jobject surface)
{
    if (surface != NULL && mid_unlock != NULL)
	(*env)->CallVoidMethod(env, surface, mid_unlock, NULL);
}

static int
getWindowHandle(JNIEnv *env, jobject component)
{
    jobject peer, surface;
    int returnVal;

    if (mid_getPeer == NULL)
	mid_getPeer = GetMethodID(env, component, "getPeer",
				  "()Ljava/awt/peer/ComponentPeer;");
    if (mid_getPeer == NULL)
	return 0;
    peer = (*env)->CallObjectMethod(env, component, mid_getPeer, NULL);
    if (peer == NULL)
	return 0;
    if (mid_getDrawingSurfaceInfo == NULL)
	mid_getDrawingSurfaceInfo = GetMethodID(env, peer,
					"getDrawingSurfaceInfo",
					"()Lsun/awt/DrawingSurfaceInfo;");
    if (mid_getDrawingSurfaceInfo == NULL)
	return 0;
    surface = (*env)->CallObjectMethod(env, peer,
				       mid_getDrawingSurfaceInfo, NULL);
    if (surface == NULL)
	return 0;
    
    /* Lock the drawing surface data */
    if (mid_lock == NULL)
	mid_lock = GetMethodID(env, surface, "lock", "()I");
    if (mid_lock == NULL)
	return 0;
    (*env)->CallIntMethod(env, surface, mid_lock);
    /* Get the HWND */
    if (mid_getHWnd == NULL)
	mid_getHWnd = GetMethodID(env, surface, "getHWnd", "()I");
    if (mid_getHWnd == NULL)
	return 0;
    returnVal =   (int) (*env)->CallIntMethod(env, surface, mid_getHWnd, NULL);
    /* Unlock the drawing surface */
    if (mid_unlock == NULL)
	mid_unlock = GetMethodID(env, surface, "unlock", "()V");
    if (mid_unlock == NULL)
	return 0;
    (*env)->CallVoidMethod(env, surface, mid_unlock, NULL);
    return returnVal;
}

nint
init_video_policy(typ_mpgbl *gb)
{
#ifdef WIN32
    /* Load the blitter */
    HINSTANCE library = NULL;
    unsigned int rmask = 0;
    unsigned int gmask;
    unsigned int bmask;
    int depth = 0;
    if (displayLock.jobj == NULL) {
	SEMA_INIT(gb->mpx_env, &displayLock, 1, 0, NULL);
    }
    gb->blitter = NULL;
    if (bltCreate == NULL) {
	if (LoadLibrary("ddraw"))
	    library = LoadLibrary("jmdx");
	if (library != NULL) {
	    bltCreate = (BltCreate)
		GetProcAddress(library, "dxCreate");
	    bltGetDisplayFormat = (BltGetDisplayFormat)
		GetProcAddress(library, "dxGetDisplayFormat");
	    bltSetWindow = (BltSetWindow)
		GetProcAddress(library, "dxSetWindow");
	    bltSetInputSize = (BltSetInputSize)
		GetProcAddress(library, "dxSetInputSize");
	    bltSetOutputSize = (BltSetOutputSize)
		GetProcAddress(library, "dxSetOutputSize");
	    bltGetSurface = (BltGetSurface)
		GetProcAddress(library, "dxGetSurface");
	    bltReleaseSurface = (BltReleaseSurface)
		GetProcAddress(library, "dxReleaseSurface");
	    bltClose = (BltClose)
		GetProcAddress(library, "dxClose");
	    printf("DX.bltCreate = %d\n", bltCreate);
	} else {
	    library = LoadLibrary("jmgdi");
	    if (library != NULL) {
		bltCreate = (BltCreate)
		    GetProcAddress(library, "gdiCreate");
		bltGetDisplayFormat = (BltGetDisplayFormat)
		    GetProcAddress(library, "gdiGetDisplayFormat");
		bltSetWindow = (BltSetWindow)
		    GetProcAddress(library, "gdiSetWindow");
		bltSetInputSize = (BltSetInputSize)
		    GetProcAddress(library, "gdiSetInputSize");
		bltSetOutputSize = (BltSetOutputSize)
		    GetProcAddress(library, "gdiSetOutputSize");
		bltGetSurface = (BltGetSurface)
		    GetProcAddress(library, "gdiGetSurface");
		bltReleaseSurface = (BltReleaseSurface)
		    GetProcAddress(library, "gdiReleaseSurface");
		bltClose = (BltClose)
		    GetProcAddress(library, "gdiClose");
		printf("GDI.bltCreate = %d\n", bltCreate);
	    }
	}
    }
    
    if (bltCreate) {
	if (gb->blitter == NULL) {
	    if (!(*bltCreate)(&(gb->blitter))) {
		printf("bltCreate failed\n");
	    } else if (!(*bltGetDisplayFormat)(gb->blitter, &rmask,
					       &gmask, &bmask,
					       &depth)) {
		printf("bltGetDisplayFormat failed\n");
	    }
	}
    }
    
    if (rmask != 0 && depth > 8) {
	YR_initialize(&(gb->converter), rmask, gmask, bmask, depth);
    }
    
#else

/*
    YR_initialize(&(gb->converter), 0x000000FF, 0x0000FF00, 0x00FF0000, 32);
*/

#endif

    gb->vdec.vdmflags = VDMF_GENERIC | VDMF_24;
    return 1;
}

void
ccnv_disp(typ_mpgbl *gb, ubyte *plm, ubyte *pcr, ubyte *pcb, nint xsize, nint ysize, nint vdm, nint zoom)
{
    jimage_t image;
    int displayed = 0;
    unsigned int rMask, gMask, bMask;
    int bitsPerPixel;
    jobject surfaceID = NULL;
    /* Get a buffer from java */
    jobject component = Jmpx_getImage(gb, xsize, ysize, &image);

    if (image.buf == 0) {
	return;
    }
    
    if (image.needYUVBuffer) {
	/* copy the y, u and v planes to the rgb buffer */
	/* This is a temporary hack */
	uint32 fullSize = xsize * ysize;
	uint32 quarterSize = (xsize * ysize) >> 2;
	memcpy((void *) image.buf, (void*)plm, xsize * ysize);
	memcpy((void *) (image.buf + quarterSize), (void *)pcb, quarterSize);
	memcpy((void *) (image.buf + quarterSize + (quarterSize>>2)), (void *)pcr, quarterSize);
    } else {
	int stride = xsize;
	/** Color Conversion to RGB **/
#ifdef WIN32
	SEMA_WAIT(gb->mpx_env, &displayLock);
	
	/* Can we also display it ? */
	if (component != NULL) {
	    if (gb->window == 0) {
		gb->windowSet = 0;
		gb->window = getWindowHandle(gb->mpx_env, component);
	    }
	} else
	    gb->window = 0;

	if (gb->window != 0) {
	    if (IsWindow((HWND)gb->window)) {
		/** Can draw **/
		displayed = 1;
		/* prepare the surface to blit into */
		if (gb->blitter) {
		    if (!gb->windowSet) {
			if (!(*bltSetWindow)(gb->blitter, gb->window)) {
			    printf("bltSetWindow failed\n");
			    displayed = 0;
			} else {
			    gb->windowSet = 1;
			    if (!(*bltSetInputSize)(gb->blitter, xsize, ysize,
						    xsize)) {
				printf("bltSetInputSize failed\n");
				displayed = 0;
			    }
			}
		    }
		    if (!(*bltSetOutputSize)(gb->blitter,
					     image.outWidth,
					     image.outHeight)) {
			printf("bltSetOutputSize failed\n");
			displayed = 0;
		    } else if (!(*bltGetSurface)(gb->blitter, &(image.buf),
						 &stride)) {
			/*			printf("bltGetSurface failed\n");*/
			displayed = 0;
		    }
		    if (displayed)
			awtLock(gb->mpx_env, component, &surfaceID);
		}
	    } else {
		gb->window = 0;
		gb->windowSet = 0;
	    }
	}
#else
	displayed = 1;
#endif

	/** Do the color conversion **/
/*
	if (gb->converter) {
	    if (displayed)
		YR_convert(gb->converter, (void *)plm, (void *)image.buf,
			   xsize, ysize, stride, ysize, xsize, ysize,
			   (void *)pcb, (void *)pcr);
	} else
*/
	   c_yuv_to_rgb(plm, pcr, pcb, image.buf, xsize >> 4, ysize >> 1, xsize,
			stride, image.XBGR);

#ifdef WIN32
	if (gb->window != 0 && displayed) {
	    /* Do the actual display */
	    HDC hdc;
	    if (!IsWindow((HWND)gb->window)) {
		displayed = 0;
	    } else {
		int result = (*bltReleaseSurface)(gb->blitter);
		/*		if (!result)*/
		/*		    printf("bltReleaseSurface failed\n");*/
	    }
	} else
	    displayed = 0;
	if (surfaceID != NULL) {
	    awtUnlock(gb->mpx_env, surfaceID);
	}
	SEMA_POST(gb->mpx_env, &displayLock);
#endif
    }

    Jmpx_releaseImage(gb, &image);

#ifndef WIN32
    /* Ask Jmpx to do the display */
    Jmpx_displayImage(gb);
#else
    if (image.needYUVBuffer)
	Jmpx_displayImage(gb);
#endif

    
    gb->exposed = 0;
}

