#
# @(#)Makefile	1.8 99/08/10
# Copyright 1998 Sun Microsystems, Inc.
# 

TOPDIR = ..\..\..\..

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

TARGDIR = $(DESTDIR)/javax/media/

!include $(MINCLUDES)\javax_media.jmk

SOURCES_java = $(SOURCES_java) \
	$(TARGDIR)cdm/CaptureDeviceManager.java \
	$(TARGDIR)pim/PlugInManager.java \
	$(TARGDIR)pm/PackageManager.java

PKG1 = javax.media
PKG2 = javax.media.util
PKG3 = javax.media.format
PKG4 = javax.media.format.audio
PKG5 = javax.media.format.video
PKG6 = javax.media.control
PKG7 = javax.media.protocol
PKG9 = javax.media.renderer
PKG10= javax.media.datasink
PKG11= javax.media.rtp
PKG12= javax.media.rtp.event
PKG13= javax.media.rtp.rtcp
PKG14= javax.media.pm
PKG15= javax.media.pim
PKG16= javax.media.cdm
PKG17= javax.media.bean.playerbean

!include $(BUILDDIR)\makefiles\classes.nmk

install: $(SOURCES_class)

