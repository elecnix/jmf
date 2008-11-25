/*
 * @(#)RgbToRgb.c	1.9 98/11/18
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
/* almost definitely will crash on Solaris because of the alignment problems in 24 bit RGB formats*/

#include <stdlib.h>
#include <stdio.h>
#include "RgbToRgb.h"

#define INNER0(out, in, m1, s1, m2, s2, m3, s3)  *in  = (*out & (m1)) >> (s1); \
*in |= (*out & (m2)) >> s2; \
*in |= (*out & (m3)) >> s3;

#define INNER1(out, in, m1, s1, m2, s2, m3, s3)  *in  = (*out & (m1)) << s1; \
*in |= (*out & (m2)) >> s2; \
*in |= (*out & (m3)) >> s3;

#define INNER2(out, in, m1, s1, m2, s2, m3, s3)  *in  = (*out & (m1)) >> s1; \
*in |= (*out & (m2)) << s2; \
*in |= (*out & (m3)) >> s3;

#define INNER3(out, in, m1, s1, m2, s2, m3, s3)  *in  = (*out & (m1)) << s1; \
*in |= (*out & (m2)) << s2; \
*in |= (*out & (m3)) >> s3;

#define INNER4(out, in, m1, s1, m2, s2, m3, s3)  *in  = (*out & (m1)) >> s1; \
*in |= (*out & (m2)) >> s2; \
*in |= (*out & (m3)) << s3;

#define INNER5(out, in, m1, s1, m2, s2, m3, s3)  *in  = (*out & (m1)) << s1; \
*in |= (*out & (m2)) >> s2; \
*in |= (*out & (m3)) << s3;

#define INNER6(out, in, m1, s1, m2, s2, m3, s3)  *in  = (*out & (m1)) >> s1; \
*in |= (*out & (m2)) << s2; \
*in |= (*out & (m3)) << s3;

#define INNER7(out, in, m1, s1, m2, s2, m3, s3)  *in  = (*out & (m1)) << s1; \
*in |= (*out & (m2)) << s2; \
*in |= (*out & (m3)) << s3;

#define BODY(inc, intype, outtype) \
  if (rS < 0) {							\
    index |= 1;							\
    rS = -rS;							\
  }								\
  if (gS < 0) {							\
    index |= 2;							\
    gS = -gS;							\
  }								\
  if (bS < 0) {							\
    index |= 4;							\
    bS = -bS;							\
  }								\
  switch (index) {						\
  case 0:				        		\
    end = (intype)(((char *) in) + width);			\
    for (row = 0; row < y; row++) {				\
      while(in < end) {						\
	INNER0(in, out, rM, rS, gM, gS, bM, bS);		\
	inc        						\
	  }      						\
      in = (intype)(((char*) in) + stride1 - width);   		\
      end = (intype)(((char *) end) + stride1);	       		\
      out = (outtype)(((char *) out) + stride2);       		\
    }                                           		\
    break;							\
  case 1:							\
    end = (intype)(((char *) in) + width);     			\
    for (row = 0; row < y; row++) {				\
      while(in < end) {						\
	INNER1(in, out, rM, rS, gM, gS, bM, bS);		\
	inc       						\
	  }							\
      in = (intype)(((char*) in) + stride1 - width);   		\
      end = (intype)(((char *) end) + stride1);	       		\
      out = (outtype)(((char *) out) + stride2);       		\
    }								\
    break;							\
  case 2:							\
    end = (intype)(((char *) in) + width);     			\
    for (row = 0; row < y; row++) {				\
      while(in < end) {						\
	INNER2(in, out, rM, rS, gM, gS, bM, bS);		\
	inc        						\
	  }							\
      in = (intype)(((char*) in) + stride1 - width);   		\
      end = (intype)(((char *) end) + stride1);	       		\
      out = (outtype)(((char *) out) + stride2);       		\
    }								\
    break;							\
  case 3:							\
    end = (intype)(((char *) in) + width);     			\
    for (row = 0; row < y; row++) {				\
      while(in < end) {						\
	INNER3(in, out, rM, rS, gM, gS, bM, bS);		\
	inc       						\
	  }							\
      in = (intype)(((char*) in) + stride1 - width);   		\
      end = (intype)(((char *) end) + stride1);	       		\
      out = (outtype)(((char *) out) + stride2);       		\
    }								\
    break;							\
  case 4:							\
    end = (intype)(((char *) in) + width);     			\
    for (row = 0; row < y; row++) {				\
      while(in < end) {						\
	INNER4(in, out, rM, rS, gM, gS, bM, bS);		\
	inc       						\
	  }							\
      in = (intype)(((char*) in) + stride1 - width);   		\
      end = (intype)(((char *) end) + stride1);	       		\
      out = (outtype)(((char *) out) + stride2);       		\
    }								\
    break;							\
  case 5:							\
    end = (intype)(((char *) in) + width);     			\
    for (row = 0; row < y; row++) {				\
      while(in < end) {						\
	INNER5(in, out, rM, rS, gM, gS, bM, bS);		\
	inc        						\
	  }							\
      in = (intype)(((char*) in) + stride1 - width);   		\
      end = (intype)(((char *) end) + stride1);	       		\
      out = (outtype)(((char *) out) + stride2);       		\
    }								\
    break;							\
  case 6:							\
    end = (intype)(((char *) in) + width);     			\
    for (row = 0; row < y; row++) {				\
      while(in < end) {						\
	INNER6(in, out, rM, rS, gM, gS, bM, bS);		\
	inc        						\
	  }							\
      in = (intype)(((char*) in) + stride1 - width);   		\
      end = (intype)(((char *) end) + stride1);	       		\
      out = (outtype)(((char *) out) + stride2);       		\
    }								\
    break;							\
  case 7:							\
    end = (intype)(((char *) in) + width);     			\
    for (row = 0; row < y; row++) {				\
      while(in < end) {						\
	INNER7(in, out, rM, rS, gM, gS, bM, bS);		\
	inc       						\
	  }							\
      in = (intype)(((char*) in) + stride1 - width);    	\
      end = (intype)(((char *) end) + stride1);	       		\
      out = (outtype)(((char *) out) + stride2);       		\
    }								\
    break;							\
  }

#define BODYMOD(inc) \
  if (rS < 0) {								\
    index |= 1;								\
    rS = -rS;								\
  }									\
  if (gS < 0) {								\
    index |= 2;								\
    gS = -gS;								\
  }									\
  if (bS < 0) {								\
    index |= 4;								\
    bS = -bS;								\
  }									\
  switch (index) {							\
  case 0:								\
    for (row = 0, end = input+3*width-1; 				\
	 row < y; 							\
	 row++, input+=stride1-3*width,end+=stride1, out+=stride2-width)	\
    while(input< end) {							\
      INNER0(in, out, rM, rS, gM, gS, bM, bS);				\
      inc        							\
    }									\
    break;								\
  case 1:								\
    for (row = 0, end = input+3*width-1; 					\
	 row < y; 							\
	 row++, input+=stride1-3*width,end+=stride1, out+=stride2-width)	\
    while(input< end) {							\
      INNER1(in, out, rM, rS, gM, gS, bM, bS);				\
      inc       							\
    }									\
    break;								\
  case 2:								\
    for (row = 0, end = input+3*width-1; 					\
	 row < y; 							\
	 row++, input+=stride1-3*width,end+=stride1, out+=stride2-width)	\
    while(input< end) {							\
      INNER2(in, out, rM, rS, gM, gS, bM, bS);				\
      inc        							\
    }									\
    break;								\
  case 3:								\
    for (row = 0, end = input+3*width-1; 					\
	 row < y; 							\
	 row++, input+=stride1-3*width,end+=stride1, out+=stride2-width)	\
    while(input< end) {							\
      INNER3(in, out, rM, rS, gM, gS, bM, bS);				\
      inc       							\
    }									\
    break;								\
  case 4:								\
    for (row = 0, end = input+3*width-1; 					\
	 row < y; 							\
	 row++, input+=stride1-3*width,end+=stride1, out+=stride2-width)	\
    while(input< end) {							\
      INNER4(in, out, rM, rS, gM, gS, bM, bS);				\
      inc       							\
    }									\
    break;								\
  case 5:								\
    for (row = 0, end = input+3*width-1; 					\
	 row < y; 							\
	 row++, input+=stride1-3*width,end+=stride1, out+=stride2-width)	\
    while(input< end) {							\
      INNER5(in, out, rM, rS, gM, gS, bM, bS);				\
      inc        							\
    }									\
    break;								\
  case 6:								\
    for (row = 0, end = input+3*width-1; 					\
	 row < y; 							\
	 row++, input+=stride1-3*width,end+=stride1, out+=stride2-width)	\
    while(input< end) {							\
      INNER6(in, out, rM, rS, gM, gS, bM, bS);				\
      inc        							\
    }									\
    break;								\
  case 7:								\
    for (row = 0, end = input+3*width-1; 					\
	 row < y; 							\
	 row++, input+=stride1-3*width,end+=stride1, out+=stride2-width)	\
    while(input< end) {							\
      INNER7(in, out, rM, rS, gM, gS, bM, bS);				\
      inc       							\
    }									\
    break;								\
  }

ms compute_ms(unsigned int source,unsigned int dest) {
  int shift = 0;
  unsigned int mask = 0;
  ms ret;
  /*where is the MSB for the first mask*/
  int s1 = 0;
  int s2 = 0;
  mask = source;
  while (mask & 0xfffffffe) {
    mask >>= 1; s1++;
  }
  /*where the MSB for the second mask*/
  mask = dest;
  while (mask & 0xfffffffe) {
    mask >>= 1; s2++;
  }
  /*align the bits*/
  shift = s1 - s2;
  /*produce the mask*/
  if (shift > 0)
    mask = source & (dest << shift);
  else 
    mask = source & (dest >> -shift);
  ret.shift = shift;
  ret.mask = mask;
#ifdef DEBUG
  fprintf(stderr, "Source mask: 0x%08x, Dest mask: 0x%08x\nOutput Mask: 0x%08x, shift: %d\n", source, dest, mask, shift);
#endif
  return ret;
}

void convert32_16(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  unsigned int *end;
  int row;
  int index = 0;                                                        
  register unsigned int *in = (unsigned int *) input;
  register unsigned short *out = (unsigned short *) output;
  stride2 = 2 * (stride2 - width);
  width = 4*width; stride1 = 4*stride1;
  BODY(in++;out++;, unsigned int *, unsigned short*);
}

void convert32_24(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  unsigned int *end;
  int row;
  int index = 0;                                                        
  register unsigned int *in = (unsigned int *) input;
  register unsigned int *out = (unsigned int *) output;
  stride2 = 3 * (stride2 - width);
  width = 4*width; stride1 = 4*stride1;
  BODY(in++;out = (unsigned int *) (((char *) out) + 3) ;, unsigned int*, unsigned int*);
}

void convert32_32(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  unsigned int *end;
  int row;
  int index = 0;                                                        
  register unsigned int *in = (unsigned int *) input;
  register unsigned int *out = (unsigned int *) output;
  stride2 = 4 * (stride2 - width);
  width = 4*width; stride1 = 4*stride1;
  BODY(in++;out++;, unsigned int*, unsigned int*);
}

void convert16_32(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  unsigned short *end;
  int row;
  int index = 0;                                                        
  register unsigned short *in = (unsigned short *) input;
  register unsigned int *out = (unsigned int *) output;
#ifdef DEBUG
  fprintf(stderr, "Params:\n\
input 0x%08x, output 0x%08x\n\
rm 0x%08x, gm 0x%08x, bm 0x%08x\n\
rs %d gs %d bs %d\n\
width %d heigth %d stride1 %d stride2 %d\n", 
(int) input,(int) output, rM, gM, bM, rS, gS, bS, width, y, stride1, stride2);
#endif
  stride2 = 4 * (stride2 - width);
  width = 2*width; stride1 = 2*stride1;
  BODY(in++; out++;, unsigned short*, unsigned int*);
}

void convert16_16(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  unsigned short *end;
  int row;
  int index = 0;                                                        
  register unsigned short *in = (unsigned short *) input;
  register unsigned short *out = (unsigned short *) output;
  stride2 = 2 * (stride2 - width);
  width = 2*width; stride1 = 2*stride1;
  BODY(in++; out++;, unsigned short*, unsigned short*);
}

void convert16_24(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  unsigned short *end;
  int row;
  int index = 0;                                                        
#ifdef _X86_
  register unsigned short *in = (unsigned short *) input;
  register unsigned int *out = (unsigned int *) output;
  stride2 = 3 * (stride2 - width);
  width = 2*width; stride1 = 2*stride1;
  BODY(in++; out = (unsigned int *)(((char*)out) + 3);, unsigned short*, unsigned int*);
#else
#endif
}

void convert24_16(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  int row;
  int index = 0;                                                        
#ifdef _X86_
  unsigned int *end;
  register unsigned int *in = (unsigned int *) input;
  register unsigned short *out = (unsigned short *) output;
  stride2 = 2 * (stride2 - width);
  width = 3*width; stride1 = 3*stride1;
  BODY(out++; in = (unsigned int *)(((char*)in) + 3);, unsigned int*, unsigned short*);
#else
  unsigned char *end;
  register unsigned int *in;
  register unsigned short *out = (unsigned short *) output;
  unsigned char tmp[4];
  in = (unsigned int *)tmp;
  stride1 *= 3;
  tmp[1] = *input++;
  tmp[2] = *input++;
  tmp[3] = *input++;
  BODYMOD(out++; tmp[1] = *input++; tmp[2] = *input++; tmp[3] = *input++;);
#endif
}

void convert24_24(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  int row;
  int index = 0;                                                        
#ifdef _X86_
  unsigned int *end;
  register unsigned int *in = (unsigned int *) input;
  register unsigned int *out = (unsigned int *) output;
  stride2 = 3 * (stride2 - width);
  width = 3*width; stride1 = 3*stride1;
  BODY(in = (unsigned int*)(((char*)in) + 3); out = (unsigned int *)(((char*)out) + 3);, unsigned int*, unsigned int*);
#else
  /* inefficient, on the assumption that it wil be used rarely*/
  unsigned char *end;
  register unsigned int *in;
  register unsigned int *out;
  unsigned char tmp1[4], tmp2[4];
  in = (unsigned int *)tmp1;
  out = (unsigned int *)tmp2;
  stride1 *= 3;
  
  tmp1[1] = *input++; 
  tmp1[2] = *input++; 
  tmp1[3] = *input++; 
  BODYMOD(tmp1[1] = *input++; tmp1[2] = *input++; tmp1[3] = *input++; *output++ = tmp2[1]; *output++ = tmp2[2]; *output++ = tmp2[3];);
#endif
}

void convert24_32(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2) {
  int row;
  int index = 0;                                                        
#ifdef _X86_
  unsigned int *end;
  register unsigned int *in = (unsigned int *) input;
  register unsigned int *out = (unsigned int *) output;
  stride2 = 4 * (stride2 - width);
  width = 3*width; stride1 = 3*stride1;
  BODY(out++; in = (unsigned int *)(((char*)in) + 3);, unsigned int*, unsigned int*);
#else
  unsigned char *end;
  register unsigned int *in;
  register unsigned int *out = (unsigned int *) output;
  unsigned char tmp[4];
  in = (unsigned int *)tmp;
  stride1 *= 3;
  tmp[1] = *input++;
  tmp[2] = *input++;
  tmp[3] = *input++;
  BODYMOD(out++; tmp[1] = *input++; tmp[2] = *input++; tmp[3] = *input++;);
#endif
}

