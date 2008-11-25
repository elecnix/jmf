/*
 * @(#)ColorUtils.h	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#define uchar unsigned char
#define uint  unsigned int

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif
    
JNIEXPORT int pYUV420_iYUYV(uchar *py, uchar *pu, uchar *pv, int strideY,
			    uint *yuyv, int strideYUYV, int width, int height);

JNIEXPORT int pYUV422_iYUYV(uchar *py, uchar *pu, uchar *pv, int strideY,
			    uint *yuyv, int strideYUYV, int width, int height);

JNIEXPORT int pYUV420_iUYVY(uchar *py, uchar *pu, uchar *pv, int strideY,
			    uint *yuyv, int strideYUYV, int width, int height);

JNIEXPORT int pYUV422_iUYVY(uchar *py, uchar *pu, uchar *pv, int strideY,
			    uint *yuyv, int strideYUYV, int width, int height);

#ifdef __cplusplus
}
#endif
