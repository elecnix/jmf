#
#  @(#)Makefile	1.2 98/08/24
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libxil
SRCDIR = $(TOPDIR)/src/solaris
SRCREFDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-L/opt/SUNWits/Graphics-sw/xil/lib \
	-L/usr/openwin/lib \
	-L$(JAVAHOME)/lib/sparc/green_threads \
	-R/opt/SUNWits/Graphics-sw/xil/lib \
	-lX11 \
	-lxil \
	-ljmutil

LIBRARY = libjmxil

include $(TOPDIR)/build/minclude/sol_libxil.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
