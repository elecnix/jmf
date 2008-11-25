#
# @(#)Makefile	1.11 99/11/15
# Copyright 1998 Sun Microsystems, Inc.
# 

TOPDIR = ..\..\..\..\..

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

TARGDIR = $(DESTDIR)/com/sun/media/

!include $(MINCLUDES)\com_sun_media.jmk


!if "$(ALLJAVA)"==""
!include $(MINCLUDES)\win32_com_sun_media.jmk
SOURCES_java = $(SOURCES_java) $(SOURCES_javan)
!endif

!include $(BUILDDIR)\makefiles\classes.nmk

!if "$(ACTION)"=="compile"
!include java.rules
!endif

install: java.rules FORCE
	$(HIDE)$(MAKE) -nologo compile ACTION=compile

compile: $(SOURCES_class)

java.rules: $(MINCLUDES)\com_sun_media.jmk $(MINCLUDES)\win32_com_sun_media.jmk
	$(ECHO) ---- Generating java.rules
	$(HIDE)java_rules_gen $(SOURCES_java) >java.rules

FORCE:

clean:
	$(RECDEL) $(DESTDIR)\com
	$(HIDE)del java.rules

