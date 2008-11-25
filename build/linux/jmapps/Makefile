#
# @(#)Makefile	1.1 00/02/10
# Copyright 2000 Sun Microsystems, Inc.
# 

TOPDIR = ../../..
PLATFORM = linux
SUBDIR = .
SRCDIR = $(TOPDIR)/jmapps
SRCREFDIR = $(TOPDIR)/jmapps

# include the list of java sources
include $(TOPDIR)/build/minclude/jmapps.jmk
SOURCES_java += \
	$(TARGDIR)V4LAuto.java

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
include	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

clean:

