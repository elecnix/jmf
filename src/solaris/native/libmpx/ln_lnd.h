/*
 * @(#)ln_lnd.h	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef _LN_LND_H_
#define _LN_LND_H_

#include <jni.h>

#ifdef THREADSTUFF
#include <threads.h>	/* green threads */
#endif

#if !defined(MPTYPES)
#define MPTYPES
typedef  unsigned char 	ubyte;
typedef  signed char  	sbyte;
typedef	 unsigned int	uint32;
typedef	 int   	     	int32;
typedef	 unsigned short	uint16;
typedef	 short   	int16;
typedef	 int		nint;
typedef	 unsigned int	nuint;
typedef	 unsigned short	nushort;
typedef	 short   	nshort;
#endif	/* MPTYPES */

#if !defined(ulong)
typedef unsigned long ulong;
typedef unsigned short ushort;
typedef unsigned int u_int;
#endif

#define	 TRUE  1
#define	 FALSE 0
#define  MAX_MPX_INSTANCE	512

/* Max #Macroblocks in a picture in a constraint parameter video stream. 
   Initial memory allocation for picture decoding and color conversion frame 
   buffers will be done based on this value.
*/
#define	 DFL_NMB	396
#define	 MAX_NMB	(1024*1024>>8)
#define	 SECTXAOFS	12
#define	 SECTXA		2340 
#define	 SECTDATA	2048

/* Initial size of memory to be allocated for operational buffers.
   This value is based on the following reasoning:
   
   I/O fifo for the Dserver	    : 256 blocks
   Elementary Stream Decoders fifos : 384 blocks
      CD-I FMV rate = 2324*75 = 174300 bytes/sec
      384*1024 > 2 * 174300
      So this gives about 2 seconds of I/O cushion
*/      
#define	 DFL_OPERBUFSZ	1024   /* In 1024 byte blocks */

#define  MIN_LUM	0.0
#define  MAX_LUM	3.0
#define  MIN_SAT	0.0
#define  MAX_SAT	3.0
#define  MIN_GAM	0.3
#define  MAX_GAM	3.0


/* ERROR CODES  */
#define	 ERR_CORELIMSET	(1<<0)
#define	 ERR_GETMSG	(1<<1)
#define	 ERR_GETSEM	(1<<2)
#define	 ERR_SETSIG	(1<<3)
#define	 ERR_DSSETSIG	(1<<4)
#define	 ERR_STARTDS    (1<<5)
#define	 ERR_SETEXITSIG (1<<6)
#define	 ERR_STARTUI    (1<<7)
#define	 ERR_SEMINIT	(1<<8)
#define	 ERR_ITIMER	(1<<9)
#define	 ERR_IPC	(1<<10)
#define	 ERR_MALLOC	(1<<11)
#define  ERR_STARTCS	(1<<12)

#define	 RERR_OPENSRC	   0
#define	 RERR_OPERATIONAL  1
#define	 RERR_CDOPER	   2
#define	 RERR_IO	   3
#define	 RERR_AUDIOPORT	   4

/* Return a pointer to the next 4K page boundry after the given address */
#define	 ALIGN_PAGE(adr)   (((ubyte *)(adr)+4095)-(((uint32)(adr)+4095) & 0xfff))


/* Convert a floating pt to 32-bit integer for used by the cmd structure.
   Only a maximum of 2^16 (65536) can be represented and precision
   is lost during the conversion. */
#define MULF	65536.0
#define FL_TO_INT(n)	(uint32)((n) * MULF)
#define INT_TO_FL(n)	((float)(n) / MULF)

/* M P E G   S E R V E R   C O M M A N D S 
   A command word is a 32 bit unsigned value.   
   31				     0
   vvvvvvvv.xxxxxxxx.xxxxxxxx.cccccccc
   cccccccc: major command group id
   vvvvvvvv: version id of the command
*/
#define	 MCMD_NULL	0
#define	 MCMD_EXIT	1
#define	 MCMD_OPENSRC	2
#define	 MCMD_CLOSESRC	3
#define	 MCMD_REENTER	4
#define	 MCMD_PLAYCTR	5
#define	 MCMD_PRESCTR	6
#define	 MCMD_STREAM	7
#define	 MCMD_SENDSTAT	8
#define	 MCMD_STATUS	9
#define	 MCMD_ACK	10
#define	 MCMD_SETRSRC	11
#define	 MCMD_CAPTURE	12
#define	 MCMD_CDOP	13
#define	 MCMD_TEST	0xff
#define  MCMD_QSIZE	30
#define  MCMD_QSTATS	31

