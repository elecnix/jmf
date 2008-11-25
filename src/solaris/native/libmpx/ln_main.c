/*
 * @(#)ln_main.c	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <jni-util.h>

#include "mp_mpd.h"
#include "mp_aud.h"
#include "ln_lnd.h"
#include "mpx.h"

/* #define STANDALONE */

#define	 IPC_PERMS	0777	/* Read, Alter/Write by everyone */
#define  INADDR_NONE	-1
#define	 DPORT		7654

static typ_mpgbl *mpgblq[MAX_MPX_INSTANCE];
static nint silent = 0;	/* verbose flag */
static nint muted = 1;
static nint start_as_muted = 0;
static nint sema_unique_key = 12345;

#ifdef NOTUSED
static ubyte sig_terminate[] = 
   { SIGINT, SIGQUIT, SIGILL, SIGTRAP, SIGABRT, SIGEMT, SIGFPE, SIGBUS, SIGSEGV,
     SIGTERM, SIGSYS, 0};
#endif

#ifndef WIN32
#pragma align 32 (gvv)		/* So that we can use ldfd, stfd */
#endif

#ifdef WIN32
#define off_t int32
#define valloc malloc
#endif

static char pwdalp[64];

#define	 O_PASSWD    0
#define	 O_AUDMODE   1
#define	 O_VIDMODE   2
#define	 O_TIMING    3
#define	 O_GAMMA     4
#define	 O_CDSCSI    5	   /* SCSI cont/id of the default CDROM device */
#define	 O_NETWORK   6

static struct { char *argnm; nshort argid; }  optnms[] = {
   "passwd:",     O_PASSWD,
   "audio_mode:", O_AUDMODE,
   "video_mode:", O_VIDMODE,
   "vak:", O_TIMING,
   "gamma:", O_GAMMA,
   "cdscsi:",O_CDSCSI,
   "network:",O_NETWORK,
   NULL,0 };

typ_vmodes vmodes[NVDM] = {
   "col",   VDM_COL,
   "colB",  VDM_COLB,
   "col8",  VDM_COL8,
    /* These below are just for backwards compatibility with SGI version*/
   "col24", VDM_COL,
   "col24B",VDM_COLB, 
   "mono",  VDM_COL };

int DEBUG = FALSE;

/* #define TIMEBOMB */
#ifdef TIMEBOMB
/*
 * Check for the time and exit if it passes the time limit.
 */
int
time_expired()
{
#define XDAY	1
#define XMONTH	4
#define XYEAR	97

	time_t		tt;
	struct tm	t;
	struct timeval	tval;

	memset(&t, 0, sizeof(t));
	t.tm_mon = XMONTH-1;
	t.tm_mday = XDAY;
	t.tm_year = XYEAR;
	t.tm_isdst = -1;

	tt = mktime(&t);
	gettimeofday(&tval, NULL);

	return (tval.tv_sec > tt);

#undef XDAY
#undef XMONTH
#undef XYEAR
}
#endif


static void
close_src(JNIEnv *env, typ_mpgbl *gb)
{
   if (gb->dsenv.state != DSS_IDLE) idle_dserver(env, gb);
#ifndef WIN32
   if (gb->dsrc.type & DSRC_SOCKET) shutdown(gb->dsrc.fid,2);
#endif
   if (gb->dsrc.fid != -1) close(gb->dsrc.fid);
   gb->dsrc.fid = -1;
   gb->dsrc.type = DSRC_NONE;
   gb->dsrc.bstrm = 0;
   /*   reset_display(gb);*/
   gb->mpenv.state &= 0xffff ^ (MPS_DSRC | MPS_DSYNC);
}  



void
appterminate(JNIEnv *env, typ_mpgbl *gb, ubyte exitcode)
{  
   int i;

   if (gb->exit)
      return;

   if (gb == NULL)
      exit(exitcode);

   if (exitcode == 111) gb->mpenv.errors = 0x88;
   if (gb->mpenv.errors) 
      mp_fprintf("MpegExpert Err_Report: %x\n",gb->mpenv.errors);

   /* Just exit the process if mpx is not started as a thread */
   if (!gb->mpenv.start_as_thread)
      exit(exitcode);

   gb->exit = 1;

   /* Clean up the gbl queue */
   for (i = 0; i < MAX_MPX_INSTANCE; i++)
      if (mpgblq[i] == gb)
      { mpgblq[i] = NULL; break;}

   /* Kill the data server thread and wait for the signal back before
      proceeding. */
   kill_dserver(gb->mpx_env, gb);

   SEMA_WAIT(env, &gb->ds_deadsync);
   SEMA_DESTROY(env, &gb->ds_deadsync);

   gb->dsenv.state = DSS_IDLE;
   close_src(env, gb);

   /* Clean up X */
   /* XCloseDisplay(gb->vdec.xdisplay); */

   /* Clean up the semaphores */
   SEMA_DESTROY(env, &gb->dsmsgq.incr);
   SEMA_DESTROY(env, &gb->dsmsgq.decr);
   SEMA_DESTROY(env, &gb->dssync.incr);
   SEMA_DESTROY(env, &gb->dssync.decr);
   SEMA_DESTROY(env, &gb->dssync.cmdsync);
   SEMA_DESTROY(env, &gb->cmdq.incr);
   SEMA_DESTROY(env, &gb->cmdq.decr);

   /* Clean up the buffers */
   (*env)->ReleaseByteArrayElements(env, gb->mpenv.joperb, 
			(jbyte *)gb->mpenv.operb, 0);
   (*env)->DeleteGlobalRef(env, gb->mpenv.joperb);

   free((char *)gb->adec.audbuf);
   free((char *)gb->mpenv.pictb);
   free((char *)gb->vdec.denv.tcfb);
   free((char *)gb->adec.smp);
   free((char *)gb->adec.vv);

   close_audio(env, gb);

/*
   if (gb->converter != 0)
	YR_close(gb->converter);
*/

   /* Close control channel */
   if (gb->mpenv.cntfid > 0)
      close(gb->mpenv.cntfid);

   (*env)->DeleteGlobalRef(env, gb->jmpx);
   (*env)->DeleteGlobalRef(env, gb->mpx_thread);
   (*env)->DeleteGlobalRef(env, gb->ds_thread);
   if (gb->dsrc.jstrm)
      (*env)->DeleteGlobalRef(env, gb->dsrc.jstrm);
}


void
mp_fprintf(char *fmt, ...)
{  va_list ap;
   if (silent) return;
   va_start(ap, fmt);
   vfprintf(stderr, fmt, ap);
   va_end(ap);
}


void
mp_printf(char *fmt, ...)
{  va_list ap;
   if (silent) return;
   va_start(ap, fmt);
   vprintf(fmt, ap);
   va_end(ap);
}


static void
init_aseqplay(typ_mpgbl *gb, uint32 iosize)
{  nuint nblk = (256*1024) / iosize;
   union semun {
   int val;
   struct semid_ds *buf;
   ushort *array;
   } semarg;
   
   gb->audb.b0 = ALIGN_PAGE(gb->mpenv.operb);
   gb->audb.b1 = gb->audb.b0 + 16*1024;
   gb->audb.b2 = gb->audb.b1 + nblk * iosize;
   gb->audb.b3 = gb->audb.b2 + 4*1024;	    /* redundant */

   gb->dsenv.blksz = iosize;
   gb->dsenv.syncrd_state = 0;
   gb->dsenv.nblk = nblk;
   
   gb->audb.dsrcend = 0;	  
   gb->audb.wp = gb->audb.b1;
   gb->audb.rp = gb->audb.b1;
   gb->audb.ready = 0;
   gb->audb.tacq = 0;
 
if (DEBUG)
   fprintf(stderr, "SEMA_INIT dssync\n");
   SEMA_INIT(gb->mpx_env, &gb->dssync.incr, 0, USYNC_THREAD, NULL);
   SEMA_INIT(gb->mpx_env, &gb->dssync.decr, nblk - 1, USYNC_THREAD, NULL);
}

static void
init_vseqplay(typ_mpgbl *gb, uint32 iosize)
{  nuint nblk = (384*1024) / iosize;
   union semun {
   int val;
   struct semid_ds *buf;
   ushort *array;
   } semarg;
 
   gb->vdec.vbsz   = nblk * iosize;
   gb->vdec.vbsqsz = 64*1024;
   gb->vdec.strm = 0;
   gb->vidb.b0 = ALIGN_PAGE(gb->mpenv.operb);
   gb->vidb.b1 = gb->vidb.b0 + gb->vdec.vbsqsz;
   gb->vidb.b2 = gb->vidb.b1 + gb->vdec.vbsz;
   gb->vidb.b3 = gb->vidb.b2 + 4*1024;
   
   gb->dsenv.blksz = iosize;
   gb->dsenv.syncrd_state = 0;
   gb->dsenv.nblk = nblk;

   gb->vidb.dsrcend = 0;	  
   gb->vidb.wp = gb->vidb.b1;
   gb->vidb.rp = gb->vidb.b1;
   gb->vidb.ready = 0;
   gb->vidb.tacq = 0;
   gb->vidb.full = nblk*iosize;

   /* Create the PANIC region for the Video buffer */
   {  nint k;
      uint32 w;
      ubyte *q = gb->vidb.b2;      
      q[0] = 0;
      q[1] = 0;
      q[2] = 1;
      q[3] = ST_PANIC;
      w = *(uint32 *)q;	   /* This guarantees being Endian free */
      for (k=0;k<256;k++)
	 ((uint32 *)q)[k] = w;
   }  

if (DEBUG)
   fprintf(stderr, "SEMA_INIT dssync\n");
   SEMA_INIT(gb->mpx_env, &gb->dssync.incr, 0, USYNC_THREAD, NULL);
   SEMA_INIT(gb->mpx_env, &gb->dssync.decr, nblk - 1, USYNC_THREAD, NULL);
}


static void
init_11172play(typ_mpgbl *gb, uint32 iosize)
{  register uint32 w;
   register nint k;
   register ubyte *q;
   nuint nblk;
   union semun {
   int val;
   struct semid_ds *buf;
   ushort *array;
   } semarg;

   nblk = (256*1024)/iosize;
   gb->dsenv.blksz = iosize;
   gb->dsenv.nblk = nblk;
   gb->dsenv.syncrd_state = 0;
   
   gb->dmxb.b0 = ALIGN_PAGE(gb->mpenv.operb);
   gb->dmxb.b1 = gb->dmxb.b0 + 4*1024;
   gb->dmxb.b2 = gb->dmxb.b1 + iosize*nblk;
   w = iosize*nblk + 4096 + 2048;
   gb->dmxb.b3 = gb->dmxb.b1 +(( w>>12)<<12);
   
   gb->vidb.b0 = gb->dmxb.b3;
   gb->vidb.b1 = gb->vidb.b0 + 64*1024;
   gb->vidb.b2 = gb->vidb.b1 + 384*1024;
   gb->vidb.b3 = gb->vidb.b2 + 4*1024;
   gb->vdec.vbsz = 384*1024;
   gb->vdec.vbsqsz = 64*1024;
   
   gb->audb.b0 = gb->vidb.b3;
   gb->audb.b1 = gb->audb.b0 + 16*1024;
   gb->audb.b2 = gb->audb.b1 + 128*1024;
   gb->audb.b3 = gb->audb.b2 + 4*1024;
	  
   gb->dmxb.rp = gb->dmxb.b1;
   gb->dmxb.wp = gb->dmxb.b1;
   gb->dmxb.ready = 0;
   gb->dmxb.dsrcend = 0;
   gb->dmxb.tacq = 0;
   	  
   /* Create the PANIC region for the Demultiplexer buffer
      There is no alignment problem here as long as iosize is a multiple of 4
   */
   q = gb->dmxb.b2;
   q[0] = 0;
   q[1] = 0;
   q[2] = 1;
   q[3] = ST_PANIC;
   w = *(uint32 *)q;	/* This guarantees being Endian free */
   for (k=0;k<256;k++)  ((uint32 *)q)[k] = w;
   
   /* Create the PANIC region for the Video buffer */
   q = gb->vidb.b2;
   q[0] = 0;
   q[1] = 0;
   q[2] = 1;
   q[3] = ST_PANIC;
   for (k=0;k<256;k++)  ((uint32 *)q)[k] = w;
   
   gb->vidb.wp = gb->vidb.b1;
   gb->vidb.rp = gb->vidb.b1;
   gb->vidb.full = 384*1024;
   gb->vidb.ready = 0;
   gb->vidb.dsrcend = 0;
   
   gb->audb.wp = gb->audb.b1;
   gb->audb.rp = gb->audb.b1;
   gb->audb.full = 72*1024;
   gb->audb.ready = 0;
   gb->audb.dsrcend = 0;
   
   reset_stamps(&gb->adec.stamps);
   reset_stamps(&gb->vdec.stamps);

if (DEBUG)
   fprintf(stderr, "SEMA_INIT dssync\n");
   SEMA_INIT(gb->mpx_env, &gb->dssync.incr, 0, USYNC_THREAD, NULL);
   SEMA_INIT(gb->mpx_env, &gb->dssync.decr, nblk - 1, USYNC_THREAD, NULL);
}


nint
init_audio(JNIEnv *env, typ_mpgbl *gb)
{
   /* P e r f o r m   A u d i o  I n i t i a l i z a t i o n s  */
#ifdef JAVA_SOUND

   jobject joperb;

   if (gb->adec.joperb != NULL)
      return 0;

   if (!CallBooleanMethod(env, gb->jmpx, "initAudio", "()Z")) {
       gb->mpenv.state |= MPS_AUDOFF; 
       fprintf(stderr, "Failed to initialize audio for mpx.\n");
   } else {
      /* Create the operation buffers. */
      joperb = (*env)->NewByteArray(env, 256*1024u);
      if (joperb == 0) {
	 /* Failed to create the operation buffers. */
         gb->mpenv.state |= MPS_AUDOFF; 
      } else {
	 gb->adec.joperb = (*env)->NewGlobalRef(env, joperb);
	 gb->adec.buf = (ubyte *)(*env)->GetByteArrayElements(env, gb->adec.joperb, 0);
	 (*env)->DeleteLocalRef(env, joperb);
	 gb->mpenv.state &= ~MPS_AUDOFF; 
	 mp_initaudio();
	 muted = 0;
      }
   }

#else /* !JAVA_SOUND */

   /* It's already initialized */
   if (gb->adec.adev != -1)
      return 0;

   if (muted)
      gb->adec.adev = open("/dev/audio", O_WRONLY | O_NONBLOCK);
   else
      gb->adec.adev = -1;

   if (-1 == gb->adec.adev)
   {  gb->mpenv.state |= MPS_AUDOFF;
      
   }
   else if ( ! test_auddev(gb) )
   {  gb->mpenv.state |= MPS_AUDOFF;
      close(gb->adec.adev);
      scrprint("\nInsufficient audio device capabilities.");
   }
   else
   {  nint flags = fcntl(gb->adec.adev, F_GETFL, 0);
      fcntl(gb->adec.adev, F_SETFL, flags | O_NONBLOCK);
      gb->mpenv.state &= ~MPS_AUDOFF; 
      mp_initaudio();
      muted = 0;
   }

#endif /* JAVA_SOUND */

   if ( gb->mpenv.state & MPS_AUDOFF )
   {
       /*      scrprint("\nAudio is turned off.");*/
      gb->mpenv.astrm = STRM_SBCOFF;
      return -1;
   }
   return 0;
}


void
close_audio(JNIEnv *env, typ_mpgbl *gb)
{
#ifdef JAVA_SOUND
   if (gb->adec.joperb == NULL)
      return;
   CallVoidMethod(env, gb->jmpx, "closeAudio", "()V");
   (*env)->ReleaseByteArrayElements(env, gb->adec.joperb, 
				(jbyte *)gb->adec.buf, 0);
   (*env)->DeleteGlobalRef(env, gb->adec.joperb);
   if (gb->adec.jdev != NULL)
      (*env)->DeleteGlobalRef(env, gb->adec.jdev);
   gb->adec.joperb = NULL;
   gb->adec.buf = NULL;
   gb->adec.jdev = NULL;
#else
   if (gb->adec.adev > 0) {
      close(gb->adec.adev);
      muted = 1;
   }
   gb->adec.adev = -1;
#endif /* JAVA_SOUND */
   gb->mpenv.state |= MPS_AUDOFF;
}


nint
process_command(JNIEnv *env, typ_mpgbl *gb, uint16 busy)
{  typ_cntcmd cntcmd;
   typ_dsmsg dsmsg;
   uint32 cmd, action;
   nint eatenit = 0;
   
   cntcmd = gb->cmdq.cmds[gb->cmdq.rp];
   
#if 0
   while ( (cntcmd.flags & MCFL_ORGMPX) && CMDRP_NEXT != cmdwp)
      cmdrp = CMDRP_NEXT;
   cmdbf[cmdrp] = cntcmd;
#endif


   if (cntcmd.flags & MCFL_SNDACK)
   {  cntcmd.flags ^= MCFL_SNDACK;
      send_ack(gb, &cntcmd);
   }

   cmd = cntcmd.cmd;

   switch ( cmd ) {
   case MCMD_TEST :
       /*      scrprint("\nCommand Test...");*/
      break;

   case MCMD_ACK :
      /* Do nothing.  It's already been handled by the send_ack() above. */
      break;
   
   case	MCMD_EXIT :
      appterminate(env, gb, 0);
      eatenit = 1;
      return (0);
   
   case MCMD_OPENSRC :
      if (busy != BSY_IDLE) {
         cmd_retain(env, &gb->cmdq);	/* leave the current command on the queue */
	 return(0);
      }
      cmd_remove(env, &gb->cmdq); eatenit = 1;
      close_src(env, gb);
      /* Reset all the capture state */
      gb->capt.mrk1.hdr = 0;
      gb->capt.mrk2.hdr = 0;
      gb->capt.shdr1[3] = 0;
      gb->capt.vstrm = 0;	/* This is redundant, just in case measure */
      gb->adec.frmhdr = 0;
      gb->vdec.gopval = 0;
      gb->vdec.fbs = 8;	/* Invalidate frame pointer */
      gb->adec.entry = gb->vdec.entry = 0;

      /* Initialize audio if it's not a video-only file */
      if (cntcmd.u.opensrc.data != BSTRM_VSEQ)
	 init_audio(gb->mpx_env, gb);

      if ((gb->mpenv.state & MPS_AUDOFF) && gb->mpenv.bstrm == BSTRM_ASEQ) break;
   
      {	 uint32 iosz; 
	 nint ftype = cntcmd.u.opensrc.type;
	 nint bstrm = cntcmd.u.opensrc.data;
	 nint strms = cntcmd.u.opensrc.strms;
	 typ_circb *bfp;
	 void (* playfunc)(typ_mpgbl *, nint);
	 
	 if (ftype == MSC_CDFILE)
	 {  
	    fprintf(stderr, "CD files not supported\n");
	    exit(1);
	 }
	 else
	 {  nint flags = cntcmd.u.opensrc.flags;
	    gb->dsrc.s.fdscp = cntcmd.u.opensrc.fdscp;
	    
	    if (! opensrc(env, gb, ftype, (void *)0)) break;
	    dsmsg.m.cmd1.fid = gb->dsrc.fid;
	    iosz = 64*1024;
	    gb->dsrc.bstrm = bstrm;
	    if ( !(gb->dsrc.type & DSRC_FWDONLY))
	    {  if (flags & MRE_FOFS)
	       {  float tf = cntcmd.u.opensrc.fofs;
		  if (tf < 0 ) tf = 0;
		  if (tf > 1.0 ) tf = 1.0;
		  gb->dsrc.ofs = (uint32) (gb->dsrc.size * tf);
	       }
	       else
	       {  uint32 ofs = cntcmd.u.opensrc.ofs;
	          if (ofs > gb->dsrc.size) ofs = gb->dsrc.size;
	          gb->dsrc.ofs = ofs;
	       }
	       mpx_seek(gb, gb->dsrc.fid, (off_t)gb->dsrc.ofs, SEEK_SET);
	    }
	 }
	 if (gb->dsrc.type &  DSRC_FWDONLY) gb->dsrc.size = 0xffffffffLU;
	 gb->dinf.loc = 0;
	 switch (gb->dsrc.bstrm) {
	 case BSTRM_ASEQ:
	    init_aseqplay(gb, iosz);
	    bfp = &gb->audb;
	    playfunc = fwd_aseq;
	    gb->adec.ahead = 0.8;
	    break;
	 case BSTRM_11172:
	    init_11172play(gb, iosz);
	    bfp = &gb->dmxb;
	    playfunc = fwd_11172;
	    gb->vdec.strm = 255;	/* An impossible one */
	    {  nint k = 0;
	       /* Erase all stream info memory */
	       do gb->vdec.strms[k].state = 0; while ( ++k < 16);
	    }
	    /* This below will be changed, once we have the streams menu */
	    gb->mpenv.astrm = strms & 0xff;
	    gb->mpenv.vstrm = (strms >> 8) & 0xff;

	    if ( gb->mpenv.state & MPS_AUDOFF) gb->mpenv.astrm |= STRM_SBCOFF;

	    set_dmxaction(gb->dmx_actions, gb->mpenv.astrm, gb->mpenv.vstrm);
	    gb->adec.ahead = 0.8;
	    if (gb->vdec.maxnrmdel < 0.8) gb->adec.ahead = gb->vdec.maxnrmdel;
	    break;
	 case BSTRM_VSEQ:
	    init_vseqplay(gb, iosz);
	    bfp = &gb->vidb;
	    playfunc = fwd_vseq;
	 }
	 if (ftype == MSC_CDFILE) dsmsg.m.cmd2.sfp = bfp;	 	    
	 else dsmsg.m.cmd1.sfp = bfp;
	    
	 dsmsg.cmd = DSCMD_SYNCREAD;
	 dsmsg.cmdflags = 0;
	 dsmsgsnd(env, gb, &dsmsg);
	 gb->mpenv.state |= MPS_DSRC | MPS_DSYNC;
	 gb->vdec.state = VD_BROKEN;
	 mp_claudbf((float *)gb->adec.vv);
	 /*	 (*playfunc)(gb, PC_PLAY);*/ /* Amith - dont start on opening the source */
       }      
      /* Report back the size of the file and loc of the file */
      send_stats(gb, 0);
      break;
   case MCMD_CLOSESRC:
      if (busy != BSY_IDLE) {
	 cmd_retain(env, &gb->cmdq);	/* leave the current command on the queue */
	 return(0);
      }
      close_src(env, gb);
      break;
   case MCMD_REENTER:
   {  nint flags, strms;

      if ( ! (gb->mpenv.state & MPS_DSRC) ) break;
      if ( (gb->dsrc.type & DSRC_FWDONLY) && (cntcmd.flags & MCFL_ORGMPX)) break;
      if ( ! (busy & (BSY_FWDASEQ | BSY_FWDVSEQ | BSY_FWD11172 | BSY_IDLE))) break;    
      if (busy != BSY_IDLE) {
	 cmd_retain(env, &gb->cmdq);	/* leave the current command on the queue */
	 return(0);
      }
      cmd_remove(env, &gb->cmdq); eatenit = 1;

      flags = cntcmd.u.reenter.flags;
      strms = cntcmd.u.reenter.strms;

      if ( !(gb->dsrc.type & DSRC_FWDONLY)) flags &= 0xffff ^ MRE_ASOPEN;
      if ( flags & MRE_ASOPEN )
      {  flags |= MRE_STRMS | MRE_SEEKVSEQ;
         gb->dsrc.bstrm = cntcmd.u.reenter.data;
         gb->vdec.fbs = 8;
      }
             
      if (gb->dsenv.state != DSS_IDLE) idle_dserver(env, gb);
      if (flags & MRE_SEEKVSEQ) 
      {  nint k;
         gb->vdec.state = VD_BROKEN;
         for (k=0; k<16; k++) gb->vdec.strms[k].state = 0;
      }
      else gb->vdec.state |= VD_BROKEN | VD_LOOKVSEQ;
      
      {  uint32 iosz;	    
	 typ_circb *bfp;
	 void (* playfunc)(typ_mpgbl *, nint);
	    
	 if (gb->dsrc.type & DSRC_CD)
	 {  uint32 blkadr;
	    iosz = 27 * gb->dsrc.cdsect;  
	    blkadr = gb->dsrc.cdadr + (uint32) (gb->dsrc.cdfsz * cntcmd.u.reenter.fofs);
	    dsmsg.m.cmd2.blkadr = blkadr;
	    dsmsg.m.cmd2.nblk = gb->dsrc.cdfsz - (blkadr - gb->dsrc.cdadr);
	    gb->dsrc.ofs = gb->dsrc.cdsect * (blkadr - gb->dsrc.cdadr);
	 }
	 else
	 {  iosz = 64*1024;
	    dsmsg.m.cmd1.fid = gb->dsrc.fid;	    
	    if ( !(gb->dsrc.type & DSRC_FWDONLY))
	    {  if (flags & MRE_FOFS)
	       {  float tf = cntcmd.u.reenter.fofs;
		  if (tf < 0 ) tf = 0;
		  if (tf > 1.0 ) tf = 1.0;
		  gb->dsrc.ofs = (uint32) (gb->dsrc.size * tf);
	       }
	       else
	       {  uint32 ofs = cntcmd.u.reenter.ofs;
	          if (ofs > gb->dsrc.size) ofs = gb->dsrc.size;
	          gb->dsrc.ofs = ofs;
	       }
	       mpx_seek(gb, gb->dsrc.fid, (off_t)gb->dsrc.ofs, SEEK_SET);
	    }	 
	 }
	 switch (gb->dsrc.bstrm) {
	 case BSTRM_ASEQ:
	    init_aseqplay(gb, iosz);
	    bfp = &gb->audb;
	    playfunc = fwd_aseq;
	    mp_claudbf((float *)gb->adec.vv);
	    break;
	 case BSTRM_11172:
	    init_11172play(gb, iosz);
	    bfp = &gb->dmxb;
	    playfunc = fwd_11172;
	    mp_claudbf((float *)gb->adec.vv);
	    gb->vdec.entry = gb->adec.entry = gb->dsrc.ofs; /* Necessary for marking */
	    if (flags & MRE_STRMS)
	    {  ubyte vstrm = (strms >> 8) & 0xff;
	       ubyte astrm = (strms)  & 0xff;
      
	       if ( gb->mpenv.state & MPS_AUDOFF) astrm = STRM_SBCOFF;
	       if (astrm & STRM_IGNOREID)
	          astrm = (gb->mpenv.astrm & STRM_IDBITS) | (astrm & STRM_SBCOFF);
	       if (vstrm & STRM_IGNOREID)
	          vstrm = (gb->mpenv.vstrm & STRM_IDBITS) | (vstrm & STRM_SBCOFF);
	       gb->mpenv.astrm = astrm;
	       gb->mpenv.vstrm = vstrm;
	       set_dmxaction(gb->dmx_actions, astrm, vstrm);
	    }
	    break;
	 case BSTRM_VSEQ:
	    init_vseqplay(gb, iosz);
	    bfp = &gb->vidb;
	    playfunc = fwd_vseq;	    
	 }
	    	 
	 if (gb->dsrc.type & DSRC_CD) dsmsg.m.cmd2.sfp = bfp;
	 else dsmsg.m.cmd1.sfp = bfp;
	 dsmsg.cmd = DSCMD_SYNCREAD;
	 dsmsg.cmdflags = 0;
	 dsmsgsnd(env, gb, &dsmsg);

	 gb->mpenv.state |= MPS_DSYNC;	   
	 (*playfunc)(gb, gb->vdec.lastplaytype);
      }
      break;
   }
   case MCMD_PLAYCTR:


      if ( ! (gb->mpenv.state & MPS_DSRC) ) break;
      if ( ! (busy & (BSY_FWDASEQ | BSY_FWDVSEQ | BSY_FWD11172 | BSY_IDLE))) break;    
      action = cntcmd.u.playctr.action;
      if (gb->dsrc.bstrm == BSTRM_ASEQ) action &= PC_AUDMSK;
      if ( ! action ) break;
      
      switch (action) {
      case PC_PAUSE:
	 if (busy & (BSY_FWDVSEQ | BSY_FWD11172)) return(1);
	 cmd_remove(env, &gb->cmdq); eatenit = 1;
	 return(0);
      case PC_PLAY:
	 if (busy == BSY_FWDASEQ) break;
	 if (busy &  (BSY_FWDVSEQ | BSY_FWD11172)) {
	    cmd_retain(env, &gb->cmdq);  /* leave the current command on the queue */
	    return(0);
	 }
	 cmd_remove(env, &gb->cmdq); eatenit = 1;
	 if (gb->dsrc.bstrm == BSTRM_ASEQ)	     fwd_aseq(gb, 0);
	 else if (gb->dsrc.bstrm == BSTRM_VSEQ)  fwd_vseq(gb, PC_PLAY);
	 else if (gb->dsrc.bstrm == BSTRM_11172) fwd_11172(gb, PC_PLAY);
	 break;
      case PC_FWDSPEED:
	 if (busy & (BSY_FWDVSEQ | BSY_FWD11172)) {
	    cmd_retain(env, &gb->cmdq);	/* leave the current cmd on the queue */
	    return(0);
	 }
	 cmd_remove(env, &gb->cmdq); eatenit = 1;
	 gb->vdec.speed = cntcmd.u.playctr.speed;
	 if (gb->dsrc.bstrm == BSTRM_VSEQ)	     fwd_vseq(gb, PC_FWDSPEED);
	 else if (gb->dsrc.bstrm == BSTRM_11172) fwd_11172(gb, PC_FWDSPEED);
	 break;
      case PC_FWDSTEP:
	 if (busy & (BSY_FWDVSEQ | BSY_FWD11172)) return(1);
	 cmd_remove(env, &gb->cmdq); eatenit = 1;
	 if (busy != BSY_IDLE) return(0);
	 if (gb->dsrc.bstrm == BSTRM_VSEQ)	     fwd_vseq(gb, PC_FWDSTEP);
	 else if (gb->dsrc.bstrm == BSTRM_11172) fwd_11172(gb, PC_FWDSTEP);
	 break; 
      }
      break;
   case MCMD_STREAM:
   {  ubyte vstrm = (cntcmd.u.action >> 8) & 0xff;
      ubyte astrm = (cntcmd.u.action)  & 0xff;
      ubyte change = 0;

      /*if ( gb->mpenv.state & MPS_AUDOFF) astrm |= STRM_SBCOFF; */
      if (astrm & STRM_IGNOREID)
	 astrm = (gb->mpenv.astrm & STRM_IDBITS) | (astrm & STRM_SBCOFF);
      if (vstrm & STRM_IGNOREID)
	 vstrm = (gb->mpenv.vstrm & STRM_IDBITS) | (vstrm & STRM_SBCOFF);
      
      if (gb->dsrc.bstrm != BSTRM_11172)
      {	 gb->mpenv.astrm = astrm;
	 gb->mpenv.vstrm = vstrm;	 
	 break;
      }
      
      /* First gracefully break out of the busy routine */    
      if (busy & BSY_FWD11172)
      {	 /* We are marking the command, so that we will know if
	    originally it was received during busy FWD_11172. 
	 */
	 gb->cmdq.cmds[gb->cmdq.rp].flags |= MCFL_MPXRSV1;
	 cmd_retain(env, &gb->cmdq);	/* leave the command on the queue */ 
	 return(0);
      }
      else if ( gb->cmdq.cmds[gb->cmdq.rp].flags & MCFL_MPXRSV1 )
      {	 /* We need to change the contents of the current command
	    without disposing it.
	 */
	 gb->cmdq.cmds[gb->cmdq.rp].cmd = MCMD_PLAYCTR;
	 gb->cmdq.cmds[gb->cmdq.rp].u.playctr.action = gb->vdec.lastplaytype;
	 gb->cmdq.cmds[gb->cmdq.rp].u.playctr.speed  = gb->vdec.speed;
	 cmd_retain(env, &gb->cmdq); eatenit = 1;
      }   
	        
      if (astrm != gb->mpenv.astrm)
      {  gb->audb.rp = gb->audb.b1;
	 gb->audb.wp = gb->audb.b1;
	 gb->audb.ready = 0;
	 reset_stamps(&gb->adec.stamps);
	 gb->mpenv.astrm = astrm;

	 /* Release the audio device is not needed */
	 if (gb->mpenv.astrm & STRM_SBCOFF)
		close_audio(env, gb);
	 else
		init_audio(env, gb);

	 /* Send back acknowledgement for the closing and opening */
	 /* of the audio device to avoid competition of the audio */
	 /* device with the controlling app. */
      	 send_ack(gb, &cntcmd);
	 change = 1;
      }
      if (vstrm != gb->mpenv.vstrm)
      {  gb->vidb.rp = gb->vidb.b1;
	 gb->vidb.wp = gb->vidb.b1;
	 gb->vidb.ready = 0;
	 gb->vdec.state |= VD_BROKEN;
	 reset_stamps(&gb->vdec.stamps);
	 gb->mpenv.vstrm = vstrm;
	 change = 1;
      }
      if (change)
	 set_dmxaction(gb->dmx_actions, astrm, vstrm);
      break;
   }
   case MCMD_PRESCTR:
      if ( cntcmd.u.presctr.which & PCTR_LSG )
      {  if (cntcmd.u.presctr.which & PCTR_LUM) gb->vdec.adjlum = cntcmd.u.presctr.lum;
	 if (cntcmd.u.presctr.which & PCTR_SAT) gb->vdec.adjsat = cntcmd.u.presctr.sat;
	 if (cntcmd.u.presctr.which & PCTR_GAM) gb->vdec.adjgam =
						    1.0/cntcmd.u.presctr.gam;
#ifndef WIN32
	 mp_initcol24(gb->vdec.adjlum, gb->vdec.adjsat, gb->vdec.adjgam);
#endif
      }
      
      if ( cntcmd.u.presctr.which & PCTR_AVOL )
         set_audvol(gb, cntcmd.u.presctr.avol);
      
      if ( cntcmd.u.presctr.which & PCTR_VMD )
      {  nuint zoom, vmd = cntcmd.u.presctr.vmd;
         zoom = vmd & 0xf;
         if ( zoom > 3 ) zoom = 3;
	 /*         set_video(gb, vmd>>8, zoom);*/
      }

      if ( cntcmd.u.presctr.which & PCTR_AMD )
      {  nuint amd = cntcmd.u.presctr.amd;
         if (busy & (BSY_FWDASEQ | BSY_FWD11172)) return(1);
	 if (amd & 0x4)	/* Quality selection */
	   gb->adec.quality = amd & 0x3;
	 if (amd & 0x20)	/* Channel selection */
	    gb->adec.listen = (amd >> 3) & 0x3;
      }
      break;
   }
   if (! eatenit) cmd_remove(env, &gb->cmdq); 
   return(2);		      /* To be ignored */
}


nint 
verify_scsid( nint scsid)
{  nint scsi_dev   = scsid % 100;
   nint controller = scsid / 100;
   
   if (scsid < 0 ||  controller > 3 || scsi_dev < 1 || scsi_dev > 15) return(0);
   else return(1);
}

#ifdef NOTUSED

static nint
parse_nad(char *nargo, typ_netadr *net)
{
#define	STR_TCP		0
#define	STR_UDP		1
#define	STR_UNIX	2
#define	STR_INET	3
#define	STR_AF		4
#define STR_FP		5
#define	STR_LP		6
#define	STR_FI		7
#define	STR_LI		8
#define	STR_FU		9
#define	STR_LU		10
#define STR_FT		11
#define STR_LT		12
#define	STR_NONE	13

   nint n1,n2;
   struct { char *tnm; nint id; }  tokens[] = {
	"tcp",	STR_TCP,
	"udp",	STR_UDP,
	"unix",	STR_UNIX,
	"inet",	STR_INET,
	"af",	STR_AF,
	"fp",	STR_FP,
	"lp",	STR_LP,
	"fi",	STR_FI,
	"li",	STR_LI,
	"fu",	STR_FU,
	"lu",	STR_LU,
	"ft", 	STR_FT,
	"lt",	STR_LT,
	0,	STR_NONE,
   };
   char narg[1024];
   
   net->flags = 0;
   net->saf = AF_INET;
   net->stype = SOCK_STREAM;
   net->li = INADDR_ANY;
   net->lp = 0;
   net->ttl = 1;
   
   /* Make a local copy, since it will be modified */
   for (n1=0; nargo[n1] && n1<1023; n1++) narg[n1] = nargo[n1];
   narg[n1] = 0;

   n1 = 0;
   while ( narg[n1] )
   {  nint tokp = 0, endadr = 0;
      n2 = n1;
      while ( narg[n2] && narg[n2] != ',' ) n2++;
      if (narg[n2]) narg[n2++] = 0;
      else endadr = 1;
      if (narg[n2] == 0) endadr = 1;
      
      while ( tokens[tokp].tnm )
         if ( strcmp(tokens[tokp].tnm, &narg[n1]) == 0) break;
         else tokp++;
      if ( tokens[tokp].tnm == 0 ) goto LB_BadReturn;
      n1 = n2;
          
      switch ( tokens[tokp].id ) {
      case STR_TCP:
         net->stype = SOCK_STREAM;
         break;
      case STR_UDP:
         net->stype = SOCK_DGRAM;
         break;
      case STR_UNIX:
         net->saf = AF_UNIX;
         break;
      case STR_INET:
	 net->saf = AF_INET;
	 break;
      case STR_AF:
         break;
      case STR_FP:
      case STR_LP:
      {  nint port;
         if (endadr) goto LB_BadReturn;
	 while ( narg[n2] && narg[n2] != ',' ) n2++;
         if (narg[n2]) narg[n2++] = 0;
         if ( !narg[n1] ) goto LB_BadReturn;     
         port = atoi(&narg[n1]);
         if (port < 0 || port > 65535) goto LB_BadReturn;
         if (tokens[tokp].id == STR_FP)
         {  net->flags |= NET_FA | NET_FP;  net->fp = port; }
   	 else
         {  net->flags |= NET_LA | NET_LP;  net->lp = port; }
         n1 = n2;
      }
         break;
      case STR_FU:
      case STR_LU:
      {  char *q;
         if (endadr) goto LB_BadReturn;
	 while ( narg[n2] && narg[n2] != ',' ) n2++;
         if (narg[n2]) narg[n2++] = 0;      
         if ( !narg[n1] ) goto LB_BadReturn;     
         if (tokens[tokp].id == STR_FU)
         {  net->flags |= NET_FA | NET_FU;  q = net->fu; }
   	 else
         {  net->flags |= NET_LA | NET_LU;  q = net->lu; }
   	 strncpy(q, &narg[n1], 108);
         n1 = n2;
      }
         break;
      case STR_FI:
      case STR_LI:
      {  uint32 inad;
	 if (endadr) goto LB_BadReturn;
	 while ( narg[n2] && narg[n2] != ',' ) n2++;
         if (narg[n2]) narg[n2++] = 0;
         if ( !narg[n1] ) goto LB_BadReturn;     
         if ( strcmp("any", &narg[n1]) == 0) inad = INADDR_ANY;
         else if ( strcmp("255.255.255.255", &narg[n1]) == 0) inad = 0xffffffffu;
         else if ( -1 == (inad = inet_addr(&narg[n1]))) goto LB_BadReturn;
         
         if (tokens[tokp].id == STR_FI)
         {  net->flags |= NET_FA | NET_FI;  net->fi = inad; }
   	 else
         {  net->flags |= NET_LA | NET_LI;  net->li = inad; }
         n1 = n2;
      }
	break;
      case STR_LT:
      case STR_FT:
      {  nint ttl;
         if (endadr) goto LB_BadReturn;
	 while ( narg[n2] && narg[n2] != ',' ) n2++;
         if (narg[n2]) narg[n2++] = 0;
         if ( !narg[n1] ) goto LB_BadReturn;     
         ttl = atoi(&narg[n1]);
         if (ttl < 0 || ttl > 256) goto LB_BadReturn;
	 net->ttl = ttl;
         n1 = n2;
      }
	break;
      }
   }
   
   if ( ! (net->flags & (NET_FA | NET_LA))) goto LB_BadReturn;
   if (net->stype == SOCK_DGRAM)
   {  if ( net->saf == AF_INET )
      {  if ( ! (net->flags & NET_LP) ||
	      ( (net->flags & NET_FA) &&
	        (net->flags & (NET_FI | NET_FP)) != (NET_FI | NET_FP)
	      )
	    )
	    goto LB_BadReturn;   
      }
      /* AF_UNIX */
      else if ( ! (net->flags & NET_LU) ||
	          (net->flags & (NET_FA | NET_FU)) == NET_FA
	      )
	      goto LB_BadReturn;
   }
   else		/* SOCK_STREAM */
   {  if ( net->saf == AF_INET )
      {  if ( (net->flags & (NET_FI | NET_FP)) != (NET_FI | NET_FP))
	    goto LB_BadReturn;
         if ( (net->flags & (NET_LA | NET_LP)) == NET_LA ) goto LB_BadReturn;
      }
      else	/* AF_UNIX */
      {  if ( ! (net->flags & NET_FU)) goto LB_BadReturn;
         if ( (net->flags & (NET_LA | NET_LU)) == NET_LA ) goto LB_BadReturn;
      }     
   }  
   
        
   return (1);
   
LB_BadReturn:
   net->flags = NET_BAD;
   return(0);
}

#endif


#ifdef NOTUSED   
static void
read_settings(typ_mpgbl *gb)
{  FILE *fid = NULL;
   nint k, linecnt;
   char *sdir[3];
   char path[512];
   char line[256], arg1[32], arg2[64];
   
   sdir[0] = getenv("MPXPATH");
   sdir[1] = getenv("HOME");
   sdir[2] = ".";
   
   for (k=0;k<3;k++)
      if ( sdir[k] != NULL )
      {	 strncpy(path, sdir[k],512-9);
	 strcat(path, "/.mpxrc");
	 fid = fopen(path, "r");
	 if (fid != NULL) break;
      }
   if (fid == NULL) goto LB_skipmpxrc;
      
   linecnt = 0;
   while ( fgets(line,255,fid) ) 
   {  nint badline = 0;
      
      linecnt++;
      if ( line[0] == '#' ) continue;
      if ( 1 != sscanf(line,"%31s",arg1) ) continue;
      k = 0;
      while ( optnms[k].argnm )
      {	 if ( 0 == strcmp(optnms[k].argnm, arg1) )
	 {  switch ( optnms[k].argid ) {
	    case O_AUDMODE:
	       if ( 2 == sscanf(line,"%s %3s",arg1, arg2) )
	       {  char c = arg2[0];
		  if      (c == 'L') gb->adec.listen = AUD_L;
		  else if (c == 'R') gb->adec.listen = AUD_R;
		  else if (c == 'S') gb->adec.listen = AUD_LR;
		  else { badline = 1; break; }
		  if (arg2[1] != ',') { badline = 1; break; }
		  c = arg2[2];
		  if      (c == '0') gb->adec.quality = 0;
		  else if (c == '1') gb->adec.quality = 1;
		  else if (c == '2') gb->adec.quality = 2;
		  else badline = 1;
	       }
	       else badline = 1;
	       break;
	    case O_VIDMODE:
	       if ( 2 == sscanf(line,"%s %8s",arg1, arg2) )
	       {  nint m, n = 0;
		  arg2[30] = 0;	 /* Just in case!! */
		  while ( arg2[n] && arg2[n] != ',') n++;
		  if ( arg2[n] != ',') { badline = 1; break; }
		  else arg2[n] = 0;
		  n = (nint)arg2[n+1] - (nint)'0';
		  if (n<1 || n>3) n = 2;
		  badline = 1;
		  for (m=0; m<NVDM; m++)
		     if ( 0 == strcmp(arg2, vmodes[m].vmodenm) )
		     {	gb->vdec.vdm = vmodes[m].vmodeval;
			badline = 0;
			break;
		     }
		  if ( ! badline ) gb->vdec.zoom = n;
	       }
	       else badline = 1;
	       break;
	    case O_PASSWD:
	       if ( 2 == sscanf(line,"%s %32s",arg1, arg2) )
		  strcpy(pwdalp, arg2);
	       else badline = 1;
	       break;
	    case O_TIMING:
	    {  float td, tbr;
	       if ( 3 == sscanf(line,"%s %f %f",arg1,&td, &tbr) )
	       {  gb->vdec.td = td;
		  gb->vdec.tbr = tbr;
	       }
	       else badline = 1;
	       break;
	    }
	    case O_GAMMA:
	    {  float tf;
	       if ( 2 == sscanf(line,"%s %f",arg1,&tf) )
	       {  if (tf >= MIN_GAM && tf <= MAX_GAM) gb->vdec.adjgam = 1.0/tf;
		  else badline = 1;
	       }
	       else badline = 1;
	       break;
	    }	    
	    case O_CDSCSI:
	    {  nint scsid;
	       if ( 2 == sscanf(line,"%s %d",arg1,&scsid) )
	       {  if ( verify_scsid(scsid)) gb->mpenv.cdscsi = scsid;
		  else badline = 1;
	       }
	       else badline = 1;
	       break;
	    }
	    case O_NETWORK:
	       if ( 2 == sscanf(line,"%s %500s",arg1, path) )
	       {  if ( ! parse_nad(path, &gb->dsrc.nxt.soc) ) badline = 1; }
	       else badline = 1;
	       break;	    	    
	    }
	    break;
	 }
	 k++;
      }
      if ( optnms[k].argnm == NULL ) badline = 1;

#ifdef STANDALONE
      if (badline)
         mp_fprintf("MpegExpert: bad resource file, line:%3d\n", linecnt);
#endif

   }
   
   fclose(fid);

LB_skipmpxrc:
   fid = NULL;
   if ( pwdalp[0] == 0)
   {  uint32 sys_id;
      char sinf[64];
      
      sysinfo(SI_HW_SERIAL, sinf, 64);
      sys_id = atoi(sinf);

      for (k=0;k<3;k++)
      if ( sdir[k] != NULL )
      {	 strncpy(path, sdir[k],512-9);
	 strcat(path, "/.mpxpwd");
	 fid = fopen(path, "r");
	 if (fid != NULL) break;
      }
      if (fid == NULL) return;
      
      while ( fgets(line,255,fid) ) 
      {	 uint32 mach_id;
	 if ( 2 != sscanf(line,"%x %48s",&mach_id, arg2) ) continue; 	 
	 if ( line[0] == '#') continue;
	 if (mach_id == sys_id) strcpy(pwdalp,arg2);
	 else continue;
	 break;
      }
      fclose(fid);
   }
}
#endif

static void
set_defaults(JNIEnv *env, typ_mpgbl *gb)
{  nint k;

   gb->dsrc.fid = -1;
   gb->dsrc.type = DSRC_NONE;
   gb->dsrc.bstrm = 0;
   gb->dsrc.s.fpath[0] = 0;
   gb->dsrc.nxt.fpath[0] = 0;
   gb->dsrc.size = 1;
   gb->dsrc.jstrm = 0;
   gb->mpenv.start_as_thread = 0;
   gb->mpenv.smph = -1;
   gb->mpenv.msgq = -1;
   gb->mpenv.errors = 0;
   gb->mpenv.options  = 0;
   gb->mpenv.state = 0;
/* If standalone, bring up the GUI by setting uitype to 0 */
#ifdef STANDALONE
   gb->mpenv.uitype = 0;
#else
   gb->mpenv.uitype = 2;
#endif
   gb->mpenv.cdfid = -1;
   gb->mpenv.cntfid = -1;
   gb->mpenv.xsvfid = -1;
   gb->mpenv.dbfid = -1;
   gb->mpenv.bstrm = BSTRM_11172;
   gb->mpenv.cdscsi = 6;
   gb->mpenv.busy = BSY_IDLE;
   gb->mpenv.astrm = STRM_AUTOSBC;
   gb->mpenv.vstrm = STRM_AUTOSBC;

   gb->dinf.ifrms = 0;
   gb->dinf.pfrms = 0;
   gb->dinf.bfrms = 0;
   gb->dinf.rdbytes = 0;
      
   /*   gb->vdec.xdisplay = 0;*/
   /*   gb->vdec.pxwin = 0;*/
   gb->vdec.adjlum = 1.0;
   gb->vdec.adjsat = 1.0;
   gb->vdec.adjgam = 1.0;
   /*   gb->vdec.vidxwin = 0;*/
   /*   gb->vdec.vidxwinbg = 0x00c0c0c0;*/
   /* 4 is extra, as a safety zone */
   gb->vdec.denv.tcfb = (int32 (*)[])calloc((6+4)*64, sizeof(int32));
   gb->vdec.denv.lk_mbbase = gb->lk_mbbase;
   gb->vdec.denv.lkp_levd = gb->lkp_levd;
   gb->vdec.denv.lki_levd = gb->lki_levd;
   gb->vdec.flags = 0;
   gb->vdec.phsz = gb->vdec.bhsz = 352;
   gb->vdec.pvsz = gb->vdec.bvsz = 240;
   gb->vdec.zoom = 1;
   gb->vdec.maxbitppix = 3.0;
   gb->vdec.avrbitppix = 0.75;
   gb->vdec.sqbitppix = 2.5;
   gb->vdec.td =  3.0;
   gb->vdec.maxnrmdel = 3;
   gb->vdec.tbr = 3.0;
   gb->vdec.vdm = VDM_NONE;   
   gb->vdec.vdmflags = 0;
   gb->vdec.x = 0;	/* Default X video window origin */
   gb->vdec.y = 0;	 
   
   gb->adec.smp = (float (*)[36][32])calloc(2*36*32, sizeof(float));
   gb->adec.vv = (float (*)[])calloc(2*52*32, sizeof(float));
   gb->adec.listen = AUD_L;	/* left channel only */
   gb->adec.quality = 1;	/* medium quality audio */

#ifdef JAVA_SOUND
   gb->adec.jdev = NULL;
   gb->adec.joperb = NULL;
   gb->adec.buf = NULL;
#else
   gb->adec.adev = -1;
#endif
 
   {  typ_netadr *n = &(gb->dsrc.nxt.soc);
      n->flags = NET_BAD;
      n->fp = n->lp = 0;
      /*      n->fi = n->li = INADDR_ANY;*/
      for (k=0; k<128; k++) n->fu[k] = n->lu[k] = 0;
   }
   gb->cntch.fpath[0] = 0;
   gb->cntch.fdscp = -1;
   gb->cntch.type   = MSC_NETWORK;
   /*   gb->cntch.soc.li = INADDR_ANY;*/
#ifndef WIN32
   gb->cntch.soc.lp = htons(DPORT);
#endif
   gb->cntch.soc.flags = NET_LA;
   for (k=0; k<32; k++) gb->cntch.fromadr[k] = 0;
   gb->cntch.fromlen = 0;

   gb->prevvdm = gb->prevorgx = -1;
   gb->cdsectmode = 0;
   gb->insignal = 0;
   gb->ds_stopped = 0;
   gb->delay_poll = 0;
   gb->poll_delayed = 0;

   gb->cmdq.incr.key = sema_unique_key++;
   gb->cmdq.decr.key = sema_unique_key++;
   gb->dsmsgq.incr.key = sema_unique_key++;
   gb->dsmsgq.decr.key = sema_unique_key++;
   gb->dssync.incr.key = sema_unique_key++;
   gb->dssync.decr.key = sema_unique_key++;
   gb->dssync.cmdsync.key = sema_unique_key++;
   gb->jmpx = NULL;
   gb->mpx_thread = gb->ds_thread = NULL;
   gb->mpx_env = gb->ds_env = NULL;
   gb->exit = 0;

   /* These synchronization locks are used to provide orderly (exclusive) 
      access to the scsi driver by the DServer and MApp processes. Otherwise 
      simultaneous read attempts with different block lengths (2048,2340) 
      would mess up things
    */
   gb->cdlock_a = 0;
   gb->cdlock_b = 0;
   
   for (k=0; k<64; k++) pwdalp[k] = 0;

   /* Initialize the command queue */
   for (k=0; k < CMD_QSIZE; k++) gb->cmdq.cmds[k].cmd = MCMD_NULL;
   gb->cmdq.rp = gb->cmdq.wp = 0;

if (DEBUG)
   fprintf(stderr, "SEMA_INIT cmdq\n");
   SEMA_INIT(env, &(gb->cmdq.incr), 0, USYNC_THREAD, NULL);
   SEMA_INIT(env, &(gb->cmdq.decr), CMD_QSIZE-1, USYNC_THREAD, NULL);

   /* Initilaize the ds message queue */
   gb->dsmsgq.rp = gb->dsmsgq.wp = 0;
if (DEBUG)
   fprintf(stderr, "SEMA_INIT dsmsgq\n");
   SEMA_INIT(env, &(gb->dsmsgq.incr), 0, USYNC_THREAD, NULL);
   SEMA_INIT(env, &(gb->dsmsgq.decr), DSMSG_QSIZE-1, USYNC_THREAD, NULL);
   SEMA_INIT(env, &gb->ds_deadsync, 0, USYNC_THREAD, NULL);

   gb->dssync.incr.jobj = NULL;
   gb->dssync.decr.jobj = NULL;

   gb->component = NULL;
   gb->window = 0;
   gb->mpxCntl = 0;
   gb->converter = 0;
}


typ_mpgbl *
init_mpx(JNIEnv *env)
{  int k;
   typ_mpgbl *gb = (typ_mpgbl *)calloc(1, sizeof(typ_mpgbl));
   if (gb == NULL) return NULL;

   /* Save the global structure in the globals queue */
   for (k = 0; k < MAX_MPX_INSTANCE; k++)
      if (mpgblq[k] == NULL) break;
   if (k >= MAX_MPX_INSTANCE)
      return NULL;
   mpgblq[k] = gb;

   set_defaults(env, gb);
   /*   read_settings(gb);*/
   return gb;
}

#ifdef NOTUSED
static nint
parse_args(typ_mpgbl *gb, int argc, char *argv[])
{
      register char c;
      register nint n, k = 0, badarg = 0;

      if (argc <= 0) return 0;
      
      argc--;      
      while (argc && !badarg )
      {  argc--;
	 if (argv[++k][0] == '-')
	    switch (argv[k][1])  {
	    case 'a':	/* Audio_mode */       
	       if (argc)
	       {  k++;
		  c = argv[k][0];
		  if      (c == 'L') gb->adec.listen = AUD_L;
		  else if (c == 'R') gb->adec.listen = AUD_R;
		  else if (c == 'S') gb->adec.listen = AUD_LR;
		  else { badarg = 1; break; }
		  if (argv[k][1] != ',') { badarg = 1; break; }
		  c = argv[k][2];
		  if      (c == '0') gb->adec.quality = 0;
		  else if (c == '1') gb->adec.quality = 1;
		  else if (c == '2') gb->adec.quality = 2;
		  else { badarg = 1; break; }
		  argc--;
	       }
	       else badarg = 1;
	       break;
	    case 'v':	/* Video_mode */
	       if (argc)
	       {  register nint m;
	          k++; n = 0;
		  while ( argv[k][n] && argv[k][n] != ',') n++;
		  if ( argv[k][n] != ',') { badarg = 1; break; }
		  else argv[k][n] = 0;
		  n = (nint)argv[k][n+1] - (nint)'0';
		  if (n<1 || n>3) n = 2;
		  badarg = 1;
		  for (m=0; m<NVDM; m++)
		     if ( 0 == strcmp(argv[k], vmodes[m].vmodenm) )
		     {	gb->vdec.vdm = vmodes[m].vmodeval;
			badarg = 0;
			break;
		     }
		  if ( ! badarg ) gb->vdec.zoom = n;
		  argc--;
	       }
	       else badarg = 1;
	       break;
	    case 'c':	/* Upper left origin  */       
	       if (argc)
	       {  k++; n = 0;
		  while ( argv[k][n] && argv[k][n] != ',') n++;
		  if ( argv[k][n] != ',' || n == 0) { badarg = 1; break; }
		  else argv[k][n] = 0;
		  gb->vdec.x = atoi(&argv[k][0]);
		  gb->vdec.y = atoi(&argv[k][n+1]);
		  if (gb->vdec.x < 0) gb->vdec.x = 0;
		  if (gb->vdec.y < 0) gb->vdec.y = 0;
		  argc--;
	       }
	       else badarg = 1;
	       break;
	    case 't':	/* Bitstream_type */
	       if (argc)
	       {  c = argv[++k][0];
		  if      (c == 'm') gb->mpenv.bstrm = BSTRM_11172;
		  else if (c == 'a') gb->mpenv.bstrm = BSTRM_ASEQ;
		  else if (c == 'v') gb->mpenv.bstrm = BSTRM_VSEQ;
		  else badarg = 1;
		  argc--;
	       }
	       else badarg = 1;
	       break;
	    case 'u':	/* User Interface type */
	       if (argc)
	       {  
/* Disable "-u 0" if not compiled as standalone. */
#ifdef STANDALONE
		  gb->mpenv.uitype = atoi(argv[++k]);
	          if (gb->mpenv.uitype != 2 && gb->mpenv.uitype) 
		     gb->mpenv.uitype = 0;
#else
		  k++;
#endif
		  argc--;		  
	       }
	       else badarg = 1;
	       break;
	    case 'o':	/* Hidden options */
	       if (argc)
	       {  gb->mpenv.options = atoi(argv[++k]);
	          if ( ! (gb->mpenv.options & OPT_VALID)) gb->mpenv.options = 0;
		  argc--;		  
	       }
	       else badarg = 1;
	       break;
	    case 'r':
	       if (argc)
	       {  float tf;
		  sscanf(argv[++k],"%f",&tf);
		  if (tf < 0.6) tf = 0.6;
		  if (tf > 4.0) tf = 4.0;
		  gb->vdec.maxnrmdel = tf;
		  argc--;
	       }
	       else badarg = 1;
	       break;
	    case 'd':	/* Default CDROM scsi id */
	       if (argc)
	       {  nint scsid = atoi(argv[++k]);
		  argc--;
		  if ( verify_scsid(scsid)) gb->mpenv.cdscsi = scsid;  
	       }
	       else badarg = 1;
	       break;
	    case 'w':	/* Parent Window id */
	       if (argc)
	       {  gb->vdec.pxwin = strtoul(argv[++k], (char **)NULL, 0);
		  argc--;		  
	       }
	       else badarg = 1;
	       break;
	    case 'b':	/* Background window color BGR */
	       if (argc)
	       {  gb->vdec.vidxwinbg = strtoul(argv[++k], (char **)NULL, 0);
		  argc--;		  
	       }
	       else badarg = 1;
	       break;   
	    case 'f':	/* Data source specification */
	       if (! argc || gb->cmdq.wp != 0) badarg = 1;
	       else
	       {  typ_cntcmd cntcmd;
		  argc--;
	          cntcmd.cmd = MCMD_OPENSRC;
	          if (argv[k][2] == 0)		/* -f */
	          {  k++;
	             if (argv[k][1] == 0 && argv[k][0] == '-')
		     {  cntcmd.u.opensrc.type = MSC_FDSCP;
		        cntcmd.u.opensrc.fdscp = STDIN_FILENO;	     
		     }
		     else
		     {  nint n;
		        cntcmd.u.opensrc.type = MSC_FNAME;
		        cntcmd.u.opensrc.ofs = 0;
		        for (n=0; argv[k][n] && n<1023; n++) 
			   gb->dsrc.nxt.fpath[n] = argv[k][n];
		        gb->dsrc.nxt.fpath[n] = 0;
		     }
		  }
		  else if (argv[k][2] == 'd')	/* -fd */
		  {  k++;
		     cntcmd.u.opensrc.fdscp = atoi(argv[k]);
		     cntcmd.u.opensrc.type = MSC_FDSCP;
		  }
		  else if (argv[k][2] == 'n')	/* -fn */
		  {  if ( ! parse_nad(argv[++k], &gb->dsrc.nxt.soc) ) badarg = 1;
		     cntcmd.u.opensrc.type = MSC_NETWORK;
		  }
		  else { badarg = 1; }
	          if (!badarg && CMD_BUF_AVAIL(gb->mpx_env, gb->cmdq))
		     cmd_add(gb->mpx_env, &gb->cmdq, &cntcmd);
	       }
	       break;
	    case 'x':	/* Control channel definition */
	       if (! argc) badarg = 1;
	       else
	       {  argc--;
	          if (argv[k][2] == 0)	/* -x */
	          {  nint n;
	             k++;
		     for (n=0; argv[k][n] && n<511; n++) 
			gb->cntch.fpath[n] = argv[k][n];
		     gb->cntch.fpath[n] = 0;
		     gb->cntch.type = MSC_FNAME;
		  }
		  else if (argv[k][2] == 'd')
		  {  k++;
		     gb->cntch.fdscp = atoi(argv[k]);
		     gb->cntch.type = MSC_FDSCP;
		  }
		  else if (argv[k][2] == 'n')
		  {  k++;
		     if ( ! parse_nad(argv[k], &gb->cntch.soc) ) badarg = 1;
		     gb->cntch.type = MSC_NETWORK;		     
		     if ( gb->cntch.soc.stype == SOCK_STREAM ||
			  gb->cntch.soc.saf == AF_UNIX || 
			  (gb->cntch.soc.flags & NET_BAD)
			)
		        badarg = 1;
		  }
		  else { badarg = 1; }
	       }
	       break;
	    case 'n':
	       /* Socket Address family and foreign/localaddress specification */
	       if (argc)
	       {  if ( ! parse_nad(argv[++k], &gb->dsrc.nxt.soc) ) badarg = 1;
	          argc--;
	       }
	       else badarg = 1;
	       break;
	    case 'g':
	       if (argc)
	       {  float gamma;
		  sscanf(argv[++k],"%f",&gamma);
		  if (gamma>=MIN_GAM && gamma<=MAX_GAM) gb->vdec.adjgam = 1.0/gamma;
		  else badarg = 1;
		  argc--;
	       }
	       else badarg = 1;
	       break;
	    case 'M':
	       start_as_muted = 1;
	       break;
	    case 'S':
	       /* silent flag */
	       silent = 1;
	       break;
  
	    default:
	       n = 0;
	       while (c = argv[k][++n])	/* Yes, an assignment */
		  if (c == 's')	 /* Open a network connection */
		  {  typ_cntcmd cntcmd;
		     cntcmd.cmd = MCMD_OPENSRC;
		     cntcmd.u.opensrc.type = MSC_NETWORK;
      		     if (CMD_BUF_AVAIL(gb->mpx_env, gb->cmdq))
		        cmd_add(gb->mpx_env, &gb->cmdq, &cntcmd);
		  }
	    }
	 else break;
      }
   
      if ( badarg )	
      {  (void)fprintf(stderr,"\nUsage: %s [-option [optargs]] ...\n", argv[0]);
	 return -1;
      }      

      return 0;
}
#endif

void
mpx_main(typ_mpgbl *gb, int argc, char *argv[])
{
   int error;
   jobject joperb;

   /* P a r s e   the   c o m m a n d   l i n e */
#ifdef NOTUSED
   if (parse_args(gb, argc, argv) < 0)
      app_exit(gb, 2);
#endif

#ifdef TIMEBOMB
   if (time_expired())
      app_exit(gb, 0);
#endif

   if ( gb->cmdq.cmds[0].cmd == MCMD_OPENSRC )
   {  gb->cmdq.cmds[0].u.opensrc.data  = gb->mpenv.bstrm;
      gb->cmdq.cmds[0].u.opensrc.flags = 0;
      gb->cmdq.cmds[0].u.opensrc.strms = (gb->mpenv.vstrm << 8) | gb->mpenv.astrm;
   }

   umask(0);		/* No file creation permission bit will be masked */

   /* A l l o c a t e  o p e r a t i o n a l  b u f f e r s  */   
   /* For Java, we create a java byte array as the input buffer.
      This way, we can call a java method to read directly into the
      array with doing any memcpy. */
   joperb = (*gb->mpx_env)->NewByteArray(gb->mpx_env, DFL_OPERBUFSZ*1024u);
   /* Exit if the allocation of operational buffer failed. */
   if (joperb == 0) return;
   gb->mpenv.joperb = (*gb->mpx_env)->NewGlobalRef(gb->mpx_env, joperb);
   gb->mpenv.operb = (ubyte *)(*gb->mpx_env)->GetByteArrayElements(gb->mpx_env,
				gb->mpenv.joperb, 0);
   (*gb->mpx_env)->DeleteLocalRef(gb->mpx_env, joperb);

   gb->adec.audbuf = (int16 *)valloc(256*1024u);
   gb->mpenv.pictb = (ubyte *)valloc(DFL_NMB * 256 *10);
   gb->vdec.maxpict = DFL_NMB;
   if (gb->mpenv.operb==NULL || gb->mpenv.pictb==NULL || gb->adec.audbuf==NULL)
      ;

   /* M i s c e l l e n e o u s  */
   init_idct();

   /* S t a r t  t h e   D a t a S e r v e r   T h r e a d */
   {
	/* Start the data server java thread.  The run method of
	   the DataThread will kick off the dserver() function */ 
	jobject thread;
	thread = GetObjectField(gb->mpx_env, gb->jmpx, "dataThread",
			"Lcom/sun/media/codec/video/jmpx/DataThread;");
	/* thread might be null under the rare case when the player is closed
	   almost immediately after prefetch has been called. */
	if (thread != 0) {
	   SetLongField(gb->mpx_env, thread, "mpxData", (jlong)gb);
	   CallVoidMethod(gb->mpx_env, thread, "start", "()V");
	   (*gb->mpx_env)->DeleteLocalRef(gb->mpx_env, thread);
	}
   }

   /* C r e a t e   t h e   U s e r   I n t e r f a c e   */
   /*   {  nint retval;*/
   /*      retval = setup_vidxwin(gb, gb->vdec.pxwin);*/
   /*      if ( ! retval) err_fatal(gb, ERR_STARTUI,5);*/
   /*   }*/

   /* V i d e o   I n i t i a l i z a t i o n s   */	
   /*if ( ! init_video_policy(gb)) appterminate(gb->mpx_env, gb, 0);*/
   init_video_policy(gb);
   /*   if ( gb->vdec.vdmflags & VDMF_24 )*/
   /*      mp_initcol24(gb->vdec.adjlum, gb->vdec.adjsat, gb->vdec.adjgam);*/
   /*   else*/
   /*   {  gb->vdec.nm_c8cells = mp_initcol8(gb->vdec.c8_lkup, gb->vdec.adjgam);*/
   /*      fill_cmap(gb, gb->vdec.xcmap);*/
   /*      mp_initcolz8(); */
   /*   }*/
   if (gb->vdec.vdm == VDM_NONE)
   {  gb->vdec.zoom = 1;
      gb->vdec.vdm  = VDM_COL;
      if (gb->vdec.vdmflags & VDMF_24)
      {  if (gb->vdec.vdmflags & VDMF_TCX) gb->vdec.vdm = VDM_COLB;
         if ( ! (gb->vdec.vdmflags & VDMF_GENERIC)) gb->vdec.zoom = 2;
      }
   }

   /* P e r f o r m   A u d i o  I n i t i a l i z a t i o n s  */
/*
   if (!start_as_muted)
      init_audio(gb->mpx_env, gb);
   else
      gb->mpenv.state |= MPS_AUDOFF;
*/

   /* Notify the controlling client that I'm ready to go */
   send_stats(gb, 0);

   /* T h e   M a i n   L o o p  */
   for (;;)
   {  /* Sleep while there is not a command */
      if (cmd_wait(gb->mpx_env, &gb->cmdq)) {
      	 process_command(gb->mpx_env, gb, BSY_IDLE);

	 /* Simply break from the loop will correctly exit the java thread */
	 if (gb->exit)
	    break;
      }
   }
}


void
proc_cmd_broadcast(JNIEnv *env, uint32 *cb, nint size) 
{
	typ_mpgbl *gb;
	int	i;

	for (i = 0; i < MAX_MPX_INSTANCE; i++) {
		if ((gb = mpgblq[i]) != NULL && !gb->exit)
			proc_extcmd(env, gb, cb, size);
	}
}


int 
SEMA_INIT(JNIEnv *env, SEMA *sema, unsigned int count, int type, void *arg) 
{
	jclass cls;
	jobject obj;
	jmethodID mid;

	cls = (*env)->FindClass(env, "com/sun/media/codec/video/jmpx/Semaphore");
	mid = (*env)->GetMethodID(env, cls, "<init>", "(I)V");
	obj = (*env)->NewObject(env, cls, mid, count);
	sema->jobj = (*env)->NewGlobalRef(env, obj);
	(*env)->DeleteLocalRef(env, cls);

if (DEBUG) {
	int c = GetIntField(env, sema->jobj, "count");
	fprintf(stderr, "SEMA_INIT: monitor key = %d; count = %d\n", sema->key, c);
}
	return 0;
}


int 
SEMA_RESET(JNIEnv *env, SEMA *sema, unsigned int count) 
{
	CallVoidMethod(env, sema->jobj, "reset", "(I)V", count);

if (DEBUG) {
	int c = GetIntField(env, sema->jobj, "count");
	fprintf(stderr, "SEMA_RESET: monitor key = %d; count = %d\n", sema->key, c);
}
	return 0;
}


int 
SEMA_DESTROY(JNIEnv *env, SEMA *sema) 
{
	(*env)->DeleteGlobalRef(env, sema->jobj);
	return 0;
}


int 
SEMA_WAIT(JNIEnv *env, SEMA *sema) 
{
	int rtn;

	if (CallBooleanMethod(env, sema->jobj, "decr", "()Z"))
		rtn = 0;
	else
		rtn = -1;

if (DEBUG) {
	int c = GetIntField(env, sema->jobj, "count");
	fprintf(stderr, "SEMA_WAIT: monitor key = %d; count = %d\n", sema->key, c);
}
	return rtn;
}


int 
SEMA_TRYWAIT(JNIEnv *env, SEMA *sema) 
{
	int rtn;

	if (CallBooleanMethod(env, sema->jobj, "tryDecr", "()Z"))
	    rtn = 0;
	else
	    rtn = 16; /* EBUSY; */

if (DEBUG) {
	int c = GetIntField(env, sema->jobj, "count");
	fprintf(stderr, "SEMA_TRYWAIT: monitor key = %d; count = %d\n", sema->key, c);
}
	return rtn;
}


int 
SEMA_POST(JNIEnv *env, SEMA *sema) 
{
	CallBooleanMethod(env, sema->jobj, "incr", "()Z");

if (DEBUG) {
	int c = GetIntField(env, sema->jobj, "count");
	fprintf(stderr, "SEMA_POST: monitor key = %d; count = %d\n", sema->key, c);
}
	return 0;
}


int
SEMA_PEEK(JNIEnv *env, SEMA *sema)
{
	int c = GetIntField(env, sema->jobj, "count");
	return c;
}

