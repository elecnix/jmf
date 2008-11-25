#
# @(#)Makefile	1.2 00/01/05
#

TOPDIR = ../../../..
PLATFORM = linux
SUBDIR = native/libh261
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so += \
	-ljmutil

CPLUSPLUSLIBRARY = TRUE
LIBRARY = libjmh261

include $(TOPDIR)/build/minclude/libh261.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
