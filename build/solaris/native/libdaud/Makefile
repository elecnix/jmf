#
# 00/03/30 @(#)Makefile	1.1
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libdaud
SRCDIR = $(TOPDIR)/src/solaris

LIBRARY = libjmdaud

include $(TOPDIR)/build/minclude/sol_libdaud.cmk

SUNOS_ULIBS =
SVR4_ULIBS =
I386_ULIBS =
ULIBS =

CPPFLAGS += \
	-I$(SRCDIR)/native/libdaud \
	-I$(STUBDIR)

LDLIBS.so += \
	-ljmutil

#CFLAGS = -Xa -fast -xO4 -xunroll=1 -DJAVA_SOUND -DJAVA_VIDEO

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include

