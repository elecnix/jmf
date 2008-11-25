#
# @(#)Makefile	1.1 99/08/31
# Copyright 1999 Sun Microsystems, Inc.
# 

TOPDIR = ../../../..
PLATFORM = linux
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

