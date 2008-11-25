/*
 * @(#)YuvToRgb.cc	1.14 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef lint
static char rcsid[] =
    "@(#) $Header: color-true.cc,v 1.41 96/02/05 14:33:34 mccanne Exp $ (LBL)";
#endif

#ifdef sun
#pragma ident "@(#)YuvToRgb.cc	1.20 98/07/09" 
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <com_sun_media_codec_video_colorspace_YUVToRGB.h>
#include "YuvToRgb.h"


/****************************************************************
 * JNI Interface
 ****************************************************************/


/*
 * Class:     com_sun_media_codec_video_colorspace_YUVToRGB
 * Method:    initConverter
 * Signature: (Ljavax/media/format/video/YUVFormat;Ljavax/media/format/video/RGBFormat;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_colorspace_YUVToRGB_initConverter(JNIEnv *env,
								 jobject yuv2rgb,
								 jint rmask,
								 jint gmask,
								 jint bmask,
								 jint screenDepth,
								 jboolean sign)
{
    YuvToRgb * converter = (YuvToRgb *) malloc(sizeof(YuvToRgb));
    converter->initialize((unsigned int)rmask,
			  (unsigned int)gmask,
			  (unsigned int)bmask,
			  (unsigned int)screenDepth,
			  (int) sign);
    return (jint) converter;
}

/*
 * Class:     com_sun_media_codec_video_colorspace_YUVToRGB
 * Method:    convert
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_colorspace_YUVToRGB_convert(JNIEnv *env,
							   jobject yuv2rgb,
							   jint jpeer,
							   jobject jinBuffer,
							   jlong inBytes,
							   jobject joutBuffer,
							   jlong outBytes,
							   jint inWidth, jint inHeight,
							   jint outWidth, jint outHeight,
							   jint clipWidth,
							   jint clipHeight,
							   jint offsetY,
							   jint offsetU,
							   jint offsetV,
							   jint strideY,
							   jint strideUV,
							   jint decimation,
							   jint bytesPerPixel)
{
    YuvToRgb *converter = (YuvToRgb *) jpeer;
    
    if ((YuvToRgb *) jpeer == NULL)
	return 0;

    unsigned char * inBuf = (unsigned char *) inBytes;
    unsigned char * outBuf = (unsigned char *) outBytes;
    
    if (inBuf == NULL)
	inBuf = (unsigned char *) env->GetByteArrayElements((jbyteArray) jinBuffer, 0);

    if (outBuf == NULL) {
	switch (bytesPerPixel) {
	case 2:
	    outBuf = (unsigned char *) env->GetShortArrayElements((jshortArray) joutBuffer, 0);
	    break;
	case 3:
	    outBuf = (unsigned char *) env->GetByteArrayElements((jbyteArray) joutBuffer, 0);
	    break;
	case 4:
	    outBuf = (unsigned char *) env->GetIntArrayElements((jintArray) joutBuffer, 0);
	}
    }
    
    converter->render(inBuf, (unsigned int *) outBuf,
		      inWidth, inHeight,
		      outWidth, outHeight,
		      clipWidth, clipHeight,
		      offsetY, offsetU, offsetV,
		      strideY, strideUV,
		      decimation, 0,
		      NULL, 0);

    if (outBytes == 0) {
	switch (bytesPerPixel) {
	case 2:
	    env->ReleaseShortArrayElements((jshortArray) joutBuffer,
					   (jshort *) outBuf, 0);
	    break;
	case 3:
	    env->ReleaseByteArrayElements((jbyteArray) joutBuffer,
					  (jbyte *) outBuf, 0);
	    break;
	case 4:
	    env->ReleaseIntArrayElements((jintArray) joutBuffer,
					 (jint *) outBuf, 0);
	}
    }

    if (inBytes == 0)
	env->ReleaseByteArrayElements((jbyteArray) jinBuffer, (jbyte *) inBuf,
				      JNI_ABORT);
    
    return 1;
}

/*
 * Class:     com_sun_media_codec_video_colorspace_YUVToRGB
 * Method:    freeConverter
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_colorspace_YUVToRGB_freeConverter(JNIEnv *env,
								 jobject yuv2rgb,
								 jint jpeer)
{
    if (jpeer != 0) {
	YuvToRgb *peer = (YuvToRgb *) jpeer;
	free(peer);
	return 1;
    }
    return 0;
}


/****************************************************************
 * End JNI Interface,  Begin class YuvToRgb
 ****************************************************************/

static int XContribsInited = 0;
static int XYContrib[128];
static int XUContribToB[512];
static int XUContribToG[512];
static int XVContribToG[512];
static int XVContribToR[512];

static int
mtos(int mask)
{
    int shift = 0;
    if (mask) {
	while ((mask & 1) == 0) {
	    mask >>= 1;
	    ++shift;
	}
    }
    return (shift);
}

void
YuvToRgb::initialize(unsigned int rmask, unsigned int gmask, unsigned int bmask, unsigned int scrDepth, int sign)
{
    ycbcr_ = 1;
    signed_ = sign;
    screenDepth_ = scrDepth;
    if (screenDepth_ == 15)
	screenDepth_ = 16;
    prepareTable(rmask, gmask, bmask, 0);
}

/**
 * Setup the UV lookup table depending on the r, g and b masks.
 * If swapBytes is non-zero, then the win32 version needs to swap bytes around
 * into BIG_ENDIAN format because BufferedImage needs the image in ABGR format
 * with the bytes in exactly that order (BIG_ENDIAN)
 */
void
YuvToRgb::prepareTable(unsigned int rmask, unsigned int gmask, unsigned int bmask, int swapBytes = 0)
{
    grayscale_ = 0; // 0 or 1
    brightness_ = 0.5;  // 0 - dark, 1.0 - bright, 0.5 neutral
    color_ = 0.5;       // 0 - no color, 1.0 - high color, 0.5 neutral
    contrast_ = 0.5;
    hue_ = 0.5;
    // Copy the masks over to member fields.
    rmask_ = rmask;
    gmask_ = gmask;
    bmask_ = bmask;

    computeShifts();
    updateUVTable();
    updateSaturationTable();
}

void
YuvToRgb::updateTable(float brite, float contrast, float color, float hue,
		      int grayscale)
{
    if (brite != brightness_ || contrast != contrast_) {
	brightness_ = brite;
	contrast_ = contrast;
	updateSaturationTable();
    }

    if (color != color_ || hue != hue_) {
	color_ = color;
	hue_ = hue;
	updateUVTable();
    }
    
    grayscale_ = grayscale;
}

void
YuvToRgb::computeShifts()
{
    // Calculate the RGB shift positions and no. of bits to truncate for each
    // component from the masks.
    rshift_ = mtos(rmask_);
    rlose_ = 8 - mtos(~(rmask_ >> rshift_));
    gshift_ = mtos(gmask_);
    glose_ = 8 - mtos(~(gmask_ >> gshift_));
    bshift_ = mtos(bmask_);
    blose_ = 8 - mtos(~(bmask_ >> bshift_));

    // Change the shift values to match BIG_ENDIAN format.
    int swapBytes = 0;
#ifdef JM_LITTLE_ENDIAN
    if (swapBytes) {
	rshift_ = 24 - rshift_;
	gshift_ = 24 - gshift_;
	bshift_ = 24 - bshift_;
    }
#endif
}

void
YuvToRgb::updateUVTable()
{
#define LIMIT(x) (((x) < -128) ? -128 : (((x) > 127) ? 127 : (x)))
    
    // Fill up the UV lookup table
    unsigned int r, g, b;
    register double uf, vf;
    double theta = (hue_ - 0.5) * 3.14159;
    double ufa, vfa;
    double costheta = cos(theta);
    double sintheta = sin(theta);
    if (ycbcr_) {
	for (int u = 0; u < 256; ++u) {
	    
	    uf = double(u - 128);
	    for (int v = 0; v < 256; ++v) {
		vf = double(v - 128);
		ufa = (uf*costheta + vf*sintheta) * (color_ * 2);
		vfa = (vf*costheta - uf*sintheta) * (color_ * 2);
		r = LIMIT(vfa * 1.596) + 128;
		b = LIMIT(ufa * 2.016) + 128;
		g = LIMIT(ufa * -0.392 - vfa * 0.813) + 128;
		// Store XBGR in uvtab_ table
		if (!signed_) {
		    uvtab_[(u << 8)|v] =
			((r & 0xFF) <<  0) |
			((g & 0xFF) <<  8) |
			((b & 0xFF) << 16);
		} else {
		    uvtab_[(((char)u + 128) << 8) | ((char)v + 128)] =
			((r & 0xFF) <<  0) |
			((g & 0xFF) <<  8) |
			((b & 0xFF) << 16);
		}
	    }
	}
    } else {
	for (int u = 0; u < 256; ++u) {
	    
	    uf = double(u - 128);
	    for (int v = 0; v < 256; ++v) {
		vf = double(v - 128);
		ufa = (uf*costheta + vf*sintheta) * (color_ * 2);
		vfa = (vf*costheta - uf*sintheta) * (color_ * 2);
		r = LIMIT(vfa * 1.402) + 128;
		b = LIMIT(ufa * 1.772) + 128;
		g = LIMIT(ufa * -0.34414 - vfa * 0.71414) + 128;
		// Store XBGR in uvtab_ table
		uvtab_[(u << 8)|v] =
		    ((r & 0xFF) <<  0) |
		    ((g & 0xFF) <<  8) |
		    ((b & 0xFF) << 16);
	    }
	}
    }	
}

void
YuvToRgb::updateSaturationTable()
{
    // Fill up the saturation table. Multiply the color by the brightness
    // and add the tint component for each color
    int s, val;
    float c;
    int p1, p2;
    int ycor = 0;
    double gamma = 1.0;
    
    if (ycbcr_) {
	gamma = 1.164;
	ycor = -16;
    }

    for (s = 0; s < 256; s++) {
	val = s;
	val = (val + ycor) * gamma + (brightness_ - 0.5) * 256.0 + 128;
	if (val > 383)
	    val = 383;
	if (val < 0)
	    val = 0;
	lumtab_[s] = val;
    }
    
    for (s = 0; s < 256; s++) {
	val = s;
	satR_[s+256] = ((val & 0xFF) >> rlose_) << rshift_;
	satG_[s+256] = ((val & 0xFF) >> glose_) << gshift_;
	satB_[s+256] = ((val & 0xFF) >> blose_) << bshift_;
	//printf("R=%7x G=%7x B=%7x \n", satR_[s], satG_[s], satB_[s]);
    }

    for (s = 0; s < 256; s++) {
	satR_[s] = satR_[256];
	satG_[s] = satG_[256];
	satB_[s] = satB_[256];

	satR_[s+512] = satR_[511];
	satG_[s+512] = satG_[511];
	satB_[s+512] = satB_[511];
    }
    
} // YuvToRgb::updateSaturationTable()

void
YuvToRgb::whichMethod(void)
{
    int index; 
    static ConvertMethod methods[] = {
	// For YUV420
	&YuvToRgb::map_420_16,
	&YuvToRgb::map_420_16,
	&YuvToRgb::map_420_24,
	&YuvToRgb::map_420_32,
	// For YUV422
	&YuvToRgb::map_422_16,
	&YuvToRgb::map_422_16,
	&YuvToRgb::map_422_24,
	&YuvToRgb::map_422_32,
	// YVU9 
	&YuvToRgb::map_yvu9_16,
	&YuvToRgb::map_yvu9_16,
	&YuvToRgb::map_yvu9_24,
	&YuvToRgb::map_yvu9_32,
	// For YUV444 or YUV111
	&YuvToRgb::map_111_16,
	&YuvToRgb::map_111_16,
	&YuvToRgb::map_111_24,
	&YuvToRgb::map_111_32,
	// For Indeo 3.2
	&YuvToRgb::map_iv32_16,
	&YuvToRgb::map_iv32_16,
	&YuvToRgb::map_iv32_24,
	&YuvToRgb::map_iv32_32,
	// For YUYV
	&YuvToRgb::map_YUYV_16,
	&YuvToRgb::map_YUYV_16,
	&YuvToRgb::map_YUYV_24,
	&YuvToRgb::map_YUYV_32
    };

    // 4 possible bpp for each decimation type
    if (decimation_ > 6)
	decimation_ = 6;
    index = (decimation_ - 1) * 4 + (int) (screenDepth_ >> 3) - 1;
    if (grayscale_)
	index = 4 * 4 + (screenDepth_ >> 3) - 1;
    method_ = methods[index];
    //printf("Index = %d\n", index);
}

int
YuvToRgb::render(unsigned char * inBuf, unsigned int * outBuf,
		 int inWidth, int inHeight,
		 int outWidth, int outHeight,
		 int clipWidth, int clipHeight,
		 int offsetY, int offsetU, int offsetV,
		 int strideY, int strideUV,
		 int decimation, int scale,
		 unsigned int* blocks, int nBlocks,
		 unsigned char * uBuf, unsigned char *vBuf)
{
    inwidth_ = inWidth;
    inheight_ = inHeight;
    outwidth_ = outWidth;
    outheight_ = outHeight;
    decimation_ = decimation;
    offsety_ = offsetY;
    offsetu_ = offsetU;
    offsetv_ = offsetV;
    stridey_ = strideY;
    strideuv_ = strideUV;
    scale_ = scale;
    framesize_ = inwidth_ * inheight_;

    whichMethod();

    if (nBlocks == 0) {
	// Do the entire frame
	// Aligning clipWidth will be the safest for 420 & 422 calculations.
	clipWidth = (clipWidth % 2 == 0 ? clipWidth : 
			clipWidth + (2 - (clipWidth % 2)));
	(this->*method_)(inBuf, outBuf, 0, 0, 0, clipWidth, clipHeight, uBuf, vBuf);
    } else {
	unsigned int* pBlock = blocks;
	for (int i = 0; i < nBlocks; i++) {
	    (this->*method_)(inBuf, outBuf, pBlock[0],
			     pBlock[1], pBlock[2],
			     pBlock[3], pBlock[4], uBuf, vBuf);
	    pBlock += 5;
	}
    }
    return 1;
}


/*************************************************************************
 * DEFINES FOR COLOR CONVERSION
 *************************************************************************/

#define GETRGB(rgb, red, green, blue) { \
    red   = rgb & 0xFF; \
    green = (rgb >> 8) & 0xFF; \
    blue  = (rgb >> 16); \
}

#define ONEPIX(r, g, b, y, dst) { \
    int t = lumtab_[y];           \
    dst = satR_[r + t] |          \
	  satG_[g + t] |          \
	  satB_[b + t];           \
}

#define ONEGRAY(y, dst) { \
    dst = satR_[y+256] |  \
	  satG_[y+256] |  \
	  satB_[y+256];   \
}

#define ONEIV32(r, g, b, dst) { \
    dst = satR_[r + 256] |          \
	  satG_[g + 256] |          \
	  satB_[b + 256];           \
}


/*************************************************************************
 * CONVERSION ROUTINES BEGIN
 *************************************************************************/
void 
YuvToRgb::map_420_16(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>2) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>2) + (x>>1);
    register unsigned short * xip = (unsigned short *) outBuf + y * outwidth_ + x;
    register int w = width;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 4) {
	register unsigned int sum;
	register unsigned int r, g, b;
	register unsigned short * xip2 = xip + outwidth_;
	register const unsigned char* yp2 = yp + iw;

#define FOUR420(n) \
	sum = uvtab_[(up[(n)/2] << 8) | vp[(n)/2]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], xip[(n)]) \
	ONEPIX(r, g, b, yp[(n)+1], xip[(n)+1]) \
	ONEPIX(r, g, b, yp2[(n)], xip2[(n)]) \
	ONEPIX(r, g, b, yp2[(n)+1], xip2[(n)+1])

	FOUR420(0)

	xip += 2;
	yp += 2;
	up += 1;
	vp += 1;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = 2 * iw - w;
	    register int cstride = (iw - w) >> 1;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += 2 * outwidth_ - w;
	}
    }
}

void 
YuvToRgb::map_420_24(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>2) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>2) + (x>>1);
    register unsigned char* xip = (unsigned char*)outBuf + (y * outwidth_ + x) * 3;
    register int w = width;
    register int incrementLine = (outwidth_ << 2) - (width << 1) + (outwidth_ << 1) - width;
    register unsigned char* xip2 = xip + (outwidth_ << 1) + outwidth_;
    
    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 4) {
	register unsigned int sum;
	register unsigned int r, g, b;
	register const unsigned char* yp2 = yp + iw;
#undef  FOUR420
#define FOUR420(n) \
	sum = uvtab_[(up[(n)/2] << 8) | vp[(n)/2]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], *((unsigned int*)&xip[(n)*3])) \
	ONEPIX(r, g, b, yp[(n)+1], *((unsigned int*)&xip[(n)*3+3])) \
	ONEPIX(r, g, b, yp2[(n)], *((unsigned int*)&xip2[(n)*3])) \
	ONEPIX(r, g, b, yp2[(n)+1], *((unsigned int*)&xip2[(n)*3+3]))

	FOUR420(0)

	xip += 6;				      // 2 * 3
	xip2 += 6;
	yp += 2;
	up += 1;
	vp += 1;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = 2 * iw - w;
	    register int cstride = (iw - w) >> 1;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += incrementLine;
	    xip2 += incrementLine;
	                              //(2 * outwidth_ - w) * 3
	}
    }
}


void 
YuvToRgb::map_420_32(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>2) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>2) + (x>>1);
    register RGBPointer xip = (RGBPointer)outBuf + y * outwidth_ + x;
    register int w = width;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 4) {
	register unsigned int sum;
	register unsigned int r, g, b;
	register RGBPointer xip2 = xip + outwidth_;
	register const unsigned char* yp2 = yp + iw;
#undef  FOUR420
#define FOUR420(n) \
	sum = uvtab_[(up[(n)/2] << 8) | vp[(n)/2]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], xip[(n)]) \
	ONEPIX(r, g, b, yp[(n)+1], xip[(n)+1]) \
	ONEPIX(r, g, b, yp2[(n)], xip2[(n)]) \
	ONEPIX(r, g, b, yp2[(n)+1], xip2[(n)+1])

	FOUR420(0)

	xip += 2;
	yp += 2;
	up += 1;
	vp += 1;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = 2 * iw - w;
	    register int cstride = (iw - w) >> 1;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += 2 * outwidth_ - w;
	}
    }
}


void 
YuvToRgb::map_422_16(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>1) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>1) + (x>>1);
    register unsigned short* xip = (unsigned short *)outBuf + (y * outwidth_ + x);
    register int w = width;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
	
#define TWO422(n) \
	sum = uvtab_[(up[(n)/2] << 8) | vp[(n)/2]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], xip[(n)]) \
	ONEPIX(r, g, b, yp[(n)+1], xip[(n)+1])

	TWO422(0);
		
	xip += 2;
	yp += 2;
	up += 1;
	vp += 1;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    register int cstride = pstride >> 1;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += outwidth_ - w;
	}
    }
}

void 
YuvToRgb::map_422_24(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>1) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>1) + (x>>1);
    register unsigned char * xip = (unsigned char*)outBuf + ((y * outwidth_ + x) * 3);
    register int w = width;
    int incrementLine = (outwidth_ - width) * 3;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
#undef TWO422	
#define TWO422(n) \
	sum = uvtab_[(up[(n)/2] << 8) | vp[(n)/2]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], *(unsigned int*)&xip[(n)*3]) \
	ONEPIX(r, g, b, yp[(n)+1], *(unsigned int*)&xip[(n)*3+3])

	TWO422(0);
		
	xip += 6;				      // 2 * 3 (2 pixels)
	yp += 2;
	up += 1;
	vp += 1;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    register int cstride = pstride >> 1;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += incrementLine;
	}
    }
}

void 
YuvToRgb::map_422_32(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>1) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>1) + (x>>1);
    register RGBPointer xip = (RGBPointer)outBuf + (y * outwidth_ + x);
    register int w = width;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
#undef TWO422	
#define TWO422(n) \
	sum = uvtab_[(up[(n)/2] << 8) | vp[(n)/2]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], xip[(n)]) \
	ONEPIX(r, g, b, yp[(n)+1], xip[(n)+1])

	TWO422(0);
		
	xip += 2;
	yp += 2;
	up += 1;
	vp += 1;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    register int cstride = pstride >> 1;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += outwidth_ - w;
	}
    }
}

void 
YuvToRgb::map_YUYV_16(unsigned char* inBuf, unsigned int * outBuf,
		      unsigned int off, unsigned int x, unsigned int y,
		      unsigned int width, unsigned int height,
		      unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>1) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>1) + (x>>1);
    register unsigned short* xip = (unsigned short *)outBuf + (y * outwidth_ + x);
    register int w = width;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
#undef TWO422
#define TWO422(n) \
	sum = uvtab_[(up[(n)] << 8) | vp[(n)]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], xip[(n)/2]) \
	ONEPIX(r, g, b, yp[(n)+2], xip[(n)/2+1])

	TWO422(0);
		
	xip += 2;
	yp += 4;
	up += 4;
	vp += 4;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - (w * 2);
	    yp += pstride;
	    up += pstride;
	    vp += pstride;
	    xip += outwidth_ - w;
	}
    }
}

void 
YuvToRgb::map_YUYV_24(unsigned char* inBuf, unsigned int * outBuf,
		      unsigned int off, unsigned int x, unsigned int y,
		      unsigned int width, unsigned int height,
		      unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>1) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>1) + (x>>1);
    register unsigned char * xip = (unsigned char*)outBuf + ((y * outwidth_ + x) * 3);
    register int w = width;
    int incrementLine = (outwidth_ - width) * 3;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
#undef TWO422	
#define TWO422(n) \
	sum = uvtab_[(up[(n)] << 8) | vp[(n)]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], *(unsigned int*)&xip[(n)/2*3]) \
	ONEPIX(r, g, b, yp[(n)+2], *(unsigned int*)&xip[(n)/2*3+3])

	TWO422(0);
		
	xip += 6;				      // 2 * 3 (2 pixels)
	yp += 4;
	up += 4;
	vp += 4;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - (w * 2);
	    yp += pstride;
	    up += pstride;
	    vp += pstride;
	    xip += incrementLine;
	}
    }
}

void 
YuvToRgb::map_YUYV_32(unsigned char* inBuf, unsigned int * outBuf,
		      unsigned int off, unsigned int x, unsigned int y,
		      unsigned int width, unsigned int height,
		      unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + (ydist>>1) + (x>>1);
    register const unsigned char* vp = inBuf + offsetv_ + (ydist>>1) + (x>>1);
    register RGBPointer xip = (RGBPointer)outBuf + (y * outwidth_ + x);
    register int w = width;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
#undef TWO422	
#define TWO422(n) \
	sum = uvtab_[(up[(n)] << 8) | vp[(n)]]; \
	GETRGB(sum, r, g, b) \
	ONEPIX(r, g, b, yp[(n)], xip[(n)/2]) \
	ONEPIX(r, g, b, yp[(n)+2], xip[(n)/2+1])

	TWO422(0);
		
	xip += 2;
	yp += 4;
	up += 4;
	vp += 4;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - (w * 2);
	    yp += pstride;
	    up += pstride;
	    vp += pstride;
	    xip += outwidth_ - w;
	}
    }
}

void 
YuvToRgb::map_111_16(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + ydist + x;
    register const unsigned char* vp = inBuf + offsetv_ + ydist + x;
    register unsigned short* xip = (unsigned short*)outBuf + (y * outwidth_ + x);
    register int w = width;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
	
	//Ignore the name TWO111	
#define TWO111(n) \
	sum = uvtab_[(up[n] << 8) | vp[n]]; \
        GETRGB(sum, r, g, b); \
	ONEPIX(r, g, b, yp[n], xip[n])
							
	TWO111(0);
	TWO111(1);
		
	xip += 2;
	yp += 2;
	up += 2;
	vp += 2;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    register int cstride = pstride;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += outwidth_ - w;
	}
    }
}

void 
YuvToRgb::map_111_24(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + ydist + x;
    register const unsigned char* vp = inBuf + offsetv_ + ydist + x;
    register unsigned char * xip = (unsigned char*)outBuf + (y * outwidth_ + x) * 3;
    register int w = width;
    int incrementLine = (outwidth_ - width) * 3;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
#undef TWO111	
	//Ignore the name TWO111	
#define TWO111(n) \
	sum = uvtab_[(up[n] << 8) | vp[n]]; \
        GETRGB(sum, r, g, b); \
	ONEPIX(r, g, b, yp[n], *(unsigned int*)&xip[(n)*3])
							
	TWO111(0);
	TWO111(1);
		
	xip += 6;				      // 2 * 3 (2 pixels)
	yp += 2;
	up += 2;
	vp += 2;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    register int cstride = pstride;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += incrementLine;
	}
    }
}

void 
YuvToRgb::map_111_32(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_ + ydist + x;
    register const unsigned char* up = inBuf + offsetu_ + ydist + x;
    register const unsigned char* vp = inBuf + offsetv_ + ydist + x;
    register RGBPointer xip = (RGBPointer)outBuf + (y * outwidth_ + x);
    register int w = width;

    if (uBuf != NULL)
	up = uBuf;
    if (vBuf != NULL)
	vp = vBuf;
    
    for (register int len = w * height; len > 0; len -= 2) {
	register unsigned int sum, r, g, b;
#undef TWO111	
	//Ignore the name TWO111	
#define TWO111(n) \
	sum = uvtab_[(up[n] << 8) | vp[n]]; \
        GETRGB(sum, r, g, b); \
	ONEPIX(r, g, b, yp[n], xip[n])
							
	TWO111(0);
	TWO111(1);
		
	xip += 2;
	yp += 2;
	up += 2;
	vp += 2;
	
	w -= 2;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    register int cstride = pstride;
	    yp += pstride;
	    up += cstride;
	    vp += cstride;
	    xip += outwidth_ - w;
	}
    }
}
/*
void 
YuvToRgb::map_gray_16(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_  + ydist + x;
    register unsigned short* xip = (unsigned short*)outBuf + (y * outwidth_ + x);
    register int w = width;

    for (register int len = w * height; len > 0; len -= 4) {
	
	ONEGRAY(yp[0], xip[0])
	ONEGRAY(yp[1], xip[1])
	ONEGRAY(yp[2], xip[2])
	ONEGRAY(yp[3], xip[3])

	xip += 4;
	yp += 4;
	
	w -= 4;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    yp += pstride;
	    xip += outwidth_ - w;
	}
    }
}

void 
YuvToRgb::map_gray_24(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_  + ydist + x;
    register unsigned char * xip = (unsigned char*)outBuf + (y * outwidth_ + x) * 3;
    register int w = width;
    int incrementLine = (outwidth_ - width) * 3;
    for (register int len = w * height; len > 0; len -= 4) {
	
	ONEGRAY(yp[0], *(unsigned int*)&xip[0])
	ONEGRAY(yp[1], *(unsigned int*)&xip[3])
	ONEGRAY(yp[2], *(unsigned int*)&xip[6])
	ONEGRAY(yp[3], *(unsigned int*)&xip[9])

	xip += 12;				      // 4 * 3 (4 pixels)
	yp += 4;
	
	w -= 4;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    yp += pstride;
	    xip += incrementLine;
	}
    }
}

void 
YuvToRgb::map_gray_32(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    register unsigned int iw = inwidth_;
    int ydist = y * inwidth_;
    register const unsigned char* yp = inBuf + offsety_  + ydist + x;
    register RGBPointer xip = (RGBPointer)outBuf + (y * outwidth_ + x);
    register int w = width;

    for (register int len = w * height; len > 0; len -= 4) {
	
	ONEGRAY(yp[0], xip[0])
	ONEGRAY(yp[1], xip[1])
	ONEGRAY(yp[2], xip[2])
	ONEGRAY(yp[3], xip[3])

	xip += 4;
	yp += 4;
	
	w -= 4;
	if (w <= 0) {
	    w = width;
	    register int pstride = iw - w;
	    yp += pstride;
	    xip += outwidth_ - w;
	}
    }
}
*/


void 
YuvToRgb::map_yvu9_16(unsigned char* inBuf, unsigned int * outBuf,
		      unsigned int off, unsigned int x, unsigned int y,
		      unsigned int width, unsigned int height,
		      unsigned char *uBuf, unsigned char *vBuf)
{

}

void 
YuvToRgb::map_yvu9_24(unsigned char* inBuf, unsigned int * outBuf,
		      unsigned int off, unsigned int x, unsigned int y,
		      unsigned int width, unsigned int height,
		      unsigned char *uBuf, unsigned char *vBuf)
{

}

void 
YuvToRgb::map_yvu9_32(unsigned char* inBuf, unsigned int * outBuf,
		      unsigned int off, unsigned int x, unsigned int y,
		      unsigned int width, unsigned int height,
		      unsigned char *uBuf, unsigned char *vBuf)
{

}

#ifdef JM_LITTLE_ENDIAN
#define RPOS 2
#define GPOS 1
#define BPOS 0
#else
#define RPOS 3
#define GPOS 2
#define BPOS 1
#endif

/****                  IV32             *****/


void
initContribs()
{
    int i;
    for (i = 0; i < 128; i++) {
	XYContrib[i]= (short)((i * 149) - 1160);
    }
    for (i = 0; i < 512; i++) {
	XUContribToB[i]= (short)((((i - 256) * 64) >> 1) +
				 (((i - 256) * 65) >> 1));
	XUContribToG[i]= (short)(-((i - 256) *  25) >> 1);
	XVContribToG[i]= (short)(-((i - 256) *  26));
	XVContribToR[i]= (short)(((i - 256) *  51));
    }
    XContribsInited = 1;
}
//	    		    Y = XYContrib[

#undef INNERLOOP
#define INNERLOOP(l) \
	    		    Y = XYContrib[*((unsigned char*)YPlane+YRowOffset+k+l)]; \
			    R = (Y + XVContribToR[V]) >> 6; \
			    G = (Y + XUContribToG[U] + XVContribToG[V]) >> 6; \
			    B = (Y + XUContribToB[U]) >> 6; \
			    ONEIV32(R, G, B, XIP); \
			    xip += XIPINC1;	      \
						      \
			    U += AdjustmentU;	      \
			    V += AdjustmentV;

/*
#undef REPEAT
#define REPEAT(R1, R2, R3, R4, R5, R6, R7, R8) {      \
		YRowOffset += stridey_;		      \
		RightUVOffset = 0;		      \
		Utmp = UPlane[UpperUVRowOffset];      \
		is_skip = Utmp & 0x80;		      \
		U = ((R1) + (R2)) >> 2;	              \
		RightU = U;			      \
		U <<= 2;			      \
		V = ((R3) + (R4)) >> 2;	              \
		RightV = V;			      \
        	V <<= 2;			      \
		for (k = 0; k < WdLim; k+=4) {	      \
		    if (k != WdLim - 4) 	      \
			RightUVOffset++;	      \
		    AdjustmentU = - RightU;	      \
		    Utmp = UPlane[UpperUVRowOffset+RightUVOffset]; \
		    RightU = ((R5) + (R6)) >> 2;      \
		    AdjustmentU += RightU;	      \
		    AdjustmentV = -RightV;	      \
		    RightV = ((R7) + (R8) ) >> 2;     \
		    AdjustmentV += RightV;	      \
		    if (is_skip) {		      \
			U += (AdjustmentU << 2);      \
			V += (AdjustmentV << 2);      \
			xip += XIPINC4;		      \
		    } else {			      \
			INNERLOOP(0);                 \
		        INNERLOOP(1);                 \
			INNERLOOP(2);                 \
			INNERLOOP(3);                 \
		    }                                 \
		    is_skip= Utmp & 0x80;	      \
		} \
		xip += XIPINCREM;   \
	    }
*/
#undef REPEAT
#define REPEAT(R1, R2, R3, R4, R5, R6, R7, R8) {      \
		YRowOffset += stridey_;		      \
		RightUVOffset = 0;		      \
		Utmp = UPlane[UpperUVRowOffset];      \
		U = ((R1) + (R2)) >> 2;	              \
		RightU = U;			      \
		U <<= 2;			      \
		V = ((R3) + (R4)) >> 2;	              \
		RightV = V;			      \
        	V <<= 2;			      \
		for (k = 0; k < WdLim; k+=4) {	      \
		    if (k != WdLim - 4) 	      \
			RightUVOffset++;	      \
		    AdjustmentU = - RightU;	      \
		    Utmp = UPlane[UpperUVRowOffset+RightUVOffset]; \
		    RightU = ((R5) + (R6)) >> 2;      \
		    AdjustmentU += RightU;	      \
		    AdjustmentV = -RightV;	      \
		    RightV = ((R7) + (R8) ) >> 2;     \
		    AdjustmentV += RightV;	      \
			INNERLOOP(0);                 \
		        INNERLOOP(1);                 \
			INNERLOOP(2);                 \
			INNERLOOP(3);                 \
		} \
		xip += XIPINCREM;   \
	    }


void 
YuvToRgb::map_iv32_16(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    int  BGR;
    int  i,j,k,l;
    int  WdLim,UpperWt,LowerWt;
    register int  RightU, AdjustmentU;
    register int  RightV, AdjustmentV;
    register int  Y, U, V, R, G, B, Utmp,is_skip;
    int  UpperUVRowOffset, LowerUVRowOffset, RightUVOffset;
    int  YRowOffset;
    unsigned char * YPlane, *UPlane, *VPlane;
    unsigned char * xip = (unsigned char*) outBuf;
    
    if (!XContribsInited) {
	initContribs();
    }
    
    YPlane = inBuf + offsety_;
    VPlane = inBuf + offsetv_;
    UPlane = inBuf + offsetu_;

    //printf("YPlane = %d, UPlane = %d, VPlane = %d\nstrideY = %d, strideUV = %d\n",
    //   YPlane, UPlane, VPlane,
    //   stridey_, strideuv_);

    {
    	WdLim = width;
    	UpperUVRowOffset = - strideuv_;
    	LowerUVRowOffset = 0;
	
    	YRowOffset = - stridey_;
	
    	for (j = 0; j < height; j+=4) {
    	    UpperUVRowOffset += strideuv_;
    	    LowerUVRowOffset += strideuv_;
    	    
	    if (j == height - 4) {
		LowerUVRowOffset = UpperUVRowOffset;
    	    }


#undef XIP
#define XIP *(unsigned short*)xip
#undef XIPINC1
#define XIPINC1 2
#undef XIPINC4
#define XIPINC4 8
#undef XIPINCREM
#define XIPINCREM (outwidth_ - WdLim) << 1

	    // Call the 4 cases (loop)
	    REPEAT( (Utmp & 0x7f) << 2, 0,
		    (VPlane[UpperUVRowOffset] & 0x7f) << 2,	0,
		    (Utmp & 0x7f) << 2, 0,
		    (VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 2, 0
		    );
	    if (j+1 < height)
		REPEAT( ((Utmp & 0x7f) << 1) + (Utmp &0x7f),
			(UPlane[LowerUVRowOffset] & 0x7f),
			((VPlane[UpperUVRowOffset] & 0x7f) << 1) +
			(VPlane[UpperUVRowOffset] & 0x7f),
			(VPlane[LowerUVRowOffset] & 0x7f),
			((Utmp & 0x7f) << 1) + (Utmp & 0x7f),
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f),
			((VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f),
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f)
			);
	    if (j+2 < height)
		REPEAT( (Utmp & 0x7f) << 1,
			(UPlane[LowerUVRowOffset] & 0x7f) << 1,
			(VPlane[UpperUVRowOffset] & 0x7f) << 1,
			(VPlane[LowerUVRowOffset] & 0x7f) << 1,
			(Utmp & 0x7f) << 1,
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1,
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 1,
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1
			);
	    if (j+3 < height)
		REPEAT( (Utmp & 0x7f),
			((UPlane[LowerUVRowOffset] & 0x7f) << 1) +
			(UPlane[LowerUVRowOffset] & 0x7f),
			(VPlane[UpperUVRowOffset] & 0x7f),
			((VPlane[LowerUVRowOffset] & 0x7f) << 1) +
			(VPlane[LowerUVRowOffset] & 0x7f),
			(Utmp & 0x7f),
			((UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f),
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f),
			((VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f)
			);
        }
    }
}

void 
YuvToRgb::map_iv32_32(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    int  BGR;
    int  i,j,k,l;
    int  WdLim,UpperWt,LowerWt;
    register int  RightU, AdjustmentU;
    register int  RightV, AdjustmentV;
    register int  Y, U, V, R, G, B, Utmp,is_skip;
    int  UpperUVRowOffset, LowerUVRowOffset, RightUVOffset;
    int  YRowOffset;
    unsigned char * YPlane, *UPlane, *VPlane;
    unsigned char * xip = (unsigned char*) outBuf;
    
    YPlane = inBuf + offsety_;
    VPlane = inBuf + offsetv_;
    UPlane = inBuf + offsetu_;
    
    if (!XContribsInited) {
	initContribs();
    }


    {
    	WdLim = width;
    	UpperUVRowOffset = - strideuv_;
    	LowerUVRowOffset = 0;
	
    	YRowOffset = - stridey_;
	
    	for (j = 0; j < height; j+=4) {
    	    UpperUVRowOffset += strideuv_;
    	    LowerUVRowOffset += strideuv_;
    	    
	    if (j == height - 4) {
		LowerUVRowOffset = UpperUVRowOffset;
    	    }


#undef XIP
#define XIP *(unsigned int*)xip	   
#undef XIPINC1
#define XIPINC1 4
#undef XIPINC4
#define XIPINC4 16
#undef XIPINCREM
#define XIPINCREM (outwidth_ - WdLim) << 2

	    // Call the 4 cases (loop)
	    REPEAT( (Utmp & 0x7f) << 2, 0,
		    (VPlane[UpperUVRowOffset] & 0x7f) << 2,	0,
		    (Utmp & 0x7f) << 2, 0,
		    (VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 2, 0
		    );
	    if (j+1 < height)
		REPEAT( ((Utmp & 0x7f) << 1) + (Utmp &0x7f),
			(UPlane[LowerUVRowOffset] & 0x7f),
			((VPlane[UpperUVRowOffset] & 0x7f) << 1) +
			(VPlane[UpperUVRowOffset] & 0x7f),
			(VPlane[LowerUVRowOffset] & 0x7f),
			((Utmp & 0x7f) << 1) + (Utmp & 0x7f),
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f),
			((VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f),
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f)
			);
	    if (j+2 < height)
		REPEAT( (Utmp & 0x7f) << 1,
			(UPlane[LowerUVRowOffset] & 0x7f) << 1,
			(VPlane[UpperUVRowOffset] & 0x7f) << 1,
			(VPlane[LowerUVRowOffset] & 0x7f) << 1,
			(Utmp & 0x7f) << 1,
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1,
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 1,
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1
			);
	    if (j+3 < height)
		REPEAT( (Utmp & 0x7f),
			((UPlane[LowerUVRowOffset] & 0x7f) << 1) +
			(UPlane[LowerUVRowOffset] & 0x7f),
			(VPlane[UpperUVRowOffset] & 0x7f),
			((VPlane[LowerUVRowOffset] & 0x7f) << 1) +
			(VPlane[LowerUVRowOffset] & 0x7f),
			(Utmp & 0x7f),
			((UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f),
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f),
			((VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f)
			);
        }
    }
}

void 
YuvToRgb::map_iv32_24(unsigned char* inBuf, unsigned int * outBuf,
		  unsigned int off, unsigned int x, unsigned int y,
		  unsigned int width, unsigned int height, unsigned char *uBuf, unsigned char *vBuf)
{
    int  BGR;
    int  i,j,k,l;
    int  WdLim,UpperWt,LowerWt;
    register int  RightU, AdjustmentU;
    register int  RightV, AdjustmentV;
    register int  Y, U, V, R, G, B, Utmp,is_skip;
    int  UpperUVRowOffset, LowerUVRowOffset, RightUVOffset;
    int  YRowOffset;
    unsigned char * YPlane, *UPlane, *VPlane;
    unsigned char * xip = (unsigned char*) outBuf;
    
    YPlane = inBuf + offsety_;
    VPlane = inBuf + offsetv_;
    UPlane = inBuf + offsetu_;

    if (!XContribsInited) {
	initContribs();
    }


    {
    	WdLim = width;
    	UpperUVRowOffset = - strideuv_;
    	LowerUVRowOffset = 0;
	
    	YRowOffset= - stridey_;
	
    	for (j = 0; j < height; j+=4) {
    	    UpperUVRowOffset += strideuv_;
    	    LowerUVRowOffset += strideuv_;
    	    
	    if (j == height - 4) {
		LowerUVRowOffset = UpperUVRowOffset;
    	    }


#undef XIP
#define XIP *(unsigned int*)xip
#undef XIPINC1
#define XIPINC1 3
#undef XIPINC4
#define XIPINC4 12
#undef XIPINCREM
#define XIPINCREM ((outwidth_ - WdLim) << 1) + (outwidth_ - WdLim)

	    // Call the 4 cases (loop)
	    REPEAT( (Utmp & 0x7f) << 2, 0,
		    (VPlane[UpperUVRowOffset] & 0x7f) << 2,	0,
		    (Utmp & 0x7f) << 2, 0,
		    (VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 2, 0
		    );

	    if (j+1 < height)
		REPEAT( ((Utmp & 0x7f) << 1) + (Utmp &0x7f),
			(UPlane[LowerUVRowOffset] & 0x7f),
			((VPlane[UpperUVRowOffset] & 0x7f) << 1) +
			(VPlane[UpperUVRowOffset] & 0x7f),
			(VPlane[LowerUVRowOffset] & 0x7f),
			((Utmp & 0x7f) << 1) + (Utmp & 0x7f),
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f),
			((VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f),
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f)
			);
	    if (j+2 < height)
		REPEAT( (Utmp & 0x7f) << 1,
			(UPlane[LowerUVRowOffset] & 0x7f) << 1,
			(VPlane[UpperUVRowOffset] & 0x7f) << 1,
			(VPlane[LowerUVRowOffset] & 0x7f) << 1,
			(Utmp & 0x7f) << 1,
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1,
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f) << 1,
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1
			);
	    if (j+3 < height)
		REPEAT( (Utmp & 0x7f),
			((UPlane[LowerUVRowOffset] & 0x7f) << 1) +
			(UPlane[LowerUVRowOffset] & 0x7f),
			(VPlane[UpperUVRowOffset] & 0x7f),
			((VPlane[LowerUVRowOffset] & 0x7f) << 1) +
			(VPlane[LowerUVRowOffset] & 0x7f),
			(Utmp & 0x7f),
			((UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(UPlane[LowerUVRowOffset+RightUVOffset] & 0x7f),
			(VPlane[UpperUVRowOffset+RightUVOffset] & 0x7f),
			((VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f) << 1) +
			(VPlane[LowerUVRowOffset+RightUVOffset] & 0x7f)
			);
        }
    }
}

