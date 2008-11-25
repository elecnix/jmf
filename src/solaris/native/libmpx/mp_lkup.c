/*
 * @(#)mp_lkup.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <math.h>
#include "mp_types.h"

/*************************************************************************
   This routine generates the 128 entry lookup table which is used
   for decoding the luminance DC value in MPEG Intra Blocks.
   Each entry is an 8bit byte. 
   Table entries are expected to be interpreted as "signed byte" entries,
   and read into a signed integer variable, so that there will be hardware
   sign bit extension.
   The table will be addressed with the next 7bits in the MPEG bitstream
   starting at the dct_dc_size_luminance vlc.
   The Interpretation of the 8bit entries are as follows:
      For the dct_dc_size_lm [0,1,2,3,4]
	 sssss.rrr
	 where sssss: signed dct_dc_differential value [-15, 15]
	       rrr  : unsigned (8 - size_of_vlc - dct_dc_size_lm)
		      (unused bits from the GETNEXT(15) bits minus 7)
      
      For the dct_dc_size_lm [5,6,7,8]
	 00uuu.000    
	 where uuu  : unsigned (15 - size_of_vlc - dct_dc_size_lm)
		      (unused bits from the GETNEXT(15) bits)



   Program Output:
 sbyte lk_lmdc[128] = {
      253,253,253,253,253,253,253,253,253,253,253,253,253,253,253,253,
       13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,
      236,236,236,236,236,236,236,236,244,244,244,244,244,244,244,244,
       20, 20, 20, 20, 20, 20, 20, 20, 28, 28, 28, 28, 28, 28, 28, 28,
        5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,
      202,202,210,210,218,218,226,226, 34, 34, 42, 42, 50, 50, 58, 58,
      137,145,153,161,169,177,185,193, 65, 73, 81, 89, 97,105,113,121,
       48, 48, 48, 48, 48, 48, 48, 48, 32, 32, 32, 32, 16, 16,  0,  0 };



*****************************************************************************/

void print_lk_lmdc()
{  register int k;
   ubyte lk_lmdc[128];

   for (k=0;k<16;k++) lk_lmdc[0x40 + k] = 5; /* Size 0 */
   for (k=0;k<16;k++)			     /* Size 1 */
   {  lk_lmdc[0x10 + k] = (1 << 3) | 5;
      lk_lmdc[0x00 + k] = (-1<< 3) | 5;
   }

   for (k=0; k<8; k++)			     /* Size 2 */
   {  lk_lmdc[0x20 + k] = (-3<<3) | 4;
      lk_lmdc[0x28 + k] = (-2<<3) | 4;
      lk_lmdc[0x30 + k] =  (2<<3) | 4;
      lk_lmdc[0x38 + k] =  (3<<3) | 4;
   }
   
   for (k=0; k<4; k++)			     /* Size 3 */
   {  lk_lmdc[0x58|(k<<1)] = ((4+k)<<3) | 2;
      lk_lmdc[0x59|(k<<1)] = ((4+k)<<3) | 2;
      lk_lmdc[0x50|(k<<1)] = ((k-7)<<3) | 2;
      lk_lmdc[0x51|(k<<1)] = ((k-7)<<3) | 2;
   }
   
   for (k=0; k<8; k++)			     /* Size 4   */
   {  lk_lmdc[0x68 | k] = ((8+k)<<3)  | 1;   /* positive */
      lk_lmdc[0x60 | k] = ((k-15)<<3) | 1;   /* negative */
   }
   
   for (k=0; k<8; k++)			     /* Size 5   */	
      lk_lmdc[0x70 + k] = 6 << 3;
   for (k=0; k<4; k++)			     /* Size 6   */
      lk_lmdc[0x78 + k] = 4 << 3;
   for (k=0; k<2; k++)			     /* Size 7   */
      lk_lmdc[0x7c + k] = 2 << 3;
      
   lk_lmdc[126] = 0;			     /* Size 8   */
   lk_lmdc[127] = 0;	/* we know this is wrong !!! */
   
   printf("sbyte lk_lmdc[128] = {");
   for (k=0;k<128;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",lk_lmdc[k]);
      
   }
   printf(" }\n");
} 





