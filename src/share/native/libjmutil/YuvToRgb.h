
/*
 * @(#)YuvToRgb.h	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef __YUVTORGB_H__
#define __YUVTORGB_H__

#include <jni.h>

typedef unsigned int* RGBPointer;

class YuvToRgb;
// Define the ConvertMethod function pointer
typedef void (YuvToRgb::*ConvertMethod)(unsigned char*, unsigned int*, unsigned int, unsigned int,
					unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);

class YuvToRgb
{
public:
    YuvToRgb(void) { }
    JNIEXPORT void initialize(unsigned int rmask, unsigned int gmask, unsigned int bmask,
			      unsigned int screenDepth, int sign);
    void updateTable(float brite, float contrast, float color,
		     float hue, int grayscale);
    JNIEXPORT int render(unsigned char * inBuf, unsigned int * outBuf,
			 int inWidth, int inHeight,
			 int outWidth, int outHeight,
			 int clipWidth, int clipHeight,
			 int offsetY, int offsetU, int offsetV,
			 int strideY, int strideUV,
			 int decimation, int scale,
			 unsigned int *blocks, int nBlocks,
			 unsigned char * uBuf = NULL, unsigned char * vBuf = NULL);

private:
    void prepareTable(unsigned int, unsigned int, unsigned int, int);
    void computeShifts();
    void updateUVTable();
    void updateSaturationTable();
    
    void whichMethod(void);
    
    void map_420_16   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_420_24   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_420_32   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_422_16   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_422_24   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_422_32   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_YUYV_16   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_YUYV_24   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_YUYV_32   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_111_16   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_111_24   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *); 
    void map_111_32   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_iv32_16   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_iv32_24   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_iv32_32   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_yvu9_16   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_yvu9_24   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);
    void map_yvu9_32   (unsigned char*, unsigned int *, unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, unsigned char *, unsigned char *);

private:
    int ycbcr_;
    int signed_;
    int inwidth_;
    int inheight_;
    int stridey_;
    int strideuv_;
    int outwidth_;
    int outheight_;
    int decimation_; // 1 = YUV420, 2 = YUV422, 4 = YUV111
    int scale_; // 0 = 2x, 1 = 1x, 2 = 0.5x, 3 = 0.25x
    int framesize_; // width * height
    int offsety_;
    int offsetu_;
    int offsetv_;
    float color_, brightness_, contrast_, hue_;
    int grayscale_;
    unsigned char * blockStamps_;
    unsigned int omask_;
    unsigned int pmask_;
    unsigned int uvtab_[65536];
    unsigned int lumtab_[256];
    unsigned int satR_[768];
    unsigned int satG_[768];
    unsigned int satB_[768];
    unsigned int rmask_;
    unsigned int gmask_;
    unsigned int bmask_;
    unsigned int screenDepth_;
    unsigned int rshift_;
    unsigned int gshift_;
    unsigned int bshift_;
    unsigned int rlose_;
    unsigned int glose_;
    unsigned int blose_;
    ConvertMethod method_;
};

#endif
