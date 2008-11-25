/*
 * @(#)ln_dmx.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include "mp_mpd.h"
#include "ln_lnd.h"

static ubyte subsnotice[12] = {0,0,1,ST_RSVDB1,1,2,3,4,5,6,7,8};
   
/* ISO_11172 Demultiplexer.
   Source data is in circular buffer "dmxb" which is filled by the Dserver
   process. There are no variables accessed by both processes, thus data
   integrity is guaranteed. dmxb.rp is maintained by this function, and
   dmxb.ready is a count of source data bytes acquired by this process
   with semaphore handshake from the Dserver. Dserver may have more
   data available in dmxb than claimed and added to dmxb.ready by this
   process.
*/
void
dmx_11172(typ_mpgbl *gb, int32 aneed, int32 vneed, ubyte action[])
{  register ubyte *d, *q;
   register int32 r0, ready, spent, position, need;
   register uint32 blksz, acquired;
   nint semid, done, error, nowait;
   typ_circb *dmxb = &(gb->dmxb);
   typ_circb *vidb = &(gb->vidb);
   typ_circb *audb = &(gb->audb);
   
   blksz = gb->dsenv.blksz;	/* Make a local copy	*/
   d  = dmxb->rp;	/* active read pointer	*/
   ready = dmxb->ready;	/* #bytes known to be available in the i/o buffer  */
   acquired = dmxb->tacq;/* #bytes acquired from the Dserver since the beginning
			   of the synchronous read cycle */ 
   semid = gb->mpenv.smph;	/* make a local copy... */
   /* Establish a modula "blksz" count of bytes consumed in the current
      block of the i/o buffer
   */
   spent = d - dmxb->b1;	   
   /* Since we at most transfer 512bytes to the sequentiality region, as long as
      blksz > 512, which is normally so, we can't be more than 512 bytes behind
      dmxb->b1
   */
   spent = (spent + blksz) % blksz;
 
   done = 0;
   do
   {  position = dmxb->b2 - d;  /* Distance from readp to end of the i/o buffer */
      need = 6;
      /* Do we have enough #bytes for a start code and length field? 4+2=6 */
      if (ready >5)
      {	 /* Again, we need to have at least 6 consecutive bytes before
	    falling off the end of the i/o buffer.
	 */
	 if (position < 6)
	 {  q = d;
	    d = dmxb->b1 - position;
	    while (position)
	    {  position--;
	       d[position] = q[position];
	    };
	    position = dmxb->b2 - d;
	 }
	 /* Are we at the beginning of a start code? */
	 if ((d[0]<<16 | d[1]<<8 | d[2]) == 1)
	    r0 = action[d[3]];
	 else
	    r0 = DMX_RESYNC;   /* No, need to resynchronize */
	 
	 switch (r0 & 0xf) {
	 case DMX_PACK:
	    need = 12;
	    /* Need to secure more bytes from the DServer ? */
	    if (ready < need) break;   
	    d += 12;
	    position -= 12;
	    if (position <= 0)
	       d = dmxb->b1 - position;
	    spent += 12;
	    ready -= 12;
	    need = 6;
	    break;
	 case DMX_SYSHDR:
	 {  nint k,n;
	    need = 6 + (d[4]<<8) + d[5];
	    if (ready < need) break;
	    n = (need > 128) ? 128 : need;   /* Just in case... */
	    for (k=0; k < n; k++) gb->capt.shdr1[k] = d[k];
	    d += need;
	    spent += need;
	    ready -= need;
	    need = 6;
	    break;
	 }	
	 /* An iso11172 entity with lenght field, to be skipped */
	 case DMX_SKIP:
	    need = 6 + (d[4]<<8) + d[5];
	    if (ready < need) break;
	    d += need;
	    position -= need;
	    if (position <= 0)
	       d = dmxb->b1 - position;
	    spent += need;
	    ready -= need;
	    need = 6;
	    break;
	 case DMX_AV:	/* A subscribed Audio or Video stream */
	 {  register int32 transfer;
	    typ_circb *strm;
	    typ_ptslog *stamps;
	    
	    if (r0 & DMXFL_VIDEO)
	    {  strm = vidb;
	       stamps = &gb->vdec.stamps;
	       if (r0 & DMXFL_SUBSCRIBE)
	       {  nint k;
		  ubyte vstrm;
		  
		  vstrm = d[3] & 0xf;
		  gb->mpenv.vstrm = vstrm;
		  for (k=0;k<12;k++) vidb->b1[k] = subsnotice[k];
		  vidb->b1[11] = vstrm;
		  vidb->ready += 12;
		  vidb->wp += 12;
		  
		  for (k=0;k<16;k++)
		     action[0xe0 | k] = DMX_SKIP;
		  action[d[3]] = DMX_AV | DMXFL_VIDEO;
	       }
	    }
	    else
	    {  strm = audb;
	       stamps = &gb->adec.stamps;
	       if (r0 & DMXFL_SUBSCRIBE)
	       {  nint k;
		  ubyte astrm;
		  
		  astrm = d[3] & 0x1f;
		  /* If /dev/audio is off, we won't reassign the strm. */
		  if (!(gb->mpenv.state & MPS_AUDOFF))
		     gb->mpenv.astrm = astrm;
		  for (k=0;k<32;k++)
		     action[0xc0 | k] = DMX_SKIP;
		  action[d[3]] = DMX_AV;
	       }
	    }
	    /* Here we are testing if we would exceed the fullness level 
	       of the target elementary stream buffer by delivering the
	       data in this packet. But note that the test here is only a
	       very close approximation, since possible padding bytes and
	       presentation and decoding time stamp information will actually
	       not be delivered.
	    */
	    need = (d[4]<<8) + d[5];
	    if ( need + strm->ready > strm->full)
	    {  done = 1;
	       need = 0;
	       break;
	    }
	    
	    need += 6;
	    if (ready < need) break;   /* Make sure required #bytes are available */
	    
	    /* Here we are testing if required #bytes are consecutively available
	       before reaching the end of the i/o buffer. If not, then delivery to
	       the elementary stream buffer should be done in two parts. Second part
	       starting from the beginning of the i/o buffer (a circular buffer).
	       But since we have to have as much as 26 bytes (upto 16 padding +
	       10 bytes for the presentation & decoding time stamps) consecutively
	       anyway, because our decoding logic expects to have them available
	       that way, and also to avoid the unnecessary overhead of "memcpy"
	       function call in case of a very short first part, we choose to 
	       copy the first part of the packet till the end of the i/o buffer
	       to the b0-b1 i/o buffer area, if first part is < 512 bytes.
	       After this operation the transfer operation is done with one shot
	       unless the recipient buffer state necessitates a two part operation.
	    */	       
	    if (need > position)
	    {  if (position < 512)
	       {  q = d;
		  d = dmxb->b1 - position;
		  memcpy(d,q,position);
		  position = dmxb->b2 - d;
	       }
	    }	    	    
	    q = d;
	    d+=6;
	    
	    /* Note that there is no possiblity of a private_stream_2 here,
	       thus the following decoding logic works.
	    */
	    while (*d == 255) d++;  /* skip stuffing */
	    
	    if ((*d & 0xc0) == 0x40) d += 2;
	    
	    r0 = *d & 0xf0;
	    if (r0 == 0x20 || r0 == 0x30)
	    {  uint32 pos, pcktsz, pckfpos;
	       ubyte *p, b0;
	       
	       p = d;
	       d += 5;
	       if (r0 == 0x30) d += 5;	       
	       pcktsz = need - (d-q);
	       pos = strm->b2 - strm->wp;
	       pckfpos = acquired - ready;
	       
	       r0 = stamps->wp;
	       b0 = p[0];
	       if ( stamps->flags & DMXF_SYSEND )
	       {  b0 &= 0xf;
		  stamps->flags &= 0xff ^ DMXF_SYSEND;
	       }
	       stamps->a[r0].pts[0] = b0;
	       stamps->a[r0].pts[1] = p[1];
	       stamps->a[r0].pts[2] = p[2];
	       stamps->a[r0].pts[3] = p[3];
	       stamps->a[r0].pts[4] = p[4];	       
	       stamps->a[r0].seq = stamps->seq;
	       stamps->a[r0].size = pcktsz;
	       stamps->a[r0].pos = pos;
	       stamps->a[r0].fpos = pckfpos;
	       
	       if (pos < pcktsz)
	       {  r0 = (r0+1) & 0x7f;   /* Modula 128, #entries in the array */		  
		  stamps->filled += 2;
		  stamps->a[r0].pts[0] = b0;
		  stamps->a[r0].pts[1] = p[1];
		  stamps->a[r0].pts[2] = p[2];
		  stamps->a[r0].pts[3] = p[3];
		  stamps->a[r0].pts[4] = p[4];		  
		  stamps->a[r0].seq = (stamps->seq + 1) & 0xff;
		  stamps->a[r0].size = pcktsz;
		  stamps->a[r0].pos = (strm->b2 - strm->b1) + pos;
		  stamps->a[r0].fpos = pckfpos;
	       }
	       else 
		  stamps->filled += 1;	       
	       stamps->wp = (r0+1) & 0x7f;	       
	    }   
	    else d += 1;
	    
	    /* Actual #bytes in this packet to be transferred to 
	       the elem_stream buffer
	    */
	    transfer = need - (d-q);   
	    if (transfer < 0)
	    {  /* Something is wrong with packet !! */
	       d = q+1;
	       ready -= 1;
	       spent += 1;
	       need = 0;
	       break;
	    }
	    
	    /* Rest of the operation is deterministic, since we have established
	       the fact that the source data and destination buffer space is
	       available. So we update those below now...
	    */
	    ready -= need;
	    spent += need;
	    strm->ready += transfer;
	    	    
	    /* We have different data transfer situations below depending
	       on the sequentiality of source and destination data space 
	    */ 
	    position -= d-q;
	    r0 = strm->b2 - strm->wp;
	    if (r0 <= transfer) stamps->seq = (stamps->seq + 1) & 0xff;
	    
	    if (position >= transfer)
	    {  if (r0 > transfer)
	       {  memcpy(strm->wp,d,transfer);
		  strm->wp += transfer;
	       }
	       else
	       {  memcpy(strm->wp,d,r0);
		  memcpy(strm->b1,d+r0,transfer-r0);
		  strm->wp = strm->b1 + transfer-r0;		  
	       }
	    }
	    else
	    {  transfer -= position;
	       if (r0 > position)
	       {  memcpy(strm->wp,d,position);
		  strm->wp += position;
	       }
	       else
	       {  memcpy(strm->wp,d,r0);
		  memcpy(strm->b1,d+r0,position-r0);
		  strm->wp = strm->b1 + position-r0;		  
	       }
	       
	       d = dmxb->b1;
	       r0 = strm->b2 - strm->wp;
	       if (r0 > transfer)
	       {  memcpy(strm->wp,d,transfer);
		  strm->wp += transfer;
	       }
	       else
	       {  memcpy(strm->wp,d,r0);
		  memcpy(strm->b1,d+r0,transfer-r0);
		  strm->wp = strm->b1 + transfer-r0;
	       }
	    }
	    d += transfer;
	    need = 6;
	    break;	    
	 }
	 case DMX_PANIC:
	 case DMX_RESYNC:	    
	    q = d;   /* Record current position */
	    
	    /* Search for a start code: 0x000001xx */
	    do do 
		  while (*d++);
	       while (*d);
	    while (d[1] != 1);
	    
	    d -= 1;  /* Go back to the first byte of the start code */
	    
	    if (d >= dmxb->b2)
	    {  /* We have searched beyond the end of the i/o buffer and
		  have encountered the ST_PANIC, the panic start code! in the
		  panic zone which is dmxb->b2..dmxb->b3 .
		  Definition:  Legal_zone is made up of the bytes currently
		  known to be available.
	       */
	       if (position >= ready)
	       {  /* We have searched beyond the legal_zone, we can't 
		     continue to search before backing up and acquiring 
		     more bytes from the data server.
		     We recognize the possibility that the end of the 
		     legal_zone might be in the middle of a possible 
		     start code. 
		  */
		  ready -= 3; /* ready was known to be >=6 ... */
		  spent += ready;
		  d = q + ready;
		  ready = 3;		  
	       }
	       else  
	       {  /* We have been searching in the legal region, but we fell
		     of the end of the circular i/o buffer into the panic_zone
		     We need to continue our search from the
		     beginning of the circular buffer. But first we have to
		     eliminate the possibility of not having recognized 
		     a start code due to its being cut in the middle by the
		     end of the i/o buffer (dmxb->b2).
		  */  
		  d = dmxb->b1 - 3;
		  q = dmxb->b2 - 3;
		  d[0] = q[0];
		  d[1] = q[1];
		  d[2] = q[2];
		  position -= 3;
		  ready -= position;
		  spent += position;		  
	       }
	    }
	    else
	    {  /* We did encounter a start code before the panic_zone,
		  but don't know if in the legal_zone or not */
	       r0 = d-q;
	       if (ready < r0+4)    /* r0+4 = travelled space + start code */
		  r0 = ready - 3;   /* consume only the data in legal_zone */
	       else if (action[d[3]] == DMX_RESYNC)
		  r0 += 4;	    /* will ignore this start code */
		  
	           
	       spent += r0;
	       ready -= r0;
	       d = q + r0;	       
	    }	    
	    need = 6;
	    break;
	 case DMX_SYSEND:
	    gb->adec.stamps.flags |= DMXF_SYSEND;
	    gb->vdec.stamps.flags |= DMXF_SYSEND;
	    d += 4;
	    spent += 4;
	    ready -= 4;
	    need = 6;
	    break;	 
	 }
      }
	       
      /* Never return without first releasing the consumed blocks, otherwise
	 the "spent" count will mess up...
      */
      while (spent >= blksz)
      {  while ((error = SEMA_POST(gb->mpx_env, &gb->dssync.decr)) != 0)
	    if (error == EINTR) continue;
	    else ;
	 spent -= blksz;
      }
      
      if (need > ready)
      {	 if ( dmxb->dsrcend )
	 {  vidb->dsrcend = TRUE;
	    audb->dsrcend = TRUE;
	    break;
	 }	 
	 
	 if (aneed <= audb->ready || vneed <= vidb->ready)
	    nowait = TRUE;
	 else nowait = FALSE;    /* will wait */
	 
	 for (;;)
	 {  /* Avoid unnecessary or hopeless blocking waits */
	    if (need <= ready || gb->dsenv.syncrd_state == DSRCEND) 
	       nowait = TRUE;
	    if ((nowait && (error = SEMA_TRYWAIT(gb->mpx_env, &gb->dssync.incr)) != 0) ||
		(!nowait && (error = SEMA_WAIT(gb->mpx_env, &gb->dssync.incr)) != 0))
	    {  /* If we were in a non blocking semaphore wait here, then 
		  returning with this errno code would mean that we otherwise
		  had to wait, and we did not, just like the way we wanted,
		  we will break away from this loop, demux whatever we
		  have, if we have anything, otherwise we will getout.
		  
		  If we were in a blocking semop, then normally we should not
		  have received this errno code, but acting the same way
		  will not do any harm
	       */

	       if ( error == EBUSY ) break;
	       
	       /* If returned as a result of an interrupt, this is probably 
		  of user interface origin, so we will return and give a chance
		  for a check up. We recently found out that sometimes
		  unexplainably semop returns -1 with errno of 0, we will
		  treat this case the same as EINTR.
	       */
	       else if (error == EINTR)
	       {  done = 1; break; }
	       else ;
	    }
	    ready += blksz;
	    acquired += blksz;
	 }
	 if (gb->dsenv.syncrd_state == DSRCEND)
	 {
	    if (SEMA_PEEK(gb->mpx_env, &gb->dssync.incr) == 0)
	    {  int32 junk = blksz - gb->dsenv.lastfill;
	       ready -= junk;
	       acquired -= junk;
	       dmxb->dsrcend = TRUE;
	    }
	 }	 
      }      
   } while ( !done &&  need<=ready);
   dmxb->rp = d;
   dmxb->ready = ready;
   dmxb->tacq = acquired;
}


