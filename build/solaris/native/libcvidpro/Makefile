#
# @(#)Makefile	1.2 99/11/17
#

TOPDIR = ../../../..
PLATFORM = solaris
SUBDIR = native/libcvidpro
SRCDIR = $(TOPDIR)/src/solaris
BUILDDIR = $(TOPDIR)/build/solaris/lib

.INIT:	$(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.defs
include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.defs

CVIDPRO = $(BUILDDIR)/libCvidPro.so
CVIDPRO_SRC = $(SRCDIR)/$(SUBDIR)/libCvidPro.so
JMFCVIDPRO = $(BUILDDIR)/libjmfCVIDPro.so
JMFCVIDPRO_SRC = $(SRCDIR)/$(SUBDIR)/libjmfCVIDPro.so


# Just copy the native libaries to the build directory.

install: all
all: announce $(CVIDPRO) $(JMFCVIDPRO)

$(CVIDPRO): $(CVIDPRO_SRC)
	$(ECHO) -- Copying libCvidPro.so
	$(CP) $(CVIDPRO_SRC) $(CVIDPRO)

$(JMFCVIDPRO): $(JMFCVIDPRO_SRC)
	$(ECHO) -- Copying libjmfCVIDPro.so
	$(CP) $(JMFCVIDPRO_SRC) $(JMFCVIDPRO)

announce:
	$(ECHO) DIRECTORY $(SUBDIR)

clean:
	$(RM) $(CVIDPRO)
	$(RM) $(JMCVIDPRO)
