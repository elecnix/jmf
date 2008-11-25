#
# @(#)GNUmakefile	1.2 98/08/02
#

TOPDIR = ../..
PLATFORM = solaris
SRCDIR = .
SUBDIR = .
DESTDIR = .

# Cheating! Need to include swing, so borrowing SOUNDCLASSES
SOUNDCLASSES = $(SWINGCLASSES):$(TOPDIR)/build/$(PLATFORM)/lib

include sources.mk

include $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include
.INIT: $(TOPDIR)/build/$(PLATFORM)/makefiles/Makefile.java.include

