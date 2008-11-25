#
# @(#)Makefile	1.1 99/08/31
#

TOPDIR = ../../../..
PLATFORM = linux
SUBDIR = native/libjpeg
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-ljmutil

LIBRARY = libjmjpeg

include $(TOPDIR)/build/minclude/libjpeg.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
