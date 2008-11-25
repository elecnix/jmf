/*
 * @(#)ln_cmd.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <jni-util.h>
#include "mp_mpd.h"
#include "mp_aud.h"
#include "ln_lnd.h"


void
send_ack(typ_mpgbl *gb, typ_cntcmd *cntcmd)
{  nint n;
   uint32 body[32];
   jbyteArray byteArray;

   for (n=4; n<32; n++) body[n] = 0;
   body[0] = 1;
   body[1] = 2;
   body[2] = 3;
   body[3] = 4;
   body[4] = 0xaaaa0001u;
   body[5] = 0xbbbb0000u | (cntcmd->channel & 0xffff);
   body[6] = cntcmd->seq;
   body[7] = 0xcccc0000u | MCFL_ORGMPX;
   body[8] = 0xdddd0001u;
   body[9] = MCMD_ACK;
   
   byteArray = (*gb->mpx_env)->NewByteArray(gb->mpx_env, 128);
   (*gb->mpx_env)->SetByteArrayRegion(gb->mpx_env, byteArray, 0, 128, (jbyte *)body);
   CallVoidMethod(gb->mpx_env, gb->mpx_thread, "replyCommand", "([B)V", byteArray);
   (*gb->mpx_env)->DeleteLocalRef(gb->mpx_env, byteArray);
}

void
send_size(typ_mpgbl *gb)
{  nint n;
   uint32 body[32];
   static seq = 0;
   jbyteArray byteArray;

   for (n=4; n<32; n++) body[n] = 0;
   body[0] = 1;
   body[1] = 2;
   body[2] = 3;
   body[3] = 4;
   body[4] = 0xaaaa0001u;
   body[5] = 0xbbbb0000u | (0 & 0xffff);
   body[6] = seq++;
   body[7] = 0xcccc0000u | MCFL_ORGMPX;
   body[8] = 0xdddd0001u;
   body[9] = MCMD_QSIZE;
   body[10] = gb->vdec.phsz;
   body[11] = gb->vdec.pvsz;
   body[12] = gb->vdec.zoom;
   /*   body[13] = (uint32)gb->vdec.xcmap;*/
   
   byteArray = (*gb->mpx_env)->NewByteArray(gb->mpx_env, 128);
   (*gb->mpx_env)->SetByteArrayRegion(gb->mpx_env, byteArray, 0, 128, (jbyte *)body);
   CallVoidMethod(gb->mpx_env, gb->mpx_thread, "replyCommand", "([B)V", byteArray);
   (*gb->mpx_env)->DeleteLocalRef(gb->mpx_env, byteArray);
}

void
send_stats(typ_mpgbl *gb, float loc)
{  nint n;
   uint32 body[32];
   jbyteArray byteArray;
   static seq = 0;

   for (n=4; n<32; n++) body[n] = 0;
   body[0] = 1;
   body[1] = 2;
   body[2] = 3;
   body[3] = 4;
   body[4] = 0xaaaa0001u;
   body[5] = 0xbbbb0000u | (0 & 0xffff);
   body[6] = seq++;
   body[7] = 0xcccc0000u | MCFL_ORGMPX;
   body[8] = 0xdddd0001u;
   body[9] = MCMD_QSTATS;
   body[10] = gb->dsrc.size;
   if (loc != 0.0) {
      if (loc > 1.0) loc = 1.0;
      if (loc < 0.0) loc = 0.0;
      body[11] = FL_TO_INT(loc);
   } else if (gb->dsrc.size <= 1)	/* unknow file size */
      body[11] = FL_TO_INT(0.0);
   else
      body[11] = FL_TO_INT(gb->dinf.loc);

   body[12] = gb->dinf.hour * 3600 + gb->dinf.min * 60 + gb->dinf.sec;
   body[13] = gb->dinf.ifrms;
   body[14] = gb->dinf.pfrms;
   body[15] = gb->dinf.bfrms;
   body[16] = gb->dinf.rdbytes;
   gb->dinf.ifrms = gb->dinf.pfrms = gb->dinf.bfrms = 0;
   gb->dinf.rdbytes = 0;

   byteArray = (*gb->mpx_env)->NewByteArray(gb->mpx_env, 128);
   (*gb->mpx_env)->SetByteArrayRegion(gb->mpx_env, byteArray, 0, 128, (jbyte *)body);
   CallVoidMethod(gb->mpx_env, gb->mpx_thread, "replyCommand", "([B)V", byteArray);
   (*gb->mpx_env)->DeleteLocalRef(gb->mpx_env, byteArray);
}

void
proc_extcmd(JNIEnv *env, typ_mpgbl *gb, uint32 *cb, nint size) 
{  nint n, badcmd=0;
   ubyte *p = (ubyte *)cb;
   uint32 cmdtype, cmd;
   typ_cntcmd cntcmd;
   static uint32 syncpat[4] = {1, 2, 3, 4};

   for (n=0; n<4; n++)  if ( syncpat[n] != cb[n] ) return;

   if ( cb[4] != 0xaaaa0001 ) return;	/* Version */
   
   if ( (cb[5] >> 16) != 0xbbbb) return;
   else cntcmd.channel = cb[5] & 0xffff;
   
   if ( (cb[7] >> 16) != 0xcccc) return;
   else cntcmd.flags = cb[7] & 0xffff;
   if (cntcmd.flags & MCFL_ORGMPX) {
	return;	/* Avoid Loopback, just in case !! */
   }
   
   if ( (cb[8] >> 16) != 0xdddd) return;
   else cmdtype = cb[8] & 0xffff;
   
   if (cmdtype != 1 && cmdtype != 2) return;
   if ( (cmdtype==1 && size!=128) || (cmdtype==2 && size!=512)) return;
   
   cntcmd.seq = cb[6];
   cmd = cb[9];
   cntcmd.cmd = cmd;
   switch (cmd & 0xff) {
   case MCMD_EXIT:
      /* First release the audio device before sending back acknowledgement. */
      /* For java threads, appterminate is called from the mpxthread so
	 each thread can safely returns */
      break;
   case MCMD_TEST:
   case MCMD_CLOSESRC:
   case MCMD_SENDSTAT:
      break;
   case MCMD_STREAM:
      cntcmd.u.action = cb[10];
      break;
   case MCMD_OPENSRC:
      cntcmd.u.opensrc.flags = cb[14];
      if ( cntcmd.u.opensrc.flags & MRE_FOFS )
         cntcmd.u.opensrc.fofs  = INT_TO_FL(cb[11]);
      else cntcmd.u.opensrc.ofs   = cb[11];
      cntcmd.u.opensrc.data  = cb[12];
      cntcmd.u.opensrc.strms = cb[13];
      cntcmd.u.opensrc.type  = cb[15];
      if ( cb[15] == MSC_FNAME )
      {  nint n;
         for (n=0; ((char *)&cb[16])[n] && n<((size-16*4)-1); n++) 
            gb->dsrc.nxt.fpath[n] = ((char *)&cb[16])[n];
         gb->dsrc.nxt.fpath[n] = 0;
      }
      else if ( cb[15] == MSC_FDSCP )  cntcmd.u.opensrc.fdscp = cb[16];
      else if ( cb[15] == MSC_JAVASTRM ) break;
      else return;
      break;
   case MCMD_REENTER:
     cntcmd.u.opensrc.flags = cb[14];
     if ( cntcmd.u.opensrc.flags & MRE_FOFS )
         cntcmd.u.opensrc.fofs  = INT_TO_FL(cb[11]);
      else cntcmd.u.opensrc.ofs   = cb[11];
      cntcmd.u.opensrc.data  = cb[12];
      cntcmd.u.opensrc.strms = cb[13];
      break;
   case MCMD_PLAYCTR:
   {  float speed = INT_TO_FL(cb[11]);
      cntcmd.u.playctr.action = cb[10];
      if (speed < 0.05) speed = 0.05;
      if (speed > 10.0) speed = 10.0;
      cntcmd.u.playctr.speed  = speed;
      if (gb->ds_thread == 0)
	 break;
      switch (cntcmd.u.playctr.action) {
      case PC_PAUSE:
	 CallVoidMethod(env, gb->ds_thread, "pause", "()V");
	 break;
      case PC_PLAY:
	 CallVoidMethod(env, gb->ds_thread, "restart", "()V");
	 break;
      case PC_FWDSPEED:
	 CallVoidMethod(env, gb->ds_thread, "restart", "()V");
	 break;
      case PC_FWDSTEP:
	 CallVoidMethod(env, gb->ds_thread, "restart", "()V");
	 break; 
      }
      break;
   }
   case MCMD_PRESCTR:
   {  float vol = INT_TO_FL(cb[13]);
      float lum = INT_TO_FL(cb[14]);
      float sat = INT_TO_FL(cb[15]);
      float gam = INT_TO_FL(cb[16]);
      
      if (lum < MIN_LUM) lum = MIN_LUM;
      if (lum > MAX_LUM) lum = MAX_LUM;
      if (sat < MIN_SAT) sat = MIN_SAT;
      if (sat > MAX_SAT) sat = MAX_SAT;
      if (gam < MIN_GAM) gam = MIN_GAM;
      if (gam > MAX_GAM) gam = MAX_GAM;
      if (vol < 0.0 ) vol = 0.0;
      if (vol > 1.0 ) vol = 1.0;
            
      cntcmd.u.presctr.which= cb[10];
      cntcmd.u.presctr.vmd  = cb[11];
      cntcmd.u.presctr.amd  = cb[12];
      cntcmd.u.presctr.avol = vol;
      cntcmd.u.presctr.lum  = lum;
      cntcmd.u.presctr.sat  = sat;
      cntcmd.u.presctr.gam  = gam;
      break;
   }
   case MCMD_ACK:
      break;
   default:
      badcmd = 1;
   }
   
   if (!badcmd && cmd_buf_wait(env, &gb->cmdq))
      cmd_add(env, &gb->cmdq, &cntcmd);
}


/* MT-safe code to manage the command buffer queue. */

/* A non-blocking check to see if commands are available on the queue.
   If true, the incr semaphore counter is decremented */
nint
cmd_avail(JNIEnv *env, typ_cmdq *q)
{
   return (SEMA_TRYWAIT(env, &(q->incr)) == 0);
}


/* A blocking check to see if commands are available on the queue.
   If true, the incr semaphore counter is decremented */
nint
cmd_wait(JNIEnv *env, typ_cmdq *q)
{
   int error;
   while ((error = SEMA_WAIT(env, &(q->incr))) != 0) {
   }
   return error == 0;
}


/* A non-blocking check to see if command buffers are available on the 
   queue for new incoming commands.  If true, the decr semaphore counter 
   is decremented */
nint
cmd_buf_avail(JNIEnv *env, typ_cmdq *q)
{
   return (SEMA_TRYWAIT(env, &(q->decr)) == 0);
}


/* A blocking check to see if command buffers are available on the 
   queue for new incoming commands.  If true, the decr semaphore counter 
   is decremented */
nint
cmd_buf_wait(JNIEnv *env, typ_cmdq *q)
{
   int error;
   while ((error = SEMA_WAIT(env, &(q->decr))) != 0) {
   }
   return error == 0;
}


/* Add a new command to the queue.  The incr semaphore counter is
   incremented so the next cmd_avail will return true. */ 
void
cmd_add(JNIEnv *env, typ_cmdq *q, typ_cntcmd *cmd)
{
   int error;
   q->cmds[q->wp] = *cmd;
   q->wp = CMD_NEXT(q->wp);
   while ((error = SEMA_POST(env, &(q->incr))) != 0) {
   }
}


/* Remove the first command from the queue.  The decr semaphone counter
   is incremented; hence a new command buffer slot is available. */
void
cmd_remove(JNIEnv *env, typ_cmdq *q)
{
   int error;
   q->rp = CMD_NEXT(q->rp);
   while ((error = SEMA_POST(env, &(q->decr))) != 0) {
   }
}


/* Retain the first command in the queue.  The incr semaphore counter
   is incremented to denote that.  Note that the wp pointer is not
   incremented since there is no new command added to the queue. */
void
cmd_retain(JNIEnv *env, typ_cmdq *q)
{
   int error;
   while ((error = SEMA_POST(env, &(q->incr))) != 0) {
   }
}

