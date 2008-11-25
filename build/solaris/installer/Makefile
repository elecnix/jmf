# @(#)Makefile	1.15 03/05/01

TOPDIR = ../../..
PLATFORM = solaris

include $(TOPDIR)/build/solaris/makefiles/Makefile.defs

JMFIMAGE = JMF-2.1.1e

ifeq ($(OUTDIR),)
OUTDIR = /tmp
endif

all:
	$(ECHO) Making bundle
	$(ECHO) -- Deleting old files
	$(RM) -r $(JMFIMAGE)
	$(ECHO) -- Copying image over
	$(MKDIR) $(JMFIMAGE)
	$(CP) -r $(TOPDIR)/build/solaris/image/* $(JMFIMAGE)
	$(ECHO) -- Creating self-extract archive
	$(CMD_PREFIX)/bin/sh genzip jmf-2_1_1e-solaris $(JMFIMAGE)
	$(CP) jmf-2_1_1e-solaris.bin $(OUTDIR)
	$(ECHO) Done.
