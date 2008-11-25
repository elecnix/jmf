#
# @(#)Makefile	1.1 98/08/24
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libjpeg
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmjpeg.dll

EXTRA_LIBS = kernel32.lib user32.lib gdi32.lib jmutil.lib

!include $(MINCLUDES)\libjpeg.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install