/* Command Flags */
#define	 MCFL_SNDACK	(1<<0)
#define	 MCFL_NOACK	(1<<1)
#define	 MCFL_ORGMPX	(1<<2)
#define	 MCFL_MPXRSV1	(1<<16)

/*	 MCMD_OPENSRC	type	*/
#define	 MSC_NONE	0
#define	 MSC_FNAME	1
#define	 MSC_CDFILE	2
#define	 MSC_NETWORK	3
#define	 MSC_FDSCP	4
#define	 MSC_JAVASTRM	5

/*	 MCMD_REENTER	flags	*/
#define	 MRE_FOFS	(1<<0)
#define	 MRE_RELOFS	(1<<1)
#define	 MRE_ASOPEN	(1<<2)
#define	 MRE_STRMS	(1<<3)
#define	 MRE_SEEKVSEQ	(1<<4)

/*	 MCMD_PLAYCTR	action	*/
#define	 PC_PLAY	(1<<0)	   
#define	 PC_FWDSPEED	(1<<1)
#define	 PC_FWDSTEP	(1<<2)
#define	 PC_PAUSE	(1<<3)
#define	 PC_AUDMSK	(PC_PLAY | PC_PAUSE | PC_FWDSTEP)


/* 	 MCMD_STREAM	stream
   vvvvvvvv.aaaaaaaa
   aaaaaaaa:
	 a7: ignore stream identifier part, bits a5-a0
	 a6: audio stream subscription 0/ON, 1/OFF
	 a5: 1/subscribe to first encountered audio stream, a4-a0 = 00000
      a4-a0: subscribe to particular audio stream [0-31]

   vvvvvvvv:
	 v7: ignore stream identifier part, bits v5-v0
	 v6: video stream subscription 0/ON, 1/OFF
	 v5: subscribe to first encountered video stream, v4-v0 = 00000
	 v4: 0
      v3-v0: subscribe to particular video stream [0-15]
	 
*/
#define	 STRM_IGNOREID	0x80
#define	 STRM_SBCOFF	0x40
#define	 STRM_AUTOSBC	0x20
#define	 STRM_IDBITS	0x3f

/*	 MCMD_PRESCTR   which */
#define	 PCTR_VMD	(1<<0)
#define	 PCTR_AMD	(1<<1)
#define	 PCTR_AVOL	(1<<2)
#define	 PCTR_LUM	(1<<3)
#define	 PCTR_SAT	(1<<4)
#define	 PCTR_GAM	(1<<5)
#define  PCTR_LSG	(PCTR_LUM | PCTR_SAT | PCTR_GAM)

/*	 MCMD_PRESCTR	vmd	Video Display Mode */
#define	 VDM_NONE	0
#define	 VDM_COL	1  
#define	 VDM_COLB	2
#define	 VDM_COL8	3

/*	MCMD_PRESCTR	amd	Audio Play Mode
	cccqqq
	
   ccc: channel listening selection
	Sxx : 1/0 -> Selection/ No Selection
	101 : Left
	110 : Right
	111 : Left & Right
   qqq: audio playback quality selection
	Sxx : 1/0 -> Selection/ No Selection
	100 : High
	101 : Medium
	110 : Low
*/


/*	 MCMD_CAPTURE	action	*/
#define	 CPS_MRK1    (1<<0)
#define	 CPS_MRK2    (1<<1)
#define	 CPS_STOP    (1<<2)
#define	 CPS_DOIT    (1<<3)


/*	 MCMD_CDOP	action	*/
#define	 CDS_OPENDEV	   0
#define	 CDS_CLOSEDEV	   1
#define	 CDS_OPENVLM	   2
#define	 CDS_CLOSEVLM	   3
#define	 CDS_EJECT	   4


/* Video Display Flags */
#define	 VDMF_24	(1<<0)	/* 24bit TrueColor display */
#define	 VDMF_DGA	(1<<1)	/* DGA on */
#define	 VDMF_CG14	(1<<2)
#define	 VDMF_CG6	(1<<3)
#define	 VDMF_TCX	(1<<4)
#define	 VDMF_LEO	(1<<5)
#define	 VDMF_GENERIC	(1<<6)


/* DATA SOURCE IDENTIFIERS */
#define	 DSRC_NONE	0
#define	 DSRC_REGULAR	(1<<0)
#define	 DSRC_PIPE	(1<<1)
#define	 DSRC_FWDONLY	(1<<2)
#define	 DSRC_DEVICE	(1<<3)
#define	 DSRC_CD	(1<<4)
#define	 DSRC_SOCKET	(1<<5)
#define	 DSRC_STREAMS	(1<<6)


/* BITSTREAM DATA TYPES */
#define	 BSTRM_11172	(1<<0)
#define	 BSTRM_VSEQ	(1<<1)
#define	 BSTRM_ASEQ	(1<<2)


/* BUSY STATUS */
#define	 BSY_IDLE	   (1<<0)
#define	 BSY_FWDASEQ	   (1<<1)
#define	 BSY_FWDVSEQ	   (1<<2)
#define	 BSY_FWD11172	   (1<<3)
#define	 BSY_BWDASEQ	   (1<<4)
#define	 BSY_BWDVSEQ	   (1<<5)
#define	 BSY_BWD11172	   (1<<6)
#define	 BSY_CAPTURE	   (1<<7)


/* DATA SERVER RELATED CONSTANTS */
#define	 DSCMD_SENSE	   0  /* Sense status */
#define	 DSCMD_DIE	   1
#define	 DSCMD_SYNCREAD	   2  /* Enter into synchronus read mode */
#define	 DSCMDFLAG_SETSEM  1  /* Set sync semaphore after completion of command */
#define	 DS_INTR     SIGUSR1  /* Signal for interrupting Data server */


/* DATA SERVER STATES */
#define	 DSS_IDLE	   0	    /* Idle, sleeping on receiving a cmd message */ 
#define	 DSS_EXECCMD	   (1<<0)   /* Executing a command */ 


/* MPEG SERVER STATES */
#define	 MPS_UIACTIVE	(1<<0)
#define	 MPS_DSRC	(1<<1)
#define	 MPS_DSYNC	(1<<2)
#define	 MPS_AUDOFF	(1<<3)

/* DEMULTIPLEXER ACTION CODES */
#define	 DMX_PACK	   0
#define	 DMX_AV		   1
#define	 DMX_SKIP	   2
#define	 DMX_SYSHDR	   3
#define	 DMX_SYSEND	   4
#define	 DMX_RESYNC	   5
#define	 DMX_PANIC	   6

#define	 DMXFL_VIDEO	   0x80
#define	 DMXFL_SUBSCRIBE   0x40


/* VIDEO DECODER ENGINE FLAGS */
#define	 VD_VSEQHDR	(1<<0)
#define	 VD_USEVBV	(1<<1)
#define	 VD_LOOKVSEQ	(1<<2)
#define	 VD_SAFEMARKED	(1<<3)
#define	 VD_STEP	(1<<4)
#define	 VD_SLOW	(1<<5)
#define	 VD_SYSTM	(1<<6)		
#define	 VD_DISPLAYED	(1<<7)
#define	 VD_BROKEN	(1<<8)


/* 11172 DECODER ENGINE FLAGS */
#define	 AVD_AUDON	(1<<0)
#define	 AVD_VIDON	(1<<1)   
#define	 AVD_ASYNC	(1<<2)
#define	 AVD_SYSTM	(1<<3)
#define	 AVD_VSKIP	(1<<4)
#define	 AVD_VPTS	(1<<5)
#define	 AVD_APTS	(1<<6)
#define	 AVD_AUDEMPTY	(1<<7)
#define	 AVD_AUDNEED	(1<<8)
#define	 AVD_AUDNODATA	(1<<9)
#define	 AVD_INPICT	(1<<10)


/* AUDIO DECODER ENGINE FLAGS   */
#define	 AD_CHGFREQ	   (1<<0)
#define	 AD_CHGCHANNEL	   (1<<1)
#define	 AD_CHGAUDM1	   (1<<2)
#define	 AD_CHGAUDM2	   (1<<3)
#define	 AD_INITIALENTRY   (1<<4)
#define	 AD_SYSEND	   (1<<5)
#define	 AD_SYSEND2	   (1<<6)

/* Our audio frame parsers decide after parsing the deterministic initial
   part of the audio frames, size of the audio data in a frame. Below is
   the maximum size of this deterministic area for both layers:
   For Layer_1 :  (32bands * 2channels) * (4bit allocation + 6bit scalefactor)
		  = 64 * 10 = 640 bits = 80 bytes
		  
   For Layer_2 :  (30bands * 2ch) * (4allocation + 2scfsi + 3*6scalefactor)
		  = 60 * 24 = 1440 bits = 180 bytes
		  rounding upto 32 bands gives 192
		  
   Including the maximum 6 bytes of header, 192+6 = 198bytes will guarantee
   safe first level parsing, including the frame header.
*/

#define	 SAFE_AUDIO  198

/* Flags to define display fields to be updated */
#define	 UPD_BAR  (1<<0)
#define	 UPD_VTM  (1<<1)
#define	 UPD_MRKS (1<<2)
#define	 UPD_VSTR (1<<3)
#define	 UPD_ASTR (1<<4)
#define	 UPD_ALL  0xff

/* Used for triggering display updates in playback engines */
#define	 DPY_VSEQ (1<<0)
#define	 DPY_ASEQ (1<<1)
#define	 DPY_SLOW (1<<2)
#define	 DPY_DISP (1<<7)      /* Don't change this */

/* CD_ROM player types */
#define	 CD_TOSHIBA  (1<<0)
#define	 CD_CHINON   (1<<1)
#define	 CD_PLEXTOR  (1<<2)
#define	 CD_SONY     (1<<3)
#define	 CD_GENERIC  (1<<4)   /* Everything else, treated as Sony */
#define	 CD_SUPPORT  (CD_TOSHIBA | CD_CHINON | CD_PLEXTOR | CD_SONY)

/* Decoder application behaviour options */
#define	 OPT_VALID   (1<<4)
#define	 OPT_IGNVBV  (1<<5)
#define	 OPT_DEBUG1  (1<<6)


typedef struct {
   jobject jobj;
   int key;
} semaq_t;

#ifdef THREADSTUFF
typedef TID		THREAD_ID;
#endif

typedef semaq_t		SEMA;

typedef struct {
   int needYUVBuffer;
   jobject jarray;
   uint32 *buf;
   int outWidth;
   int outHeight;
   int XBGR;
} jimage_t;


#define USYNC_THREAD	0

typedef struct {
   jbyteArray joperb;
   ubyte *operb;  /* Operational buffers base */
   ubyte *pictb;  /* Picture buffers base */
   int smph;	  /* Semaphore group id */
   int msgq;	  /* Dserver message queue id */
#ifdef THREADSTUFF
   pid_t mppid;	  /* MPEG player process id */
   THREAD_ID mpthr; /* MPEG player (main body) thread id */
   THREAD_ID dsthr; /* DServer thread id */
   THREAD_ID csthr; /* CmdServer thread id */
#endif
   uint16 state;  /* MP state */
   uint16 options;/* Decoder and application behaviour options */
   uint16 uitype; /* User interface type */
   uint16 start_as_thread; /* This is running as a thread from another app. */
   nint xsvfid, cntfid;	/* Xserver and Control-Channel STREAMS file descriptors */
   ubyte bstrm;	  /* Bitstream data type (Expected) */
   ubyte astrm;
   ubyte vstrm;
   uint16 busy;	  /* The busy routine the application is in */
   nint cdfid;	  /* mounted CD device file id  */
   nint dbfid;	  /* proc process file id */
   nint cdscsi;	  /* SCSI id of the CD_ROM player */
   uint16 errors; /* Operational error log */
} typ_mpenv;


#define	 DMXF_SYSEND	(1<<0)
#define	 DMXF_NEWSTRM	(1<<1)

typedef struct {
   uint16 rp;
   uint16 wp;
   uint16 filled;
   ubyte flags;
   ubyte seq;
   struct {
      uint32 pos;
      uint32 fpos;
      uint16 size;
      ubyte pts[5];
      ubyte seq;
   } a[128];
} typ_ptslog;


typedef struct {
   uint16 state;
   mp_vseq vseq;
} typ_vstrminfo;


typedef struct {
   uint32 gopval;    /* Last GOP header before the marked picture	*/
   uint32 gopmrk;    /* Location of the GOP header in the data source	*/
   uint32 entry;     /* A known location behind the mark point in the data source */
   uint32 hdr;	     /*	Marked audio frame header or picture header	*/
} typ_mrkinfo;


typedef struct {
   typ_mrkinfo mrk1, mrk2;
   ubyte vstrm;
   ubyte astrm;
   mp_vseq vseq;
   ubyte shdr1[128]; /* A maximum of 40 streams supported...*/
   ubyte shdr2[128];
} typ_capt;


typedef struct {
   /* Refer to proc_vseqhdr() for the explanations of the below */   
   float  maxbitppix;
   float  avrbitppix;
   float  sqbitppix;
   
   float  maxnrmdel;	/* maximum normalization delay in seconds */
   uint32 vbsz;		/* Current video buffer size */
   uint32 vbsqsz;	/* Current sequentiality area size */
   uint16 maxpict;	/* Maximum picture area which the currently allocated
			   buffer space can handle. In # macroblocks, e.g for
			   352x288 -> maxtpict = 396 */
   
   uint16 phsz, pvsz;	/* On screen video window dimensions   */
   uint16 bhsz, bvsz;	/* Vseq dimensions rounded upto nearest x16  */
   
   nshort x,y;		/* Upper left origin of display window */
   nshort nm_c8cells;	/* #color cells used in the color index mode */
   nshort coltabsz;	/* Size of the color lookup table in X pseudo color visual */	
   nshort vdm;		/* Video display mode */
   uint32 vdmflags;
   nshort zoom;
   uint16 flags;
   nuint  strm;		/* Current video stream no [0..15] */
   
   float adjgam, adjlum, adjsat;
    /*   Display *xdisplay;*/
    /*   XImage  *ximage;*/
    /*   GC xgc;*/
    /*   Colormap xcmap;*/
    /*   Window vidxwin;	/* Video window X is */
    /*   Window pxwin;  	/* Parent X Window id */
    /*   uint32 vidxwinbg;*/

   float    speed;
   uint16   state;
   nint	    toggle;
   nint	    phigh,ptref,pdisp;
   nint	    pfut, refstat;
   nint	    lastplaytype;
   ubyte    *safemark;
   uint32   gopmrk, gopval,entry;
   float    vpts;
   float    td,tbr;
   ubyte    fbs;

   uint32 hdr1, hdr2;
   uint32 vbfullness;	/* Used if Vbv_delay field is not (can't be) used */
   float  vbvmult;	/* Multiplies the Vbv_delay field */
   void  *dispm;	/* Display memory (Origin for XputImage) */
   mp_denv denv;	/* Mpeg video decoder environment structure */
   ubyte *fb[3][3];	/* YCrCb buffers, past/current/future */
   typ_mrkinfo mrk[3];
   ubyte c8_lkup[256][3];
   
   typ_ptslog stamps;	      /* Video time stamps */
   typ_vstrminfo strms[16];   /* To be able to track 16 video streams simultaneously */
} typ_vdec;


typedef struct {
#ifdef JAVA_SOUND
   jobject jdev;
   jobject joperb;
   ubyte *buf;
#else
   nint adev;		/* Audio device file id */
#endif /* JAVA_SOUND */
   ubyte quality;	/* Audio play quality */
   ubyte listen;	/* Audio Channel selections */
   uint32 entry;
   uint32 frmhdr;
   float  apts;
   int16 *audbuf; /* Decoded audio samples buffer */
   float ahead;	  /* Seconds of audio samples to be maintained in hardware buffer */
   float (*vv)[52*32];	   /* Synthesis subband filter working buffer pointer */
   float (*smp)[36][32];   /* Decoded subband samples buffer pointer */  
   typ_ptslog stamps;
} typ_adec;