/***********************************************************************
   This one is for decoding the chrominance DC.
   Format:
      For dct_dc_size_chr [0,1,2,3]
      sssss.rrr
      where sssss: signed dct_dc_differential value [-7, 7]
	    rrr  : unsigned (8 - size_of_vlc - dct_dc_size_chr)
		   (unused bits from the GETNEXT(16) bits minus 8)
      
      For the dct_dc_size_chr [4,5,6,7,8]
	 0uuuu.000    
	 where uuuu  : unsigned (16 - size_of_vlc - dct_dc_size_chr)
		      (unused bits from the GETNEXT(16) bits)
************************************************************************/
	    
	    
	    
void print_lk_chrdc()
{  register int k;
   ubyte lk_chrdc[128];

   for (k=0;k<32;k++) lk_chrdc[k] = 6;	     /* Size 0 */
   for (k=0;k<16;k++)			     /* Size 1 */
   {  lk_chrdc[0x30 + k] = (1 << 3) | 5;
      lk_chrdc[0x20 + k] = (-1<< 3) | 5;
   }

   for (k=0; k<8; k++)			     /* Size 2 */
   {  lk_chrdc[0x40 + k] = (-3<<3) | 4;
      lk_chrdc[0x48 + k] = (-2<<3) | 4;
      lk_chrdc[0x50 + k] =  (2<<3) | 4;
      lk_chrdc[0x58 + k] =  (3<<3) | 4;
   }
   
   for (k=0; k<4; k++)			     /* Size 3 */
   {  lk_chrdc[0x68|(k<<1)] = ((4+k)<<3) | 2;
      lk_chrdc[0x69|(k<<1)] = ((4+k)<<3) | 2;
      lk_chrdc[0x60|(k<<1)] = ((k-7)<<3) | 2;
      lk_chrdc[0x61|(k<<1)] = ((k-7)<<3) | 2;
   }
   
   
   for (k=0; k<8; k++)			     /* Size 4   */	
      lk_chrdc[0x70 + k] = 8 << 3;
   for (k=0; k<4; k++)			     /* Size 5   */	
      lk_chrdc[0x78 + k] = 6 << 3;
   for (k=0; k<2; k++)			     /* Size 6   */
      lk_chrdc[0x7c + k] = 4 << 3;
   for (k=0; k<1; k++)			     /* Size 7   */
      lk_chrdc[0x7e + k] = 2 << 3;
      
   /* Here we assume that a "0" is following the "111.1111" which is the
      vlc for dct_dc_size_chrominance=8
      So we are unable to flag an error for the sequence "1111.1111"
      The error will not live much then anyway...
   */
   lk_chrdc[127] = 0;			     /* Size 8   */
   
   
   printf("sbyte lk_chrdc[128] = {");
   for (k=0;k<128;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",lk_chrdc[k]);
      
   }
   printf(" }\n");
} 




/*****************************************************************************
   This routine creates the four lookup tables which are used to decode 
   the VLCs of the form :     ..001xxxxs
   The created tables are of the form: ubyte runlev_x[16][2]
      runlev_x[16][0] = Run+1
      runlev_x[16][1] = 2*level  

*****************************************************************************/
const ubyte rlev1[16][3] = 
      {	 0,8,13,  0,9,8,  0,10,3,  0,11,0,  1,5,11,  2,4,4,  3,3,12,
      	 4,3,2,  6,2,14,  7,2,5,  8,2,1, 17,1,15,  18,1,10,  19,1,9,
      	 20,1,7,  21,1,6 };
const ubyte rlev2[16][3] = 
      {	 0,12,10,  0,13,9,  0,14,8,  0,15,7,  1,6,6,  1,7,5,  2,5,4,
      	 3,4,3,  5,3,2,  9,2,1,  10,2,0,  22,1,15,  23,1,14,  24,1,13,
      	 25,1,12,  26,1,11 };
const ubyte rlev3[16][3] = 
      {	 0,32,8,  0,33,7,  0,34,6,  0,35,5,  0,36,4,  0,37,3,  0,38,2,
      	 0,39,1,  0,40,0,  1,8,15,  1,9,14,  1,10,13,  1,11,12,  
      	 1,12,11,  1,13,10,  1,14,9 };
const ubyte rlev4[16][3] = 
      {	 1,15,3,  1,16,2,  1,17,1,  1,18,0,   6,3,4,  11,2,10,
      	 12,2,9,  13,2,8,  14,2,7,  15,2,6,  16,2,5,  27,1,15,  
      	 28,1,14,  29,1,13,  30,1,12,  31,1,11 };   	 
	 
void print_lkac()
{  int k;
   ubyte rl[16][2];

   for (k=0;k<16;k++)
   {  rl[rlev1[k][2]][0] = rlev1[k][0]+1;  
      rl[rlev1[k][2]][1] = rlev1[k][1]<<1; 
   }
   printf("const ubyte runlev1[16][2] = {");
   for (k=0; k<16; k++)
   {  if (! (k%10)) printf("\n   ");
      printf("%d,%d,  ",rl[k][0],rl[k][1]);
   }
   printf(" };\n");
   
   
   
   for (k=0;k<16;k++)
   {  rl[rlev2[k][2]][0] = rlev2[k][0]+1;  
      rl[rlev2[k][2]][1] = rlev2[k][1]<<1; 
   }
   printf("const ubyte runlev2[16][2] = {");
   for (k=0; k<16; k++)
   {  if (! (k%10)) printf("\n   ");
      printf("%d,%d,  ",rl[k][0],rl[k][1]);
   }
   printf(" };\n");
   
   
   for (k=0;k<16;k++)
   {  rl[rlev3[k][2]][0] = rlev3[k][0]+1;  
      rl[rlev3[k][2]][1] = rlev3[k][1]<<1; 
   }
   printf("const ubyte runlev4[16][2] = {");
   for (k=0; k<16; k++)
   {  if (! (k%10)) printf("\n   ");
      printf("%d,%d,  ",rl[k][0],rl[k][1]);
   }
   printf(" };\n");
   
   
   for (k=0;k<16;k++)
   {  rl[rlev4[k][2]][0] = rlev4[k][0]+1;  
      rl[rlev4[k][2]][1] = rlev4[k][1]<<1; 
   }
   printf("const ubyte runlev5[16][2] = {");
   for (k=0; k<16; k++)
   {  if (! (k%10)) printf("\n   ");
      printf("%d,%d,  ",rl[k][0],rl[k][1]);
   }
   printf(" };\n");
   
   
}



/****************************************************************************
   This routine creates the lookup table which is used to skip, without
   interpreting, the chrominance DC.
   The table is addressed with the next 7bits in the bitstream.
   The corresponding entry returns:
      (16 - dct_dc_size_chr - sizeof_VLC) for that VLC.
****************************************************************************/
void print_chrdcskip()
{  register int k,m;
   ubyte x[128];

   for (k=0;k<32;k++) 
   {  x[k] = 0+2;
      x[k + 0x20] = 1+2;
      x[k + 0x40] = 2+2;
   }
   
   for (k=0;k<5;k++)
   {  for (m=0;m < (1<<(4-k)); m++)
	 x[(((1<<(2+k))-1) << (5-k)) + m] = (3+k)*2;
   }
   x[127] = 8+8;  /* we know this is wrong, and causes not to be able to flag
		     an error for "1111111[1]"
		  */
   for (k=0;k<128;k++) x[k] = 16 - x[k];

   printf("ubyte lk_chrdcskip[128] = {");
   for (k=0;k<128;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",x[k]);
      
   }
   printf(" }\n");

   for (k=0;k<128;k++) 
   {  printf("%3d    ",x[k]);
      for (m=7;m>=0;m--) 
	 if (k& (1<<m)) printf("1"); else printf("0");
      printf("\n");
   }

}
  

 
#define	 FLAG_DC     0x8
#define	 FLAG_OROW   0x2
#define	 FLAG_OCOL   0x1
#define	 FLAG_INNER  0x4

const ubyte dzz[64] = 
   {0,1,8,16,9,2,3,10,17,
    24,32,25,18,11,4,5,12,
    19,26,33,40,48,41,34,27,
    20,13,6,7,14,21,28,35,
    42,49,56,57,50,43,36,29,
    22,15,23,30,37,44,51,58,
    59,52,45,38,31,39,46,53,
    60,61,54,47,55,62,63};
  


