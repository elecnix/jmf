# @(#)Makefile	1.12 00/08/30

TOPDIR = ..\..\..

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

!ifndef ISDIR
ISDIR=C:\progra~1\instal~1
!endif

default: DIRS REL_IS_FILES RUN_ISBUILD

COMPDEFS = "JMF-2.0\\Component Definitions"
FILEGRPS = "JMF-2.0\\File Groups"
MEDIA = "JMF-2.0\\Media"
DEFMEDIA = "JMF-2.0\\Media\\DefaultMedia"
REGENTR = "JMF-2.0\\Registry Entries"
SCRIPTS = "JMF-2.0\\Script Files"
SETUPS = "JMF-2.0\\Setup Files"
COMPR = "JMF-2.0\\Setup Files\\Compressed Files"
LANGINDP = "JMF-2.0\\Setup Files\\Compressed Files\\Language Independent"
OSINDP = "JMF-2.0\\Setup Files\\Compressed Files\\Language Independent\\OS Independent"
UNCOMPR = "JMF-2.0\\Setup Files\\Uncompressed Files"
ULANGINDP = "JMF-2.0\\Setup Files\\Uncompressed Files\\Language Independent"
UOSINDP = "JMF-2.0\\Setup Files\\Uncompressed Files\\Language Independent\\OS Independent"
SHELLOBJ = "JMF-2.0\\Shell Objects"
STRINGTAB = "JMF-2.0\\String Tables"
STRENG = "JMF-2.0\\String Tables\\0009-English"
TEXTSUBS = "JMF-2.0\\Text Substitutions"
# The following locations should not be changed. They are hardcoded in the scripts
TEMPDIR = C:\TEMP
IMAGECOPY = C:\TEMP\JMFIMAGE

DIRS: $(TEMPDIR) $(TEMPDIR)\JMF-2.0 $(COMPDEFS) $(FILEGRPS) $(MEDIA) $(DEFMEDIA) $(REGENTR) $(SCRIPTS) $(SETUPS) $(COMPR) $(LANGINDP) $(OSINDP) $(UNCOMPR) $(ULANGINDP) $(UOSINDP) $(SHELLOBJ) $(STRINGTAB) $(STRENG) $(TEXTSUBS) $(IMAGECOPY)

$(TEMPDIR) $(TEMPDIR)\JMF-2.0 $(COMPDEFS) $(FILEGRPS) $(MEDIA) $(DEFMEDIA) $(REGENTR) $(SCRIPTS) $(SETUPS) $(COMPR) $(LANGINDP) $(OSINDP) $(UNCOMPR) $(ULANGINDP) $(UOSINDP) $(SHELLOBJ) $(STRINGTAB) $(STRENG) $(TEXTSUBS) $(IMAGECOPY):
	$(HIDE)mkdir $@

REL_IS_FILES:
	$(HIDE)copy Default.cdf $(COMPDEFS)
	$(HIDE)copy AllFiles.fgl $(FILEGRPS)
	$(HIDE)copy Default.fgl $(COMPDEFS)
	$(HIDE)copy Default.fdf $(FILEGRPS)
	$(HIDE)copy default.mda $(DEFMEDIA)
	$(HIDE)copy Default.rge $(REGENTR)
	$(HIDE)copy Setup.rul $(SCRIPTS)
	$(HIDE)copy license.txt $(OSINDP)
	$(HIDE)copy setup.bmp $(UOSINDP)
	$(HIDE)copy Default.shl $(SHELLOBJ)
	$(HIDE)copy Default2.shl $(STRINGTAB)\Default.shl
	$(HIDE)copy value.shl $(STRENG)
	$(HIDE)copy Build.tsb $(TEXTSUBS)
	$(HIDE)copy Setup.tsb $(TEXTSUBS)
	$(HIDE)xcopy /s /q $(TOPDIR)\build\win32\image\* $(IMAGECOPY)
	$(HIDE)xcopy /s /q JMF-2.0 $(TEMPDIR)\JMF-2.0

RUN_ISBUILD: FORCE
	$(HIDE)cd /D "C:\temp\jmf-2.0\script files"
	$(HIDE)$(ISDIR)\instal~1.1pr\program\COMPILE.EXE -v0 -I$(ISDIR)\instal~1.1pr\Include Setup.rul
	$(HIDE)copy $(MAKEDIR)\Setup.ins "C:\temp\jmf-2.0\script files\"
	$(HIDE)$(ISDIR)\instal~1.1pr\program\ISBUILD.EXE -p$(TEMPDIR)\JMF-2.0 -mDefaultMedia
	$(HIDE)cd /D $(MAKEDIR)
	$(HIDE)copy jmf20.pfw $(TEMPDIR)
	$(HIDE)attrib -r $(TEMPDIR)\jmf20.pfw
	$(HIDE)$(ISDIR)\packag~1\pftwwiz.exe $(TEMPDIR)\jmf20.pfw -a -s

FORCE:

