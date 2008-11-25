#
# @(#)Makefile	1.1 99/09/16
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libxlib
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-ljmutil

LIBRARY = libjmxlib

include $(TOPDIR)/build/minclude/sol_libxlib.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