void
dmx_mpegcd(typ_mpgbl * gb, int32 aneed, int32 vneed, ubyte action[])
{  register ubyte *d, *q, *qq, r0;
   register int32 spent;
   register int32 transfer, k, done;
   register uint32 blksz, acquired;
   typ_circb *strm;
   typ_ptslog *stamps;
   register int32 ready;
   nint error, nowait;
   typ_circb *dmxb = &(gb->dmxb);
   typ_circb *vidb = &(gb->vidb);
   typ_circb *audb = &(gb->audb);

   blksz = gb->dsenv.blksz;	/* Make a local copy	*/
   d  = dmxb->rp;	/* active read pointer	*/
   acquired = dmxb->tacq;
   ready = dmxb->ready;	/* #bytes known to be available in the i/o buffer  */
   
   /* Establish a modula "blksz" count of bytes consumed in the current
      block of the i/o buffer
   */
   spent = d - dmxb->b1;	   
   spent = (spent + blksz) % blksz;
   
   done = 0;
   do
   {  if (ready < SECTXA)
      {	 if ( dmxb->dsrcend )
	 {  vidb->dsrcend = TRUE;
	    audb->dsrcend = TRUE;
	    break;
	 }
	 if (aneed <= audb->ready || vneed <= vidb->ready)
	    nowait = TRUE;
	 else nowait = FALSE; 
	 
	 for (;;)
	 {  if (gb->dsenv.syncrd_state == DSRCEND) nowait = TRUE; 
	    if ((nowait && (error = SEMA_TRYWAIT(gb->mpx_env, &gb->dssync.incr)) != 0) ||
		(!nowait && (error = SEMA_WAIT(gb->mpx_env, &gb->dssync.incr)) != 0))
	    {  if (error == EBUSY) break;
	       else if (error == EINTR)
	       {  done = 1;  break; }
	       else ;
	    }
	    ready += blksz;
	    acquired += blksz;
	    nowait = TRUE; 	/* Since blksz > SECTXA */	 
	 }
	 if (gb->dsenv.syncrd_state == DSRCEND)
	 {
	    if (SEMA_PEEK(gb->mpx_env, &gb->dssync.incr) == 0)
	    {  int32 junk = blksz - gb->dsenv.lastfill;
	       ready -= junk;
	       acquired -= junk;
	       dmxb->dsrcend = TRUE;
	    }
	 }	 
	 if (ready < SECTXA || done) break;
      }
      
      qq = d;
      r0 = d[19 - SECTXAOFS];
      
      if ( r0 == 0x7f )
      {	 /* MPEG Audio sector */
	 strm = audb;
	 stamps = &gb->adec.stamps;
      }
      else if (r0==0x0f || r0==0x1f || r0==0x3f)
      {	 /* MPEG Video sector: Motion_Pict, Still norm_res, Still high_res */
	 strm = vidb;
	 stamps = &gb->vdec.stamps;
      }       
      else goto skipped_sector;
      
	      
      if (strm->ready + 2306 > strm->full)  done = 1;
      else
      {  d += 24 - SECTXAOFS;
	 while ((d - qq < (SECTXA-7)) && (d[0]<<16 | d[1]<<8 | d[2])==1)
	 {  r0 = action[d[3]];	    
	    switch (r0 & 0xf) {
	    case DMX_PACK:
	       d += 12;
	       break;
	    case DMX_SYSEND:
	       d+=4;
	       break;
	    case DMX_AV:
	       if (r0 & DMXFL_VIDEO)
	       {  if (r0 & DMXFL_SUBSCRIBE)
		  {  ubyte vstrm = d[3] & 0xf;
		     gb->mpenv.vstrm = vstrm;
		     for (k=0;k<12;k++) vidb->b1[k] = subsnotice[k];
		     vidb->b1[11] = vstrm;
		     vidb->ready += 12;
		     vidb->wp += 12;
		     
		     for (k=0;k<16;k++)  action[0xe0 | k] = DMX_SKIP;
		     action[d[3]] = DMX_AV | DMXFL_VIDEO;
		  }
	       }
	       else
	       {  if (r0 & DMXFL_SUBSCRIBE)
		  {  ubyte astrm = d[3] & 0x1f;
		     /* If /dev/audio is off, we won't reassign the strm. */
		     if (!(gb->mpenv.state & MPS_AUDOFF))
		     	gb->mpenv.astrm = astrm;
		     for (k=0;k<32;k++)  action[0xc0 | k] = DMX_SKIP;
		     action[d[3]] = DMX_AV;
		  }
	       }	       
	       transfer = (d[4]<<8) + d[5];
	       d += 6;
	       q = d;
	       while (*d == 255) d++;   /* skip stuffing */
	       if ((*d & 0xc0) == 0x40) 
		  d += 2;
	       r0 = *d & 0xf0;
	       if (r0 == 0x20 || r0 == 0x30)
	       {  uint32 pos, pcktsz, pckfpos;
		  ubyte *p;
		  
		  p = d;
		  d += 5;
		  if (r0 == 0x30) d += 5;
		  
		  pcktsz = transfer - (d-q);
		  pos = strm->b2 - strm->wp;
		  pckfpos = acquired - ready + (d-qq);
		  		  
		  k = stamps->wp;
		  stamps->a[k].pts[0] = p[0];
		  stamps->a[k].pts[1] = p[1];
		  stamps->a[k].pts[2] = p[2];
		  stamps->a[k].pts[3] = p[3];
		  stamps->a[k].pts[4] = p[4];		  
		  stamps->a[k].seq = stamps->seq;
		  stamps->a[k].size = pcktsz;
		  stamps->a[k].pos = pos;
		  stamps->a[k].fpos = pckfpos;
		  
		  if (pos < pcktsz)
		  {  k = (k+1) & 0x7f;   /* Modula 128, #entries in the array */		     
		     stamps->filled += 2;
		     stamps->a[k].pts[0] = p[0];
		     stamps->a[k].pts[1] = p[1];
		     stamps->a[k].pts[2] = p[2];
		     stamps->a[k].pts[3] = p[3];
		     stamps->a[k].pts[4] = p[4];		     
		     stamps->a[k].seq = (stamps->seq + 1) & 0xff;
		     stamps->a[k].size = pcktsz;
		     stamps->a[k].pos = (strm->b2 - strm->b1) + pos;
		     stamps->a[k].fpos = pckfpos;
		  }
		  else 
		     stamps->filled += 1;		  
		  stamps->wp = (k+1) & 0x7f;		  
	       }
	       else d += 1;
	       
	       transfer -= (d-q); 
	       strm->ready += transfer;
	           
	       k = strm->b2 - strm->wp;
	       if (k > transfer)
	       {  memcpy(strm->wp,d,transfer);
		  strm->wp += transfer;	  
	       }
	       else
	       {  memcpy(strm->wp,d,k);
		  memcpy(strm->b1,d+k,transfer-k);
		  strm->wp = strm->b1 + transfer-k;
		  stamps->seq = (stamps->seq + 1) & 0xff;
	       }
	       
	       d += transfer;
	       break;
	    case DMX_SYSHDR:
	    {  ubyte *q;
	       nint n = 6 + (d[4]<<8) + d[5];
	       
	       if (n > 128) n = 128;		      /* Just in case */
	       if (r0 & DMXFL_VIDEO) q = gb->capt.shdr1; /* Video Stream Syshdr */
	       else q = gb->capt.shdr2;		      /* Audio Stream Syshdr */	       
	       for (k = 0; k < n; k++) q[k] = d[k];
	    }  /* Falls down... */   
	    case DMX_SKIP:
	       d += 6 + (d[4]<<8) + d[5];
	       break;
	    default:
	       d += 1;
	    }
	 }
      }

skipped_sector:
      if (! done)
      {  ready -= SECTXA;
	 spent += SECTXA;
	 d = qq + SECTXA;
	 if (d >= dmxb->b2) d = dmxb->b1;
      }      
      while (spent >= blksz)	 /* Release consumed blocks */
      {  spent -= blksz;
	 while ((error = SEMA_POST(gb->mpx_env, &gb->dssync.decr)) != 0)
	    if (error != EINTR) break;
      }
      
   } while (!done);

   dmxb->rp = d;
   dmxb->ready = ready;
   dmxb->tacq = acquired;
}


