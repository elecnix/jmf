#
# @(#)Makefile	1.2 99/02/17
#

TOPDIR = ..\..\..\..
PLATFORM = win32
# I don't think we need these defines but just in case we do...
DEFINES=-D DBG=0 -D WINVER=0x400 -D _X86_=1 -D "_DLL" -D "_MT" -D "_WIN32" -D "WIN32" -D "STRICT" -D "INC_OLE2" -D try=__try -D except__except -D leave=__leave -D finally=__finally -D _WIN32_WINNT=-0x0400

OPTFLAG=-Gz -MD -W3 -Gy -Oxs -Gf

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIR = native\libds
TARGDIR = $(JMFBUILDDIR)\			# Trailing space needed
LIBRARY = jmds.dll

EXTRA_LIBS = strmbase.lib msvcrt.lib quartz.lib vfw32.lib winmm.lib kernel32.lib advapi32.lib version.lib largeint.lib user32.lib gdi32.lib comctl32.lib ole32.lib olepro32.lib oleaut32.lib uuid.lib /base:"0x1d1c0000" /entry:"DllEntryPoint@12" /dll /pdb:none /machine:I386 /nodefaultlib /def:"..\..\..\..\src\win32\native\libds\filter2stream.def" /subsystem:windows,4.0 /align:0x1000 /opt:ref /release /debug:none 

!include $(MINCLUDES)\win32_libds.cmk

!include $(BUILDDIR)\makefiles\library.nmk

default: install

