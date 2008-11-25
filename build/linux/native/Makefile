#
# @(#)Makefile	1.9 01/02/09
#

DIRS = libjmutil libjmfjawt libxlib libdaud libjpeg libh261 libmpx libv4l

ifdef COMPILE_VH263
DIRS += libvh263
endif


TARGETS = all clean install objects packages targets sources sccs_clean sbtags sblink sblink_clean

.KEEP_STATE:

$(TARGETS):
	$(MAKE) $(DIRS) TARGET=$@

$(DIRS):: FORCE
	cd $@; $(MAKE) $(TARGET)

FORCE:
