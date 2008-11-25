#
# @(#)Makefile	1.2 00/02/27
# Copyright 2000 Sun Microsystems, Inc.
# 

TOPDIR = ../../../../../..
PLATFORM = solaris
DESTDIR = $(TOPDIR)/build/$(PLATFORM)/customlib
SUBDIR = com/sun/media/customizer
SRCDIR = $(TOPDIR)/src/share
SRCREFDIR = $(TOPDIR)/src/solaris
CLASSPATH := $(TOPDIR)/build/$(PLATFORM)/lib

# include the list of java source
include $(TOPDIR)/build/minclude/customizer.jmk

ifeq ($(ALLJAVA),)
SOURCES_java += $(SOURCES_javan)
include $(TOPDIR)/build/minclude/customizer.jmk
endif

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
include	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

clean:


