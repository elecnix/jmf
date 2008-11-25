/*
 * @(#)DDRenderer.cc	1.17 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <malloc.h>
#include "com_sun_media_renderer_video_DDRenderer.h"
#include <jni-util.h>
#include <ColorUtils.h>
#include <windows.h>
#include <ddraw.h>

#define PRINTF(p)
#define PRINTF2(p1,p2)
#define PRINTF3(p1,p2,p3)

// Only one DirectDraw object needed

static LPDIRECTDRAW         directDraw = 0;
static LPDIRECTDRAWSURFACE  screen = 0;
static int nOverlays = 0;

typedef struct {
    LPDIRECTDRAW            dd;			      // DirectDraw object
    LPDIRECTDRAWSURFACE     video;		      // Offscreen surface 1
    LPDIRECTDRAWSURFACE     overlay;
    DDCOLORKEY              colorkey;
    int                     yuvType;
    LPDIRECTDRAWSURFACE     screen;
    LPDIRECTDRAWCLIPPER     clipper;		      // clipper for primary
    LPDIRECTDRAWPALETTE     pal;		      // DirectDraw palette
    BOOL                    bActive;		      // is application active?
    HWND                    hwnd;		      // Handle to the window
    int                     inWidth;		      // Input width
    int                     inHeight;		      // Input height
    int                     outWidth;		      // Output width
    int                     outHeight;		      // Output height
    int                     inStride;		      // Stride for input lines
    int                     bitsPerPixel;	      // For display adapter
    int                     bpp;		      // bytes per pixel
    int                     flipped;
    unsigned int            rMask;
    unsigned int            gMask;
    unsigned int            bMask;
    RECT                    srcRect;
    RECT                    dstRect;
    int                     bltToScreen;
    jobject                 component;
    BOOL                    overlayWorking;

    BOOL                    useGDI;
    HDC                     hdc;                      // DC of window
    BITMAPINFO              *bmi;
} DxBlitter;


// Forward
int gdiDraw(DxBlitter *dx,
	    void *data,
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
	    jint windowHandle);


DWORD
DDColorMatch(COLORREF rgb)
{
    COLORREF                rgbT;
    HDC                     hdc;
    DWORD                   dw = CLR_INVALID;
    DDSURFACEDESC           ddsd;
    HRESULT                 hres;

    //
    //  Use GDI SetPixel to color match for us
    //
    if (rgb != CLR_INVALID && screen->GetDC(&hdc) == DD_OK)
    {
        rgbT = GetPixel(hdc, 0, 0);     // Save current pixel value
        SetPixel(hdc, 0, 0, rgb);       // Set our value
        screen->ReleaseDC(hdc);
    }
    //
    // Now lock the surface so we can read back the converted color
    //
    ddsd.dwSize = sizeof(ddsd);
    while ((hres = screen->Lock(NULL, &ddsd, 0, NULL)) == DDERR_WASSTILLDRAWING)
        ;
    if (hres == DD_OK)
    {
        dw = *(DWORD *) ddsd.lpSurface;                 // Get DWORD
        if (ddsd.ddpfPixelFormat.dwRGBBitCount < 32)
            dw &= (1 << ddsd.ddpfPixelFormat.dwRGBBitCount) - 1;  // Mask it to bpp
        screen->Unlock(NULL);
    }
    //
    //  Now put the color that was there back.
    //
    if (rgb != CLR_INVALID && screen->GetDC(&hdc) == DD_OK)
    {
        SetPixel(hdc, 0, 0, rgbT);
        screen->ReleaseDC(hdc);
    }
    return dw;
}

DDCOLORKEY
DDGetColorKey(COLORREF rgb)
{
    DDCOLORKEY              ddck;

    ddck.dwColorSpaceLowValue = DDColorMatch(rgb);
    ddck.dwColorSpaceHighValue = ddck.dwColorSpaceLowValue;
    return ddck;
}

/*************************************************************************
 * Implementation for DDRenderer - WIN32 fast blitter using DirectDraw
 *************************************************************************/

DWORD fccYUY2 = MAKEFOURCC('Y', 'U', 'Y', '2');
DWORD fccUYVY = MAKEFOURCC('U', 'Y', 'V', 'Y');
DWORD prefFCC = 0;
DWORD YPOS = 0;
DWORD UPOS = 1;
DWORD Y2POS = 2;
DWORD VPOS = 3;

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_DDRenderer_dxInitialize(JNIEnv *env,
							  jobject ddrenderer)
{
    HRESULT retVal;
    DDSURFACEDESC ddsd;
    int rmask, gmask, bmask;
    int bitsPerPixel;
    int i;
    DWORD prefFCC = 0;
    DWORD codes[6];
    DWORD nCodes = 6;
    HRESULT result = 0;

    PRINTF("In dxInitialize\n");
    // Try to initialize direct draw if not already initialized
    if (directDraw == 0) {
	// directDraw variable is static because we can only initialize
	// Direct Draw once per application instance.
	retVal = DirectDrawCreate(NULL, &directDraw, NULL);
	// If it failed, return 0
	if (retVal != DD_OK)
	    goto abort;
    }

    // Get the display mode details
    
    ddsd.dwSize = sizeof(ddsd);
    retVal = directDraw->GetDisplayMode(&ddsd);
    if (retVal != DD_OK)
	goto abort;
    // Get the pixel depth from DD
    bitsPerPixel = ddsd.ddpfPixelFormat.dwRGBBitCount;
    if (bitsPerPixel == 15)
	bitsPerPixel = 16;
    // Get the RGB masks from DD
    if (bitsPerPixel != 8) {
	rmask = ddsd.ddpfPixelFormat.dwRBitMask;
	gmask = ddsd.ddpfPixelFormat.dwGBitMask;
	bmask = ddsd.ddpfPixelFormat.dwBBitMask;
    } else {
	rmask = 0x000000FF;
	gmask = 0x0000FF00;
	bmask = 0x00FF0000;
    }

    codes[0] = codes[1] = codes[2] = codes[3] = codes[4] = codes[5] = 0;
    result = directDraw->GetFourCCCodes(&nCodes, codes);
    for (i = 0; i < nCodes; i++) {
	if (codes[i] == fccYUY2) {
	    // we prefer this
	    prefFCC = codes[i];
	    YPOS = 0;
	    UPOS = 1;
	    Y2POS = 2;
	    VPOS = 3;
	    break;
	} else if (codes[i] == fccUYVY) {
	    // this one's ok too
	    prefFCC = codes[i];
	    UPOS = 0;
	    YPOS = 1;
	    VPOS = 2;
	    Y2POS = 3;
	    break;
	}
    }

    if (prefFCC != 0) {
	SetIntField(env, ddrenderer, "fccSupported",
		    GetIntField(env, ddrenderer, "YUYV"));
    }
    
    // Set the values in the DDRenderer class
    SetIntField(env, ddrenderer, "defrMask", rmask);
    SetIntField(env, ddrenderer, "defgMask", gmask);
    SetIntField(env, ddrenderer, "defbMask", bmask);
    SetIntField(env, ddrenderer, "defbitsPerPixel", bitsPerPixel);

    PRINTF("Out dxInitialize\n");
    // DirectX is available
    return 1;

    // Something went wrong
abort:

    PRINTF("Aborting dxInitialize\n");
    directDraw = 0;
    return 0;
}


/**
 * A new window handle has been registered.
 */
JNIEXPORT jboolean JNICALL 
Java_com_sun_media_renderer_video_DDRenderer_dxSetComponent(JNIEnv *env,
							  jobject ddrenderer,
							  jint windowHandle)
{
    HRESULT        retVal;
    DDSURFACEDESC  ddsd;
    DDPIXELFORMAT  ddpf;
    
    // Create a DxBlitter object.
    DxBlitter * dx = (DxBlitter *) malloc(sizeof(DxBlitter));
    PRINTF("In dxSetComponent...");
    if (dx == NULL)
	return (jboolean) 0;
    
    dx->hwnd = (HWND) windowHandle;
    dx->hdc = 0;
    dx->useGDI = FALSE;
    dx->bmi = NULL;
    dx->dd = directDraw;
    
    // Lets work in window mode
    retVal = (dx->dd)->SetCooperativeLevel(dx->hwnd, DDSCL_NORMAL);
    if (retVal != DD_OK) {
	dx->useGDI = TRUE;
	PRINTF("Switching to GDI Rendering !!!!!!!!!!!!!!!!!!!\n");
    }
    // screen is a static variable because we can have only one instance of
    // the primary surface.
    if (!dx->useGDI) {
	if (screen == 0) {
	    // Prepare to create primary surface (screen) 
	    ddsd.dwSize = sizeof(ddsd);
	    ddsd.dwFlags = DDSD_CAPS;
	    ddsd.ddsCaps.dwCaps = DDSCAPS_PRIMARYSURFACE;

	    // Try to create the surface
	    retVal = (dx->dd)->CreateSurface(&ddsd, &screen, NULL);
	    if (retVal != DD_OK) {
		// Couldn't create the surface. Oops!
		PRINTF("Couldn't create primary surface\n");
		dx->useGDI = TRUE;
		PRINTF("Switching to GDI Rendering !!!!!!!!!!!!!!!!!!!\n");
		//free(dx);
		//return 0;
	    }
	} else {
	    retVal = screen->Restore();
	    if (retVal != DD_OK) {
		PRINTF("Couldn't restore primary surface\n");
		dx->useGDI = TRUE;
		PRINTF("Switching to GDI Rendering !!!!!!!!!!!!!!!!!!!\n");
	    }
	}
    }
    // Use the pixel depth from FastBlt
    dx->bitsPerPixel = GetIntField(env, ddrenderer, "bitsPerPixel");
    dx->bpp = dx->bitsPerPixel / 8;

    if (!dx->useGDI) {
	// Try to create a clipper for the window
	retVal = (dx->dd)->CreateClipper(0, &(dx->clipper), NULL);
	if (retVal != DD_OK) {
	    // Couldn't create clipper.
	    PRINTF("Couldn't create clipper\n");
	    free(dx);
	    return 0;
	}
	
	// Tell the clipper which window we need to clip to
	retVal = (dx->clipper)->SetHWnd( 0, dx->hwnd );
	if (retVal != DD_OK) {
	    // Couldn't set window handle
	    PRINTF("Couldn't set window for clipper\n");
	    free(dx);
	    return 0;
	}
	
	// Try to set the clipper for the primary surface.  
	retVal = screen->SetClipper(dx->clipper);
	if (retVal != DD_OK) {
	    // Couldn't set clipper on surface
	    PRINTF("Couldn't set clipper on window\n");
	    free(dx);
	    return 0;
	}
    }
    
    dx->video = NULL;
    dx->overlay = NULL;
    dx->yuvType = 0;
    dx->hdc = 0;
    dx->bmi = NULL;
    SetIntField(env, ddrenderer, "blitter", (int) dx);
    PRINTF("2.\n");
    return 1; // So far so good.
}


static int
overlayAvailable()
{
    if (directDraw == 0)
	return 0;
    // Retrieve the number of available overlay surfaces
    DDCAPS caps;
    caps.dwSize = sizeof(caps);
    directDraw->GetCaps(&caps, NULL);
    //PRINTF3("max = %d, curr = %d\n", caps.dwMaxVisibleOverlays,
    //   caps.dwCurrVisibleOverlays);
    if (nOverlays > 0)
	return 0;
    
    if (caps.dwMaxVisibleOverlays - caps.dwCurrVisibleOverlays > 0)
	return 1;
    else
	return 0;
}

void
releaseSurfaces(DxBlitter *dx)
{
    // Release any surface created earlier.
    if (dx->video != NULL) {
	dx->video->Release();
	dx->video = NULL;
    }
    if (dx->overlay != NULL) {
	dx->overlay->Release();
	dx->overlay = NULL;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_DDRenderer_dxSetFourCCInputFormat(JNIEnv *env,
								    jobject ddrenderer,
								    jint width,
								    jint height,
								    jint yuvType)
{
    DxBlitter *dx = (DxBlitter *) GetIntField(env, ddrenderer, "blitter");
    HRESULT retVal;
    DDSURFACEDESC ddsd;
    DDPIXELFORMAT DDPF_UYVY = {sizeof(DDPIXELFORMAT),
			       DDPF_FOURCC,MAKEFOURCC('U','Y','V','Y'),0,0,0,0,0};
    DDPIXELFORMAT DDPF_YUY2 = {sizeof(DDPIXELFORMAT),
			       DDPF_FOURCC,MAKEFOURCC('Y','U','Y','2'),0,0,0,0,0};
    if (dx != NULL) {
	// Store the sizes
	dx->inWidth = width;
	dx->inHeight = height;
	dx->yuvType = yuvType;

	releaseSurfaces(dx);

	ddsd.dwSize = sizeof(ddsd);
	ddsd.ddsCaps.dwCaps = DDSCAPS_OFFSCREENPLAIN ;
	ddsd.dwWidth = width;
	ddsd.dwHeight = height;

	ddsd.ddpfPixelFormat = DDPF_YUY2;

	// Set the flags for relevant properties
	ddsd.dwFlags = DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT;

	dx->overlay = NULL;

	// Try to create an overlay surface if possible
	if (overlayAvailable()) {
	    DDCOLORKEY ddck = DDGetColorKey(RGB(255, 0, 255));
	    dx->colorkey = ddck;
	    ddsd.ddsCaps.dwCaps = DDSCAPS_OVERLAY | DDSCAPS_VIDEOMEMORY;
	    retVal = dx->dd->CreateSurface(&ddsd, &(dx->overlay), NULL);
	    if (retVal == DD_OK)
		dx->overlay->SetColorKey(DDCKEY_DESTOVERLAY, &ddck);
	    nOverlays++;
	} else
	    retVal = !DD_OK;

	
	// If overlay surface failed, try a normal surface
	if (retVal != DD_OK) {
	    dx->overlay = NULL;
	    ddsd.ddsCaps.dwCaps = DDSCAPS_OFFSCREENPLAIN;
	    retVal = dx->dd->CreateSurface(&ddsd, &(dx->video), NULL);
	} else {
	    PRINTF("Created yuv overlay surface!");
	}
	
	if (retVal != DD_OK) {
	    PRINTF("Could not create video surface\n");
	    return 0;
	}
    }
    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_DDRenderer_dxSetInputFormat(JNIEnv *env,
							      jobject ddrenderer,
							      jint width,
							      jint height,
							      jint strideX,
							      jint bitsPerPixel,
							      jint rMask,
							      jint gMask,
							      jint bMask,
							      jboolean flipped)
{
    // Get the blitter from java
    DxBlitter *dx = (DxBlitter *) GetIntField(env, ddrenderer, "blitter");
    
    HRESULT retVal;
    DDSURFACEDESC ddsd;
    PRINTF("In dxSetInputFormat...");
    
    if (dx != NULL) {
	PRINTF("1...");
	// Store the sizes
	dx->inWidth = width;
	dx->inHeight = height;
	dx->inStride = strideX;
	dx->bitsPerPixel = bitsPerPixel;
	dx->rMask = rMask;
	dx->gMask = gMask;
	dx->bMask = bMask;
	
	if (!dx->useGDI)
	    releaseSurfaces(dx);
	
	// Prepare to create a new video surface
	ddsd.dwSize = sizeof(ddsd);
	ddsd.ddsCaps.dwCaps = DDSCAPS_OFFSCREENPLAIN;
	ddsd.dwWidth = strideX;
	ddsd.dwHeight = height;
	if (flipped)
	    dx->flipped = 1;
	else
	    dx->flipped = 0;

	ddsd.dwAlphaBitDepth = 0;
	
	// Set the pixel format   !!!!!! Ignored for now. 
	ddsd.ddpfPixelFormat.dwSize = sizeof(DDPIXELFORMAT);
	ddsd.ddpfPixelFormat.dwRGBBitCount = bitsPerPixel;
	ddsd.ddpfPixelFormat.dwRBitMask = rMask;
	ddsd.ddpfPixelFormat.dwGBitMask = gMask;
	ddsd.ddpfPixelFormat.dwBBitMask = bMask;
	ddsd.ddpfPixelFormat.dwRGBAlphaBitMask = 0x00;
	ddsd.ddpfPixelFormat.dwFlags = DDPF_RGB;

	// Set the flags for relevant properties
	ddsd.dwFlags = DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH; // | DDSD_PIXELFORMAT;

	dx->overlay = NULL;

	if (!dx->useGDI) {
	    PRINTF("2...");
	    // Try to create an overlay surface if possible
	    if (overlayAvailable()) {
		DDCOLORKEY ddck = DDGetColorKey(RGB(255, 0, 255));
		dx->colorkey = ddck;
		ddsd.ddsCaps.dwCaps = DDSCAPS_OVERLAY | DDSCAPS_VIDEOMEMORY;
		retVal = dx->dd->CreateSurface(&ddsd, &(dx->overlay), NULL);
		if (retVal == DD_OK)
		    dx->overlay->SetColorKey(DDCKEY_DESTOVERLAY, &ddck);
		nOverlays++;
	    } else
		retVal = !DD_OK;
	    
	    
	    // If overlay surface failed, try a normal surface
	    if (retVal != DD_OK) {
		PRINTF("creating normal surface...");
		dx->overlay = NULL;
		ddsd.ddsCaps.dwCaps = DDSCAPS_OFFSCREENPLAIN;
		retVal = dx->dd->CreateSurface(&ddsd, &(dx->video), NULL);
	    } else {
		//PRINTF("Created overlay surface!");
	    }
	    
	    if (retVal != DD_OK) {
		PRINTF("Could not create video surface\n");
		dx->useGDI = TRUE;
		PRINTF("Switching to GDI Rendering !!!!!!!!!!!!!!!!!!!\n");
	    }
	}
    }
    PRINTF("3\n");

    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_DDRenderer_dxSetOutputSize(JNIEnv *env,
							     jobject ddrenderer,
							     jint width,
							     jint height)
{
    // Get the blitter object from java
    DxBlitter *dx = (DxBlitter *) GetIntField(env, ddrenderer, "blitter");

    // Check
    if (dx == NULL) {
	PRINTF("Dx is null\n");
	return 0;
    }

    // Store the output size
    dx->outWidth = width;
    dx->outHeight = height;

    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_DDRenderer_dxDraw(JNIEnv *env,
						    jobject ddrenderer,
						    jobject array,
						    jlong arrayBytes,
						    jint width, jint height)
{
    DxBlitter *dx = (DxBlitter *) GetIntField(env, ddrenderer, "blitter");
    char    *imageData = (char *) arrayBytes;
    char    *imageDataCopy;
    HRESULT retVal;
    DDSURFACEDESC ddsd;
    RECT    srcRect;
    RECT    dstRect;
    POINT   pt;
    jmethodID unlockID;
    jobject surface;
    int     bltToScreen = 1;
    RGNDATA *clipRegion = NULL;
    DWORD   crSize;
    int     offX = GetIntField(env, ddrenderer, "offX");
    int     offY = GetIntField(env, ddrenderer, "offY");
    int     bytesPerPixel = GetIntField(env, ddrenderer, "bytesPerPixel");
    PRINTF("in dxdraw...");
    if (dx == NULL) {
	PRINTF("Dx is null to bltDoBlt() \n");
	return 0;
    }
    if (array == NULL)
	return 0;

    if (arrayBytes == 0) {
	if (bytesPerPixel == 1) {
	    imageData = (char*) env->GetByteArrayElements((jbyteArray)array, 0);
	} else if (bytesPerPixel == 2) {
	    imageData = (char*) env->GetShortArrayElements((jshortArray)array, 0);
	} else if (bytesPerPixel == 4) {
	    imageData = (char*) env->GetIntArrayElements((jintArray)array, 0);
	}
    }
    
    imageDataCopy = (char*) imageData;
    dx->outWidth = width;
    dx->outHeight = height;

    if (!dx->useGDI)
	screen->SetClipper(dx->clipper);
    srcRect.top = 0;
    srcRect.left = 0;
    srcRect.right = dx->inWidth;
    srcRect.bottom = dx->inHeight;

    // unlockID = awtLock(env, component, &surface);
    
    GetClientRect(dx->hwnd, &dstRect);
    pt.x = pt.y = 0;
    ClientToScreen(dx->hwnd, &pt);
    OffsetRect(&dstRect, pt.x, pt.y);
    dstRect.right = dstRect.left + dx->outWidth;
    dstRect.bottom = dstRect.top + dx->outHeight;
    OffsetRect(&dstRect, offX, offY);
    PRINTF("1...");
    if (!dx->useGDI) {
	PRINTF("2...");
	retVal = dx->clipper->GetClipList(&dstRect, NULL, &crSize);
	if (retVal == DDERR_NOCLIPLIST)
	    goto skip;
	if (crSize == 0 || crSize == sizeof(RGNDATAHEADER))
	    goto skip;
	
	clipRegion = (RGNDATA*) malloc(crSize);
	(clipRegion->rdh).dwSize = crSize;
	retVal = dx->clipper->GetClipList(&dstRect, clipRegion, &crSize);
	if ((clipRegion->rdh).nCount != 1)
	    bltToScreen = 0;
	else {
	    RECT * rects = (RECT *) clipRegion->Buffer;
	    if (rects->right - rects->left != dx->inWidth ||
		rects->bottom - rects->top != dx->inHeight)
		bltToScreen = 0;
	}
	free(clipRegion);
    }
    
    ddsd.dwSize = sizeof(ddsd);

    // Lock the video surface
    LPDIRECTDRAWSURFACE srcSurface;
    if (dx->overlay != NULL)
	srcSurface = dx->overlay;
    else
	srcSurface = dx->video;

    // Not really a loop, will break out after one iteration
    while (!dx->useGDI) {
	int tryRestore = 3;
	PRINTF("3...");
	while (( retVal = srcSurface->Lock(NULL, &ddsd, 0, NULL)) != DD_OK &&
	       tryRestore > 0) {
	    PRINTF2("locking surface...%d...", retVal);
	    if (retVal == DDERR_SURFACELOST) {
		PRINTF("Video surface lost\n");
		srcSurface->Restore();
		tryRestore--;
	    } else if (retVal == DDERR_WASSTILLDRAWING) {
		
	    } else
		break;
	}
	if (tryRestore == 0) {
	    dx->useGDI = TRUE;
	    PRINTF("Switching to GDI Rendering mid stream  !!!!!!!!!!!\n");
	    srcSurface->Unlock(NULL);
	    break;
	}

	/*    retVal  = dx->video->Lock(NULL, &ddsd, 0, NULL);*/
	
        // Copy the video to the video surface. *** Need to eliminate this step
	if (retVal == DD_OK) {
	    char * data = (char *) ddsd.lpSurface;
	    int strideX = ddsd.lPitch;		      // in bytes, not pixels
	    
	    // YUV Data ?
	    if (dx->yuvType != 0) {
		int offsetY = GetIntField(env, ddrenderer, "offsetY");
		int offsetU = GetIntField(env, ddrenderer, "offsetU");
		int offsetV = GetIntField(env, ddrenderer, "offsetV");
		int inStride = GetIntField(env, ddrenderer, "lineStride");
		
		if (dx->yuvType == GetIntField(env, ddrenderer, "P420"))
		    pYUV420_iYUYV((uchar*) (imageData + offsetY),
				  (uchar*) (imageData + offsetU),
				  (uchar*) (imageData + offsetV),
				  inStride,
				  (uint*) data, strideX / 4,
				  dx->inWidth, dx->inHeight);
		else if (dx->yuvType == GetIntField(env, ddrenderer, "P422"))
		    pYUV422_iYUYV((uchar*) (imageData + offsetY),
				  (uchar*) (imageData + offsetU),
				  (uchar*) (imageData + offsetV),
				  inStride,
				  (uint*) data, strideX / 4,
				  dx->inWidth, dx->inHeight);
	    } else {
		// RGB data
		if ((strideX == dx->inStride * dx->bpp) && !dx->flipped) {
		    // Contiguous memory, copy entive image to video surface
		    memcpy((void*) data, (void*) imageData,
			   dx->inStride * dx->inHeight * dx->bpp);
		} else {
		    int increment = dx->inStride * dx->bpp;
		    // Non contiguous, copy line at a time
		    if (dx->flipped) {
			imageData += (dx->inHeight - 1) * increment;
			increment = -increment;
		    }
		    
		    for (int h = 0 ; h < dx->inHeight; h++) {
			memcpy((void*)data, (void*)imageData, dx->inWidth * dx->bpp);
			data += strideX;
			imageData += increment;
		    }
		}
	    }
	    
	    srcSurface->Unlock(NULL);
	
	    // If its an overlay surface, try to overlay it.
	    if (dx->overlay != NULL) {
		DDOVERLAYFX ovfx;
		ZeroMemory(&ovfx, sizeof(ovfx));
		ovfx.dwSize = sizeof(ovfx);
		ovfx.dckDestColorkey = dx->colorkey;
		//ovfx.dckDestColorkey.dwColorSpaceLowValue=0; // Specify black
		//as the color key
		//ovfx.dckDestColorkey.dwColorSpaceHighValue=0;
		
		retVal = dx->overlay->UpdateOverlay(NULL, screen, &dstRect,
						    DDOVER_SHOW | DDOVER_DDFX | DDOVER_KEYDESTOVERRIDE,
						    &ovfx);
		if (retVal != DD_OK)
		    dx->overlay->UpdateOverlay(NULL, screen, &dstRect, DDOVER_HIDE, NULL);
	    } else
		retVal = !DD_OK;
	    
	    if (srcSurface != NULL && retVal != DD_OK) {
		do {
		    retVal = screen->Blt(&dstRect, (srcSurface), &srcRect, 0, NULL);
		    if (retVal == DDERR_SURFACELOST) {
			//PRINTF("Surface lost\n");
			srcSurface->Restore();
			screen->Restore();
			retVal = DDERR_WASSTILLDRAWING;
		    }
		    
		} while (retVal == DDERR_WASSTILLDRAWING);
		
		if (retVal != DD_OK) {
		    // PRINTF("Error while blitting\n");
		    goto abort;
		}
	    }
	}
	break;
    }

    if (dx->useGDI) {
	PRINTF("...in gdiDraw\n");
	// Use GDI to draw the video frame
	gdiDraw(dx, (void *) imageDataCopy, (jint) bytesPerPixel,
		(jint) dx->inWidth, (jint) dx->inHeight, (jint) dx->inStride,
		(jint) dx->outWidth, (jint) dx->outHeight,
		(jint) dx->rMask, (jint) dx->gMask, (jint) dx->bMask,
		(jboolean) dx->flipped,
		(jint) dx->hwnd);
    }

 skip:
    // Successful
    if (arrayBytes == 0) {
	if (bytesPerPixel == 1) {
	    env->ReleaseByteArrayElements((jbyteArray)array,
					  (signed char *) imageDataCopy, JNI_ABORT);
	} else if (bytesPerPixel == 2) {
	    env->ReleaseShortArrayElements((jshortArray)array,
					   (short *) imageDataCopy, JNI_ABORT);
	} else if (bytesPerPixel == 4) {
	    env->ReleaseIntArrayElements((jintArray)array,
					 (long *) imageDataCopy, JNI_ABORT);
	}
    }
    PRINTF(".\n");
    return 1;
 abort:
    // awtUnlock(env, surface, unlockID);
    if (arrayBytes == 0) {
	if (bytesPerPixel == 1) {
	    env->ReleaseByteArrayElements((jbyteArray)array,
					  (signed char *) imageDataCopy, JNI_ABORT);
	} else if (bytesPerPixel == 2) {
	    env->ReleaseShortArrayElements((jshortArray)array,
					   (short *) imageDataCopy, JNI_ABORT);
	} else if (bytesPerPixel == 4) {
	    env->ReleaseIntArrayElements((jintArray)array,
					 (long *) imageDataCopy, JNI_ABORT);
	}
    }
    return 0;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_renderer_video_DDRenderer_dxFree(JNIEnv *env,
						  jobject ddrenderer)
{
    DxBlitter *dx = (DxBlitter *) GetIntField(env, ddrenderer, "blitter");

    if (dx == NULL) {
	return 0;
    }
    PRINTF("In dxFree\n");
    if (dx->video != NULL)
	dx->video->Release();
    if (dx->overlay != NULL) {
	dx->overlay->Release();
	nOverlays--;
    }

    if (dx->hdc != 0)
	ReleaseDC(dx->hwnd, dx->hdc);
    if (dx->bmi != NULL)
	free(dx->bmi);
    free(dx);
    dx = NULL;
    SetIntField(env, ddrenderer, "blitter", NULL);
    PRINTF("Out dxFree\n");

    return 1;
}

int gdiDraw(DxBlitter *dx,
	    void *data,
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
    if (dx == NULL)
	return 0;

    // Do the handle stuff
    if (dx->hwnd != (HWND) windowHandle || dx->hdc == 0) {
	if (dx->hdc)
	    ReleaseDC(dx->hwnd, dx->hdc);
	dx->hwnd = (HWND) windowHandle;
	dx->hdc = GetDC(dx->hwnd);
    }
    if (dx->hwnd == NULL) {
	PRINTF("Don't have window handle\n");
	return 0;
    }

    HDC hdc = dx->hdc;

    int size = sizeof(BITMAPINFOHEADER) + sizeof(RGBQUAD) * 3;
    if (dx->bmi == NULL)
	dx->bmi = (BITMAPINFO *) malloc(size);

    //if (bytesPerPixel == 3)
    //	srcStride /= 3;

    BITMAPINFO *bmi = dx->bmi;
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

    if (  srcWidth == dstWidth &&
	  srcHeight == dstHeight) {
	
	::SetDIBitsToDevice(hdc, 0, 0, srcWidth, srcHeight, 0, 0, 
			    0, srcHeight, data, bmi, DIB_RGB_COLORS);
	// PRINTF2("Drew = %d\n", result);
    } else {
	//::SetStretchBltMode(hdc, WHITEONBLACK);
	::SetMapMode(hdc, MM_TEXT);
	::StretchDIBits(hdc, 0, 0, dstWidth, dstHeight,
			0, 0, srcWidth, srcHeight,
			data, bmi, DIB_RGB_COLORS, SRCCOPY);
    }

    // ReleaseDC(hdc, blitter->hwnd);
    return 1;
}

/*************************************************************************
 * END DDRenderer native method implementation
 *************************************************************************/

