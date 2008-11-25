#
# @(#)Makefile	1.1 00/02/10
# Copyright 2000 Sun Microsystems, Inc.
# 

TOPDIR = ../../..
PLATFORM = solaris
SUBDIR = .
SRCDIR = $(TOPDIR)/jmapps
SRCREFDIR = $(TOPDIR)/jmapps

# include the list of java sources
include $(TOPDIR)/build/minclude/jmapps.jmk
SOURCES_java += \
	$(TARGDIR)SunVideoAuto.java \
	$(TARGDIR)SunVideoManual.java \
	$(TARGDIR)SunVideoPlusAuto.java \
	$(TARGDIR)SunVideoPlusManual.java

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
include	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

clean:

