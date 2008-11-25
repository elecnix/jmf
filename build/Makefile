#
# %W% %E%
#

TOPDIR = ..
JAVADOCDIR = $(TOPDIR)/build/apidocs

ifeq ($(JAVAHOME),)
JAVAHOME = /usr/local/java/jdk1.2/solaris
endif
JAVABIN = $(JAVAHOME)/bin

all: javadocs

clean:
	rm -rf $(JAVADOCDIR)

spec-license: $(JAVADOCDIR)
	/bin/cp spec-license.html $(JAVADOCDIR)
	/bin/chmod 664 $(JAVADOCDIR)/spec-license.html

javadocs: javadocs-footer

#
# Adds a footer to each javadoc generated page.
# This makes the best output; use this one normally.
#
javadocs-footer: $(JAVADOCDIR) spec-license
	$(JAVABIN)/javadoc -classpath $(TOPDIR)/src/share \
	-windowtitle "JMF 2.0 API (`date +%m/%d/%y`)" \
	-bottom "<font size=\"-1\"> \
	<a href=\"mailto:jmf-bugs@sun.com\">Submit a bug or feature</a> \
	<br>Copyright 1999-2001 Sun Microsystems, Inc. 901 San Antonio Road, \
	Palo Alto, California, 94303, U.S.A. All Rights Reserved. \
	See the <a href=\"spec-license.html\">Specification License</a> for more details. \
	<br>Sun, Sun Microsystems, and Java are trademarks or registered \
	trademarks of Sun Microsystems, Inc. in the US and other countries.</font>" \
	-sourcepath $(TOPDIR)/src/share \
	-d $(JAVADOCDIR) \
	javax.media \
	javax.media.protocol \
	javax.media.datasink \
	javax.media.control \
	javax.media.format \
	javax.media.renderer \
	javax.media.util \
	javax.media.rtp \
	javax.media.rtp.event \
        javax.media.rtp.rtcp \
	javax.media.bean.playerbean

#
# Adds a footer to each javadoc generated page.
# Use for proposed API changes.
#
javadocs-apichange: $(JAVADOCDIR) spec-license
	$(JAVABIN)/javadoc -classpath $(TOPDIR)/src/share \
	-windowtitle "JMF 2.0 Proposed API Changes (`date +%m/%d/%y`)" \
	-bottom "<font size=\"-1\"> \
	<b>JMF 2.0 Proposed API Changes (`date +%m/%d/%y`)</b>: \
	<a href=\"mailto:jmf-comments@sun.com\">click here to send API Feedback</a> \
	<br>Copyright 2001 Sun Microsystems, Inc. 901 San Antonio Road, \
	Palo Alto, California, 94303, U.S.A. All Rights Reserved. \
	Sun, Sun Microsystems, and Java are trademarks or registered \
	trademarks of Sun Microsystems, Inc. in the US and other countries.</font>" \
	-sourcepath $(TOPDIR)/src/share \
	-d $(JAVADOCDIR) \
	javax.media \
	javax.media.protocol \
	javax.media.datasink \
	javax.media.control \
	javax.media.format \
	javax.media.renderer \
	javax.media.util \
	javax.media.rtp \
	javax.media.rtp.event \
        javax.media.rtp.rtcp \
	javax.media.bean.playerbean

#
# Adds a header and a footer to each javadoc generated page.
# This is ugly. Keep here in case Legal wants to see it again.
#
javadocs-header: $(JAVADOCDIR) spec-license
	$(JAVABIN)/javadoc -classpath $(TOPDIR)/src/share \
	-windowtitle "JMF 2.1" \
	-header "<font size=\"-1\"> \
	<a href=\"mailto:jmf-bugs@sun.com\">Submit a bug or feature</a> \
	<br>Copyright 1999-2001 Sun Microsystems, Inc. 901 San Antonio Road, \
	Palo Alto, California, 94303, U.S.A. All Rights Reserved. \
	See the <a href=\"spec-license.html\">Specification License</a> for more details. \
	<br>Sun, Sun Microsystems, and Java are trademarks or registered \
	trademarks of Sun Microsystems, Inc. in the US and other countries.</font>" \
	-sourcepath $(TOPDIR)/src/share \
	-d $(JAVADOCDIR) \
	javax.media \
	javax.media.protocol \
	javax.media.datasink \
	javax.media.control \
	javax.media.format \
	javax.media.renderer \
	javax.media.util \
	javax.media.rtp \
	javax.media.rtp.event \
        javax.media.rtp.rtcp \
	javax.media.bean.playerbean

$(JAVADOCDIR):
	mkdir -p $(JAVADOCDIR)

