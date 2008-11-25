#
#  @(#)Makefile	1.2 98/10/11
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libjpeg
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-ljmutil

LIBRARY = libjmjpeg

include $(TOPDIR)/build/minclude/libjpeg.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
