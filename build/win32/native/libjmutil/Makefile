#
# @(#)Makefile	1.1 98/08/20
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libjmutil
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmutil.dll

EXTRA_LIBS = kernel32.lib user32.lib gdi32.lib

!include $(MINCLUDES)\libjmutil.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install
