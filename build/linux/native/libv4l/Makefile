#
# @(#)Makefile	1.1 98/10/20
#

TOPDIR = ../../../..
PLATFORM = linux
SUBDIR = native/libv4l
SRCDIR = $(TOPDIR)/src/linux

LDLIBS.so += \
	-ljmutil

LIBRARY = libjmv4l

include $(TOPDIR)/build/minclude/linux_libv4l.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
