#
# @(#)Makefile	1.1 99/08/14
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libmci
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmmci.dll

EXTRA_LIBS = user32.lib gdi32.lib kernel32.lib jmutil.lib winmm.lib

!include $(MINCLUDES)\win32_libmci.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

