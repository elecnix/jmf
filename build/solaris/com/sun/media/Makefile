#
# @(#)Makefile	1.4 99/11/15
# Copyright 1998 Sun Microsystems, Inc.
# 

TOPDIR = ../../../../..
PLATFORM = solaris
SUBDIR = com/sun/media
SRCDIR = $(TOPDIR)/src/share
SRCREFDIR = $(TOPDIR)/src/solaris

# include the list of java source
include $(TOPDIR)/build/minclude/com_sun_media.jmk

ifeq ($(ALLJAVA),)
SOURCES_java += $(SOURCES_javan)
include $(TOPDIR)/build/minclude/sol_com_sun_media.jmk
endif

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
include	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

clean:

