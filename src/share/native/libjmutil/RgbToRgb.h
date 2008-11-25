/*
 * @(#)RgbToRgb.h	1.4 98/03/28
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
#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  int mask;
  unsigned int shift;
} ms;

ms compute_ms(unsigned int source,unsigned int dest);

typedef void (*rgb2rgb_conversion_fun)(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);

void convert32_16(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);
void convert32_24(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);
void convert32_32(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);

void convert24_16(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);
void convert24_24(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);
void convert24_32(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);

void convert16_16(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);
void convert16_24(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);
void convert16_32(unsigned char *input, unsigned char *output, 
		  unsigned int rM, unsigned int gM, unsigned int bM,
		  int rS, int gS, int bS, 
		  int width, int y, int stride1, int stride2);

#ifdef __cplusplus
}
#endif
