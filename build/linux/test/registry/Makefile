#
# @(#)Makefile	1.1 99/08/31
# Copyright 1998-1999 Sun Microsystems, Inc.
# 

TOPDIR = ../../../..
PLATFORM = linux
SUBDIR = .
SRCDIR = $(TOPDIR)/test/registry
SRCREFDIR = $(TOPDIR)/test/registry

# include the list of java sources
include $(TOPDIR)/build/minclude/test_registry.jmk
SOURCES_java += \
	$(TARGDIR)SunVideoAuto.java \
	$(TARGDIR)SunVideoManual.java \
	$(TARGDIR)SunVideoPlusAuto.java \
	$(TARGDIR)SunVideoPlusManual.java

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
include	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

clean:

