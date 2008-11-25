/*
 * @(#)CacheControlComponent.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.*;
import javax.media.*;
import com.sun.media.util.*;

public class CacheControlComponent extends BufferedPanel {

    public CacheControlComponent(CachingControl ctrl, Player player) {
        GridBagLayout gbl;
        GridBagConstraints gbc;
        Panel controls;
        Label label;

        this.ctrl = ctrl;
        this.player = player;

        if (ctrl instanceof ExtendedCachingControl) {
            xtdctrl = (ExtendedCachingControl) ctrl;
        }
        setBackground(DefaultControlPanel.colorBackground);
        setBackgroundTile(BasicComp.fetchImage("texture3.gif"));
        setLayout(gbl = new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        label = new Label(JMFI18N.getResource("mediaplayer.download"), Label.CENTER);
        add(label);
        gbl.setConstraints(label, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        cancelButton = new CancelButton();
        add(cancelButton);
        gbl.setConstraints(cancelButton, gbc);
        gbc.gridx++;
        progressBar = new ProgressBar(ctrl);
        add(progressBar);
        gbl.setConstraints(progressBar, gbc);
    }

    public void addNotify() {
        super.addNotify();
        setSize(getPreferredSize());
    }

    public Component getProgressBar() {
        return progressBar;
    }

    protected CachingControl ctrl = null;
    private ExtendedCachingControl xtdctrl = null;
    protected Player player = null;
    protected ButtonComp cancelButton = null;
    protected ProgressBar progressBar = null;

    class CancelButton extends ButtonComp {

        public CancelButton() {
            super ( "Suspend download",
                "pause.gif", "pause-active.gif", "pause-pressed.gif", "pause-disabled.gif",
                "play.gif", "play-active.gif", "play-pressed.gif", "play-disabled.gif" );
        }

        public void action() {
            super.action();
            if (player != null) {
		// It would be nice if CachingControl had stopDownload and
		// startDownload methods so that we can avoid the cast
/*
		if (state) {
		    ((com.sun.media.MediaCachingControl) ctrl).stopDownload();
		} else {
		    ((com.sun.media.MediaCachingControl) ctrl).startDownload();
		}
*/
            }
            if (xtdctrl != null) {
                if (state) {
                    xtdctrl.pauseDownload();
                } else {
                    xtdctrl.resumeDownload();
                }
            }
        }
    }
}


