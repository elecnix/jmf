/*
 * @(#)ln_mui.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <signal.h>
#include <poll.h>
#include "mp_mpd.h"
#include "ln_lnd.h"
extern int DEBUG;

#ifdef NOTUSED

#include <X11/Intrinsic.h>
#include <X11/StringDefs.h>
#include <X11/cursorfont.h>

#include <unistd.h>
#include <stdio.h>
#include <stropts.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <stdarg.h>
#include <stdlib.h>
#include <math.h>
#include <errno.h>

Widget wd_top, wdc_shell, wdp_shell;
Pixmap icon_pxm, apvlogo_pxm, mpxlab_pxm;


static typ_mpgbl *xmpgbl = NULL;

static struct {
   Visual *visual;
   Colormap cmap;
   nuint depth;
   } topshellinfo;
static Atom atom_wmdelwin;   
static Display *display;
static Screen  *screen;
static nint  screenno;
static nint  parentisroot;
static Window windows[4];static char *buttonnm[6] = 
   {"button_0", "button_1","button_2","button_3","button_4","button_5" };     

static nint dummy_argc = 3;
static String dummy_argv[] = {"mpx", "-display", ":0.0"};

#include "ln_xrsc.h"

static char *helptxt = 
#include "helptxt.h" 
;

static char *inside_message[] = {" " };

static char *themsg =
   "MpegExpert Version SUNW_2.00                          \n"
   "Copyright 1994,1995 APPLIED VISION,  All Rights Reserved";

#endif

void  hold_poll(typ_mpgbl *gb)
{  
   gb->delay_poll = 1; 
}

void  release_poll(typ_mpgbl *gb)
{  gb->delay_poll = 0;
   if (gb->poll_delayed)
        THREAD_KILL(gb->mpenv.mpthr, SIGPOLL);
}
   
void
scrprint(char *fmt,...)
{
   if (DEBUG) {
      va_list args;
      va_start(args, fmt);
      vfprintf(stderr, fmt, args);
      va_end(args);
   }
}

#ifdef NOTUSED

static nint
hnd_xerr(Display *display, XErrorEvent *myerr)
{  typ_mpgbl *gb = gbl_lookup(THREAD_SELF());
   mp_fprintf("MpegExpert terminating due to an X Error.\n");
   appterminate(gb->mpx_env, gb, 1);
}


void
hnd_exposedvideo(typ_mpgbl *gb, Window vidxwin)
{
    nint fbs;
    
    gb->exposed = 1;
    if (gb->mpenv.busy & (BSY_FWDVSEQ | BSY_FWD11172)) return;
    
    if ( gb->vdec.fbs > 2 || gb->dsrc.bstrm == BSTRM_ASEQ || !(gb->mpenv.state & MPS_DSRC)){
	XClearWindow(display, vidxwin);
	return;
    }
    fbs = gb->vdec.fbs;      
    ccnv_disp(gb, gb->vdec.fb[fbs][Y], gb->vdec.fb[fbs][CR],gb->vdec.fb[fbs][CB],
	      gb->vdec.bhsz, gb->vdec.bvsz, gb->vdec.vdm, gb->vdec.zoom);
}


#define  COLORBASE	13

void
fill_cmap(typ_mpgbl *gb, Colormap cmap)
{  register nint k;
   XColor colors[256];
   
   for (k=0; k < gb->vdec.nm_c8cells; k++)
   {  colors[k].pixel = COLORBASE + k;
      colors[k].red   = gb->vdec.c8_lkup[k][0] << 8;
      colors[k].green = gb->vdec.c8_lkup[k][1] << 8;
      colors[k].blue  = gb->vdec.c8_lkup[k][2] << 8;
      colors[k].flags = DoRed | DoGreen | DoBlue;
   }
   XStoreColors(display, cmap, colors, gb->vdec.nm_c8cells);
}


static void
map_vidxwin(Window xwin, nint xorg, nint yorg, nuint xsize, nuint ysize)
{  XSizeHints szhints;
   XWindowAttributes wattr;
   
   if ( ! XGetWindowAttributes(display, xwin, &wattr) ) return;
   XMoveResizeWindow(display, xwin, xorg, yorg, xsize, ysize); 
  
   if (parentisroot)
   {  szhints.x = xorg;
      szhints.y = yorg;
      szhints.min_width = szhints.max_width = szhints.width = xsize;
      szhints.min_height = szhints.max_height = szhints.height = ysize;
      szhints.flags = USPosition | USSize | PMinSize | PMaxSize;
      XSetWMNormalHints(display, xwin, &szhints);
   }
   
   if ( ! wattr.map_installed ) 
      XInstallColormap(display, wattr.colormap);

   if (wattr.map_state == IsUnmapped)
   {  XEvent event;
      XMapWindow(display, xwin);
      XFlush(display);
      do
         XNextEvent(display, &event);
      while (event.xany.type != Expose);
   }
}


void
set_video(typ_mpgbl *gb, nint vdm, nint zoom)
{  
#ifdef JAVA_VIDEO

   return;

#else /* !JAVA_VIDEO */

   Dimension  hsz, vsz, rqhsz, rqvsz;

   if ( ! gb->insignal) sighold(SIGPOLL);
   if ( gb->vdec.vdmflags & VDMF_GENERIC ) { zoom = 1; vdm = VDM_COL; }
   if ( ! (gb->vdec.vdmflags & VDMF_24) )
   {  vdm = VDM_COL; if ( zoom > 2 ) zoom = 2; }
   
   if ( (gb->vdec.vdmflags & (VDMF_24 | VDMF_CG14)) ==  VDMF_24 )
   {  if (zoom > 2)  zoom = 2; 
      if (zoom == 2) vdm = VDM_COLB;
   }
      
   gb->exposed = 1;
   
   if (vdm == VDM_COL)
      mp_initcol24(gb->vdec.adjlum, gb->vdec.adjsat, gb->vdec.adjgam);
   else if (zoom == 2)
      mp_initcol24(gb->vdec.adjlum*1.5, gb->vdec.adjsat*1.25, gb->vdec.adjgam);
   else
      mp_initcol24(gb->vdec.adjlum*1.5, gb->vdec.adjsat*1.25, gb->vdec.adjgam);
   
   rqhsz = gb->vdec.phsz * zoom + 32;
   rqvsz = gb->vdec.pvsz * zoom;
   if (gb->mpenv.uitype == 2)
   {  map_vidxwin(gb->vdec.vidxwin, gb->vdec.x, gb->vdec.y, rqhsz, rqvsz);
      gb->vdec.zoom = zoom;
      gb->vdec.vdm = vdm;
      XSync(display,0);
      hnd_exposedvideo(gb, gb->vdec.vidxwin );
      if ( ! gb->insignal) sigrelse(SIGPOLL);

      /* Report the size back to controling client. */
      send_size(gb);
      return;
   }

   gb->vdec.zoom = zoom;
   gb->vdec.vdm = vdm;
   
   XSync(display, 0);

   hnd_exposedvideo(gb, gb->vdec.vidxwin);
   
   if ( ! gb->insignal) sigrelse(SIGPOLL);   

   /* Report the size back to controling client. */
   send_size(gb);

#endif /* JAVA_VIDEO */
}



void
reset_display(typ_mpgbl *gb)
{
   gb->dinf.loc = 1.0;
   gb->dinf.hour = gb->dinf.min = gb->dinf.sec = gb->dinf.frm = 0;
   gb->dinf.mrk1on = gb->dinf.mrk2on = 0;
   gb->dinf.ptype = 0;
}


int
setup_vidxwin(typ_mpgbl *gb, Window pwin)
{  
#ifdef JAVA_VIDEO

       return 1;

#else /* !JAVA_VIDEO */

   Colormap colormap = NULL;
   XVisualInfo vinfo;
   XSetWindowAttributes attr;
   Window xrootwin, twin, cmapwin;
   XWindowAttributes pxwatr;
   XTextProperty xtxtprop;
   char *nmlist[2];

   display = XOpenDisplay(NULL);
   if (display == NULL) return 0;
   gb->mpenv.xsvfid = ConnectionNumber(display);
   if (!gb->mpenv.start_as_thread) {
      (void)XSetErrorHandler(hnd_xerr);
      (void)XSetIOErrorHandler(hnd_xerr);
   }

   xrootwin = XRootWindowOfScreen( XDefaultScreenOfDisplay(display) );
   if (pwin == 0) pwin = xrootwin;
   for (;;)
   {  nint retval = XGetWindowAttributes(display, pwin, &pxwatr);
      if ( ! retval )
      {  if (pwin == xrootwin) return 0;
         pwin = xrootwin;
      }
      else break;
   }
   cmapwin = pwin;
   xrootwin = pxwatr.root;
   screen = pxwatr.screen;
   screenno = XScreenNumberOfScreen(screen);   
   if ( xrootwin == pwin ) parentisroot = 0xfff;
   if ( gb->vdec.vdm != VDM_COL8 && 
	XMatchVisualInfo(display, screenno, 24, TrueColor, &vinfo))
      gb->vdec.vdmflags |= VDMF_24;
   else if ( XMatchVisualInfo(display, screenno, 8, PseudoColor, &vinfo))
      gb->vdec.vidxwinbg = 254;
   else
   {  mp_fprintf("mpx: Cannot allocate any visuals!\n"); return 0; }
  
   colormap = XCreateColormap(display, pwin, vinfo.visual, AllocNone);
   gb->vdec.xcmap = colormap;

   attr.border_pixel = 0;
   attr.colormap = colormap;
   attr.background_pixel = gb->vdec.vidxwinbg;
   attr.event_mask = ExposureMask | StructureNotifyMask;
   twin = XCreateWindow( display, pwin, 16, 16, 32, 32, 0, vinfo.depth,
      InputOutput, vinfo.visual,
      CWBorderPixel | CWColormap | CWBackPixel | CWEventMask, 
      &attr);

   atom_wmdelwin = XInternAtom(display, "WM_DELETE_WINDOW", FALSE);
   XSetWMProtocols(display, twin, &atom_wmdelwin, 1);
   

   if (parentisroot)
   {  cmapwin = twin;
      nmlist[0] = "MpegExpert";
      nmlist[1] = (char *)NULL;
   
      if ( XStringListToTextProperty(nmlist, 1, &xtxtprop) )
      {  XSetWMName(display, twin, &xtxtprop);
         XFree(xtxtprop.value);
      }
   }

   {   Window *cmap_wins, *rtn_cmap_wins = NULL;
       int i, j, count = 0;
       XSetWindowColormap(display, cmapwin, colormap);
       XSetWindowColormap(display, twin, colormap);

       XGetWMColormapWindows(display, cmapwin, &rtn_cmap_wins, &count);
       cmap_wins = (Window *)malloc((count + 2)*sizeof(Window));
       cmap_wins[0] = twin;
       for (i = 0, j = 1; i < count; i++) {
	  if (rtn_cmap_wins[i] != twin && rtn_cmap_wins[i] != cmapwin) {
	     cmap_wins[j] = rtn_cmap_wins[i]; j++;
	  }
       }
       cmap_wins[j] = cmapwin; j++;
       XSetWMColormapWindows(display, cmapwin, cmap_wins, j);

       free((char *)cmap_wins);
       if (rtn_cmap_wins)
          free((char *)rtn_cmap_wins);
   }
   
   gb->vdec.pxwin = pwin;
   gb->vdec.vidxwin = twin;
   gb->vdec.xdisplay = display;
   gb->vdec.xgc = XCreateGC(display, twin, 0, 0);
   gb->vdec.ximage = XCreateImage(display, vinfo.visual, vinfo.depth, ZPixmap,
   		0, (char *)gb->vdec.dispm, 352, 240, 32, 0);
   
   if ( ! (gb->vdec.vdmflags & VDMF_24) )
   {  nuint pixs[256];
      XAllocColorCells(display, colormap, 0, 0, 0, pixs, 256);

      {  uint32 pixs[16]; nint k;
         for (k=0; k<13; k++) pixs[k] = k;
         XFreeColors(display, colormap, pixs, 13, 0x0);
      }
   }
   return 1;

#endif /* JAVA_VIDEO */
}

#endif
