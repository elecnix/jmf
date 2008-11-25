#
# @(#)Makefile	1.1 02/12/06
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libjmfjawt
SRCDIR = $(TOPDIR)/src/share

LDLIBS.so = -L$(JAVAHOME)/jre/lib/sparc -ljawt

LIBRARY = libjmfjawt

include $(TOPDIR)/build/minclude/libjmfjawt.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
