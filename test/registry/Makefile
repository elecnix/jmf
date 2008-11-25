#
# @(#)Makefile	1.1 98/08/02
# Copyright 1998 Sun Microsystems, Inc.
# 

BUILDDIR = ..\..\build\win32

TARGDIR = ./
DESTDIR = .
SOUNDCLASSES = $(SWINGCLASSES);$(BUILDDIR)\lib;.

!include $(BUILDDIR)\makefiles\defs.nmk

DESTDIR = .

!include sources.mk

.SUFFIXES: .java .class

SOURCES_class = $(SOURCES_java:.java=.class)
SOURCES_class = $(SOURCES_class:/=\)

.java.class:
	$(ECHO) ---- Compiling $<
	$(JAVAC) $<

default: install

install: $(SOURCES_class)


