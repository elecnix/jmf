/*
 * @(#)ln_vseq.c	1.2 02/08/21
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
#include "ln_lnd.h"

#ifdef WIN32
#define valloc malloc
#define hrtime_t unsigned long
#define gethrtime (unsigned long)GetTickCount
#endif

#ifdef LINUX
#define hrtime_t unsigned long
unsigned long gethrtime() {
    struct timeval tv;
    struct timezone tz;
    gettimeofday(&tv, &tz);
    return tv.tv_sec * 1000000000 + tv.tv_usec * 1000;
}
#endif

/* R e a d  P i c t u r e  H e a d e r
   This is a custom made one for the VSEQ and 11172 players.

   The one single field in the picture header, which can mess up our decoder
   environment is the forward/bacward_f_code. That supposed to have a value
   between 1-7. Before deriving the pict.fwd/bwd_rsize from it, we are forcing
   it to be 1. 
*/ 
ubyte *rdpicthdr(ubyte *d, uint32 phdr, nint ptype, mp_pict *pict)
{  register uint32 r0,r1;
   register nint bits;

   pict->ptype = ptype;
   if (ptype == P_PICT)		 /* 33bits */
   {  pict->fullpel_fwd = (phdr>>2) & 1;
      r1 = ((phdr&3)<<8) | *d++;
      r0 = r1>>7;
      r0 |= r0 == 0; /* Force to nonzero */
      pict->fwd_rsize = r0 - 1;
      r1 &= 0x7f;
      bits = 7;      
   }
   else if (ptype == B_PICT)     /* 37bits */
   {  pict->fullpel_fwd = (phdr>>2) &1;
      r1 = ((phdr&3)<<8) | *d++;
      r0 = r1>>7;
      r0 |= r0 == 0;
      pict->fwd_rsize = r0 - 1;
      pict->fullpel_bwd = (r1>>6) & 1;
      r0 = (r1>>3) & 7;
      r0 |= r0 == 0;
      pict->bwd_rsize = r0 - 1;
      r1 &= 7;
      bits = 3;      
   }
   else				 /* 29bits */
   {  r1 = phdr & 0x7;
      bits = 3;
   }
   /*Extract extra_information_picture */
   while ((1<<(bits-1)) & r1)
   {  if (bits<11)
      {	 r1 = (r1 << 16) | (d[0] << 8) | d[1];
      	 d += 2;
      	 bits +=16;
      }
      bits -= 9;
   }
   bits -=1;   /* Waste extra_bit_picture */
   
   /* Put back unnecessarily retrieved bytes*/
   d -= bits >> 3;
   
   /* We will get rid of any picture_extension_data or user_data so that
      the caller can assume to have slice start codes next. Since it is
      very unlikely to find any extension code, following type of search
      will get us out of here with minimum overhead...
      Note that after the last skipped start code, we are not skipping 
      the user_data or extension_data part, and relying on the picture
      decoder to do the rest of the skipping...
   */
   r1 = ST_EXTENSION;
LB_SkipExtensions:
   r0 = d[0]<<16 | d[1]<<8 | d[2];
   d += 3;
   while (r0 != 1)
      r0 = ((r0 & 0xffff) << 8) | *d++;
   r0 = *d++;
   if ( r0 == r1 || r0 == ST_USERDATA)
   {  if ( r0 == ST_USERDATA) return(d);
      r1 = ST_USERDATA;
      goto LB_SkipExtensions;
   }   
   return( d - 4 );
}

/* Release the previously allocated picture buffers memory and allocate new
   one to accomodate picture size of the new video sequence.
   1(Y) +  3*1.5 (YCrCb) + 4 (rendered image buffer for capture tool) = 9.5
   Round up 9.5 to 10.
   
   So we allocate 10 bytes for every possible image pixel.
   An additional Y size buffer is allocated at the beginning to avoid bad data
   segment violation cases. Note that we only allow the vertical reference
   vector to be as big as the vertical size of the picture. So worst case will
   not reference behind the allocated buffer area...
*/  
static nint
adj_pictb(typ_mpgbl *gb, uint32 pictsz)
{  uint32 msz = 256 * 10; /* pictsize is in MacroBlocks (= 16x16 = 256pixels) */ 
   free(gb->mpenv.pictb);   
   gb->mpenv.pictb = (ubyte *)valloc(pictsz * msz);
   if ( gb->mpenv.pictb == NULL ) ;
   return(1);   
}

static nint
adj_vidb(uint32 vbsz, uint32 vbsqsz)
{
   /*mp_printf("Asking more video operational  buffer \n");*/
   return(1);
}

nint
proc_vseqhdr(typ_mpgbl *gb, mp_vseq *nv, mp_vseq *ov, nint fresh)
{  nint k,m,n;
   uint32 w;
   ubyte *q;
   typ_vdec *vdec = &(gb->vdec);

   /* This is to handle the case where a vseqhdr is repeated in the
      video bitstream with a possible quantizer matrix change.
   */
   if ( ! fresh )
   {
      if ( nv->defaultinter ) 
      {	 if ( ! ov->defaultinter )
	 {  for (k=0;k<31;k++)
	    {  nuint r0, r1;
	       r0 = 3*(k+1);
	       r1 = 2*(k+1);
	       for (m=0;m<6;m++)
	       {  w = r0 - (~r0 & 1);
		  for (n=0;n<64;n++)
		     vdec->denv.lkp_levd[k][m][n] = w;
		  r0 += r1;
	       }
	    }
	    ov->defaultinter = 1;
	 }
      }
      else
      {	 nuint r2, prevr2, prevn;
	 prevr2 = 512;	/* an impossible quantizer matrix value */
	 prevn  = 255;	/* an impossible n */
	 ov->defaultinter = 0;
	 
	 for (n=0;n<64;n++)
	 {  r2 = nv->inter_qntmat[n];
	    if (r2 != ov->inter_qntmat[n])
	    {  ov->inter_qntmat[n] = r2;
	       if (r2 == prevr2)
		  for (k=0;k<31;k++)
		  for (m=0;m<6;m++)
		     vdec->denv.lkp_levd[k][m][n] = vdec->denv.lkp_levd[k][m][prevn];
	       else
	       {  prevr2 = r2;
		  prevn  = n;
		  for (k=0;k<31;k++)
		  {  nuint r0, r1;
		     r1 = 2*(k+1);
		     r0 = 3*(k+1);
		     for (m=0;m<6;m++)
		     {	w = r0 * r2;
			w >>=4;
			w -= (~w & 1);
			if (w > 2047) w = 2047;
			vdec->denv.lkp_levd[k][m][n] = w;
			r0 += r1;
		     }
		  }
	       }
	    }
	 }
      }	 
      if ( ! nv->defaultintra || ! ov->defaultintra) 
      {	 nuint r2, prevr2, prevn;
	 prevr2 = 512;
	 prevn  = 255;
	 ov->defaultintra = nv->defaultintra;
	 for (n=0; n<64; n++)
	 {  r2 = nv->intra_qntmat[n];
	    if (r2 != ov->intra_qntmat[n])
	    {  ov->intra_qntmat[n] = r2;
	       if (r2 == prevr2)
		  for (k=0;k<31;k++)
		  for (m=0;m<6;m++)
		     vdec->denv.lki_levd[k][m][n] = vdec->denv.lki_levd[k][m][prevn];
	       else
	       {  prevr2 = r2;
		  prevn  = n;
		  for (k=0; k<31; k++)
		  {  nuint r0, r1;
		     r0 = 2*(k+1);
		     r1 = r0;
		     for (m=0; m<6; m++)
		     {	w = r0 * r2;
			w >>=4;
			w -= (~w & 1);
			if (w > 2047) w = 2047;
			vdec->denv.lki_levd[k][m][n] = w;
			r0 += r1;
		     }
		  }
	       }
	    }
	 }
      } 	 
      return(1);
   }   
   
   /* See if the currently allocated picture buffers can accomodate the size
      of this vseq.
   */
   {  nuint hmb, vmb;
      hmb = (nv->hsize+15)>>4;
      vmb = (nv->vsize+15)>>4;
      w = hmb * vmb;
      if (w>MAX_NMB || hmb>2048/16 || vmb>2048/16) 
      {
	  /*	 scrprint("\nPicture size is too big to handle !!");*/
	 return(0);
      }
      if (w > vdec->maxpict)
      {	 /* Need to allocate bigger picture&display buffers */
	 if (! adj_pictb(gb, w) ) return(0);
	 vdec->maxpict = w;
	 /* So that we will not attempt to redisplay the displayed picture if any */
	 vdec->fbs = 8;
      }
	 
      vdec->bhsz = hmb << 4;
      vdec->bvsz = vmb << 4;
      vdec->phsz = nv->hsize;
      vdec->pvsz = nv->vsize;

      /* send back through the control channel the sizes of the image. */
      /* send_size(); */
   }
   
   /* Video buffer areas sizes sufficiency check */
   {  uint32 brate, vbsz, vbsqsz;
   
      /* Check the sufficieny of the size of the sequentiality area in the 
	 video stream  buffer. We always have at least 64Kbyte of sequentiality
	 area allocated, which for most MPEG1 video sequences (396 MBs), 
	 translates into a bit/pixel value of 5.17, more than enough for the
	 typical highest density I_Picture pit/pixel value of 2.25.
	 In case we are dealing with a larger picture size, the default value
	 of 2.5, we are otherwise using should be more than enough. Even if
	 that is not the case, we will simply be -gracefully- skipping that
	 particular picture...
      */
      vbsqsz = (uint32)(nv->hsize *  nv->vsize * vdec->sqbitppix) >> 3;
      vbsqsz = ((vbsqsz + 4095) / 4096) * 4096;
      if (vbsqsz < 64*1024) vbsqsz = 64*1024;
      
      /* If a constant bitrate is specified in the vseq header, use it, otherwise
	 for variable bitrate sequences, make an estimation based on the picture
	 size. Assume 30frms/sec. to be on the safe side.
      */	  
      if (nv->bitrate == 0x3ffff)
      {	 /* For most MPEG sequences average bitrate is about 0.53bit/pixel.
	    Our system parameter "avrbitppix" is by default at least 0.75bit/pix
	    to be on the safe side. So we are using a video buffer length of
	    1.75sec instead of the otherwise ideal 2.25sec. knowing that we
	    already used a high average bit/pix value and this is a variable
	    bitrate seq. anyway and we do not feel very much reponsible for the
	    consequences of these assumptions. Also note that normally the ideal
	    video buffer length is 1.25sec and the reason for the other 1 sec is
	    for the maximum allowed lag time between audio and video data in the
	    multiplexed stream.
	 */
	 brate = (uint32)(nv->hsize *  nv->vsize * vdec->avrbitppix * 30) >> 3;
	 vbsz = 1.75 * brate;
      } 
      else
      {	 brate = nv->bitrate * 50;
	 vbsz = brate * 2.25;
      }
      
      if (vbsz > 1024*1024)
      {	 if (1024*1024.0 / brate > 1.5)
	    vbsz = 1024*1024;
	 else
	 {  vbsz = brate * 1.5;
	    /* We are allowing a maximum video buffer size of 4Mbytes! */
	    if (vbsz > 4*1024*1024) vbsz = 4*1024*1024;
	 }
      }
      vbsz = ((vbsz + 4095) / 4096) * 4096;
      if (vbsz>vdec->vbsz || vbsqsz>vdec->vbsqsz)
	 if ( adj_vidb(vbsz, vbsqsz) == 0) return(0);   
   }
   
   
   
   /* Establish what video buffer fullness level to use before starting
      to decode a picture. Since our picture decoder expects to find
      all the data associated with a picture sequentially available, it is
      important to get this right! In constant bitrate operation there is
      a fullness level defined in every picture header, and we will use that.
      The highest value which can be defined there is about 0.73 seconds of
      video bytes, which by definition we can definitely fit into our video
      buffer. For a variable bitrate stream, we have to define a safe
      level ourselves, and we do so by using the picture size and a 
      comfortable bit/pixel value which we can expect to encounter in
      unusually dense pictures (low compressed relative to others).
      Note that a large fullness level does not cause inefficiency in 
      the decoder engine, since we have additional heuristics down there...      
   */
   vdec->vbfullness =  (uint32)(nv->hsize *  nv->vsize * vdec->maxbitppix) >> 3;
   w = nv->vbv * 2048;	      /* See if the VBV size is less than that */
   if (w < 40960) w = 40960;  /* Don't trust too much to correctness of VBV value */
   if (w < vdec->vbfullness) vdec->vbfullness = w;
   /* Set the minimum fullness level to 40960 regardless... */
   if (vdec->vbfullness < 40960) vdec->vbfullness = 40960;
   
   if (nv->bitrate == 0x3ffff || ( gb->mpenv.options & OPT_IGNVBV) )
      vdec->state &= 0xffff ^ VD_USEVBV;
   else
   {  vdec->state |= VD_USEVBV;
      vdec->vbvmult = nv->bitrate/1800.0;
   } 
   
   w = vdec->bhsz * vdec->bvsz;
   q = ALIGN_PAGE((ubyte *)gb->mpenv.pictb + vdec->maxpict*256); /* Note the buffer zone */  
   for (k=0;k<3;k++)
   {  vdec->fb[k][0] = q;
      vdec->fb[k][1] = q + w;
      vdec->fb[k][2] = q + w + (w >> 2);
      q += w + (w >> 1);
   }
   vdec->dispm = (void *)ALIGN_PAGE(q);
   
   w = vdec->bhsz;
   vdec->denv.hsz = w;
   vdec->denv.hsza[0] = w;
   vdec->denv.hsza[1] = w;
   vdec->denv.hsza[2] = w;
   vdec->denv.hsza[3] = w;
   vdec->denv.hsza[4] = w >> 1;
   vdec->denv.hsza[5] = w >> 1;
      
   w = vdec->bhsz >> 4;
   for (k=0; k < (nint)(vdec->bvsz>>4); k++)
      for (m=0; m<w; m++)
      {  vdec->denv.lk_mbbase[k*w+m][0] = k*w;
	 vdec->denv.lk_mbbase[k*w+m][1] = m;
      }
      
   /* We are not concerned with speed here, so we are leaving the code
      below as is, to the mercy of the stupid compiler...
   */ 
   for (k=0;k<31;k++)
   for (m=0;m<6;m++)
   for (n=0;n<64;n++)
   {  w = (2*m+2) * (k+1) * nv->intra_qntmat[n];
      w >>=4;
      w -= (~w & 1); /* if even(w) w -= 1; */
      if (w > 2047) w = 2047;
      vdec->denv.lki_levd[k][m][n] = w;
      
      w = (2*m+3) * (k+1) * nv->inter_qntmat[n];
      w >>=4;
      w -= (~w & 1);
      if (w > 2047) w = 2047;
      vdec->denv.lkp_levd[k][m][n] = w;	       
   }   
   return(1);  
}