void
set_dmxaction(ubyte *act, ubyte astrm, ubyte vstrm)
{  register int k;
  
   for (k=0;k<256;k++) act[k] = DMX_RESYNC;
   act[ST_PACK] = DMX_PACK;
   act[ST_SYSHDR] = DMX_SYSHDR;
   act[ST_SYSEND] = DMX_SYSEND;
   act[ST_PRIVSTRM1] = DMX_SKIP;
   act[ST_PRIVSTRM2] = DMX_SKIP;
   act[ST_PADDSTRM] = DMX_SKIP;
   act[ST_RSVDSTRM] = DMX_SKIP;
   
   /* Skip all reserved data streams */
   for (k=0;k<16;k++) act[0xf0 | k] = DMX_SKIP;
   
   if (vstrm & STRM_SBCOFF)
      for (k=0;k<16;k++) act[0xe0 | k] = DMX_SKIP;   
   else if (vstrm & STRM_AUTOSBC)
      /* Declare all video streams as being eligible to be subscribed.
	 Subscribe to the first one encountered, drop the DMXFL_SUBSCRIBE
	 flag from it, and declare all other video streams as to be
	 ignored -skipped-
      */
      for (k=0;k<16;k++)
	 act[0xe0 | k] = DMX_AV | DMXFL_SUBSCRIBE | DMXFL_VIDEO;
   else
   {  for (k=0;k<16;k++) act[0xe0 | k] = DMX_SKIP;
      vstrm &= STRM_IDBITS;
      if (vstrm < 16) act[0xe0 | vstrm] = DMX_AV | DMXFL_VIDEO;
   }
   
   if (astrm & STRM_SBCOFF)
      for (k=0;k<32;k++) act[0xc0 | k] = DMX_SKIP;   
   else if (astrm & STRM_AUTOSBC)
      for (k=0;k<32;k++)
	 act[0xc0 | k] = DMX_AV | DMXFL_SUBSCRIBE; 
   else
   {  for (k=0;k<32;k++) act[0xc0 | k] = DMX_SKIP;
      astrm &= STRM_IDBITS;
      if (astrm < 32) act[0xc0 | astrm] = DMX_AV;
   }
}


