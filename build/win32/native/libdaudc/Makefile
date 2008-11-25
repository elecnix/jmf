#
# @(#)Makefile	1.1 00/09/10
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libdaud
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmdaudc.dll

EXTRA_LIBS = winmm.lib user32.lib jmutil.lib dsound.lib

!include $(MINCLUDES)\win32_libdaudc.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

