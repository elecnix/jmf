#
# @(#)Makefile	1.2 02/12/06
# Copyright (c) 2002 Sun Microsystems, Inc.

TOPDIR = ../../../..
PLATFORM = linux
SUBDIR = native/libjmfjawt
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so = -L$(JAVAHOME)/jre/lib/i386 -ljawt

LIBRARY = libjmfjawt

include $(TOPDIR)/build/minclude/libjmfjawt.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
