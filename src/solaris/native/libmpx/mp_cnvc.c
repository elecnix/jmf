/*
 * @(#)mp_cnvc.c	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#ifdef WIN32
/** Color conversion code for WIN32 with possible MMX accel.. */


#ifdef DDRAW

#include <windows.h>
#include <ddraw.h>

static LPDIRECTDRAW         directDraw = 0;
static LPDIRECTDRAWSURFACE  screen = 0;
static HWND                 rootWindow = 0;

#endif

#include <math.h>
#include "mp_mpd.h"

uint64 UTAB[256];
uint64 VTAB[256];
uint64 LDUP[256];

char uvtablecreated = 0;

#define REDPOS   0
#define GREENPOS 8
#define BLUEPOS  16

void
updateUVTable()
{
    /* Fill up the UV lookup table */
    short r, g, b;
    double compf;
    int comp;

    for (comp = 0; comp < 256; ++comp) {
	compf = comp - 128.0;

	/* U component */
	r = (short) 0;
	b = (short) (compf * 1.772);
	g = (short) (compf * -0.34414);
	
	UTAB[comp] = 
	    (((uint64)b & 0xFFFF) <<  0) |
	    (((uint64)g & 0xFFFF) << 16) |
	    (((uint64)r & 0xFFFF) << 32);
	
	r = (short) (compf * 1.402);
	b = (short) 0;
	g = (short) (compf * -0.71414);

	VTAB[comp] = 
	    (((uint64)b & 0xFFFF) <<  0) |
	    (((uint64)g & 0xFFFF) << 16) |
	    (((uint64)r & 0xFFFF) << 32);

	LDUP[comp] =
	    (uint64) ((comp) & 0xFFFF) |
	    ((uint64)((comp) & 0xFFFF) << 16) |
	    ((uint64)((comp) & 0xFFFF) << 32);
    }
}

void
createUVTable() {
    updateUVTable();
#ifdef DDRAW
    if (screen == 0) {
	HRESULT retVal;
	DDSURFACEDESC ddsd;
	rootWindow   = GetDesktopWindow();
	retVal = DirectDrawCreate(NULL, &directDraw, NULL);
	if (retVal == DD_OK)
	    printf("Initialized DDRAW in mpx\n");
	else
	    return;
	retVal = IDirectDraw_SetCooperativeLevel(directDraw,
						 rootWindow, DDSCL_NORMAL);
	if (retVal != DD_OK)
	    return;
	ddsd.dwSize = sizeof(ddsd);
	ddsd.dwFlags = DDSD_CAPS;
	ddsd.ddsCaps.dwCaps = DDSCAPS_PRIMARYSURFACE;
	retVal = IDirectDraw_CreateSurface(directDraw,
					   &ddsd, &screen, NULL);
    }
#endif
}

void
c_yuv_to_rgb(ubyte *py, ubyte* pv, ubyte *pu, uint32 *prgb, nint xcnt, nint ycnt,
	     nint srcStride, nint dstStride) {

    uint32 *line1 = prgb;
    uint32 *line2 = line1 + dstStride;
    ubyte  *py2   = py + srcStride;
    uint32 width = xcnt * 16;
    uint32 height = ycnt * 2;
    uint32 len;
    uint32 w = width;
    ubyte sum0, sum1;
    uint16 y0, y1, y2, y3;
    
    dstStride *= 8;
    len = (width * height) / 16;
    
#define EXP 1
#ifdef EXP
    __asm {
	mov esi, pu ;
	mov edi, pv ;
	mov ecx, line1 ;
	mov edx, line2 ;
	xor ebx, ebx ;
	movd mm6, DWORD PTR py ;
	movd mm7, DWORD PTR py2 ;
    LOOP1:
	movd eax, mm6 ;
	movq mm4, QWORD PTR [eax] ;
	movd eax, mm7 ;
	movq mm5, QWORD PTR [eax] ;
	xor ebx, ebx ;
	
	; get the RGB values for U and V and add them ;  U[0] V[0] ;
	
	mov bl, BYTE PTR [esi] ;
	movq mm0, QWORD PTR UTAB[ebx * 8] ;
	mov bl, BYTE PTR [edi] ;
	paddsw mm0, QWORD PTR VTAB[ebx * 8] ;

	movd eax, mm4 ;
	movq mm1, mm0 ;

	and eax, 255 ;
	psrlq mm4, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm4 ;
	movq mm3, mm0 ;

	movq mm0, mm1 ;
	and eax, 255 ;
	psrlq mm4, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm5 ;
	packuswb mm3, mm0 ;
	movq QWORD PTR [ecx], mm3 ;

	movq mm0, mm1 ;
	and eax, 255 ;
	psrlq mm5, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm5 ;
	movq mm3, mm0 ;

	movq mm0, mm1 ;
	and eax, 255 ;
	psrlq mm5, 8 ;
	inc esi ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	packuswb mm3, mm0 ;
	inc edi ;
	movq QWORD PTR [edx], mm3 ;
	
	
	; get the RGB values for U and V and add them ;  U[1] V[1] ;
	
	mov bl, BYTE PTR [esi] ;
	movq mm0, QWORD PTR UTAB[ebx * 8] ;
	mov bl, BYTE PTR [edi] ;
	paddsw mm0, QWORD PTR VTAB[ebx * 8] ;
	movq mm1, mm0 ;

	movd eax, mm4 ;
	and eax, 255 ;
	psrlq mm4, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm4 ;
	movq mm3, mm0 ;

	movq mm0, mm1 ;
	and eax, 255 ;
	psrlq mm4, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm5 ;
	packuswb mm3, mm0 ;
	and eax, 255 ;
	movq QWORD PTR [ecx + 1*8], mm3 ;

	movq mm0, mm1 ;
	psrlq mm5, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm5 ;
	movq mm3, mm0 ;
	and eax, 255 ;

	movq mm0, mm1 ;
	psrlq mm5, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	inc esi ;
	packuswb mm3, mm0 ;
	inc edi ;
	movq QWORD PTR [edx + 1*8], mm3 ;

	
	; get the RGB values for U and V and add them ;  U[2] V[2] ;
	
	mov bl, BYTE PTR [esi] ;
	movq mm0, QWORD PTR UTAB[ebx * 8] ;
	mov bl, BYTE PTR [edi] ;
	paddsw mm0, QWORD PTR VTAB[ebx * 8] ;
	movd eax, mm4 ;
	movq mm1, mm0 ;

	and eax, 255 ;
	psrlq mm4, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm4 ;
	movq mm3, mm0 ;
	and eax, 255 ;

	movq mm0, mm1 ;
	psrlq mm4, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm5 ;
	packuswb mm3, mm0 ;
	and eax, 255 ;
	movq QWORD PTR [ecx + 2*8], mm3 ;

	movq mm0, mm1 ;
	psrlq mm5, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm5 ;
	movq mm3, mm0 ;
	and eax, 255 ;

	movq mm0, mm1 ;
	psrlq mm5, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	inc esi ;
	packuswb mm3, mm0 ;
	inc edi ;
	movq QWORD PTR [edx + 2*8], mm3 ;

	
	; get the RGB values for U and V and add them ;  U[3] V[3] ;
	
	mov bl, BYTE PTR [esi] ;
	movq mm0, QWORD PTR UTAB[ebx * 8] ;
	mov bl, BYTE PTR [edi] ;
	paddsw mm0, QWORD PTR VTAB[ebx * 8] ;
	movd eax, mm4 ;
	movq mm1, mm0 ;

	and eax, 255 ;
	psrlq mm4, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm4 ;
	movq mm3, mm0 ;
	and eax, 255 ;

	movq mm0, mm1 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm5 ;
	packuswb mm3, mm0 ;
	and eax, 255 ;
	movq QWORD PTR [ecx + 3*8], mm3 ;

	movq mm0, mm1 ;
	psrlq mm5, 8 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	movd eax, mm5 ;
	movq mm3, mm0 ;
	and eax, 255 ;

	movq mm0, mm1 ;
	paddsw mm0, QWORD PTR LDUP[eax * 8] ;
	inc esi ;
	packuswb mm3, mm0 ;
	inc edi ;
	movq QWORD PTR [edx + 3*8], mm3 ;

	
	add ecx, 8*4 ;
	add edx, 8*4 ;
	movd eax, mm6 ;
	add eax, 8 ;
	movd ebx, mm7 ;
	add ebx, 8 ;

	push esi ;
	mov esi, DWORD PTR w ;
	sub esi, 8 ;
	jnz SKIP1 ;
	mov esi, width ;
	add ecx, dstStride ;
	shl esi, 2 ;
	add edx, dstStride ;
	sub ecx, esi ;
	sub edx, esi ;
	shr esi, 2 ;
	add eax, esi ;
	add ebx, esi ;
    SKIP1:
	movd mm6, eax ;
	movd mm7, ebx ;
	mov DWORD PTR w, esi ;
	pop esi ;

	dec len ;
	jnz LOOP1 ;
	emms ;
    }

#else
    
    for (len = width * height; len > 0; len -= 16) {
#define FOUR411(n)					\
	y0 = py[n];					\
	y1 = py[n+1];					\
	y2 = py2[n];					\
	y3 = py2[n+1];					\
							\
	__asm {						\
		__asm xor ebx, ebx			\
		__asm xor edx, edx			\
		__asm mov esi, pu			\
		__asm mov bl, BYTE PTR [esi + n/2]	\
		__asm movq mm0, QWORD PTR UTAB[ebx*8]	\
		__asm mov esi, pv			\
		__asm mov bl, BYTE PTR [esi + n/2]	\
		__asm paddsw mm0, QWORD PTR VTAB[ebx*8] \
		__asm mov esi, line1			\
		__asm mov edi, line2			\
		__asm movq mm1, mm0			\
		__asm pxor mm3, mm3			\
							\
		__asm mov dx, y0			\
		__asm paddsw mm0, QWORD PTR LDUP[edx*8]	\
		__asm movq mm4, mm0			\
							\
		__asm movq mm0, mm1			\
		__asm mov dx, y1			\
		__asm paddsw mm0, QWORD PTR LDUP[edx*8]	\
		__asm packuswb mm4, mm0			\
		__asm movq QWORD PTR [esi + 4*n], mm4	\
							\
		__asm movq mm0, mm1			\
		__asm mov dx, y2			\
		__asm paddsw mm0, QWORD PTR LDUP[edx*8]	\
		__asm movq mm4, mm0			\
							\
		__asm movq mm0, mm1			\
		__asm mov dx, y3			\
		__asm paddsw mm0, QWORD PTR LDUP[edx*8]	\
		__asm packuswb mm4, mm0			\
		__asm movq QWORD PTR [edi + 4*n], mm4	\
		__asm EMMS				\
	}

/*
	ONEPIX(r, g, b, py[(n)], line1[(n)]) \
	ONEPIX(r, g, b, py[(n)+1], line1[(n)+1]) \
	ONEPIX(r, g, b, py2[(n)], line2[(n)]) \
	ONEPIX(r, g, b, py2[(n)+1], line2[(n)+1])
*/

        FOUR411(0)
	FOUR411(2)
	FOUR411(4)
        FOUR411(6)

        line1 += 8;
	line2 += 8;

	py += 8;
	py2 += 8;
	pu += 4;
	pv += 4;
	w -= 8;
	if (w <= 0) {
	    w = width;
	    line1 += w;
	    line2 += w;
	    py += w;
	    py2 += w;
	}
    }
#endif
#ifdef DDRAW
    IDirectDrawSurface_Unlock(screen, NULL);
#endif
}

