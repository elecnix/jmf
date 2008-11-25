/*
 * @(#)mp_ri.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <varargs.h>
#include "mp_mpd.h"


/* Get N bits from the data stream right aligned into zzb
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
 * More than 16bits  can be read with GETNEXTS(N)
 * IMPORTANT: note that the read into zzb is not a clean read, and it
 * should further be masked with the knowledge of valid bits in zzb.
 * This cleaning is not done here for purposes of efficiency.
 *
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


/* Ivan */
/* The real mp_printf is defined in lion/ln_main.c */
#define _USE_LION_
#ifdef _USE_LION_
extern void mp_printf(char *, ...);
#else
void 
mp_printf(char *fmt, ...)
{  va_list ap;
   va_start(ap);
   vprintf(fmt, ap);
   va_end(ap);
}
#endif


/************************************************************************** 
   Render an Intra Picture  
**************************************************************************/  
ubyte *
mp_rendi(ubyte *zzp, mp_pict *pict, mp_denv *denv, uint32 flags )
{  register uint32 zza,zzb,w;
   register nint zzrmbits,mbadr;
   register uint16 (*base)[64];
   register int32 *tcf;
   register nint nblk;
   register nint r2; 
   register nuint scale;
   nint  maxmb, prev_lmdc, prev_chrdc[2];
   nuint mb_width;
   ubyte *qntmat, *fbp[6];
   uint32 bl_patterns[6];
   nint badflag = 0;

   zzrmbits = 0;  /* Reset the remaining #bits in the bit buffer */
   mb_width = pict->vseq->mbwidth;
   maxmb = pict->vseq->maxmb;
   qntmat = pict->vseq->intra_qntmat;
   
L_Synchronize:
   if (badflag)
   {  badflag = 0;
      mp_printf("mpx: bad data : ra\n");
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
   mbadr = (nint)((w - 1) * mb_width) - 1;  
   /* Read the quantizer_scale (5bits) and the extra_bit_slice (1bit)*/
   GETNEXTS(6);
   zzb &= 0x3f;
   scale = zzb >> 1;
   scale |= scale == 0;	   /* Force to nonzero */
   base = denv->lki_levd[scale-1];
   /* Skip all the extra_information_slice */
   while (zzb & 1) GETNEXTS(9);
   
   do			/*Skip all the stuffing bits */
   {  GETNEXT16(11);
      zzb &= 0x7ff;
   }  while (zzb == 0xf);
    
   while(zzb == 0x8)	/* Look for macroblock_escape(s) */  
   {  mbadr += 33;
      GETNEXT16(11);
      zzb &= 0x7ff;
   }

   /* D e c o d e  M a c r o b l o c k   A d d ress  I n c rement */
   w = lk_mbadr1[zzb>>6];
   if (w)
   {  mbadr += w & 0xf;
      zzrmbits += w >> 4;
   }
   else
   {  w = lk_mbadr2[zzb];  /* bits 7-10 in zzb known to be 0 */
      if (w)
      {	 mbadr += 8 + (w & 0x1f);
	 zzrmbits += w >> 5;
      }
      else 
      {	 zzrmbits += 11;
	 goto L_Synchronize;
      }
   }
   
   if (mbadr > maxmb) { badflag = 1; goto L_Synchronize; }
   
   
   GETNEXT9(7);
   if (zzb & 0x40)   /* Either 1xxxxxx , no quantizer type */
      zzrmbits += 6;
   else		     /* Or with a quantizer, 01qqqqq */
   {  scale = zzb & 0x1f;
      scale |= scale == 0;
      base = denv->lki_levd[scale-1];
   }   
   prev_chrdc[0] = prev_chrdc[1] = prev_lmdc = 128*8;
   goto L_StartBlockDecode;
   
L_MacroBlockStart:
   GETNEXT9(8);
   
   /* In an Intra Picture all the macroblocks should be present in the
      bitstream in order. Thus expected macroblock_address_increment value
      below is always 1. Furthermore, an Intra MB can be of only one two
      types; intra_mb with/without a quantizer. Vlc is "1" for the intra_mb
      without a quantizer, vlc is "01" for the intra_mb with a quantizer.
      So if the next 11bits are not stuffing bits, 11xx.xxxx and 101xxxxx
      are the only legal values. Since the quantizer_scale is 5bits, the
      8 bits read, will contain all the bits required to decode the
      mb_addr_incr, mb_type and quantizer_scale.
      Note also that we are not trying to decode a macroblock_escape, since
      it is not supposed to be encountered...
   */
L_DecAddrIncr:
   w = zzb & 0xe0;
   /* If (w == 101x.xxxx) then xxxxx=scale */
   if (w == 0xa0) 
   {  scale = zzb & 0x1f;
      scale |= scale == 0;
      base = denv->lki_levd[scale-1];
   }
   /* If (w == 11xx.xxxx) then put back the unused bits */
   else if (w >= 0xc0) zzrmbits += 6;
   else
   {  /* Either there are mb_stuffing bits, or we have encountered bad bits */
      zzrmbits += 8; /* First put back the previously read bits */      
      do	     /* Skip all the stuffing bits */
      {  GETNEXT16(11);
	 zzb &= 0x7ff;
      }  while (zzb == 0xf);
    
      /* Test if encountered a slice start code or possibly, bad bits ! */
      if ( zzb < 0x500 ) 
      {	 zzrmbits += 11;   /* Put back the unused bits */
	 goto L_Synchronize;
      }       
      else  /* To normalize the state, put back the last 3 of the bits read */
      {	 zzrmbits += 3;
	 zzb >>=3;
	 goto L_DecAddrIncr;
      }
   }
   mbadr++;

L_StartBlockDecode:   
   
   {  register nint  r0 = denv->lk_mbbase[mbadr][0];
      register nint  r1 = denv->lk_mbbase[mbadr][1];
      register ubyte *q;
      fbp[0] = q = denv->fp[CURRENT][Y] + (r0<<8) + (r1<<4);
      fbp[1] = q + 8;
      fbp[2] = q = q + (mb_width<<7);
      fbp[3] = q + 8;
      r0 = (r0<<6) + (r1<<3);
      fbp[4] = denv->fp[CURRENT][CB] + r0;
      fbp[5] = denv->fp[CURRENT][CR] + r0;
   }
   
   r2 = 6; 
   nblk = 0;  
   tcf = (int32 *)denv->tcfb;

   while (r2 > 0)
   {  register nint ps, ps0;
      register uint32 blkpat;
      
      r2--;
      if (r2 > 1)
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
	 prev_chrdc[r2] += r0 << 3;
	 tcf[0] = prev_chrdc[r2];
      }
      ps = 0;
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
	       ps += 1;		 /* run is always 0 (+1) for these VLCs */
	       cf = 31 - cf;	 /* level = 31-xxxx for these VLCs */
	       cf <<=1;		 /* unlike the implicit 2*level in the
				    lookup tables, *2 should be done here */
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
	    ps0 = 0;	   /* sign flag, positive */
	    if (!zzb)	   /* 128<= level <=255 */
	    {  GETNEXTS(8);   
	       cf = zzb & 0xff;
	    }
	    else if (zzb == 128)    /* -255 <= level <= -128 */
	    {  GETNEXTS(8);
	       cf = 256 - (zzb&0xff);
	       ps0 = -1;	    /* sign flag, negative */
	    }
	    else if (zzb & 0x80)    /* -127 <= level < 0    */ 
	    {  ps0 = -1; 
	       cf = 256 - zzb; 
	    }
	    else cf = zzb;	    /* 0 < level <= 127	    */
	    cf <<= 1;
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
	 break;
      
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
   shidct(bl_patterns, (int32 *)denv->tcfb, denv->hsza, fbp, 0, 0, nblk); 
   goto L_MacroBlockStart;
}
