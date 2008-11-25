/*
 * @(#)mp_rpb.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include "mp_mpd.h"

/* Get N (where N<=16) bits from the data stream right aligned into zzb
 * So the last of the received bits will be the least significant bit
 * in zzb.
 * GETNEXT9(N) is to be used when N<=9
 * GETNEXT16(N)is to be used when 9<N<=16
 * Both routines read as many bytes as they can from the code stream
 * if #remaining bits in zza is less than the #bits asked for, e.g for 
 * GETNEXT9(N), 3bytes are read for present and future use.
 *
 * GETNEXTS(N) is to be used when the read bits are known not to be
 * going to be put back. ( S stands for safe reading...)
 * IMPORTANT: note that the read into zzb is not a clean read, and it
 * should further be masked with the knowledge of valid bits in zzb.
 * This cleaning is not done here for purposes of efficiency. 
 */

#define   GETNEXT9(N)	{   	 \
   if ( (zzrmbits-=(N)) < 0)	 \
   {  zzrmbits += 24;    	 \
      zza <<= 24;   	      	 \
      zza |= zzp[0]<<16 | zzp[1]<<8 | zzp[2];	\
      zzp +=3; 	     	      	 \
   }				 \
   zzb = zza >> zzrmbits;  }
   
#define   GETNEXT16(N)	{	 \
   if ((zzrmbits-=(N)) < 0)	 \
   {  zzrmbits += 16;    	 \
      zza <<= 16;   	      	 \
      zza |= zzp[0]<<8 | zzp[1]; \
      zzp += 2; 	     	 \
   }				 \
   zzb = zza >> zzrmbits;  }

#define   GETNEXTS(N)	{     \
   zzb = 0;		      \
   if ( (zzrmbits-=(N)) < 0)  \
   {  zzb = 0 - zzrmbits;     \
      zzb = zza << zzb;	      \
      zzrmbits += 32;	      \
      zza = zzp[0]<<24 | zzp[1]<<16 | zzp[2]<<8 | zzp[3];  \
      zzp += 4;		      \
   }			      \
   zzb |= zza >> zzrmbits;  }


#define _USE_LION_
#ifdef _USE_LION_
extern void mp_printf(char *, ...);
#endif


/***************************************************************************
   Render a P or B picture.
***************************************************************************/
ubyte *
mp_rendpb(ubyte *zzp, mp_pict *pict, mp_denv *denv, uint32 flags)
{  register uint32 zza,zzb,w;
   register int zzrmbits;
   register nuint scale;
   register uint16 (*base)[64];
   register int32 *tcf;
   register ubyte *qntmat;
   register nint r2,r3,r4,nblk;
   nuint mb_width;
   nint prev_mbadr, prev_lmdc, prev_chrdc[2];
   nint prev_hf, prev_vf, prev_hb, prev_vb, prev_intradr, prev_mbtype;
   nint maxmb, vsize, ptype;
   mp_macroblk mb;
   ubyte *fbp[6];
   uint32 bl_patterns[8];
   nint badflag = 0;

   zzrmbits = 0;  /* Reset the remaining #bits in the bit buffer */
   mb_width = pict->vseq->mbwidth;
   prev_mbadr = -1; 
   maxmb = pict->vseq->maxmb;
   vsize = ((pict->vseq->vsize+15) >> 4) << 4;
   mb.flags = flags;
   ptype = (pict->ptype-1) << 6;

L_Synchronize:
   if (badflag)
   {  badflag = 0;
      mp_printf("mpx: bad data : rb\n");
   }
   /* Backup by the # bytes in the bit buffer.
      Note that start codes are byte aligned, so the truncation works fine.
   */   
   zzp -= zzrmbits >> 3;
   zzrmbits = 0;

   w = zzp[0]<<16 | zzp[1]<<8 | zzp[2];	  /* Read the next 3 bytes */
   zzp += 3;
   while (w != 1)    /* Search for 0x000001, a start code      */
      w = ((w&0xffff) << 8) | *zzp++;
   w = *zzp++;	     /* Read the last byte of the start code   */
   
   /* A valid slice start code is [1,0xaf] */
   if ( w  &&  w<=MAXSLICE) goto L_SliceStart;
   
   /* If this is not a valid slice start code, then backup the data pointer
      to the beginning of the start code and return, since this normally
      means that we have reached the end of the  current picture. A slice
      start code is the only start code that can exist before the end of the
      picture. 
   */
   else return(zzp-4); 
  

L_SliceStart:  
   prev_hf = 0;
   prev_vf = 0;
   prev_hb = 0;
   prev_vb = 0; 
   mb.adr = (nint)((w - 1) * mb_width) - 1;
   prev_intradr = -2;
   prev_mbtype = 0;
   
   /* read the quantizer_scale (5bits) and the extra_bit_slice(1bit)*/
   GETNEXTS(6);
   zzb &= 0x3f;
   scale = zzb >> 1;
   scale |= scale == 0;	   /* Force to nonzero */
   /* Throw away all the extra_bit_slice information  */
   while (zzb & 1) GETNEXTS(9);
      

L_MacroBlockStart:
   do			/*Skip all the stuffing bits */
   {  GETNEXT16(11);
      zzb &= 0x7ff;
   }  while (zzb == 0xf);
    
   while(zzb == 0x8)	/* Look for macroblock_escape(s) */  
   {  mb.adr += 33;
      GETNEXT16(11);
      zzb &= 0x7ff;
   }
   
   /* D e c o d e  M a c r o b l o c k   A d d r e s s   I n c r e m e n t */
   w = lk_mbadr1[zzb>>6];
   if (w)
   {  mb.adr += w & 0xf;
      zzrmbits += w >> 4;
   }
   else
   {  w = lk_mbadr2[zzb];  /* bits 7-10 in zzb known to be 0 */
      if (w)
      {	 mb.adr += 8 + (w & 0x1f);
	 zzrmbits += w >> 5;
      }
      else 
      {	 zzrmbits += 11;
	 goto L_Synchronize;  /* Start of a new slice */
      }
   }
   
   if (mb.adr > maxmb) {  badflag = 1; goto L_Synchronize; }
   
   r3 = mb.adr - prev_mbadr - 1;
   if (r3 < 0) {  badflag = 1; goto L_Synchronize; }
   
   if (pict->ptype == P_PICT)
   {  /* Render all the skipped macroblocks, if any */ 
      if (r3)
      {	 prev_hf = 0;
      	 prev_vf = 0;
	 skipmb(prev_mbadr + 1, r3, denv);	 
      }
      else if (!(prev_mbtype & MB_FWD) )
      {	 prev_hf = 0;
      	 prev_vf = 0;
      }
   }
   else	       /* B_picture */
   {  if (prev_mbtype & MB_INTRA)
      {	 prev_hf = 0;
      	 prev_vf = 0;
      	 prev_hb = 0;
      	 prev_vb = 0;
      }
      /* Render all the skipped macroblocks, if any */ 
      if (r3)
      {	 mb.adr = prev_mbadr + 1;
	 mb.cbp = 0;
	 do
	 {  rendmb(&mb, denv, 0);
      	    mb.adr += 1;
	    r3 -= 1;
	 } while (r3);
      }
   }   
   
   /* D e c o d e  M a c r o b l o c k   T y p e  &   
      Q u a n t i z e r   S c a l e 
   */
   {  register nint r0;
      
      GETNEXT16(11); 
      w = (zzb >> 5) & 0x3f;
      mb.type = dvlc_mbtype[ptype | w];
      r0 = mb.type & 0x7;	 /* 6 - size of mb type */
      zzrmbits += r0 + 5;
      if (mb.type & MB_QUANT) 
      {  zzb >>= r0;
	 scale = zzb & 0x1f;
	 scale |= scale == 0;	 /* Force to nonzero */	 
	 zzrmbits -= 5; 
      }
   }
   
   prev_mbadr = mb.adr;
   prev_mbtype = mb.type;
   
   if (mb.type & MB_INTRA)
      goto L_StartIntraBlockDecode;

   /*Initialize flags to no filtering */
   r3 = 0;     /* luminance filtering flags */
   r4 = 0;     /* chrominance filtering flags */
   /* Chroma filtering is currently disabled, which would have been 
      calculated in r4
   */
   
   /* Differential motion vector value is coded in two parts.
      First part, the motion VLC code is the signed quotient part,
      and the second part is the unsigned remainder part which is
      "rsize" bits long.
      Thus the differential value = (VLC<<rsize) + remainder
      Maximum motion vector value = (16 << rsize) - 1
      Minumum motion vector value = (-16 << rsize)
      rsize = [0..6]
      Note that, as an example, for rsize=0, motion_vector = [-16..15]
      and the diff_motion_vect = [-31..31], the differential values
      are normalized to range [-16..16] by adding or substracting 32<<rsize
      
      So if Previous_motion_vect + diff_motion_value > MaxValue,
	 we substract 32<<rsize.
	 if Previous_motion_vect + diff_motion_value < MinValue,
	 we add 32<<rsize.
      Thus, in effect, VLC code for 2, actually corresponds to 2 or -30
      
      rsize is such that all the diff_motion_values in the picture is
      within [-((32<<rsize)-1) .. (32<<rsize)-1]
   */
   
   /* D e c o d e  F o r w a r d   M o t i o n  V e c t o r s  */
   if (mb.type & MB_FWD)
   {  register nint rsize   = pict->fwd_rsize;
      register nint fullpel = pict->fullpel_fwd;
      register nint maxmv = 16 << rsize;  /* Maximum motion vector value + 1  */
      register nint minmv = -maxmv;	  /* Minimum motion vector value      */
      register nint r0;
    
      /* Decode Forward H o r i z o n t a l  motion code   */
      GETNEXT16(11);
      if (zzb & 0x400)
      {  zzrmbits += 10;
	 r0 = prev_hf;
      } 
      else
      {  /* Since at this point we know that the most significant bit
	    of the motion VLC code is 0, disgarding the least significant 
	    bit of the motion VLC code, the sign bit, we will only decode
	    the 9 bits out of the 11 maximum length motion VLC code
	    
	    Contents of the motion VLC code decoding lookup table is:
	    cccc.rrrr
	    where:   cccc -> abs(motion code) - 1
		     rrrr -> 11 - size of the VLC, which is also equal to the
			     position of the sign bit			     
	 */
	 register nint r1 = dvlc_mv[(zzb>>1) & 0x1ff];
	 r0 = ((r1 >> 4) << rsize ) + 1;
	 r1 = r1 & 0xf;	      /* unused # bits	*/
	 zzrmbits += r1;
	 r1 = zzb & (1 << r1);
	 if (rsize)	     	      
	 {  /* A remainder for the motion vector difference is also coded */
	    GETNEXT9(rsize);
	    r0 += zzb & ~(-1 << rsize);
	 }
	 if (r1)	      /* Negative motion code */
	 {  r0 = prev_hf - r0;
	    if (r0 < minmv) r0 += maxmv << 1;
	 }
	 else		      /* Positive motion code */
	 {  r0 = prev_hf + r0;
	    if (r0 >= maxmv) r0 -= maxmv << 1;  
	 }
	 prev_hf = r0;
      }   
      if (fullpel)
      {  mb.fwd_lhmv = r0; 
         mb.fwd_chmv = r0 >> 1; 
	 /*r4 |= (r0 & 1) << 3;*/
      }
      else 
      {  r3 |= (r0 & 1) << 3; 
	 mb.fwd_lhmv = r0 >> 1;
	 if (r0 > 0)	      /* If motion vector is positive */
	 {  mb.fwd_chmv = r0 >> 2;
	    /*r4 |= (r0 & 2) << 2;*/
	 }
	 else
	 {  /* we are implementing (r0/2)>>1 where "/" is division with
	       truncation towards zero, and ">>" is signed right shift
	       as specified in MPEG clause 2.4.4.2
	    */
	    mb.fwd_chmv = -(-r0>>1) >> 1;
	    /*r4 |= (-(-r0 >> 1) - (mb.fwd_chmv << 1)) << 3;*/
	 }
      }      
      
      /* Decode Forward  V e r t i c a l  Motion code */
      GETNEXT16(11);
      if (zzb & 0x400)
      {  zzrmbits += 10;
	 r0 = prev_vf;
      } 
      else
      {  register nint r1 = dvlc_mv[(zzb>>1) & 0x1ff];
	 r0 = ((r1 >> 4) << rsize ) + 1;
	 r1 = r1 & 0xf;
	 zzrmbits += r1;
	 r1 = zzb & (1 << r1);
	 if (rsize)
	 {  GETNEXT9(rsize);
	    r0 += zzb & ~(-1 << rsize);
	 }
	 if (r1)
	 {  r0 = prev_vf - r0;
	    if (r0 < minmv) r0 += maxmv << 1;
	 }
	 else
	 {  r0 = prev_vf + r0;
	    if (r0 >= maxmv) r0 -= maxmv << 1;  
	 }
	 prev_vf = r0;
      }   
      if (fullpel)
      {  mb.fwd_lvmv = r0; 
         mb.fwd_cvmv = r0 >> 1; 
	 /*r4 |= (r0 & 1) << 2;*/
      }
      else 
      {  r3 |= (r0 & 1) << 2; 
	 mb.fwd_lvmv = r0 >> 1;
	 if (r0 > 0) 
	 {  mb.fwd_cvmv = r0 >> 2;
	    /*r4 |= (r0 & 2) << 1;*/
	 }
	 else
	 {  mb.fwd_cvmv = -(-r0>>1) >> 1;
	    /*r4 |= (-(-r0 >> 1) - (mb.fwd_cvmv << 1)) << 2;*/
	 }
      }
      
      /* This is to detect junk data */
      r0  = mb.fwd_lvmv < -vsize;
      r0 |= (mb.fwd_lvmv > vsize);
      if ( r0 ) {  badflag = 1; goto L_Synchronize; }
   }
      
   /* D e c o d e  B a c k w a r d    M o t i o n  V e c t o r s  */
   if (mb.type & MB_BWD)
   {  register nint rsize = pict->bwd_rsize;
      register nint fullpel = pict->fullpel_bwd;
      register nint maxmv = 16 << rsize;
      register nint minmv = -maxmv;
      register nint r0;
     
      /* Decode Backward H o r i z o n t a l  motion code   */
      GETNEXT16(11);
      if (zzb & 0x400)
      {  zzrmbits += 10;
	 r0 = prev_hb;
      } 
      else
      {  register nint r1 = dvlc_mv[(zzb>>1) & 0x1ff];
	 r0 = ((r1 >> 4) << rsize ) + 1;
	 r1 = r1 & 0xf;
	 zzrmbits += r1;
	 r1 = zzb & (1 << r1);
	 if (rsize)
	 {  GETNEXT9(rsize);
	    r0 += zzb & ~(-1 << rsize);
	 }
	 if (r1)
	 {  r0 = prev_hb - r0;
	    if (r0 < minmv) r0 += maxmv << 1;
	 }
	 else
	 {  r0 = prev_hb + r0;
	    if (r0 >= maxmv) r0 -= maxmv << 1;  
	 }
	 prev_hb = r0;
      }   
      if (fullpel)
      {  mb.bwd_lhmv = r0; 
         mb.bwd_chmv = r0 >> 1; 
	 /*r4 |= (r0 & 1) << 1;*/
      }
      else 
      {  r3 |= (r0 & 1) << 1; 
	 mb.bwd_lhmv = r0 >> 1;
	 if (r0 > 0) 
	 {  mb.bwd_chmv = r0 >> 2;
	    /*r4 |= r0 & 2;*/
	 }
	 else
	 {  mb.bwd_chmv = -(-r0>>1) >> 1; 
	    /*r4 |= (-(-r0 >> 1) - (mb.bwd_chmv << 1)) << 1;*/
	 }
      }
            
      /* Decode Backward  V e r t i c a l  Motion code	 */
      GETNEXT16(11);
      if (zzb & 0x400)
      {  zzrmbits += 10;
	 r0 = prev_vb;
      } 
      else
      {  register nint r1 = dvlc_mv[(zzb>>1) & 0x1ff];
	 r0 = ((r1 >> 4) << rsize ) + 1;
	 r1 = r1 & 0xf;
	 zzrmbits += r1;
	 r1 = zzb & (1 << r1);
	 if (rsize)
	 {  GETNEXT9(rsize);
	    r0 += zzb & ~(-1 << rsize);
	 }
	 if (r1)
	 {  r0 = prev_vb - r0;
	    if (r0 < minmv) r0 += maxmv << 1;
	 }
	 else
	 {  r0 = prev_vb + r0;
	    if (r0 >= maxmv) r0 -= maxmv << 1;  
	 }
	 prev_vb = r0;
      }   
      if (fullpel)
      {  mb.bwd_lvmv = r0; 
         mb.bwd_cvmv = r0 >> 1; 
	 /*r4 |= r0 & 1;*/
      }
      else 
      {  r3 |= r0 & 1; 
	 mb.bwd_lvmv = r0 >> 1;
	 if (r0 > 0) 
	 {  mb.bwd_cvmv = r0 >> 2;
	    /*r4 |= (r0 & 2) >> 1;*/
	 }
	 else
	 {  mb.bwd_cvmv = -(-r0 >> 1) >> 1; 
	    /*r4 |= -(-r0>>1) - (mb.bwd_cvmv << 1);*/
	 }
      }

      r0  = mb.bwd_lvmv < -vsize;
      r0 |= (mb.bwd_lvmv > vsize);
      if ( r0 ) {  badflag = 1; goto L_Synchronize; }
   } 
   mb.lmfilt = r3;
   mb.chfilt = 0; /* Disabled chroma filtering */
   

   /* D e c o d e   C o d e d   B l o c k   P a t t e r n */
   if (mb.type & MB_CBP)
   {  GETNEXT16(9);
      mb.cbp = dvlc_cbp[zzb & 0x1ff];
      zzrmbits += 9 - lvlc_cbp[mb.cbp];
   }
   else 
   {  mb.cbp = 0;
      rendmb(&mb,denv,0);
      goto L_MacroBlockStart; /* Make a quit exit */
   }
      
   /* D e c o d e  D C T  c o e f f i c i e n t s */
      
   /* Start Inter Block Decode */
   r4 = one_count[mb.cbp];
   
   base = (uint16 (*) [64]) ( (ubyte*)denv->lkp_levd + (scale-1)*(6*64*2) );
   qntmat = pict->vseq->inter_qntmat;
   r2 = 1;  /* factor to be added to 2*level */
   goto L_DecodeBlocks;

L_StartIntraBlockDecode:
   {  register nint  r0 = denv->lk_mbbase[mb.adr][0];
      register nint  r1 = denv->lk_mbbase[mb.adr][1];
      register ubyte *q;
      if (mb.adr - prev_intradr > 1)
      {  prev_lmdc = 128*8;
	 prev_chrdc[0] = 128*8;
	 prev_chrdc[1] = 128*8;
      }
      prev_intradr = mb.adr;
      
      fbp[0] = q = denv->fp[CURRENT][Y] + (r0<<8) + (r1<<4);
      fbp[1] = q + 8;
      fbp[2] = q = q + (mb_width << 7);
      fbp[3] = q + 8;
      r3 = 0;
      r0 = (r0<<6) + (r1<<3);
      fbp[4] = denv->fp[CURRENT][CB] + r0;
      fbp[5] = denv->fp[CURRENT][CR] + r0;
   }
   base = (uint16 (*) [64]) ( (ubyte*)denv->lki_levd + (scale-1)*(6*64*2) );
   qntmat = pict->vseq->intra_qntmat;
   r2 = 0;  /* factor to be added to 2*level */
   r4 = 6;

L_DecodeBlocks:   
   tcf = (int32 *)denv->tcfb;
   nblk = 0;

   for (;;)
   {  register nint ps, ps0;
      register uint32 blkpat;
      
      if ( mb.type & MB_INTRA ) 
	 if (r4 > 0)
	 {  r4--;
	    if (r4 > 1)
	    {  register nint r0,r1;
	       /* Luminance Blocks */
	       GETNEXT16(15);
	       w = zzb >> 8;
	       w &= 0x7f;
	       r0 = lk_lmdc[w];
	       r1 = r0 & 0x7;
	       r0 >>= 3;
	       if (r1) zzrmbits += r1 + 7;
	       else
	       {  zzrmbits += r0;
		  zzb >>= r0;
		  r0 = 8 - (r0 >> 1);
		  zzb &= ~(-1 << r0);
		  r0 = 1 << (r0 - 1);
		  if ( zzb & r0)  r0 = zzb;
		  else r0 = 1 + zzb - (r0 << 1);
	       }	 
	       prev_lmdc += r0 << 3;
	       tcf[0] = prev_lmdc;
	    }
	    else
	    {  register nint r0,r1;
	       /* Chrominance Blocks */
	       GETNEXT16(16);
	       w = zzb >> 9;
	       w &= 0x7f;
	       r0 = lk_chrdc[w];
	       r1 = r0 & 0x7;
	       r0 >>= 3;
	       if (r1) zzrmbits += r1 + 8;
	       else
	       {  zzrmbits += r0;
		  zzb >>= r0;
		  r0 = 8 - (r0 >> 1);
		  zzb &= ~(-1 << r0);
		  r0 = 1 << (r0 - 1);
		  if ( zzb & r0)  r0 = zzb;
		  else r0 = 1 + zzb - (r0 << 1);
	       }	 
	       prev_chrdc[r4] += r0 << 3;
	       tcf[0] = prev_chrdc[r4];
	    }
	    ps = 0;
	 }
	 else
	 {  shidct(bl_patterns,(int32 *)denv->tcfb, denv->hsza, fbp, 0, 0, nblk);
	    goto L_MacroBlockStart;
	 }
      else if (r4)
      {  /* First decode the First_Coeff */
	 r4--;
	 GETNEXT9(2);   
	 if (zzb & 2)
	 {  register nint r1, r0 = 3 * scale * qntmat[0] >> 4;
	    r0 -= ~r0 & 1;
	    r1 = zzb & 1;
	    tcf[0] = (r0 ^ -r1) + r1;
	    ps = 0;
	 }  
	 else
	 {  zzrmbits += 2;
	    ps = -1;
	 }
      }
      else
      {	 rendmb(&mb,denv,bl_patterns);
	 goto L_MacroBlockStart;
      }
   
      blkpat = 0;
      for (;;)	  /* Decode block's transform coefficients */
      {	 register nint cf, cf0;
      
	 GETNEXT9(9);
	 zzb &= 0x1ff;
	 switch (jmp_ac9dec[zzb]) {
      
#include "mp_acjmp.h"  
      
	 case 152:	  /* "0000001xx" */
	    zzrmbits += 2;
	    GETNEXTS(4);
	    zzb &= 0xf;
	    cf = zzb >> 1;
	    ps += runlev0[cf][0];
	    cf = runlev0[cf][1];
	    cf += r2;
	    cf *= scale;
	    cf *= qntmat[ps];
	    cf >>= 4;
	    /* cf += (cf & 1) - 1;	 Odd/Even test */
	    /* Cutting a little corner, clipping to -2047 instead of -2048 */
	    /* if (cf > 2047) cf = 2047; */  	    
	    /* Sign test without an if! :  "if (zzb & 1) cf = -cf" */
	    cf0 = zzb & 1;
	    cf = (cf ^ -cf0) + cf0;
	    goto L_SingleCoef;
	    
	 case 153:	  /* "00000001x" */
	    zzrmbits += 1;
	    GETNEXTS(5);
	    zzb &= 0x1f;
	    cf = zzb >> 1;
	    ps += runlev1[cf][0];
	    cf = runlev1[cf][1];
	    cf += r2;
	    cf *= scale;
	    cf *= qntmat[ps];
	    cf >>= 4;
	    /* cf += (cf & 1) - 1;	  */
	    /* if (cf > 2047) cf = 2047;  */
	    cf0 = zzb & 1;
	    cf = (cf ^ -cf0) + cf0;
	    goto L_SingleCoef;
	    
	 case 154:	  /* "000000001" */
	    GETNEXTS(5);
	    zzb &= 0x1f;
	    cf = zzb >> 1;
	    ps += runlev2[cf][0];
	    cf = runlev2[cf][1];
	    cf += r2;
	    cf *= scale;
	    cf *= qntmat[ps];
	    cf >>= 4;
	    /* cf += (cf & 1) - 1;	  */
	    /* if (cf > 2047) cf = 2047;  */
	    cf0 = zzb & 1;
	    cf = (cf ^ -cf0) + cf0;
	    goto L_SingleCoef;
	    
	 case 155:	  /* "000000000" */
	    GETNEXT9(8);
	    zzb &= 0xff;
	    if (zzb > 127)	   
	    {  zzrmbits +=2;
	       cf = (zzb>>3) & 0xf;
	       ps += 1;		 /* run is always 0 (+1) for these VLCs	   */
	       cf = 31 - cf;	 /* level = 31-xxxx for these VLCs	   */
	       cf <<=1;		 /* unlike the implicit 2*level in the
				    lookup tables, *2 should be done here  */
	       cf += r2;
	       cf *= scale;
	       cf *= qntmat[ps];
	       cf >>= 4;
	       /* cf += (cf & 1) - 1;	     */
	       /* if (cf > 2047) cf = 2047;  */  
	       if (zzb & 4) cf = -cf;	    
	       goto L_SingleCoef;
	    }
	    if (zzb > 63)	   
	    {  zzrmbits +=1;
	       cf = (zzb>>2) & 0xf;
	       ps += runlev4[cf][0];
	       cf = runlev4[cf][1];
	       cf += r2;
	       cf *= scale;
	       cf *= qntmat[ps];
	       cf >>= 4;
	       /* cf += (cf & 1) - 1;	     */
	       /* if (cf > 2047) cf = 2047;  */
	       if (zzb & 2) cf = -cf;	    
	       goto L_SingleCoef;
	    }
	    if (zzb > 31)	   
	    {  cf = (zzb>>1) & 0xf;
	       ps += runlev5[cf][0];
	       cf = runlev5[cf][1];
	       cf += r2;
	       cf *= scale;
	       cf *= qntmat[ps];
	       cf >>= 4;
	       /* cf += (cf & 1) - 1;	     */
	       /* if (cf > 2047) cf = 2047;  */
	       cf0 = zzb & 1;
	       cf = (cf ^ -cf0) + cf0;
	       goto L_SingleCoef;
	    }
	    badflag = 1;
	    goto L_Synchronize;	    /* Bad Code !!!*/
	    
	 case 156:	  /* EOB */
	    zzrmbits += 7;
	    goto L_EOB_Return;
	 
	 case 157:	  /* ESC */
	    zzrmbits += 3;	   /* 9bits - 6bit_ESC code = 3unused bits */
	    GETNEXTS(14);	   /* run(6bits) + level(8bits) */
	    ps += 1+ ((zzb >> 8) & 0x3f);
	    zzb &= 0xff;
	    
	    /* Note the trick used for negating the resultant transform coef.
	       without using an if statement. -a = (a ^ -1) + 1
	       Two's complement arithmetic...
	    */
	    ps0 = 0;	   /* sign flag, positive  */
	    if (!zzb)	   /* 128<= level <=255	   */
	    {  GETNEXTS(8);   
	       cf = zzb & 0xff;
	    }
	    else if (zzb == 128) /* -255 <= level <= -128*/
	    {  GETNEXTS(8);
	       cf = 256 - (zzb&0xff);
	       ps0 = -1;	 /* sign flag, negative	 */
	    }
	    else if (zzb & 0x80) /* -127 <= level < 0	 */ 
	    {  ps0 = -1; 
	       cf = 256 - zzb; 
	    }
	    else cf  = zzb;	 /* 0 < level <= 127  */
	    cf <<= 1;
	    cf += r2;
	    cf *= scale;
	    cf *= qntmat[ps];
	    cf >>= 4;
	    /* cf += (cf & 1) - 1;	  */
	    /* if (cf > 2047) cf = 2047;  */
	    cf = (ps0 ^ cf) - ps0;
	    goto L_SingleCoef;   
	 }	      
      L_SingleCoef:
	 blkpat |= pattern[ps];
	 tcf[dzz[ps]] = cf;
	 continue;
	 
      L_DoubleCoef:
	 blkpat |= pattern[ps];
	 blkpat |= pattern[ps0];
	 tcf[dzz[ps0]] = cf0;
	 tcf[dzz[ps]]  = cf;
	 continue;
      
      L_DoubleCoefEOB:
	 blkpat |= pattern[ps];
	 blkpat |= pattern[ps0];
	 tcf[dzz[ps0]] = cf0;
	 tcf[dzz[ps]]  = cf;
	 bl_patterns[nblk] = blkpat;
	 nblk += 1;
	 tcf += 64;
	 break;;
      
      L_SingleCoefEOB:
	 blkpat |= pattern[ps];
	 tcf[dzz[ps]] = cf;   
      
      L_EOB_Return:
	 bl_patterns[nblk] = blkpat;
	 nblk += 1;
	 tcf += 64;
	 break;	  
      }
   }   
   goto L_MacroBlockStart;
}
