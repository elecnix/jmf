#
# @(#)Makefile	1.1 00/05/01
#

TOPDIR = ../../../..
PLATFORM = linux
SUBDIR = native/libdaud
SRCDIR = $(TOPDIR)/src/linux

LDLIBS.so += \
	-ljmutil

LIBRARY = libjmdaud

include $(TOPDIR)/build/minclude/sol_libdaud.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
