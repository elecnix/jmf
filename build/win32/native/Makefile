#
# @(#)Makefile	1.31 01/03/28
#

TOPDIR = ..\..\..

!include $(TOPDIR)\build\win32\makefiles\defs.nmk

SUBDIRS = libjmutil libjmfjawt libdaud libdaudc libmci libvfw libvcm libddraw libgdi libjpeg libacm libh261

!ifdef DXMEDIA
SUBDIRS = $(SUBDIRS) libam
!endif

default: $(DESTDIR) $(SUBDIRS)

install: FORCE
	@$(MAKE) -nologo ACTION=install

$(SUBDIRS): FORCE
	@echo -- DIRECTORY $@
	@cd $@
	@$(MAKE) -nologo $(ACTION)
	@cd ..

clean: FORCE
	@$(MAKE) -nologo ACTION=clean

$(DESTDIR):
	$(ECHO) ---- Creating $(DESTDIR)
	$(HIDE)md $(DESTDIR)

FORCE:
