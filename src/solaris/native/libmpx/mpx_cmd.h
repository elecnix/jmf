/*
 * @(#)mpx_cmd.h	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef _MPX_CMD_H_
#define _MPX_CMD_H_

extern int	mpxSendAck(MpxCntl *);
extern int	mpxSendExit(MpxCntl *);
extern int	mpxSendOpenFile(MpxCntl *, char *);
extern int	mpxSendAction(MpxCntl *, int);
extern int	mpxSendSeek(MpxCntl *, float);
extern int	mpxSendFlush(MpxCntl *);
extern int	mpxSendMute(MpxCntl *, int);
extern int	mpxSendZoom(MpxCntl *, int);
extern int	mpxSendInterleave(MpxCntl *, int);
extern int	mpxSendAudioMode(MpxCntl *, MpxAudio);
extern int	mpxSendAudioQuality(MpxCntl *, MpxAudio);
extern int	mpxSendGamma(MpxCntl *, float);
extern int	mpxProcessInput(MpxCntl *, u_int *);

#endif /* _MPX_CMD_H_ */
