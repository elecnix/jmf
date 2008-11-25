/*
 * @(#)yrc.cc	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "bsd-endian.h"
#include "yrc.h"
#include <YuvToRgb.h>

int
YR_initialize(void **vcc, int rmask, int gmask, int bmask, int depth)
{
    YuvToRgb *yr = new YuvToRgb();

    yr->initialize(rmask, gmask, bmask, depth);    
    *vcc = (void *) yr;
    return 1;
}

int
YR_convert(void *vcc, void * inBuf, void * outBuf,
	   int inWidth, int inHeight,
	   int outWidth, int outHeight,
	   int clipWidth, int clipHeight, void *uBuf, void *vBuf)
{
    YuvToRgb *yr = (YuvToRgb*) vcc;
    if (yr == NULL)
	return 0;

    yr->render((unsigned char*)inBuf, (unsigned int *)outBuf,
	       inWidth, inHeight, outWidth, outHeight,
	       clipWidth, clipHeight, 

		/** -ivg offY, offU, offV */
		0, inWidth, inWidth+inWidth/4,
		/** -ivg strideY, strideUV */
		inWidth, inWidth/2,

		1 /*decimation YUV_411*/,
	       1, /* scale */
	       NULL, /* blocks */
	       0, /*no. of blocks */
	       (unsigned char *)uBuf,
	       (unsigned char *)vBuf
	       );
    return 1;
}

int
YR_close(void *vcc) {
    YuvToRgb *yr = (YuvToRgb*) vcc;
    if (yr == NULL)
	return 0;
    delete yr;
    return 1;
}
