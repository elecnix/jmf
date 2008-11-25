#
# @(#)Makefile	1.4 99/01/11
# Copyright 1998 Sun Microsystems, Inc.
# 

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = javax/media
SRCDIR = $(TOPDIR)/src/share
SRCREFDIR = $(TOPDIR)/src/solaris

# include the list of java sources
include $(TOPDIR)/build/minclude/javax_media.jmk

SOURCES_java += \
	cdm/CaptureDeviceManager.java \
	pim/PlugInManager.java \
	pm/PackageManager.java

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
include	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

clean:

