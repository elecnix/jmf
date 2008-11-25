.KEEP_STATE:

include ./Makefile.include

PACKAGE= com/sun/media/customizer
SRCDIR= $(TOPDIR)/src/share/$(PACKAGE)
OBJDIR= $(TOPDIR)/build/solaris/lib

$$(OBJDIR)/%.class: $(SRCDIR)/%.java
	$(JAVAC) -d $(OBJDIR) $<

SRCFILES = \
	TokenDef.java \
        DBItem.java \
        CustomDB.java \
	I18N.java \
	Props.java \
        ProgressDiag.java \
        ProcessJAR.java \
        GeneralPane.java \
        ProtocolPane.java \
        MFormatPane.java \
        CodecPane.java \
        RendererPane.java \
        MuxPane.java \
	Customizer.java \
	HelperDiag.java

OBJS = $(SRCFILES:%.java=$(OBJDIR)/%.class)

default-target: all

all: class-dir $(OBJS)

class-dir:
	@test -d $(OBJDIR) || mkdir -p $(OBJDIR)

#html: @echo html must be made from top level src directory


