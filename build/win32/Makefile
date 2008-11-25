#
# %W% %E%
# Copyright 1998-1999 Sun Microsystems, Inc.
# 

TOPDIR = ..\..

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIRS = prologue javax\media com\sun\media com\ibm\media customizer

!if "$(ALLJAVA)"==""
SUBDIRS = $(SUBDIRS) native
!endif

SUBDIRS = $(SUBDIRS) jmapps registry

#SUBDIRS = prologue javax\media com\sun\media hotmedia com\ibm\media hotmedia2 native test\player test\registry registry

SCSL = $(DESTDIR)\com

default: $(DESTDIR) $(SCSL) $(CUSTOMLIB) $(SUBDIRS)

IMAGEDIR = $(TOPDIR)\build\win32\image
TESTPLAYER = $(TOPDIR)\test\player\				# Need this comment here
TESTPLAYERFILE = $(TOPDIR)\test\player\file\			# Need this comment here
TESTREGISTRY = $(TOPDIR)\test\registry\				# Need this comment here
TESTPLAYERRTP = $(TOPDIR)\test\player\rtp\			# Need this comment here
TESTPLUGIN = $(TOPDIR)\test\plugin\				# Need this comment here
SAMPLESDIR = $(IMAGEDIR)\samples
SAMPLESRTPDIR = $(IMAGEDIR)\samples\rtp
CP = $(HIDE)COPY >$(TOPDIR)\tmp
IFNATIVE= $(HIDE)IF "A$(ALLJAVA)"=="A"
IFALLJAVA= $(HIDE)IF NOT "A$(ALLJAVA)"=="A"

# !include $(TOPDIR)\build\minclude\test_samples.jmk

BEANDIR = javax\media\bean\playerbean
MANIFEST = $(TOPDIR)\src\share\$(BEANDIR)\manifest.txt

image: cleanimage $(IMAGEDIR) $(IMAGEDIR)\lib $(IMAGEDIR)\bin $(IMAGEDIR)\doc
	$(ECHO) -- Creating image
	$(ECHO) ---- Deleting unnecessary files
	$(HIDE)if exist $(DESTDIR)\jmf.log del /q $(DESTDIR)\jmf.log >$(TOPDIR)\tmp
	$(IFNATIVE) echo ---- Copying libraries
	$(IFNATIVE) copy $(DESTDIR)\*.dll $(IMAGEDIR)\lib >$(TOPDIR)\tmp
	$(HIDE)     copy $(DESTDIR)\jmf.properties $(IMAGEDIR)\lib >$(TOPDIR)\tmp
	$(IFNATIVE) echo ---- Copying javasound stuff
        $(IFNATIVE) copy $(SOUNDCLASSES)\win32\jsound.dll $(IMAGEDIR)\lib >$(TOPDIR)\tmp
	$(IFNATIVE) copy $(SOUNDCLASSES)\sound.jar $(IMAGEDIR)\lib >$(TOPDIR)\tmp
	$(IFNATIVE) copy $(SOUNDCLASSES)\soundbank.gm $(IMAGEDIR)\lib >$(TOPDIR)\tmp
	$(HIDE)cd $(DESTDIR)
	$(ECHO) ---- Creating jmf.jar file
	$(HIDE)$(JAVAHOME)\bin\jar cfM ..\$(IMAGEDIR)\lib\jmf.jar *.class codecLib javax com jmapps
	$(ECHO) ---- Creating mediaplayer.jar file
	$(HIDE)copy ..\$(TOPDIR)\src\share\$(BEANDIR)\*.gif $(BEANDIR) >..\$(TOPDIR)\tmp
	$(HIDE)$(JAVAHOME)\bin\jar cfm ..\$(IMAGEDIR)\lib\mediaplayer.jar ..\$(MANIFEST) $(BEANDIR)
	$(HIDE)del $(BEANDIR)\*.gif >..\$(TOPDIR)\tmp
	$(HIDE)cd ..
	$(ECHO) ---- Creating customizer.jar file
	$(HIDE)cd customlib
	$(HIDE)copy ..\$(TOPDIR)\src\share\com\sun\media\customizer\CustomizerHelp.html .
	$(HIDE)if exist javax $(RECDEL) javax
	$(HIDE) $(JAVAHOME)\bin\jar cfM ..\$(IMAGEDIR)\lib\customizer.jar *
	$(HIDE)cd ..
	$(ECHO) ---- Creating launch executables
	$(IFNATIVE) $(MSDEVHOME)\BIN\CL -nologo -I$(MSDEVHOME)\INCLUDE -o $(IMAGEDIR)\bin\jmstudio.exe -DJMSTUDIO $(TOPDIR)\src\win32\native\jmplay\jmlaunch.c /link /LIBPATH:$(MSDEVHOME)\lib >$(TOPDIR)\tmp
	$(IFNATIVE) $(MSDEVHOME)\BIN\CL -nologo -I$(MSDEVHOME)\INCLUDE -o $(IMAGEDIR)\bin\jmfregistry.exe -DJMFREGISTRY $(TOPDIR)\src\win32\native\jmplay\jmlaunch.c  /link /LIBPATH:$(MSDEVHOME)\lib >$(TOPDIR)\tmp
	$(IFNATIVE) $(MSDEVHOME)\BIN\CL -nologo -I$(MSDEVHOME)\INCLUDE -o $(IMAGEDIR)\bin\jmfinit.exe -DJMFINIT $(TOPDIR)\src\win32\native\jmplay\jmlaunch.c  /link /LIBPATH:$(MSDEVHOME)\lib >$(TOPDIR)\tmp
	$(IFNATIVE) $(MSDEVHOME)\BIN\CL -nologo -I$(MSDEVHOME)\INCLUDE -o $(IMAGEDIR)\bin\jmfcustomizer.exe -DJMFCUST $(TOPDIR)\src\win32\native\jmplay\jmlaunch.c  /link /LIBPATH:$(MSDEVHOME)\lib >$(TOPDIR)\tmp
	$(IFNATIVE) del jmlaunch.obj >$(TOPDIR)\tmp
	$(HIDE)copy $(TOPDIR)\doc\share\* $(IMAGEDIR)\doc >$(TOPDIR)\tmp
	$(HIDE)copy $(TOPDIR)\src\solaris\bin\jm* $(IMAGEDIR)\bin >$(TOPDIR)\tmp
	$(ECHO) -- Done creating image
	$(HIDE)copy $(TOPDIR)\src\win32\bin\JMF.ico $(IMAGEDIR)\bin >$(TOPDIR)\tmp

#	$(HIDE)copy $(SOUNDCLASSES)\soundbank.gm $(IMAGEDIR)\lib >$(TOPDIR)\tmp

cleanimage: FORCE
	$(HIDE) if exist $(IMAGEDIR) $(RECDEL) $(IMAGEDIR) >$(TOPDIR)\tmp

full: default image signit bundle

!ifdef AUTOSIGN

!ifdef ALLJAVA

signit:
	$(ECHO) -- Signing Jmf.jar
	$(HIDE)IF EXIST A:\SIGNTOOL.exe Copy $(IMAGEDIR)\lib\jmf.jar C:\TEMP
	$(HIDE)IF EXIST A:\SIGNTOOL.exe A:\SIGN C:\TEMP\jmf.jar $(JAVAHOME)
	$(HIDE)IF EXIST A:\SIGNTOOL.exe Copy C:\TEMP\jmf.jar $(IMAGEDIR)\lib
!else
signit:
	$(ECHO) -- Signing Jmf.jar
	$(HIDE)IF EXIST A:\SIGNTOOL.exe Copy $(IMAGEDIR)\lib\jmf.jar C:\TEMP
	$(HIDE)IF EXIST A:\SIGNTOOL.exe A:\SIGN C:\TEMP\jmf.jar $(JAVAHOME)
	$(HIDE)IF EXIST A:\SIGNTOOL.exe Copy C:\TEMP\jmf.jar $(IMAGEDIR)\lib
	$(ECHO) -- Signing Sound.jar
	$(HIDE)IF EXIST A:\SIGNTOOL.exe Copy $(IMAGEDIR)\lib\sound.jar C:\TEMP
	$(HIDE)IF EXIST A:\SIGNTOOL.exe A:\SIGN C:\TEMP\sound.jar $(JAVAHOME)
	$(HIDE)IF EXIST A:\SIGNTOOL.exe Copy C:\TEMP\sound.jar $(IMAGEDIR)\lib
!endif

!else

signit:

!endif

!ifdef ALLJAVA

bundle:
	$(HIDE)if exist JMF2.1.1 $(RECDEL) JMF2.1.1
	$(HIDE)move image JMF2.1.1
	$(ECHO) -- Zipping
	$(HIDE)ZIP -r jmf-2_1_1e-alljava.zip JMF2.1.1

!else

bundle:
	$(HIDE)if exist C:\TEMP\JMFIMAGE $(RECDEL) C:\TEMP\JMFIMAGE
	$(HIDE)if exist C:\TEMP\JMF-2.0 $(RECDEL) C:\TEMP\JMF-2.0
	$(HIDE)cd ishield
	$(HIDE)$(MAKE) -nologo
	$(HIDE)cd ..

!endif

$(TEMPDIR) $(IMAGEDIR) $(IMAGEDIR)\lib $(IMAGEDIR)\bin $(IMAGEDIR)\doc:
	$(HIDE)md $@

prologue: FORCE
	@echo -- DIRECTORY $@
	@cd $@
	@$(MAKE) -nologo install
	@cd ..

javax\media test\plugin test\bean: FORCE
	@echo -- DIRECTORY $@
	@cd $@
	@$(MAKE) -nologo install
	@cd ..\..

com\sun\media com\ibm\media: FORCE
	@echo -- DIRECTORY $@
	@cd $@
	@$(MAKE) -nologo install
	@cd ..\..\..

registry jmapps hotmedia hotmedia2 native customizer : FORCE
	@cd $@
	@$(MAKE) -nologo install
	@cd ..

clean:
	$(ECHO) -- Deleting $(DESTDIR)
	$(HIDE)if exist $(DESTDIR) $(RECDEL) $(DESTDIR)
	$(ECHO) -- Deleting $(TOPDIR)\build\$(PLATFORM)\bin
	$(HIDE)if exist $(TOPDIR)\build\$(PLATFORM)\bin $(RECDEL) $(TOPDIR)\build\$(PLATFORM)\bin
	$(ECHO) -- Cleaning native
	@cd native
	@$(MAKE) -nologo clean
	@cd ..

$(DESTDIR):
	$(ECHO) ---- Creating $(DESTDIR)
	$(HIDE)md $(DESTDIR)

$(CUSTOMLIB):
	$(ECHO) ---- Creating $(CUSTOMLIB)
	$(HIDE)md $(CUSTOMLIB)

$(SCSL):
	$(ECHO) -- Copy class files and libs for which source is not included
	$(HIDE)xcopy /s /q $(TOPDIR)\derived $(DESTDIR)
	$(HIDE)xcopy /s /q $(TOPDIR)\derived_win32 $(DESTDIR)

FORCE:
