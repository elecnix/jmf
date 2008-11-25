#
# @(#)Makefile	1.1 99/07/19
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libmpx
SRCDIR = $(TOPDIR)/src/solaris

LIBRARY = libjmmpx

include $(TOPDIR)/build/minclude/sol_libmpx.cmk

SUNOS_ULIBS =
SVR4_ULIBS =
I386_ULIBS =
ULIBS =

CPPFLAGS += \
	-I$(SRCDIR)/native/libmpx \
	-I$(STUBDIR)

#CFLAGS = -Xa -fast -xO4 -xunroll=1 -DJAVA_SOUND -DJAVA_VIDEO
CFLAGS += -Xa -xunroll=1 -DJAVA_SOUND -DJAVA_VIDEO
LDLIBS.so += -lposix4 -ljmutil -lm

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.native.include

