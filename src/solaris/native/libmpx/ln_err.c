/*
 * @(#)ln_err.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <unistd.h>
#include <sys/types.h>
#include <stdio.h>
#include <errno.h>
#include "mp_mpd.h"
#include "ln_lnd.h"

static char *errtext[] = {
   "Can't open data source",
   "Operational error",
   "CD error",
   "IO error",
   "Audio port error"
   };
static char *pnames[] = {"MP", "DS", "!?"};


void
err_fatal(typ_mpgbl *gb, uint16 err, ubyte errloc)
{  THREAD_ID thr_id = THREAD_SELF();
   nint pind = 2;
   
   if	    (thr_id == gb->mpenv.mpthr) pind = 0;
   else if  (thr_id == gb->mpenv.dsthr) pind = 1;
   
   gb->mpenv.errors |= err;
   mp_fprintf("Err_Fatal %s: Err:%x Loc:%d  %s\n",
   	   pnames[pind], gb->mpenv.errors, errloc, strerror(errno));
   appterminate(gb->mpx_env, gb, 4);
}

void
err_report(typ_mpgbl *gb, nint err, nint errloc)
{  THREAD_ID thr_id = THREAD_SELF();
   nint pind = 2;
   
   if	    (thr_id == gb->mpenv.mpthr) pind = 0;
   else if  (thr_id == gb->mpenv.dsthr) pind = 1;
   
   if (errno) scrprint("\n%s",strerror(errno));
   scrprint("\nErr_Report %s: Loc:%d  %s", pnames[pind], errloc, errtext[err]);
}
