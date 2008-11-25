#
# @(#)Makefile	1.4 00/03/27
# Copyright 1999-2000 Sun Microsystems, Inc.
# 

TOPDIR = ../../../../..
PLATFORM = linux
SUBDIR = com/sun/media
SRCDIR = $(TOPDIR)/src/share
SRCREFDIR = $(TOPDIR)/src/solaris
SRCREFDIR2 = $(TOPDIR)/src/linux

# include the list of java source
include $(TOPDIR)/build/minclude/com_sun_media.jmk

ifeq ($(ALLJAVA),)
SOURCES_java += $(SOURCES_javan)
include $(TOPDIR)/build/minclude/linux_com_sun_media.jmk
endif

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
include	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

clean:

