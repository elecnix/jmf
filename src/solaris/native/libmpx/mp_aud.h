/*
 * @(#)mp_aud.h	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#if !defined(MPTYPES)
#define MPTYPES
typedef  unsigned char 	ubyte;
typedef  signed char  	sbyte;
#ifndef WIN32
typedef  unsigned long long  uint64;
typedef  long long int64;
#endif
#ifdef WIN32
#define M_PI 3.1415926
#define M_SQRT1_2 0.70710678118654752440
#endif

typedef	 unsigned int	uint32;
typedef	 int   	     	int32;
typedef	 unsigned short	uint16;
typedef	 short   	int16;
typedef	 int		nint;
typedef	 unsigned int	nuint;
typedef	 unsigned short	nushort;
typedef	 short   	nshort;
#endif

#define	 LEFT  0
#define	 RIGHT 1
#define	 AUD_L	     (1)
#define	 AUD_R	     (1<<1)
#define	 AUD_LR	     (AUD_L | AUD_R)
#define	 AUD_JOINT   (1<<2)
#define	 AUD_DUAL    (1<<3)
#define	 AUD_SINGLE  (1<<4)
#define	 AUD_JOINTST (1<<5)
#define	 AUD_LAYER1  (1<<6)
#define	 AUD_LAYER2  (1<<7)

/* largest possible non-free-bitrate frame size is:
      Layer_2 : (384000*144)/32000 = 1728 bytes
      Layer_1 : (448000*48)/32000  = 672 bytes
*/

#define	 FREE_BRATE  (1<<11)  /* a value larger than any regular frame size */


#define	 AERR_00  0  /* Sync pattern bits or the MPEG ID bit not found */
#define	 AERR_01  1  /* Unsupported layer encountered */
#define	 AERR_02  2  /* Illegal bitrate-sampling_rate combination */
#define	 AERR_03  3  /* Required # bytes for frame decode are not in buffer */

typedef struct {
   int16 *ap;	     /* Output decoded audio samples buffer pointer  */
   ubyte quality;    /* Audio output quality
			0: Highest quality level, full sample rate, all subbands
			1: Half the sampling rate, subbands 0-15
			2: Quarter the sampling rate, subbands 0-7   */
   ubyte listen;     /*	Channels of audio to be decoded
			bits 0&1 are used
			Left_Channel only, Right_Channel only, or both	*/
   nint dsize;	     /* #bytes available in the decode data buffer starting
			with the first byte of the frame header.  */
   uint16 need;	     /* #bytes required in the decode data buffer for
			complete frame decode starting with the first byte
			of the frame header. */
   uint16 nsmp;	     /* #decoded audio samples generated
			depends on the layer and #channels processed.
			possible values are [384, 2*384, 1152, 2*1152]	*/
   uint16 status;    /* Indicates the error in case of bad return from
			mp_daframe  */
   float (*vv)[52*32];	   /* Synthesis subband filter working buffer */
   float (*smp)[36][32];   /* Decoded subband samples buffer */   
} mp_aprms;


ubyte *mp_daframe(ubyte *d, mp_aprms *p);
void mp_initaudio();
void mp_claudbf(float *vv);
extern uint16 lk_framesz[];

