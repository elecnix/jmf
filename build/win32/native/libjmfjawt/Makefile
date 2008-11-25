#
# @(#)Makefile	1.2 02/12/09
# Copyright (c) 2002 Sun Microsystems, Inc.

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libjmfjawt
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmfjawt.dll

EXTRA_LIBS = -LIBPATH:$(JAVAHOME)\jre\lib jawt.lib

!include $(MINCLUDES)\libjmfjawt.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install
