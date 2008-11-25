/*
 * @(#)GainControlComponent.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.lang.reflect.Method;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import com.sun.media.controls.*;
import javax.media.GainChangeListener;
import javax.media.GainControl;
import javax.media.GainChangeEvent;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.sun.media.util.*;
import com.sun.media.renderer.audio.AudioRenderer;

public class GainControlComponent extends Container implements GainChangeListener {

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];

    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }

    public GainControlComponent(GainControl gain) {
	GridBagLayout gbl;
	GridBagConstraints gbc = new GridBagConstraints();

	this.gain = gain;
	gain.addGainChangeListener(this);

        setLayout(gbl = new GridBagLayout());

	if (canChangeVolume()) {

	    gbc.insets = new Insets(0, 0, 0, 0);
	    gbc.gridheight = 2;

	    muteButton = new MuteButton();
	    add(muteButton);
	    gbl.setConstraints(muteButton, gbc);

	    gbc.gridx = 1;
	    gbc.gridheight = 1;
	    upButton = new VolumeButton("volumeUp.gif",
				    "volumeUp-active.gif",
				    "volumeUp-pressed.gif",
				    "volumeUp-disabled.gif",
				    "Increase volume",
				    VolumeIncrement);
	    add(upButton);
	    gbl.setConstraints(upButton, gbc);

	    gbc.gridy = 1;
 	    downButton = new VolumeButton("volumeDown.gif",
				      "volumeDown-active.gif",
				      "volumeDown-pressed.gif",
				      "volumeDown-disabled.gif",
				      "Decrease volume",
				      -VolumeIncrement);

	    add(downButton);
	    gbl.setConstraints(downButton, gbc);
        } else {
            fUseVolumeControl=false;
            muteButton = new MuteButton();
	    add(muteButton);
        }

    }

    public void gainChange(GainChangeEvent e) {

        if (fUseVolumeControl==true) {
            float level = e.getLevel();
	    upButton.setEnabled(level < 1.0f);
	    downButton.setEnabled(level > 0.0f);
        }
	muteButton.setValue(e.getMute());
    }

    protected boolean canChangeVolume() {

        if ( (gain==null) || (gain.getLevel()<0.0f) ) {
          return false;
        }

	return true;
    }

    protected GainControl gain = null;
    protected MuteButton muteButton = null;
    protected VolumeButton upButton = null;
    protected VolumeButton downButton = null;
    protected boolean fUseVolumeControl=true;

    protected static final float VolumeIncrement = 0.05f;
    protected static final int RepeatDelay = 100;

    class VolumeButton extends ButtonComp {

	public VolumeButton(String imgNormal,
			    String imgActive,
			    String imgDown,
			    String imgDisabled,
			    String tip,
			    float increment) {
	    super ( tip, imgNormal, imgActive, imgDown, imgDisabled,
                         imgNormal, imgActive, imgDown, imgDisabled );

	    this.increment = increment;
	}

	public void action() {
	    float level;

	    if (gain != null) {
		level = gain.getLevel() + increment;
		if (level < 0.0f) {
		    level = 0.0f;
		} else if (level > 1.0f) {
		    level = 1.0f;
		}

		gain.setLevel(level);
		gain.setMute(false);
	    }
	}

	public void mousePressed(MouseEvent e) {
	    super.mousePressed(e);
	    if (repeater == null) {
		repeater = new Thread() {
		    public void run() {
			if (gain != null) {
			    float lastLevel = gain.getLevel();
			    float newLevel;
			    int unchangedCount = 0;

			    while (mouseDown && (unchangedCount < 5)) {
				try {
				    Thread.sleep(RepeatDelay);
				    try {
					action();
				    } catch (Exception ex) {
					mouseDown = false;
				    }
				    newLevel = gain.getLevel();
				    if (lastLevel == newLevel) {
					unchangedCount++;
				    } else {
					lastLevel = newLevel;
					unchangedCount = 0;
				    }
				} catch (InterruptedException ex) {
				    unchangedCount = 10;
				}
			    }
			}
		    }
		};
		repeater.start();
	    }
	}

	public void mouseReleased(MouseEvent e) {
	    super.mouseReleased(e);
	    if (repeater != null) {
		Thread killIt = repeater;
		repeater = null;
		boolean permission = true;

// 		try {
// 		    JMFSecurity.enablePrivilege.invoke(JMFSecurity.privilegeManager,
// 						       JMFSecurity.threadArgs);
// 		} catch (SecurityException sexc) {
// 		    permission = false;
// 		} catch (Exception exc) {
// 		}

		if ( securityPrivelege ) {
		    if (jmfSecurity != null) {
			try {
			    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD);
			    m[0].invoke(cl[0], args[0]);
			} catch (Exception ex) {
			    if (JMFSecurityManager.DEBUG) {
				System.err.println("Unable to get " + permission +
						   " privilege  " + ex);
			    }
			    permission = false;
			}
		    }
		} else {
		    permission = false;
		}
		if (permission) {
		    killIt.interrupt();
		}
	    }
	}

	public void setEnabled(boolean enabled) {
	    if (enabled != isEnabled()) {
		super.setEnabled(enabled);
		mouseActivity();
	    }
	}

	protected float increment;
	Thread repeater = null;
    }

    class MuteButton extends ButtonComp {

	public MuteButton() {
	    super( "Mute audio", "audio.gif", "audio-active.gif",
                                "audio-pressed.gif", "audio-disabled.gif",
                                "mute.gif", "mute-active.gif",
                                "mute-pressed.gif", "audio-disabled.gif" );
	}

	public void action() {

            if (gain != null) {
		gain.setMute(!gain.getMute());
	    }

	}
    }
}


