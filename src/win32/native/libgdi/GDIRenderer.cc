/*
 * @(#)GDIRenderer.cc	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <malloc.h>
#include <windows.h>
#include <vfw.h>
#include <jni-util.h>
#include "com_sun_media_renderer_video_GDIRenderer.h"

typedef struct {
    BOOL                    bActive;		      // is application active?
    HWND                    hwnd;		      // Handle to the window
    int                     inWidth;		      // Input width
    int                     inHeight;		      // Input height
    int                     outWidth;		      // Output width
    int                     outHeight;		      // Output height
    int                     inStride;		      // Stride for input lines
    int                     bitsPerPixel;	      // For display adapter
    int                     bpp;		      // bytes per pixel
    unsigned int            rMask;
    unsigned int            gMask;
    unsigned int            bMask;
    jobject                 component;
    BITMAPINFO             *bmi;
    HDC                     hdc;
    void *                  bits;
    HDRAWDIB                drawdib;
    int                     lastDstWidth;
    int                     lastDstHeight;
    int                     lastBytesPerPixel;
    
} GDIBlitter;

/*************************************************************************
 * Implementation for GDI Blitter - on win32
 *************************************************************************/

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_GDIRenderer_gdiInitialize(JNIEnv *env,
							    jobject gdiRenderer)
{
    int rmask = 0x00FF0000;
    int gmask = 0x0000FF00;
    int bmask = 0x000000FF;
    int bitsPerPixel, rasterCaps;

    HWND hRootWindow   = ::GetDesktopWindow();
    HDC  hdcRootWindow = ::GetDC(hRootWindow);
    bitsPerPixel = ::GetDeviceCaps(hdcRootWindow, BITSPIXEL);
    rasterCaps   = ::GetDeviceCaps(hdcRootWindow, RASTERCAPS);    
    ::ReleaseDC(hRootWindow, hdcRootWindow);

    if (bitsPerPixel == 15) {
	rmask = 0x00007C00;
	gmask = 0x000003E0;
	bmask = 0x0000001F;
    } else if (bitsPerPixel == 16) {
	rmask = 0x0000F800;
	gmask = 0x000007E0;
	bmask = 0x0000001F;
    } else if (bitsPerPixel == 24) {
	rmask = 0x00FF0000;
	gmask = 0x0000FF00;
	bmask = 0x000000FF;
    }
    
    // Set the values in the FastBlt class
    SetIntField(env, gdiRenderer, "defrMask", rmask);
    SetIntField(env, gdiRenderer, "defgMask", gmask);
    SetIntField(env, gdiRenderer, "defbMask", bmask);
    SetIntField(env, gdiRenderer, "defbitsPerPixel", bitsPerPixel);

    GDIBlitter * blitter = new GDIBlitter;
    blitter->hwnd = (HWND) 0;
    blitter->component = NULL;
    blitter->hdc = NULL;
    blitter->bmi = NULL;
    blitter->drawdib = NULL;
    
    SetIntField(env, gdiRenderer, "blitter", (int) blitter);
    
    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_GDIRenderer_gdiSetComponent(JNIEnv *env,
							      jobject gdiRenderer,
							      jint handle)
{
    GDIBlitter * blitter = (GDIBlitter*) GetIntField(env, gdiRenderer, "blitter");
    if (blitter == NULL)
	return 0;
    blitter->hwnd = (HWND) handle;
    return 1; // So far so good.
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_GDIRenderer_gdiSetOutputSize(JNIEnv *env,
							       jobject gdiRenderer,
							       jint width,
							       jint height)
{
    GDIBlitter * blitter = (GDIBlitter*) GetIntField(env, gdiRenderer, "blitter");
    if (blitter == NULL)
	return 0;
    if (width < 1)
	width = 1;
    if (height < 1)
	height = 1;
    blitter->outWidth = width;
    blitter->outHeight = height;
    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_GDIRenderer_gdiDraw(JNIEnv *env,
						      jobject gdiRenderer,
						      jobject buffer,
						      jlong dataBytes,
						      jint elSize,
						      jint bytesPerPixel,
						      jint srcWidth,
						      jint srcHeight,
						      jint srcStride,
						      jint dstWidth,
						      jint dstHeight,
						      jint rMask,
						      jint gMask,
						      jint bMask,
						      jboolean flipped,
						      jint windowHandle)
{
    GDIBlitter * blitter = (GDIBlitter*) GetIntField(env, gdiRenderer, "blitter");
    if (blitter == NULL)
	return 0;

    // Do the handle stuff
    if (blitter->hwnd != (HWND) windowHandle) {
	if (blitter->hdc)
	    ReleaseDC(blitter->hwnd, blitter->hdc);
	blitter->hwnd = (HWND) windowHandle;
	blitter->hdc = GetDC(blitter->hwnd);
    }    
    if (blitter->hwnd == NULL) {
	printf("Don't have window handle\n");
	return 0;
    }

    HDC hdc = blitter->hdc;
    void *bitmapData = (void*) dataBytes;

    if (dataBytes == 0) {
	if (elSize == 1)
	    bitmapData = (void *) env->GetByteArrayElements((jbyteArray) buffer, 0);
	else if (elSize == 2)
	    bitmapData = (void *) env->GetShortArrayElements((jshortArray) buffer, 0);
	else if (elSize == 3)
	    bitmapData = (void *) env->GetByteArrayElements((jbyteArray) buffer, 0);
	else if (elSize == 4)
	    bitmapData = (void *) env->GetIntArrayElements((jintArray) buffer, 0);
    }
    
    int size = sizeof(BITMAPINFOHEADER) + sizeof(RGBQUAD) * 3;
    if (blitter->bmi == NULL)
	blitter->bmi = (BITMAPINFO *) malloc(size);

    //if (bytesPerPixel == 3)
    //	srcStride /= 3;

    BITMAPINFO *bmi = blitter->bmi;
    BITMAPINFOHEADER *bmih = (BITMAPINFOHEADER *) bmi;
    DWORD *masks = (DWORD*) (bmih+1);
    bmih->biSize = size - 12; //((bytesPerPixel % 2) == 0? 0 : 12) ;
    bmih->biWidth = srcStride;
    if (flipped)
	bmih->biHeight = srcHeight;
    else
	bmih->biHeight = -srcHeight;
    bmih->biPlanes = 1;
    bmih->biBitCount = bytesPerPixel * 8;
    //if (bytesPerPixel == 3)
    bmih->biCompression = BI_RGB;
    if ((bytesPerPixel % 2) == 0)
	bmih->biCompression = BI_BITFIELDS;
    bmih->biSizeImage = srcStride * srcHeight * bytesPerPixel;
    bmih->biClrUsed = 0;
    bmih->biClrImportant = 0;
    bmih->biXPelsPerMeter = 10000;
    bmih->biYPelsPerMeter = 10000;
    masks[0] = rMask;
    masks[1] = gMask;
    masks[2] = bMask;

    /*
    if (blitter->drawdib == NULL) {
	blitter->drawdib = DrawDibOpen();
    }
    */
    /* DRAWDIB if possible */
    if (blitter->drawdib != NULL && flipped &&
	(bytesPerPixel != 2 || rMask != 0x7C00)) {
	if (dstWidth != blitter->lastDstWidth ||
	    dstHeight != blitter->lastDstHeight ||
	    bytesPerPixel != blitter->lastBytesPerPixel) {
	    blitter->lastDstWidth = dstWidth;
	    blitter->lastDstHeight = dstHeight;
	    blitter->lastBytesPerPixel = bytesPerPixel;
	    
	    DrawDibBegin(blitter->drawdib, hdc,
			 dstWidth, dstHeight,
			 bmih,
			 srcWidth, srcHeight,
			 0);
	}
	
	DrawDibDraw(blitter->drawdib, hdc,
		    0, 0, dstWidth, dstHeight,
		    bmih, bitmapData,
		    0, 0, srcWidth, srcHeight,
		    DDF_SAME_DRAW);
    } else {

	if (  srcWidth == dstWidth &&
	      srcHeight == dstHeight) {
	    
	    ::SetDIBitsToDevice(hdc, 0, 0, srcWidth, srcHeight, 0, 0, 
				0, srcHeight, bitmapData, bmi, DIB_RGB_COLORS);
	    // printf("Drew = %d\n", result);
	} else {
	    //::SetStretchBltMode(hdc, WHITEONBLACK);
	    ::SetMapMode(hdc, MM_TEXT);
	    ::StretchDIBits(hdc, 0, 0, dstWidth, dstHeight,
			    0, 0, srcWidth, srcHeight,
			    bitmapData, bmi, DIB_RGB_COLORS, SRCCOPY);
	}
    }

    if (dataBytes == 0) {
	if (elSize == 1)
	    env->ReleaseByteArrayElements((jbyteArray) buffer,
					  (signed char *) bitmapData, 0);
	else if (elSize == 2)
	    env->ReleaseShortArrayElements((jshortArray) buffer,
					   (short *) bitmapData, 0);
	else if (elSize == 3)
	    env->ReleaseByteArrayElements((jbyteArray) buffer,
					  (signed char *) bitmapData, 0);
	else if (elSize == 4)
	    env->ReleaseIntArrayElements((jintArray) buffer,
					 (long *) bitmapData, 0);
    }
    // ReleaseDC(hdc, blitter->hwnd);
    return 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_GDIRenderer_gdiFree(JNIEnv *env,
					       jobject gdiRenderer)
{
    GDIBlitter * blitter = (GDIBlitter*) GetIntField(env, gdiRenderer, "blitter");
    if (blitter == NULL)
	return 0;
    if (blitter->bmi != NULL)
	free(blitter->bmi);
    blitter->bmi = NULL;

    if (blitter->drawdib != NULL) {
	DrawDibClose(blitter->drawdib);
	blitter->drawdib = NULL;
    }

    if (blitter->hdc != NULL)
	ReleaseDC(blitter->hwnd, blitter->hdc);
    blitter->hdc = NULL;
    
    SetIntField(env, gdiRenderer, "blitter", 0);
    
    return 1;
}
