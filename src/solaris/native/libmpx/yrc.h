/*
 * @(#)yrc.h	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#if !defined(__YRC_H__)
#define __YRC_H__

#ifdef __cplusplus
extern "C" {
#endif
    int YR_initialize(void **vcc, int rmask, int gmask, int bmask, int depth);
    int YR_convert(void *vcc, void * inBuf, void * outBuf,
		   int inWidth, int inHeight,
		   int outWidth, int outHeight,
		   int clipWidth, int clipHeight,
		   void *uBuf, void *vBuf);
    int YR_close(void *vcc);
#ifdef __cplusplus
}
#endif

#endif
