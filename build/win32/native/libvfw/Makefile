#
# @(#)Makefile	1.3 98/10/29
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libvfw
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmvfw.dll

EXTRA_LIBS = vfw32.lib user32.lib gdi32.lib kernel32.lib jmutil.lib

!include $(MINCLUDES)\win32_libvfw.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

