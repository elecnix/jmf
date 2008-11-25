#
# @(#)Makefile	1.31 01/02/09
#
DIRS = libjmutil libjmfjawt libcvidpro libxlib libxil  libdaud libjmopi libjpeg libh261 libmpx

TARGETS = all clean install objects packages targets sources sccs_clean sbtags sblink sblink_clean

.KEEP_STATE:

$(TARGETS):
	$(MAKE) $(DIRS) TARGET=$@

$(DIRS):: FORCE
	cd $@; $(MAKE) $(TARGET)

FORCE:
