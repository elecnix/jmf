/*
 * @(#)ln_aseq.c	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <sys/types.h>

#ifdef WIN32
#include <windows.h>
#endif

#include <errno.h>
#include "mp_mpd.h"
#include "mp_aud.h"
#include "ln_lnd.h"


static long alrates[4] = { 44100, 48000, 32000, 44100 };

nint
test_auddev(typ_mpgbl *gb)
{
#ifdef JAVA_SOUND
   return 1;
#else
   audio_info_t	adevinfo;
   nint res, try;

   /* if ( gb->mpenv.state & MPS_AUDOFF) return 0; */

   AUDIO_INITINFO(&adevinfo);
   for ( try=0; try<4; try++)
   {  adevinfo.play.pause = 0;
      adevinfo.play.error = 0;
      adevinfo.output_muted = 0;
      adevinfo.play.precision = 16;
      adevinfo.play.encoding = AUDIO_ENCODING_LINEAR;   
      adevinfo.play.sample_rate = 44100;
      adevinfo.play.channels =  2;
      res = ioctl(gb->adec.adev, AUDIO_SETINFO, &adevinfo);
      if ( res != -1 ) break;
   }
   if (res == -1) goto LB_BadReturn;
   
   for ( try=0; try<8; try++)
   {  res = ioctl(gb->adec.adev, AUDIO_GETINFO, &adevinfo);
      if (res != -1) break;
   }
   if ( res == -1 ) goto LB_BadReturn;

   if ( adevinfo.play.precision != 16  ||
	adevinfo.play.encoding  != AUDIO_ENCODING_LINEAR ||
	adevinfo.play.sample_rate != 44100 ||
	adevinfo.play.channels !=  2
      )
      goto LB_BadReturn;
   
   return 1;

LB_BadReturn:
   return 0;
#endif /* JAVA_SOUND */
}

void
set_audvol(typ_mpgbl *gb, float fvol)
{
#ifdef JAVA_SOUND
   return;
#else
   audio_info_t	adevinfo;
   
   if ( gb->mpenv.state & MPS_AUDOFF ) return;
   if ( fvol < 0.0  || fvol > 1.0) return;
   AUDIO_INITINFO(&adevinfo);
   adevinfo.output_muted = 0;
   adevinfo.play.pause = 0;
   adevinfo.play.error = 0;
   adevinfo.play.gain = (nuint)(fvol*255);
   ioctl(gb->adec.adev, AUDIO_SETINFO, &adevinfo);
#endif /* JAVA_SOUND */
}


nint
set_audio(typ_mpgbl *gb, nuint smpfreq, nint quality, nint listen)
{
#ifdef JAVA_SOUND
   jobject tmp;
   if (gb->exit) return 0;
   if (gb->adec.jdev != NULL)
      (*gb->mpx_env)->DeleteGlobalRef(gb->mpx_env, gb->adec.jdev);
   if (!CallBooleanMethod(gb->mpx_env, gb->jmpx, "setAudio", "(II)Z",
			smpfreq >> quality, (listen == AUD_LR) ? 2 : 1))
      return -1;
   tmp = (jobject)GetObjectField(gb->mpx_env, gb->jmpx, "audio",
				"Lcom/sun/media/codec/video/jmpx/JmpxAudio;");
   gb->adec.jdev = (*gb->mpx_env)->NewGlobalRef(gb->mpx_env, tmp);
   (*gb->mpx_env)->DeleteLocalRef(gb->mpx_env, tmp);
   return 0;
#else
   audio_info_t	adevinfo;

   if ( gb->mpenv.state & MPS_AUDOFF) return;
   AUDIO_INITINFO(&adevinfo);
   adevinfo.output_muted = 0;
   adevinfo.play.pause = 0;
   adevinfo.play.error = 0;
   adevinfo.play.precision = 16;
   adevinfo.play.encoding = AUDIO_ENCODING_LINEAR;   
   adevinfo.play.sample_rate = smpfreq >>= quality;
   adevinfo.play.channels = (listen == AUD_LR) ? 2 : 1;
   if ( -1 == ioctl(gb->adec.adev, AUDIO_SETINFO, &adevinfo)) 
      return 0;
   else 
      return 1;
#endif /* JAVA_SOUND */
}

#ifndef WIN32
void
apvnap(int32 millisecs)
{  struct timespec naptime;
   naptime.tv_sec = 0;
   naptime.tv_nsec = millisecs * 1000000;
   /* Sleep  for  millisecs  m i l l i s e c o n d s */
   nanosleep(&naptime, NULL);
}
#else
void
apvnap(int32 millisecs)
{
    /* WIN32 way of sleeping for milliseconds */
    Sleep(millisecs); 
}
#endif

void
wraud(typ_mpgbl *gb, int16 *audbuf, int32 nsmp)
{  
   nsmp <<= 1;	/* # 16bit samples to # bytes */
   for (;;)
   { register nint k;  
#ifdef JAVA_SOUND
      if (gb->adec.jdev == 0)
	 k = 0;
      else {
         memcpy(gb->adec.buf, audbuf, nsmp);
	 
	 (*gb->mpx_env)->ReleaseByteArrayElements(gb->mpx_env, gb->adec.joperb,
						  (jbyte *)gb->adec.buf, 0);
         k = CallIntMethod(gb->mpx_env, gb->adec.jdev, "write", "([BII)I",
				gb->adec.joperb, 0, nsmp);
	 gb->adec.buf = (ubyte *)(*gb->mpx_env)->GetByteArrayElements(gb->mpx_env, gb->adec.joperb, 0);
      }
#else
      k = write(gb->adec.adev, audbuf, nsmp);
#endif /* JAVA_SOUND */
      if (k == nsmp) return;
      if (k != -1)  {  nsmp -= k; audbuf += k >> 1; }
      else if ( ! (errno==EINTR || errno==EAGAIN || ! errno)) break;
      apvnap(50);
   }
}

void
drainaud(typ_mpgbl *gb, nint nowait)
{
#ifdef JAVA_SOUND
   if (gb->adec.jdev == NULL)
      return;
#endif /* JAVA_SOUND */

   if ( nowait )
#ifdef JAVA_SOUND
   {
      CallVoidMethod(gb->mpx_env, gb->adec.jdev, "flush", "()V");
      return;
   }
#else
   {  audio_info_t	adevinfo;
      ioctl(gb->adec.adev, I_FLUSH, FLUSHW);
      AUDIO_INITINFO(&adevinfo);
      adevinfo.play.samples = 0;
      adevinfo.play.pause = 0;
      ioctl(gb->adec.adev, AUDIO_SETINFO, &adevinfo);
      return;
   }
#endif /* JAVA_SOUND */
   
#ifdef JAVA_SOUND
   CallVoidMethod(gb->mpx_env, gb->adec.jdev, "drain",  "()V");
#else
   do
   {  ioctl(gb->adec.adev, AUDIO_DRAIN, 0);
      AUDIO_INITINFO(&adevinfo);
      adevinfo.play.samples = 0;
      adevinfo.play.pause = 0;
      if ( -1 == ioctl(gb->adec.adev, AUDIO_SETINFO, &adevinfo)) break;
   } while (adevinfo.play.active);
#endif /* JAVA_SOUND */
}

nint
filledaud(typ_mpgbl *gb, long long acntout)
{  
   long long outsamples;
   register uint32 channels;

#ifdef JAVA_SOUND
   if (gb->adec.jdev == NULL)
      outsamples = 0;
   else {
       uint32 high = (uint32) CallIntMethod(gb->mpx_env, gb->adec.jdev, 
					     "getSampleCountHigh", "()I");
       uint32 low = (uint32) CallIntMethod(gb->mpx_env, gb->adec.jdev, 
					     "getSampleCountLow", "()I");
       outsamples = (long long) low | ((long long) high << 32);
   }
   channels = 1;
#else /* !JAVA_SOUND */
   audio_info_t	adevinfo;
   register nint res;
   
   while (-1 == (res = ioctl(gb->adec.adev, AUDIO_GETINFO, &adevinfo)))
      if (errno != EINTR) break;
   if (res == -1) return 0;
   outsamples = adevinfo.play.samples;
   channels = adevinfo.play.channels;
#endif /* JAVA_SOUND */

   outsamples = outsamples << (channels-1);
   if (acntout >= outsamples) return (int) (acntout - outsamples);
   return 0;
}



void
fwd_aseq(typ_mpgbl *gb, nint playtype /* Dummy for the time being */)
{  register ubyte *q, *lastq;
   register uint16 audattn, prevhdr, chanfreq;
   register int32 need, space, ready, apos, blksz, spent ;
   register uint32 frmcnt, instore, r0, r1;
   long long acntout;
   register int32 aworkmark, asoftwait ;
   nint semid, framesz, error, nowait;
   uint32 acquired, afrmhdr, display ;
   typ_cntcmd cntcmd;
   mp_aprms aprms;
   typ_adec *adec = &(gb->adec);
   typ_circb *audb = &(gb->audb);
   typ_dinf *dinf = &(gb->dinf);
   
   drainaud(gb, 1);   /* Drain by flushing, for fast start after a jump */
   semid = gb->mpenv.smph;
   
   ready = audb->ready;
   acquired = audb->tacq;
   blksz = gb->dsenv.blksz;
   gb->mpenv.busy = BSY_FWDASEQ;
   
   /* Establish a modula "blksz" count of bytes consumed in the current
      block of the i/o buffer
   */
   q = audb->rp;
   while (q < audb->b1) q += blksz;
   spent = (q - audb->b1) % blksz;   
   q = audb->rp;
      
   aprms.ap = adec->audbuf;
   aprms.quality = adec->quality;
   aprms.listen = adec->listen;
   aprms.vv = adec->vv;
   aprms.smp = adec->smp;
   
   afrmhdr = 0;	  /* Actually redundant, just in case measure */
   
   /* # Audio samples which we will try have in the machine's audio hardware
      buffer for the purpose of continous audio output. By default we assume
      a 44100Hz Mono (actual or listening) Audio sequence.
   */ 
   aworkmark = adec->ahead * (44100 >> adec->quality);  
   
   display = 0;
   audattn = AD_INITIALENTRY;
   asoftwait = 0;
   acntout = 0;
   framesz = 0;
   prevhdr = 0x103;  /* an impossible value -9bit ! - */
   chanfreq = 0xf;   /* ccss -> impossible value. ss: '11' reserved sampling freq.
			valid cc is '00' for stereo, '01' for mono aud.seq. */

   for (;;)
   {  
      for (;;)
      {  if ( aworkmark < filledaud(gb, acntout) )  apvnap(100);
	 else break;
      }

      if (gb->exit) goto LB_Return;
           
      if (CMD_AVAIL(gb->mpx_env, gb->cmdq))
      {	 uint32 cmd;
	 nint r;
	 
	 if ( (r = process_command(gb->mpx_env, gb, BSY_FWDASEQ) ) == 0)
	 {  drainaud(gb, 1); goto LB_Return; }
	 if ( r == 1)
	 {  cntcmd = gb->cmdq.cmds[gb->cmdq.rp];
	    cmd_remove(gb->mpx_env, &gb->cmdq);
	    cmd = cntcmd.cmd;
	    
	    if (cmd == MCMD_PRESCTR && (cntcmd.u.presctr.which & PCTR_AMD)) 
	    {  nuint amd = cntcmd.u.presctr.amd;
	       if (amd & 0x4)	 /* Quality selection  */
		  adec->quality = amd & 0x3;
	       if (amd & 0x20)	 /* Channel selection  */
		  adec->listen = (amd >> 3) & 0x3;
	       audattn |= AD_CHGAUDM1;
	    }
	 }
      }
      	      
      apos = audb->b2 - q;      
      space = ready;
      if (apos < space) space = apos;
      lastq = q;

LB_search_nextframe:     
      space -= SAFE_AUDIO;
      /* Search for an audio frame header by looking at the validity
	 of the first 15bits: Sync_word 0xfff followed by MPEG id bit of '1'
	 and 2 layer bits indicating Layer_1 '11' or  Layer_2 '10'
      */
      do
      {  do
	 {  if (space < 0) 
	    {  need = SAFE_AUDIO;
	       goto LB_Reconcile;
	    }
	    r0 = q[0];
	    q++;
	    space--;
	 } while (r0 != 0xff); /* Checking for the first 8bits of a Sync_word */ 
	 r0 = q[0];	 
      } while ( (r0 & 0xfc) != 0xfc );
      
      /* Combine bitrate index "bbbb", sampling rate "ss", 
	 layer_bit_0 "L" and single channel flag "C"
	 into  LCbb.bbss
	 C : 1 for single channel, 0 for stereo, double or joint
	 L : 1 for Layer_1,  0 for Layer_2
	 
	 This one byte info will be used to make a validity check for
	 this frame header. Only certain combinations of bitrate, samp_freq
	 and channel mode is permitted for a Layer_2 frame. Bitrate index '1111'
	 is forbidden and sampling frequency '11' is reserved.
      */
      r1 = (q[1] >> 2) | ((r0 & 0x2) << 6);
      /* Create a single channel flag and shift it to right place */
      r1 |= ((q[2]>>6) == 0x3) << 6;
      
      space += SAFE_AUDIO;    /* Renormalize space count */
          
      /* Here we have the first level of heuristics.
	 We are comparing the current frame's header info with that of the
	 previously validated one. If there is no previous one, e.g. this
	 is our initial entry, then prevhdr would have an impossible (9bit!)
	 value. Otherwise this frame may be the first frame of a new Audio
	 Sequence, or possibly an erroneous one. Either way, if the new
	 frame info does not match the previous one, then we will see if it is
	 valid, and if so we will raise the appropriate attention conditions,
	 so that hardware (re)initializations can later be done. Note that
	 a new frame type will not establish a new Audio Sequence untill
	 after a number of them (8 currently) are received back to back, after
	 which time attention conditions will be lowered, any required hardware
	 settings will be done, and generated audio samples will actually be
	 output...
      */
	 
      if (prevhdr != r1)
      {	 nint r2;
      
	 r2 = lk_framesz[r1];	    /* r1 = LCbb.bbss  */
	 if ( ! r2 ) goto LB_search_nextframe;
	 
	 if (! (r2 & FREE_BRATE) ) framesz = r2;
	 else framesz = 0;
	 
	 /* If more than 8 audio frames with the same bitrate, samp_freq,
	    layer and single/dual channel setting have been processed,
	    dump all the buffered samples, otherwise waste them
	 */
	 if (frmcnt > 8)
	 {  wraud(gb, adec->audbuf, asoftwait); acntout += asoftwait; }
	 
	 asoftwait = 0;
	 aprms.ap = adec->audbuf;
	 frmcnt = 0;
	 instore = 0;
	 
	 /* Check sampling rate change  */
	 if ((r1 & 0x3) != (chanfreq & 0x3)) 
	    audattn |= AD_CHGFREQ;
	 else
	    /* We have to get rid of any previous, now invalid setting */ 
	    audattn &= 0xffff ^ AD_CHGFREQ;
	 
	 /* Check dual/stereo/joint <-> single change */
	 if (((chanfreq & 0xc) << 4) != (r1 & 0x40))  
	    audattn |= AD_CHGCHANNEL;
	 else audattn &= 0xffff ^ AD_CHGCHANNEL;
	 
	 /*mp_printf("has verified a frame: framesz: %d\n", framesz);*/
	 prevhdr = r1;
	 afrmhdr = q[0]<<16 | q[1]<<8 | q[2];
	 display |= DPY_ASEQ;	 
      }
      
      q -= 1;	     /* point back to the frame header */
      space += 1;
          
      if (framesz)
      {	 nint r2;
	 /* W A R N I N G :
	    We are relying on the correct padding bit value. This may cause
	    us to skip a frame, if there is a wrong padding bit value, because
	    we will then be seeking for the next frame header just after the
	    actual frame header. Doing it this way, we are pointing right at the
	    beginning of the next frame rather than pointing little bit behind it.
	 */
	 r2 =  (nint) (q[2] & 0x2) >> 1;   /* put padding bit at bit0 */
	 if (r1 & 0x80) r2 <<= 2;   /* if layer_1 one slot is 4 bytes */
	 r2 += framesz;
	 aprms.dsize = r2;	 
	 if (r2 > space)
	 {  need = r2;
	    goto LB_Reconcile;
	 }
	 if ( ! mp_daframe(q, &aprms)) 
	 {  q += 1;
	    space -= 1;
	    mp_printf("mpx: Bad audio frame! :  code: %d\n",aprms.status);
	    goto LB_search_nextframe;	    
	 }
	 dinf->rdbytes += r2; 
	 q += r2;
	 space -= r2;
      }
      else
      {	 ubyte *qq;
      
	 aprms.dsize = space;	
	 if (!(qq=mp_daframe(q, &aprms))) 
	 {  need = aprms.need;
	    goto LB_Reconcile;
	 }
	 dinf->rdbytes += (qq - q);
	 space -= qq - q;
	 q = qq;	 
      }
      
      asoftwait += aprms.nsmp;
      frmcnt++;
      instore++;
      
      if (audattn)
      {	 if (audattn & AD_CHGCHANNEL)
	 {  if (frmcnt > 8)
	    {  if (r1 & 0x40)	 /* Change to Mono Aud_Seq */
	       {  /* Resetting is required only if we currently listening stereo */
		  if (aprms.listen == AUD_LR)
		     set_audio(gb, alrates[r1 & 0x3],aprms.quality,AUD_L);
		  aprms.listen = AUD_L;
	       } 
	       else		 /* Change to Stereo Aud_Seq */
	       {  /* Resetting is required only if desired listening mode is stereo */
		  if (adec->listen == AUD_LR)
		     set_audio(gb, alrates[r1 & 0x3],aprms.quality,AUD_LR);
		  aprms.listen = adec->listen;
	       }
	       aworkmark = adec->ahead * ((alrates[chanfreq & 0x3]<<1) >> 
					 (aprms.quality + (aprms.listen != AUD_LR)));
	       chanfreq = ((r1 & 0x40)>>4) | (chanfreq & 0x3);
	       audattn &= 0xffff ^ AD_CHGCHANNEL;
	    }
	 }
	 else if (audattn & AD_CHGFREQ)
	 {  if (frmcnt > 8)
	    {  set_audio(gb, alrates[r1 & 0x3],aprms.quality,aprms.listen);
	       chanfreq = (r1 & 0x3) | (chanfreq & 0xc);
	       aworkmark = adec->ahead * ((alrates[chanfreq & 0x3]<<1) >>
					 (aprms.quality + (aprms.listen != AUD_LR)));
	       audattn &= 0xffff ^ AD_CHGFREQ;
	    }
	 }
	 else if (audattn & AD_CHGAUDM1)
	 {  wraud(gb, adec->audbuf, asoftwait);
	    acntout += asoftwait;
	    asoftwait = 0;
	    instore = 0;
	    aprms.ap = adec->audbuf;
	    aprms.quality = adec->quality;
	    if ((chanfreq & 0xc) == 0)
	       aprms.listen = adec->listen;
	    audattn &= 0xffff ^ AD_CHGAUDM1;
	    audattn |= AD_CHGAUDM2;
	    aworkmark = adec->ahead * ((alrates[chanfreq & 0x3]<<1) >>
				      (aprms.quality + (aprms.listen != AUD_LR)));
	    mp_claudbf((float *)aprms.vv);
	 }
	 else if (audattn & AD_CHGAUDM2)
	 {  drainaud(gb, 0);
	    acntout = 0;
	    set_audio(gb, alrates[chanfreq & 0x3],aprms.quality,aprms.listen);
	    audattn &= 0xffff ^ AD_CHGAUDM2;	    
	 }
	 else if (audattn & AD_INITIALENTRY)
	 {  if (frmcnt > 20)
	       audattn &= 0xffff ^ AD_INITIALENTRY;
	 }	 
      }
      else if (instore >= 8)
      {	 nint update = 0;
	 wraud(gb, adec->audbuf,asoftwait);
	 acntout += asoftwait;   	    	    
	 aprms.ap = adec->audbuf;
	 asoftwait = 0;
	 instore = 0;
	 need = SAFE_AUDIO;
	 adec->entry = (gb->dsrc.ofs + acquired - ready + (q - lastq));
	 adec->frmhdr = afrmhdr;
#ifdef DISABLE	 
	 if (acntout > 44100*3600*12)  /* Avoid audio sample count overflow */
	 {  drainaud(gb, 0); acntout = 0; }
#endif
	 if (display & DPY_ASEQ)
	 {  display = 0;
	    update |= UPD_ASTR | UPD_BAR;
	 }
	 /* Do not update the location bar for sequential data sources */
	 if ( ! (gb->dsrc.type & DSRC_FWDONLY))
	 {  dinf->loc = (float)adec->entry / gb->dsrc.size;
	    update  = UPD_BAR;
	 }
	 if (update) send_stats(gb, 0);
	 goto LB_Reconcile;
      }	 
      goto LB_search_nextframe;

LB_Reconcile:
      r0 = q - lastq;
      spent += r0;
      apos -= r0;
      ready -= r0;
      if (need>apos && ready>=apos)
      {	 lastq = audb->b1 - apos;
	 (void) memcpy(lastq, q, apos);
	 q = lastq;
      }

      while (spent >= blksz)
      {  while ((error = SEMA_POST(gb->mpx_env, &gb->dssync.decr)) != 0)
	    if (error == EINTR) continue;
	    else ;
	 spent -= blksz;
      }
                  
      if (need > ready)
      {	 if ( audb->dsrcend ) 
	 {  send_stats(gb, 1);  /* Signal the ctrl client we are done */
	    break;
	 }
	 
	 nowait = FALSE;
	 for (;;)
	 {  if (need <= ready || gb->dsenv.syncrd_state == DSRCEND) 
	       nowait = TRUE;
	    if ((nowait && (error = SEMA_TRYWAIT(gb->mpx_env, &gb->dssync.incr)) != 0) ||
		(!nowait && (error = SEMA_WAIT(gb->mpx_env, &gb->dssync.incr)) != 0))
	    {  if (error == EBUSY || error == EINTR) break;
	       else ;
	    }
	    ready += blksz;
	    acquired += blksz;
	 }
	 if (gb->dsenv.syncrd_state == DSRCEND)
	 {
	    if (SEMA_PEEK(gb->mpx_env, &gb->dssync.incr) == 0)
	    {  int32 junk = blksz - gb->dsenv.lastfill;
	       ready -= junk;
	       acquired -= junk;
	       audb->dsrcend = TRUE;
	    }
	 }
      }
   }
   
LB_Return:
   gb->mpenv.busy = BSY_IDLE;   
   audb->ready = ready;
   audb->rp = q;
   audb->tacq = acquired;
   wraud(gb, adec->audbuf,asoftwait);   	    	    
}
