/*
 * @(#)New.cc	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include "New.h"

void* operator new( size_t s ) 
{ 
	return malloc(s); 
}

void operator delete( void *p ) 
{ 
	if (p)
        	free((char*)p); 
}


void _pure_error_()
{ 
	/* 

	Workaround for bug 1141822: C++ compiler has _pure_error_ 
	dependency even in cases it's not needed.

	This defines a stub function so the libC version will not
	be called.

	*/
}
