/*
 * @(#)mp_rndmb.c	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include "mp_mpd.h"

/* Copy the consecutively skipped MacroBlocks of a P picture from the
   past reference picture buffer to the current picture buffer.
*/
void
skipmb(nint mbadr, nint cnt, mp_denv *denv)
{  register ubyte *p,*q;
   register uint32 r1,r2,r3,r4,r5,r6,wy,wc;
   register uint16 (*mbbase)[2];
   
   mbbase = denv->lk_mbbase;
   do
   {  r1 = mbbase[mbadr][0];
      r2 = mbbase[mbadr][1];
      wy = (r1<<8) + (r2<<4);	 /* Offset of top left Lm pixel */
      wc = (r1<<6) + (r2<<3);	 /* Offset of top left Cr/Cb pixel */
      
      p = denv->fp[PAST][Y]    + wy;
      q = denv->fp[CURRENT][Y] + wy;
      r1 = denv->hsz;
      r2 = 16; /* #lines in a MB */
      do       /* Copy the 4 Y blocks */
      {  r3 = ((uint32*)p)[0];	 /* Read 16 pixels  */
	 r4 = ((uint32*)p)[1];
	 r5 = ((uint32*)p)[2];
	 r6 = ((uint32*)p)[3];
	 ((uint32 *)q)[0] = r3;	 /* Write 16 pixels */
	 ((uint32 *)q)[1] = r4;
	 ((uint32 *)q)[2] = r5;
	 ((uint32 *)q)[3] = r6;
	 p += r1;
	 q += r1;
	 r2--;
      }  while (r2);
            
         /* Copy the CR block */
	 p = denv->fp[PAST][CR]    + wc;
	 q = denv->fp[CURRENT][CR] + wc;
	 r1 >>= 1;   /* Half Lm size for Cr/Cb */
	 r2 = 4;     /* Will read,write 2 lines at a time, 8/2 = 4 */
	 do
	 {  r3 = ((uint32 *)p)[0]; 
	    r4 = ((uint32 *)p)[1];
	    p += r1;
	    r5 = ((uint32 *)p)[0]; 
	    r6 = ((uint32 *)p)[1];
	    ((uint32 *)q)[0] = r3;
	    ((uint32 *)q)[1] = r4;
	    q += r1;
	    ((uint32 *)q)[0] = r5;
	    ((uint32 *)q)[1] = r6;
	    q += r1;
	    p += r1;
	    r2--;
	 } while (r2);
	 	 
	 /* Copy the CB block */
	 p = denv->fp[PAST][CB]    + wc;
	 q = denv->fp[CURRENT][CB] + wc;
	 r2 = 4;
	 do
	 {  r3 = ((uint32 *)p)[0]; 
	    r4 = ((uint32 *)p)[1];
	    p += r1;
	    r5 = ((uint32 *)p)[0]; 
	    r6 = ((uint32 *)p)[1];
	    ((uint32 *)q)[0] = r3;
	    ((uint32 *)q)[1] = r4;
	    q += r1;
	    ((uint32 *)q)[0] = r5;
	    ((uint32 *)q)[1] = r6;
	    q += r1;
	    p += r1;
	    r2--;
	 } while (r2);
	 
      mbadr += 1;      
   } while (--cnt);  
}







/* Render a predicted Macroblock.
   First the prediction blocks are filtered. If no differential transform
   coefs. are present for a block, corresponding prediction block
   is written directly to current picture buffer, else it is written
   to a temporary buffer which will be read by the shidct routine.
   Note that block informations to be passed to shidct are stored 
   sequentially. So the shidct works on a given number of blocks without
   regard to their position in the MacroBlock or their being a Lm or
   Cr/Cb block.
*/
void
rendmb(mp_macroblk *mb, mp_denv *denv, uint32 *patterns)
{
   nuint hsz = denv->hsz;
   uint32 wy,wc;
   nint k,n;
   ubyte flt[2];
   ubyte *fbp[6];
   uint16 fbw[6];
   ubyte *destp[6], *src1p[6], *src2p[6];
   ubyte predbf[6][64];
   
   {  register nuint r0 = denv->lk_mbbase[mb->adr][0];
      register nuint r1 = denv->lk_mbbase[mb->adr][1];   
      wy = (r0<<8) + (r1<<4) ;
      wc = (r0<<6) + (r1<<3) ;
   }
   
   /* Filtering flags (mb->lmfilt, mb->chfilt) are:
      bit0: bwd vert
      bit1: bwd horz
      bit2: fwd vert
      bit3: fwd horz
   */
      
   if (!(mb->flags & RF_FILT))  mb->lmfilt = mb->chfilt = 0;   
   
   {  register ubyte *q;
      destp[0] = q = wy + denv->fp[CURRENT][Y];
      destp[4] = wc + denv->fp[CURRENT][CB];
      destp[5] = wc + denv->fp[CURRENT][CR];
      destp[1] = q + 8;
      destp[2] = q = q + (hsz << 3);
      destp[3] = q + 8;
   }
   
   /* Render an averaged prediction block of a B Picture to frame buffer,
      We are making an important shortcut here by not doing any filtering
      at all for this kind of macroblock. Any negligible errors will
      not propagate since the block belongs to a B Picture.
   */
   if ((mb->type & (MB_FWD | MB_BWD)) == (MB_FWD | MB_BWD))
   {  {  register ubyte *q;
	 register uint32 r0 = hsz << 3;
	 src1p[0] = q = denv->fp[PAST][Y] + mb->fwd_lvmv*hsz + wy + mb->fwd_lhmv;
	 src1p[1] = q + 8;
	 src1p[2] = q = q + r0;
	 src1p[3] = q + 8;
	 src2p[0] = q = denv->fp[FUTURE][Y] + mb->bwd_lvmv*hsz + wy + mb->bwd_lhmv;
	 src2p[1] = q + 8;
	 src2p[2] = q = q + r0;
	 src2p[3] = q + 8;
      }

      {	 register uint32 r0 = hsz >> 1, r1;
	 r1 = mb->fwd_cvmv*r0 + wc + mb->fwd_chmv;
	 r0 = mb->bwd_cvmv*r0 + wc + mb->bwd_chmv;
	 src1p[4] = denv->fp[PAST][CB] + r1;
	 src1p[5] = denv->fp[PAST][CR] + r1;
	 src2p[4] = denv->fp[FUTURE][CB] + r0;
	 src2p[5] = denv->fp[FUTURE][CR] + r0;
      }
      
      k = 0;
      n = 0;
      do
      {	 register ubyte *q, *p, *s;
         register dhsz, shsz = hsz >> (k >> 2);
         nint m;
	 if (mb->cbp & (32 >> k)) 
      	 {  q = predbf[n]; 
      	    dhsz = 8;
	    fbw[n] = shsz;
	    fbp[n] = destp[k];
	    n++;
      	 }
      	 else  
      	 {  q = destp[k];
      	    dhsz = shsz;
      	 } 
      	 p = src1p[k];
      	 s = src2p[k];
	 m = 8;
#ifdef WIN32_MMX
	 __asm {
	     mov ecx, 8;
	     mov esi, p;
	     mov ebx, s;
	     mov edi, q;
	 BAC2:
	     movd mm0, DWORD PTR [esi];
	     movd mm1, DWORD PTR [ebx];
	     pxor mm7, mm7;
	     punpcklbw mm0, mm7;
	     movd mm2, DWORD PTR [esi+4];
	     punpcklbw mm1, mm7;
	     movd mm3, DWORD PTR [ebx+4];
	     paddusw mm0, mm1;
	     psrlw mm0, 1;
	     
	     punpcklbw mm2, mm7;
	     add esi, shsz;
	     punpcklbw mm3, mm7;
	     paddusw mm2, mm3;
	     add ebx, shsz;
	     psrlw mm2, 1;
	     
	     packuswb mm0, mm2;
	     movq QWORD PTR [EDI], mm0;
	     add edi, dhsz;
	     dec ecx;
	     jnz BAC2;
	     emms;
	 }
	     
#else
	 do
	 {  register uint32 r0,r1,r2,r3,r4,r5,r6,r7;
	    m--;
	    r0 = p[0];
	    r1 = p[1];
	    r2 = p[2];
	    r3 = p[3];
	    
	    r4 = s[0];
	    r5 = s[1];
	    r6 = s[2];
	    r7 = s[3];
	    
	    q[0] = (r0 + r4) >> 1;
	    q[1] = (r1 + r5) >> 1;
	    q[2] = (r2 + r6) >> 1;
	    q[3] = (r3 + r7) >> 1;
	    
	    r0 = p[4];
	    r1 = p[5];
	    r2 = p[6];
	    r3 = p[7];
	    
	    r4 = s[4];
	    r5 = s[5];
	    r6 = s[6];
	    r7 = s[7];
	    
	    q[4] = (r0 + r4) >> 1;
	    q[5] = (r1 + r5) >> 1;
	    q[6] = (r2 + r6) >> 1;
	    q[7] = (r3 + r7) >> 1;
	    		
	    p += shsz;
	    s += shsz;
	    q += dhsz;
	 } while (m);
#endif
      	 k++;
      } while (k < 6);
      if (mb->cbp)
	 shidct(patterns, (int32 *)denv->tcfb,fbw,fbp,(ubyte *)predbf, RF_PRED, n);
      return;
   }
   
   if (mb->type & MB_FWD)
   {  register ubyte *q;
      register uint32 r0 = hsz << 3;
      src1p[0] = q = denv->fp[PAST][Y]+ mb->fwd_lvmv*hsz + wy + mb->fwd_lhmv;
      src1p[1] = q + 8;
      src1p[2] = q = q + r0;
      src1p[3] = q + 8;
      flt[0] = mb->lmfilt >> 2;
      flt[1] = mb->chfilt >> 2;
      r0 = hsz >> 1;
      r0 = mb->fwd_cvmv*r0 + wc + mb->fwd_chmv;
      src1p[4] = denv->fp[PAST][CB] + r0;
      src1p[5] = denv->fp[PAST][CR] + r0;
   }
   else if (mb->type & MB_BWD)
   {  register ubyte *q;
      register uint32 r0 = hsz << 3;
      src1p[0] = q = denv->fp[FUTURE][Y]+ mb->bwd_lvmv*hsz + wy + mb->bwd_lhmv;
      src1p[1] = q + 8;
      src1p[2] = q = q + r0;
      src1p[3] = q + 8;
      flt[0] = mb->lmfilt;
      flt[1] = mb->chfilt;
      r0 = hsz >> 1;
      r0 = mb->bwd_cvmv*r0 + wc + mb->bwd_chmv;
      src1p[4] = denv->fp[FUTURE][CB] + r0;
      src1p[5] = denv->fp[FUTURE][CR] + r0;
   }
   else	 /* A P_Picture MB with no motion vector, but predicted */
   {  register ubyte *q;
      src1p[0] = q = denv->fp[PAST][Y] + wy;
      src1p[1] = q + 8;
      src1p[2] = q = q + (hsz << 3);
      src1p[3] = q + 8;
      flt[0] = 0;
      flt[1] = 0;
      src1p[4] = denv->fp[PAST][CB]+ wc;
      src1p[5] = denv->fp[PAST][CR]+ wc;
   }
   
   n = 0;
   k = 0;
   do
   {  register ubyte *p, *s, *q;
      /* k: [0,4] ->Lm blocks, 4->Cb, 5->Cr .    For Cr,Cb half the horz. size */
      register nuint dhsz, shsz = hsz >> (k >> 2);
      
      if (mb->cbp & (32 >> k)) 
      {  /* Prepare for IDCT */
	 q = predbf[n]; 
	 dhsz = 8;
	 fbw[n] = shsz;
	 fbp[n] = destp[k];
	 n++;
      }
      else  
      {  /* No IDCT for this block, output direct to picture buffer */
	 q = destp[k];
	 dhsz = shsz;
      } 
      
      p = src1p[k];
      
      /* flt[0] -> filter flags for Lm blocks
	 flt[1] -> filter flags for Cr/Cb blocks
      */
      
      switch ( flt[k >> 2] ) {
      case  0: /* No filtering on the prediction block*/
      {
	  nint m = 8;
#if defined(WIN32) || defined(X86)
	  do {
	      m--;
	      *(uint32*)q = *(uint32*)p;
	      *(uint32*)(q+4) = *(uint32*)(p+4);
	      p += shsz;
	      q += dhsz;
	  } while (m);
#else   /* Alignment related SPARC code */
	  if ( (uint32)p & 1)
	    do 
	    {  register nuint r0,r1,r2,r3,r4,r5,r6,r7;
	       m--;
	       r0 = p[0];
	       r1 = p[1];
	       r2 = p[2];
	       r3 = p[3];
	       r4 = p[4];
	       r5 = p[5];
	       r6 = p[6];
	       r7 = p[7];		       
	       q[0] = r0;
	       q[1] = r1;
	       q[2] = r2;
	       q[3] = r3;
	       q[4] = r4;
	       q[5] = r5;
	       q[6] = r6;
	       q[7] = r7;	       
	       p += shsz;
	       q += dhsz;
	    } while (m);
	 
	 else if ( (uint32)p & 2 )  /* Pred. block is aligned at 2byte boundary */
	    do
	    {  register nuint r0,r1,r2,r3;
	       m--;
	       r0 = ((uint16 *)p)[0];
	       r1 = ((uint16 *)p)[1];
	       r2 = ((uint16 *)p)[2];
	       r3 = ((uint16 *)p)[3];
	       ((uint16 *)q)[0] = r0;
	       ((uint16 *)q)[1] = r1;
	       ((uint16 *)q)[2] = r2;
	       ((uint16 *)q)[3] = r3;
	       p += shsz; 
      	       q += dhsz;
	    } while (m);
	 else			    /* Pred. block is aligned at 4byte boundary */
	    do
	    {  register uint32 r0,r1;
	       m--;
	       r0 = ((uint32 *)p)[0];
	       r1 = ((uint32 *)p)[1];
	       ((uint32 *)q)[0] = r0;
	       ((uint32 *)q)[1] = r1;
	       p += shsz; 
      	       q += dhsz;
	    } while (m);
#endif
	 break;
      }

      case  2: /* Horizontal filtering on the prediction block */
      {
#ifdef WIN32_MMX
	    {
		__asm {
		    mov ecx, 8
		    mov esi, p;
		    mov edi, q
		BAC:
		    movd mm0, DWORD PTR [esi];
		    movd mm1, DWORD PTR [esi+1];
		    pxor mm7, mm7;
		    punpcklbw mm0, mm7;
		    punpcklbw mm1, mm7;
		    paddusw mm0, mm1;
		    psrlw mm0, 1;

		    movd mm2, DWORD PTR [esi+4];
		    movd mm3, DWORD PTR [esi+5];
		    punpcklbw mm2, mm7;
		    punpcklbw mm3, mm7;
		    paddusw mm2, mm3;
		    psrlw mm2, 1;

		    packuswb mm0, mm2;
		    movq QWORD PTR [EDI], mm0;
		    add esi, shsz;
		    add edi, dhsz;
		    dec ecx;
		    jnz BAC;
		    emms;
		}
	    }
#else
	 nint m = 8;
         do
      	 {  nuint r0,r1,r2,r3,r4,r5,r6,r7;
      	    m--;
      	    r0 = p[0];
	    r1 = p[1];
	    r2 = p[2];
	    r3 = p[3];
	    r4 = p[4];
	    r5 = p[5];
	    r6 = p[6];
	    r7 = p[7];
	    
	    q[0] = (r0+r1) >> 1;
	    r0 = p[8];
	    q[1] = (r1+r2) >> 1;
	    q[2] = (r2+r3) >> 1;
	    q[3] = (r3+r4) >> 1;
	    q[4] = (r4+r5) >> 1;
	    q[5] = (r5+r6) >> 1;
	    q[6] = (r6+r7) >> 1;
	    q[7] = (r7+r0) >> 1;
	    p += shsz;
      	    q += dhsz;
      	  } while (m);
#endif
      	  break;
      }
      case 1: /* Vertical filtering on the prediction block */
      {	 register nuint r0,r1,r2,r3,r4,r5,r6,r7;
         ubyte *savedq = q;
         nint m = 4;
      	 s = p ;
	 r0 = p[0];
	 r1 = p[1];
	 r2 = p[2];
	 r3 = p[3];
	 p += shsz;
      	 do    /* Vertically filter left half of the block */
      	 {  m--;
      	    r4 = p[0];
	    r5 = p[1];
	    r6 = p[2];
	    r7 = p[3];
	    q[0] = (r0+r4) >> 1;
	    q[1] = (r1+r5) >> 1;
	    q[2] = (r2+r6) >> 1;
	    q[3] = (r3+r7) >> 1;
	    p += shsz;
	    q += dhsz;
	    r0 = p[0];
	    r1 = p[1];
	    r2 = p[2];
	    r3 = p[3];
	    q[0] = (r0+r4) >> 1;
	    q[1] = (r1+r5) >> 1;
	    q[2] = (r2+r6) >> 1;
	    q[3] = (r3+r7) >> 1;
	    p += shsz;
	    q += dhsz;
	 } while (m);	    
	 
	 m = 4;
      	 p = s;
	 q = savedq ;
	 r0 = p[4];
	 r1 = p[5];
	 r2 = p[6];
	 r3 = p[7];
	 p += shsz;
      	 do    /* Vertically filter right half of the block */
      	 {  m--;
      	    r4 = p[4];
	    r5 = p[5];
	    r6 = p[6];
	    r7 = p[7];
	    q[4] = (r0+r4) >> 1;
	    q[5] = (r1+r5) >> 1;
	    q[6] = (r2+r6) >> 1;
	    q[7] = (r3+r7) >> 1;
	    p += shsz;
	    q += dhsz;
	    r0 = p[4];
	    r1 = p[5];
	    r2 = p[6];
	    r3 = p[7];
	    q[4] = (r0+r4) >> 1;
	    q[5] = (r1+r5) >> 1;
	    q[6] = (r2+r6) >> 1;
	    q[7] = (r3+r7) >> 1;
	    p += shsz;
	    q += dhsz;
	 } while (m);	    	    
      	 break;
      }
      case 3:  /* Horz. & Vert. filtering on the prediction block */
      {  nint m = 8;
      	 s = p + shsz;
      	 do
      	 {  register nuint r0,r1,r2,r3,r4,r5,r6,r7;
            m--;
      	    r0 = p[0];
	    r1 = s[0];
	    r2 = p[1];
	    r3 = s[1]; 
	    
	    r6 = r0 + r1;
	    r7 = r2 + r3;
	    r0 = p[2];
	    r1 = s[2];
	    q[0] = (r6 + r7) >> 2;
	    r6 = r0 + r1;
	    r0 = p[3];
	    r1 = s[3]; 
	    q[1] = (r6 + r7) >> 2;
	    r7 = r0 + r1;
	    r0 = p[4];
	    r1 = s[4];
	    q[2] = (r6 + r7) >> 2;
	    r6 = r0 + r1;
	    r0 = p[5];
	    r1 = s[5]; 
	    q[3] = (r6 + r7) >> 2;
	    r7 = r0 + r1;
	    r0 = p[6];
	    r1 = s[6];
	    q[4] = (r6 + r7) >> 2;
	    r6 = r0 + r1;
	    r0 = p[7];
	    r1 = s[7]; 
	    q[5] = (r6 + r7) >> 2;
	    r7 = r0 + r1;
	    r0 = p[8];
	    r1 = s[8]; 
	    q[6] = (r6 + r7) >> 2;
	    r6 = r0 + r1;
	    q[7] = (r6 + r7) >> 2;
	    	    
      	    p += shsz;
      	    s += shsz;
      	    q += dhsz;
      	 }  while (m);
      }
      }  /* Switch */
      k++;
   } while (k < 6);

   if (mb->cbp)
      shidct(patterns, (int32 *)denv->tcfb, fbw, fbp, (ubyte *)predbf, RF_PRED, n);	 

}

