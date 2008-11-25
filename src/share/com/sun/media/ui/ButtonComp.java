/*
 * @(#)ButtonComp.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.awt.*;
import java.awt.event.*;

public class ButtonComp extends BasicComp implements MouseListener {

    Image imageNormal[];
    Image imageActive[];
    Image imageDown[];
    Image imageDisabled[];

    static final int NORMAL = 1;
    static final int ACTIVE = 2;
    static final int DOWN   = 4;
    static final int DISABLED = 8;

    int width, height;

    boolean state = false;
    boolean mouseIn = false;
    boolean mouseDown = false;
    boolean mouseUp = false;
    boolean mouseClick = false;
    int visualState = NORMAL;

    private PopupMenu           menuPopup = null;
    private ContPressThread     threadContPress = null;
    private boolean             boolContPress = false;
    private boolean             boolPopup = false;
    private boolean             boolDoAction = false;

    private static final int    POPUP_DELAY = 1000;
    

    public ButtonComp(String label,
		      String imgNormal0,
		      String imgActive0,
		      String imgDown0,
		      String imgDisabled0,
		      String imgNormal1,
		      String imgActive1,
		      String imgDown1,
		      String imgDisabled1) {
        super(label);

        // Two images each
        imageNormal = new Image[2];
        imageActive = new Image[2];
        imageDown = new Image[2];
        imageDisabled = new Image[2];

        // Load the images
        imageNormal[0] = fetchImage(imgNormal0);
        imageNormal[1] = fetchImage(imgNormal1);
        imageActive[0] = fetchImage(imgActive0);
        imageActive[1] = fetchImage(imgActive1);
        imageDown[0] = fetchImage(imgDown0);
        imageDown[1] = fetchImage(imgDown1);
        imageDisabled[0] = fetchImage(imgDisabled0);
        imageDisabled[1] = fetchImage(imgDisabled1);

        width = imageNormal[0].getWidth( this );
        height = imageNormal[0].getHeight( this );
        visualState = NORMAL;
        setSize(width, height);
        setVisible(true);
        addMouseListener(this);
    }

    public void mouseActivity() {

        if (isEnabled()) {
            if (mouseIn) {
                if (mouseDown) {
                    visualState = DOWN;
                    if (mouseUp) {
                        action();
                        visualState = ACTIVE;
                    }
                }
                else {
                    visualState = ACTIVE;
                }
            }
            else {
                visualState = NORMAL;
            }
        }
        else {
            visualState = DISABLED;
        }
        repaint();
    }

    public void action() {
        if ( boolDoAction == false )
            return;
        state = !state;
        informListener();
    }

    public void paint(Graphics g) {
        int index = state? 1:0;
        Image image = null;

        switch (visualState) {
            case NORMAL:
                image = imageNormal[index];
                break;
            case ACTIVE:
                image = imageActive[index];
                break;
            case DOWN:
                image = imageDown[index];
                break;
            case DISABLED:
                image = imageDisabled[index];
                break;
        }
        if (image != null)
            g.drawImage(image, 0, 0, this);
    }

    public void setEnabled(boolean value) {
        super.setEnabled(value);
        if (value == false) {
            visualState = DISABLED;
            //setCursor( new Cursor(Cursor.DEFAULT_CURSOR) );
        }
        else {
            if (mouseIn) {
                if (mouseDown)
                    visualState = DOWN;
                else
                    visualState = ACTIVE;
            }
            else {
                visualState = NORMAL;
                //setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        }
        repaint();
    }

    public boolean getValue() {
        return state;
    }

    public void setValue(boolean newState) {
        if (state != newState) {
            state = newState;
            repaint();
        }
    }

    public void setPopupMenu ( PopupMenu menuPopup ) {
	if (menuPopup != null) {
            setMousePopup ( true );
            this.menuPopup = menuPopup;
            this.add ( menuPopup );
	} else if (this.menuPopup != null) {
	    setMousePopup(false);
	    remove(this.menuPopup);
	    this.menuPopup = null;
	}
    }

    public void setMousePopup ( boolean boolPopup ) {
        this.boolPopup = boolPopup;
    }

    public void setContMousePress ( boolean boolSet ) {
        boolContPress = boolSet;
    }

    public void mouseEntered(MouseEvent e) {
        mouseIn = true;
        mouseActivity();
    }

    public void mouseExited(MouseEvent e) {
        mouseIn = false;
        mouseActivity();

        if ( threadContPress != null ) {
            threadContPress.stopNormaly ();
            threadContPress = null;
        }
    }

    public void mousePressed(MouseEvent e) {
        int modifier = e.getModifiers();
        if ((modifier & InputEvent.BUTTON2_MASK) == 0
                        &&  (modifier & InputEvent.BUTTON3_MASK) == 0 ) {
            mouseDown = true;
            mouseUp = false;
            mouseActivity();

            if ( boolContPress == true  ||  boolPopup == true ) {
                if ( threadContPress != null )
                    threadContPress.stopNormaly ();
                threadContPress = new ContPressThread ( this );
                if ( boolPopup == true )
                    threadContPress.setDelayedPress ( POPUP_DELAY );
                threadContPress.start ();
            }
            boolDoAction = true;
        }
    }

    public void mouseReleased(MouseEvent e) {
        int modifier = e.getModifiers();
        if ((modifier & InputEvent.BUTTON2_MASK) == 0
                        &&  (modifier & InputEvent.BUTTON3_MASK) == 0 ) {
            mouseUp = true;
            mouseActivity();
            mouseUp = false;
            mouseDown = false;

            if ( threadContPress != null ) {
                threadContPress.stopNormaly ();
                threadContPress = null;
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        int modifier = e.getModifiers();
        if ((modifier & InputEvent.BUTTON2_MASK) == 0
                        &&  (modifier & InputEvent.BUTTON3_MASK) == 0 ) {
            mouseClick = true;
            mouseActivity();
            mouseClick = false;
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    protected void processMouseEvent ( MouseEvent event ) {
        super.processMouseEvent ( event );

        if ( event.isPopupTrigger() ) {
            processMousePopup ();
        }
    }

    protected void processMousePopup () {
//      Dimension    dim;

        if ( menuPopup != null ) {
//          dim = this.getSize ();
            menuPopup.show ( this, 0, height );
        }
    }

    protected void processContPress () {
        if ( boolContPress == true )
            informListener ();
        else if ( boolPopup == true  &&  mouseIn  &&  mouseDown ) {
            boolDoAction = false;
            processMousePopup ();
        }
    }



    class ContPressThread extends Thread {

        protected ButtonComp    button = null;
        protected boolean       boolContinueRun = true;
        protected boolean       boolIgnoreFirst = true;

        protected boolean       boolDelayedPress = false;
        protected long          lMills = 500;

        public ContPressThread ( ButtonComp button ) {
            this.button = button;
        }

        public void setDelayedPress ( long lMills ) {
            boolDelayedPress = true;
            this.lMills = lMills;
        }

        public void stopNormaly () {
            boolContinueRun = false;
        }

        public void run () {
//            boolContinueRun = true;
            if ( boolDelayedPress == true )
                boolIgnoreFirst = false;
            else
                boolIgnoreFirst = true;

            while ( boolContinueRun ) {
                try {
                    sleep ( lMills );
                }
                catch ( Exception exception ) {
                }
                if ( button != null  &&  boolIgnoreFirst == false )
                    button.processContPress ();
                boolIgnoreFirst = false;
                if ( boolDelayedPress == true )
                    boolContinueRun = false;
            }

            boolDelayedPress = false;
            lMills = 250;
        }
    }
}


