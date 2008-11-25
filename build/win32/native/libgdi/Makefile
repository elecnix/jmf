#
# @(#)Makefile	1.2 99/10/20
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libgdi
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmgdi.dll

EXTRA_LIBS = kernel32.lib user32.lib gdi32.lib jmutil.lib vfw32.lib

!include $(MINCLUDES)\win32_libgdi.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

