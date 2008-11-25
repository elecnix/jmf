#
#  @(#)Makefile	1.2 00/03/17
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libjmutil
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-L/opt/SUNWits/Graphics-sw/xil/lib \
	-L/usr/openwin/lib \
	-L$(JAVAHOME)/lib/sparc/green_threads \
	-R/opt/SUNWits/Graphics-sw/xil/lib \
	-lX11 \
	-lm

LIBRARY = libjmutil

include $(TOPDIR)/build/minclude/libjmutil.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