void
fwd_vseq(typ_mpgbl *gb, nint playtype)
{  register ubyte *d, *d1, *lastd;
   register uint16 vdecflags;
   register int32 space, ready, vpos, vneed, spent;
   register nint toggle, refstat, fbs;
   register int32  phigh, ptref, pdisp, pthis, pfut, vtrefbase;
   register float ptime;
   register double tf0, tf1;
   nint semid;
   int32 blksz;
   uint32 acquired, gopmrk, gopval, display;
   mp_pict tpict;
   typ_cntcmd cntcmd;
   float tolerance[4];
   mp_vseq tvseq;
   hrtime_t hrtm, vsystmbase;
   nint frmcnt = 0, error, nowait;
   typ_vdec *vdec = &(gb->vdec);
   typ_circb *vidb = &(gb->vidb);
   typ_dinf *dinf = &(gb->dinf);
   
   gb->mpenv.busy = BSY_FWDVSEQ;
   tpict.vseq = &vdec->strms[0].vseq;
   vdecflags = vdec->state;
   gopmrk = vdec->gopmrk;
   gopval = vdec->gopval;

   if (vdecflags & VD_BROKEN)
   {  pdisp  = pfut = ptref = 0;
      toggle = 1;
      refstat = 2;
      phigh  = -1;
      gopval = 0; /* There is a marker bit "1", in time code */
      vdecflags &= 0xffff ^ VD_SAFEMARKED;
   }
   else
   {  phigh = vdec->phigh;
      ptref = vdec->ptref;
      pfut  = vdec->pfut;
      pdisp = vdec->pdisp;
      refstat = vdec->refstat;   
      toggle  = vdec->toggle;
   }
   	
   /* Avoid inheriting outdated flags from previous session */
   vdecflags &= VD_SAFEMARKED | VD_USEVBV | VD_VSEQHDR | VD_LOOKVSEQ;
   
   
   if (playtype == PC_PLAY)
      vdec->speed = 1.0;
   else if (playtype == PC_FWDSTEP)
      vdecflags |= VD_STEP;
   else if (vdec->speed < 1.0)
      vdecflags |= VD_SLOW;
   
   display = 0xe000;	/* Just to force update of location bar at once */
   /* Will force display updating after every displayed picture in slow mode */
   if ( vdecflags & ( VD_SLOW | VD_STEP)) display |= DPY_SLOW;
   dinf->ptype = 0;   /* Start with a blanked picture type field in the display */

   semid = gb->mpenv.smph;
   
   if (vdecflags & VD_VSEQHDR)
   {  ptime = 1.0/look_picrate[vdec->strms[0].vseq.picrate];
      tolerance[B_PICT] = -vdec->tbr*ptime;
      tolerance[P_PICT] = -ptime;
      tolerance[I_PICT] = -ptime;
   }
   else refstat = 0;
   
   vsystmbase = gethrtime();
   
   /* Total bytes acquired from the DServer since the opening of the data source */
   acquired = vidb->tacq;
   ready = vidb->ready;
   blksz = gb->dsenv.blksz;
   
   d = vidb->rp;
   while (d < vidb->b1) d += blksz;
   spent = (d - vidb->b1) % blksz;   
   d = vidb->rp;
      
   for (;;)
   {  if (CMD_AVAIL(gb->mpx_env, gb->cmdq))
      {	 nint r0;
	 
	 if ( (r0 = process_command(gb->mpx_env, gb, BSY_FWDVSEQ) ) == 0) goto LB_Return;
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
	 }
      }
      vpos = vidb->b2 - d;
      space = ready;
      if (vpos < space) space = vpos;
      
      lastd = d;
      
      for (;;)
      {  uint32 r0;
	 
	 vneed = 4;
	 if (vneed > space) break;
	 /* Are we at a start code? */
	 if ( ( d[0]<<16 | d[1]<<8 | d[2] ) != 1) goto LB_Resync;
	 
	 r0 = d[3];
		 
	 if (r0 == ST_PICTURE) 	
	 {  uint32 phdr, mrkpict;
	 
	    /* Secure all the bytes in the picture header till the extension stuff */
	    if (space < 9)
	    {  vneed = 9;
	       break;
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
	    /* Increment the display update trigger count */
	    display += 1<<8;
	    
	    /* Skip this picture if reference picture(s) are missing */
	    if (refstat < (r0<<1))
	    {  d += 8;
	       space -= 8;
	       goto LB_Resync;
	    }
	    
	    if (vdecflags & VD_SYSTM)
	    {  /* Find the elapsed time since the last established real time base.
		  Scale this with the playback speed.
		  Find #pictures received since the last established real time base.
		  Calculate if it is too late to render this picture.
		  If we seem to be late more than 2 seconds, something is wrong, force
		  reestablishment of the time base after next time a picture is displayed
	       */
#ifdef WIN32
	       double t = gethrtime();
	       tf0 = (double)(t - vsystmbase) * 0.001;
#else
	       tf0 = (gethrtime() - vsystmbase) * 0.000000001;
#endif
	       tf0 *= vdec->speed;
	       tf1 = (pthis - vtrefbase) * ptime;
	       tf1 += tolerance[r0];
	       tf1 -= tf0;
	       if (tf1 < 0)
	       {  if (tf1 > -2.0) goto LB_Skiprender;
		  else vdecflags &= 0xffff ^ (VD_SYSTM | VD_DISPLAYED);
	       }
	    }
		    
	    if (vdecflags & VD_USEVBV)
	    {  vneed = ((phdr>>3) & 0xffff) * vdec->vbvmult;
	       if (vneed < 40*1024) vneed = 40960;
	    }
	    else vneed = vdec->vbfullness;
	    
	    if (space < vneed)
	    {  if (! (vdecflags & VD_SAFEMARKED))
	       {  if ( ready >= vpos || vidb->dsrcend )
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
		  else break;
	       }	       
	       if ( d >= vdec->safemark)
	       {  vdecflags &= 0xffff ^ VD_SAFEMARKED;
		  break;
	       }
	    }
	    vneed = 4;
	    
	    mrkpict = (d - lastd) + acquired - ready + gb->dsrc.ofs;
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
	       d = mp_rendpb(d, &tpict, &vdec->denv, RF_COLOR | RF_FILT);
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
		  r0 += 8;    /* Just to serve as a flag */
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
		  fbs = toggle;     /* May use a constant fbs here... */
	       }
	       else
	       {  refstat += 2;
		  pfut = pthis;
		  r0 += 8;
	       }
	    }     
	    
	    {  nint i = r0 & 0x3;
	       vdec->mrk[i].gopmrk = gopmrk;
	       vdec->mrk[i].gopval = gopval;
	       vdec->mrk[i].entry = mrkpict;
	       vdec->mrk[i].hdr = phdr;
	       if (r0 > 3)  break;  /* Or may go direct to rendering another */	       
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
	 
	    if (vdecflags & VD_SLOW)
	    {  for (;;)
	       {  hrtm = gethrtime();
#ifdef WIN32
		  tf0 = (double)(hrtm - vsystmbase) * 0.001;
#else
		  tf0 = (hrtm - vsystmbase) * 0.000000001;
#endif
		  if (tf0 < ptime/vdec->speed) apvnap(5);
		  else break;
	       }
	       vsystmbase = hrtm;
	    }
	    else if ( (vdecflags & VD_SYSTM) && (refstat > 4) )
	       for (;;)
	       {
#ifdef WIN32
		  tf0 = (double)(gethrtime() - vsystmbase) * 0.001;
#else
		  tf0 = (gethrtime() - vsystmbase) * 0.000000001;
#endif
		  tf0 *= vdec->speed;
		  tf1 = (pdisp - vtrefbase - vdec->td) * ptime - tf0;
		  if (tf1 > 0) 
		  {  if (tf1 > 4.0)
		     {	vdecflags &= 0xffff ^ (VD_SYSTM | VD_DISPLAYED);
			break;
		     }
		     apvnap(2);
		     /*mp_printf("nap: %7.3f\n", tf1);*/
		  }
		  else break;
	       }

	       
	   ccnv_disp(gb, vdec->fb[fbs][Y], vdec->fb[fbs][CR],vdec->fb[fbs][CB],
		     vdec->bhsz, vdec->bvsz, vdec->vdm, vdec->zoom);
	   frmcnt++;
	   vdec->fbs = fbs;
	   vdecflags |= VD_DISPLAYED;
	   display |= DPY_DISP;
	   break;	       
	 }	    
	 else if (r0 < 0xb0)	 /* Slice Start codes: [0x01 .. 0xAF] */  
	 {  d += 4;
	    space -= 4;
	 }  	    
	 else if (r0 == ST_GOP)
	 {  if (space < 8)
	    {  vneed = 8;
	       break;
	    }
	    ptref = phigh + 1;
	    r0 = d[4]<<24 | d[5]<<16 | d[6]<<8 | d[7];
	    if ((r0 & 0x60) == 0x20)    /*Broken Link but NOT Closed GOP */
	       if (refstat == 6) refstat = 5;
	    
	    gopmrk = gb->dsrc.ofs + acquired - ready + (d-lastd);
	    gopval = r0;
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
	       break;
	    }
	    /* Make a quick comparison with the previously received vseqhdr */
	    rt0 = d[4]<<24 | d[5]<<16 | d[6]<<8 | d[7];
	    rt1 = d[8]<<24 | d[9]<<16 | d[10]<<8 | d[11];
	    newvseq = rt0 != vdec->hdr1 || (rt1 | 0x3) != vdec->hdr2;
	    
	    tp = mp_rdvseqhdr(d+4, &tvseq);
	    
	    if ( ((vdecflags & (VD_VSEQHDR | VD_LOOKVSEQ)) == VD_VSEQHDR && newvseq)
		 || tp == 0)
	       goto LB_SkipVseqHdr;
	    
	    if ( ! newvseq && (vdecflags & (VD_VSEQHDR | VD_LOOKVSEQ)) == VD_VSEQHDR )
	       (void)proc_vseqhdr(gb, &tvseq,&vdec->strms[0].vseq,0);
	    else
	    {  if ( ! proc_vseqhdr(gb, &tvseq,NULL,1) ) goto LB_SkipVseqHdr;
	       vdec->hdr1 = rt0;
	       vdec->hdr2 = rt1 | 0x3;		  
	       vdec->strms[0].vseq = tvseq;	       
	       vdecflags |= VD_VSEQHDR;
	       vdecflags |= vdec->state & VD_USEVBV;
	       vdecflags &= 0xffff ^ (VD_SYSTM | VD_DISPLAYED | VD_SAFEMARKED);
	       pdisp  = pfut = ptref = 0;
	       toggle = 1;
	       refstat = 2;
	       phigh  = -1;	  
	       ptime = 1.0/look_picrate[tvseq.picrate];
	       tolerance[B_PICT] = -vdec->tbr*ptime;
	       tolerance[P_PICT] = -ptime;
	       tolerance[I_PICT] = -ptime;
	       /*	       set_video(gb, vdec->vdm,vdec->zoom);*/
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
	 
	 else if (r0 == ST_VSEQEND)
	 {  vdecflags |= VD_LOOKVSEQ;
	    space -= 4;
	    d += 4;
	    continue;
	 }
	 else d += 4;
	 
      LB_Resync:
	 {  ubyte *q;
	    int32 r2;
	    
	    /* Just a sanity check ! */
	    if (space < 0)
	    {  d += space - 3;
	       space = 3;
	       vneed = 4;
	       break;
	    }
	    q = d;
	    do do do ;
		  while (*d++);
	       while (*d);
	    while (d[1] != 1);
	    d -= 1; 
	    r2 = d - q;
	    if (r2+4 > space)
	    {  r2 = space - 3;
	       d = q + r2;
	       vneed = 4;
	       break;
	    }
	    else space -= r2;
	 }
      }
      
      /* Another sanity check */
      {	 int32 r0;
	 if (d > vidb->b2) d = vidb->b2;
	 r0 = d - lastd;
	 if (r0 > ready)
	 {  d = lastd + ready;
	    r0 = ready;
	 }
	 ready -= r0;
	 spent += r0;
	 vpos  -= r0;
      }
            
      /* We would rather put this block after the block in which  we claim
	 more data. But since we have the minute case here in which the
	 spent count is inreased, we are putting it here. What we loose is,
	 in the case where vneed> vpos && vneed>ready, if more data is acquired
	 and vneed<ready, this sequentiality process will require another trip down
	 here, because the condition will not have been detected first time
	 around... (A case which would never happen if Dserver is working ahead,
	 as it would in normal operation)
      */
      if ( vneed > vpos  &&  ready >= vpos )
      {	 if (vpos > vdec->vbsqsz)
	 {  /* Sequentiality area is not big enough for the transfer!,
	       we have to waste some of the data... */
	    uint32 waste = vpos - vdec->vbsqsz;
	    spent += waste;
	    ready -= waste;
	    vpos = vdec->vbsqsz;
	 }
	 lastd = vidb->b1 - vpos;
	 (void) memcpy(lastd, vidb->b2 - vpos, vpos);
	 d = lastd;
	 vdecflags &= 0xffff ^ VD_SAFEMARKED;
      }
      
      while (spent >= blksz)
      {  while ((error = SEMA_POST(gb->mpx_env, &gb->dssync.decr)) != 0)
	    if (error == EINTR) continue;
	    else ;
	 spent -= blksz;
      }
            
      if (vneed > ready)
      {	 if ( vidb->dsrcend ) 
	 {  send_stats(gb, 1);  /* Signal the ctrl client we are done */
	    break;
	 }
	 
	 nowait = FALSE; 	/* Set blocking semaphore opr. */
	 for (;;)
	 {  /* Avoid unnecessary or hopeless blocking waits */
	    if (vneed <= ready || gb->dsenv.syncrd_state == DSRCEND) 
	       nowait = TRUE;
	    
	    if ((nowait && (error = SEMA_TRYWAIT(gb->mpx_env, &gb->dssync.incr)) != 0) ||
		(!nowait && (error = SEMA_WAIT(gb->mpx_env, &gb->dssync.incr)) != 0))
	    {  if (error == EBUSY)  
	       {  /* Semaphore op. would otherwise block. Which means that in a
		     previous loop we already acquired at least what we needed, and
		     turned on the IPC_NOWAIT flag. We will avoid working below
		     marginal conditions though by doing a fullness level check
		  */  	       
		  if (ready < (vidb->full>>2) && gb->dsenv.syncrd_state != DSRCEND)
		  {  /* As long as we are not at the end of the data source,
			wait till fullnesslevel is reached */
		     apvnap(100);
		     vdecflags &= 0xffff ^ VD_SYSTM;
		     continue;
		  }
		  else break;
	       }
	       /* Semaphore operation might have been interrupted do to User 
		  Interface generated signal. So rather than continuing the loop,
		  we will break out to give UI polling a chance.
		  "errno == 0" is to guard against  wierd situations which we
		  have encountered, where semop returns -1 with no error...
	       */		  
	       else if (error == EINTR)  break;
	       /* Something really went wrong with semop!! */
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
	       vidb->dsrcend = TRUE;
	    }
	 }
      }
      

      /*
      The display area may be  updated if a VSEQHDR has been received, if the player
      is in slow motion or step mode, or more than 15 pictures have been received
      since the last update. If the data source is a sequential data source though,
      location bar will always be shown as full, and display update will not be
      triggered based on the #pictures received.
      Note that : for every new picture received display += (1 << 8)
      */
      if ( display & (0xe000 | DPY_VSEQ | DPY_SLOW))
      {	 nint update = 0;
	 
	 /* Update the location bar only if at least one new picture has been
	    received, and the data source is not sequential.
	 */
	 if ( /*(display & 0xe000) && */ !(gb->dsrc.type & DSRC_FWDONLY) )
	 {  dinf->loc = (float)(acquired - ready + gb->dsrc.ofs) / gb->dsrc.size;
	    update |= UPD_BAR;
	 }

	 /* If there is a new displayed picture, display info about it */
	 if (display & DPY_DISP)
	 {  uint32 phdr = vdec->mrk[fbs].hdr;
	    uint32 gop  = vdec->mrk[fbs].gopval;
	    if (display & DPY_SLOW) dinf->ptype = (phdr>>19) & 0x7;	    
	    
	    /* Since we allow I picture random entries, a picture might not
	       have an associated  GOP .
	    */
	    if (gop)	/* Do we really have a valid gop ? */
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
	 
	 /* Need to display info about a new video sequence ? */	 
	 if (display & DPY_VSEQ)
	 {
	    update |= UPD_VSTR | UPD_BAR;
	 }
	 /* Clear all the display flags except the SLOW playmode flag which is permanent */ 
	 display = display & DPY_SLOW;    
	 if ( update ) send_stats(gb, 0);
      }
      
      /* Break out after a displayed pictured in step mode. */     
      if ((vdecflags & (VD_STEP | VD_DISPLAYED)) == (VD_STEP | VD_DISPLAYED))
	 break;
      
      /* Establishe the system time base if there is not one, only in normal
	 play mode.
      */
      if ( (vdecflags & (VD_SYSTM | VD_SLOW | VD_STEP| VD_DISPLAYED)) == VD_DISPLAYED)
      {	 vsystmbase = gethrtime();
	 vtrefbase = pdisp;
	 vdecflags |= VD_SYSTM;
      }
   }

LB_Return:   
   vdec->lastplaytype = playtype;
   vdec->state = vdecflags;
   vdec->phigh = phigh;
   vdec->ptref = ptref;
   vdec->pfut  = pfut;
   vdec->pdisp = pdisp;
   vdec->refstat = refstat;
   vdec->toggle  = toggle;
   vdec->gopmrk = gopmrk;
   vdec->gopval = gopval;
   
   vidb->rp = d;
   vidb->ready = ready;
   vidb->tacq = acquired; 
   gb->mpenv.busy = BSY_IDLE;
}   

