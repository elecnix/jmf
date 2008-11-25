/*
 * @(#)mp_rhdrs.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include "mp_mpd.h"


/* R e a d  V i d e o   S e q u e n c e  H e a d e r
   A 0 return value indicates a junk vseqhdr
*/
ubyte *
mp_rdvseqhdr(ubyte *d, mp_vseq *vseq )
{  register uint32 w,ww;
   register nint k;
   
   w = d[0]<<24 | d[1]<<16 | d[2]<<8 | d[3]; /*read the next 32 bits*/
   for (k=0; k<8; k++) vseq->hdr[k] = d[k];
   vseq->hsize = w>>20; 
   vseq->mbwidth = (vseq->hsize + 15) >> 4;
   vseq->vsize = (w >> 8) & 0x0fff;
   vseq->aspect = (w >> 4) & 0xf;
   vseq->picrate = w & 0xf;
   vseq->maxmb = vseq->mbwidth * ((vseq->vsize+15)>>4) - 1;

   w = d[4]<<24 | d[5]<<16 | d[6]<<8 | d[7]; /*read the next 32 bits*/;
   vseq->bitrate = (w >> 14) & 0x3ffff;
   vseq->vbv = (w >> 3) & 0x3ff;
   vseq->constraint = (w >> 2) & 1;
   d+=8; /*skip the 8 bytes which are already read*/
   
   /* Make a validy check */
   if (vseq->aspect==0 || vseq->picrate==0 || vseq->bitrate==0)
      return(0);

   /* Check if intra quantizer matrix exists.
      Note that vseq->intra_qntmat and vseq->inter_qntmat contents are
      in ZIGZAG order, just like they would come in the bit stream.
   */

   if (w & 2)  
   {  /* Intra quantizer matrix starts at aligned byte - 1bit position,
      	 So the matrix elements should be realigned.
      */
      for (k=0; k<64; k++)
      {  ubyte r0;
	 ww = *d++;
      	 r0 = (ww>>1) | (w<<7 & 0x80);
	 if (r0 == 0) return(0);
      	 vseq->intra_qntmat[k] = r0;
      	 w = ww;
      }
      if ( vseq->intra_qntmat[0] != 8 ) return(0);
      vseq->defaultintra = 0;
   }
   else
   {  for (k=0; k<64; k++)	
	 vseq->intra_qntmat[k] = zzdefault_intraq[k];
      vseq->defaultintra = 1;
   }
   
   /* check if inter quantizer matrix is given  */
   if (w & 1)  
   {  /*inter quantization matrix is byte aligned  */
      for (k=0;k<64;k++) 
	 if ((vseq->inter_qntmat[k] = *d++) == 0) return(0); 
      vseq->defaultinter = 0;
   }
   else
   {  for (k=0; k<64; k++)	
	 vseq->inter_qntmat[k] = 16;
      vseq->defaultinter = 1;
   }
   /* At this point we have read either of 8, (8+64) or (8+64+64) bytes,
      and we are byte aligned
   */
   return(d);
}
  


/* R e a d   G r o u p   o f  P i c t u r e s   H e a d e r */
ubyte
*mp_rdgophdr(ubyte *d, mp_gop *gop)
{  register uint32 w;

   w = d[0]<<24 | d[1]<<16 | d[2]<<8 | d[3]; /*read the next 32 bits*/
   gop->drop = w>>31;
   gop->hour = (w>>26) & 0x1f;
   gop->minute = (w>>20) & 0x3f;
   gop->second = (w>>13) & 0x3f;
   gop->picture = (w>>7) & 0x3f;
   gop->closed_gop = (w>>6) & 1;
   gop->broken_link = (w>>5) & 1;

   /* The remaining 5 bits should be stuffing "0" bits */
   return(d+4);
}


/* R e a d  P i c t u r e  H e a d e r */ 
ubyte
*mp_rdpicthdr(ubyte *d, mp_pict *pict)
{  register uint32 r0,r1;
   register nint bits;

   r0 = d[0]<<24 | d[1]<<16 | d[2]<<8 | d[3];
   d+=4;

   pict->tref = (r0>>22) & 0x3ff;
   pict->ptype = (r0>>19) & 0x7;
   pict->vbv_delay = (r0>>3) & 0xffff;
   
   if (pict->ptype == P_PICT)	       /* 33bits */
   {  pict->fullpel_fwd = (r0>>2) & 1;
      r1 = ((r0&3)<<8) | *d++;
      pict->fwd_rsize = (r1>>7) - 1;
      r1 &= 0x7f;
      bits = 7;      
   }
   else if (pict->ptype == B_PICT)     /* 37bits */
   {  pict->fullpel_fwd = (r0>>2) &1;
      r1 = ((r0&3)<<8) | *d++;
      pict->fwd_rsize = (r1>>7) - 1;
      pict->fullpel_bwd = (r1>>6) & 1;
      pict->bwd_rsize = ((r1>>3) & 7) - 1;
      r1 &= 7;
      bits = 3;      
   }
   else				       /* 29bits */
   {  r1 = r0 & 7;
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
   bits -=1;   /*Extra_bit_picture */
   
   /* Put back unnecessarily retrieved bytes*/
   d -= bits >> 3;
   
   /* We will get rid of any picture_extension_data or user_data so that
      the caller can assume to have slice start codes next. Since it is
      very unlikely to find any extension code, following type of search
      will get us out of here with minimum overhead...
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




/* R e a d  ISO11172 S y s t e m  H e a d e r 
   Currently disgards the stream buffer sizes info
*/
ubyte *mp_rdsyshdr(ubyte *d, mp_syshdr *hdr)
{  register uint32 w;
   register uint32 hdr_length;

   hdr_length = d[0]<<8 | d[1];
   w = d[2]<<16 | d[3]<<8 | d[4];
   hdr->rate_bound = (w>>1) & 0x3fffff;
   w = d[5];
   hdr->aud_bound = w>>2;
   hdr->fixed = w & 2;
   hdr->csps = w & 1;
   w = d[6];
   hdr->aud_lock = w & 0x80;
   hdr->vid_lock = w & 0x40;
   hdr->vid_bound = w & 0x1f;
   
   d += 8;  /* skip the reserved_byte */

   while ( *d & 0x80) d+= 3;
   return(d);
}
