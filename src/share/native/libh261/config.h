/*
 * Copyright (c) 1995 The Regents of the University of California.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 * 	This product includes software developed by the Network Research
 * 	Group at Lawrence Berkeley National Laboratory.
 * 4. Neither the name of the University nor of the Laboratory may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * @(#)config.h	1.4 02/08/21
 */

#ifndef vic_config_h
#define vic_config_h

#ifndef SOLARIS_5_6
#if defined(sgi) || defined(__bsdi__) || defined(__FreeBSD__)
#include <sys/types.h>
#elif defined(linux)
#include <sys/bitypes.h>
#else
#ifdef ultrix
#include <sys/types.h>
#endif /* ultrix */

/*XXX*/
/*
#ifdef sco
typedef char int8_t;
#else
typedef signed char int8_t;
#endif*/	 /* sco */

typedef unsigned char u_int8_t;
/*typedef short int16_t;*/
typedef unsigned short u_int16_t;
typedef unsigned int u_int32_t;
#endif /* defined(sgi) ... */
#endif /* SOLARIS_5_6 */

#include <stdlib.h>
#ifndef WIN32
#include <unistd.h>
#endif

#ifdef WIN32
typedef unsigned int u_int;
typedef unsigned char u_char;
#endif

#endif
