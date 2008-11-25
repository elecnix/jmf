#
# @(#)Makefile	1.2 99/11/15
# Copyright 1998 Sun Microsystems, Inc.
# 

TOPDIR = ../../../../..
PLATFORM = solaris
SUBDIR = com/ibm/media
SRCDIR = $(TOPDIR)/src/share
SRCREFDIR = $(TOPDIR)/src/solaris

# include the list of java source
include $(TOPDIR)/build/minclude/com_ibm_media.jmk

ifeq ($(ALLJAVA),)
SOURCES_java += $(SOURCES_javan)
endif

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
include	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

clean:

