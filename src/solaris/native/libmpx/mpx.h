/*
 * @(#)mpx.h	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef _MPX_DFN_H_
#define _MPX_DFN_H_

#include <jni.h>

#if defined(__cplusplus) || defined(c_plusplus)
extern "C" {
#endif

#include "c_varieties.h"

#define MAX_ENV_LEN	64


/* Stream type */
#define	MpxStrm_11172	(1<<0)
#define	MpxStrm_VSEQ	(1<<1)
#define	MpxStrm_ASEQ	(1<<2)

#ifdef WIN32

typedef unsigned int u_int;
typedef unsigned char u_char;

#define valloc malloc

#if !defined(timeval)
struct timeval {
    long tv_sec;
    long tv_usec;
};
#endif

#endif

#ifdef LINUX
#include <sys/time.h>
#endif
    
/* Actions */
#define	 MpxActionPlay		(1<<0)	   
#define	 MpxActionFwdSpeed	(1<<1)
#define	 MpxActionFwdStep	(1<<2)
#define	 MpxActionPause		(1<<3)


typedef enum {
	MpxNend = 0,	/* reserved for the NULL terminator */ 
			/* creation only attributes */

			/* create only */
	MpxNnewProcess = 1,
	MpxNexecPath = 2,
	MpxNerrorSilent = 3,
	MpxNuiType = 4,
	MpxNwinXid = 5,
	MpxNwinDepth = 6,
	MpxNbackgroundColor = 7,
	MpxNjavaClient = 8,
	MpxNjniEnv = 9,

			/* create, set, get */
	MpxNaudioMode = 51,
	MpxNaudioQuality = 52,
	MpxNvideoInterleave = 53,
	MpxNvideoZoom = 54,
	MpxNstrmType = 59,
	MpxNinputFile = 60,
	MpxNinputNetPort = 61,
	MpxNclientData = 62,
	MpxNinputNetAddr = 63,
	MpxNinputNetTTL = 64,

			/* set, get */
	MpxNaction = 100,
	MpxNforwardSpeed = 101,
	MpxNseekPos= 102,
	MpxNmute = 103,
	MpxNgamma = 104,

			/* set only */
	MpxNflush = 151,
	MpxNsync = 152,
	MpxNexit = 153,

			/* Callbacks, set, get */
	MpxNsizeChangeCallback = 200,
	MpxNstatsUpdateCallback = 201,
	MpxNackNotifyCallback = 202,
	MpxNexitCallback = 203,
	MpxNfileDoneCallback = 204
} MpxAttr;


typedef enum {
	MpxFullGUI = 0,
	MpxNoGUI = 2
} MpxUIType;


typedef enum {
	MpxAudioLeft = 01,
	MpxAudioRight = 02,
	MpxAudioStereo = 03,
	MpxAudioLowQ = 010,
	MpxAudioMedQ = 020,
	MpxAudioHiQ = 030
} MpxAudio;


typedef struct {
  u_int	width, height, zoom;
} MpxSizeChangeData;


typedef struct {
	float	fps, kbps, loc;
	u_int	time;
	int	size;
} MpxStatsUpdateData;


typedef struct {
	u_int	sequence;
} MpxAckNotifyData;


typedef struct {
	float	fps, kbps;
	u_int	time, frames;
	int	size;
} MpxFileDoneData;


struct MpxCntl;
typedef void (*MpxExitCallback)(struct MpxCntl *);
typedef void (*MpxSizeChangeCallback)(struct MpxCntl *, MpxSizeChangeData *);
typedef void (*MpxStatsUpdateCallback)(struct MpxCntl *, MpxStatsUpdateData *);
typedef void (*MpxAckNotifyCallback)(struct MpxCntl *, MpxAckNotifyData *);
typedef void (*MpxFileDoneCallback)(struct MpxCntl *, MpxFileDoneData *);


typedef struct MpxCntl {
	void 	*thrID;
	char	*parentID;
	int 	fd;
	int	audioMode, audioQuality;
	int	interleave;
	int 	strmType;
	int	action;
	int	muted;
	int	size;
        jobject component;
	u_int	width, height;
	u_int	depth;
	u_int	zoom;
	u_int	time;
	u_int	cmap;
	u_int	ackSeq;
	u_int	frames;
	float	gamma;
	float	fwdSpeed;
	float	loc;
	float	fps, kbps;
	void	*clientData;
	void	*jmpx;
	void	*jniEnv;
	void	*gb;
	struct timeval lastStats;
	struct timeval startTime;

	/* Callbacks */
	MpxExitCallback		exitCallback;
	MpxSizeChangeCallback	sizeChangeCallback;
	MpxStatsUpdateCallback	statsUpdateCallback;
	MpxAckNotifyCallback	ackNotifyCallback;
	MpxFileDoneCallback	fileDoneCallback;
} MpxCntl;


EXTERN_FUNCTION( MpxCntl *mpxCreate, (char *, DOTDOTDOT) );
EXTERN_FUNCTION( int mpxSet, (MpxCntl *, DOTDOTDOT) );
EXTERN_FUNCTION( int mpxGet, (MpxCntl *, DOTDOTDOT) );
EXTERN_FUNCTION( void mpxDestroy, (MpxCntl *) );
/*EXTERN_FUNCTION( void mpxXtInputCallback, (MpxCntl *, int *, XtInputId *) );*/
EXTERN_FUNCTION( int mpxProcessInput, (MpxCntl *, u_int *) );
EXTERN_FUNCTION( MpxCntl *mpxStartThread, (MpxCntl *, char **) );
EXTERN_FUNCTION( MpxCntl *mpxStartProcess, (MpxCntl *, char **, char *) );
EXTERN_FUNCTION( void *mpxThread, (void *) );
EXTERN_FUNCTION( char **mpxMakeEnv, (char **, DOTDOTDOT) );
EXTERN_FUNCTION( void mpxDestroyEnv, (char **) );
EXTERN_FUNCTION( int isMpegFile, (char *, int *, int *) );


#define	MpxInputFd(cntl)	((cntl)->fd)
#define MpxPlayLoc(cntl)	((cntl)->loc)
#define MpxZoomFactor(cntl)	((cntl)->zoom)
#define MpxColormap(cntl)	((cntl)->cmap)
#define MpxActionState(cntl)	((cntl)->action)
#define MpxVideoWidth(cntl)	((cntl)->width)
#define MpxVideoHeight(cntl)	((cntl)->height)
#define MpxClientData(cntl)	((cntl)->clientData)

#if defined(__cplusplus) || defined(c_plusplus)
} /* extern "C" */
#endif

#endif /* _MPX_DFN_H_ */
