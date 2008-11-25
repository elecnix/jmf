/*
 * @(#)mp_aud.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

/*************************************************************************
   All the routines and data structures for MPEG LAYER1/2 Audio decoding.
   The code here is Endian independent.
   We rely on certain integer mathematical operations' working in an
   assumed way, and they do work that way with the SPARC C compiler.
   Those are:
      int32 i;
      right shifts of i are sign filled from left.
      i<<0 == i  and i>>0 == i
   
**************************************************************************/

#include <math.h>
#include <string.h>

#include "mpaud1.h"  /* Layer 1/2 Scalefactors array: double scalefacts[63]*/
#include "mpaud2.h"  /* Decoding windowing coefficients: float fwind[257]  */
#include "mp_aud.h"
  
  
#define CLIP_TO_16BIT( s32 ) \
{  if (s32 < -32768) s32 = -32768;  else if (s32 > 32767 ) s32 = 32767; }
  
  
/*********************************************************************** 
   TABLES 3-B.2	  Used for Layer2
   qlev_ab[band_no][index]
   
   "index" is the allocation[sb][] value read from the bitstream, which is
   a value between [0,15]. 
   
   qlev_ab[band_no][0] = 0
   
   qlev_ab[band_no][16] = #bits to be read from the bitstream as the 
      allocation[sb][] value.
   
   Other entries (with valid values of  1-17 ) are used to index into 
   nbits[], nnbits[], cd[], cd1[] and cd_add[]. The entries indicate
   #quantization steps to be used for a subband. Of the 17 possible
   #qsteps, 15 are 2**n-1 where n is [2-16], and other 2 are for #qsteps
   3 and 9. All these #qsteps are ordered in magnitude and indexed with
   a value between 1-17. 
************************************************************************/  

/* Table 3-B.2b ( also good for Table 3-B.2a )	*/
static ubyte qlev_ab[30][17] = {
   0,1,3,5,6,7,8,9,10,11,12,13,14,15,16,17,4,
   0,1,3,5,6,7,8,9,10,11,12,13,14,15,16,17,4,
   0,1,3,5,6,7,8,9,10,11,12,13,14,15,16,17,4,
   0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,4,
   0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,4,
   0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,4,
   0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,4,
   0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,4,
   0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,4,
   0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,4,
   0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17,4,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,3,4,5,6,17,0,0,0,0,0,0,0,0,3,
   0,1,2,17,0,0,0,0,0,0,0,0,0,0,0,0,2,
   0,1,2,17,0,0,0,0,0,0,0,0,0,0,0,0,2,
   0,1,2,17,0,0,0,0,0,0,0,0,0,0,0,0,2,
   0,1,2,17,0,0,0,0,0,0,0,0,0,0,0,0,2,
   0,1,2,17,0,0,0,0,0,0,0,0,0,0,0,0,2,
   0,1,2,17,0,0,0,0,0,0,0,0,0,0,0,0,2,
   0,1,2,17,0,0,0,0,0,0,0,0,0,0,0,0,2 };
   

/* Table 3-B.2d ( also good for Table 3-B.2c )	*/
static ubyte qlev_cd[12][17] = {
   0,1,2,4,5,6,7,8,9,10,11,12,13,14,15,16,4,
   0,1,2,4,5,6,7,8,9,10,11,12,13,14,15,16,4,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3,
   0,1,2,4,5,6,7,8,0,0,0,0,0,0,0,0,3 };

/* lk_qnttable[i] tells which of the 4 quantizer_info tables to use
   for the particular bitrate_per_channel/sampling rate combination,
   which is indicated by i.
   Format of i -crrrrss- :
      rrrrss : bitrate_index & sampling_frequency bits read from the
	       frame header.
      c	     : 0 if bitrate_index represents total for 2 channels
	       1 if for 1 channel. (Single_channel mode )

   Meanings of table values:
      4: Illegal bitrate & sampling_frequency combination.
      0: Table 3-B.2a
      1: Table 3-B.2b
      2: Table 3-B.2c
      3: Table 3-B.2d
*/
static ubyte lk_qnttable[128] = {
   1,0,1,4,4,4,4,4,4,4,4,4,4,4,4,4,2,2,3,4,4,4,4,4,2,2,3,4,0,0,0,4,
   0,0,0,4,0,0,0,4,1,0,1,4,1,0,1,4,1,0,1,4,1,0,1,4,1,0,1,4,4,4,4,4,
   1,0,1,4,2,2,3,4,2,2,3,4,0,0,0,4,0,0,0,4,0,0,0,4,1,0,1,4,1,0,1,4,
   1,0,1,4,1,0,1,4,1,0,1,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4 };

/* Layer2: #bits used to code the subband sample, given #qsteps used to quantize it */
static ubyte nbits[18] =  {0,5,7,3,10,4,5,6,7,8,9,10,11,12,13,14,15,16 };

/* Layer2: same as nbits[], but the total for the 3 samples of a granule   */
static ubyte nnbits[18] = {0,5,7,9,10,12,15,18,21,24,27,30,33,36,39,42,45,48 };

/* This table is addressed by the 2bits of scfsi.
   It contains the # scalefactor bits to be read (6,12 or 18) corresponding to the
   scfsi, and also the scfsi itself.
*/
static ubyte scbits[4] = 
   {  ((3*6<<2) | 0),
      ((2*6<<2) | 1),
      ((1*6<<2) | 2),
      ((2*6<<2) | 3)
   };
/* sblimits coreesponding to Tables 3-B.2.a-d */
static ubyte lk_nsb[4] = {27,30,8,12};

/* Highest subband to be processed, given the quality levels 0,1,2.
   Quality level 0 is the highest quality level, possibly all the subbands,
   [0-31] for layer1 and [0-29] for layer2 are processed.
   Last entry is dummy.
*/
static ubyte lk_maxsub[4] = {31,15,7,31};

/* Table 3-B.4 -C-
   Used in Layer2 processing.
   First entry is dummy.
   Others are normalized with 1/32768 in order to push the fractional point
   of the samples in the form of ssssssss ssssssss s.fffffff ffffffff
   to the right of bit0, e.g bit0 becomes the sign bit, or 1...
   This should actually be an array of double. 
*/  
static float cd[18] = {
   0,
   4/(32768*3.0),
   1.6/32768.0,
   8/(32768*7.0),
   16/(32768*9.0),
   16/(32768*15.0),
   32/(32768*31.0),
   64/(32768*63.0),
   128/(32768*127.0),
   256/(32768*255.0),
   512/(32768*511.0),
   1024/(32768*1023.0),
   2048/(32768*2047.0),
   4096/(32768*4095.0),
   8192/(32768*8191.0),
   16384/(32768*16383.0),
   32768/(32768*32767.0),
   65536/(32768*65535.0)
   };

/* Same as cd[], the rescaling constants for subband samples, corresponding 
   to possible #qsteps, but suitable for layer1 indexing. In layer1 there
   are 14 possible #qsteps.
   Used in Layer1 processing.
   First two entries are dummy.
*/
static float cd1[16] = {
   0,
   0,
   4/(32768*3.0),
   8/(32768*7.0),
   16/(32768*15.0),
   32/(32768*31.0),
   64/(32768*63.0),
   128/(32768*127.0),
   256/(32768*255.0),
   512/(32768*511.0),
   1024/(32768*1023.0),
   2048/(32768*2047.0),
   4096/(32768*4095.0),
   8192/(32768*8191.0),
   16384/(32768*16383.0),
   32768/(32768*32767.0)
   };

/* Table 3-B.4 -D-
   First entry is dummy.
   Used in Layer2 processing.
   These should be considered as integer representation of otherwise fractional
   values.
   These are the additive constants used in a 32bit context where the
   fractional point is to the left of bit14 :
      ssssssss ssssssss s.fffffff ffffffff
   This is the logical fractional representation we bring the subband samples
   into after we decode them from the bitstream.
*/  
static int16 cd_add[18] = {
   0,
   1<<(15-1),
   1<<(15-1),
   1<<(15-2),
   1<<(15-1),
   1<<(15-3),
   1<<(15-4),
   1<<(15-5),
   1<<(15-6),
   1<<(15-7),
   1<<(15-8),
   1<<(15-9),
   1<<(15-10),
   1<<(15-11),
   1<<(15-12),
   1<<(15-13),
   1<<(15-14),
   1<<(15-15) };   

static float lcos[8];	/* lcos[n] = 1.0/(2*cos(n/16)) */

static uint16 brate1[16] = 
   { 0,32,64,96,128,160,192,224,256,288,320,352,384,416,448,0 };
static uint16 brate2[16] =
   { 0,32,48,56,64,80,96,112,128,160,192,224,256,320,384,0 };
static uint16 smpfreq[3] =
   {  44100, 48000, 32000 };

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
uint16 lk_framesz[256];

/* These lookup tables are used to decode each of the 3 grouped subband
   samples. They return the final floating point value of the decoded &
   dequantized subband sample. In other words they perform:
      Decoding the sample from the common group code
      flipping the most significant bit
      + D
      * C

   These may well be designed to work as  signed byte grp[][3] or
   as uint16 grp[]. It is all a matter of speed/cache/CPU architecture
   tradeoffs...
*/
static float grp10[1024][3];
static float grp5[32][3];
static float grp7[128][3];

static float nn[32][32];
static float dsw[32][16];

      

typedef struct {
   ubyte maxsub;  /* Highest subband which is need to be processed */
   ubyte qnttable;/* [0-3] which quantizer info table to use: Table3-B.2a-d */
   ubyte bound;	  /* Bound value for the joint mode */
   nint	 dsize;	  /* #bytes available in the decode data buffer starting
		     with the first byte after the frame header. */
   uint16 need;	  /* #bytes required in the decode data buffer for
		     complete frame decode starting with the first byte
		     after the frame header. */
   ubyte maxband[2]; /* Output parameters from the layer1/2 frame bitstream 
			decoders, indicating the actual highest subbands
			to be processed in the Synthesis subband filtering
			phase */
} adec_env;



/* Get N bits from the data stream right aligned into zzb
 * So the last of the received bits will be the least significant bit
 * in zzb.
 * GETNEXT16(N)is to be used when N<=16
 * GETNEXTS(N) is to be used when the read bits are known not to be
 * going to be put back. ( S stands for safe reading...)
 * More than 16bits  can be read with GETNEXTS(N)
 * Both routines read as many bytes as they can from the code stream
 * if #remaining bits in zza is less than the #bits asked for, e.g for 
 * GETNEXT16(N), 2bytes are read for present and future use.
 *
 * IMPORTANT: note that the read into zzb is not a clean read, and it
 * should further be masked with the knowledge of valid bits in zzb.
 * This cleaning is not done here for purposes of efficiency.
 *
 * If the else branch is taken, only 3 cycles are spent for the whole 
 * read operation.
 */

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


/*
   Initializes the frame size lookup table
*/
void init_lkframesz(uint16 tbl[])
{  uint16 brate, sfreq, r0;
   nint k;
   
   for (k=0;k<256;k++)
   {  tbl[k] = 0;    /* Initialize to invalid frame type */
      brate  = (k>>2) & 0xf;
      if (brate == 0xf) continue;   
      sfreq = k & 0x3;
      if (sfreq == 0x3) continue;
      
      if (brate == 0)
      {	 tbl[k] = FREE_BRATE;
	 continue;
      }
      
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
   }
}

/*
   One time initialization of data structures for the Layer1/2
   decoding process. All of the data structures initialized here are
   in reentrant data areas.
*/
void mp_initaudio()
{  register nint i,k;
   register uint32 w;
   register int32 r0;

   init_lkframesz(lk_framesz);
   
   for (i=0;i<32;i++)
   for (k=0;k<32;k++)
      nn[i][k] = cos((16+(i+17))*(2*k+1)*M_PI/64.0);

   for (k=0;k<8;k++)
      lcos[k] = 0.5/cos((M_PI / 16) * (k+1));   
   
   /* Initialize Synthesis window coefficients, Table 3-B.3
      Second half of the dsw[] is essentially a negative mirror image
      of the first half with some exceptions, we exploit this symmetry...
      Furthermore we arrange it in blocks of 16, dsw[32][16], to provide
      sequential access to it in the Synthesis filter routines.
      This also helps to have better cache performance.
   */
   for (i=0;i<32;i++)
   {  for (k=0;k<8;k++)
	 dsw[i][k] = fwind[k*32+i];
      for (k=8;k<16;k++)
	 dsw[i][k] = -fwind[255-(k*32+i-256-1)];
   }
   dsw[0][ 8] = fwind[256];
   dsw[0][10] = fwind[192];
   dsw[0][12] = fwind[128];
   dsw[0][14] = fwind[ 64];
   

   /* IMPORTANT NOTE!!: Here we assume sign filled right shifts of 
      signed integers.
   */
   for (k=0;k<32;k++)
   {  w = k;  
      r0 = w % 3;
      w = w / 3;
      r0 ^= 0x2;  /* Invert bit1, the sign bit */
      r0 <<= 30;  /* Left align it at the most significant bit, bit31 */
      r0 >>= 16;  /* Align the sign bit at bit15 with sign filling from left
		     We are interpreting this sample as a fractional number
		     represented in a 32bit word:
			ssssssss ssssssss sfffffff ffffffff
		  */
      grp5[k][0] = (r0 + 0x4000 )*(4/(32768*3.0)); /* C * (s + D) */
      r0 = w % 3;
      w = w / 3;
      r0 ^= 0x2;
      r0 <<= 30;
      r0 >>= 16;
      grp5[k][1] = (r0 + 0x4000 )*(4/(32768*3.0));
      r0 = w % 3;
      w = w / 3;
      r0 ^= 0x2;
      r0 <<= 30;
      r0 >>= 16;
      grp5[k][2] = (r0 + 0x4000 )*(4/(32768*3.0));
   }

   for (k=0;k<128;k++)
   {  w = k;  
      r0 = w % 5;
      w = w / 5;
      r0 ^= 0x4;  /*invert bit2 */
      r0 <<= 29;
      r0 >>= 16;
      grp7[k][0] = (r0 + 0x4000 )*(1.6/32768);
      r0 = w % 5;
      w = w / 5;
      r0 ^= 0x4;
      r0 <<= 29;
      r0 >>= 16;
      grp7[k][1] = (r0 + 0x4000 )*(1.6/32768);
      r0 = w % 5;
      w = w / 5;
      r0 ^= 0x4;
      r0 <<= 29;
      r0 >>= 16;
      grp7[k][2] = (r0 + 0x4000 )*(1.6/32768);
   }

   for (k=0;k<1024;k++)
   {  w = k;  
      r0 = w % 9;
      w = w / 9;
      r0 ^= 0x8;  /*invert bit3 */
      r0 <<= 28;
      r0 >>= 16;
      grp10[k][0] = (r0 + 0x4000 )*(16.0/(9*32768));
      r0 = w % 9;
      w = w / 9;
      r0 ^=  0x8;
      r0 <<= 28;
      r0 >>= 16;
      grp10[k][1] = (r0 + 0x4000 )*(16.0/(9*32768));
      r0 = w % 9;
      w = w / 9;
      r0 ^=  0x8;
      r0 <<= 28;
      r0 >>= 16;
      grp10[k][2] = (r0 + 0x4000 )*(16.0/(9*32768));
   }
}


/*
   Only reason of this routine's not being inside filt1 is to avoid
   the stupid compiler's messing up the pipelines...
*/ 
static void
doit1(nint maxsb, float smp[][32], float *vvp, nint scnt)
{  register float *vp1, (*np)[32];
   register float f0,f1,f2,f3,f4,f5,f6,f7,f8;
   register int k,n,m;
	 
   f0=f1=f2=f3=f4=f5=f6=f7 = 0;

   for (n=0;n<scnt;n++)
   {  np = (float (*)[32])nn;
      vp1 = vvp-n*32;
      for (m=0;m<4;m++)
      {	 
	 for (k=0;k<=maxsb;k++)
	 {  f8 = smp[n][k];
	    f0 += np[0][k] * f8;
	    f1 += np[1][k] * f8;
	    f2 += np[2][k] * f8;
	    f3 += np[3][k] * f8;
	    f4 += np[4][k] * f8;
	    f5 += np[5][k] * f8;
	    f6 += np[6][k] * f8;
	    f7 += np[7][k] * f8;
	 }
	 vp1[0] = f0;  f0 = 0;
	 vp1[1] = f1;  f1 = 0;
	 vp1[2] = f2;  f2 = 0;
	 vp1[3] = f3;  f3 = 0;
	 vp1[4] = f4;  f4 = 0;
	 vp1[5] = f5;  f5 = 0;
	 vp1[6] = f6;  f6 = 0;
	 vp1[7] = f7;  f7 = 0;
	 np += 8;
	 vp1+= 8;
      }      
   }
}
 
/*******************************************************************************
   Synthesis subband filter for  F u l l  S a m p l i n g   M o d e
   ap	 : output sample buffer pointer.
   apofs : pointer increment value between samples
	   1 in single channel mode
	   2 in Dual/Stereo mode
   scnt	 : #samples per subband in smp[][32]
	   36 for layer2
	   12 for layer1
   maxsb : highest subband needs to be processed, reflects the highest
	   subband encountered in frame decode
   vv	 : working area, should be the same as used in previous frames
	   minimum size (16+36)*32 -layer2-
			(16+12)*32 -layer1-
******************************************************************************/ 
static void
filt1(nint maxsb, float smp[][32], float *vv, int16 *ap, nint scnt, nint apofs)
{  register float *vvp, *vp1, *vp2, sm0, sm1;
   register int k,n,r0;
   
   vvp = vv+(scnt-1)*32;
   doit1(maxsb,smp,vvp,scnt);
   
   for (n=0;n<scnt;n++)
   {  vp1 = vvp+15;
      vp2 = vvp+32+15;
      for (k=0;k<16;k++) 
      {  sm0  = vp2[0*64] * dsw[k][1];
	 sm1  = vp2[1*64] * dsw[k][3];
         sm0 -= vp1[0*64] * dsw[k][0];
	 sm1 -= vp1[1*64] * dsw[k][2];
	 sm0 -= vp1[2*64] * dsw[k][4];
	 sm1 += vp2[2*64] * dsw[k][5];
	 sm0 -= vp1[3*64] * dsw[k][6];
	 sm1 += vp2[3*64] * dsw[k][7];
	 sm0 -= vp1[4*64] * dsw[k][8];
	 sm1 += vp2[4*64] * dsw[k][9];
	 sm0 -= vp1[5*64] * dsw[k][10];
	 sm1 += vp2[5*64] * dsw[k][11];
	 sm0 -= vp1[6*64] * dsw[k][12];
	 sm1 += vp2[6*64] * dsw[k][13];
	 sm0 -= vp1[7*64] * dsw[k][14];
	 sm1 += vp2[7*64] * dsw[k][15];
	 vp1 -= 1;
	 vp2 += 1;
	 r0 = (sm0 + sm1) * 32768;
	 CLIP_TO_16BIT(r0);	 
	 *ap = r0;
	 ap += apofs;	 
      }
      
      vp1  = vvp+32+31;
      sm0  = vp1[0*64] * dsw[16][1];
      sm1  = vp1[1*64] * dsw[16][3];
      sm0 += vp1[2*64] * dsw[16][5];
      sm1 += vp1[3*64] * dsw[16][7];
      sm0 += vp1[4*64] * dsw[16][9];
      sm1 += vp1[5*64] * dsw[16][11];
      sm0 += vp1[6*64] * dsw[16][13];
      sm1 += vp1[7*64] * dsw[16][15];
      r0 = (sm0 + sm1) * 32768;
      CLIP_TO_16BIT(r0);	 
      *ap = r0;
      ap += apofs;
	 
      vp1 = vvp;
      vp2 = vvp+32+30;
      for (k=17;k<32;k++) 
      {  sm0  = vp1[0*64] * dsw[k][0];
	 sm1  = vp2[0*64] * dsw[k][1];
	 sm0 += vp1[1*64] * dsw[k][2];
	 sm1 += vp2[1*64] * dsw[k][3];
	 sm0 += vp1[2*64] * dsw[k][4];
	 sm1 += vp2[2*64] * dsw[k][5];
	 sm0 += vp1[3*64] * dsw[k][6];
	 sm1 += vp2[3*64] * dsw[k][7];
	 sm0 += vp1[4*64] * dsw[k][8];
	 sm1 += vp2[4*64] * dsw[k][9];
	 sm0 += vp1[5*64] * dsw[k][10];
	 sm1 += vp2[5*64] * dsw[k][11];
	 sm0 += vp1[6*64] * dsw[k][12];
	 sm1 += vp2[6*64] * dsw[k][13];
	 sm0 += vp1[7*64] * dsw[k][14];
	 sm1 += vp2[7*64] * dsw[k][15];	 
	 vp1 += 1;
	 vp2 -= 1;
         r0 = (sm0 + sm1) * 32768;
	 CLIP_TO_16BIT(r0);	 
	 *ap = r0;
	 ap += apofs;	 
      }      
      vvp -= 32;  
   }
      
   /* Prepare for the next frame's filtering */      
   if (scnt == 36)	/* TRUE for Layer 2 */
   {  register double *dp, *sp, d0, d1, d2, d3;
      
      dp = (double *) &vv[32*scnt];
      sp = (double *) &vv[0];
      for (k=0; k<30; k++)	/* (512-32) / (2*8) */
      {  d0 = sp[0]; d1 = sp[1]; d2 = sp[2]; d3 = sp[3];
         dp[0] = d0; dp[1] = d1; dp[2] = d2; dp[3] = d3;
         sp += 4; dp += 4;
         d0 = sp[0]; d1 = sp[1]; d2 = sp[2]; d3 = sp[3];
         dp[0] = d0; dp[1] = d1; dp[2] = d2; dp[3] = d3;
         sp += 4; dp += 4;
      }
   }
   else
   /* This is exactly the same as what the above is doing, but guarding
      against the overlapping regions. And we don't care much about the
      performance of already fast Layer_1 decoding.
   */
      memmove(&vv[32*scnt], &vv[0],(512-32)*sizeof(float));
}  


static void
doit2( nint maxsb, float smp[][32], float *vvp, nint scnt)
{  register float *vp1, (*np)[32];
   register float f0,f1,f2,f3,f4,f5,f6,f7,f8;
   register int k,n,m;

   f0=f1=f2=f3=f4=f5=f6=f7 = 0;
   for (n=0;n<scnt;n++)
   {  np = (float (*)[32])nn+1;
      vp1 = vvp-n*16;
      for (m=0;m<2;m++)
      {	 
	 for (k=0;k<=maxsb;k++)
	 {  f8 = smp[n][k];
	    f0 += np[0][k]  * f8;
	    f1 += np[2][k]  * f8;
	    f2 += np[4][k]  * f8;
	    f3 += np[6][k]  * f8;
	    f4 += np[8][k]  * f8;
	    f5 += np[10][k] * f8;
	    f6 += np[12][k] * f8;
	    f7 += np[14][k] * f8;
	 }
	 vp1[0] = f0;  f0 = 0;
	 vp1[1] = f1;  f1 = 0;
	 vp1[2] = f2;  f2 = 0;
	 vp1[3] = f3;  f3 = 0;
	 vp1[4] = f4;  f4 = 0;
	 vp1[5] = f5;  f5 = 0;
	 vp1[6] = f6;  f6 = 0;
	 vp1[7] = f7;  f7 = 0;
	 np += 16;
	 vp1+= 8;
      }     
   }
}
   

 
/*******************************************************************************
   Synthesis subband filter for  H a l f    S a m p l i n g   M o d e
*******************************************************************************/
static void
filt2(nint maxsb, float smp[][32], float *vv, int16 *ap, nint scnt, nint apofs)
{  register float *vvp, *vp1, *vp2;
   register float sm0, sm1;
   register int k,n, r0;

   vvp = vv+(scnt-1)*16;
   doit2(maxsb,smp,vvp,scnt);
   
   for (n=0;n<scnt;n++)
   {  vp1 = vvp+7;
      vp2 = vvp+16+7;
      for (k=0;k<16;k+=2) 
      {  sm0 = vp2[0*32] * dsw[k][1];
	 sm1 = vp2[1*32] * dsw[k][3];
	 sm0 -= vp1[0*32] * dsw[k][0];
	 sm1 -= vp1[1*32] * dsw[k][2];
	 sm0 -= vp1[2*32] * dsw[k][4];
	 sm1 += vp2[2*32] * dsw[k][5];
	 sm0 -= vp1[3*32] * dsw[k][6];
	 sm1 += vp2[3*32] * dsw[k][7];
	 sm0 -= vp1[4*32] * dsw[k][8];
	 sm1 += vp2[4*32] * dsw[k][9];
	 sm0 -= vp1[5*32] * dsw[k][10];
	 sm1 += vp2[5*32] * dsw[k][11];
	 sm0 -= vp1[6*32] * dsw[k][12];
	 sm1 += vp2[6*32] * dsw[k][13];
	 sm0 -= vp1[7*32] * dsw[k][14];
	 sm1 += vp2[7*32] * dsw[k][15];
         vp1 -= 1;
	 vp2 += 1;
	 r0 = (sm0 + sm1) * 32768;
	 CLIP_TO_16BIT(r0);	 
	 *ap = r0;
	 ap += apofs;	 
      }
      
      vp1 = vvp+16+15;
      sm0 = vp1[0*32] * dsw[16][1];
      sm1 = vp1[1*32] * dsw[16][3];
      sm0 += vp1[2*32] * dsw[16][5];
      sm1 += vp1[3*32] * dsw[16][7];
      sm0 += vp1[4*32] * dsw[16][9];
      sm1 += vp1[5*32] * dsw[16][11];
      sm0 += vp1[6*32] * dsw[16][13];
      sm1 += vp1[7*32] * dsw[16][15];
      r0 = (sm0 + sm1) * 32768;
      CLIP_TO_16BIT(r0);
      *ap = r0;
      ap += apofs;
	 
      vp1 = vvp;
      vp2 = vvp+16+14;
      for (k=18;k<32;k+=2) 
      {  sm0 = vp1[0*32] * dsw[k][0];
	 sm1 = vp2[0*32] * dsw[k][1];
	 sm0 += vp1[1*32] * dsw[k][2];
	 sm1 += vp2[1*32] * dsw[k][3];
	 sm0 += vp1[2*32] * dsw[k][4];
	 sm1 += vp2[2*32] * dsw[k][5];
	 sm0 += vp1[3*32] * dsw[k][6];
	 sm1 += vp2[3*32] * dsw[k][7];
	 sm0 += vp1[4*32] * dsw[k][8];
	 sm1 += vp2[4*32] * dsw[k][9];
	 sm0 += vp1[5*32] * dsw[k][10];
	 sm1 += vp2[5*32] * dsw[k][11];
	 sm0 += vp1[6*32] * dsw[k][12];
	 sm1 += vp2[6*32] * dsw[k][13];
	 sm0 += vp1[7*32] * dsw[k][14];
	 sm1 += vp2[7*32] * dsw[k][15];
	 vp1 += 1;
	 vp2 -= 1;
         r0 = (sm0 + sm1) * 32768;
	 CLIP_TO_16BIT(r0);
	 *ap = r0;
	 ap += apofs;	 
      }
      vvp -= 16;  
   }
   
   if (scnt == 36)
   {  register double *dp, *sp, d0, d1, d2, d3;
      dp = (double *) &vv[16*scnt];
      sp = (double *) &vv[0];
      for (k=0; k<15; k++)
      {  d0 = sp[0]; d1 = sp[1]; d2 = sp[2]; d3 = sp[3];
         dp[0] = d0; dp[1] = d1; dp[2] = d2; dp[3] = d3;
         sp += 4; dp += 4;
         d0 = sp[0]; d1 = sp[1]; d2 = sp[2]; d3 = sp[3];
         dp[0] = d0; dp[1] = d1; dp[2] = d2; dp[3] = d3;
         sp += 4; dp += 4;
      }
   }
   else
      memmove(&vv[16*scnt],&vv[0],(256-16)*sizeof(float));
   
}  


#define	 C116  0
#define	 C316  2
#define	 C516  4
#define	 C716  6
#define	 C18   1
#define	 C38   5
#define	 C14   3


static void 
fdct2(float *d, float *x, nint scnt)
{  register float f0,f1,f2,f3,f4,f5,f6,f7,t0,t1,t2,t3;
   
   do
   {
      f0 = d[0];
      f1 = d[7];
      f2 = d[1];
      f3 = f0+f1;	/* 0 */
      f4 = d[6];
      f5 = f0-f1;	/* 4 */
      f0 = d[2];
      f6 = f2+f4;	/* 1 */
      f1 = d[5];
      f7 = f2-f4;	/* 5 */
      f2 = d[3];
      t0 = f0+f1;	/* 3 */
      f4 = d[4];
      t1 = f0-f1;	/* 7 */
      t2 = lcos[C116];
      f0 = f2+f4;	/* 2 */
      f1 = f2-f4;	/* 6 */
      t3 = lcos[C316];
      
      f5 *= t2;		/* a */
      f2 = f3+f0;	/* e */
      t2 = lcos[C716];
      
      f7 *= t3;		/* b */
      f4 = f3-f0;	/* g */
      t3 = lcos[C516];
      
      f1 *= t2;		/* c */
      f3 = f6 + t0;	/* f */
      t2 = lcos[C18];
      
      t1 *= t3;		/* d */
      f0 = f6-t0;	/* h */
      t3 = lcos[C38];
      
      f4 *= t2;		/* gg */
      f6 = f2+f3;	/* m ->x[0] */
      t2 = lcos[C14];
      f0 *= t3;		/* hh */
      t0 = f2-f3;	/* n */
      x[7] = f6;
      t0 *= t2;		/* nn ->x[4] */
      f2 = f4-f0;	/* p */
      x[3] = t0;
      f2 *= t2;		/* p ->x[6] */
      f3 = f4+f0;	/* o */
      x[1] = f2;
      t0 = f2+f3;	/* u ->x[2] */
      
      f0 = f1+f5;	/* i */
      x[5] = t0;
      f2 = f5-f1;	/* k */
      t0 = lcos[C18];
      f3 = f7+t1;	/* j */
      f4 = f7-t1;	/* l */
      
      f2 *= t0;		/* kk */
      f6 = f0 - f3;	/* r */
      
      f4 *= t3;		/* ll */
      f7 = f0+f3;	/* q */
      
      f6 *= t2;		/* rr */
      f1 = f2-f4;	/* t */
      
      f1 *= t2;		/* tt */
      f5 = f2+f4;	/* s */
      
      f0 = f1+f5;	/* v */
      x[0] = f1;
      f2 = f1+f6;	/* y */
      f3 = f0+f7;	/* w */
      x[2] = f2;
      f4 = f6+f0;	/* z */
      x[6] = f3;
      x[4] = f4;
      
      x -= 8;
      d += 32;
   } while (--scnt);
}


/*******************************************************************************
   Synthesis subband filter for  Q u a r t e r   S a m p l i n g   M o d e
*******************************************************************************/
static void 
filt4(float smp[][32], float *vv, int16 *ap, nint scnt, nint apofs)
{  register float *vvp, *vp1, *vp2;
   register float sm0, sm1;
   register int k,n,r0;

   vvp = vv+(scnt-1)*8;
   fdct2(smp[0], vvp, scnt);
   
   for (n=0;n<scnt;n++)
   {  vp1 = vvp+3;
      vp2 = vvp+8+3;
      for (k=0;k<16;k+=4) 
      {  
	 sm0 = vp2[0*16] * dsw[k][1];
	 sm1 = vp2[1*16] * dsw[k][3];
         sm0 -= vp1[0*16] * dsw[k][0];
	 sm1 -= vp1[1*16] * dsw[k][2];
	 sm0 -= vp1[2*16] * dsw[k][4];
	 sm1 += vp2[2*16] * dsw[k][5];
	 sm0 -= vp1[3*16] * dsw[k][6];
	 sm1 += vp2[3*16] * dsw[k][7];
	 sm0 -= vp1[4*16] * dsw[k][8];
	 sm1 += vp2[4*16] * dsw[k][9];
	 sm0 -= vp1[5*16] * dsw[k][10];
	 sm1 += vp2[5*16] * dsw[k][11];
	 sm0 -= vp1[6*16] * dsw[k][12];
	 sm1 += vp2[6*16] * dsw[k][13];
	 sm0 -= vp1[7*16] * dsw[k][14];
	 sm1 += vp2[7*16] * dsw[k][15];
	 vp1 -= 1;
	 vp2 += 1;
         r0 = (sm0 + sm1) * -32768;
	 CLIP_TO_16BIT(r0);	 
	 *ap = r0;
	 ap += apofs;
      }
      
      vp1 = vvp+8+7;
      sm0 = vp1[0*16]  * dsw[16][1];
      sm1 = vp1[1*16]  * dsw[16][3];
      sm0 += vp1[2*16] * dsw[16][5];
      sm1 += vp1[3*16] * dsw[16][7];
      sm0 += vp1[4*16] * dsw[16][9];
      sm1 += vp1[5*16] * dsw[16][11];
      sm0 += vp1[6*16] * dsw[16][13];
      sm1 += vp1[7*16] * dsw[16][15];
      r0 = (sm0 + sm1) * -32768;
      CLIP_TO_16BIT(r0);	 
      *ap = r0;
      ap += apofs;
      
      vp1 = vvp;
      vp2 = vvp+8+6;
      for (k=20;k<32;k+=4) 
      {  sm0 = vp1[0*16] * dsw[k][0];
	 sm1 = vp2[0*16] * dsw[k][1];
	 sm0 += vp1[1*16] * dsw[k][2];
	 sm1 += vp2[1*16] * dsw[k][3];
	 sm0 += vp1[2*16] * dsw[k][4];
	 sm1 += vp2[2*16] * dsw[k][5];
	 sm0 += vp1[3*16] * dsw[k][6];
	 sm1 += vp2[3*16] * dsw[k][7];
	 sm0 += vp1[4*16] * dsw[k][8];
	 sm1 += vp2[4*16] * dsw[k][9];
	 sm0 += vp1[5*16] * dsw[k][10];
	 sm1 += vp2[5*16] * dsw[k][11];
	 sm0 += vp1[6*16] * dsw[k][12];
	 sm1 += vp2[6*16] * dsw[k][13];
	 sm0 += vp1[7*16] * dsw[k][14];
	 sm1 += vp2[7*16] * dsw[k][15];
	 vp1 += 1;
	 vp2 -= 1;
         r0 = (sm0 + sm1) * -32768;
	 CLIP_TO_16BIT(r0);	 
	 *ap = r0;
	 ap += apofs;
      }
      
      vvp -= 8;  
   }
   if (scnt == 36)
   {  register double *dp, *sp, d0, d1, d2, d3;
      dp = (double *) &vv[8*scnt];
      sp = (double *) &vv[0];
      for (k=0; k<15; k++)
      {  d0 = sp[0]; d1 = sp[1]; d2 = sp[2]; d3 = sp[3];
         dp[0] = d0; dp[1] = d1; dp[2] = d2; dp[3] = d3;
         sp += 4; dp += 4;
      }
   }
   else
      memmove(&vv[8*scnt],&vv[0],(128-8)*sizeof(float));
}

  



