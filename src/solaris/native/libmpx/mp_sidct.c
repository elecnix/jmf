/*
 * @(#)mp_sidct.c	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

/* 
 * S H I D C T 
 * I N T E G E R   S H I F T / A D D   A R I T H M E T I C  V E R S I O N 
*/

#include "math.h"
#include "mp_mpd.h"

#define	M_SQRT2		1.41421356237309504880
#define	M_PI		3.14159265358979323846
#define M_SQRT1_2       0.70710678118654752440

#if defined(WIN32) || defined(X86)

#define MULT2(r0, r1) { \
   r1 = r0 * 277;       \
   r0 = r0 * 669;       \
}

#else

#define MULT2(r0,r1)	\
{  register int32 ra,rb;\
   ra   = r0 << 4;	\
   ra  += r0;		\
   rb   = r0 << 2;	\
   ra  += rb;		\
   r1   = r0 << 8;	\
   r1  += ra;		\
   ra <<= 5;		\
   ra  -= rb;		\
   r0  += ra;		\
}

#endif

#if defined(WIN32) || defined(X86)

#define MULT4(r0, r1, r2, r3) { \
   r1 = r0 * 602;               \
   r2 = r0 * 402;               \
   r3 = r0 * 141;               \
   r0 = r0 * 710;               \
}

#else

#define MULT4(r0,r1,r2,r3)	\
{				\
   r3   = r0 << 7;  /*128*/	\
   r2   = r0 + r0;  /*  2*/	\
   r1   = r2 + r0;  /*  3*/	\
   r0   = r2 << 3;  /* 16*/	\
   r3   = r3 - r1;  /*125*/	\
   r3  += r0;	    /*141*/	\
   r1 <<= 7;	    /*384*/	\
   r1  += r0;	    /*400*/	\
   r0   = r2 << 8;  /*512*/	\
   r0  -= r2;	    /*510*/	\
   r2  += r1;	    /*402*/	\
   r1 >>= 1;	    /*200*/	\
   r0  += r1;	    /*710*/	\
   r1  += r2;	    /*602*/	\
}

#endif

static uint64 zeros[2] = { 0, 0};
static float mc[8];
static ubyte easy[256];
static ubyte clip[256*3];
static ubyte *clipb = clip+256;

void init_idct()
{  nint k;

   mc[0] = M_SQRT2;
   mc[1] = M_SQRT2*cos(M_PI/8.0);	/* sng20  mc2 */
   mc[2] = M_SQRT2*sin(M_PI/8.0);	/* sng21  ms2 */
   mc[3] = M_SQRT2*cos(M_PI/16.0);	/* sng10  mc1 */
   mc[4] = M_SQRT2*sin(M_PI/16.0);	/* sng13  ms1 */
   mc[5] = M_SQRT2*cos(5*M_PI/16.0);	/* sng12  mc5 */
   mc[6] = M_SQRT2*sin(5*M_PI/16.0);	/* sng11  ms5 */
   
   for (k=1; k<=7; k++)
      if ( k & 1)	mc[k] = M_SQRT1_2/cos(k*M_PI/16.0);
      else		mc[k] = 0.5/cos(k*M_PI/16.0);
   
   for (k=0; k<256; k++)
   {  clipb[k] = k;
      clipb[256+k] = 255;
      clipb[-k] = 0;
   }
   
   for (k=0; k<256; k++)
   {  register nuint pat = k;
   
      pat |= (pat & 3) << 4;
            
      if (pat & 0x20)
      {  if ( pat & 0x10 ) easy[k] = 7<<5;	/* reg7 */
         else easy[k] = 6<<2;			/* reg4 */
      }
      else easy[k] = 5;			/* dbl3 */
   }  
}



void
shidct( uint32 bl_patterns[], int32 *tcf, uint16 fbw[], ubyte *fbp[],
	ubyte* pred, uint32 flags, uint32 nblocks)
{  register double dblzero = ((double *)zeros)[0];
   register float fzero = ((float *)zeros)[0];
   nint b;
   double bdf[8];
   int32 bj[8*10];

   for (b = 0; b < nblocks; b++)
   {  register uint32 ptrn = bl_patterns[b];
      register int32 *d = tcf;
      register ubyte *w = fbp[b];
      register nint yoffs = fbw[b];
                        
      tcf += 64;
      if ( (ptrn & 0x4) == 0 )		/* DRC */
      {  nint irc = ptrn & 0x3;
         if (irc == 0)			/* DC only */
         {  register int32 dc = d[0] >> 3;
	    ((float*)d)[0] = fzero;
	    if (flags & RF_PRED)
	    {  register nint row = 8;
               do
	       {  register nint s0,s1;
	          w[0] = s0 = pred[0] + dc;
	          w[1] = s1 = pred[1] + dc; s0 |= s1;
                  w[2] = s1 = pred[2] + dc; s0 |= s1;
                  w[3] = s1 = pred[3] + dc; s0 |= s1;
                  w[4] = s1 = pred[4] + dc; s0 |= s1;
                  w[5] = s1 = pred[5] + dc; s0 |= s1;
                  w[6] = s1 = pred[6] + dc; s0 |= s1;
                  w[7] = s1 = pred[7] + dc; s0 |= s1;
                  if ( (nuint)s0 > 255 )
                  {  w[0] = clipb[pred[0] + dc];
	             w[1] = clipb[pred[1] + dc];
                     w[2] = clipb[pred[2] + dc];
                     w[3] = clipb[pred[3] + dc];
                     w[4] = clipb[pred[4] + dc];
                     w[5] = clipb[pred[5] + dc];
                     w[6] = clipb[pred[6] + dc];
                     w[7] = clipb[pred[7] + dc];
                  }
                  
                  w += yoffs;
                  pred += 8;
               } while (--row);
	    }
	    else
	    {
	       register double tdf;
	    
	       if ((uint32)dc > 255) dc = 255;
	       dc |= dc<<8;
	       dc |= dc<<16;
	       ((uint32 *)w)[0] = (uint32)dc;
	       ((uint32 *)w)[1] = (uint32)dc;
	       tdf = ((double *)w)[0];
	       w += yoffs;	    
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf;	 
	    }
	    continue;
         }
         
         if (irc & 1)		/* OCOLUMN  ? */
         {  nuint ocol = (ptrn >> 6) & 0xf;
            register int32 r0,r1,r2,r3,r4,r5,r6,r7;
            r0 = d[8];
            if (!(ocol & 2))
            {  register int32 dc = d[0];
	       ((float*)d)[0*8] = fzero;	       
	       dc <<= 9;
               if (ocol == 4)		/* sng1 */
               {  MULT4(r0,r1,r2,r3);
                  ((float*)d)[1*8] = fzero;                  
		  r4 = dc - r3;
		  r5 = dc - r2;
		  r6 = dc - r1;
		  r7 = dc - r0;
		  r0 = dc + r0;
		  r1 = dc + r1;
		  r2 = dc + r2;
		  r3 = dc + r3;		  
	       }
	       else if (ocol == 12)	/* dbl3 */
	       {  register int32 s0,s1;
	          s0 = d[16];
	          MULT4(r0,r1,r2,r3);
	          ((float*)d)[1*8] = fzero;
	          MULT2(s0,s1);
	          ((float*)d)[2*8] = fzero;
	          r7 = s0+dc-r0;
	          r0 = s0+dc+r0;
	          r6 = s1+dc-r1;
	          r1 = s1+dc+r1;
	          s0 = dc - s0;
	          s1 = dc - s1;
	          r4 = s0 - r3;
	          r5 = s1 - r2;
	          r2 = s1 + r2;
	          r3 = s0 + r3;
	       }
	       else			/* sng2 */
	       {  r0 = d[2*8];
	          MULT2(r0,r1);
	          ((float*)d)[2*8] = fzero;
	          r2 = dc - r1;
	          r3 = dc - r0;
	          r0 = dc + r0;
	          r1 = dc + r1;
	          r4 = r3;
	          r5 = r2;
	          r6 = r1;
	          r7 = r0;
	       }
	    }
	    else if ( !(ocol & 1))	/* reg4 */
	    {  register int32 r8;
	       r5 = d[3*8];
	       MULT4(r0,r3,r2,r1);
	       ((float *)d)[1*8] = fzero;
	       MULT4(r5,r7,r4,r6);
	       r5 = r2 - r5;  r2 = d[2*8];
	       r4 = r1 - r4;  r1 = d[4*8];
	       r7 = r0 + r7;  r0 = d[0*8];
	       r6 = r3 - r6;  r1 <<= 9;
	       MULT2(r2,r3);
	       r0 <<= 9;
	       ((float *)d)[0*8] = fzero;
	       r8 = r0 + r1;  r0 = r0 - r1;
	       ((float *)d)[2*8] = fzero;
	       r1 = r8 + r2;  r8 = r8 - r2;
	       ((float *)d)[3*8] = fzero;
	       r2 = r0 + r3;  r3 = r0 - r3;
	       ((float *)d)[4*8] = fzero;
	       r0 = r1 + r7;
	       r7 = r1 - r7;
	       r1 = r2 + r6;
	       r6 = r2 - r6;
	       r2 = r3 + r5;
	       r5 = r3 - r5;
	       r3 = r8 + r4;
	       r4 = r8 - r4;
	    }
	    else			/* reg7 */
	    {  register int32 r8;
	       r5 = d[3*8];
	       MULT4(r0,r3,r2,r1);
	       ((float *)d)[1*8] = fzero;
	       MULT4(r5,r7,r4,r6);
	       r3 = r3 - r6;  r6 = d[5*8];
	       r0 = r0 + r7;  r1 = r1 - r4;  r2 = r2 - r5;
	       MULT4(r6,r4,r7,r5);
	       r1 = r1 + r4;  r4 = d[7*8];
	       r0 = r0 + r7;  r2 = r2 + r5;  r3 = r3 - r6;
	       MULT4(r4,r5,r6,r7);
	       r5 = r2 + r5;  r2 = d[2*8];
	       r7 = r0 + r7;  r0 = d[6*8];
	       r4 = r1 - r4;
	       r6 = r3 - r6;
	       MULT2(r2,r3);
	       MULT2(r0,r1);
	       r2 += r1;  r1 = d[4*8];
	       r3 -= r0;  r0 = d[0*8];
	       r1 <<= 9;
	       r0 <<= 9;
	       ((float *)d)[0*8] = fzero;
	       r8 = r0 + r1;  r0 = r0 - r1;
	       ((float *)d)[2*8] = fzero;
	       r1 = r8 + r2;  r8 = r8 - r2;
	       ((float *)d)[3*8] = fzero;
	       r2 = r0 + r3;  r3 = r0 - r3;
	       ((float *)d)[4*8] = fzero;
	       r0 = r1 + r7;
	       r7 = r1 - r7;
	       ((float *)d)[5*8] = fzero;
	       r1 = r2 + r6;
	       r6 = r2 - r6;
	       ((float *)d)[6*8] = fzero;
	       r2 = r3 + r5;
	       r5 = r3 - r5;
	       ((float *)d)[7*8] = fzero;
	       r3 = r8 + r4;
	       r4 = r8 - r4;
	    }
            bj[0] = r0 >> 12;
            bj[1] = r1 >> 12;
            bj[2] = r2 >> 12;
            bj[3] = r3 >> 12;
            bj[4] = r4 >> 12;
            bj[5] = r5 >> 12;
            bj[6] = r6 >> 12;
            bj[7] = r7 >> 12;
            
            if (irc & 2) goto LB_ORow;
            
            
            if (flags & RF_PRED)
	    {  nint k;
	       for (k=0; k<8; k++)
	       {  register nint dc;
	          register nint s0,s1;
	          dc = bj[k];
	          w[0] = s0 = pred[0] + dc;
                  w[1] = s1 = pred[1] + dc; s0 |= s1;
                  w[2] = s1 = pred[2] + dc; s0 |= s1;
                  w[3] = s1 = pred[3] + dc; s0 |= s1;
                  w[4] = s1 = pred[4] + dc; s0 |= s1;
                  w[5] = s1 = pred[5] + dc; s0 |= s1;
                  w[6] = s1 = pred[6] + dc; s0 |= s1;
                  w[7] = s1 = pred[7] + dc; s0 |= s1;
                  if ( (nuint)s0 > 255)
                  {  w[0] = clipb[pred[0] + dc];
                     w[1] = clipb[pred[1] + dc];
                     w[2] = clipb[pred[2] + dc];
                     w[3] = clipb[pred[3] + dc];
                     w[4] = clipb[pred[4] + dc];
                     w[5] = clipb[pred[5] + dc];
                     w[6] = clipb[pred[6] + dc];
                     w[7] = clipb[pred[7] + dc];
                  }
                  w += yoffs;
                  pred += 8;
               }
            }
	    else
	    {  nint k;
	       for (k=0; k<8; k++)
	       {  register nint r0;
	          r0 = bj[k];
	          if ( (nuint)r0 > 255) r0 = clipb[r0];
	          r0 |= r0<<8;
	          r0 |= r0<<16;
	          ((uint32 *)w)[0] = r0;
	          ((uint32 *)w)[1] = r0;
		  w += yoffs;
	       }
	    }
	    continue;
         }
         
         
         LB_ORow:	/* OROW	*/
                  
         {  nuint orow = (ptrn >> 19) & 0xf;
            register int32 r0,r1,r2,r3,r4,r5,r6,r7;
            
            r0 = d[1];
            if (!(orow & 2))
            {  register int32 dc = d[0];
	       ((double *)d)[0] = dblzero;	       
	       dc <<= 9;
               if (orow == 4)		/* sng1 */
               {  MULT4(r0,r1,r2,r3);                  
		  r4 = dc - r3;
		  r5 = dc - r2;
		  r6 = dc - r1;
		  r7 = dc - r0;
		  r0 = dc + r0;
		  r1 = dc + r1;
		  r2 = dc + r2;
		  r3 = dc + r3;		  
	       }
	       else if (orow == 12)	/* dbl3 */
	       {  register int32 s0,s1;
	          s0 = d[2];
	          MULT4(r0,r1,r2,r3);
	          MULT2(s0,s1);
	          ((float*)d)[2] = fzero;
	          r7 = s0+dc-r0;
	          r0 = s0+dc+r0;
	          r6 = s1+dc-r1;
	          r1 = s1+dc+r1;
	          s0 = dc - s0;
	          s1 = dc - s1;
	          r4 = s0 - r3;
	          r5 = s1 - r2;
	          r2 = s1 + r2;
	          r3 = s0 + r3;
	       }
	       else			/* sng2 */
	       {  r0 = d[2];
	          MULT2(r0,r1);
	          ((float*)d)[2] = fzero;
	          r2 = dc - r1;
	          r3 = dc - r0;
	          r0 = dc + r0;
	          r1 = dc + r1;
	          r4 = r3;
	          r5 = r2;
	          r6 = r1;
	          r7 = r0;
	       }
	    }
	    else if ( !(orow & 1))	/* reg4 */
	    {  register int32 r8;
	       r5 = d[3];
	       MULT4(r0,r3,r2,r1);
	       MULT4(r5,r7,r4,r6);
	       r5 = r2 - r5;  r2 = d[2];
	       r4 = r1 - r4;  r1 = d[4];
	       r7 = r0 + r7;  r0 = d[0];
	       r6 = r3 - r6;  r1 <<= 9;
	       MULT2(r2,r3);
	       r0 <<= 9;
	       ((double *)d)[0] = dblzero;
	       r8 = r0 + r1;  r0 = r0 - r1;
	       ((double *)d)[1] = dblzero;
	       r1 = r8 + r2;  r8 = r8 - r2;
	       ((double *)d)[2] = dblzero;
	       r2 = r0 + r3;  r3 = r0 - r3;
	       r0 = r1 + r7;
	       r7 = r1 - r7;
	       r1 = r2 + r6;
	       r6 = r2 - r6;
	       r2 = r3 + r5;
	       r5 = r3 - r5;
	       r3 = r8 + r4;
	       r4 = r8 - r4;
	    }
	    else			/* reg7 */
	    {  register int32 r8;
	       r5 = d[3];
	       MULT4(r0,r3,r2,r1);
	       MULT4(r5,r7,r4,r6);
	       r3 = r3 - r6;  r6 = d[5];
	       r0 = r0 + r7;  r1 = r1 - r4;  r2 = r2 - r5;
	       MULT4(r6,r4,r7,r5);
	       r1 = r1 + r4;  r4 = d[7];
	       r0 = r0 + r7;  r2 = r2 + r5;  r3 = r3 - r6;
	       MULT4(r4,r5,r6,r7);
	       r5 = r2 + r5;  r2 = d[2];
	       r7 = r0 + r7;  r0 = d[6];
	       r4 = r1 - r4;
	       r6 = r3 - r6;
	       MULT2(r2,r3);
	       MULT2(r0,r1);
	       r2 += r1;  r1 = d[4];
	       r3 -= r0;  r0 = d[0];
	       r1 <<= 9;
	       r0 <<= 9;
	       ((double *)d)[0] = dblzero;
	       r8 = r0 + r1;  r0 = r0 - r1;
	       ((double *)d)[1] = dblzero;
	       r1 = r8 + r2;  r8 = r8 - r2;
	       ((double *)d)[2] = dblzero;
	       r2 = r0 + r3;  r3 = r0 - r3;
	       ((double *)d)[3] = dblzero;
	       r0 = r1 + r7;
	       r7 = r1 - r7;
	       r1 = r2 + r6;
	       r6 = r2 - r6;
	       r2 = r3 + r5;
	       r5 = r3 - r5;
	       r3 = r8 + r4;
	       r4 = r8 - r4;
	    }
	    r0 >>= 12;
	    r1 >>= 12;
	    r2 >>= 12;
	    r3 >>= 12;
	    r4 >>= 12;
	    r5 >>= 12;
	    r6 >>= 12;
	    r7 >>= 12;
	   
	    if ( irc & 1 ) goto LB_ORowColumn;
	    	   
	    if (flags & RF_PRED)
	    {  nint k = 0x80;
	       do
	       {  register nint s0,s1;
	          k >>= 1;
	          w[0] = s0 = r0 + pred[0]; 
		  w[1] = s1 = r1 + pred[1]; s0 |= s1;
		  w[2] = s1 = r2 + pred[2]; s0 |= s1;
		  w[3] = s1 = r3 + pred[3]; s0 |= s1;
		  w[4] = s1 = r4 + pred[4]; s0 |= s1;
		  w[5] = s1 = r5 + pred[5]; s0 |= s1;
		  w[6] = s1 = r6 + pred[6]; s0 |= s1;
		  w[7] = s1 = r7 + pred[7]; s0 |= s1;
		  if ( (nuint)s0 > 255 )
		  {  w[0] = clipb[r0 + pred[0]];
		     w[1] = clipb[r1 + pred[1]];
		     w[2] = clipb[r2 + pred[2]];
		     w[3] = clipb[r3 + pred[3]];
		     w[4] = clipb[r4 + pred[4]];
		     w[5] = clipb[r5 + pred[5]];
		     w[6] = clipb[r6 + pred[6]];
		     w[7] = clipb[r7 + pred[7]];
		  }
		  w += yoffs; pred += 8;
	       } while (k);
	    }
	    else
	    {  register uint64 r64;
	       register double tdf;
	       nuint s0 = r0 | r1 | r2 | r3 | r4 | r5 | r6 | r7;
	       if ( s0 > 255 )
	       {  r0 = clipb[r0];
		  r1 = clipb[r1];
		  r2 = clipb[r2];
		  r3 = clipb[r3];
		  r4 = clipb[r4];
		  r5 = clipb[r5];
		  r6 = clipb[r6];
		  r7 = clipb[r7];
	       }

#ifndef _X86_
	       r0 = r0 << 24;
	       r1 = r1 << 16;
	       r2 = r2 << 8;
	       r0 |= r1;
	       r2 |= r3;
	       r64 = (uint64)((uint32)(r0 | r2)) << 32;
	       
	       r4 <<= 24;
	       r5 <<= 16;
	       r6 <<= 8;
	       r4 |= r5;
	       r6 |= r7;
	       r64 |= (uint32)(r4 | r6);
	       ((uint64 *)w)[0] = r64;
#else       
	       /* Little Endian */
	       r7 = r7 << 24;
	       r6 = r6 << 16;
	       r5 = r5 << 8;
	       r7 |= r6;
	       r5 |= r4;
	       ((uint32*)w)[1] = (uint32)(r7 | r5);
	       
	       r3 <<= 24;
	       r2 <<= 16;
	       r1 <<= 8;
	       r3 |= r2;
	       r1 |= r0;
	       ((uint32*)w)[0]= (uint32)(r3 | r1);
#endif
	       tdf = ((double *)w)[0];
	       w += yoffs;	    
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf; w += yoffs;
	       ((double *)w)[0] = tdf;
	    }
	    continue;
         
	    LB_ORowColumn:
         
	    if (flags & RF_PRED)
	    {  nint k;
	       for (k=0; k<8; k++)
	       {  register int32 dc = bj[k];
		  register nint s0,s1;
		  w[0] = s0 = r0 + dc + pred[0];
		  w[1] = s1 = r1 + dc + pred[1]; s0 |= s1;
		  w[2] = s1 = r2 + dc + pred[2]; s0 |= s1;
		  w[3] = s1 = r3 + dc + pred[3]; s0 |= s1;
		  w[4] = s1 = r4 + dc + pred[4]; s0 |= s1;
		  w[5] = s1 = r5 + dc + pred[5]; s0 |= s1;
		  w[6] = s1 = r6 + dc + pred[6]; s0 |= s1;
		  w[7] = s1 = r7 + dc + pred[7]; s0 |= s1;
		  if ( (nuint)s0 > 255)
		  {  w[0] = clipb[r0 + dc + pred[0]];
		     w[1] = clipb[r1 + dc + pred[1]];
		     w[2] = clipb[r2 + dc + pred[2]];
		     w[3] = clipb[r3 + dc + pred[3]];
		     w[4] = clipb[r4 + dc + pred[4]];
		     w[5] = clipb[r5 + dc + pred[5]];
		     w[6] = clipb[r6 + dc + pred[6]];
		     w[7] = clipb[r7 + dc + pred[7]];
		  }		  
		  w += yoffs; pred += 8;
	       }
	    }
	    else
	    {  nint k;
	       for (k=0; k<8; k++)
	       {  register int32 dc = bj[k];
		  register nint s0, s1;
		  w[0] = s0 = r0 + dc;
		  w[1] = s1 = r1 + dc; s0 |= s1;
		  w[2] = s1 = r2 + dc; s0 |= s1;
		  w[3] = s1 = r3 + dc; s0 |= s1;
		  w[4] = s1 = r4 + dc; s0 |= s1;
		  w[5] = s1 = r5 + dc; s0 |= s1;
		  w[6] = s1 = r6 + dc; s0 |= s1;
		  w[7] = s1 = r7 + dc; s0 |= s1;
		  if ( (nuint)s0 > 255)
		  {  w[0] = clipb[r0 + dc];
		     w[1] = clipb[r1 + dc];
		     w[2] = clipb[r2 + dc];
		     w[3] = clipb[r3 + dc];
		     w[4] = clipb[r4 + dc];
		     w[5] = clipb[r5 + dc];
		     w[6] = clipb[r6 + dc];
		     w[7] = clipb[r7 + dc];
		  }
		  w += yoffs;
	       }
	    }
	    
	    
	    continue;
	 }
      }

      LB_reg:
      
      {  nuint iptrn;
	 register int32 *j = bj;
         
         {  register nuint irow = (ptrn >> 24) & 0xfe;
            register nuint icol = (ptrn >> 11) & 0xfe;
            register nuint hfease = easy[(ptrn>>6 ) & 0xff];
            register nuint vfease = easy[(ptrn>>19) & 0xff];
            register nuint hes, ves;
            hes = hfease;  hfease <<= 1;
            ves = vfease;  vfease <<= 1;
                        
	    if (hfease == vfease)
	    {  vfease = irow ;
	       hfease = icol ;
	    }
	    hfease += 1;		/* Punish Horizontal Pass first */
	    if (hfease < vfease)
	    {  iptrn = 1 | icol | (hes<<3);
	       goto LB_HorzFirst;
	    }
	    iptrn = 1 | irow | (ves<<3);	    
	 }
	 
	 LB_VertFirst:
	 
	 do
	 {  register int32 r0,r1,r2,r3,r4,r5,r6,r7,r8;  
	    if (iptrn & 1 )
	    {  r1 = d[6*8];
	       r8 = d[7*8];
	       r6 = d[5*8];
	       r1 = r1 | r8;
	       r1 = r1 | r6;
	       r0 = d[1*8];
	       if ( r1 == 0)	/* reg4 */
	       {  r5 = d[3*8];
	          MULT4(r0,r3,r2,r1);
	          ((float *)d)[1*8] = fzero;
	          MULT4(r5,r7,r4,r6);
	          ((float *)d)[3*8] = fzero;
	          r5 = r2 - r5;  r2 = d[2*8];
	          r4 = r1 - r4;  r1 = d[4*8];
	          r7 = r0 + r7;  r0 = d[0*8];
	          r6 = r3 - r6;  r1 <<= 9;
	          ((float *)d)[0*8] = fzero;
	          MULT2(r2,r3);
	          r0 <<= 9;
	          ((float *)d)[2*8] = fzero;
	          r8 = r0 + r1;  r0 = r0 - r1;
	          ((float *)d)[4*8] = fzero;
	          r1 = r8 + r2;  r8 = r8 - r2;
	          r2 = r0 + r3;  r3 = r0 - r3;
	          j[0*8] = r1 + r7;
		  j[7*8] = r1 - r7;
		  j[1*8] = r2 + r6;
		  j[6*8] = r2 - r6;
		  j[2*8] = r3 + r5;
		  j[5*8] = r3 - r5;
		  j[3*8] = r8 + r4;
		  j[4*8] = r8 - r4;
	       }
	       else		/* reg7 */
	       {  MULT4(r0,r3,r2,r1);
	          ((float *)d)[1*8] = fzero;	          
	          MULT4(r6,r4,r7,r5);
	          ((float *)d)[5*8] = fzero;	          
	          r2 = r2 + r5;  r5 = d[3*8];
	          r1 = r1 + r4;  r0 = r0 + r7;  r3 = r3 - r6;	          
	          ((float *)d)[3*8] = fzero;	          
	          MULT4(r5,r7,r4,r6);
	          ((float *)d)[7*8] = fzero;	          
	          r1 = r1 - r4;  r4 = r8;
	          r3 = r3 - r6;  r0 = r0 + r7;  r2 = r2 - r5;
	          MULT4(r4,r5,r6,r7);
	          r5 = r2 + r5;  r2 = d[2*8];
	          r7 = r0 + r7;  r0 = d[6*8];
	          r4 = r1 - r4;  r6 = r3 - r6;
	          ((float *)d)[2*8] = fzero;	          
	          MULT2(r2,r3);
	          ((float *)d)[6*8] = fzero;	          
	          MULT2(r0,r1);
	          r2 += r1;  r1 = d[4*8];
	          r3 -= r0;  r0 = d[0*8];
	          r1 <<= 9;  r0 <<= 9;
	          ((float *)d)[0*8] = fzero;	          
	          r8 = r0 + r1;  r0 = r0 - r1;
	          r1 = r8 + r2;  r8 = r8 - r2;
	          ((float *)d)[4*8] = fzero;	          
	          r2 = r0 + r3;  r3 = r0 - r3;
		  j[0*8] = r1 + r7;
		  j[7*8] = r1 - r7;
		  j[1*8] = r2 + r6;
		  j[6*8] = r2 - r6;
		  j[2*8] = r3 + r5;
		  j[5*8] = r3 - r5;
		  j[3*8] = r8 + r4;
		  j[4*8] = r8 - r4;
	       }
	    }
	    else
	    {  r0 = d[0]<<9; d[0] = 0;
	       j[0*8] = r0; j[1*8] = r0; j[2*8] = r0; j[3*8] = r0;
	       j[4*8] = r0; j[5*8] = r0; j[6*8] = r0; j[7*8] = r0;
	    }
	    d += 1;
	    j += 1;
	    iptrn >>= 1;
	 } while (iptrn > 7);
	 iptrn &= 3;
	 	    
	 d = bj;
	 if ( iptrn == 2 )	/* reg4 */
	 {  register int32 r0,r1,r2,r3,r4,r5,r6,r7,r8;
	    nint k = 0x80;  
	    do
	    {  k >>= 1;	       
	       r0 = d[1];
	       r5 = d[3];
	       MULT4(r0,r3,r2,r1);
	       MULT4(r5,r7,r4,r6);
	       r5 = r2 - r5;  r2 = d[2];
	       r4 = r1 - r4;  r1 = d[4];
	       r7 = r0 + r7;  r0 = d[0];
	       r6 = r3 - r6;  r1 <<= 9;
	       MULT2(r2,r3);
	       r0 <<= 9;
	       r8 = r0 + r1;  r0 = r0 - r1;
	       r1 = r8 + r2;  r8 = r8 - r2;
	       r2 = r0 + r3;  r3 = r0 - r3;
	       r0 = (r1 + r7) >> 21;
	       r7 = (r1 - r7) >> 21;
	       r1 = (r2 + r6) >> 21;
	       r6 = (r2 - r6) >> 21;
	       r2 = (r3 + r5) >> 21;
	       r5 = (r3 - r5) >> 21;
	       r3 = (r8 + r4) >> 21;
	       r4 = (r8 - r4) >> 21;
	       if (flags & RF_PRED)
	       {   r0 += pred[0];
		   r7 += pred[7];
		   r1 += pred[1];
		   r6 += pred[6];
		   r2 += pred[2];
		   r5 += pred[5];
		   r3 += pred[3];
		   r4 += pred[4];
		   pred += 8;
	       }
	       d += 8;
	       w[0] = r0;  r8 = r0 | r1;
	       w[1] = r1;  r8 |= r2;
	       w[2] = r2;  r8 |= r3;
	       w[3] = r3;  r8 |= r4;
	       w[4] = r4;  r8 |= r5;
	       w[5] = r5;  r8 |= r6;
	       w[6] = r6;  r8 |= r7;
	       w[7] = r7;
	       if ( (uint32)r8 > 255)  /* Clipping required */
	       {  w[0] = clipb[r0];
	          w[1] = clipb[r1];
	          w[2] = clipb[r2];
	          w[3] = clipb[r3];
	          w[4] = clipb[r4];
	          w[5] = clipb[r5];
	          w[6] = clipb[r6];
	          w[7] = clipb[r7];
	       }
	       w += yoffs;
	    } while ( k );
	 }
	 else if (iptrn == 1)	/* dbl3 */
	 {  register int32 r0,r1,r2,r3,r4,r5,r6,r7,s0,s1;
	    nint k = 0x80;  
	    do
	    {  k >>= 1;
	       r0 = d[1];
	       s0 = d[2];
	       MULT4(r0,r1,r2,r3);
	       r4  = d[0]<<9;
	       MULT2(s0,s1);
	       r7 = (s0+r4-r0) >> 21;
	       r0 = (s0+r4+r0) >> 21;
	       r6 = (s1+r4-r1) >> 21;
	       r1 = (s1+r4+r1) >> 21;
	       s0 = r4 - s0;
	       s1 = r4 - s1;
	       r4 = (s0 - r3) >> 21;
	       r5 = (s1 - r2) >> 21;
	       r2 = (s1 + r2) >> 21;
	       r3 = (s0 + r3) >> 21;
	       if (flags & RF_PRED)
	       {   r0 += pred[0];
		   r7 += pred[7];
		   r1 += pred[1];
		   r6 += pred[6];
		   r2 += pred[2];
		   r5 += pred[5];
		   r3 += pred[3];
		   r4 += pred[4];
		   pred += 8;
	       }
	       d += 8;
	       w[0] = r0;  s0 = r0 | r1;
	       w[1] = r1;  s0 |= r2;
	       w[2] = r2;  s0 |= r3;
	       w[3] = r3;  s0 |= r4;
	       w[4] = r4;  s0 |= r5;
	       w[5] = r5;  s0 |= r6;
	       w[6] = r6;  s0 |= r7;
	       w[7] = r7;
	       if ( (uint32)s0 > 255)  /* Clipping required */
	       {  w[0] = clipb[r0];
	          w[1] = clipb[r1];
	          w[2] = clipb[r2];
	          w[3] = clipb[r3];
	          w[4] = clipb[r4];
	          w[5] = clipb[r5];
	          w[6] = clipb[r6];
	          w[7] = clipb[r7];
	       }
	       w += yoffs;
	    } while (k);
	 }
	 else
	 {  register int32 r0,r1,r2,r3,r4,r5,r6,r7,r8;
	    nint k = 0x80;  
	    do
	    {  k >>= 1;
	       r0 = d[1];	       
	       r5 = d[3];
	       MULT4(r0,r3,r2,r1);
	       MULT4(r5,r7,r4,r6);
	       r3 = r3 - r6;  r6 = d[5];
	       r0 = r0 + r7;  r1 = r1 - r4;  r2 = r2 - r5;
	       MULT4(r6,r4,r7,r5);
	       r1 = r1 + r4;  r4 = d[7];
	       r0 = r0 + r7;  r2 = r2 + r5;  r3 = r3 - r6;
	       MULT4(r4,r5,r6,r7);
	       r5 = r2 + r5;  r2 = d[2];
	       r7 = r0 + r7;  r0 = d[6];
	       r4 = r1 - r4;  r6 = r3 - r6;
	       MULT2(r2,r3);
	       MULT2(r0,r1);
	       r2 += r1;  r1 = d[4];
	       r3 -= r0;  r0 = d[0];
	       r1 <<= 9;  r0 <<= 9;
	       r8 = r0 + r1;  r0 = r0 - r1;
	       r1 = r8 + r2;  r8 = r8 - r2;
	       r2 = r0 + r3;  r3 = r0 - r3;
	       r0 = (r1 + r7) >> 21;
	       r7 = (r1 - r7) >> 21;
	       r1 = (r2 + r6) >> 21;
	       r6 = (r2 - r6) >> 21;
	       r2 = (r3 + r5) >> 21;
	       r5 = (r3 - r5) >> 21;
	       r3 = (r8 + r4) >> 21;
	       r4 = (r8 - r4) >> 21;
	       if (flags & RF_PRED)
	       {   r0 += pred[0];
		   r7 += pred[7];
		   r1 += pred[1];
		   r6 += pred[6];
		   r2 += pred[2];
		   r5 += pred[5];
		   r3 += pred[3];
		   r4 += pred[4];
		   pred += 8;
	       }
	       d += 8;
	       w[0] = r0;  r8 = r0 | r1;
	       w[1] = r1;  r8 |= r2;
	       w[2] = r2;  r8 |= r3;
	       w[3] = r3;  r8 |= r4;
	       w[4] = r4;  r8 |= r5;
	       w[5] = r5;  r8 |= r6;
	       w[6] = r6;  r8 |= r7;
	       w[7] = r7;
	       if ( (uint32)r8 > 255)  /* Clipping required */
	       {  w[0] = clipb[r0];
	          w[1] = clipb[r1];
	          w[2] = clipb[r2];
	          w[3] = clipb[r3];
	          w[4] = clipb[r4];
	          w[5] = clipb[r5];
	          w[6] = clipb[r6];
	          w[7] = clipb[r7];
	       }
	       w += yoffs;
	    } while ( k );
	 }
	 continue;

	 LB_HorzFirst:
	 
	 do
	 {  register int32 r0,r1,r2,r3,r4,r5,r6,r7,r8;  
	    if (iptrn & 1 )
	    {  r1 = d[6];
	       r8 = d[7];
	       r6 = d[5];
	       r1 = r1 | r8;
	       r1 = r1 | r6;
	       r0 = d[1];
	       if ( r1 == 0)	/* reg4 */
	       {  r5 = d[3];
	          MULT4(r0,r3,r2,r1);
	          ((float *)d)[1] = fzero;
	          MULT4(r5,r7,r4,r6);
	          ((float *)d)[3] = fzero;
	          r5 = r2 - r5;  r2 = d[2];
	          r4 = r1 - r4;  r1 = d[4];
	          r7 = r0 + r7;  r0 = d[0];
	          r6 = r3 - r6;  r1 <<= 9;
	          ((float *)d)[0] = fzero;
	          MULT2(r2,r3);
	          r0 <<= 9;
	          ((float *)d)[2] = fzero;
	          r8 = r0 + r1;  r0 = r0 - r1;
	          ((float *)d)[4] = fzero;
	          r1 = r8 + r2;  r8 = r8 - r2;
	          r2 = r0 + r3;  r3 = r0 - r3;
	          j[0*8] = r1 + r7;
		  j[7*8] = r1 - r7;
		  j[1*8] = r2 + r6;
		  j[6*8] = r2 - r6;
		  j[2*8] = r3 + r5;
		  j[5*8] = r3 - r5;
		  j[3*8] = r8 + r4;
		  j[4*8] = r8 - r4;
	       }
	       else		/* reg7 */
	       {  MULT4(r0,r3,r2,r1);
	          ((float *)d)[1] = fzero;	          
	          MULT4(r6,r4,r7,r5);
	          ((float *)d)[5] = fzero;	          
	          r2 = r2 + r5;  r5 = d[3];
	          r1 = r1 + r4;  r0 = r0 + r7;  r3 = r3 - r6;	          
	          ((float *)d)[3] = fzero;	          
	          MULT4(r5,r7,r4,r6);
	          ((float *)d)[7] = fzero;	          
	          r1 = r1 - r4;  r4 = r8;
	          r3 = r3 - r6;  r0 = r0 + r7;  r2 = r2 - r5;
	          MULT4(r4,r5,r6,r7);
	          r5 = r2 + r5;  r2 = d[2];
	          r7 = r0 + r7;  r0 = d[6];
	          r4 = r1 - r4;  r6 = r3 - r6;
	          ((float *)d)[2] = fzero;	          
	          MULT2(r2,r3);
	          ((float *)d)[6] = fzero;	          
	          MULT2(r0,r1);
	          r2 += r1;  r1 = d[4];
	          r3 -= r0;  r0 = d[0];
	          r1 <<= 9;  r0 <<= 9;
	          ((float *)d)[0] = fzero;	          
	          r8 = r0 + r1;  r0 = r0 - r1;
	          r1 = r8 + r2;  r8 = r8 - r2;
	          ((float *)d)[4] = fzero;	          
	          r2 = r0 + r3;  r3 = r0 - r3;
		  j[0*8] = r1 + r7;
		  j[7*8] = r1 - r7;
		  j[1*8] = r2 + r6;
		  j[6*8] = r2 - r6;
		  j[2*8] = r3 + r5;
		  j[5*8] = r3 - r5;
		  j[3*8] = r8 + r4;
		  j[4*8] = r8 - r4;
	       }
	    }
	    else
	    {  r0 = d[0]<<9; d[0] = 0;
	       j[0*8] = r0; j[1*8] = r0; j[2*8] = r0; j[3*8] = r0;
	       j[4*8] = r0; j[5*8] = r0; j[6*8] = r0; j[7*8] = r0;
	    }
	    d += 8;
	    j += 1;
	    iptrn >>= 1;
	 } while (iptrn > 7);
	 iptrn &= 3;
	 
	 d = bj;
	 if ( iptrn == 2 )	/* reg4 */
	 {  register int32 r0,r1,r2,r3,r4,r5,r6,r7,r8;
	    nint k = 0x80;
	    ubyte *b = (ubyte *)bdf;  
	    do
	    {  k >>= 1;	       
	       r0 = d[1];
	       r5 = d[3];
	       MULT4(r0,r3,r2,r1);
	       MULT4(r5,r7,r4,r6);
	       r5 = r2 - r5;  r2 = d[2];
	       r4 = r1 - r4;  r1 = d[4];
	       r7 = r0 + r7;  r0 = d[0];
	       r6 = r3 - r6;  r1 <<= 9;
	       MULT2(r2,r3);
	       r0 <<= 9;
	       r8 = r0 + r1;  r0 = r0 - r1;
	       r1 = r8 + r2;  r8 = r8 - r2;
	       r2 = r0 + r3;  r3 = r0 - r3;
	       r0 = (r1 + r7) >> 21;
	       r7 = (r1 - r7) >> 21;
	       r1 = (r2 + r6) >> 21;
	       r6 = (r2 - r6) >> 21;
	       r2 = (r3 + r5) >> 21;
	       r5 = (r3 - r5) >> 21;
	       r3 = (r8 + r4) >> 21;
	       r4 = (r8 - r4) >> 21;
	       if (flags & RF_PRED)
	       {   r0 += pred[0*8];
		   r7 += pred[7*8];
		   r1 += pred[1*8];
		   r6 += pred[6*8];
		   r2 += pred[2*8];
		   r5 += pred[5*8];
		   r3 += pred[3*8];
		   r4 += pred[4*8];
		   pred += 1;
	       }
	       d += 8;
	       b[0*8] = r0;  r8 = r0 | r1;
	       b[1*8] = r1;  r8 |= r2;
	       b[2*8] = r2;  r8 |= r3;
	       b[3*8] = r3;  r8 |= r4;
	       b[4*8] = r4;  r8 |= r5;
	       b[5*8] = r5;  r8 |= r6;
	       b[6*8] = r6;  r8 |= r7;
	       b[7*8] = r7;
	       if ( (uint32)r8 > 255)  /* Clipping required */
	       {  b[0*8] = clipb[r0];
	          b[1*8] = clipb[r1];
	          b[2*8] = clipb[r2];
	          b[3*8] = clipb[r3];
	          b[4*8] = clipb[r4];
	          b[5*8] = clipb[r5];
	          b[6*8] = clipb[r6];
	          b[7*8] = clipb[r7];
	       }
	       b += 1;
	    } while (k);
	 }
	 else if (iptrn == 1)	/* dbl3 */
	 {  register int32 r0,r1,r2,r3,r4,r5,r6,r7,s0,s1;
	    nint k = 0x80;  
	    ubyte *b = (ubyte *)bdf;
	    
	    do
	    {  k >>= 1;
	       r0 = d[1];
	       s0 = d[2];
	       MULT4(r0,r1,r2,r3);
	       r4  = d[0]<<9;
	       MULT2(s0,s1);
	       r7 = (s0+r4-r0) >> 21;
	       r0 = (s0+r4+r0) >> 21;
	       r6 = (s1+r4-r1) >> 21;
	       r1 = (s1+r4+r1) >> 21;
	       s0 = r4 - s0;
	       s1 = r4 - s1;
	       r4 = (s0 - r3) >> 21;
	       r5 = (s1 - r2) >> 21;
	       r2 = (s1 + r2) >> 21;
	       r3 = (s0 + r3) >> 21;
	       if (flags & RF_PRED)
	       {   r0 += pred[0*8];
		   r7 += pred[7*8];
		   r1 += pred[1*8];
		   r6 += pred[6*8];
		   r2 += pred[2*8];
		   r5 += pred[5*8];
		   r3 += pred[3*8];
		   r4 += pred[4*8];
		   pred += 1;
	       }
	       d += 8;
	       b[0*8] = r0;  s0 = r0 | r1;
	       b[1*8] = r1;  s0 |= r2;
	       b[2*8] = r2;  s0 |= r3;
	       b[3*8] = r3;  s0 |= r4;
	       b[4*8] = r4;  s0 |= r5;
	       b[5*8] = r5;  s0 |= r6;
	       b[6*8] = r6;  s0 |= r7;
	       b[7*8] = r7;
	       if ( (uint32)s0 > 255)  /* Clipping required */
	       {  b[0*8] = clipb[r0];
	          b[1*8] = clipb[r1];
	          b[2*8] = clipb[r2];
	          b[3*8] = clipb[r3];
	          b[4*8] = clipb[r4];
	          b[5*8] = clipb[r5];
	          b[6*8] = clipb[r6];
	          b[7*8] = clipb[r7];
	       }
	       b += 1;
	    } while (k);
	 }
	 else
	 {  register int32 r0,r1,r2,r3,r4,r5,r6,r7,r8;
	    ubyte *b = (ubyte *)bdf;
	    nint k = 0x80;  
	    do
	    {  k >>= 1;
	       r0 = d[1];	       
	       r5 = d[3];
	       MULT4(r0,r3,r2,r1);
	       MULT4(r5,r7,r4,r6);
	       r3 = r3 - r6;  r6 = d[5];
	       r0 = r0 + r7;  r1 = r1 - r4;  r2 = r2 - r5;
	       MULT4(r6,r4,r7,r5);
	       r1 = r1 + r4;  r4 = d[7];
	       r0 = r0 + r7;  r2 = r2 + r5;  r3 = r3 - r6;
	       MULT4(r4,r5,r6,r7);
	       r5 = r2 + r5;  r2 = d[2];
	       r7 = r0 + r7;  r0 = d[6];
	       r4 = r1 - r4;  r6 = r3 - r6;
	       MULT2(r2,r3);
	       MULT2(r0,r1);
	       r2 += r1;  r1 = d[4];
	       r3 -= r0;  r0 = d[0];
	       r1 <<= 9;  r0 <<= 9;
	       r8 = r0 + r1;  r0 = r0 - r1;
	       r1 = r8 + r2;  r8 = r8 - r2;
	       r2 = r0 + r3;  r3 = r0 - r3;
	       r0 = (r1 + r7) >> 21;
	       r7 = (r1 - r7) >> 21;
	       r1 = (r2 + r6) >> 21;
	       r6 = (r2 - r6) >> 21;
	       r2 = (r3 + r5) >> 21;
	       r5 = (r3 - r5) >> 21;
	       r3 = (r8 + r4) >> 21;
	       r4 = (r8 - r4) >> 21;
	       if (flags & RF_PRED)
	       {   r0 += pred[0*8];
		   r7 += pred[7*8];
		   r1 += pred[1*8];
		   r6 += pred[6*8];
		   r2 += pred[2*8];
		   r5 += pred[5*8];
		   r3 += pred[3*8];
		   r4 += pred[4*8];
		   pred += 1;
	       }
	       d += 8;
	       b[0*8] = r0;  r8 = r0 | r1;
	       b[1*8] = r1;  r8 |= r2;
	       b[2*8] = r2;  r8 |= r3;
	       b[3*8] = r3;  r8 |= r4;
	       b[4*8] = r4;  r8 |= r5;
	       b[5*8] = r5;  r8 |= r6;
	       b[6*8] = r6;  r8 |= r7;
	       b[7*8] = r7;
	       if ( (uint32)r8 > 255)  /* Clipping required */
	       {  b[0*8] = clipb[r0];
	          b[1*8] = clipb[r1];
	          b[2*8] = clipb[r2];
	          b[3*8] = clipb[r3];
	          b[4*8] = clipb[r4];
	          b[5*8] = clipb[r5];
	          b[6*8] = clipb[r6];
	          b[7*8] = clipb[r7];
	       }
	       b += 1;
	    } while (k);
	 }
	 
	 ((double *)w)[0] = bdf[0]; w += yoffs;
	 ((double *)w)[0] = bdf[1]; w += yoffs;
	 ((double *)w)[0] = bdf[2]; w += yoffs;
	 ((double *)w)[0] = bdf[3]; w += yoffs;
	 ((double *)w)[0] = bdf[4]; w += yoffs;
	 ((double *)w)[0] = bdf[5]; w += yoffs;
	 ((double *)w)[0] = bdf[6]; w += yoffs;
	 ((double *)w)[0] = bdf[7];
	 pred += 56;
	 
      } 	/* Inner */   
   }  		/* for blocks */
}

