
#ifndef _NEW_HH
#define _NEW_HH

/*
 * @(#)New.h	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <malloc.h>
#include <sys/types.h>

extern void* operator 	new( size_t s );
extern void  operator 	delete( void* p );
extern "C" { void  _pure_error_(); }

#endif /* !_NEW_HH */
