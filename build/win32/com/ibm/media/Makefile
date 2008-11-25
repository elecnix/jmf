#
# @(#)Makefile	1.3 99/11/15
# Copyright 1998 Sun Microsystems, Inc.
# 

TOPDIR = ..\..\..\..\..

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

TARGDIR = $(DESTDIR)/com/ibm/media/

!include $(MINCLUDES)\com_ibm_media.jmk

!if "$(ALLJAVA)"==""
!include $(MINCLUDES)\win32_com_ibm_media.jmk
SOURCES_java = $(SOURCES_java) $(SOURCES_javan)
!endif

!include $(BUILDDIR)\makefiles\classes.nmk

!if "$(ACTION)"=="compile"
!include java.rules
!endif

install: java.rules FORCE
	$(HIDE)$(MAKE) -nologo compile ACTION=compile

compile: $(SOURCES_class)

java.rules: $(MINCLUDES)\com_ibm_media.jmk $(MINCLUDES)\win32_com_ibm_media.jmk
	$(ECHO) ---- Generating java.rules
	$(HIDE)..\..\..\com\sun\media\java_rules_gen $(SOURCES_java) >java.rules

FORCE:

clean:
	$(RECDEL) $(DESTDIR)\com
	$(HIDE)del java.rules

