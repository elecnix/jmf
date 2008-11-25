/*
 * @(#)ln_io.c	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <fcntl.h>
#ifndef WIN32
#include <sys/time.h>
#endif
#include <jni-util.h>
#include "mp_mpd.h"
#include "ln_lnd.h"


#ifdef WIN32
/** Define the file open, read and seek commands **/

static int open(char *filename, int flag)
{
    FILE *fp;
    char *temp;

    /* Strip off preceding slashes */
    while (strlen(filename) > 2 &&
	   filename[0] == '/' &&
	   (filename[1] == '/' || filename[2] == ':'))
	filename++;
    
    temp = filename;
    fp = fopen(filename, "rb");
    if (fp == NULL) {
	printf("Couldn't open file: %s\n", filename);
	return -1;
    } else
	return (int) fp;
}

static int read(int fid, char *buffer, int size)
{
    FILE *fp = (FILE*) fid;
    return fread(buffer, 1, size, fp);
}

static int close(int fid)
{
    FILE *fp = (FILE*) fid;
    fclose(fp);
    return 0;
}

static int lseek(int fid, int location, int whence)
{
    FILE *fp = (FILE*) fid;
    return fseek(fp, location, whence);
}
#endif /* WIN32 */

extern int DEBUG;

extern long Jmpx_getContentLength(JNIEnv *env, jobject jmpx);

void
check_debug(void)
{ }


static void 
hnd_alarm(int sig)
{ }
   

int
mpx_seek(typ_mpgbl *gb, int fid, int offset, int whence)
{
#ifdef _EXPERIMENT_
    if (gb->dsrc.jstrm != 0) {
	if (whence != SEEK_SET)
	    return -1;
	/* Check to see if the jstrm is seekable */
	if (!IsInstanceOf(gb->ds_env, gb->dsrc.jstrm, 
			  "javax/media/protocol/Seekable")) 
	    return -1;
	return (int)CallLongMethod(gb->ds_env, gb->dsrc.jstrm, "seek", 
				   "(J)J", (jlong)offset);
    }
#endif
    if (fid == 0 || fid == -1)
	return 0;
    return lseek(fid, offset, whence);
}


static nint 
ds_syncrd_rgf(typ_mpgbl *gb, int fid, typ_circb *iob)
{
    nint p, rval, maxblk, error;
    uint32 iosize;
    int32 nbytes;
    jbyteArray jtempArray;
    int pinnedarrays = 0;
    
    rval = 1;
    iosize = gb->dsenv.blksz;
    maxblk = gb->dsenv.nblk - 1;
    gb->dsenv.lastfill = iosize;
    p = (iob->wp - iob->b1) / iosize;

    jtempArray = (jbyteArray) GetObjectField(gb->ds_env, gb->jmpx, "tempArray", "[B");
    
    for (;;) {
	if (gb->ds_stopped)  {  nbytes = 0; break; }
	
	/* #bytes read in "read" sytem call maybe less than what was asked, for 
	   a socket or pipe. If a signal was caught during the read system call
	   and if the kernel was configured to support short reads, read may return with
	   lesser #bytes in that case too. Read would return with -1 and errno
	   set to EINTR if the signal was caught before any bytes were read.
	   If short reads are disabled, any partial data read during an 
	   interrupted read call is lost (in which case read returns with -1)
	   */
	
	nbytes = iosize;
	while ( nbytes > 0  ) {
	    int32 n;
	    int32 offset;
	    
	    /* Check to see if the thread had been paused */
	    CallVoidMethod(gb->ds_env, gb->ds_thread, "checkPause", "()V");
	    
	    if (gb->ds_stopped)  {  nbytes = 0; break; }
	    
	    if (gb->dsrc.jstrm != 0) {
		/* Read from the an InputStream to the operation buffer. */
		offset = iob->wp - gb->mpenv.operb;
		/* This code only works for VMs with pinned arrays */
		if (pinnedarrays) {
		    (*gb->ds_env)->ReleaseByteArrayElements(gb->ds_env,
							     gb->mpenv.joperb, 
							     (jbyte *)gb->mpenv.operb, 0);
		    n = CallIntMethod(gb->ds_env, gb->jmpx, "readFromStream", "([BII)I",
				      gb->mpenv.joperb, offset, nbytes);
		    gb->mpenv.operb = (ubyte *)(*gb->ds_env)->GetByteArrayElements(gb->ds_env,
									     gb->mpenv.joperb, 0);
		} else {
		    /* ay - Added this code to work for HotSpot/ExactVM. They don't
		       pin arrays, so we need to make a copy.
		       Will reduce performance a bit when running in older VMs */
		    n = CallIntMethod(gb->ds_env, gb->jmpx, "readFromStream", "(II)I",
				      0, nbytes);
		    if (n > 0)
			(*gb->ds_env)->GetByteArrayRegion(gb->ds_env, jtempArray,
							  0, n, (jbyte*) iob->wp);
		}
	    } else { /* Read from a file descriptor. */
		n = read(fid,iob->wp,nbytes);
		if (n == 0) n = -1;
	    }
		
	    /* 
	      fprintf(stderr, "length read = %d/%d\n", n, nbytes);
	     */
	    
	    /* Check to see if the thread had been paused during the last read */
	    CallVoidMethod(gb->ds_env, gb->ds_thread, "checkRead", "()V");
	    
	    if ( n < 0) {
		rval = -1;
		break;
	    }

	    if (n == 0) {
		/* Can't read any data.  wait a short while before 
		   trying again */
		jclass cls = (*gb->ds_env)->FindClass(gb->ds_env, 
						"java/lang/Thread");
		jlong t = (jlong)500;
		CallStaticVoidMethod(gb->ds_env, cls, "sleep", "(J)V", t);
		(*gb->ds_env)->DeleteLocalRef(gb->ds_env, cls);
	    }

	    iob->wp += n;
	    nbytes -= n;
	}
	
	nbytes = iosize - nbytes;
	if (nbytes != iosize) break;            
	
	if (p == maxblk)  {  p = 0;  iob->wp = iob->b1; }
	else p++;
	
	while ((error = SEMA_POST(gb->ds_env, &gb->dssync.incr)) != 0) {
	    /*	  if (error != EINTR) {  rval = -2;  goto LB_Return; }*/
	    if (gb->ds_stopped) goto LB_Return;
	}
	
	while ((error = SEMA_WAIT(gb->ds_env, &gb->dssync.decr)) != 0) {
	    /* Only expected sources of interrupt is the signal sent by the main
	       application to take the DServer out of this loop and the alarmclock.
	       */
	    if (error != EINTR)  {  rval = -2;  goto LB_Return; }
	    if (gb->ds_stopped) goto LB_Return;
	}
    }
LB_Return:
    (*gb->ds_env)->DeleteLocalRef(gb->ds_env, jtempArray);
    
    /* Getting rid of the thr_suspend() and thr_continue() might cause
       some problem with the data integrity of the dsenv structure.
       To fix this completely, we'll need to mutex guard each piece of
       code that reference dsenv and the io buf.  For now, it's taken
       out because it causes a race condition with the controlling thread. */
    /* thr_suspend(gb->mpenv.mpthr); */
    gb->dsenv.syncrd_state = DSRCEND;
    gb->dsenv.lastfill = nbytes;
    memset(iob->wp, 0, iosize - nbytes);
    while ((error = SEMA_POST(gb->ds_env, &gb->dssync.incr)) != 0) ;
    if (gb->ds_stopped) rval = 2;
    return(rval);
}


void *
dserver(void *mpgbl)
{
    register int32 k;
    typ_dsmsg msg;
    nint error;
    /*   sigset_t set;*/
    typ_mpgbl *gb = (typ_mpgbl *)mpgbl;
    
    gb->dsenv.state = DSS_IDLE;
    gb->dsenv.cmd  = 0;
    gb->dsenv.rval = 1;
    
    for (;;) {
	/* first  0 arg: Get the first message on the queue,
	   second 0 arg: No flags are set, will wait if there are no 
	   messages in the queue.
	   */
	if (dsmsgrcv(gb->ds_env, gb, &msg) != 0)
	    ;
	else {	 
	    switch (msg.cmd) {
	    case DSCMD_SENSE:
		gb->ds_stopped = 0;
		gb->dsenv.rval = 1;
		gb->dsenv.state = DSS_IDLE;
		gb->dsenv.cmd = DSCMD_SENSE; 
		if (msg.cmdflags & DSCMDFLAG_SETSEM)
		    while ((error = SEMA_POST(gb->ds_env, &gb->dssync.cmdsync)) != 0)
			/*		  if (error != EINTR) break;*/ ;
		break;
	    case DSCMD_DIE:
		/* Acknowledge to the ones waiting that the data thead is dead */
		SEMA_POST(gb->ds_env, &gb->ds_deadsync);
		goto WAY_OUT;
		
	    case DSCMD_SYNCREAD:
		gb->ds_stopped = 0;
		gb->dsenv.cmd = DSCMD_SYNCREAD;
		gb->dsenv.state = DSS_EXECCMD;
		gb->dsenv.rval = ds_syncrd_rgf(gb, msg.m.cmd1.fid, 
					       msg.m.cmd1.sfp);
		break;
	    }
	}
	gb->dsenv.state = DSS_IDLE;
    }
    
WAY_OUT:
    return NULL;
}


void
idle_dserver(JNIEnv *env, typ_mpgbl *gb)
{
    typ_dsmsg dsmsg;
    nint error;
    
    /* There could some redundant commands left behind from the past.
       Let's purge this command queue first. */
    (void)dsmsgpurge(env, gb);
    
    dsmsg.cmd = DSCMD_SENSE;
    dsmsg.cmdflags = DSCMDFLAG_SETSEM;
    
    /*
      fprintf(stderr, "SEMA_INIT cmdsync\n");
      */
    SEMA_INIT(env, &gb->dssync.cmdsync, 0, USYNC_THREAD, NULL);
    
    dsmsgsnd(env, gb, &dsmsg);
    
    gb->ds_stopped = 1;
    
    /* Release the block on the DS thread. Otherwise, it won't be able to exit */
    CallVoidMethod(env, gb->ds_thread, "restart", "()V");
    
    while ((error = SEMA_POST(env, &gb->dssync.decr)) != 0) ;    
    while ((error = SEMA_WAIT(env, &gb->dssync.cmdsync)) != 0)
	if (error != EINTR) break;
}


/* 
   This is to accomdate the non-preemptive style of green threads.  i.e.,
   can't use signal
*/
void
kill_dserver(JNIEnv *env, typ_mpgbl *gb)
{
    typ_dsmsg dsmsg;
    nint error;
    
    /* There could some redundant commands left behind from the past.
       Let's purge this command queue first. */
    (void)dsmsgpurge(env, gb);
    
    dsmsg.cmd = DSCMD_DIE;
    dsmsg.cmdflags = DSCMDFLAG_SETSEM;
    
    /*
      fprintf(stderr, "SEMA_INIT cmdsync\n");
      */
    SEMA_INIT(env, &gb->dssync.cmdsync, 0, USYNC_THREAD, NULL);
    dsmsgsnd(env, gb, &dsmsg);
    
    gb->ds_stopped = 1;
    
    /* Release the block on the DS thread. Otherwise, it won't be able to exit */
    CallVoidMethod(env, gb->ds_thread, "restart", "()V");
    
    if (gb->dssync.decr.jobj == NULL) return;
    
    while ((error = SEMA_POST(env, &gb->dssync.decr)) != 0) ;
}


nint
dsmsgsnd(JNIEnv *env, typ_mpgbl *gb, typ_dsmsg *dsmsg)
{
    nint error;
    while ((error = SEMA_WAIT(env, &gb->dsmsgq.decr)) != 0)
	if (error != EINTR) return -1;
    gb->dsmsgq.msgq[gb->dsmsgq.wp] = *dsmsg;
    gb->dsmsgq.wp = DSMSG_NEXT(gb->dsmsgq.wp);
    while ((error = SEMA_POST(env, &gb->dsmsgq.incr)) != 0) ;
    return 0;
}


nint
dsmsgrcv(JNIEnv *env, typ_mpgbl *gb, typ_dsmsg *dsmsg)
{
   nint error;
   while ((error = SEMA_WAIT(env, &gb->dsmsgq.incr)) != 0)
       if (error != EINTR) return -1;
   *dsmsg = gb->dsmsgq.msgq[gb->dsmsgq.rp];
   gb->dsmsgq.rp = DSMSG_NEXT(gb->dsmsgq.rp);
   while ((error = SEMA_POST(env, &gb->dsmsgq.decr)) != 0) ;
   return 0;
}


void
dsmsgpurge(JNIEnv *env, typ_mpgbl *gb)
{
   /* Initilaize the ds message queue */
   gb->dsmsgq.rp = gb->dsmsgq.wp = 0;
/*
   SEMA_RESET(env, &(gb->dsmsgq.incr), 0);
   SEMA_RESET(env, &(gb->dsmsgq.decr), DSMSG_QSIZE-1);
*/
}

nint
opensrc(JNIEnv *env, typ_mpgbl *gb, nint srctype, void *src)
{
   nint fid = -1, errloc;
   char *fname, *ftype;
   int32 recvbuf_size;
   typ_datasrc *dsrc = &(gb->dsrc);
   
   if ( srctype == MSC_JAVASTRM) {
      long size = Jmpx_getContentLength(env, gb->jmpx);
      if (size > 0)
	  dsrc->size = size;
      return 1;
   }
   if ( srctype == MSC_FNAME)
   {  nint oflag, n;
      oflag = O_RDONLY;
      for (n=0; dsrc->nxt.fpath[n] && n<1023; n++)
         dsrc->s.fpath[n] = dsrc->nxt.fpath[n];
      dsrc->s.fpath[n] = 0;

#ifdef WIN32
      if ( (fid = open( dsrc->s.fpath, oflag)) == -1)
#else
      if ( (fid = open( dsrc->s.fpath, oflag)) == -1)
#endif
      {  errloc = 8; goto LB_BadReturn; }
      fname = dsrc->s.fpath;
   }

#ifdef WIN32
   if (fid != 0 && fid != -1) {
       mpx_seek(gb, fid, 0, SEEK_END);
       dsrc->size = ftell((FILE*)fid);
       mpx_seek(gb, fid, 0, SEEK_SET);
   }
   if (dsrc->size == -1L) {
       dsrc->type |= DSRC_FWDONLY;
       dsrc->size = 0;
   } else {
       mpx_seek(gb, fid, 0, SEEK_SET);
   }
#else
   if ( (dsrc->size = mpx_seek(gb, fid, 0, SEEK_END)) == -1) {
       dsrc->type |= DSRC_FWDONLY;
       dsrc->size = 0;
   } else {
       mpx_seek(gb, fid, 0, SEEK_SET);
   }
#endif
   
   if (dsrc->size != -1)
       dsrc->type |= DSRC_REGULAR;

   dsrc->fid = fid;
   dsrc->ofs = 0.0;
   
   /*   scrprint("\nOpened: %s : %s file", fname, ftype);*/
   return(1);
   
LB_BadReturn:
   if (fid != -1)
       close(fid);
   return(0);
}
