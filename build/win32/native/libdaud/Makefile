#
# @(#)Makefile	1.2 00/03/30
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libdaud
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmdaud.dll

EXTRA_LIBS = winmm.lib user32.lib jmutil.lib dsound.lib

!include $(MINCLUDES)\win32_libdaud.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

