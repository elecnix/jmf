#
#  @(#)Makefile	1.1 98/08/18
#

TOPDIR = ../../../..
PLATFORM = linux
SUBDIR = native/libjmutil
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-L/usr/X11R6/lib \
	-L$(JAVAHOME)/lib/i386/green_threads \


LIBRARY = libjmutil
CPLUSPLUSLIBRARY = TRUE

include $(TOPDIR)/build/minclude/libjmutil.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
