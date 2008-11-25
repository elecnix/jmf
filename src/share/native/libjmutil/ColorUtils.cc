/*
 * @(#)ColorUtils.cc	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "ColorUtils.h"

// Convert YUV 4:2:2 planar to YUYV interleaved.
int
pYUV422_iYUYV(uchar *py, uchar *pu, uchar *pv, int strideY,
	      uint *yuyv, int strideYUYV, int width, int height)
{
    int x, y;
    uint pix;
    
    for (y = 0; y < height; y++) {
	for (x = 0; x < width; x += 2) {
	    pix =
		((uint)py[0] << 0) |
		((uint)*pu << 8)   |
		((uint)py[1] << 16)  |
		((uint)*pv << 24);
	    *yuyv = pix;
	    yuyv++;
	    py += 2;
	    pu++;
	    pv++;
	}
	yuyv += (strideYUYV - width / 2);
	py += strideY - width;
	pu += (strideY - width) >> 1;
	pv += (strideY - width) >> 1;
    }
    return 1;
}

// Convert YUV 4:2:0 planar to YUYV interleaved.
int
pYUV420_iYUYV(uchar *py, uchar *pu, uchar *pv, int strideY,
	      uint *yuyv, int strideYUYV, int width, int height)
{
    int x, y;
    uint pix;
    
    for (y = 0; y < height; y += 2) {
	for (x = 0; x < width; x += 2) {
	    pix =
		((uint)py[0] << 0) |
		((uint)*pu << 8)   |
		((uint)py[1] << 16)  |
		((uint)*pv << 24);
	    *yuyv = pix;
	    pix =
		((uint)py[strideY] << 0) |
		((uint)*pu << 8)   |
		((uint)py[strideY + 1] << 16)  |
		((uint)*pv << 24);
	    yuyv[strideYUYV] = pix;
	    yuyv++;
	    py += 2;
	    pu++;
	    pv++;
	}
	yuyv += (strideYUYV - width / 2) + strideYUYV;
	py += 2 * strideY - width;
	pu += (strideY - width) >> 1;
	pv += (strideY - width) >> 1;
    }
    return 1;
}
// Convert YUV 4:2:2 planar to YUYV interleaved.
int
pYUV422_iUYVY(uchar *py, uchar *pu, uchar *pv, int strideY,
	      uint *yuyv, int strideYUYV, int width, int height)
{
    int x, y;
    uint pix;
    
    for (y = 0; y < height; y++) {
	for (x = 0; x < width; x += 2) {
	    pix =
		((uint)py[0] << 8) |
		((uint)*pu << 0)   |
		((uint)py[1] << 24)  |
		((uint)*pv << 16);
	    *yuyv = pix;
	    yuyv++;
	    py += 2;
	    pu++;
	    pv++;
	}
	yuyv += (strideYUYV - width / 2);
	py += strideY - width;
	pu += (strideY - width) >> 1;
	pv += (strideY - width) >> 1;
    }
    return 1;
}

// Convert YUV 4:2:0 planar to YUYV interleaved.
int
pYUV420_iUYVY(uchar *py, uchar *pu, uchar *pv, int strideY,
	      uint *yuyv, int strideYUYV, int width, int height)
{
    int x, y;
    uint pix;
    
    for (y = 0; y < height; y += 2) {
	for (x = 0; x < width; x += 2) {
	    pix =
		((uint)py[0] << 8) |
		((uint)*pu << 0)   |
		((uint)py[1] << 24)  |
		((uint)*pv << 16);
	    *yuyv = pix;
	    pix =
		((uint)py[strideY] << 8) |
		((uint)*pu << 0)   |
		((uint)py[strideY + 1] << 24)  |
		((uint)*pv << 16);
	    yuyv[strideYUYV] = pix;
	    yuyv++;
	    py += 2;
	    pu++;
	    pv++;
	}
	yuyv += (strideYUYV - (width / 2)) + strideYUYV;
	py += 2 * strideY - width;
	pu += (strideY - width) >> 1;
	pv += (strideY - width) >> 1;
    }
    return 1;
}