/**************************************************************************
   LAYER2 Bitstream decoder & subband sample dequantizer
**************************************************************************/   
static ubyte 
*decaudio2( ubyte *zzp, nuint aprms, float smplr[][36][32], adec_env *denv)
{  register int32 zzrmbits,sb,r0,r1,r2,r3,r4,k,m,n,gr,bitsum,bofs,nsb,ch;
   register uint32 zza,zzb,flip,*q;
   register ubyte  *base, (*p)[17], *r;
   register float (*smp)[32] ;
   register float f0,f1,f2,f3,f4,f5,f6,f7;
   nint saved_bofs, maxsub;
   ubyte *saved_zzp;
   float sb_scales[6];
   ubyte alloc[32][2];
   ubyte skip[33][2];
   ubyte ind[32][2];
   ubyte scfsi[64];
   uint32 scf[64];
   
   saved_zzp = zzp;
   zzrmbits = 0;
   bitsum = 0;   
   
   if (denv->qnttable > 1) p = qlev_cd;
   else p = qlev_ab;
   nsb = lk_nsb[denv->qnttable];    /* sblimit for this quantizer info table */
   
   maxsub = denv->maxsub;	    /* need to make a local copy */
   
   /* maxsub is the input to this routine indicating the highest subband 
      need to be processed for the choosen quality mode.
      But for this frames's bitrate&sampling_freq combination, highest 
      possible subband which might have been encoded, may well be smaller
      than the one which our choosen quality mode allows.
      So we need to adjust maxsub accordingly.
   */
   if (maxsub >= nsb) maxsub = nsb-1;
      
   r0 = 0;
   r1 = 0;
   r3 = 0;
   n = -1;
   
   /* This if_else section below decodes the allocation info for all
      the subbands. It is a deterministic procedure. We have three
      different loops, one for each of the Dual, Single and Joint modes.
   */
   if (aprms & AUD_DUAL)
   {  sb = 0;  /* Compilers love do loops... */
      do
      {  k = p[sb][16]; /* nbal value sored at index [16]
			   length of the allocation info for this subband */
	 GETNEXTS(k);
	 m = p[sb][zzb & ~(-1<<k)];
	 alloc[sb][LEFT] = m; /* store the quantizer index info
				 m==0 if no bits are allocated for this <sb,ch> */
	 r2 = 0;
	 if (m)
	 {  r2 = nnbits[m];   /* #bits used to code this <sb,ch> granules
				 1 granule is made up of 3 consequtive samples */
	    bitsum += r2;     /* Add to the count of bits making up a granule
				 from all the subbands. Note that this count is
				 fixed for all the granules, and since there
				 are 12 granules, the #bits used in this frame
				 to code all the subband samples will be equal
				 to 12*bitsum, a very valuable information
				 for our later processing... */
	    n += 1;	      /* n is the count of actually coded <sb,ch> s.
				 we initialized n to -1 so that it could be
				 used as an index value starting from 0 */
	    r0 = sb;	      /* r0 will contain the highest used subband
				 for this Left channel */
	 }
	 /* After the decoding of the allocation information for the subbands,
	    we will be decoding the scfsi (scale factor selection info) and
	    scalefactors for the allocated <sb,ch>s. Since we want to employ
	    generic, deterministic loops at those stages, regardless of audio
	    mode being Single,Dual or Joint, we have been keeping a count of
	    active (bit allocated) <sb,ch>s. So at those stages we will be
	    reading that many items from the bitstream without caring which
	    belongs to which <sb,ch>. But to be able to make the association
	    later on, we are storing in array "ind" an index into the logical
	    sequential list of active <sb,ch>s corresponding to each <sb,ch>.
	    Note that entries for actually inactive <sb,ch>s contain the index
	    for the last active <sb,ch>, which will be quite a convenience later on
	 */
	 ind[sb][LEFT] = n;
	 
	 /* Since we will be  decoding subband samples channel by channel and
	    subband by subband, we need to know the #bits between the same
	    channels of consequtive subbands. For the Right channel this length
	    is equal to the sum of bits of this subband's RCh and next
	    subband's Lch, either or both of which may be 0.
	    Thus r3 below is info from the previous loop, and it is also
	    why we had to initialize it to 0, for the case of first loop.
	    Note that skip[sb][] stores info for [sb+1]. So later on we
	    need to access it by [sb+1].
	    For the Left Channel, lenght is equal to the sum of bits of
	    this subband's Lch and Rch.
	 */
	 skip[sb][RIGHT] = r2 + r3;
	 
	 /* Now the same processing for the Left Channel. */
	 GETNEXTS(k);
	 m = p[sb][zzb & ~(-1<<k)];
	 alloc[sb][RIGHT] = m; 
	 r3 = 0;
	 if (m)
	 {  r3 = nnbits[m];
	    bitsum += r3;
	    n += 1;
	    r1 = sb;
	 }
	 ind[sb][RIGHT] = n;
	 skip[sb+1][LEFT] = r2 + r3;
	 sb++;
      } while (--nsb);
   }
   else if (aprms & AUD_SINGLE)
   {  sb = 0;
      do
      {  k = p[sb][16];
	 GETNEXTS(k);
	 m = p[sb][zzb & ~(-1<<k)];
	 alloc[sb][LEFT] = m;
	 r2 = 0;
	 if (m)
	 {  r2 = nnbits[m];
	    bitsum += r2;
	    n += 1;
	    r0 = sb;
	 }
	 ind[sb][LEFT] = n;
	 skip[sb+1][LEFT] = r2;
	 sb++;
      } while (--nsb); 
   }
   else	 /* JOINT_Stereo */
   {  r4 = denv->bound;
      /* We are extra cautious here against the possibility that the Bound
	 value may have been erroneously specified in the frame header
      */
      if (r4 > nsb) r4 = nsb;
      sb = 0;
      do    /* Process the allocation info for subbands before the bound */
      {  k = p[sb][16];
	 GETNEXTS(k);
	 m = p[sb][zzb & ~(-1<<k)];
	 alloc[sb][LEFT]  = m;
	 r2 = 0;
	 if (m)
	 {  r2 = nnbits[m];
	    bitsum += r2;
	    n += 1;
	    r0 = sb;
	 }
	 ind[sb][LEFT] = n;
	 skip[sb][RIGHT] = r2 + r3;
	 GETNEXTS(k);
	 m = p[sb][zzb & ~(-1<<k)];
	 alloc[sb][RIGHT] = m;
	 r3 = 0;
	 if (m)
	 {  r3 = nnbits[m];
	    bitsum += r3;
	    n += 1;
	    r1 = sb;
	 }
	 ind[sb][RIGHT] = n;
	 skip[sb+1][LEFT] = r2 + r3;
	 sb++;
      } while (--r4);
      skip[sb][RIGHT] = r3;   /* No more Left_channel sample bits to skip ! */
      
      /* Note below that even though info decoded is common to both channels
	 we store it twice, repeating for each channel, so that it will be
	 convenient when we selectively decode a frame for single channel
	 playback...
      */
      for (sb=denv->bound; sb<nsb; sb++)
      {  k = p[sb][16];
	 GETNEXTS(k);
	 m = p[sb][zzb & ~(-1<<k)];
	 alloc[sb][LEFT]  = m;
	 alloc[sb][RIGHT] = m;
	 r2 = 0;
	 if (m)
	 {  r2 = nnbits[m];
	    bitsum += r2;
	    n += 2;
	    r0 = sb;
	    r1 = sb;
	 }
	 ind[sb][LEFT]  = n-1;
	 ind[sb][RIGHT] = n;
	 skip[sb+1][LEFT] = r2;
	 skip[sb+1][RIGHT] = r2;
      }
      /* Here we are testing if either the highest subband need to be processed
	 as a result of the chosen quality mode is before the bound, or if we
	 at all actually encountered an active subband >= bound.
	 If there are no active subbands to be processed >= bound, or if we
	 won't be processing both channels, then we do not need to do
	 special joint_stereo mode processing in the subband samples 
	 decoding phase later on.
      */	 
      if ((aprms & (AUD_L | AUD_R)) == (AUD_L | AUD_R))
      {	 if (denv->bound <= maxsub && denv->bound <= r0)
	    aprms |= AUD_JOINTST;
      }
      /* First "if" checks to see if we are processing both channels.
	 In the second "if" we might as well have checked against r1.
	 Note that if we have at all encountered an active subband >= bound,
	 then r0=r1 & r0>=bound & r1>=bound.
      */
   }

   /* Compare the highest active subbands encountered in each channel against
      the highest subband which needs to be processed given the quality mode.
   */
   if (r0>maxsub) r0 = maxsub;
   if (r1>maxsub) r1 = maxsub;
   
   /* Establish the number of scalefactor groups (1 group per <sb,ch> and
      there may be upto 3 scalefactors in each group) which need to be read
      from the bitstream in the scalefactors decoding phase. Note that
      this way we avoid unnecessarily reading the scalefactors for 
      active subbands which will not be processed. Nevertheless we will
      still be reading scalefactor indices for the ignored channel,
      if there is one...
   */
   m = aprms & (AUD_L | AUD_R);
   if (m == (AUD_L | AUD_R))
   {  if (r0>r1) k = ind[r0][LEFT];
      else k = ind[r1][RIGHT];
   }
   else if (m == AUD_L) k = ind[r0][LEFT];
   else k = ind[r1][RIGHT];
      
   /* The information below is output from this routine, and it will be
      used in the synthesis subband filter routines. It indicates the
      highest subband to be processed in those routines for each channel.
      That is the smaller of the maxband value which was passed as input to 
      this routine, and the actual active subbands encountered in this
      frame decoding phase...
   */
   denv->maxband[LEFT]  = r0;
   denv->maxband[RIGHT] = r1;
   
   /* Below we are reading the scfsi for all the active <sb,ch>s.
      As we do this we store the read values after interpreting them 
      through the scbits[] lookup table.
	 scbits[scfsi] = i
	 i -> nnnnnnss
      where:
	 ss: scfsi
	 nnnnnn: 6 * #scalefactors scfsi specifies
		 (each scalefactor index is 6bits long)
   */
   r = scfsi;
   r1 = 0;
   do
   {  GETNEXTS(2);
      zzb &= 0x3;
      r0 = scbits[zzb];
      /* Keep a total count of bits used to code scalefactor indices */
      r1 += r0 >> 2;
      *r++ = r0;
      n--;
   } while (n>=0);
   
   
   /* At this point we know exactly how many more bits there are to be processed
      in this frame. We will compare this with the amount of bytes stated
      to be available at the input to this routine, and thus will never overflow.
      If the required number of bytes are not available, we will return
      immediately with the # required bytes, and hopefully this situation
      will be handled in the upper layers.
      base & bofs together precisely show our position in the bitstream
      after the scalefactor indices.
   */
   base = zzp - (zzrmbits>>3) - 1;     /* byte position */
   bofs = 8 - (zzrmbits & 0x7) + r1;   /* bit position, as offset from the 
					  byte position */
   
   r0 = base + ((7+bofs+bitsum*12) >>3) - saved_zzp;
   if (r0 > denv->dsize)
   {  denv->need = r0;
      return(0);
   }
   
   saved_bofs = bofs;
   flip = 1;
   flip <<= 31;
   r = scfsi;
   q = scf;
   
   do
   {  r0 = *r++;
      r1 = r0>>2;    /* throw away scfsi info bits*/
      /* read all the scalefactor indices in one shot for this <sb,ch> */
      GETNEXTS(r1);
      r0 &= 0x3;     /* isolate scfsi info */
      zzb <<= 2;
      r0 |= zzb;     /* will store scfsi and just read scalefactor indices */  
      *q++ = r0;
      k--;
   } while (k>=0);
     
   
   
   /* We will decode the subband samples one channel at a time. If both channels
      are to be processed first Left then the Right channel will be processed.
      In the Joint Stereo mode, if both channels are being processed, and if
      there are actually active joint subbands (AUD_JOINTST flag on), then
      those joint subband samples will be decoded seperately at the end, after
      the subband samples which belong to different channels are processed
      in the same code section as those of Single and Dual modes.
   */
      
   if (aprms & AUD_L)
   {  ch = 0;
      smp = smplr[0];	/* set pointer to Left Channel samples storage area */
   }
   else
   {  ch = 1;
      smp = smplr[1];
      bofs += nnbits[alloc[0][LEFT]];  /* skip the subband_0 Left Channel
					  sample bits */
      aprms ^= AUD_R;	/* Turn off the right_channel flag*/
   }

lb_nextchannel:

   r2 = denv->maxband[ch]; /* Highest subband to be processed for this channel */  
   
   /* If any joint stereo processing to be done, then now only decode subband
      samples which are not common to both channels
   */
   if ( aprms & AUD_JOINTST ) r2 = denv->bound - 1;
   
   
   for (sb=0; sb<=r2; sb++)
   {  /* #Bits to read for each sample or 3 grouped samples of this <sb,ch> */
      n = alloc[sb][ch];   
      /* Initialize a pointer to the first granule of this <sb,ch> */
      k = bofs;
      /* Update bofs to point to the first granule of next subband in this channel*/
      bofs += skip[sb+1][ch]; 
      
      /* If this is not an active <sb,ch> (no bits have been allocated), just fill
	 the corresponding sample locations in the output array with 0 values.
	 Note that we may still be encountering inactive subbands between active
	 subbands.
      */
      if (!n)
      {	 for (k=0;k<36;k++) smp[k][sb] = 0;
	 continue;
      }
      
      /* Retrieve the previously stored scalefactor indices for this <sb,ch> */
      r0 = scf[ind[sb][ch]];
      f2 = scalefacts[(r0>> 2) & 0x3f];	  /* get the actual scale factor */
      
      /* Based on the scfsi value, read more scalefactors and do the
	 assignments of scalefactors to granule groups of this <sb,ch>
      */
      switch (r0&0x3) {
      case 0:
	 f1 = scalefacts[(r0>> 8) & 0x3f];
	 f0 = scalefacts[(r0>>14) & 0x3f];
	 break;
      case 1:
	 f0 = scalefacts[(r0>> 8) & 0x3f];
	 f1 = f0;
	 break;
      case 2:
	 f0 = f1 = f2;
	 break;
      case 3:
	 f0 = scalefacts[(r0>> 8) & 0x3f];
	 f1 = f2;
	 break;
      }
      
      switch (n) {
	 case 1:
	    sb_scales[0] = f0;
	    sb_scales[1] = f1;
	    sb_scales[2] = f2;
	    
	    for (gr=0; gr<12; gr++)
	    {  zzp = base + (k>>3);
	       zza = zzp[0]<<8 | zzp[1];  /* need 5bits, but must read 2bytes */
	       zzrmbits = 16 - 5 - (k & 7);
	       f3 = sb_scales[gr>>2];
	       zzb = zza >> zzrmbits;
	       zzb &= 0x1f;
	       smp[3*gr][sb]   = f3 * grp5[zzb][0];
	       smp[3*gr+1][sb] = f3 * grp5[zzb][1];
	       smp[3*gr+2][sb] = f3 * grp5[zzb][2];
	       k += bitsum;
	    }
	    break;
	 case 2:
	    sb_scales[0] = f0;
	    sb_scales[1] = f1;
	    sb_scales[2] = f2;
	    
	    for (gr =0; gr<12; gr++)
	    {  zzp = base + (k>>3);
	       zza = zzp[0]<<8 | zzp[1];  /* need 7bits, but must read 2bytes */
	       zzrmbits = 16 - 7 - (k & 7);
	       f3 = sb_scales[gr>>2];
	       zzb = zza >> zzrmbits;
	       zzb &= 0x7f;
	       smp[3*gr][sb]   = f3 * grp7[zzb][0];
	       smp[3*gr+1][sb] = f3 * grp7[zzb][1];
	       smp[3*gr+2][sb] = f3 * grp7[zzb][2];
	       k += bitsum;
	    }
	    break;
	  case 4:
	    sb_scales[0] = f0;
	    sb_scales[1] = f1;
	    sb_scales[2] = f2;
	    
	    for (gr =0; gr<12; gr++)
	    {  zzp = base + (k>>3);
	       /* need 10bits, but must read 3bytes */
	       zza = zzp[0]<<16 | zzp[1]<<8 | zzp[2];
	       zzrmbits = 24 - 10 - (k & 7);
	       f3 = sb_scales[gr>>2];
	       zzb = zza >> zzrmbits;
	       zzb &= 0x3ff;
	       smp[3*gr][sb]   = f3 * grp10[zzb][0];
	       smp[3*gr+1][sb] = f3 * grp10[zzb][1];
	       smp[3*gr+2][sb] = f3 * grp10[zzb][2];
	       k += bitsum;
	    }
	    break;
	 default:
	    m  = nbits[n];
	    f3 = cd[n];
	    r1 = cd_add[n];
	    sb_scales[0] = f0 * f3;
	    sb_scales[1] = f1 * f3;
	    sb_scales[2] = f2 * f3;
	    
	    for (gr =0; gr<12; gr++)
	    {  zzp = base + (k>>3);
	       /* This reading of 3bytes will more than likely be enough for
		  the other 2 samples, and we will get away with a mere
		  3 MIPS cycle waste for them...
	       */
	       zza = zzp[0]<<24 | zzp[1]<<16 | zzp[2]<<8 | zzp[3];
	       zzp += 4;
	       zzrmbits = 32 - m - (k & 7);
	       f3 = sb_scales[gr>>2];
	       zzb = zza >> zzrmbits;
	       zzb <<= (32-m);	 /* align the most significant bit at bit 31 */
	       r0 = zzb ^ flip;	 /* flip it */
	       r0 >>= 16;	 /* align the most significant sign bit of the
				    sample at bit15, refer to previous exp. */
	       smp[3*gr+0][sb]=f3*(r0+r1); /* perform C * (s + D) */
	       GETNEXT16(m);
	       zzb <<= (32-m);
	       r0 = zzb ^ flip;
	       r0 >>= 16;
	       smp[3*gr+1][sb]=f3*(r0+r1);
	       GETNEXT16(m);
	       zzb <<= (32-m);
	       r0 = zzb ^ flip;
	       r0 >>= 16;
	       smp[3*gr+2][sb]=f3*(r0+r1);
	       k += bitsum;	    
	    }
      }
   }
   
   /* Prepare to decode the  samples of the Right Channel if there is one,
      and if it is to be processed.
   */
   if (aprms & AUD_R)
   {  ch = 1;
      smp = smplr[1];
      bofs = saved_bofs + nnbits[alloc[0][LEFT]];
      aprms ^= AUD_R;	/* Turn off the right_channel flag*/
      goto lb_nextchannel;
   }


   if ( !(aprms & AUD_JOINTST) )
   {  zzp = base + ((7+12*bitsum+saved_bofs) >>3);
      return(zzp);
   }
   
   /* Decode the joint subband samples in Joint Stereo mode */
   for (sb=denv->bound; sb<=denv->maxband[0]; sb++)
   {  n = alloc[sb][0];
      k = bofs;
      bofs += skip[sb+1][0];
      
      if (!n)
      {	 for (k=0;k<36;k++) 
	 {  smplr[0][k][sb] = 0;
	    smplr[1][k][sb] = 0;
	 }
	 continue;
      }
      
      r0 = scf[ind[sb][0]];
      f2 = scalefacts[(r0>> 2) & 0x3f];      
      switch (r0&0x3) {
      case 0:
	 f1 = scalefacts[(r0>> 8) & 0x3f];
	 f0 = scalefacts[(r0>>14) & 0x3f];
	 break;
      case 1:
	 f0 = scalefacts[(r0>> 8) & 0x3f];
	 f1 = f0;
	 break;
      case 2:
	 f0 = f1 = f2;
	 break;
      case 3:
	 f0 = scalefacts[(r0>> 8) & 0x3f];
	 f1 = f2;
	 break;
      }      
      
      r0 = scf[ind[sb][1]];
      f6 = scalefacts[(r0>> 2) & 0x3f];      
      switch (r0&0x3) {
      case 0:
	 f5 = scalefacts[(r0>> 8) & 0x3f];
	 f4 = scalefacts[(r0>>14) & 0x3f];
	 break;
      case 1:
	 f4 = scalefacts[(r0>> 8) & 0x3f];
	 f5 = f4;
	 break;
      case 2:
	 f4 = f5 = f6;
	 break;
      case 3:
	 f4 = scalefacts[(r0>> 8) & 0x3f];
	 f5 = f6;
	 break;
      }      
      
      switch (n) {
	 case 1:
	    sb_scales[0] = f0;
	    sb_scales[1] = f1;
	    sb_scales[2] = f2;
	    sb_scales[3] = f4;
	    sb_scales[4] = f5;
	    sb_scales[5] = f6;
	    
	    for (gr =0; gr<12; gr++)
	    {  zzp = base + (k>>3);
	       zza = zzp[0]<<8 | zzp[1];
	       zzrmbits = 16 - 5 - (k & 7);
	       f3 = sb_scales[gr>>2];
	       f7 = sb_scales[(gr>>2)+3];
	       zzb = zza >> zzrmbits;
	       zzb &= 0x1f;
	       smplr[0][3*gr+0][sb] = f3 * grp5[zzb][0];
	       smplr[1][3*gr+0][sb] = f7 * grp5[zzb][0];
	       smplr[0][3*gr+1][sb] = f3 * grp5[zzb][1];
	       smplr[1][3*gr+1][sb] = f7 * grp5[zzb][1];
	       smplr[0][3*gr+2][sb] = f3 * grp5[zzb][2];
	       smplr[1][3*gr+2][sb] = f7 * grp5[zzb][2];
	       k += bitsum;
	    }
	    break;
	 case 2:
	    sb_scales[0] = f0;
	    sb_scales[1] = f1;
	    sb_scales[2] = f2;
	    sb_scales[3] = f4;
	    sb_scales[4] = f5;
	    sb_scales[5] = f6;
	    
	    for (gr =0; gr<12; gr++)
	    {  zzp = base + (k>>3);
	       zza = zzp[0]<<8 | zzp[1];
	       zzrmbits = 16 - 7 - (k & 7);
	       f3 = sb_scales[gr>>2];
	       f7 = sb_scales[(gr>>2)+3];
	       zzb = zza >> zzrmbits;
	       zzb &= 0x7f;
	       smplr[0][3*gr+0][sb] = f3 * grp7[zzb][0];
	       smplr[1][3*gr+0][sb] = f7 * grp7[zzb][0];
	       smplr[0][3*gr+1][sb] = f3 * grp7[zzb][1];
	       smplr[1][3*gr+1][sb] = f7 * grp7[zzb][1];
	       smplr[0][3*gr+2][sb] = f3 * grp7[zzb][2];
	       smplr[1][3*gr+2][sb] = f7 * grp7[zzb][2];
	       k += bitsum;
	    }
	    break;
	  case 4:
	    sb_scales[0] = f0;
	    sb_scales[1] = f1;
	    sb_scales[2] = f2;
	    sb_scales[3] = f4;
	    sb_scales[4] = f5;
	    sb_scales[5] = f6;
	    
	    for (gr =0; gr<12; gr++)
	    {  zzp = base + (k>>3);
	       zza = zzp[0]<<16 | zzp[1]<<8 | zzp[2];
	       zzrmbits = 24 - 10 - (k & 7);
	       f3 = sb_scales[gr>>2];
	       f7 = sb_scales[(gr>>2)+3];
	       zzb = zza >> zzrmbits;
	       zzb &= 0x3ff;
	       smplr[0][3*gr+0][sb] = f3 * grp10[zzb][0];
	       smplr[1][3*gr+0][sb] = f7 * grp10[zzb][0];
	       smplr[0][3*gr+1][sb] = f3 * grp10[zzb][1];
	       smplr[1][3*gr+1][sb] = f7 * grp10[zzb][1];
	       smplr[0][3*gr+2][sb] = f3 * grp10[zzb][2];
	       smplr[1][3*gr+2][sb] = f7 * grp10[zzb][2];
	       k += bitsum;
	    }
	    break;
	 default:
	    m  = nbits[n];
	    f3 = cd[n];
	    r1 = cd_add[n];
	    sb_scales[0] = f0*f3;
	    sb_scales[1] = f1*f3;
	    sb_scales[2] = f2*f3;
	    sb_scales[3] = f4*f3;
	    sb_scales[4] = f5*f3;
	    sb_scales[5] = f6*f3;
	    
	    for (gr =0; gr<12; gr++)
	    {  zzp = base + (k>>3);
	       zza = zzp[0]<<24 | zzp[1]<<16 | zzp[2]<<8 | zzp[3];
	       zzp += 4;
	       zzrmbits = 32 - m - (k & 7);
	       f3 = sb_scales[gr>>2];
	       f7 = sb_scales[(gr>>2)+3];
	       zzb = zza >> zzrmbits;
	       zzb <<= (32-m);
	       r0 = zzb ^ flip;
	       r0 >>= 16;
	       smplr[0][3*gr+0][sb]=f3*(r0+r1);
	       smplr[1][3*gr+0][sb]=f7*(r0+r1);
	       GETNEXT16(m);
	       zzb <<= (32-m);
	       r0 = zzb ^ flip;
	       r0 >>= 16;
	       smplr[0][3*gr+1][sb]=f3*(r0+r1);
	       smplr[1][3*gr+1][sb]=f7*(r0+r1);
	       GETNEXT16(m);
	       zzb <<= (32-m);
	       r0 = zzb ^ flip;
	       r0 >>= 16;
	       smplr[0][3*gr+2][sb]=f3*(r0+r1);
	       smplr[1][3*gr+2][sb]=f7*(r0+r1);
	       k += bitsum;	    
	    }
      }
   }

   zzp = base + ((7+12*bitsum+saved_bofs) >>3);
   return(zzp); 
}



/**************************************************************************
   LAYER1 Bitstream decoder & subband sample dequantizer
   Refer to the explanations of decaudio2
**************************************************************************/   
static ubyte 
*decaudio1( ubyte *zzp, nuint aprms, float smplr[][36][32], adec_env *denv)
{  register int32 zzrmbits,sb,r0,r1,r2,r3,r4,k,m,n,gr,bitsum,bofs,ch;
   register uint32 zza,zzb,flip;
   register ubyte  *base, *r;
   register float (*smp)[32] ;
   register float f0,f1;
   nint saved_bofs, maxsub;
   ubyte *saved_zzp;
   ubyte alloc[32][2];
   ubyte skip[33][2];
   ubyte ind[32][2];
   ubyte scaleind[64];
   
   
   zzrmbits = 0;
   bitsum = 0;   
   maxsub = denv->maxsub;
   saved_zzp = zzp;
   
   r0 = 0;
   r1 = 0;
   r3 = 0;
   n = -1;
   
   if (aprms & AUD_DUAL)
   {  for (sb=0;sb<32;sb++)
      {  zzb = *zzp++;
	 r2 = zzb >> 4;
	 if (r2)
	 {  r2 += 1;
	    bitsum += r2;
	    n += 1;
	    r0 = sb;
	 }
	 alloc[sb][LEFT] = r2;
	 ind[sb][LEFT] = n;
	 skip[sb][RIGHT] = r2 + r3;
	 
	 r3 = zzb & 0xf;
	 if (r3)
	 {  r3 += 1;
	    bitsum += r3;
	    n += 1;
	    r1 = sb;
	 }
	 alloc[sb][RIGHT] = r3; 
	 ind[sb][RIGHT] = n;
	 skip[sb+1][LEFT] = r2 + r3;
      }
   }
   else if (aprms & AUD_SINGLE)
   {  for (sb=0;sb<16;sb++)
      {  GETNEXTS(4);
	 r2 = zzb & 0xf;
	 if (r2)
	 {  r2 += 1;
	    bitsum += r2;
	    n += 1;
	    r0 = sb;
	 }
	 alloc[sb][LEFT] = r2;
	 ind[sb][LEFT] = n;
	 skip[sb+1][LEFT] = r2; 
      }
   }
   else
   {  r4 = denv->bound;
      sb = 0;
      do
      {  zzb = *zzp++;
	 r2 = zzb >> 4;
	 if (r2)
	 {  r2 += 1;
	    bitsum += r2;
	    n += 1;
	    r0 = sb;
	 }
	 alloc[sb][LEFT] = r2;
	 ind[sb][LEFT] = n;
	 skip[sb][RIGHT] = r2 + r3;
	 
	 r3 = zzb & 0xf;
	 if (r3)
	 {  r3 += 1;
	    bitsum += r3;
	    n += 1;
	    r1 = sb;
	 }
	 alloc[sb][RIGHT] = r3; 
	 ind[sb][RIGHT] = n;
	 skip[sb+1][LEFT] = r2 + r3;
	 sb++;
      } while (--r4);
      
      skip[sb][RIGHT] = r3;
      
      for (sb=denv->bound; sb<32; sb++)
      {  GETNEXTS(4);
	 r2 = zzb & 0xf;
	 if (r2)
	 {  r2 += 1;
	    bitsum += r2;
	    n += 2;
	    r0 = sb;
	    r1 = sb;
	 }
	 alloc[sb][LEFT]  = r2;
	 alloc[sb][RIGHT] = r2;
	 ind[sb][LEFT]  = n-1;
	 ind[sb][RIGHT] = n;
	 skip[sb+1][LEFT] = r2;
	 skip[sb+1][RIGHT] = r2;
      }
      if ((aprms & (AUD_L | AUD_R)) == (AUD_L | AUD_R))
      {	 if (denv->bound <= maxsub && denv->bound <= r0)
	    aprms |= AUD_JOINTST;
      }
   }

   
   if (r0>maxsub) r0 = maxsub;
   if (r1>maxsub) r1 = maxsub;
   
   m = aprms & (AUD_L | AUD_R);
   if (m == (AUD_L | AUD_R))
   {  if (r0>r1) k = ind[r0][LEFT];
      else k = ind[r1][RIGHT];
   }
   else if (m == AUD_L) k = ind[r0][LEFT];
   else k = ind[r1][RIGHT];
      
   denv->maxband[LEFT]  = r0;
   denv->maxband[RIGHT] = r1;
   
   
   base = zzp - (zzrmbits>>3) - 1;
   bofs = 8 - (zzrmbits & 0x7) + (n+1)*6 ;
   
   r0 = base + ((7+bofs+bitsum*12) >>3) - saved_zzp;
   if (r0 > denv->dsize)
   {  denv->need = r0;
      return(0);
   }
   
   saved_bofs = bofs;
   flip = 1;
   flip <<= 31;
   
   r = scaleind;
   do
   {  GETNEXTS(6);
      *r++ = zzb & 0x3f;
      k--;
   } while (k>=0);
    
   if (aprms & AUD_L)
   {  ch = 0;
      smp = smplr[0];
   }
   else
   {  ch = 1;
      smp = smplr[1];
      bofs += alloc[0][LEFT];
      aprms ^= AUD_R;
   }

lb_nextchannel:

   r2 = denv->maxband[ch];  
   if ( aprms & AUD_JOINTST ) r2 = denv->bound - 1;
   
   
   for (sb=0; sb<=r2; sb++)
   {  n = alloc[sb][ch];
      k = bofs;
      bofs += skip[sb+1][ch];
      
      if (!n)
      {	 for (k=0;k<12;k++) smp[k][sb] = 0;
	 continue;
      }
      
      f0 = scalefacts[scaleind[ind[sb][ch]]] * cd1[n];
      r1 = 1<<(16-n);
      	    
      for (gr=0; gr<12; gr++)
      {	 zzp = base + (k>>3);
	 zza = zzp[0]<<16 | zzp[1]<<8 | zzp[2];
	 zzrmbits = 24 - n - (k & 7);
	 zzb = zza >> zzrmbits;
	 zzb <<= (32-n);
	 r0 = zzb ^ flip;
	 r0 >>= 16;
	 smp[gr][sb] = f0*(r0+r1);
	 k += bitsum;	    
      }      
   }
   
   if (aprms & AUD_R)
   {  ch = 1;
      smp = smplr[1];
      bofs = saved_bofs + alloc[0][LEFT];
      aprms ^= AUD_R;
      goto lb_nextchannel;
   }

   if ( !(aprms & AUD_JOINTST) ) goto lb_endofdecode;
  
   for (sb=denv->bound; sb<=denv->maxband[0]; sb++)
   {  n = alloc[sb][0];
      k = bofs;
      bofs += skip[sb+1][0];
      
      if (!n)
      {	 for (k=0;k<12;k++) 
	 {  smplr[0][k][sb] = 0;
	    smplr[1][k][sb] = 0;
	 }
	 continue;
      }
      
      f0 = scalefacts[scaleind[ind[sb][0]]] * cd1[n];
      f1 = scalefacts[scaleind[ind[sb][1]]] * cd1[n];
      r1 = 1<<(16-n);
          
      for (gr =0; gr<12; gr++)
      {	 zzp = base + (k>>3);
	 zza = zzp[0]<<16 | zzp[1]<<8 | zzp[2];
	 zzrmbits = 24 - n - (k & 7);
	 zzb = zza >> zzrmbits;
	 zzb <<= (32-n);
	 r0 = zzb ^ flip;
	 r0 >>= 16;
	 smplr[0][gr][sb]=f0*(r0+r1);
	 smplr[1][gr][sb]=f1*(r0+r1);
	 k += bitsum;	    
      }
   }

lb_endofdecode:
   zzp = base + ((7+12*bitsum+saved_bofs) >>3) ;
   return(zzp); 
}

/****************************************************************************
   Decode a Layer1/2 MPEG Audio frame.
   
   d  : Pointer to the first byte of the frame header.
   
   Returns a pointer to the byte after the last byte processed if
   everything goes smoothly, otherwise returns 0 with the error code in
   ast->status.
****************************************************************************/
ubyte *mp_daframe(ubyte *d, mp_aprms *ast)
{  int32 r0,r1,r2,r3,listen,hdrsize;
   nuint aprms;
   adec_env denv;
   
   hdrsize = 4;
   r0 = d[0]<<8 | d[1];
   r1 = d[2]<<8 | d[3];
   if (!(r0 & 1)) 
   {  d+=2;
      hdrsize = 6;
   }
   d +=4;
   if ((r0 & 0xfff8) != 0xfff8)
   {  /* Can't find either the sync pattern bits 0xfff, or the MPEG ID bit */
      ast->status = AERR_00;
      return(0);
   }
   
   r2 = r0 & 0x6; /* layer bits, left shifted by one bit position*/
   aprms = 0;
   if (r2==2<<1) aprms |= AUD_LAYER2;
   else if (r2==3<<1) aprms |= AUD_LAYER1;
   else 
   {  /* LAYER 3 or Reserved layer */
      ast->status = AERR_01;
      return(0);
   }   
   
   r2 = (r1>>10) & 0x3f;   /* Get the bitrate_index & sampling frequency */
   listen = ast->listen & 0x3;
   /* Examine mode bits */
   if ((r1 & 0x40) == 0) aprms |= AUD_DUAL;	  /* Dual or Stereo mode */
   else if (r1 & 0x80) 
   {  aprms |= AUD_SINGLE;
      r2 |= 0x40;
      listen = AUD_L;	   /* Ignore channel decode selections in single
			      channel mode... */
   }
   else 
   {  aprms |= AUD_JOINT;
      denv.bound = (((r1>>4) & 0x3) + 1) << 2;
   }
   
   if (aprms & AUD_LAYER1) goto lb_layer1bypass;
   
   r3 = lk_qnttable[r2];   /* Get the index into one of the 4 qnt. tables */
   if (r3 > 3)
   {  /* Illegal bitrate-sampling frequency combination */
      ast->status = AERR_02;
      return(0);
   }
   denv.qnttable = r3;

lb_layer1bypass:   
   
   denv.maxsub = lk_maxsub[ast->quality & 0x3];
   aprms |= listen;
   
   denv.dsize = ast->dsize - hdrsize;;
   if (aprms & AUD_LAYER2)
   {  d = decaudio2( d, aprms, ast->smp, &denv);
      r1 = 1152;
      r2 = 36;
   }
   else
   {  d = decaudio1( d, aprms, ast->smp, &denv);
      r1 = 384;
      r2 = 12;
   }
   
   if (!d)
   {  ast->status = AERR_03;
      ast->need = denv.need + hdrsize;
      return(0);
   }

   r1 >>= ast->quality;
   
   aprms &= AUD_L | AUD_R;
   if (aprms == ( AUD_L | AUD_R ))
   {  r1 <<= 1;   /* because we are stereo */
      switch (ast->quality) {
      case 0:
	 filt1(denv.maxband[0], ast->smp[0], ast->vv[0], ast->ap,r2,2);
	 filt1(denv.maxband[1], ast->smp[1], ast->vv[1], ast->ap+1,r2,2);
	 break;
      case 1:
	 filt2(denv.maxband[0], ast->smp[0], ast->vv[0], ast->ap,r2,2);
	 filt2(denv.maxband[1], ast->smp[1], ast->vv[1], ast->ap+1,r2,2);
	 break;
      case 2:
	 filt4(ast->smp[0], ast->vv[0], ast->ap,r2,2);
	 filt4(ast->smp[1], ast->vv[1], ast->ap+1,r2,2);
	 break;
      }
   }
   else
   {  if (aprms == AUD_L) r3 = 0;
      else r3 = 1;
      switch (ast->quality) {
      case 0:
	 filt1(denv.maxband[r3], ast->smp[r3], ast->vv[r3], ast->ap,r2,1);
	 break;
      case 1:
	 filt2(denv.maxband[r3], ast->smp[r3], ast->vv[r3], ast->ap,r2,1);
	 break;
      case 2:
	 filt4(ast->smp[r3], ast->vv[r3], ast->ap,r2,1);
	 break;
      }
   }
   ast->ap += r1;
   ast->nsmp = r1;
   return(d);   
}

/* Clear the Synthesis subband filter working buffers.
   This is currently doing much more work than required...
   Actual work to be done depends on the quality, channels and layer.
*/
void
mp_claudbf(float *vv)
{  nint k;
   
   k = 2*52*32 >> 3;
   do
   {  vv[0] = 0;
      vv[1] = 0;
      vv[2] = 0;
      vv[3] = 0;
      vv[4] = 0;
      vv[5] = 0;
      vv[6] = 0;
      vv[7] = 0;
      vv += 8;
      k--;
   } while (k);
}     

