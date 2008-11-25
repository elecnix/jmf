#
# @(#)Makefile	1.12 02/12/08
#

TOPDIR = ../../..
PLATFORM = solaris
SUBDIR = javax/media
SRCDIR = $(TOPDIR)/src/share
SRCREFDIR = $(TOPDIR)/src/solaris

include $(TOPDIR)/build/solaris/makefiles/Makefile.defs

#include $(TOPDIR)/build/minclude/sol_plugins.jmk
#include $(TOPDIR)/build/minclude/plugins.jmk

TESTDIR = $(TOPDIR)/test/registry
REGISTRYDIR = $(TOPDIR)/src/share/com/sun/media/util

all: announce REGISTRYSTUFF CREATECUSTOMIZERJAR

DOREGISTRYSTUFF: JMFPROP $(TESTDIR)/RegisterPlugIn.class $(DESTDIR)/jmf.properties register registrylib GENERATE_JMFPROPERTIES

announce:
	$(ECHO) Registering default plugins and committing...

JMFPROP: FORCE
	@cp $(TOPDIR)/test/solaris/jmf.properties $(DESTDIR)/jmf.properties
	@chmod 644 $(DESTDIR)/jmf.properties

ifeq ($(QUICKBUILD),TRUE)

REGISTRYSTUFF: $(DESTDIR)/jmf.properties

$(DESTDIR)/jmf.properties:
	$(HIDE)$(MAKE) DOREGISTRYSTUFF

else

REGISTRYSTUFF: DOREGISTRYSTUFF

endif

$(TESTDIR)/RegisterPlugIn.class: $(TESTDIR)/RegisterPlugIn.java
	$(ECHO) -- Compiling $<
	$(JAVAC) -d $(TESTDIR) -classpath $(CLASSPATH) $<

register: FORCE
	$(JAVA) -classpath $(CLASSPATH):$(TESTDIR) RegisterPlugIn

registrylib: FORCE
	$(JAVA) -classpath $(CLASSPATH) com.sun.media.util.RegistryGen \
	     -d $(REGISTRYDIR) com.sun.media.util.RegistryLib
	$(JAVAC) -d $(DESTDIR) -classpath $(CLASSPATH) $(REGISTRYDIR)/RegistryLib.java
FORCE:

#
# Generate the jmf.properties file
#
GENERATE_JMFPROPERTIES:
	$(ECHO) -- Generating jmf.properties
	$(JAVA) -classpath $(CLASSPATH) com.sun.media.util.JMFPropertiesGen \

CREATECUSTOMIZERJAR:
	$(ECHO) -- Generating customizer.jar
	$(CMD_PREFIX)cp $(TOPDIR)/src/share/com/sun/media/customizer/CustomizerHelp.html $(TOPDIR)/build/solaris/customlib
	$(CMD_PREFIX)chmod 666 $(TOPDIR)/build/solaris/customlib/CustomizerHelp.html
	$(CMD_PREFIX)(cd $(TOPDIR)/build/solaris/customlib ; rm -rf javax)
	$(CMD_PREFIX)(cd $(TOPDIR)/build/solaris/customlib ; $(JAVAHOME)/bin/jar cf $(TOPDIR)/build/solaris/customizer.jar * )

install: all

clean:

