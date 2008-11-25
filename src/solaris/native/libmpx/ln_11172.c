/*
 * @(#)ln_11172.c	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#ifndef WIN32
#include <sys/time.h>
#endif

#ifdef WIN32
#include <windows.h>
#endif

#include "mp_mpd.h"
#include "mp_aud.h"
#include "ln_lnd.h"

static long alrates[4] = { 44100, 48000, 32000, 44100 };

/* This routine makes the association between the stream data and the packets
   it was delivered in. Demultiplexer puts info on packets belonging to a 
   subscribed stream into the associated fifo. Currently, entries are made
   only for those packets carrying PTS.
*/
static float
getpts(typ_ptslog *stamps, typ_circb *strmb, uint32 pos, uint32 *fpos)
{  register uint32 r0, filled;
   register nint rp, seq, curseq;
      
   /* Note that all the position values in this routine are the distances
      from the end of the stream buffer. Last byte of the stream buffer
      has the position value of 1.
   */
   if ((filled = stamps->filled) == 0) return(-1.0);   
   seq = curseq = stamps->seq;
   rp = stamps->rp;
   r0 =  strmb->b2 - strmb->wp;
   
   /* The comparison below is completely reliable. We are comparing the 
      given position (e.g. first byte of an access unit) with that of the
      position of the write pointer to this stream buffer. Since any input
      position is supposed to be in the not yet overwritten area of the 
      stream buffer, if the following is an equality, then this means that 
      the buffer is 100% full, and we are looking for a packet info belonging
      to the previous sequence. If it is otherwise true, then the input
      position is ahead of the write pointer in the stream buffer and it
      belongs to the previous sequence.
   */
   if (pos <= r0)
      seq = ((seq + 256) - 1) & 0xff;
      	    
   /* If the fifo has overflowed, set the read pointer to the earliest entry
      in the fifo, where the write pointer is currently pointing.
   */
   if (filled >= 128) 
   {  filled = 128;
      rp = stamps->wp;
   }
   
   do
   {  if (  stamps->a[rp].seq == seq)
      {	 static double scfact = 1.0/90000;
	 r0 = stamps->a[rp].pos;
	 /* Is this packet ahead of the given position ? */
	 if ( r0 < pos) break;
	 /* Is the given position inside this packet ? */
	 if ( (r0 - stamps->a[rp].size)  < pos )
	 {  ubyte *p;
	    float pts;
	    
	    p = stamps->a[rp].pts;
	    *fpos = stamps->a[rp].fpos;
	    /* Note that we are recording a modula 32 time stamp, instead of
	       the actual modula 33 clock. Furthermore we are returning the
	       time stamp in floating point, which can carry a resolution
	       of at least 1/350 second (worst case) in its 24bit(23+1) mantissa
	    */
	    r0 = (p[0]&0x6)<<29 | p[1]<<22 | (p[2]&0xfe)<<14 | p[3]<<7 | p[4]>>1;
	    pts = r0 * scfact;
	    stamps->rp = (rp + 1) & 0x7f;
	    stamps->filled = filled - 1;
	    /* Remember first 5bits of the 5byte PTS is cleared as an indication
	       of a prior SYSEND startcode
	    */
	    if ( !(p[0] & 0xf0) )
	       stamps->flags |= DMXF_NEWSTRM;
	    return(pts);
	 }
	 rp = (rp + 1) & 0x7f;
	 filled--;
      }
      else
      {	 /* Since we are here, if we have encountered a packet of the current
	    sequence, it can only be the earliest packet of the current sequence
	    in the fifo. We should stop the search here and save the state.
	 */
	 if ( stamps->a[rp].seq == curseq ) break;
	 /* Otherwise we are skipping the now outdated entries */
	 rp = (rp + 1) & 0x7f;
	 filled--;
      }     
   } while (filled > 0);   
   
   stamps->rp = rp;
   stamps->filled = filled;
   return(-2.0);
}


/* Same as getpts(), but just for the purpose of getting position info */
static void
getptsmrk(typ_ptslog *stamps, typ_circb *strmb, uint32 pos, uint32 *fpos)
{  register uint32 r0, filled;
   register nint rp, seq, curseq;
      
   if ((filled = stamps->filled) == 0) return;   
   seq = curseq = stamps->seq;
   rp = stamps->rp;
   r0 =  strmb->b2 - strmb->wp;   
   if (pos <= r0)
      seq = ((seq + 256) - 1) & 0xff;      	    
   if (filled >= 128) 
   {  filled = 128;
      rp = stamps->wp;
   }   
   do
   {  if (  stamps->a[rp].seq == seq)
      {	 r0 = stamps->a[rp].pos;
	 if ( r0 < pos) break;
	 if ( (r0 - stamps->a[rp].size)  < pos )
	 {  *fpos = stamps->a[rp].fpos;
	    return;
	 }
	 rp = (rp + 1) & 0x7f;
	 filled--;
      }
      else
      {	 if ( stamps->a[rp].seq == curseq ) break;
	 rp = (rp + 1) & 0x7f;
	 filled--;
      }     
   } while (filled > 0);
}

	 	 
/* Put the PTS fifo read pointer and the "filled" value into a correct
   state, based on the current value of the write pointer and the contents
   of the fifo. Upon return, the read pointer will be left pointing to the
   earliest packet of the desired sequence if one is found, otherwise
   to the earliest packet of the current sequence if one is found, or
   otherwise it will be left equal to the write pointer, indicating an
   empty fifo sate. "filled" value will reflect the relative positions
   of the read and write pointers to the fifo.
*/
static void
correct_ptsfifo(typ_ptslog *stamps, typ_circb *strmb, uint32 pos)
{  register uint32 r0, filled;
   register nint rp, seq, curseq;
   
   r0 =  strmb->b2 - strmb->wp;
   seq = curseq = stamps->seq;
   if (pos <= r0)
      seq = ((seq + 256) - 1) & 0xff;
   
   filled = 128;	/* Assume all the entries are valid */
   rp = stamps->wp;	/* Remember, first write, them wp++ */
   r0 = 555;		/* A value bigger than max stamps array index */
   while (stamps->a[rp].seq != seq)
   {  /* Record the index to the earliest packet from the current sequence */
      if (curseq == stamps->a[rp].seq) 
	 if (r0 == 555) r0 = rp;
      rp = (rp + 1) & 0x7f;
      filled--;
      if (filled == 0)
      {  /* There is no packet of the desired sequence in the fifo !
	    If at this point r0 != 555, this means that while we couldn't find
	    a packet of the sequence we want, we did encounter the earliest 
	    packet of the current sequence in the fifo.
	 */
	 if (r0 != 555)
	 {  filled = (stamps->wp + 128 - r0) & 0x7f;
	    rp = r0;
	 }
	 break;
      }
   }         	 
   stamps->rp = rp;
   stamps->filled = filled;
}


void
reset_stamps(typ_ptslog *stamps)
{  register nint k;

   stamps->rp = 0;
   stamps->wp = 0;
   stamps->filled = 0;
   stamps->seq = 2;
   stamps->flags = 0;
   for (k=0;k<128;k++) stamps->a[k].seq = 0;
}

/**
 * Get the high resolution time (in nanosecs), convert to micros and then to secs.
 * On Windows, GetTickCount() is in millisecs.
 */
#ifndef WIN32
#define getcountdown(psec) *(psec) = (-(gethrtime() / 1000) * 0.000001)
#else
#define getcountdown(psec) *(psec) = ((-(double)GetTickCount() * 0.001))
#endif

void
fwd_11172(typ_mpgbl *gb, nint playtype)
{  register uint16 vdecflags, isoflags;
   register int32 vneed, aneed;
   register int32 aworkmark;
   int32 phigh, ptref, pdisp, pthis, pfut, vlastptstref;
   nint  toggle, refstat;
   uint16 audattn, prevhdr, chanfreq;
   uint32 frmcnt, instore;
   long long acntout;
   int32 asoftwait, aswaitpts;
   nint framesz,fbs;   
   uint32 gopval, gopmrk, afrmhdr, display, amrk, vmrk;
   register float ptime, amfreq ;
   register double tf0, tf1, vsystmbase, vnomtmbase, vlastpts, anomtm, alastpts;
   typ_vdec *vdec = &(gb->vdec);
   typ_adec *adec = &(gb->adec);
   typ_circb *vidb = &(gb->vidb);
   typ_circb *audb = &(gb->audb);
   typ_dinf *dinf = &(gb->dinf);
   static nint itimer_set = 0;
   double sec;

   void (*dmxmpeg)(typ_mpgbl *, int32 aneed, int32 vneed, ubyte action[]);
   mp_aprms aprms;
   mp_pict tpict;
   mp_vseq tvseq;
   typ_cntcmd cntcmd;
   float tolerance[4];

   drainaud(gb, 1);   /* Drain by flushing, for fast start after a jump */
   gb->mpenv.busy = BSY_FWD11172;   
   if ( (gb->dsrc.type & DSRC_CD) && (gb->dsrc.cdsect == SECTXA) )
      dmxmpeg = dmx_mpegcd;
   else dmxmpeg = dmx_11172;

   getcountdown(&sec);
   vsystmbase = sec;
   
   if (playtype != PC_FWDSTEP)
   {  uint32 vfull, afull;
      register float tf0,tf1;

      vfull = vidb->full - (vidb->full>>3);
      afull = audb->full - (audb->full>>3);
      getcountdown(&sec);
      tf0 = sec;
      tf1 = 0;   
      /* Spend at most about 4 seconds with nonblocking DMX calls */
      while (vidb->ready < vfull  &&  audb->ready < afull  &&  tf1 < vdec->maxnrmdel )
      {  apvnap(50);
	 dmxmpeg(gb, 0,0,gb->dmx_actions);
	 getcountdown(&sec);
	 tf1 = tf0 - sec;
	 if (gb->dsenv.syncrd_state == DSRCEND) break;
      }
   }

   /* Reset all local audio state regardless */
   aprms.ap = adec->audbuf;
   aprms.quality = adec->quality;
   aprms.listen = adec->listen;
   aprms.vv = adec->vv;
   aprms.smp = adec->smp;   
   aworkmark = 44100;	   /* Almost anything would go here... */     
   asoftwait = 0;
   acntout = 0;
   aswaitpts = -256000;   
   audattn = AD_INITIALENTRY;
   frmcnt = 0;
   framesz = 0;
   prevhdr = 0x103;
   afrmhdr = 0;
   chanfreq = 0xf;
   instore = 0;		   /* Actually a redundant initialization... */
   
   vdecflags = vdec->state;
   gopval = vdec->gopval;
   gopmrk = vdec->gopmrk;
   vmrk = vdec->entry;
   amrk = adec->entry;
   if (vdecflags & VD_BROKEN)
   {  pdisp  = pfut = ptref = 0;
      toggle = 1;
      refstat= 2;
      phigh  = -1;
      gopval = 0;
      amrk = vmrk = 0;
      vdecflags &= 0xffff ^ VD_SAFEMARKED;
   }
   else
   {  toggle = vdec->toggle;
      phigh  = vdec->phigh;
      ptref  = vdec->ptref;
      pfut   = vdec->pfut;
      pdisp  = vdec->pdisp;
      refstat= vdec->refstat;
   }
   	
   /* Avoid inheriting outdated flags from previous session */
   vdecflags &= VD_SAFEMARKED | VD_USEVBV | VD_VSEQHDR | VD_LOOKVSEQ;
   isoflags = AVD_VIDON;
   display = (40<<8) | (51<<16);  /* Just to force update of location bar at once */
   
   if (playtype == PC_PLAY)
   {  isoflags |= AVD_AUDON;
      vdec->speed = 1.0;      
   }   
   else if (playtype == PC_FWDSTEP)
      vdecflags |= VD_STEP;
   else if (vdec->speed < 1.0) 
      vdecflags |= VD_SLOW;   

   /* We definitely expect to have subscription(s) to either or both of
      audio/video streams. Furthermore DMX actions should have been set
      to reflect the state of mpenv.astrm and mpenv.vstrm which at any
      time carry the subscription state & wishes...
   */
   if (gb->mpenv.astrm & STRM_SBCOFF)
   {  isoflags |= AVD_VIDON;
      isoflags &= 0xffff ^ AVD_AUDON;
   }
   
   if (gb->mpenv.vstrm & STRM_SBCOFF)
   {  isoflags |= AVD_AUDON;
      isoflags &= 0xffff ^ AVD_VIDON;      
   }   
   /* If at this point we still have an autosubscribe flag associated with
      the video stream, instead of a specific stream id, we can't do what we
      do below till DMX establishes one.
   */
   else if (! (gb->mpenv.vstrm & STRM_AUTOSBC))
   {  ubyte vstrm;
      vstrm = gb->mpenv.vstrm & STRM_IDBITS;
      tpict.vseq = &vdec->strms[vstrm].vseq;
      
      /* Is this new session starting with subscription to a new video stream? */
      if (vdec->strm != vstrm)
      {	 /* Erase any flags associated with the previous stream */
	 vdecflags &= VD_SLOW | VD_STEP;
	 vdec->strm = vstrm;
	 
	 /* Have we ever dealt with this stream since the current data source
	    was opened ?
	 */
	 if (vdec->strms[vstrm].state)
	 {  vdecflags |= VD_VSEQHDR | VD_LOOKVSEQ;
	    (void)proc_vseqhdr(gb, &vdec->strms[vstrm].vseq,NULL,1);
	    vdecflags |= vdec->state & VD_USEVBV;
	    /*	    set_video(gb, vdec->vdm,vdec->zoom);*/
	    display |= DPY_VSEQ;
	 }	 
      }   
   }
   
   if ((isoflags & AVD_VIDON) && (vdecflags & ( VD_SLOW | VD_STEP)))
      display |= DPY_SLOW;
   dinf->ptype = 0;

   vneed = 4;   
   aneed = SAFE_AUDIO;
   /* If audio or video stream data is not subscribed to, or going to be
      ignored, we are declaring the needed data  for its decoder engine
      as the maximum amount its buffer can hold so that DMX calls can
      lock for the active stream - e.g. instead of always performing 
      nonblocking calls because the inactive stream seems to have enough
      data to do something...-
   */
   if ( !(isoflags & AVD_VIDON)) vneed = vidb->full;
   if ( !(isoflags & AVD_AUDON)) aneed = audb->full;
   
   if (vdecflags & VD_VSEQHDR)
   {  ptime = 1.0/look_picrate[vdec->strms[vdec->strm].vseq.picrate];
      tolerance[B_PICT] = -vdec->tbr*ptime;
      tolerance[P_PICT] = -ptime;
      tolerance[I_PICT] = -ptime;
   }
   else refstat = 0;
   
   for (;;)
   {
      if (!(isoflags & AVD_VSKIP) && CMD_AVAIL(gb->mpx_env, gb->cmdq))
      {	 nint r0;
	 
	 if ((r0 = process_command(gb->mpx_env, gb, BSY_FWD11172) ) == 0) goto LB_Return;
	 if ( r0 == 1)
	 {  uint32 cmd;
	    cntcmd = gb->cmdq.cmds[gb->cmdq.rp];
	    cmd_remove(gb->mpx_env, &gb->cmdq);
	    cmd = cntcmd.cmd;
	    if (cmd == MCMD_PLAYCTR)
	    {  if ( cntcmd.u.playctr.action & (PC_FWDSTEP | PC_PAUSE))
	       {  playtype = PC_FWDSTEP;
		  goto LB_Return;
	       }
	    }
	    
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
      
      if ( (isoflags & (AVD_VIDON | AVD_VSKIP)) == AVD_VIDON )
      {	 register int32 space;
	 register ubyte *d;
	 register ubyte *lastd;
	 uint32 phdr,r0;
	 int32 vpos;
	 
	 d = vidb->rp;   
	 vpos = vidb->b2 - d;
	 space = vidb->ready;
	 if (vpos < space) space = vpos;
	 lastd = d;
	 isoflags &= 0xffff ^ AVD_INPICT;
	 
	 for (;;)
	 {  /* The reason we are resetting vneed to 4, -length of a start code-
	       here, is because of a minute possibility. Consider the situation
	       in which a picture couldn't be rendered because vneed>space
	       and space>vpos && space<vidb->ready. Now clearly, in that 
	       situation a SAFEMARK search would alread have been done for
	       the last part of the vidb. But when sequentiality is reestablished,
	       with the newly acquired data, space value will be different and
	       if we actually read the final data bytes, not resetting the vneed
	       here would lock us up here without giving a chance to a
	       new (because of dsrcend) SAFEMARK search. i.e. otherwise, vneed
	       being larger than actually it needs be, would prevent this session
	       from ending.
	    */
	    vneed = 4;	    
	    if (vneed > space) 
	       goto LB_VReconcile;
	    if ( ( d[0]<<16 | d[1]<<8 | d[2] ) != 1) goto LB_Resync;
	    
	    r0 = d[3];
	    if (r0 == ST_PICTURE) 	
	    {  /* Max 5bytes of relevant pict.hdr. info + 4byte start code */
	       if (space < 9)
	       {  vneed = 9;
		  goto LB_VReconcile;
	       }
	       phdr = d[4]<<24 | d[5]<<16 | d[6]<<8 | d[7];
	       r0 = (phdr>>19) & 0x7;  /* Picture type */
	       if (r0<1 || r0>3)       /* Unsupported picture type */ 
	       {  d += 4;
		  space -= 4;
		  goto LB_Resync;
	       }     
	       pthis = ptref + ((phdr>>22) & 0x3ff);  /* Temporal reference */  	     
	       if (pthis > phigh) phigh = pthis;
	       
	       display += 1<<8;
	       isoflags |= AVD_INPICT;
	       
	       /* If a PTS is associated with this picture, retrieve it.
		  Note that if we bypass a picture for later processing, a PTS
		  might have been retrieved first time around, and second time
		  around getpts will not return one because the read pointer
		  will have moved in the stamps structure. This will not 
		  cause any problems however since PTS processing involving 
		  a future picture -done first time around- will work just
		  fine.
	       */
	       tf0 = getpts(&vdec->stamps, vidb, vidb->b2 - d, &vmrk);
	    	  
	       if ( tf0 >= 0 )
	       {  vlastpts = tf0;
		  if ( !(isoflags & AVD_VPTS))
		  {  /* We do this because a system time base might have already
			been set before this time base association is made.
			vlastptstref will not have a valid value if a time
			base was not already established, but in that case
			vnomtmbase will not be used anyway.
		     */ 
		     isoflags |= AVD_VPTS;
		     vnomtmbase = vlastpts - (pthis-vlastptstref)*ptime;		     
		  }
		  vlastptstref = pthis;
	       }
	       
	       if (refstat < (r0<<1))
	       {  d += 8;
		  space -= 8;
		  goto LB_Resync;
	       }

	       /* Logic to decide whether it is too late to render this picture.
		  Depending on whether audio or system time based synchronization,
		  is used, or whether one is used at all, seperate logic is applied.
		  When there is no established time sync. a picture is always
		  rendered, like in slow motion or at initial antry etc..
	       */
	       if ((isoflags & (AVD_ASYNC|AVD_VPTS)) == (AVD_ASYNC|AVD_VPTS))
	       {  uint32 r1 = filledaud(gb, acntout);
		  tf0 = anomtm - r1*amfreq;
		  tf1 = (pthis - vlastptstref) * ptime + vlastpts;

		  /* -ivg 
		     After switching over to JavaSound, many more frames
		     are being dropped.  I suspect it's caused by the
		     latency of JavaSound.  By ignoring the tolerence
		     factor, we were able to achieve 30fps.
		  */
		  /* tf1 += tolerance[r0]; */

		  if (tf1 < tf0)
		  {  goto LB_Skiprender;
		  }
		  
		  /* Logic to give priority to audio processing. Bypass and
		     come back to this picture again if decoded audio samples
		     are below the fullness level and audio engine actually
		     seems to have available data to process. For B pictures
		     fullness, for others half fullness is sought.
		  */
		  if (r1 < aworkmark && !(isoflags & AVD_AUDNODATA))
		     if ( r0==3  || r1<(aworkmark>>1) ) break;
	       }
	       else if (isoflags & AVD_SYSTM)
	       {  getcountdown(&sec);
		  tf0 = vsystmbase - sec;
		  tf0 *= vdec->speed;
		  tf1 = (pthis - vlastptstref) * ptime + vlastpts - vnomtmbase;
		  tf1 += tolerance[r0];
		  tf1 -= tf0;
		  if (tf1 < 0)
		  {  if (tf1 > -2.0)
		     {  goto LB_Skiprender;
		     }   
		     else 
		     {	isoflags  &= 0xffff ^ AVD_SYSTM;
			vdecflags &= 0xffff ^ VD_DISPLAYED;
		     }
		     /* Above is the logic to serve as a safety valve. If for 
			some weird reason we are behind in time by more than
			2 seconds, instead of skipping, render this picture and
			cause reestablisment of time base.
		     */
		  }
	       }
	       
	       vneed = vdec->vbfullness;
	       if (vdecflags & VD_USEVBV)
	       {  vneed = ((phdr>>3) & 0xffff) * vdec->vbvmult;
		  if (vneed < 40*1024) vneed = 40*1024;
	       }

	       if (space < vneed)
	       {  if (! (vdecflags & VD_SAFEMARKED))
		  {  if ( vidb->ready >= vpos || vidb->dsrcend )
		     {  ubyte *q, scod;
			
			q = d + space - 1;
			/* This search is sure to end since we at least have
			   the picture start code at which we are.
			*/
		     LB_Search:
			q -= 2;
			do do do ; 
			      while(*q--);
			   while (*q);
			while (q[2] != 1);
			scod = q[3];
			if ( scod != ST_PICTURE && scod != ST_GOP &&
			     scod != ST_VSEQ    && scod != ST_VSEQEND )
			   goto LB_Search;
			vdecflags |= VD_SAFEMARKED;
			vdec->safemark = q;		     
		     }
		     else goto LB_VReconcile;
		  }	       
		  if ( d >= vdec->safemark)
		  {  vdecflags &= 0xffff ^ VD_SAFEMARKED;
		     goto LB_VReconcile;
		  }
	       }
	       vneed = 4;
	       isoflags &= 0xffff ^ AVD_INPICT;

	       d = rdpicthdr(d+8, phdr, r0, &tpict);     
	       
	       if (r0 == B_PICT) 
	       {  uint32 rndflags = RF_COLOR;
		  if (vdecflags & (VD_SLOW | VD_STEP)) rndflags |= RF_FILT;
		  vdec->denv.fp[CURRENT][Y]  = vdec->fb[2][Y];
		  vdec->denv.fp[CURRENT][CR] = vdec->fb[2][CR];
		  vdec->denv.fp[CURRENT][CB] = vdec->fb[2][CB];
		  vdec->denv.fp[FUTURE][Y]  = vdec->fb[toggle][Y];
		  vdec->denv.fp[FUTURE][CR] = vdec->fb[toggle][CR];
		  vdec->denv.fp[FUTURE][CB] = vdec->fb[toggle][CB];
		  vdec->denv.fp[PAST][Y]  = vdec->fb[toggle^1][Y];
		  vdec->denv.fp[PAST][CR] = vdec->fb[toggle^1][CR];
		  vdec->denv.fp[PAST][CB] = vdec->fb[toggle^1][CB];	    
		  d = mp_rendpb(d, &tpict, &vdec->denv, rndflags);
		  dinf->bfrms++;
		  dinf->rdbytes += (d - lastd);
		  pdisp = pthis;
		  fbs = 2;
		  r0 = 2;
	       }
	       else if (r0 == P_PICT)
	       {  vdec->denv.fp[PAST][Y]  = vdec->fb[toggle][Y];
		  vdec->denv.fp[PAST][CR] = vdec->fb[toggle][CR];
		  vdec->denv.fp[PAST][CB] = vdec->fb[toggle][CB];
		  toggle ^= 1;
		  r0 = toggle;
		  vdec->denv.fp[CURRENT][Y]  = vdec->fb[toggle][Y];
		  vdec->denv.fp[CURRENT][CR] = vdec->fb[toggle][CR];
		  vdec->denv.fp[CURRENT][CB] = vdec->fb[toggle][CB];
		  d = mp_rendpb(d, &tpict, &vdec->denv, RF_FILT | RF_COLOR);
		  dinf->pfrms++;
		  dinf->rdbytes += (d - lastd);
		  if (refstat > 4)
		  {  pdisp = pfut;
		     fbs = toggle ^ 1;
		     pfut = pthis;
		  }
		  else
		  {  refstat += 2;
		     pfut = pthis;
		     r0 += 8;	    /* Just to serve as a flag */
		  }
	       }
	       else
	       {  toggle ^= 1;
		  r0 = toggle;
		  vdec->denv.fp[CURRENT][Y]  = vdec->fb[toggle][Y];
		  vdec->denv.fp[CURRENT][CR] = vdec->fb[toggle][CR];
		  vdec->denv.fp[CURRENT][CB] = vdec->fb[toggle][CB];
		  d = mp_rendi(d, &tpict, &vdec->denv, RF_COLOR);	    
		  dinf->ifrms++;
		  dinf->rdbytes += (d - lastd);
		  if (refstat > 4)
		  {  pdisp = pfut;
		     fbs = toggle ^ 1;
		     pfut = pthis;
		  }
		  else if (refstat == 2) 
		  {  refstat = 4;
		     pdisp = pthis;
		     fbs = toggle;
		  }
		  else
		  {  refstat += 2;
		     pfut = pthis;
		     r0 += 8;
		  }
	       }     
	       
	       {  nint i = r0 & 0x3;
		  vdec->mrk[i].gopval = gopval;
		  vdec->mrk[i].gopmrk = gopmrk;
		  vdec->mrk[i].entry  = vmrk + gb->dsrc.ofs;
		  vdec->mrk[i].hdr   = phdr;
		  if ( r0 > 2 ) break;	  /* Or may go to render another picture */		  
	       }
	       goto LB_jump;
	       
	    LB_Skiprender:
	       space -= 8;
	       d += 8;
	       if (r0 == B_PICT) goto LB_Resync;
	       if (refstat > 4)
	       {  refstat = 2;
		  pdisp = pfut;
		  fbs = toggle;
	       }
	       else
	       {  refstat = 2;
		  goto LB_Resync;
	       }
	       
	    LB_jump:
	       isoflags |= AVD_VSKIP;        
	        
	       break;	       
	    }	    
	    else if (r0 < 0xb0)	 /* Slice Start codes: [0x01 .. 0xAF] */  
	    {  d += 4;
	       space -= 4;
	    }  	    
	    else if (r0 == ST_GOP)
	    {  if (space < 8)
	       {  vneed = 8;
		  goto LB_VReconcile;
	       }
	       ptref = phigh + 1;
	       r0 = d[4]<<24 | d[5]<<16 | d[6]<<8 | d[7];
	       if ((r0 & 0x60) == 0x20)   /* Broken Link but NOT Closed GOP */
		  if (refstat == 6) refstat = 5;
	       
	       getptsmrk(&vdec->stamps, vidb, vidb->b2 - d, &vmrk);
	       gopval = r0;
	       gopmrk = vmrk + gb->dsrc.ofs;
	       space -= 8;
	       d += 8;
	       continue;
	    }	    
	    else if (r0 == ST_VSEQ)
	    {  uint32 rt0, rt1;
	       ubyte *tp;
	       nint newvseq;
	       
	       if (space < 140)
	       {  vneed = 140;
		  goto LB_VReconcile;
	       }
	       /* Make a quick comparison with the previously received vseqhdr,
		  ignore the default quantizer matrix indicator bits.
	       */
	       rt0 = d[4]<<24 | d[5]<<16 | d[6]<<8 | d[7];
	       rt1 = d[8]<<24 | d[9]<<16 | d[10]<<8 | d[11];
	       newvseq = rt0 != vdec->hdr1 || (rt1 | 0x3) != vdec->hdr2;
	       
	       tp = mp_rdvseqhdr(d+4, &tvseq);
	       
	       if ( ((vdecflags & (VD_VSEQHDR | VD_LOOKVSEQ)) == VD_VSEQHDR && newvseq)
		    || tp == 0)
		  goto LB_SkipVseqHdr;
	       
	       
	       if ( ! newvseq && (vdecflags & (VD_VSEQHDR | VD_LOOKVSEQ)) == VD_VSEQHDR )
		  (void)proc_vseqhdr(gb, &tvseq,&vdec->strms[vdec->strm].vseq,0);
	       else
	       {  if ( ! proc_vseqhdr(gb, &tvseq,NULL,1) ) goto LB_SkipVseqHdr;
		  vdec->hdr1 = rt0;
		  vdec->hdr2 = rt1 | 0x3;		  
		  vdec->strms[vdec->strm].vseq = tvseq;
		  vdec->strms[vdec->strm].state = 1;	       
		  vdecflags |= vdec->state & VD_USEVBV;
		  vdecflags |= VD_VSEQHDR;
		  vdecflags &= 0xffff ^ (VD_SYSTM | VD_DISPLAYED | VD_SAFEMARKED);
		  pdisp  = pfut = ptref = 0;
		  toggle = 1;
		  refstat= 2;
		  phigh  = -1;	  
		  ptime = 1.0/look_picrate[tvseq.picrate];
		  tolerance[B_PICT] = -vdec->tbr*ptime;
		  tolerance[P_PICT] = -ptime;
		  tolerance[I_PICT] = -ptime;
		  /*		  set_video(gb, vdec->vdm,vdec->zoom);*/
		  display |= DPY_VSEQ;  
	       }
	       vdecflags &= 0xffff ^ VD_LOOKVSEQ;   	       
	       r0 = tp - d;
	       d += r0;
	       space -= r0;             
	       continue;
	    
	    LB_SkipVseqHdr:	       
	       d += 4;
	       space -= 4;             
	       continue;
	    }   
	    else if (r0 == ST_RSVDB1)
	    {  ubyte vstrm;
	       
	       if (space < 12)
	       {  vneed = 12;
		  goto LB_VReconcile;
	       }
	       
	       /* Make sure that this reserved start code was put into the video
		  stream by us. If so, this means that we entered this session with
		  an "autosubscribe to video" setting, and DMX has since subscribed
		  to the first encountered video stream and put the 12byte notice
		  indicating this fact and the stream id, at the very beginning of
		  the video buffer.
	       */
	       {  nint k, bogus;
		  bogus = 0;
		  
		  for (k=1; k<8; k++) bogus |= (d[3+k] != k);
		  if ( bogus || d != vidb->b1)
		  {  d += 4;
		     space -= 4;
		     continue;
		  }
	       }
	       /* Subscribed stream id is the 12th byte of this notice */
	       vstrm = d[11];
	       tpict.vseq = &vdec->strms[vstrm].vseq;
	       
	       if (vdec->strm != vstrm)
	       {  vdecflags &= VD_SLOW | VD_STEP;
		  vdec->strm = vstrm;
		  if (vdec->strms[vstrm].state)
		  {  vdecflags |= VD_VSEQHDR | VD_LOOKVSEQ;
		     (void)proc_vseqhdr(gb, &vdec->strms[vstrm].vseq,NULL,1);
		     vdecflags |= vdec->state & VD_USEVBV;
		     /*		     set_video(gb, vdec->vdm,vdec->zoom);*/
		     display |= DPY_VSEQ;
		     ptime = 1.0/look_picrate[vdec->strms[vdec->strm].vseq.picrate];
		     tolerance[B_PICT] = -vdec->tbr*ptime;
		     tolerance[P_PICT] = -ptime;
		     tolerance[I_PICT] = -ptime;
		  }
		  else refstat = 0;
	       }
	       d += 12;
	       space -= 12;
	       continue;
	    }
	    else if (r0 == ST_VSEQEND)
	    {  vdecflags |= VD_LOOKVSEQ;
	       isoflags &= 0xffff ^ (AVD_SYSTM | AVD_VPTS | AVD_ASYNC);
	       space -= 4;
	       d += 4;
	       continue;
	    }
	    else 
	    {  d += 4;
	       space -= 4;
	    }
	 
	 /* We either jump here to skip a picture, or find out at the very top
	    that we are not at a start code. Also, flow of control drops to
	    here when we encounter a start code that we do not deal with, or 
	    a slice to be skipped. We will be searching for a start code.
	 */
	 LB_Resync:
	    {  ubyte *q;
	       int32 r2;
	       /* This is a sanity check. If space is actually ever < 0, then
		  this means because of bad data, we did wrong decoding at
		  some level (GOP, VSEQHDR but not at PICTURE level) and
		  underflowed available sequential space. Thus we are
		  making a recovery attempt. Note that even we had bad data,
		  ending up here with negative space value is a very low
		  probability situation.
	       */
	       if (space < 0)
	       {  d += space - 3;
		  space = 3;
		  vneed = 4;
		  goto LB_VReconcile;
	       }
	       
	       /* We know that this search for a start code is bound to succeed
		  because of the panic region at the end of the video buffer.
	       */
	       q = d;
	       do do do ;
		     while (*d++);
		  while (*d);
	       while (d[1] != 1);
	       d -= 1; 
	       r2 = d - q;	       
	       if (r2+4 > space)
	       {  /* A valid start code in the available space couldn't be
		     found, we back up by 3 bytes because just the last
		     byte of a start code might have been missing
		  */
		  r2 = space - 3;
		  d = q + r2;
		  vneed = 4;
	       }
	       else 
	       {  space -= r2;
		  continue;
	       }
	    }
	 
	 /* Jumping or dropping down to here only when vneed > space */ 
	 LB_VReconcile:
	    r0 = d - lastd;
	    vidb->ready -= r0;
	    vpos -= r0;
	    lastd = d;  
	    if (vidb->ready >= vpos)
	    {  if (vpos > vdec->vbsqsz) 
	       {  vidb->ready -= vpos - vdec->vbsqsz;
		  vpos = vdec->vbsqsz;
	       }
	       lastd = vidb->b1 - vpos;
	       (void) memcpy(lastd, vidb->b2 - vpos, vpos);
	       vpos = vidb->b2 - lastd;
	       d = lastd;
	       space = vidb->ready;
	       vdecflags &= 0xffff ^ VD_SAFEMARKED;
	    }
	    if (vneed > vidb->ready) break;
	 }
	 /* Another sanity check here, if we had a buffer underflow at the
	    picture decoding level it is caught here. Only reason we would ever
	    have such a situation would be either bad data at picture level
	    or unusually large picture data in a free bit rate stream. Note
	    that these two cases do not necessarily cause buffer underflow, and
	    instead may just require a resync.
	 */
	 if (d > vidb->b2) d = vidb->b2;
	 r0 = d - lastd;
	 if (r0 > vidb->ready)
	 {  d = lastd + vidb->ready;
	    vidb->ready = 0;
	 }
	 else vidb->ready -= r0;
	 vidb->rp = d;
      }
      /*
      else if ( ! (isoflags & AVD_VIDON) )
      {
	 If we had a freeze video mode we would handle it here... 
      }	
      */
      
      /* Ofcourse, AVD_VSKIP is always off when AVD_VIDON is off */
      if ( !(isoflags & AVD_VSKIP)) goto LB_Audioduty;
      
      if (vdecflags & VD_SLOW)
      {  for (;;)
	 {  getcountdown(&sec);
	    tf1 = sec;
	    tf0 = vsystmbase - tf1;
	    if (tf0*vdec->speed < ptime) apvnap(4);
	    else break;
	 }
	 vsystmbase = tf1;
      }
      else if (refstat > 4)
      {	 nint flipflop = 0;
	 
	 if ((isoflags & (AVD_ASYNC|AVD_VPTS)) == (AVD_ASYNC|AVD_VPTS)) 
	    for (;;)
	    {  uint32 r1;
	       r1 = filledaud(gb, acntout);
	       tf0 = anomtm - r1*amfreq;
	       tf1 = (pdisp - vlastptstref - vdec->td) * ptime + vlastpts;
	       if (tf1 > tf0) 
	       {  if ( r1 > aworkmark ) apvnap(4);
		  else if (aneed < audb->ready || flipflop) goto LB_Audioduty;
		  else { flipflop = 1;  apvnap(4); }
	       }
	       else break;
	    }
	 else if (isoflags & AVD_SYSTM)
	    for (;;)
	    {  getcountdown(&sec);
	       tf0 = vsystmbase - sec;
	       tf0 *= vdec->speed;
	       tf1 = (pdisp - vlastptstref - vdec->td) * ptime + vlastpts - vnomtmbase;
	       tf1 -= tf0;
	       if (tf1 > 0)
	       {  /* If for some weird reason we seem to be ahead by more than
		     5 seconds, we are taking care of this situation. Note that
		     we do not have this test in the case of audio sync based 
		     operation, because in that case existence of audio and our 
		     auto sync. type switching logic will take care of it.
		  */
		  if (tf1 > 5.0)
		  {  isoflags  &= 0xffff ^ (AVD_VPTS | AVD_SYSTM);
		     vdecflags &= 0xffff ^ VD_DISPLAYED;
		     break;
		  }
		  if ( vidb->ready < vidb->full-(vidb->full>>3) && flipflop)
		     goto LB_Audioduty;
		  else { flipflop = 1;  apvnap(4); }
	       }
	       else break;
	    }
      }
      ccnv_disp(gb, vdec->fb[fbs][Y], vdec->fb[fbs][CR],vdec->fb[fbs][CB],
		vdec->bhsz, vdec->bvsz, vdec->vdm, vdec->zoom);
      isoflags &= 0xffff ^ AVD_VSKIP;
      display |= DPY_DISP;
      vdecflags |= VD_DISPLAYED;
      vdec->fbs = fbs;

      if (fbs == 2) goto LB_SkipAllDuty;
              
   LB_Audioduty:      
      if ( isoflags & AVD_AUDON )
      {	 register int32  apos,space;  
	 register uint32 r0,r1;
	 register ubyte *lastq, *q;
      
	 /* Audio alone sleeping & Time to process audio threshold  Logic */
	 for (;;)
	 {  r0 = filledaud(gb, acntout);
	    if (r0 > aworkmark)
	       if ( vneed > vidb->ready) apvnap(4);
	       else goto LB_Skipaudio;
	    else break;
	 }
	 
	 /* Part of when to switch to system time based synchronization Logic */
	 isoflags &= 0xffff ^ (AVD_AUDEMPTY | AVD_AUDNEED);
	 if (r0 == 0) isoflags &= 0xffff ^ AVD_ASYNC;
	 if (r0 < (aworkmark>>1)) isoflags |= AVD_AUDEMPTY;
	 
	 q = audb->rp;
	 
	 for (;;)
	 {  apos = audb->b2 - q;
	    space = audb->ready;
	    if (apos < space) space = apos;
	    lastq = q;
      
	 LB_search_nextframe:     
	    space -= SAFE_AUDIO;
	    do
	    {  do
	       {  if (space < 0) 
		  {  aneed = SAFE_AUDIO;  goto LB_AReconcile; }
		  r0 = q[0];
		  q++;
		  space--;
	       } while (r0 != 0xff);
	       r0 = q[0];	 
	    } while ( (r0 & 0xfc) != 0xfc );
	    
	    r1 = (q[1] >> 2) | ((r0 & 0x2) << 6);
	    r1 |= ((q[2]>>6) == 0x3) << 6;
	    
	    space += SAFE_AUDIO;
		
	    if (prevhdr != r1)
	    {  r0 = lk_framesz[r1];	    /* r1 = LCbb.bbss  */
	       if ( ! r0 ) goto LB_search_nextframe;	 
	       if (! (r0 & FREE_BRATE) ) framesz = r0;
	       else framesz = 0;	 
	       if (frmcnt > 8)
	       {  wraud(gb, adec->audbuf, asoftwait);
	          acntout += asoftwait;
		  anomtm += asoftwait * amfreq;;
	       }
	       asoftwait = 0;
	       aprms.ap = adec->audbuf;
	       frmcnt = 0;
	       instore = 0;
	       
	       if ((r1 & 0x3) != (chanfreq & 0x3)) 
		  audattn |= AD_CHGFREQ;
	       else
		  audattn &= 0xffff ^ AD_CHGFREQ;	 
	       if (((chanfreq & 0xc) << 4) != (r1 & 0x40))  
		  audattn |= AD_CHGCHANNEL;
	       else audattn &= 0xffff ^ AD_CHGCHANNEL;	 
	       prevhdr = r1;
	       afrmhdr = q[0]<<16 | q[1]<<8 | q[2];
	       display |= DPY_ASEQ;	 
	    }      
	    q -= 1;	     /* point back to the frame header */
	    space += 1;
		 
	    if (framesz)
	    {  r0 = (uint32)(q[2] & 0x2) >> 1; /* put padding bit at bit0 */
	       if (r1 & 0x80) r0 <<= 2;   /* if layer_1 one slot is 4 bytes */
	       r0 += framesz;
	       aprms.dsize = r0;	 
	       if (r0 > space)
	       {  aneed = r0;
		  goto LB_AReconcile;
	       }
	       if ( ! mp_daframe(q, &aprms)) 
	       {  q += 1;
		  space -= 1;
		  mp_printf("mpx: bad audio frame : %d\n",aprms.status);
		  goto LB_search_nextframe;	    
	       }	 	       
	       dinf->rdbytes += r0; 
	    }
	    else
	    {  ubyte *qq;
	    
	       aprms.dsize = space;	
	       if (!(qq=mp_daframe(q, &aprms))) 
	       {  aneed = aprms.need;
		  goto LB_AReconcile;
	       }
	       r0 =  qq - q;
	       dinf->rdbytes += r0;
	    }
	    
	    tf0 = getpts(&adec->stamps, audb, audb->b2 - q, &amrk);
	    if (tf0 >= 0 )
	    {  alastpts = tf0;
	       aswaitpts = 0;
	       if ( adec->stamps.flags & DMXF_NEWSTRM )
	       {  adec->stamps.flags &= 0xff ^ DMXF_NEWSTRM;
		  audattn |= AD_SYSEND;
	       }
	    }
	    q += r0;
	    space -= r0;
	    
	    asoftwait += aprms.nsmp;
	    aswaitpts += aprms.nsmp;
	    frmcnt++;
	    instore++;
	    
	    if (audattn)
	    {  if (audattn & AD_SYSEND)
	       {  wraud(gb, adec->audbuf, asoftwait);
		  acntout += asoftwait;
		  anomtm += asoftwait * amfreq;
		  asoftwait = 0;
		  instore = 0;
		  audattn &= 0xffff ^ AD_SYSEND;
		  audattn |= AD_SYSEND2;
	       }
	       else if (audattn & AD_SYSEND2)
	       {  if ( filledaud(gb, acntout) == 0 )
		     audattn &= 0xffff ^ AD_SYSEND2;
	       }
	       else if (audattn & AD_CHGCHANNEL)
	       {  if (frmcnt > 8)
		  {  if (r1 & 0x40)
		     {  if (aprms.listen == AUD_LR)
			   set_audio(gb, alrates[r1 & 0x3],aprms.quality,AUD_L);
			aprms.listen = AUD_L;
		     } 
		     else
		     {  if (adec->listen == AUD_LR)
			   set_audio(gb, alrates[r1 & 0x3],aprms.quality,AUD_LR);
			aprms.listen = adec->listen;
		     }
		     chanfreq = ((r1 & 0x40)>>4) | (chanfreq & 0x3);
		     audattn &= 0xffff ^ AD_CHGCHANNEL;
		     r0 = (alrates[chanfreq & 0x3]<<1) >>
			   (aprms.quality + (aprms.listen != AUD_LR));
		     aworkmark = adec->ahead * r0;
		     amfreq = 1.0 / r0;
		  }
	       }
	       else if (audattn & AD_CHGFREQ)
	       {  if (frmcnt > 8)
		  {  set_audio(gb, alrates[r1 & 0x3],aprms.quality,aprms.listen);
		     chanfreq = (r1 & 0x3) | (chanfreq & 0xc);
		     audattn &= 0xffff ^ AD_CHGFREQ;
		     r0 = (alrates[chanfreq & 0x3]<<1) >>
			   (aprms.quality + (aprms.listen != AUD_LR));
		     aworkmark = adec->ahead * r0;
		     amfreq = 1.0 / r0;
		  }
	       }
	       else if (audattn & AD_CHGAUDM1)
	       {  wraud(gb, adec->audbuf, asoftwait);
		  acntout += asoftwait;
		  anomtm += asoftwait * amfreq;
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
	       }
	       else if (audattn & AD_CHGAUDM2)
	       {  if (filledaud(gb, acntout) == 0)
		  {  set_audio(gb, alrates[chanfreq & 0x3],aprms.quality,aprms.listen);
		     audattn &= 0xffff ^ AD_CHGAUDM2;
		     amfreq = 1.0 / ((alrates[chanfreq & 0x3]<<1) >> 
				     (aprms.quality + (aprms.listen != AUD_LR)));
		  }
		  else if (asoftwait > aworkmark)
		  {  drainaud(gb, 0); acntout = 0; }
	       }
	       else if ((audattn & AD_INITIALENTRY) && (frmcnt > 20))
	          audattn &= 0xffff ^ AD_INITIALENTRY;
	    }
	    else if (instore >= 5) goto LB_AReconcile;
	    goto LB_search_nextframe;
      
      
	 LB_AReconcile:
	    r0 = q - lastq;
	    apos -= r0;
	    audb->ready -= r0;
	    if (aneed>apos && audb->ready>=apos)
	    {  lastq = audb->b1 - apos;
	       (void) memcpy(lastq, q, apos);
	       q = lastq;
	    }
	    
	    if (aneed <= audb->ready)
	    {  isoflags &= 0xffff ^ AVD_AUDNODATA;
	       if ( audattn || instore < 5 ) continue;
	    }
	    else isoflags |= AVD_AUDNODATA;

	    if (! audattn  &&  instore > 0)
	    {  if (aswaitpts < 0)
		  anomtm += asoftwait * amfreq;
	       else
	       {  anomtm = alastpts + aswaitpts * amfreq;
		  isoflags |= AVD_ASYNC | AVD_APTS;
	       }
	       if (isoflags & AVD_APTS) isoflags |= AVD_ASYNC;
	       wraud(gb, adec->audbuf, asoftwait);
	       acntout += asoftwait;
#ifdef DISABLE	       
	       if (acntout > 44100*8)	/* Avoid overflow of audio sample counts */
	       {
		   drainaud(gb, 0);
		   acntout = 0;
	       }
#endif 
	       aprms.ap = adec->audbuf;
	       asoftwait = 0;
	       aswaitpts = -256000;
	       aneed = SAFE_AUDIO;
	       if (instore < 5) isoflags |= AVD_AUDNEED;
	       display += instore<<16;
	       instore = 0;
	    }
	    break;
	 }   
	 audb->rp = q;       	    
      }
      else if (audb->ready > (audb->full>>2))
      {	 audb->rp += audb->ready - (audb->full>>2);
	 audb->ready = (audb->full>>2);
	 if (audb->rp >= audb->b2)
	    audb->rp = audb->b1 + (audb->rp - audb->b2);
      }
   LB_Skipaudio:
   
      /* Logic to decide if normalization is required from either of
	 audio or video engine's perspective.
      */
      if ( isoflags & AVD_INPICT )
	 if ( vneed > vidb->ready )
	    if ( audb->ready < (audb->full >> 1))	    
      {	 if ((isoflags & (AVD_ASYNC|AVD_VPTS)) == (AVD_ASYNC|AVD_VPTS))
	 {  uint32 r1;
	    r1 = filledaud(gb, acntout);
	    tf0 = anomtm - amfreq * r1; 
	    tf1 = (pthis - vlastptstref) * ptime + vlastpts;
	    tf1 -= 0.35;
	    if (tf1 < tf0) goto LB_Normalize;
	 }
	 else if (isoflags & AVD_SYSTM)
	 {  getcountdown(&sec);
	    tf0 = vsystmbase - sec;
	    tf0 *= vdec->speed;
	    tf1 = (pthis - vlastptstref) * ptime + vlastpts - vnomtmbase;
	    tf1 -= 0.35;
	    if (tf1 < tf0) goto LB_Normalize;
	 }
      }
      
      if ( ! ( (isoflags & (AVD_AUDEMPTY | AVD_AUDNEED)) == (AVD_AUDEMPTY | AVD_AUDNEED) 
	       && vidb->ready < (vidb->full >> 1))
	 )
	 goto LB_SkipNormalize;
	 
   LB_Normalize:
      {	 uint32 vfull, afull;
	 register float tf0, tf1;
	 if (gb->mpenv.options & OPT_DEBUG1)
	 {  float ntm;
	    getcountdown(&sec);
	    ntm = sec;
	    mp_printf("mpx: Normalizing, at:%8.2f seconds.\n", ntm);
	 }
	    
	 vfull = vidb->full - (vidb->full>>3);
	 afull = audb->full - (audb->full>>3);
	 getcountdown(&sec);
	 tf0 = sec;
	 tf1 = 0;   
	 while (vidb->ready < vfull  &&  audb->ready < afull  &&  tf1 < vdec->maxnrmdel)
	 {  apvnap(50);
	    dmxmpeg(gb, afull,vfull,gb->dmx_actions);
	    getcountdown(&sec);
	    tf1 = tf0 - sec;
	    if (gb->dsenv.syncrd_state == DSRCEND) break;
	 }
	 vdecflags &= 0xffff ^ VD_DISPLAYED;
	 isoflags  &= 0xffff ^ AVD_SYSTM;
	 
      }
   LB_SkipNormalize:
         
      /* Voluntary DMX duty logic */      
      if (  vidb->ready < vidb->full-(vidb->full>>3) &&   
	    audb->ready < audb->full-(audb->full>>3)  )
      {	 dmxmpeg(gb, aneed, vneed, gb->dmx_actions);	 
	 if (vneed > vidb->ready && aneed > audb->ready) 
	 {  /* Signal the ctrl client the movie has ended. */ 
	    if ( vidb->dsrcend && audb->dsrcend ) send_stats(gb, 1);
	    if ( vidb->dsrcend || audb->dsrcend ) break; 
	 }
      }
   
   LB_SkipAllDuty:
      
      /* Display area may be updated only if a new VSEQHDR is received, or a new
	 audio sequence has started, or if the player is in slow motion or step mode.
	 Also if 32 pictures have been received or 50 audio frames output.
	 Note that: for every new picture received  display += (1<<8)
		    for every audio frame dump      display += (1<<16)
      */
      if ( ((display>>16)>50) || (display & ( 0xe000 | DPY_VSEQ | DPY_ASEQ | DPY_SLOW)))
      {	 nint update = 0;
	 
	 /* Do not update the location bar for sequential data sources */
	 if ( ! (gb->dsrc.type & DSRC_FWDONLY) )
	 {  if (/*(display & 0xe000) && */ vmrk != 0)
	    {  dinf->loc = (float)(vmrk + gb->dsrc.ofs) / gb->dsrc.size;
	       update |= UPD_BAR;
	    }
	    else if ( ((display>>16) > 50) && amrk != -1)
	    {  dinf->loc = (float)(amrk + gb->dsrc.ofs) / gb->dsrc.size;
	       update |= UPD_BAR;
	    }
	 }
	 
	 /* Update video time and picture info only if a new picture has been displayed */
	 if (display & DPY_DISP)
	 {  uint32 phdr = vdec->mrk[fbs].hdr;
	    uint32 gop  = vdec->mrk[fbs].gopval;
	    
	    /* Picture type is to be shown and updated only in slow motion */
	    if (display & DPY_SLOW) dinf->ptype = (phdr>>19) & 0x7;
	      
	    /* Can't generate video time info if not have a valid GOP associated
	       with this latest displayed picture. (Random jumps...)
	    */
	    if (gop)
	    {  nuint fps  = look_picrate[tpict.vseq->picrate] + 0.5;
	       nuint hour = (gop >> 26) & 0x1f;
	       nuint min  = (gop >> 20) & 0x3f;
	       nuint sec  = (gop >> 13) & 0x3f;
	       nuint frm  = (gop >>  7) & 0x3f;
	       
	       frm += (phdr>>22) & 0x3ff; /* Add tref */
	       while (frm >= fps)
	       {  frm -= fps; sec += 1;
		  if (sec > 59)
		  {  sec = 0; min += 1;
		     if (min > 59) { min = 0; hour += 1; }
		  }
	       }	       
	       dinf->hour = hour;
	       dinf->min  = min;
	       dinf->sec  = sec;
	       dinf->frm  = frm;
	       update |= UPD_VTM | UPD_BAR;		   
	    }	    
	 }	 
	 if (display & DPY_ASEQ)
	 {
	    update |= UPD_ASTR | UPD_BAR;
	    afrmhdr = 0;
	 }	 
	 if (display & DPY_VSEQ)
	 {
	    update |= UPD_VSTR | UPD_BAR;
	 }
	 display = display & DPY_SLOW;    
	 if (update) send_stats(gb, 0);
      }
      
      if ((vdecflags & (VD_STEP | VD_DISPLAYED)) == (VD_STEP | VD_DISPLAYED))
	 break;
      
      /* Switch synchronization scheme if required */
      if ((isoflags & (AVD_ASYNC | AVD_VPTS)) == (AVD_ASYNC | AVD_VPTS))
	 isoflags &= 0xffff ^ AVD_SYSTM;
      else if ( !(isoflags & AVD_SYSTM) &&
	         ((vdecflags & (VD_SLOW | VD_STEP | VD_DISPLAYED)) == VD_DISPLAYED))
      {  getcountdown(&sec);
	 vsystmbase = sec;
	 if (isoflags & AVD_VPTS)
	    vnomtmbase = (pdisp - vlastptstref) * ptime + vlastpts;
	 else
	 {  vlastpts = 0;
	    vnomtmbase = 0;;
	    vlastptstref = pdisp;
	 }
	 isoflags |= AVD_SYSTM;    
      }
      vdecflags &= 0xffff ^ VD_DISPLAYED;
   }

LB_Return:
   vdec->lastplaytype = playtype;
   vdec->state = vdecflags;
   vdec->toggle = toggle;
   vdec->phigh  = phigh;
   vdec->ptref  = ptref;
   vdec->pfut   = pfut;
   vdec->pdisp  = pdisp;
   vdec->refstat= refstat;
   vdec->gopval = gopval;
   vdec->gopmrk = gopmrk;
   vdec->entry = vmrk;
   adec->entry = amrk;
   gb->mpenv.busy = BSY_IDLE;
}
