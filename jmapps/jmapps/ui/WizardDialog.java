/*
 * @(#)WizardDialog.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import com.sun.media.util.JMFI18N;


public class WizardDialog extends JMDialog /*implements WindowListener, ActionListener*/ {

    public static final String		ACTION_FINISH = JMFI18N.getResource("jmstudio.wizard.finish");
    public static final String		ACTION_CANCEL = JMFI18N.getResource("jmstudio.wizard.cancel");
    public static final String		ACTION_NEXT = JMFI18N.getResource("jmstudio.wizard.next");
    public static final String		ACTION_BACK = JMFI18N.getResource("jmstudio.wizard.back");

    private String      strResultAction = ACTION_CANCEL;
    private String      strImage;

    private JMPanel     panelPages;
    private Button		buttonBack;
    private Button		buttonNext;
    private Button		buttonFinish;
    private Button		buttonCancel;

    private Vector		vectorPages = null;
    private Panel		panelCurPage = null;
    private CardLayout  layoutCard;

    protected Frame         frameOwner = null;


    public WizardDialog ( Frame frameOwner, String strTitle, boolean boolModal, String strImage ) {
    	this ( frameOwner, strTitle, boolModal, strImage, null );
    }

    public WizardDialog ( Frame frameOwner, String strTitle, boolean boolModal, String strImage, Panel arrPages[] ) {
    	super ( frameOwner, strTitle, boolModal );

        this.frameOwner = frameOwner;
        this.strImage = strImage;
    	try {
    	    init ();
    	}
    	catch ( Exception exception ) {
    	    exception.printStackTrace ();
    	}
    	setPages ( arrPages );
    }

    public String getAction () {
    	return ( strResultAction );
    }

    public Panel getCurrentPage () {
    	return ( panelCurPage );
    }

    public void setPages ( Panel arrPages[] ) {
    	int		i;
    	int		nCount;

    	if ( arrPages != null ) {
    	    panelCurPage = null;
    	    nCount = arrPages.length;
    	    vectorPages = new Vector ();
    	    for ( i = 0;  i < nCount;  i++ )
    	    	vectorPages.addElement ( arrPages[i] );
    	}

    	setNextPage ();
    }

    protected void init () throws Exception {
    	Panel		panel;
    	Panel		panelContent;
        ImageArea       imageArea;
        Image           image;

        this.setBackground ( Color.lightGray );
    	this.setLayout ( new BorderLayout(6,6) );
        this.setResizable ( false );
//    	this.addWindowListener ( this );

    	panelContent = new Panel ( new BorderLayout(6,6) );
        panelContent.setBackground ( Color.lightGray );
    	this.add ( panelContent, BorderLayout.CENTER );

        if ( strImage != null ) {
            panel = new Panel ( new BorderLayout() );
    	    panelContent.add ( panel, BorderLayout.WEST );

            image = ImageArea.loadImage ( strImage, this, true );
            imageArea = new ImageArea ( image );
            imageArea.setInsets ( 12, 12, 12, 12 );
    	    panel.add ( imageArea, BorderLayout.NORTH );
        }

    	layoutCard = new CardLayout ( 6, 6 );
    	panelPages = new JMPanel ( layoutCard );
        panelPages.setEmptyBorder ( 6, 6, 6, 6 );
    	panelContent.add ( panelPages, BorderLayout.CENTER );

    	panel = createPanelButtons ();
    	panelContent.add ( panel, BorderLayout.SOUTH );
    }

    private Panel createPanelButtons () {
    	Panel	panel;
    	Panel	panelButtons;

    	panelButtons = new Panel ( new FlowLayout(FlowLayout.RIGHT) );

    	panel = new Panel ( new GridLayout(1,0,6,6) );
    	panelButtons.add ( panel );

    	buttonBack = new Button ( ACTION_BACK );
    	buttonBack.addActionListener ( this );
    	panel.add ( buttonBack );

    	buttonNext = new Button ( ACTION_NEXT );
    	buttonNext.addActionListener ( this );
    	panel.add ( buttonNext );

    	buttonFinish = new Button ( ACTION_FINISH );
    	buttonFinish.addActionListener ( this );
    	panel.add ( buttonFinish );

    	buttonCancel = new Button ( ACTION_CANCEL );
    	buttonCancel.addActionListener ( this );
    	panel.add ( buttonCancel );

    	return ( panelButtons );
    }

    protected void setNextPage () {
    	Panel	panelPage;

    	if ( panelCurPage != null  &&  onPageDone(panelCurPage) == false )
            return;
    	panelPage = getNextPage ( panelCurPage );
    	setPage ( panelPage );
    }

    protected void setPrevPage () {
    	Panel	panelPage;

    	panelPage = getPrevPage ( panelCurPage );
    	setPage ( panelPage );
    }

    private void setPage ( Panel panelPage ) {
    	if ( panelPage == null )
    	    return;

    	panelCurPage = panelPage;
    	onPageActivate ( panelCurPage );

    	panelPages.add ( panelCurPage, panelCurPage.getName() );
    	layoutCard.show ( panelPages, panelCurPage.getName() );

    	if ( isFirstPage(panelCurPage) ) {
    	    buttonBack.setEnabled ( false );
    	    if ( getFocusOwner() == buttonBack )
    	    	buttonNext.requestFocus ();
    	}
    	else {
    	    buttonBack.setEnabled ( true );
    	}

    	if ( isLastPage(panelCurPage) ) {
    	    buttonNext.setEnabled ( false );
            buttonFinish.setEnabled ( true );
    	    if ( getFocusOwner() == buttonNext )
    	    	buttonFinish.requestFocus ();
    	}
    	else {
    	    buttonNext.setEnabled ( true );
            buttonFinish.setEnabled ( false );
    	}

    	this.validate ();
    	panelCurPage.validate ();
    }

    protected boolean onPageDone ( Panel panelPage ) {
        return ( true );
    }

    protected boolean onPageActivate ( Panel panelPage ) {
        return ( true );
    }

    protected boolean onFinish () {
        return ( true );
    }

    protected Panel getFirstPage () {
    	Panel	panelPage = null;

    	if ( vectorPages != null  &&  !vectorPages.isEmpty() ) {
    	    panelPage = (Panel) vectorPages.firstElement ();
    	}
    	return ( panelPage );
    }

    protected Panel getLastPage () {
    	Panel	panelPage = null;

    	if ( vectorPages != null  &&  !vectorPages.isEmpty() ) {
    	    panelPage = (Panel) vectorPages.lastElement ();
    	}
    	return ( panelPage );
    }

    protected Panel getNextPage ( Panel panelPage ) {
    	int		nIndex;
    	Panel	panelPageNext = null;

    	if ( panelPage == null ) {
    	    panelPageNext = getFirstPage ();
    	}
    	else if ( vectorPages != null  &&  !vectorPages.isEmpty() ) {
    	    nIndex = vectorPages.indexOf ( panelPage );
    	    if ( nIndex >= 0  &&  nIndex < vectorPages.size() - 1 )
    	    	panelPageNext = (Panel) vectorPages.elementAt ( nIndex + 1 );
    	}
    	return ( panelPageNext );
    }

    protected Panel getPrevPage ( Panel panelPage ) {
    	int		nIndex;
    	Panel	panelPagePrev = null;

    	if ( panelPage == null ) {
    	    panelPagePrev = getLastPage ();
    	}
    	else if ( vectorPages != null  &&  !vectorPages.isEmpty() ) {
    	    nIndex = vectorPages.indexOf ( panelPage );
    	    if ( nIndex > 0  &&  nIndex < vectorPages.size() )
    	    	panelPagePrev = (Panel) vectorPages.elementAt ( nIndex - 1 );
    	}
    	return ( panelPagePrev );
    }

    protected boolean isFirstPage ( Panel panelPage ) {
    	boolean		boolResult;

    	boolResult = (panelPage == getFirstPage());
    	return ( boolResult );
    }

    protected boolean isLastPage ( Panel panelPage ) {
    	boolean		boolResult;

    	boolResult = (panelPage == getLastPage());
    	return ( boolResult );
    }

    public void actionPerformed ( ActionEvent event ) {
    	String		strCmd;

    	strCmd = event.getActionCommand ();
    	if ( strCmd.equals(ACTION_FINISH)  ||  strCmd.equals(ACTION_CANCEL) ) {
    	    if ( strCmd.equals(ACTION_FINISH) ) {
    	    	if ( onPageDone(panelCurPage) == false )
                    return;
    	    	if ( onFinish() == false ) {
                    setPage ( getFirstPage() );
                    return;
                }
    	    }
    	    strResultAction = strCmd;
    	    this.dispose ();
    	}
    	else if ( strCmd.equals(ACTION_BACK) ) {
    	    setPrevPage ();
    	}
    	else if ( strCmd.equals(ACTION_NEXT) ) {
    	    setNextPage ();
    	}
    }

    public void windowOpened ( WindowEvent event ) {
    }

    public void windowClosing ( WindowEvent event ) {
    	this.dispose ();
    }

    public void windowClosed ( WindowEvent event ) {
    }

    public void windowIconified ( WindowEvent event ) {
    }

    public void windowDeiconified ( WindowEvent event ) {
    }

    public void windowActivated ( WindowEvent event ) {
    }

    public void windowDeactivated ( WindowEvent event ) {
    }


}