typedef struct {
   ubyte *b0,*b1,*b2,*b3;
   ubyte *rp;
   ubyte *wp;
   int32 ready;
   int32 full;
   uint32 tacq;
   nint  dsrcend;
} typ_circb;


typedef struct {
   float loc;
   float lmrk1;
   float lmrk2;
   ubyte mrk1on, mrk2on;
   ubyte hour, min, sec, frm;
   ubyte ptype;
   nshort h,w;
   char vstr[64];
   char astr[64];
   ulong ifrms, bfrms, pfrms;
   ulong rdbytes;
} typ_dinf;


typedef struct{
   ubyte cmd;
   ubyte cmdflags;
   union {
      int32 cmd0; /* Inquire Status */      
      struct {
	 int fid;
	 typ_circb *sfp;
      } cmd1;  /* Start sync_fwd_read, Regular file */      
      struct {
	 uint32 blkadr;	/* CD logical block number to start reading */
	 uint32 nblk;	/* Number of blocks to read */
	 typ_circb *sfp;
      } cmd2;  /* Start sync_fwd_read, CD */
   }m;
} typ_dsmsg;


#define DSRCEND	     1
#define DSMSG_SIZE   (sizeof(typ_dsmsg) - sizeof(long))
#define DSMSG_QSIZE  16
#define DSMSG_NEXT(i)    (((i)+1) & 0xf) 

typedef struct {
   SEMA incr, decr;
   nint rp, wp;
   typ_dsmsg msgq[DSMSG_QSIZE];
} typ_dsmsgq;


typedef struct {
   SEMA incr, decr;
   SEMA cmdsync;
} typ_dssync;


typedef struct{
   uint16 state;
   ubyte cmd;	  /* Current/last executed command */
   nint rval;	  /* Return value from the last command */
   
   /* These below are for Synchronus read operation */
   uint32   blksz;	   /* Size of each block to be transferred */
   uint32   lastfill;	   /* # valid bytes in the last block transferred */
   ushort   syncrd_state;  /* State & flags */
   ushort   nblk;	   /* #blocks which can fit into the circular buffer */
} typ_dsenv;


#define	NET_FA	(1<<0)
#define	NET_LA	(1<<1)
#define	NET_FU	(1<<2)
#define	NET_LU	(1<<3)
#define	NET_FI	(1<<4)
#define	NET_LI	(1<<5)
#define	NET_FP	(1<<6)
#define	NET_LP	(1<<7)
#define	NET_BAD	(1<<8)


typedef struct {
   nint	   flags;
   nshort  saf;		/* socket address family, [AF_UNIX , AF_INET]   */
   nshort  stype;	/* SOCK_DGRAM or SOCK_STREAM	*/
   uint32  fi, li;	/* foreign/local ip address	*/
   nushort fp, lp;	/* foreign/local port		*/
   nint    ttl;		/* ttl */
   /* foreign/local address path names for AF_UNIX, max 108 chars */
   char fu[128], lu[128];
} typ_netadr;


typedef struct {
   nint type;		/* Source specification, file name, fdscp or network */
   nint fdscp;
   ubyte fromadr[32];
   nint fromlen;
   typ_netadr soc;
   char fpath[512];	/* path/name of the source STREAMS file */
}typ_cntch;


typedef struct {
   uint32 lbn;
   uint32 size;
   uint16 attr;
   ubyte xreclen;
   ubyte funit;
   ubyte igap;
   ubyte fno;
} typ_cdfile;


typedef struct {
   ubyte bstrm;
   ubyte type;
   nint  fid;
   uint32 size;
   uint32 ofs;
   uint32 cdadr;     /* Lbn start addr. of the data area of the CD file */
   uint32 cdfsz;     /* Size in sectors of CD file data */
   uint16 cdsect;
   union {
      typ_cdfile cdf;
      typ_netadr soc;
      nint fdscp;
      char fpath[1024];
   } s, nxt;
   jobject jstrm;
} typ_datasrc;


typedef struct {
   uint32 channel;
   uint32 seq;
   uint32 flags;
   uint32 cmd;
   union {
      uint32 action;
      struct {
	 uint32 ofs;
	 float  fofs;
	 uint32 data;
	 uint32 strms;
	 uint32 flags;
	 } reenter;
      struct {
	 uint32 ofs;
	 float  fofs;
	 uint32 data;
	 uint32 strms;
	 uint32 flags;
	 uint32 type;
	 uint32 fdscp;
	 void *src;
	 } opensrc;
      struct {
	 uint32 which;
	 uint32 vmd;
	 uint32 amd;
	 float avol;
	 float lum;
	 float sat;
	 float gam;
	 } presctr;
      struct {
	 uint32 action;
	 float speed;
	 } playctr;
   } u;
} typ_cntcmd;


#define  CMD_QSIZE	   	16
#define  CMD_NEXT(i)	   	(((i)+1) & 0xf)
#define  CMD_AVAIL(env, q)      (SEMA_TRYWAIT((env), &((q).incr)) == 0)
#define  CMD_BUF_AVAIL(env, q)  (SEMA_TRYWAIT((env), &((q).decr)) == 0)
#define  CMD_CHECK(env, q)	   (SEMA_PEEK((env), &((q).incr)) > 0) /* just to check on the q
				 without committing to doing anything */

typedef struct {
   SEMA incr, decr;
   nint rp, wp;
   typ_cntcmd cmds[CMD_QSIZE];
} typ_cmdq;


#define NVDM	6
typedef struct {
   char *vmodenm;
   nshort vmodeval;
} typ_vmodes;


/* Globals state */
/* One of these per instantiation of the decoder */
typedef struct {
   typ_mpenv mpenv;
   typ_vdec vdec;
   typ_adec adec;
    /*   typ_dgainf dgainf;*/
   typ_capt capt;
   typ_circb vidb, audb, dmxb;
   typ_datasrc dsrc;
   typ_cntch cntch;
   typ_cmdq cmdq;		/* control command queue */
   typ_dsmsgq dsmsgq;		/* dserver message queue */
   typ_dssync dssync;		/* dserver synch controller */
   typ_dinf dinf;		/* display info */
   typ_netadr netadr;
   uint16 lk_mbbase[MAX_NMB][2];
   uint16 lkp_levd[32][6][64];
   uint16 lki_levd[32][6][64];
   ubyte dmx_actions[256];
   nint cdtype;			/* TOSHIBA, CHINON, SONY, TEXEL etc */
   nint prevvdm, prevorgx;	/* ln_disp.c */
   volatile typ_dsenv dsenv;
   volatile uint16 cdsectmode;	/* last used sector mode, (2048,2340) */
   volatile nint insignal;	/* ln_mui.c */
   volatile nint exposed;	/* ln_disp.c */
   volatile nint ds_stopped;
   volatile nint cdlock_a, cdlock_b;
   volatile nint delay_poll, poll_delayed;
   jobject jmpx;
   jobject mpx_thread;
   jobject ds_thread;
   JNIEnv *mpx_env;
   JNIEnv *ds_env;
   SEMA mpx_deadsync;
   SEMA ds_deadsync;
   nint exit;
   jobject  component;
   int  window;
   int  windowSet;
   void *mpxCntl;
   void *blitter;
   void *converter;
} typ_mpgbl;


void mpx_main(typ_mpgbl *, int argc, char *argv[]);
void *mpx_thread(void *);
typ_mpgbl *init_mpx(JNIEnv *);

nint cmd_avail(JNIEnv *, typ_cmdq *);
nint cmd_wait(JNIEnv *, typ_cmdq *);
nint cmd_buf_avail(JNIEnv *, typ_cmdq *);
nint cmd_buf_wait(JNIEnv *, typ_cmdq *);
void cmd_add(JNIEnv *, typ_cmdq *, typ_cntcmd *);
void cmd_remove(JNIEnv *, typ_cmdq *);
void cmd_retain(JNIEnv *, typ_cmdq *);

void *mpxThread(void *);
void *dserver(void *);
void idle_dserver(JNIEnv *, typ_mpgbl *);
void kill_dserver(JNIEnv *, typ_mpgbl *);
nint dsmsgsnd(JNIEnv *, typ_mpgbl *, typ_dsmsg *);
nint dsmsgrcv(JNIEnv *, typ_mpgbl *, typ_dsmsg *);
void dsmsgpurge(JNIEnv *, typ_mpgbl *);
nint opensrc(JNIEnv *, typ_mpgbl *, nint srctype, void *src);

nint opencdf(typ_mpgbl *, typ_cdfile *cdf);
void cd_opendev(typ_mpgbl *);
void cd_closedev(typ_mpgbl *);
void cd_openvlm(typ_mpgbl *);
void cd_closevlm(typ_mpgbl *);
void create_cdtool(typ_mpgbl *);

/*void hnd_xevent(int sig);*/
/*nint setup_vidxwin(typ_mpgbl *, Window pwin);*/
/*void set_video(typ_mpgbl *, nint vdm, nint zoom);*/
/*void reset_display(typ_mpgbl *);*/
/*void fill_cmap(typ_mpgbl *, Colormap);*/
void scrprint(char *fmt,...);
void hold_poll(typ_mpgbl *);
void release_poll(typ_mpgbl *);

void apvnap(int32 milliseconds);
nint set_audio(typ_mpgbl *, nuint smpfreq, nint quality, nint listen);
void fwd_aseq(typ_mpgbl *, nint playtype);
void wraud(typ_mpgbl *, int16 *audbuf, int32 nsmp);
void drainaud(typ_mpgbl *, nint nowait);
nint filledaud(typ_mpgbl *, long long acntout);
nint test_auddev(typ_mpgbl *);
void set_audvol(typ_mpgbl *, float fvol);

ubyte *rdpicthdr(ubyte *d, uint32 phdr, nint ptype, mp_pict *pict);
nint proc_vseqhdr(typ_mpgbl *, mp_vseq *nv, mp_vseq *ov, nint fresh);
void fwd_vseq(typ_mpgbl *, nint playtype);

void fwd_11172(typ_mpgbl *, nint playtype);
void reset_stamps(typ_ptslog *stamps);

void set_dmxaction(ubyte *act, ubyte astrm, ubyte vstrm);
void dmx_11172(typ_mpgbl *, int32 aneed, int32 vneed, ubyte action[]);
void dmx_mpegcd(typ_mpgbl *, int32 aneed, int32 vneed, ubyte action[]);

/*void err_fatal(typ_mpgbl *, uint16 err, ubyte errloc);*/
/*void err_report(typ_mpgbl *, nint err, nint errloc);*/

void appterminate(JNIEnv *, typ_mpgbl *, ubyte exitcode);
nint process_command(JNIEnv *, typ_mpgbl *, uint16 busy);
nint init_audio(JNIEnv *, typ_mpgbl *);
void close_audio(JNIEnv *, typ_mpgbl *);
nint verify_scsid(nint scsid);
void app_exit(typ_mpgbl *, int);

void proc_cmd(JNIEnv *, void *gb, u_int *, int);
void proc_cmd_broadcast(JNIEnv *, u_int *, int);

nint start_capt(typ_mpgbl *);
nint make_captcmd(typ_mpgbl *);
void create_capttool(typ_mpgbl *);

void blankitsx(nint xsize, nint ysize, nint zoom);
void ccnv_disp(typ_mpgbl *, ubyte *plm, ubyte *pcr, ubyte *pcb, 
		nint xsize, nint ysize, nint vdm, nint zoom);
nint init_video_policy(typ_mpgbl *);

void send_ack(typ_mpgbl *, typ_cntcmd *);
void send_size(typ_mpgbl *);
void send_stats(typ_mpgbl *, float);
void mp_fprintf(char *, ...);
void mp_printf(char *, ...);

int mpx_seek(typ_mpgbl *, int, int, int);

extern int errno;

#define EINTR 4
#define EBUSY 16

extern int SEMA_INIT(JNIEnv *, SEMA *, unsigned int, int, void *);
extern int SEMA_RESET(JNIEnv *, SEMA *, unsigned int);
extern int SEMA_DESTROY(JNIEnv *, SEMA *);
extern int SEMA_WAIT(JNIEnv *, SEMA *);
extern int SEMA_TRYWAIT(JNIEnv *, SEMA *);
extern int SEMA_POST(JNIEnv *, SEMA *);
extern int SEMA_PEEK(JNIEnv *, SEMA *);

#endif /* _LN_LND_H_ */
