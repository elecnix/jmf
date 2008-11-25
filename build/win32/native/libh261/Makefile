#
# @(#)Makefile	1.1 98/11/25
#

TOPDIR = ..\..\..\..
PLATFORM = win32

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libh261
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmh261.dll

EXTRA_LIBS = kernel32.lib user32.lib gdi32.lib jmutil.lib

!include $(MINCLUDES)\libh261.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

