/*
 * @(#)mp_disp.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <math.h>
#include "mp_mpd.h"

#define	 RGBCLIP_BASE	256
#define  COLORBASE	13

uint64 gf12cr[256], gf12cb[256];
ubyte  gfy[256];
ubyte  cliprgb[768];
uint32 rgbbf[2][2048];


void
dcnvclp(ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
	   nint xcnt, nint ycnt, nint shsz, nint dhsz,
	   nint clipx0, nint clipy0, nint clipx1, nint clipy1,
	   nint blank, nint zoom)
{  register ubyte *plm2 = plm + shsz;
   register nint x, yy=0;
 
   do
   {  for (x=0; x< (xcnt<<1); x+=2)
      {  register uint32 pel;
	 register ubyte *clipr, *clipb, *clipg;
	 register nuint lm0, lm1, lm2, lm3;

         uint64 r0, r1;  
	 r0 = gf12cr[pcr[0]];
	 r1 = gf12cb[pcb[0]];
         clipr = (ubyte *) (r0>>32);
	 clipb = (ubyte *) (r1>>32);
	 clipg = (ubyte *) ((uint32)r1 - (uint32)r0); 
	 
	 lm0 = plm[0];
	 lm1 = plm[1];
	 lm2 = plm2[0];
	 lm0 = gfy[lm0];
	 lm3 = plm2[1];
	 lm1 = gfy[lm1];
	 lm2 = gfy[lm2];
	 lm3 = gfy[lm3];
	 
	 pel  = *(clipr+lm0);
	 pel |= *(clipg+lm0) << 8;
	 pel |= *(clipb+lm0) << 16;
	 rgbbf[0][x] = pel;
	 pel  = *(clipr+lm1);
	 pel |= *(clipg+lm1) << 8;
	 pel |= *(clipb+lm1) << 16;
	 rgbbf[0][x+1] = pel;
	 pel  = *(clipr+lm2);
	 pel |= *(clipg+lm2) << 8;
	 pel |= *(clipb+lm2) << 16;
	 rgbbf[1][x] = pel;
	 pel  = *(clipr+lm3);
	 pel |= *(clipg+lm3) << 8;
	 pel |= *(clipb+lm3) << 16;
	 rgbbf[1][x+1] = pel;

	 pcr   += 1;
	 pcb   += 1;
	 plm   += 2;
	 plm2  += 2;	      
      }
      pcr = pcr - xcnt + (shsz>>1);
      pcb = pcb - xcnt + (shsz>>1);
      plm = plm - (xcnt<<1) + (shsz<<1);
      plm2 = plm + shsz;
      
      if (zoom == 1)
      {  nint n;
	 for (n=0; n<2; n++)
	 {  nint k, oyy = yy;
	    yy++;
	    if (oyy <  clipy0 )
	    {  prgb = (uint32 *)((ubyte *)prgb + dhsz);
	       continue;
	    }
	    if (oyy >= clipy1 ) break;
	    for (k=clipx0; k<clipx1; k++) prgb[k] = rgbbf[n][k];
	    prgb = (uint32 *)((ubyte *)prgb + dhsz);
	 }
      }
      else if (zoom == 2)
      {  nint n;
         for (n=0; n<4; n++)
	 {  nint cnt, r0, oyy = yy;
	    uint32 *ps, *pd;
	    yy++;
	    pd = prgb;
	    prgb = (uint32 *)((ubyte *)prgb + dhsz);
	    if (oyy <  clipy0 ) continue;
	    if (oyy >= clipy1 ) break;
	    if ((n & 1) && blank) continue;
	    
	    ps = &rgbbf[n >> 1][0];
	    cnt = clipx1 - clipx0;
	    if ( clipx1 & 1 )
	    {  pd[clipx1 - 1] = ps[clipx1>>1];
	       cnt--;
	    }
	    
	    r0 = clipx0 >> 1;
	    pd += r0 << 1;
	    ps += r0;
	    r0 = clipx0 & 1;
	    
	    if (r0)
	    {  pd[1] = ps[0];
	       pd += 2; ps += 1;
	       cnt--;
	    }
	    cnt >>= 1; 
	    while (cnt--)
	    {  uint32 rgb = *ps++;
	       pd[0] = rgb;
	       pd[1] = rgb;
	       pd += 2;
	    }
	    
	 }
      }
      else
      {  nint n;
         for (n=0; n<6; n++)
	 {  nint r0, cnt, oyy = yy;
	    uint32 *ps, *pd;
	    yy++;
	    pd = prgb;
	    prgb = (uint32 *)((ubyte *)prgb + dhsz);
	    if (oyy <  clipy0 ) continue;
	    if (oyy >= clipy1 ) break;
	    if ((n==1 || n==4) && blank) continue;
	    
	    ps = &rgbbf[n/3][0];
	    cnt = clipx1 - clipx0;
	    r0 = clipx1 % 3;
	    if ( r0 )
	    {  cnt -= r0;
	       while (r0)
	       {  pd[clipx1 - r0] = ps[clipx1/3];
	          r0--;
	       }
	    }
	    
	    r0 = clipx0 / 3;
	    pd += r0 * 3;
	    ps += r0;
	    r0 = clipx0 % 3;
	    
	    if (r0)
	    {  cnt -= 3-r0;
	       while (r0 < 3)
	       {  pd[r0] = ps[0];
	          r0++;
	       }
	       pd += 3; ps += 1;
	    }
	    cnt /= 3; 
	    while (cnt--)
	    {  uint32 rgb = *ps++;
	       pd[0] = rgb;
	       pd[1] = rgb;
	       pd[2] = rgb;
	       pd += 3;
	    }	    
	 }
      }
   } while (--ycnt);
}
      
    
void 
mp_initcol24(float adjlum, float adjsat, float adjgam)
{  register nint k;
   register ubyte *clipbase;
   register float tf;
   
   for (k=0; k<256; k++) 
   {  cliprgb[k] = 0;
      cliprgb[k+256] = (ubyte)(255*pow(k/255.0,(double)adjgam));
      cliprgb[k+512] = 255;
   }
   for (k=16; k<236; k++) 
   {  tf = (k-16)*(adjlum*255.0/220.0);
      if (tf > 255) tf = 255;
      gfy[k] = (ubyte)tf;
   }
   
   for (k=0; k<16; k++) gfy[k] = 0;
   tf = 255 * adjlum;
   if (tf > 255) tf = 255;
   for (k=236; k<256; k++) gfy[k] = (ubyte)tf;
   	
   clipbase = cliprgb + RGBCLIP_BASE;
   for (k=0; k<256; k++)
   {  register float crf, cbf;
      tf = (adjsat * (k-128)) / 224.0;
      if (tf < -0.5) tf = -0.5;
      if (tf > 0.5 ) tf =  0.5;
      
      crf = tf * 1.402 * 255;     
      cbf = tf * 1.772 * 255;      
      gf12cr[k] = 
         (uint64)((nint)crf + clipbase)<<32 |	/* f1cr */
         (uint32)((int32)(crf * (0.299/0.587)));  /* f2cr */
      
      gf12cb[k] = 
         (uint64)((nint)cbf + clipbase)<<32 |		  /* f1cr */
         (uint32)(clipbase - (nint)(cbf *(0.114/0.587))); /* clipbase - f2cb*/    
   }
}



static int16 qntchr[11] = {-112,-89,-67,-44,-22,0,22,44,67,89,112};
static int16 qntlum[11] = {0,22,44,66,88,110,132,154,176,198,220};

ubyte clkup[11][16][16];
ubyte gqntcr[288];
ubyte gqntcb[288];
ubyte gqntlm[288];


nint
mp_initcol8(ubyte csmp[][3], float gamma_adj)
{  register int k,m,n,i,mm,nn,rv,q;
   float fcr,fcb,flm,tcr,tcb,red,green,blue,tf,dist;  
   
 
   for (k=1;k<11;k++)
      for (m=qntchr[k-1]+128; m<qntchr[k]+128; m++) gqntcr[m] = k-1;
   for (k=1;k<11;k++)
      for (m=qntlum[k-1]+16; m<qntlum[k]+16; m++) gqntlm[m] = k-1;
   
   for (k=0;k<qntchr[0]+128;k++) gqntcr[k] = 0;
   for (k=qntchr[10]+128;k<288;k++) gqntcr[k] = 10;
   
   for (k=0;k<qntlum[0]+16;k++) gqntlm[k] = 0;
   for (k=qntlum[10]+16;k<288;k++) gqntlm[k] = 10;
   

   for (k=0; k<288; k++) gqntcb[k] = gqntcr[k]<<4;
   i = 0;
   for (k=0;k<11;k++)
   for (m=0;m<11;m++)
   for (n=0;n<11;n++)
   {  fcr = 0.975*qntchr[m] /(224.0 * 0.713); /*Desaturate little bit*/
      fcb = 0.975*qntchr[n] /(224.0 * 0.564);
      flm = 1.0*qntlum[k] /220.0;	     /*Deilluminate littlebit*/
      
      red = flm + fcr;
      if (red<0 || red>1) continue;
      blue = flm + fcb;
      if (blue<0 || blue>1) continue;
      green = (flm - 0.299*red - 0.114*blue) / 0.587;
      if (green<0 || green>1) continue;

      red   = pow(red,gamma_adj);
      green = pow(green,gamma_adj);
      blue  = pow(blue,gamma_adj);

      csmp[i][0] = 255 * red;
      csmp[i][1] = 255 * green;
      csmp[i][2] = 255 * blue;
      
      
      clkup[n][m][k] = i+COLORBASE;
      i++;
   }
  
   rv = i;
  
   /* Now fill in the blanks -Clipped entries- */  
   for (k=0;k<11;k++)
   for (m=0;m<11;m++)
   for (n=0;n<11;n++)
   {  flm = 1.0*qntlum[k] /220.0;
      q = 16;
      do
      {	 tcr = q*qntchr[m]/16.0;
	 tcb = q*qntchr[n]/16.0;
	 dist = 9999;
	 for (i=0;i<11;i++)
	 {  tf = fabs(tcr - qntchr[i]);
	    if (tf<dist) 
	    {  dist = tf;
	       mm = i;
	    }
	 }
	 
	 dist = 9999;
	 for (i=0;i<11;i++)
	 {  tf = fabs(tcb - qntchr[i]);
	    if (tf<dist) 
	    {  dist = tf;
	       nn = i;
	    }
	 }
	 
	 fcr = 0.975*qntchr[mm] /(224.0 * 0.713); 
	 fcb = 0.975*qntchr[nn] /(224.0 * 0.564);
	 red = flm + fcr;
	 blue = flm + fcb;
	 green = (flm - 0.299*red - 0.114*blue) / 0.587;
	 q--;
      }	while (red<0 || red>1 || green<0 || green>1 || blue<0 || blue >1); 

      clkup[n][m][k]  = clkup[nn][mm][k];      
   }     
   return(rv);
}


static int16 zqchr[16] =
   {-112,-97,-82,-67,-52,-37,-22,-7,8,23,38,53,68,83,98,112};
static int16 zqlum[16] =
   {0,14,29,44,58,73,88,102,117,132,146,161,176,190,205,220};

static ubyte xx[16][2] =
   {0,0,1,0,2,1,2,2,3,2,4,3,4,4,5,4,6,5,6,6,7,6,8,7,8,8,9,8,10,9,10,10};
static ubyte zqcr[288], zqlm[288], zqcb[288];
static uint16 zzlk[16][16][16];


void
mp_initcolz8()
{  register nint k,m,n;

   for (k=1;k<16;k++)
      for (m=zqchr[k-1]+128; m<zqchr[k]+128; m++) zqcr[m] = k-1;
   for (k=1;k<16;k++)
      for (m=zqlum[k-1]+16; m<zqlum[k]+16; m++) zqlm[m] = k-1;
   
   for (k=0;k<zqchr[0]+128;k++) zqcr[k] = 0;
   for (k=zqchr[15]+128;k<288;k++) zqcr[k] = 15;
   
   for (k=0;k<zqlum[0]+16;k++) zqlm[k] = 0;
   for (k=zqlum[15]+16;k<288;k++) zqlm[k] = 15;
   
   for (k=0; k<288; k++) 
   {  zqcb[k] = zqcr[k]<<4;
      zqlm[k] = zqlm[k]<<1;
   }
   
   for (k=0;k<16;k++)
   for (m=0;m<16;m++)
   for (n=0;n<16;n++)
      zzlk[k][m][n] = (clkup[xx[k][0]][xx[m][0]][xx[n][0]] << 8 ) |
		      (clkup[xx[k][1]][xx[m][1]][xx[n][1]] );

}



#define	 DC0   3
#define	 DC1   6
#define	 DC2   12
#define	 DC3   9

#define	 DL0   3
#define	 DL1   6
#define	 DL2   12
#define	 DL3   9

void
dicnv2( ubyte *lm, ubyte *cr, ubyte *cb, uint32 *dispm, nint xcnt, nint ycnt,
		nint shsz, nint dhsz)
{  register ubyte *lm1, *lm2, *cr1, *cb1;
   register uint32 *w1,*w2, *w3, *w4;
   register nint xx;
   register uint16 *lk;
   
   ycnt >>= 1;
   lk = (uint16 *)zzlk;
   do
   {  lm1 = lm;
      lm2 = lm1 + shsz;
      lm += shsz<<1;	/* skip 2 scanlines */

      cr1 = cr;
      cr += shsz>>1;	/* skip 1 scanline */
      
      cb1 = cb;
      cb += shsz>>1;
      
      w1 = dispm;
      w2 = w1 + (dhsz>>2);
      w3 = w2 + (dhsz>>2);
      w4 = w3 + (dhsz>>2);
      dispm += dhsz;
      xx = xcnt>>3;
      do
      {	 register uint32 r3;
	 register uint32 r0,r1,r2;
	 register ubyte *qcr,*qcb;

	 qcr = cr1[0] + zqcr;
	 qcb = cb1[0] + zqcb;
	 r0 = qcr[DC0];
	 r1 = qcb[DC0];
	 r2 = lm1[0];
	 r0 |= r1;
	 r2 = zqlm[r2+DL0];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC1];
	 r3 = *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC1];
	 r2 = lm1[1];
	 r3 <<= 16;
	 r0 |= r1;
	 r2 = zqlm[r2+DL1];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC2];
	 r3 |= *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC2];
	 r2 = lm2[0];
	 w2[0] = w1[0] = r3;
	 r0 |= r1;
	 r2 = zqlm[r2+DL2];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC3];
	 r3 = *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC3];
	 r2 = lm2[1];
	 r3 <<= 16;
	 r0 |= r1;
	 r2 = zqlm[r2+DL3];
	 r0 <<= 5;
	 r2 |= r0;
	 r3 |= *(uint16*)((ubyte*)lk+r2);
	 
	 
	 qcr = cr1[1] + zqcr;
	 qcb = cb1[1] + zqcb;
	 r0 = qcr[DC0];
	 r1 = qcb[DC0];
	 r2 = lm1[2];
	 w4[0] = w3[0] = r3;
	 r0 |= r1;
	 r2 = zqlm[r2+DL0];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC1];
	 r3 = *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC1];
	 r2 = lm1[3];
	 r3 <<= 16;
	 r0 |= r1;
	 r2 = zqlm[r2+DL1];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC2];
	 r3 |= *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC2];
	 r2 = lm2[2];
	 w2[1] = w1[1] = r3;
	 r0 |= r1;
	 r2 = zqlm[r2+DL2];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC3];
	 r3 = *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC3];
	 r2 = lm2[3];
	 r3 <<= 16;
	 r0 |= r1;
	 r2 = zqlm[r2+DL3];
	 r0 <<= 5;
	 r2 |= r0;
	 r3 |= *(uint16*)((ubyte*)lk+r2);
	 
	 qcr = cr1[2] + zqcr;
	 qcb = cb1[2] + zqcb;
	 r0 = qcr[DC0];
	 r1 = qcb[DC0];
	 r2 = lm1[4];
	 w4[1] = w3[1] = r3;
	 r0 |= r1;
	 r2 = zqlm[r2+DL0];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC1];
	 r3 = *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC1];
	 r2 = lm1[5];
	 r3 <<= 16;
	 r0 |= r1;
	 r2 = zqlm[r2+DL1];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC2];
	 r3 |= *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC2];
	 r2 = lm2[4];
	 w2[2] = w1[2] = r3;
	 r0 |= r1;
	 r2 = zqlm[r2+DL2];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC3];
	 r3 = *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC3];
	 r2 = lm2[5];
	 r3 <<= 16;
	 r0 |= r1;
	 r2 = zqlm[r2+DL3];
	 r0 <<= 5;
	 r2 |= r0;
	 r3 |= *(uint16*)((ubyte*)lk+r2);
	 
	 
	 
	 qcr = cr1[3] + zqcr;
	 qcb = cb1[3] + zqcb;
	 r0 = qcr[DC0];
	 r1 = qcb[DC0];
	 r2 = lm1[6];
	 w4[2] = w3[2] = r3;
	 r0 |= r1;
	 r2 = zqlm[r2+DL0];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC1];
	 r3 = *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC1];
	 r2 = lm1[7];
	 r3 <<= 16;
	 r0 |= r1;
	 r2 = zqlm[r2+DL1];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC2];
	 r3 |= *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC2];
	 r2 = lm2[6];
	 w2[3] = w1[3] = r3;
	 r0 |= r1;
	 r2 = zqlm[r2+DL2];
	 r0 <<= 5;
	 r2 |= r0;
	 r0 = qcr[DC3];
	 r3 = *(uint16*)((ubyte*)lk+r2);
	 r1 = qcb[DC3];
	 r2 = lm2[7];
	 r3 <<= 16;
	 r0 |= r1;
	 r2 = zqlm[r2+DL3];
	 r0 <<= 5;
	 r2 |= r0;
	 r3 |= *(uint16*)((ubyte*)lk+r2);
	 
	 cr1 += 4;
	 cb1 += 4;
	 w4[3] = w3[3] = r3;
	 lm1 += 8;
	 lm2 += 8;
	 w1 += 4;
	 w2 += 4;
	 w3 += 4;
	 w4 += 4;
	 xx--;
      } while (xx);
      
      ycnt--;
   } while (ycnt);
}

