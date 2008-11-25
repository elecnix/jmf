#
#  @(#)Makefile	1.1 98/11/30
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libh261
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-ljmutil

LIBRARY = libjmh261

include $(TOPDIR)/build/minclude/libh261.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