/***************************************************************************
   IDCT patterns:
      
	 bits[3-0]: DIRC
   	 bits[0]: Dirty Outer Column
   	 bits[1]: Dirty Outer Row
   	 bits[2]: Dirty Inner 7x7 Region
	 bits[3]: Dirty DC
   	    Each of the four fields below consist of 7bits,
   	    higher order bits in each field correspond to higher order row
   	    or columns;
   	 bits[10-4]:  IC, Inner_Column, flags for inner rows 7-1 
   	 bits[17-11]: IR
   	 bits[24-18]: OC, Outer_Column
   	 bits[31-25]: OR   
   
      OR       OC      IR        IC    DIRC 
   6543210  6543210  6543210  6543210  3210
   31   25  24   18  17   11  10    4  3  0  bit#
****************************************************************************/
void print_patterns()
{  register uint32 w;
   register int k,m;
   static uint32 pattern[64];
/*
   for (k=0;k<64;k++)
   {  w = 0;
      if (k==0)	  w = FLAG_DC;
      else if (k<8)	      w = FLAG_OROW | (1<<(k-1+25));
      else if ((k&0x7) == 0)  w = FLAG_OCOL | (1<<((k>>3)-1+18));
      else w = FLAG_INNER | (1<<((k>>3)-1+4)) | (1<<((k&7)-1+11)); 
      pattern[k] = w;
   }
*/
   
   for (m=0; m<64; m++)
   {  nuint k = m;
      w = 0;
      if (k==0)	  w = FLAG_DC;
      else if (k<8)
      {  if ( k > 2) w = 2;	/* reg4 or reg7 */
         if ( k > 4) w |=1;	/* reg7 */
         if (k == 1) w |=4;	/* sng1 */
         if (k == 2) w |=8;  	/* sng2 */
         w = FLAG_OROW | (w<<19);
      }
      else if ((k&0x7) == 0)
      {  k >>= 3;
         if ( k > 2) w = 2;	/* reg4 or reg7 */
         if ( k > 4) w |=1;	/* reg7 */
         if (k == 1) w |=4;	/* sng1 */
         if (k == 2) w |=8;  	/* sng2 */
         w = FLAG_OCOL | (w<<6);
      }
      else
      {  uint32 irow = 1<<((k&7)-1+2);
         uint32 icol = 1<<((k>>3)-1+2);
         if ((k&7) > 2) irow |= 2;
         if ((k&7) > 4) irow |= 1;
         if ((k>>3) > 2) icol |= 2;
         if ((k>>3) > 4) icol |= 1;
         w = (irow<<23) | (icol<<10) | FLAG_INNER;
      }
      pattern[m] = w;
   }
   /*
   for (k=0; k<64; k++)
      printf("%2d:  col:%3d %1d    row:%3d %1d\n", 
         k, pattern[k]>>25, (pattern[k]>>23)&0x3,
         (pattern[k]>>12)&0x7f, (pattern[k]>>10)&0x3);
   */
   
   
   printf("uint32 pattern[64] = {");
   for (k=0;k<64;k++) 
   {  if (! (k%6)) printf("\n      ");
      printf("%0#10x,",pattern[dzz[k]]);      
   }
   printf(" }\n");

/*
   for (k=0;k<64;k++) 
   {  printf("%3d    ",k);
      for (m=31;m>=0;m--) 
	 if (pattern[k] & (1<<m)) printf("1"); else printf("0");
      printf("\n");
   }
*/
}


/**************************************************************************
   ubyte maincase[128] :
      ssssss.mm : 
	 for regular idct, sssss gives the # of 1s in the entry address,
	 for others it gives the subcase idct #, e.g, for single cases 1-7,
	    and for doubles, 8-22
	 mm: 1-> single
	     2-> double
	     3-> triple (not used yet)
	     0-> regular

   Example: 0001000 -> ssssss = 4, mm = 1
**************************************************************************/

#define DOUBLE_BASE 8
#define REGIDCT_BASE (DOUBLE_BASE+15)

void print_maincase()
{  ubyte maincase[128];
   register int k,m,n,cnt;
   register uint32 w;

   for (k=0;k<128;k++)
   {  w = k;
      n = 1;
      cnt =0;
      while (w)
      {	 if (w&1) cnt++;
	 else n++;
	 w >>= 1;
      }
      if (cnt==1) maincase[k] = (n<<2) + 1; /* position, single */
      else  maincase[k] = ((cnt+REGIDCT_BASE)<<2); /* count of 1s */

   }

   /* Manually fill in all the duble cases */

   maincase[3] = 2 | (DOUBLE_BASE + 0 << 2);
   maincase[5] = 2 | (DOUBLE_BASE + 1 << 2);
   maincase[9] = 2 | (DOUBLE_BASE + 2 << 2);
   maincase[17] = 2 | (DOUBLE_BASE + 3 << 2);
   maincase[33] = 2 | (DOUBLE_BASE + 4 << 2);
   maincase[6] =  2 | (DOUBLE_BASE + 5 << 2);
   maincase[10] = 2 | (DOUBLE_BASE + 6 << 2);
   maincase[18] = 2 | (DOUBLE_BASE + 7 << 2);
   maincase[34] = 2 | (DOUBLE_BASE + 8 << 2);
   maincase[12] = 2 | (DOUBLE_BASE + 9 << 2);
   maincase[20] = 2 | (DOUBLE_BASE + 10 << 2);
   maincase[36] = 2 | (DOUBLE_BASE + 11 << 2);
   maincase[24] = 2 | (DOUBLE_BASE + 12 << 2);
   maincase[40] = 2 | (DOUBLE_BASE + 13 << 2);
   maincase[48] = 2 | (DOUBLE_BASE + 14 << 2);
   
   for (k=0;k<128;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",maincase[k]);
      
   }
   printf("\n");

/*
   for (k=0;k<128;k++) 
   {  printf("ssssss:%3d    mm: %3d      ",maincase[k]>>2, maincase[k]&3);
      for (m=8;m>=0;m--) 
	 if (k & (1<<m)) printf("1"); else printf("0");
      printf("\n");
   }
*/
}
      





void print_1count()
{  ubyte onecnt[128];
   register int k,m,n,cnt;
   register uint32 w;

   for (k=0;k<128;k++)
   {  w = k;
      cnt =0;
      while (w)
      {	 if (w&1) cnt++;
	 w >>= 1;
      }
      onecnt[k] = cnt;
      
   }

   for (k=0;k<128;k++) 
   {  if (! (k%32)) printf("\n      ");
      printf("%1d,",onecnt[k]);
      
   }
   printf("\n");

   for (k=0;k<128;k++) 
   {  printf("%3d      ",onecnt[k]);
      for (m=6;m>=0;m--) 
	 if (k & (1<<m)) printf("1"); else printf("0");
      printf("\n");
   }
}







/* Macroblock address increment codes: first entry is dummy, the rest
   are for [1..33] in this order 
*/
const ubyte 
   vlc_mba[34] = {0,1,3,2,3,2,3,2,7,6,11,10,9,8,7,6,23,22,21,20,19,18,
      	       	  35,34,33,32,31,30,29,28,27,26,25,24},
   lvlc_mba[34] = { 0,1,3,3,4,4,5,5,7,7,8,8,8,8,8,8,10,10,10,10,10,10,11,
      	       	    11,11,11,11,11,11,11,11,11,11,11 };
  
void print_lkupmbadrs()
{  int k,m;
   ubyte lk_mbadr1[32], lk_mbadr2[128];

   /* Format: rrrr.vvvv
      rrrr: unused bits, 11-lenght of the vlc
	    (because we start with 11 GETNEXT(11) )
      vvvv: MB increment value 
   */
   for (k=0; k<(1<<5); k++)  /*5bit addressing */
   {  lk_mbadr1[k] = 0;	/* initialize to not decodable */
      for (m=1; m<=7; m++)
      	 if (k >> (5-lvlc_mba[m]) == vlc_mba[m]) 
      	 {  lk_mbadr1[k] = ((11-lvlc_mba[m])<<4) | m;
      	    break;
      	 }
   }

   
   /* Format: rrr.vvvvv
      rrrr: unused bits, 7-lenght of the vlc
      vvvv: MB increment value - 8 ( so that it fits to 5 bits)
   */
   for (k=0; k<(1<<7); k++)  /*7bit addressing */
   {  lk_mbadr2[k] = 0;	/* initialize to not decodable */
      for (m=8; m<=33; m++)
      	 if (k >> (4+7-lvlc_mba[m]) == vlc_mba[m]) 
      	 {  lk_mbadr2[k] = ((4+7-lvlc_mba[m])<<5) | (m-8);
      	    break;
      	 }
   }
   

   printf("ubyte lk_mbadr1[32] = {");
   for (k=0;k<32;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",lk_mbadr1[k]);
      
   }
   printf(" }\n");
   
   printf("ubyte lk_mbadr2[128] = {");
   for (k=0;k<128;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",lk_mbadr2[k]);
      
   }
   printf(" }\n");

}






/* Addressed with (picture_type<<6 | next 6 bits of the code stream)
   Picture_type is 2 bits: I_pict 00, P_pict 01, B_pict 10, DCI_pict 11
   8bit unsigned entries : FFFFFLLL (5bit flags, 3bit lenght)
      Flags bit7-3: QUANT,FORWARD,BACKWARD,PATTERN,INTRA (in order)   
*/
ubyte dvlc_mbtype[256] = 
   {
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    138,138,138,138,138,138,138,138,138,138,138,138,138,138,138,138,
      9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,
      9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,
    255,142,149,149,213,213, 13, 13, 67, 67, 67, 67, 67, 67, 67, 67,
     18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
     81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81,
     81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81,
    255,142,182,214,245,245, 13, 13, 68, 68, 68, 68, 84, 84, 84, 84,
     35, 35, 35, 35, 35, 35, 35, 35, 51, 51, 51, 51, 51, 51, 51, 51,
     98, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98, 98,
    114,114,114,114,114,114,114,114,114,114,114,114,114,114,114,114,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,
      9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9
   };

/* The above table, but 6-LLL, since we are using GETNEXT9(6) */
void print_mbtype()
{  ubyte a[256];
   int k;

   for (k=0;k<256;k++)
     a[k] = (6 - (dvlc_mbtype[k] & 0x7)) | (dvlc_mbtype[k] & 0xf8);

   printf("ubyte dvlc_mbtype[256] = {");
   for (k=0;k<256;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",a[k]);
      
   }
   printf(" }\n");  
}







/*Motion vector codes: [-16..0..16] in this order */
ubyte vlc_mvd[33] = 
   {25,27,29,31,33,35,19,21,23,7,9,11,7,3,3,3,1,2,2,2,6,10,
    8,6,22,20,18,34,32,30,28,26,24};
/* And their VLC lenghts... */
ubyte lvlc_mvd[33] = 
   {11,11,11,11,11,11,10,10,10,8,8,8,7,5,4,3,1,3,4,5,7,8,8,
    8,10,10,10,11,11,11,11,11,11};
ubyte dvlc_mvd[2048];

void print_motionv()
{  int k,m,r0,r1;
   ubyte mv[512];

   /* Initialize the DeHuffman table for the mb_mvd vlc decoding   */
   for (k=0; k<(1<<11); k++)  /* 11bit addressing */
   {  dvlc_mvd[k] = 0;
      for (m=0; m<33; m++)
      	 if (k >> (11-lvlc_mvd[m]) == vlc_mvd[m]) 
      	 {  dvlc_mvd[k] = m;
      	    break;
      	 }
   }

   for (k=0;k<512;k++)
   {  r0 = dvlc_mvd[k<<1];
      r1 = 11 - lvlc_mvd[r0];
      /* we are using  absolute motion code -1 */
      mv[k] = ((abs(r0 - 16)-1)<< 4 ) | r1;   
   }

   printf("ubyte mv[512] = {");
   for (k=0;k<512;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",mv[k]);
      
   }
   printf(" }\n");  
}




/* Coded block pattern codes: first entry is dummy  */
const ubyte 
   vlc_cbp[64] = {0,11,9,13,13,23,19,31,12,22,18,30,19,27,23,19,11,21,
      	       	  17,29,17,25,21,17,15,15,13,3,15,11,7,7,10,20,16,28,
      	       	  14,14,12,2,16,24,20,16,14,10,6,6,18,26,22,18,13,9,5,
      	       	  5,12,8,4,4,7,10,8,12};
/* Lengths of the VLCs for the coded_block_pattern, first entry is dummy*/
ubyte lvlc_cbp[64] = { 
   0,5,5,6,4,7,7,8,4,7,7,8,5,8,8,8,4,7,7,8,5,8,8,8,6,8,8,9,5,8,8,9,4,
   7,7,8,6,8,8,9,5,8,8,8,5,8,8,9,5,8,8,8,5,8,8,9,5,8,8,9,3,5,5,6 };


void print_mbcbp(void)
{  register int k,m;
   ubyte dvlc_cbp[512];

   for (k=0; k<(1<<9); k++)   /* 9 bit addressing */
   {  dvlc_cbp[k] = 0;
      for (m=1; m<64; m++)
      	 if (k >> (9-lvlc_cbp[m]) == vlc_cbp[m]) 
      	 {  dvlc_cbp[k] = m;
      	    break;
      	 }
   }

   printf("ubyte dvlc_cbp[512] = {");
   for (k=0;k<512;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",dvlc_cbp[k]);      
   }
   printf(" }\n");  
}

uint16 brate[16] = {0,32,48,56,64,80,96,112,128,160,192,224,256,320,384,2};
#define AUD_SINGLE   0x40
/***************************************************************************
   Generates an index into one of the 4 quantizer tables
   format of the entry address:
      sbbbbff
	 where s: 1=Single Channel
	      ff: frequency index as in the mpeg audio frame header
		  44.1, 48, 32 HZ respectively for = 0,1,2
	     bbbb: bitrate index for layer 2 as in Mpeg audio frame header
****************************************************************************/
#define JUNK 4	 
ubyte table[128];
void print_audtables()
{  nint k,freq,rate;
  

   for (k=0;k<128;k++) table[k] = 5;  /* Initialize all to illegal */
   for (k=0;k<128;k++)
   {  rate = brate[(k>>2) & 0xf];
      if (!(k & AUD_SINGLE)) rate /=2; 
      freq = k & 0x3;
      if (freq==0) /* 44.1Khz */
      {	 if ((rate>=96 && rate<=192) || rate==0) table[k] = 1;
	 else if (rate==32 || rate==48) table[k] = 2;
	 else if (rate>=56 && rate <=80) table[k] = 0;
	 else table[k] = JUNK;
      }
      else if (freq==1)	/* 48Khz */
      {	 if ((rate>=56 && rate<=192) || rate==0) table[k]=0;
	 else if (rate==32 || rate==48) table[k]=2;
	 else table[k] = JUNK;
      }
      else if (freq==2)	/* 32Khz */
      {	 if (rate>=56 && rate<=80) table[k]=0;
	 else if ((rate>=96 &&  rate<=192) || rate==0) table[k] = 1;
	 else if (rate==32 || rate==48) table[k]=3;
	 else table[k] = JUNK;
      }
      else table[k] = JUNK;
   }

   printf("ubyte lk_qnttable[128] = {");
   for (k=0;k<128;k++) 
   {  if (! (k%32)) printf("\n      ");
      printf("%d,",table[k]);      
   }
   printf(" }\n");    

}      


/* Addressed by:
   8bit LCbb.bbss
   bbbb: bitrate index
   ss: sampling rate
    L: layer_bit_0 , 1 for Layer_1,  0 for Layer_2
    C: single channel flag, 1 for single channel, 0 for stereo, double or joint
   
   Checks the validity of bitrate, sampling rate, layer and mode
   combinations. A valid combination returns with the frame size
   for that frame, or a special code for the free format.
   0 is returned for an invalid case
*/
      
const uint16 brate1[16] = 
   { 0,32,64,96,128,160,192,224,256,288,320,352,384,416,448,0 };
const uint16 brate2[16] =
   { 0,32,48,56,64,80,96,112,128,160,192,224,256,320,384,0 };
const uint16 smpfreq[3] =
   {  44100, 48000, 32000 };
      
/* largest possible non-free-bitrate frame size is:
      Layer_2 : (384000*144)/32000 = 1728 bytes
      Layer_1 : (448000*48)/32000  = 672 bytes
*/
#define	 FREE_BRATE  2048     /* a value larger than any regular frame size */

   
void print_frametable()
{  uint16 tbl[256], brate, sfreq, r0;
   nint k;
   
   for (k=0;k<256;k++)
   {  tbl[k] = 0;    /* Initialize to invalid frame type */
      brate  = (k>>2) & 0xf;
      if (brate == 0xf) continue;   
      sfreq = k & 0x3;
      if (sfreq == 0x3) continue;
      
      
      if (! (k&0x80) )	/* If Layer_2 */
      {	 /* bitrates 32,48,56,80 are only supported with single channel */
	 if ( !(k & 0x40) && (brate==1 || brate==2 || brate==3 || brate==5))
	    continue;
	 /* bitrates 224,256,320,384 are not supported with single channel */
	 if ( (k & 0x40) && (brate>=11 && brate<=14))
	    continue;
	 tbl[k] = (uint16)((brate2[brate]*1000*144.0) / smpfreq[sfreq]);
      }
      else  /* Layer_1 */
      {	 r0 = (uint16)((brate1[brate]*1000*12.0) / smpfreq[sfreq]);
	 tbl[k] = r0 * 4; /* Each layer_1 slot is 4 bytes */
      }
      
      if (brate == 0) tbl[k] = FREE_BRATE;
   }
      
}


main()
{  
   print_patterns();   
}
