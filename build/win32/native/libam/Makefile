#
# @(#)Makefile	1.1 99/07/14
#

TOPDIR = ..\..\..\..
PLATFORM = win32
# I don't think we need these defines but just in case we do...
#DEFINES= -D _X86_=1 -D "_DLL" -D "_MT" -D "_WIN32" -D "WIN32"
#OPTFLAG=-Gz -MD -W3 -Gy -Oxs -Gf

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libam
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmam.dll

EXTRA_LIBS = jmutil.lib amstrmid.lib daguid.lib danim.lib measure.lib guids.lib msvcrt.lib quartz.lib strmbase.lib uuid.lib strmiids.lib ole32.lib user32.lib advapi32.lib winmm.lib gdi32.lib comctl32.lib kernel32.lib olepro32.lib oleaut32.lib /nodefaultlib

!include $(MINCLUDES)\win32_libam.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