#else /* SOLARIS COLOR CONVERSION STARTS HERE */

#include <math.h>
#include "mp_mpd.h"

uint32 UVTAB[65536];
uint32 SATR[512];
uint32 SATG[512];
uint32 SATB[512];

char uvtablecreated = 0;
#ifdef SOLARIS
  #define REDPOS   0
  #define GREENPOS 8
  #define BLUEPOS  16
#else
  #define REDPOS   16
  #define GREENPOS 8
  #define BLUEPOS  0
#endif

#define GETRGB(rgb, red, green, blue, rsh, bsh) { \
    red   = (rgb >> rsh) & 0xFF; \
    green = (rgb >> 8) & 0xFF; \
    blue  = (rgb >> bsh) & 0xFF; \
}

#define ONEPIX(r, g, b, y, dst) { \
    dst = SATR[r + y] |          \
	  SATG[g + y] |          \
	  SATB[b + y];           \
}


void
updateUVTable()
{
#define LIMIT(x) (((x) < -128) ? -128 : (((x) > 127) ? 127 : (x)))
    
    /* Fill up the UV lookup table */
    uint32 r, g, b;
    int u, v;
    register double uf, vf;
    for (u = 0; u < 256; ++u) {
	
	uf = (double) (u - 128);
	for (v = 0; v < 256; ++v) {
	    vf = (double) (v - 128);
	    r = LIMIT(vf * 1.402) + 128;
	    b = LIMIT(uf * 1.772) + 128;
	    g = LIMIT(uf * -0.34414 - vf * 0.71414) + 128;
	    /* Store XBGR in UVTAB table */
	    UVTAB[(u << 8)|v] =
		((r & 0xFF) <<  0) |
		((g & 0xFF) <<  8) |
		((b & 0xFF) << 16);
	}
    }
}

void
updateSaturationTable()
{
    uint32 val, val1, s;
    for (s = 0; s < 256; s++) {
	val = s;
	if (val > 255)
	    val = 255;
	SATR[s+128] = (val & 0xFF) << REDPOS;

	val = s;
	if (val > 255)
	    val = 255;
	SATG[s+128] = (val & 0xFF) << GREENPOS;

	val = s;
	if (val > 255)
	    val = 255;
	SATB[s+128] = (val & 0xFF) << BLUEPOS;
    }

    for (s = 0; s < 128; s++) {
	SATR[s] = SATR[128];
	SATG[s] = SATG[128];
	SATB[s] = SATB[128];

	SATR[s+382] = SATR[381];
	SATG[s+382] = SATG[381];
	SATB[s+382] = SATB[381];
    }    
}

void
createUVTable() {
    updateUVTable();
    updateSaturationTable();
}

void
c_yuv_to_rgb(ubyte *py, ubyte* pv, ubyte *pu, uint32 *prgb, nint xcnt, nint ycnt,
	     nint srcStride, nint dstStride, nint XBGR) {

    register uint32 *line1 = prgb;
    register uint32 *line2 = line1 + dstStride;
    register ubyte  *py2   = py + srcStride;
    uint32 width = xcnt * 16;
    uint32 height = ycnt * 2;
    register uint32 len;
    register uint32 r, g, b, sum;
    register uint32 w = width;
    register uint32 rsh = 0;
    register uint32 bsh = 16;
    
    if (!XBGR) {
	rsh = 16;
	bsh = 0;
    }
    
    for (len = width * height; len > 0; len -= 16) {
#define FOUR411(n) \
	sum = UVTAB[(pu[(n)/2] << 8) | pv[(n)/2]]; \
	GETRGB(sum, r, g, b, rsh, bsh) \
	ONEPIX(r, g, b, py[(n)], line1[(n)]) \
	ONEPIX(r, g, b, py[(n)+1], line1[(n)+1]) \
	ONEPIX(r, g, b, py2[(n)], line2[(n)]) \
	ONEPIX(r, g, b, py2[(n)+1], line2[(n)+1])

        FOUR411(0)
	FOUR411(2)
	FOUR411(4)
        FOUR411(6)

        line1 += 8;
	line2 += 8;

	py += 8;
	py2 += 8;
	pu += 4;
	pv += 4;
	w -= 8;
	if (w <= 0) {
	    w = width;
	    line1 += w;
	    line2 += w;
	    py += w;
	    py2 += w;
	}
    }
}

#endif /* ! WIN32 */

