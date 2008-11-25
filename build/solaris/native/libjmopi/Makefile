#
#  @(#)Makefile	1.3 00/03/15
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libjmopi
SRCDIR = $(TOPDIR)/src/solaris
SRCREFDIR = $(TOPDIR)/src/share

#*************************************************************************
#* Force the link to include libC.so.x so the OPI libraries get
#* their references to libC resolved. Also must include more than
#* just -lopi so the other libraries get their references resolved.
#*************************************************************************

LDLIBS.so += \
	-L/usr/openwin/lib \
	-L$(JAVAHOME)/lib/sparc/green_threads \
	-R/opt/SUNWopi/lib \
	-R/opt/MMACopi/lib \
	libopi.so \
	libopisw.so \
	libopio1k.so \
	/usr/lib/libC.so.5 \
	-lthread \
	-ljmutil


LIBRARY = libjmopi

include $(TOPDIR)/build/minclude/sol_libjmopi.cmk

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
