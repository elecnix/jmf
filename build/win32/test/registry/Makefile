#
# @(#)Makefile	1.3 99/11/15
# Copyright 1998-1999 Sun Microsystems, Inc.
# 

TOPDIR = ..\..\..\..

SUBDIR = test\registry

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SRCDIR = $(TOPDIR)\test\registry

TARGDIR = $(DESTDIR)/

!include $(MINCLUDES)\test_registry.jmk

## If not ALLJAVA build
!if "$(ALLJAVA)"==""
SOURCES_java = $(SOURCES_java) \
	$(TARGDIR)VFWAuto.java
!endif

!include $(BUILDDIR)\makefiles\classes.nmk

install: $(SOURCES_class)

