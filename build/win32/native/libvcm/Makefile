#
# @(#)Makefile	1.1 98/10/16
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libvfw
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmvcm.dll

EXTRA_LIBS = vfw32.lib user32.lib gdi32.lib kernel32.lib jmutil.lib

!include $(MINCLUDES)\win32_libvcm.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

