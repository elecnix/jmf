/*
 * @(#)mpx_cmd.c	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
#ifndef WIN32
#include <unistd.h>
#endif
#include <stdio.h>
#include <malloc.h>
#include <sys/types.h>
#include "mpx.h"
#include "mp_mpd.h"
#include "mp_aud.h"
#include "ln_lnd.h"


/* MPX command structure. */
typedef struct MpxCmd {
	u_int	sync[4];
	u_int	version;
	u_int	channel;
	u_int	sequence;
	u_int	flags;
	u_int	type;
	u_int 	id;
	u_int	params[118];
} MpxCmd;


static MpxCmd *
newCmd()
{
	MpxCmd *cmd = (MpxCmd *)calloc(1, sizeof(struct MpxCmd));
#ifdef JM_BIG_ENDIAN
	/* Fill in the command header. */
	cmd->sync[0] = htonl(0x00000001);
	cmd->sync[1] = htonl(0x00000002);
	cmd->sync[2] = htonl(0x00000003);
	cmd->sync[3] = htonl(0x00000004);
	cmd->version = htonl(0xaaaa0001);
	cmd->channel = htonl(0xbbbb0000);
	cmd->sequence = htonl(0x00000000);
	cmd->flags = htonl(0xcccc0000); 
	cmd->type = htonl(0xdddd0002);
#endif
	return cmd;
}


static int
mpxSend(MpxCntl *cntl, MpxCmd *c)
{
    return 0;
}


int
mpxSendAck(MpxCntl *cntl)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->flags |= MCFL_SNDACK;
	cmd->id = MCMD_ACK;
	cmd->sequence = cntl->ackSeq;
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendExit(MpxCntl *cntl)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->flags |= MCFL_SNDACK;
	cmd->id = MCMD_EXIT;
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendOpenFile(MpxCntl *cntl, char *path)
{
	MpxCmd 	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_OPENSRC;
	cmd->params[0] = 0;
	cmd->params[1] = 0;
	cmd->params[2] = cntl->strmType;
	cmd->params[3] = 0;
	cmd->params[4] = MRE_FOFS;
	cmd->params[5] = MSC_FNAME;
	strcpy((char *)(cmd->params+6), path);
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
#ifndef WIN32
	gettimeofday(&cntl->startTime, NULL);
#endif
	cntl->frames = 0;

	return error;
}


int
mpxSendAction(MpxCntl *cntl, int act)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_PLAYCTR;
	cmd->params[0] = act;
	cmd->params[1] = FL_TO_INT(cntl->fwdSpeed);
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendSeek(MpxCntl *cntl, float l)
{
#ifndef WIN32
    MpxCmd	*cmd = newCmd();
    int	error;
    
    cmd->id = MCMD_REENTER;
    cmd->params[0] = 0; 
    cmd->params[1] = FL_TO_INT(l);
    cmd->params[2] = cntl->strmType; 
    cmd->params[3] = 0;
    cmd->params[4] = MRE_FOFS; 
    error = mpxSend(cntl, cmd);
    free((char *)cmd);
    if (l == 0.0) {
	gettimeofday(&cntl->startTime, NULL);
	cntl->frames = 0;
    }
    
    return error;
#else
    printf("win32!!\n");
    return 0;
#endif
}


int
mpxSendFlush(MpxCntl *cntl)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_REENTER;
	cmd->params[0] = 0; 
	cmd->params[1] = 0; 
	cmd->params[2] = cntl->strmType;
	cmd->params[3] = 0x2020;
	cmd->params[4] = MRE_ASOPEN; 
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendMute(MpxCntl *cntl, int m)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_STREAM;
	if (m)
		cmd->params[0] = STRM_IGNOREID | STRM_SBCOFF;
	else
		cmd->params[0] = 0;
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendZoom(MpxCntl *cntl, int z)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_PRESCTR;
	cmd->params[0] = PCTR_VMD;
	cmd->params[1] = ((cntl->interleave ? VDM_COLB : VDM_COL) << 8);
	cmd->params[1] |= z;
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendInterleave(MpxCntl *cntl, int l)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_PRESCTR;
	cmd->params[0] = PCTR_VMD;
	cmd->params[1] = ((l ? VDM_COLB : VDM_COL) << 8);
	cmd->params[1] |= cntl->zoom;
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendAudioMode(MpxCntl *cntl, MpxAudio m)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_PRESCTR;
	cmd->params[0] = PCTR_AMD;
	if ((m & MpxAudioStereo) == MpxAudioStereo)
		cmd->params[2] = 070;
	else if (m & MpxAudioRight)
		cmd->params[2] = 060;
	else
		cmd->params[2] = 050;
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendAudioQuality(MpxCntl *cntl, MpxAudio q)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_PRESCTR;
	cmd->params[0] = PCTR_AMD;
	if ((q & MpxAudioHiQ) == MpxAudioHiQ)
		cmd->params[2] = 04;
	else if (q & MpxAudioMedQ)
		cmd->params[2] = 05;
	else
		cmd->params[2] = 06;
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxSendGamma(MpxCntl *cntl, float g)
{
	MpxCmd	*cmd = newCmd();
	int	error;

	cmd->id = MCMD_PRESCTR;
	cmd->params[0] = PCTR_GAM;
	cmd->params[6] = FL_TO_INT(g); 
	error = mpxSend(cntl, cmd);
	free((char *)cmd);
	return error;
}


int
mpxProcessInput(MpxCntl *cntl, u_int *seq)
{
    return 0;
}

