/*
 * @(#)mp_mpd.h	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifndef _MP_MPD_H_
#define _MP_MPD_H_

#define	 ST_VSEQ  	0xb3
#define	 ST_VSEQEND    	0xb7
#define	 ST_GOP   	0xb8
#define	 ST_PICTURE	0x00
#define	 ST_EXTENSION	0xb5
#define	 ST_USERDATA	0xb2
#define	 ST_SYSHDR	0xbb
#define	 ST_SYSEND	0xb9
#define	 ST_SEQERR	0xb4
#define	 ST_RSVDB0	0xb0
#define	 ST_RSVDB1	0xb1
#define	 ST_RSVDB6	0xb6

#define	 ST_PACK	0xba
#define	 ST_AUDIO_0	0xc0
#define	 ST_VIDEO_0	0xe0
#define	 ST_PRIVSTRM1	0xbd
#define	 ST_PRIVSTRM2	0xbf
#define	 ST_PADDSTRM	0xbe
#define	 ST_RSVDSTRM	0xbc
#define	 ST_PANIC	0xb4

#define	 I_PICT	     	1
#define	 P_PICT	     	2
#define	 B_PICT	     	3
#define	 DC_PICT     	4

#define	 MB_QUANT    	0x80
#define	 MB_FWD  	0x40
#define	 MB_BWD 	0x20
#define	 MB_CBP	     	0x10
#define	 MB_INTRA    	0x08
#define	 MAXSLICE  	0xaf

#define	 FLAG_OCOL   1
#define	 FLAG_OROW   2
#define	 FLAG_INNER  4
#define	 FLAG_DC     8

#define	 RF_CLIP     1
#define	 RF_PRED     2
#define	 RF_FILT     4
#define	 RF_COLOR    8

#define	 PAST  	  0
#define	 CURRENT  1
#define	 FUTURE	  2
#define	 Y     0
#define	 CB    1
#define	 CR    2


#if !defined(MPTYPES)
#define MPTYPES

typedef  unsigned char 	ubyte;
typedef  signed char  	sbyte;
#ifdef WIN32
typedef  unsigned __int64  uint64;
typedef  __int64 int64;
#else
typedef unsigned long long uint64;
typedef long long int64;
#endif
typedef	 unsigned int	uint32;
typedef	 int   	     	int32;
typedef	 unsigned short	uint16;
typedef	 short   	int16;
typedef	 int		nint;
typedef	 unsigned int	nuint;
typedef	 unsigned short	nushort;
typedef	 short   	nshort;
#ifdef WIN32
typedef unsigned long ulong;
typedef unsigned short ushort;
typedef unsigned int u_int;
#endif
#endif

/* V i d e o   S e q u e n c e */
typedef  struct {
   int16 hsize;	     /* horizontal size in pels [1-4095] */
   int16 vsize;	     /* vertical size in pels [1-4095]	 */
   int16 aspect;     /* aspect ratio code [1-15]	 */
   int16 picrate;    /* picture rate code [1-15]	 */
   int32 bitrate;    /* bit rate: in units of 400bits/sec for constant rates
			== 0x3ffff for variable bit rate */
   int16 vbv;  	     /* minimum decoder buffer size (in 2048byte units)	*/ 
   int16 constraint; /* constraint_parameters_flag */
   int16 mbwidth;    /* # Macroblocks horizontally */
   int16 maxmb;	     /* address of the last MB in the picture */
   ubyte defaultintra, defaultinter;   /* quantizer matrix flags */
   ubyte hdr[8];     /* first 8 bytes after the start code */
   ubyte intra_qntmat[64], inter_qntmat[64]; /* quantizer matrices */
   } mp_vseq;

/* G r o u p   O f  P i c t u r e s */
typedef	 struct {
   ubyte drop;	     /* 1bit drop_frame_flag */
   ubyte hour; 	     /*	[0-23] */
   ubyte minute;     /*	[0-59] */
   ubyte second;     /*	[0-59] */
   ubyte picture;    /*	[0-59] */
   ubyte closed_gop; /* 1/0 true/false flag */
   ubyte broken_link;/* 1/0 true/false flag */
   } mp_gop;
	 
/* P i c t u r e */
typedef	 struct {
   uint16   vbv_delay;	/* in 90Khz clock units */
   uint16   tref;    	/* [0-1023] */
   ubyte    ptype;	/* [1-4], I,P,B,DC */
   ubyte    fullpel_fwd, fullpel_bwd;
   ubyte    fwd_rsize, bwd_rsize;
   mp_vseq  *vseq;   	/* pointer to  video sequence information */
   } mp_pict;


/* M a c r o b l o c k */
typedef	 struct {
   nuint scale;
   nint	 adr;
   nuint chfilt, lmfilt;
   nint	 fwd_lhmv, fwd_lvmv, fwd_chmv, fwd_cvmv,
	 bwd_lhmv, bwd_lvmv, bwd_chmv, bwd_cvmv;
   mp_pict *pict;    /* pointer to parent picture structure */
   uint32 flags;
   nint type, cbp;
   } mp_macroblk;


typedef struct {
   uint32 rate_bound;
   ubyte aud_bound;
   ubyte vid_bound;
   ubyte fixed;	     /* These 4 true/false flags are 0 or >0 */
   ubyte csps;
   ubyte aud_lock;
   ubyte vid_lock;   
} mp_syshdr;
  
/* D e c o d e r   E n v i r o n m e n t */
typedef	 struct {
   ubyte *fp[3][3];
   uint16 hsz;		   /* Horizontal size for Y decoder picture buffer */
   uint16 hsza[6];	   /* Horizontal sizes for each block's dec. pict.buf */
   int32  (*tcfb)[64];
   uint16 (*lk_mbbase)[2];
   uint16 (*lkp_levd)[6][64];
   uint16 (*lki_levd)[6][64];
   } mp_denv;

typedef void (* typ_dispf)(ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
   nint xcnt, nint ycnt, nint shsz, nint dhsz, void *sxpg);

ubyte *mp_rdvseqhdr(ubyte *d, mp_vseq *vseq);
ubyte *mp_rdgophdr(ubyte *d, mp_gop *gop);
ubyte *mp_rdpicthdr(ubyte *d, mp_pict *pict);
ubyte *mp_rdsyshdr(ubyte *d, mp_syshdr *hdr);
ubyte *mp_rendpb(ubyte *d, mp_pict *pict, mp_denv *denv, uint32 flags);
ubyte *mp_rendi(ubyte *d, mp_pict *pict, mp_denv *denv, uint32 flags);
void  skipmb(nint mbadr, nint cnt,mp_denv *denv);
void  rendmb(mp_macroblk *mb, mp_denv *denv, uint32 *patterns);
void  shidct(uint32 bl_patterns[], int32 *tcf, uint16 fbw[], ubyte *fbp[],
	     ubyte* pred, uint32 flags, uint32 nblocks);

nint  mp_initcol8(ubyte csmp[][3], float gamma_adj);
void  mp_initcol24(float adjlum, float adjsat, float adjgam);
void  mp_initcolz8();


void createUVTable();

void c_yuv_to_rgb (ubyte *plm, ubyte *pcr, ubyte *pcb,
		   uint32 *prgb, nint xcnt, nint ycnt, nint shsz, nint dhsz, nint XBGR);

#ifdef NOT_USED
void dicnv (ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *dispm,
		nint xcnt, nint ycnt, nint shsz, nint dhsz, nint oddpel);
void dicnv2(ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		    nint xcnt, nint ycnt, nint shsz, nint dhsz);
void dcnv (ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		nint xcnt, nint ycnt, nint shsz, nint dhsz);
void dcnv2b (ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		nint xcnt, nint ycnt, nint shsz, nint dhsz);
void dscnv2 (ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		   nint xcnt, nint ycnt, nint shsz, nint dhsz, void *sxpg);
void dscnv3 (ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		   nint xcnt, nint ycnt, nint shsz, nint dhsz, void *sxpg);
void dscnv3b (ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		    nint xcnt, nint ycnt, nint shsz, nint dhsz, void *sxpg);
void dscnv2b (ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		    nint xcnt, nint ycnt, nint shsz, nint dhsz, void *sxpg);
void dcnvclp(ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		   nint xcnt, nint ycnt, nint shsz, nint dhsz,
		   nint clipx0, nint clipy0, nint clipx1, nint clipy1,
		   nint blank, nint zoom);
void dicnv2(ubyte *plm, ubyte *pcr, ubyte *pcb, uint32 *prgb,
		    nint xcnt, nint ycnt, nint shsz, nint dhsz);
#endif

extern ubyte runlev0[][2], runlev1[][2], runlev2[][2];
extern ubyte runlev4[][2], runlev5[][2];
extern ubyte lk_mbadr1[], lk_mbadr2[];
extern ubyte jmp_ac9dec[], lk_chracskip[], lk_chrdcskip[];
extern ubyte dvlc_cbp[], dvlc_mbtype[], lvlc_cbp[], dvlc_mv[];
extern ubyte dzz[], one_count[]; 
extern sbyte lk_lmdc[], lk_chrdc[];
extern uint32 pattern[];
extern const float look_asp[], look_picrate[];

extern ubyte zzdefault_intraq[];

extern char uvtablecreated;
extern int MMX;

#endif /* _MP_MPD_H_ */
