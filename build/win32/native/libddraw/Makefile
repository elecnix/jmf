#
# @(#)Makefile	1.1 98/08/20
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libddraw
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmddraw.dll

EXTRA_LIBS = kernel32.lib user32.lib gdi32.lib ddraw.lib jmutil.lib

!include $(MINCLUDES)\win32_libddraw.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

