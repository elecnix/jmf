#
# @(#)Makefile	1.4 00/08/30
# Copyright 2000 Sun Microsystems, Inc.
# 

TOPDIR = ..\..\..

SUBDIR = jmapps

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SRCDIR = $(TOPDIR)\jmapps

TARGDIR = $(DESTDIR)/

!include $(MINCLUDES)\jmapps.jmk
## If not ALLJAVA build
!if "$(ALLJAVA)"==""
SOURCES_java = $(SOURCES_java) \
	$(TARGDIR)VFWAuto.java \
	$(TARGDIR)VFWManual.java \
	$(TARGDIR)DirectSoundAuto.java
!endif

!include $(BUILDDIR)\makefiles\classes.nmk

install: $(SOURCES_class)

