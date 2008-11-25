#
# @(#)Makefile	1.1 00/10/25
# Copyright 2000 Sun Microsystems, Inc.
# 

TOPDIR = ..\..\..

!include $(TOPDIR)\build\win32\makefiles\defs.nmk
DESTDIR = $(TOPDIR)\build\win32\customlib
TARGDIR = $(DESTDIR)/com/sun/media/customizer/

!include $(MINCLUDES)\customizer.jmk

!include $(BUILDDIR)\makefiles\classes.nmk

install: FORCE
	$(HIDE)$(MAKE) -nologo compile ACTION=compile

compile: $(SOURCES_class)

FORCE:

clean:
	$(RECDEL) $(DESTDIR)\com\sun\media\customizer

