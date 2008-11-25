#
# @(#)Makefile	1.1 98/08/07
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libdevice
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmdevice.dll

EXTRA_LIBS = kernel32.lib user32.lib gdi32.lib winmm.lib

!include $(MINCLUDES)\win32_libdevice.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

