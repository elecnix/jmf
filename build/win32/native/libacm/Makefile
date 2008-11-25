#
# @(#)Makefile	1.1 98/10/13
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libacm
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmacm.dll

EXTRA_LIBS = msacm32.lib user32.lib kernel32.lib

!include $(MINCLUDES)\win32_libacm.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

