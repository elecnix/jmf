/*
 * @(#)mpx_api.c	1.5 02/08/28
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef WIN32
#include <unistd.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <jni-util.h>
#include <com_sun_media_codec_video_jmpx_Jmpx.h>	/* Java mpx glue */
#include "mpx.h"
#include "mpx_cmd.h"
#include "mp_mpd.h"
#include "mp_aud.h"
#include "ln_lnd.h"


static MpxCntl	*createCntl();
static int 	mpxWait(int fd, int t);
static int	blockForAck(MpxCntl *);

extern int DEBUG;


MpxCntl *
mpxCreate(char *id, ...)
{
	va_list	ap;
	int	attr, ival;
	int	strmType = -1;
	int	port = -1;
	int	ttl = -1;
	char	**env = NULL;
	char	*sval, buf[128], tmp[8];
	char	*mpxpath = NULL;
	char	*file = NULL;
	char	*addr = NULL;
	void	*pval;
	MpxCntl	*mpxCntl = createCntl();

	if (!mpxCntl)
		return NULL;

	/* If we are forking smpx, we want to include the name
	   as the first element in the argument list. */
	env = mpxMakeEnv(env, "smpx", NULL);

	va_start(ap, id);
	while ((attr = va_arg(ap, int)) != 0) {
		switch (attr) {
		case MpxNerrorSilent:
			ival = va_arg(ap, int);
			if (ival)
				env = mpxMakeEnv(env, "-S", NULL);
			break;

		case MpxNuiType:
			ival = va_arg(ap, int);
			if (ival != MpxFullGUI && ival != MpxNoGUI)
				ival = MpxFullGUI;
			sprintf(buf, "%d", ival);
			env = mpxMakeEnv(env, "-u", buf, NULL);
			break;

		case MpxNwinXid:
			mpxCntl->component = (jobject) va_arg(ap, void *);
			sprintf(buf, "%d", ival);
			env = mpxMakeEnv(env, "-w", buf, NULL);
			break;

		case MpxNwinDepth:
			mpxCntl->depth = va_arg(ap, int);
			if (mpxCntl->depth != 8 && mpxCntl->depth != 24)
				mpxCntl->depth = 24;
			break;

		case MpxNbackgroundColor:
			ival = va_arg(ap, int);
			sprintf(buf, "%d", ival);
			env = mpxMakeEnv(env, "-b", buf, NULL);
			break;

		case MpxNaudioMode:
			mpxCntl->audioMode = (va_arg(ap, int) & MpxAudioStereo);
			break;

		case MpxNaudioQuality:
			mpxCntl->audioQuality = (va_arg(ap,int) & MpxAudioHiQ);
			break;

		case MpxNvideoInterleave:
			ival = va_arg(ap, int);
			mpxCntl->interleave = (ival ? TRUE : FALSE);
			break;

		case MpxNvideoZoom:
			mpxCntl->zoom = va_arg(ap, int);
			if (mpxCntl->zoom < 1 || mpxCntl->zoom > 3)
				mpxCntl->zoom = 1;
			break;

		case MpxNstrmType:
			strmType = va_arg(ap, int);
			if (strmType < MpxStrm_11172 && strmType > MpxStrm_ASEQ)
				strmType = MpxStrm_11172;
			break;

		case MpxNmute:
			ival = va_arg(ap, int);
			if (ival)
				env = mpxMakeEnv(env, "-M", NULL); 
			break;

		case MpxNinputFile:
			file = va_arg(ap, char *);
			break;

		case MpxNinputNetAddr:
			addr = va_arg(ap, char *);
			break;

		case MpxNinputNetPort:
			port = va_arg(ap, int);
			break;

		case MpxNinputNetTTL:
			ttl = va_arg(ap, int);
			if (ttl < 0)
				ttl = 0;
			else if (ttl > 256)
				ttl = 256;
			break;

		case MpxNclientData:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->clientData = pval;
			break;

		case MpxNjavaClient:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->jmpx = pval;
			break;

		case MpxNjniEnv:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->jniEnv = pval;
			break;

		default:
			fprintf(stderr, "Unknown attribute: %d\n", attr);
		}
	}
	va_end(ap);

	if (file) {
		env = mpxMakeEnv(env, "-f", file, NULL);
		mpxCntl->action = MpxActionPlay;
	} else if (port != -1) {
		/* A net address or port is given. */
		sprintf(buf, "af,inet,udp,li,%s,lp,%d", 
			addr ? addr : "any", port);
		if (ttl != -1) {
			sprintf(tmp, ",lt,%d", ttl);
			strcat(buf, tmp);
		}
		env = mpxMakeEnv(env, "-fn", buf, NULL);
		mpxCntl->action = MpxActionPlay;
	} else if (addr) {
		fprintf(stderr, "mpx: an address %s is given but no port is mentioned\n", addr);
	}

	/* Generate the start-up audio settings */
	if (mpxCntl->audioMode == MpxAudioStereo)
		buf[0] = 'S';
	else if (mpxCntl->audioMode & MpxAudioRight)
		buf[0] = 'R';
	else
		buf[0] = 'L';
	buf[1] = ',';
	if (mpxCntl->audioQuality == MpxAudioHiQ)
		buf[2] = '0';
	else if (mpxCntl->audioQuality & MpxAudioMedQ)
		buf[2] = '1';
	else
		buf[2] = '2';
	buf[3] = '\0';
	env = mpxMakeEnv(env, "-a", buf, NULL);

	/* Generate the start-up video options. */
	strcpy(buf, "col");
	if (mpxCntl->depth == 8) 
		strcat(buf, "8");
	else if (mpxCntl->interleave) 
		strcat(buf, "B");
	sprintf(tmp, ",%d", mpxCntl->zoom);
	strcat(buf, tmp);
	env = mpxMakeEnv(env, "-v", buf, NULL);

	/* Generate the file option */
	if (file && strmType == -1) {
		/* A file is given but no type is specified.
		   Try to auto-detect */
		if ((strmType = isMpegFile(file, 0, 0)) == 0) {
			/* Not an MPEG file */
			fprintf(stderr, "mpx: %s is not an MPEG file\n", file);
			file = NULL;
		}
	}

	if (file || port != -1) {
		switch (strmType) {
		case MpxStrm_VSEQ:
			strcpy(buf, "v"); break;
		case MpxStrm_ASEQ:
			strcpy(buf, "a"); break;
		default:
			strcpy(buf, "m"); break;
		}
		env = mpxMakeEnv(env, "-t", buf, NULL);
		mpxCntl->strmType = strmType;
	}

	/* The env++ is to get rid of the smpx argument */
	mpxCntl = mpxStartThread(mpxCntl, env++);

	if (mpxCntl && id)
	    mpxCntl->parentID = (char *)strdup(id);
#ifndef WIN32
	if (mpxCntl)
	    gettimeofday(&mpxCntl->startTime, NULL);
#endif
	return mpxCntl;
}


int
mpxSet(MpxCntl *mpxCntl, ...)
{
	va_list	ap;
	int	attr, ival;
	char	*sval;
	float	fval;
	void	*pval;
	char	*file = NULL;
	int	strmType = -1;
	int	blockCall = FALSE;

	va_start(ap, mpxCntl);
	while ((attr = va_arg(ap, int)) != 0) {
		switch (attr) {
		case MpxNaudioMode:
			ival = (va_arg(ap, int) & MpxAudioStereo);
			mpxSendAudioMode(mpxCntl, ival);
			mpxCntl->audioMode = ival;
			break;

		case MpxNaudioQuality:
			ival = (va_arg(ap, int) & MpxAudioHiQ);
			mpxSendAudioQuality(mpxCntl, ival);
			mpxCntl->audioQuality = ival;
			break;

		case MpxNvideoInterleave:
			ival = va_arg(ap, int);
			mpxSendInterleave(mpxCntl, (ival ? TRUE : FALSE));
			mpxCntl->interleave = (ival ? TRUE : FALSE);
			break;

		case MpxNvideoZoom:
			ival = va_arg(ap, int);
			if (ival < 1 || ival > 3)
				ival = 1;
			mpxSendZoom(mpxCntl, ival);
			mpxCntl->zoom = ival;
			break;

		case MpxNgamma:
			fval = (float)va_arg(ap, double);
			mpxSendGamma(mpxCntl, fval);
			mpxCntl->gamma = fval;
			break;

		case MpxNstrmType:
			strmType = va_arg(ap, int);
			if (strmType < MpxStrm_11172 && strmType > MpxStrm_ASEQ)
				strmType = MpxStrm_11172;
			break;

		case MpxNinputFile:
			file = va_arg(ap, char *);
			break;

		case MpxNaction:
			ival = va_arg(ap, int);
			if (ival != MpxActionPlay && 
			    ival != MpxActionFwdSpeed &&
			    ival != MpxActionFwdStep && 
			    ival != MpxActionPause)
				ival = MpxActionPlay;

			if (ival != MpxActionFwdSpeed) {
				mpxSendAction(mpxCntl, ival);
				mpxCntl->action = ival;
				mpxCntl->fwdSpeed = 1.0;
			}
			break;

		case MpxNforwardSpeed:
			fval = (float)va_arg(ap, double);
			mpxCntl->fwdSpeed = fval;
			mpxSendAction(mpxCntl, MpxActionFwdSpeed);
			mpxCntl->action = MpxActionFwdSpeed;
			mpxCntl->fwdSpeed = fval;
			break;

		case MpxNseekPos:
			fval = (float)va_arg(ap, double);
			mpxSendSeek(mpxCntl, fval);
			mpxCntl->loc = fval;
			break;

		case MpxNmute:
			ival = va_arg(ap, int);
			mpxSendMute(mpxCntl, (ival ? TRUE : FALSE));
			mpxCntl->muted = (ival ? TRUE : FALSE);
			break;

		case MpxNflush:
			ival = va_arg(ap, int);
			if (ival)
				mpxSendFlush(mpxCntl);
			break;

		case MpxNsync:
			ival = va_arg(ap, int);
			blockCall = (ival ? TRUE : FALSE);
			break;

		case MpxNexit:
			/* Immediately return after processing the
			   the exit command */
			ival = va_arg(ap, int);
			if (ival) {
				mpxSendExit(mpxCntl);

				/* The exit command always blocks */
				/* Wait for acknowledgement before moving on */
				mpxWait(mpxCntl->fd, 5);

				/* MPX has already exited.  So just return */
				return;
			}
			break;

		case MpxNclientData:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->clientData = pval;
			break;

		case MpxNsizeChangeCallback:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->sizeChangeCallback = 
					(MpxSizeChangeCallback)pval;
			break;

		case MpxNstatsUpdateCallback:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->statsUpdateCallback = 
					(MpxStatsUpdateCallback)pval;
			break;

		case MpxNackNotifyCallback:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->ackNotifyCallback = 
					(MpxAckNotifyCallback)pval;
			break;

		case MpxNfileDoneCallback:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->fileDoneCallback =
					(MpxFileDoneCallback)pval;
			break;

		case MpxNexitCallback:
			pval = va_arg(ap, void *);
			if (pval)
				mpxCntl->exitCallback = 
					(MpxExitCallback)pval;
			break;

		default:
			fprintf(stderr, "Unknown attribute: %d\n", attr);
		}
	}
	va_end(ap);

	/* A file is given but no type is specified.  We'll
	   try auto-detect */
	if (file && strmType == -1) {
		if ((strmType = isMpegFile(file, 0, 0)) == 0) {
			/* Not an MPEG file */
			fprintf(stderr, "mpx: %s is not an MPEG file\n", file);
			file = NULL;
		}
	}

	if (file) {
		mpxSendOpenFile(mpxCntl, file);
		mpxCntl->strmType = strmType;
		mpxCntl->action = MpxActionPlay;
	}

	/* If the block flag is set, we'll send an ack command and wait
	   for mpx to return before proceeding */
	if (blockCall)
		return blockForAck(mpxCntl);

	return 0;
}


/*
 * Not implemented yet.
 */
int
mpxGet(MpxCntl *mpxCntl, ...)
{
	va_list	ap;
	int	attr, *ival;
	float	*fval;
	char	**sval;
	void	**pval;

	va_start(ap, mpxCntl);
	while ((attr = va_arg(ap, int)) != 0) {
		switch (attr) {
		case MpxNaudioMode:
			ival = va_arg(ap, int *);
			*ival = mpxCntl->audioMode;
			break;

		case MpxNaudioQuality:
			ival = va_arg(ap, int *);
			*ival = mpxCntl->audioQuality;
			break;

		case MpxNvideoInterleave:
			ival = va_arg(ap, int *);
			*ival = mpxCntl->interleave;
			break;

		case MpxNvideoZoom:
			ival = va_arg(ap, int *);
			*ival = mpxCntl->zoom;
			break;

		case MpxNgamma:
			fval = va_arg(ap, float *);
			*fval = mpxCntl->gamma;
			break;

		case MpxNstrmType:
			ival = va_arg(ap, int *);
			*ival = mpxCntl->strmType;
			break;

		case MpxNaction:
			ival = va_arg(ap, int *);
			*ival = mpxCntl->action;
			break;

		case MpxNforwardSpeed:
			fval = va_arg(ap, float *);
			*fval = mpxCntl->fwdSpeed;
			break;

		case MpxNseekPos:
			fval = va_arg(ap, float *);
			*fval = mpxCntl->loc;
			break;

		case MpxNmute:
			ival = va_arg(ap, int *);
			*ival = mpxCntl->muted;
			break;

		case MpxNclientData:
			pval = va_arg(ap, void **);
			*pval = mpxCntl->clientData;
			break;

		default:
			fprintf(stderr, "Unknown attribute: %d\n", attr);
		}
	}
	va_end(ap);

	return 0;
}


static MpxCntl *
createCntl()
{
	MpxCntl *mpxCntl = (MpxCntl *)calloc(1, sizeof(MpxCntl));

	if (!mpxCntl) return NULL;

	mpxCntl->thrID = NULL;
	mpxCntl->parentID = NULL;
	mpxCntl->fd = 0;
	mpxCntl->audioMode = MpxAudioLeft;
	mpxCntl->audioQuality = MpxAudioLowQ;
	mpxCntl->interleave = TRUE;
	mpxCntl->width = mpxCntl->height = 0;
	mpxCntl->depth = 24;
	mpxCntl->size = 1;
	mpxCntl->loc = 0;
	mpxCntl->time = 0;
	mpxCntl->zoom = 1;
	mpxCntl->frames = 0;
	mpxCntl->strmType = MpxStrm_11172;
	mpxCntl->action = MpxActionPlay;
	mpxCntl->fwdSpeed = 1.0;
	mpxCntl->ackSeq = 1;
	/*	mpxCntl->cmap = None;*/
#ifndef WIN32
	gettimeofday(&mpxCntl->lastStats, NULL);
#endif
	return mpxCntl;
}


void
mpxDestroy(MpxCntl *cntl)
{
	if (cntl->fd > 0) close(cntl->fd);
	if (cntl->parentID) free(cntl->parentID);
	free((char *)cntl);
}


/*#include <X11/Intrinsic.h>*/

MpxCntl *
mpxStartThread(MpxCntl *mpxCntl, char **env)
{
#define ERRPIPE	"Failed to open the MPEG control pipe"
#define ERRTHR	"Failed to start the MPEG thread"
#define ERRMSG	"Failed to initialize the MPEG decoder"

	int 	error;	
	int	cfds[2];
	char	ctl[32];
	/*	sigset_t oldm, newm;*/
	/*	caddr_t stackbase;*/
	typ_mpgbl *gb;

	JNIEnv *jniEnv = (JNIEnv *)mpxCntl->jniEnv;
	jobject jmpx = (jobject)mpxCntl->jmpx;
	jobject thread;

	if ((gb = init_mpx(jniEnv)) == NULL)
		return NULL;

	gb->jmpx = jmpx;

	/* Start the mpx decoding java thread.  The run method of
	   the MpxThread will kick off the mpxThread() function */ 
	thread = GetObjectField(jniEnv, jmpx, "mpxThread",
			"Lcom/sun/media/codec/video/jmpx/MpxThread;");
	SetLongField(jniEnv, thread, "mpxData", (jlong)gb);
	SetLongField(jniEnv, thread, "clientData", (jlong)env);
	CallVoidMethod(jniEnv, thread, "start", "()V");
	(*jniEnv)->DeleteLocalRef(jniEnv, thread);

if (DEBUG)
	fprintf(stderr, "waiting for the threads to come up.\n");
	CallVoidMethod(jniEnv, jmpx, "listenCommand", "()V");
if (DEBUG)
	fprintf(stderr, "finished waiting for the threads to come up.\n");

	if (!mpxCntl && (mpxCntl = createCntl()) == NULL) {
		perror(ERRMSG);
		return  NULL;
	}

	mpxCntl->gb = gb;
	gb->mpxCntl = mpxCntl;
	gb->component = mpxCntl->component;
	gb->window = 0;
	return mpxCntl;

thread_error:
	perror(ERRTHR);
	return NULL;

#undef ERRPIPE
#undef ERRTHR
#undef ERRMSG
}


/*
 * The Java version of the mpx thread.
 */
void *
mpxThread(void *mpgbl)
{
    typ_mpgbl *gb = (typ_mpgbl *)mpgbl;
    char	**argv = (char **)GetLongField(gb->mpx_env, 
					       gb->mpx_thread, "clientData");
    nint c = 0;
    
    gb->mpenv.start_as_thread = 1;
    gb->mpenv.uitype = 2;	/* always disable the GUI */
    
    if (argv)
	while (argv[c]) c++;
    
    /* this is how all things begins */
    
    mpx_main(gb, c, argv);
}


static int
mpxWait(int fd, int t)
{
    return 0;
}


static int
blockForAck(MpxCntl *cntl)
{
	u_int	seq;
	int	cmd, n = 3;

	/* Send the acknowledge command and wait for mpx to reply to it */
	/* Wait up to 3 commands lest we wait too long. */
	cntl->ackSeq++;
	mpxSendAck(cntl);	
	while (n > 0 && mpxWait(cntl->fd, 10) > 0) {

		cmd = mpxProcessInput(cntl, &seq);
		if (cmd < 0)
			return -1;

		if (cmd == MCMD_ACK && seq == cntl->ackSeq)
			break;

		n--;
	}

	return 0;
}


/*
 * Build a argv style list to be used as the initial startup environment.
 */
char **
mpxMakeEnv(char **argv, ...)
{
	va_list ap;
	char *arg;
	int c = 0;

	if (argv == NULL) {
		argv = (char **)malloc(MAX_ENV_LEN * sizeof(char *));
		argv[0] = NULL;
	} else {
		argv;
		while (argv[c] && c < MAX_ENV_LEN) c++;
	}

	va_start(ap, argv);
	while ((arg = va_arg(ap, char *)) != NULL && c < MAX_ENV_LEN)
		argv[c++] = (char *)strdup(arg);
	argv[c] = NULL;
	va_end(ap);

	return argv;
}


/*
 * Destroy the argv list generated by mpxMakeEnv()
 */
void
mpxDestroyEnv(char **argv)
{
	int	c;

	for (c = 0; c < MAX_ENV_LEN; c++) {
		if (argv[c])
			free(argv[c]);
	}
	free(argv);
}

#ifdef NOTUSED
void
mpxXtInputCallback(MpxCntl *mc, int *dummy1, XtInputId *dummy2)
{ 
	mpxProcessInput(mc, NULL);
}
#endif


/**************************************************
 * Code to check the MPEG type.
 **************************************************/

#define BSIZ 300

typedef struct BufStrm {
	JNIEnv *env;
	jobject strm;
	jbyteArray jary;
	char *buf;
	char *bp;
	FILE *fp;
	int size;
	int (*read)(struct BufStrm *);
} BufStrm;

int isMpegFile(char *file, int *width, int *height);

int
bufRead(BufStrm *js)
{
	while (js->size <= 0) {
		js->size = (js->read)(js);
		if (js->size < 0 || js->size == EOF)
			return EOF;
		js->bp = js->buf;
	}
	js->size--;
	return (int)((*js->bp++) & 0xFF);
}


int
bufReadFile(BufStrm *js)
{
    int returnVal;
    returnVal = fread(js->buf, 1, BSIZ, js->fp);
    if (returnVal <= 0) {
	if (feof(js->fp))
	    return EOF;
    } else
	return returnVal;
}


int
bufReadStrm(BufStrm *js)
{
    int retVal;
    (*js->env)->ReleaseByteArrayElements(js->env, js->jary, 
					 (jbyte *)js->buf, JNI_ABORT);
    retVal = CallIntMethod(js->env, js->strm, "read", "([BII)I", js->jary, 0, BSIZ);
    js->buf = (char *)(*js->env)->GetByteArrayElements(js->env, js->jary, 0);
    return retVal;
}


int
bufOpenFile(BufStrm *js, char *filename)
{
#ifdef WIN32
    while (strlen(filename) > 2 &&
	   filename[0] == '/' &&
	   (filename[1] == '/' || filename[2] == ':')) {
	filename++;
    }
    js->fp = fopen(filename, "rb");
#else
    js->fp = fopen(filename, "r");
#endif
    js->buf = (char *)malloc(sizeof(char) * BSIZ);
    js->bp = js->buf;
    js->size = 0;
    js->read = bufReadFile;
    return (js->fp == NULL ? -1 : 0);
}


void
bufOpenStrm(BufStrm *js, JNIEnv *env, jobject strm)
{
	js->env = env;
	js->strm = strm;
	js->jary = (*env)->NewByteArray(env, BSIZ);
	js->buf = (char *)(*env)->GetByteArrayElements(env, js->jary, 0);
	js->bp = js->buf;
	js->size = 0;
	js->read = bufReadStrm;
}


void
bufFreeFile(BufStrm *bs)
{
	fclose(bs->fp);
	free(bs->buf);
}
 

void
bufFreeStrm(BufStrm *js)
{
	(*js->env)->ReleaseByteArrayElements(js->env, js->jary, 
				(jbyte *)js->buf, JNI_ABORT);
}

int checkMpegStream(BufStrm bs, int *, int *);

int
isMpegFile(char *file, int *width, int *height)
{
	BufStrm bs;
	int rtn;

	bufOpenFile(&bs, file);
	rtn = checkMpegStream(bs, width, height);
	bufFreeFile(&bs);
	return rtn;
}

int
isMpegStream(JNIEnv *env, jobject strm, int *width, int *height)
{
	BufStrm bs;
	int rtn;

	bufOpenStrm(&bs, env, strm);
	rtn = checkMpegStream(bs, width, height);
	bufFreeStrm(&bs);
	return rtn;
}


/*
 * A kludge to auto-detect the stream type of an MPEG-I file.
 * It's not 100% accurate.
 */
int
checkMpegStream(BufStrm bs, int *width, int *height)
{
    int ch;
    int i = 0;
    int sysFound = 0;
    int vseqFound = 0, aseqFound = 0;
    
    /* Search for the start code for at most 64k characters. */
    while ((ch = bufRead(&bs)) != EOF && ++i < 64000) {
	if (ch == 0) {
	    ch = bufRead(&bs); i++;
	    if (ch == EOF)
		break;
	    else if (ch == 0) {
		ch = bufRead(&bs); i++;
		if (ch == EOF)
		    break;
		else if (ch == 1) {
		    /* Found start code. */
		    ch = bufRead(&bs);
		    if (ch == EOF)
			break;
		    switch ((u_char)ch) {
		    case 0xBA:
		    case 0xBB:
			sysFound = 1;
			break;
		    case 0xB3:
			vseqFound = 1;
			
			{
			    int size = 0;
			    int c2;
			    if ((c2 = bufRead(&bs)) != EOF) {
				++i;
				size = size | ((int)c2 << 16);
				if ((c2 = bufRead(&bs)) != EOF) {
				    ++i;
				    size = size | ((int)c2 << 8);
				    if ((c2 = bufRead(&bs)) != EOF) {
					++i;
					size = size | ((int)c2);
					if (width != 0)
					    *width = size >> 12;
					if (height != 0)
					    *height = size & 0xFFF;
					
					if (sysFound)
					    return MpxStrm_11172;
				    }
				}
			    }
			}
		    default:
			aseqFound = 1;
		    }
		}
	    }
	}
	if (ch == EOF)
	    break;
    }
    if (sysFound)
	return MpxStrm_11172;

    if (vseqFound)
	return MpxStrm_VSEQ;
    
    /* Before we can truly identify the audio sequence,
       I'll just default it to system stream first since
       audio-only stream is so rare... */
    if (aseqFound)
	return MpxStrm_11172;
    
    return 0;
}

static int audio_bitrate1[] =
{
    -1, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, -1,
    -1, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1
};

static int audio_bitrate2[] =
{
    -1, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1,
    -1, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, -1
};

