#
# @(#)Makefile	1.1 99/09/16
#

TOPDIR = ../../../..
PLATFORM = linux
SUBDIR = native/libxlib
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-lX11 -lXext -ljmutil

LDFLAGS.so += \
	-L/usr/X11R6/lib

LIBRARY = libjmxlib

include $(TOPDIR)/build/minclude/sol_libxlib.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