int
getMediaDuration(char * filename)
{
    FILE *fp;
    unsigned char peekBuffer[65536], ch;		    /* Buffer to read into*/
    int hours, minutes, seconds;
    int index;
    int size;
    int streamType = 0;
    int startTime = -1;
    int firstTime = 1;
    int timeCode;
    int SANE_VALUE = 20 * 3600;				    /* 20 hours */
    int fileLength;
    int checkCode;
    int firstClockRef = -1;
    int lastClockRef = -1;
    int clock;
#ifdef WIN32
    while (strlen(filename) > 2 &&
	   filename[0] == '/' &&
	   (filename[1] == '/' || filename[2] == ':'))
	filename++;
#endif    
    fp = fopen(filename, "rb"); 
    if (fp == NULL)
	return -1;					    /* dunno */
    fseek(fp, -1, SEEK_END);
    fileLength = ftell(fp);
    fseek(fp, 0, SEEK_SET);
    while (1) {
	size = fread(peekBuffer, 1, 65536, fp);
	if (size <= 0) {
	    break;
	}

	/* Look for a video/system stream */
	index = 0;
	checkCode = 0xFFFFFF;
	while (index < size - 4) {
	    ch = peekBuffer[index++];
	    checkCode = ((checkCode << 8) & 0xFFFFFF) | ch;
	    if (checkCode == 0x000001) {
		ch = peekBuffer[index++];
		switch (ch) {
		case 0xBA: /* Pack_start_code */
		    {
			unsigned char ch0 = peekBuffer[index++];
			unsigned char ch1 = peekBuffer[index++];
			unsigned char ch2 = peekBuffer[index++];
			unsigned char ch3 = peekBuffer[index++];
			unsigned char ch4 = peekBuffer[index++];
			if ((ch0 & 0xF0) != 0x20)
			    break;
			clock = ((ch0 & 0x0E) << (26 - 1)) |
			        ((ch1       ) << (18    )) |
			        ((ch2 & 0xFE) << (11 - 1)) |
			        ((ch3       ) << (3     )) |
			        ((ch4 & 0xFE) >> (4     ));
			if (firstTime && firstClockRef == -1) {
			    firstClockRef = clock;
			} else {
			    lastClockRef = clock;
			}
		    }
		    break;
		    
		case 0xB8:
		    {
			int i = -4;
			unsigned char c0, c1, c2, c3, c4, c5, c6;
			while (i < 1000 && (index + i) < size) {
			    ch = peekBuffer[index+i];
			    /*				//printf("%03d = %02X\n", i, ch);*/
			    c6 = c5;
			    c5 = c4;
			    c4 = c3;
			    c3 = c2;
			    c2 = c1;
			    c1 = c0;
			    c0 = ch;
			    
			    if (c6 == 0) {
				if (c5 == 0)
				    if (c4 == 0x01)
					if (c3 == 0xB8) {
					    streamType = 2;
					    timeCode = (c2 << 16) |
						(c1 << 8)  |
						(c0);
					    
					    hours = (timeCode >> 18)   & 0x1F;
					    minutes = (timeCode >> 12) & 0x3F;
					    seconds = (timeCode >> 5)  & 0x3F;
					    i = 1001;
					    
					    if (startTime == -1) {
						startTime = seconds +
						    minutes * 60 +
						    hours * 3600;
					    }
					    
					}
			    }
			    
			    i++;
			}
		    }
		    break;
		}
	    }
	}
	
	/* look for an audio stream */
	if (  firstTime  && fileLength > 0 && streamType < 2) {
	    index = 0;
	    while ( index < size - 4 ) {
		if (peekBuffer[index] == 0xff) {
		    if ((peekBuffer[index + 1] & 0xF8) == 0xF8) {
			/* Its an audio stream */
			int id = (peekBuffer[1] >> 3) & 1;
			int layer = 4 - ((peekBuffer[1] >> 1) & 0x3);
			int index = (peekBuffer[2] >> 4) & 0x0F;
			int bitRate;
			if (layer == 1)
			    bitRate = audio_bitrate1[index + id * 16];
			else if (layer == 2)
			    bitRate = audio_bitrate2[index + id * 16];
			else
			    return -1;			    /* Don't support layer
							       3*/
			if (bitRate <= 0)
			    return -1;
			seconds = fileLength / ((bitRate * 1024) / 8);
			if (seconds > 0 && seconds < SANE_VALUE)
			    return seconds;
			else
			    return -1;
		    }
		}
		index++;
	    }
	}

	if (firstTime) {
	    firstTime = 0;
	    fseek(fp, -128 * 1024, SEEK_END);
	}   
    }

    fclose(fp);

    /*    // If the first 13 bits are "1111 1111 1111 1" then its an audio stream*/
    /*    // This is needed because some audio streams have a System stream start*/
    /*    // code embedded in the data.*/
    
    if (peekBuffer[0] == 0xFF &&
	(peekBuffer[1] & 0xF8) == 0xF8)
	streamType = 1;

    seconds = seconds + hours * 3600 + minutes * 60;
    if (startTime >= 0 && startTime < SANE_VALUE)
	seconds -= startTime;
    /*    printf("Time = %d\n", seconds);*/
    if (firstClockRef != -1 && lastClockRef != -1) {
	/* Lets use the clock */
	if (firstClockRef > lastClockRef) {
	    firstClockRef = firstClockRef - (1 << 29);
	}
	clock = lastClockRef - firstClockRef;
	seconds = clock / 5625;
	/*	printf("Revised time = %d\n", seconds);*/
    }
    if (seconds > 0 && seconds < SANE_VALUE)
	return seconds;
    else
	return -1;
}
