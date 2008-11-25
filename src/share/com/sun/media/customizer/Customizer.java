/*
 * @(#)Customizer.java	1.24 02/12/17
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.File;

/**
 *  This is the main class of JMF Customizer, which controls the wizard GUI
 *  flow. It will lead the users to go through several pages to select their
 *  desired DataSource/protocol, media formats, codecs, renderers and 
 *  multiplexers. Finally, when the last page is reached and button customize
 *  is clicked, a dialog box will be popped up to report the progress of 
 *  customizing.
 *
 *  @version 2.0
 */

public class Customizer extends JFrame implements ActionListener, TokenDef {
    public static final String ACTION_FINISH = I18N.getResource("customizer.Customize");
    public static final String ACTION_CANCEL = I18N.getResource("customizer.Quit");
    public static final String ACTION_NEXT = I18N.getResource("customizer.Next");
    public static final String ACTION_BACK = I18N.getResource("customizer.Back");
    public static final String ACTION_HELP = I18N.getResource("customizer.Help");
    public static final int GENERAL = 0;
    public static final int PROTOCOL = 1;
    public static final int MFORMAT = 2;
    public static final int MUX = 3;
    public static final int CODEC = 4;
    public static final int RENDERER = 5;


    private String strResultAction = ACTION_CANCEL;

    //private JTabbedPane panelPages;
    private JPanel panelPages;
    private JButton buttonBack;
    private JButton buttonNext;
    private JButton buttonFinish;
    private JButton buttonCancel;
    private JButton buttonHelp;

    private Vector vectorPages = null;
    private JPanel panelCurPage = null;
    private JPanel oldPage = null;
    private boolean bypassMux = false;
    private boolean bypassMFormat = false;
    private boolean showHelp = false;
    private HelperDiag helpDlg = null;

    private boolean[] resultFunc, resultProtocol, resultMFormat, resultCodec,
                     resultRndr, resultMux;

    private int xx, yy, ww, hh;
    private CusRegistry theRegistry = null;
    int release = 0;

    public Customizer (String strTitle, int x, int y, int w, int h, CusRegistry theRegistry) {
	super(strTitle);
	this.theRegistry = theRegistry;
	init();
        createPages();
	xx = x;
	yy = y;
	ww = w;
	hh = h;
	this.setSize(ww,hh);
	this.setLocation(xx, yy);

	this.show();

    }

    public String getAction() {
	return strResultAction;
    }

    public JPanel getCurrentPage() {
	return panelCurPage;
    }

    public void createPages() {
        vectorPages = new Vector();
	vectorPages.addElement(new GeneralPane(this));
	vectorPages.addElement(new ProtocolPane());
        vectorPages.addElement(new MFormatPane());
	vectorPages.addElement(new MuxPane());
	vectorPages.addElement(new CodecPane());
	vectorPages.addElement(new RendererPane());
	
	oldPage = panelCurPage = null;
	setNextPage();
    }

    protected void init() {
	JPanel panel;
        JPanel panelContent;

	this.getContentPane().setBackground(Color.lightGray);
	this.getContentPane().setLayout(new BorderLayout(6,6));
	this.setResizable(false);
	this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0);}
        });

	panelContent = new JPanel ( new BorderLayout(8,8));
	panelContent.setBackground(Color.lightGray);
	this.getContentPane().add("Center", panelContent);
	panelContent.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

	panelPages = new JPanel(new BorderLayout());
	panelPages.setBorder(BorderFactory.createRaisedBevelBorder());
	// panelPages.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
	panelContent.add("Center", panelPages);

	panel = createPanelButtons();
	panelContent.add("South", panel);
    }

    private JPanel createPanelButtons() {
	JPanel panel;
	JPanel panelButtons;

	// outerPanel = new JPanel ( new BorderLayout());

	// panelButtons = new JPanel( new FlowLayout(FlowLayout.RIGHT));

	// panel = new JPanel( new GridLayout(1,0,0,0));
	panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	panel.setBackground(Color.lightGray);
	// panelButtons.add(panel);

	buttonHelp = new JButton(ACTION_HELP);
	buttonHelp.addActionListener(this);
	// panel.add(buttonHelp);

	buttonBack = new JButton(ACTION_BACK);
	buttonBack.addActionListener(this);
	panel.add(buttonBack);

	buttonNext = new JButton(ACTION_NEXT);
	buttonNext.addActionListener(this);
	panel.add(buttonNext);

	buttonFinish = new JButton (ACTION_FINISH);
	buttonFinish.addActionListener(this);
	panel.add(buttonFinish);

	buttonCancel = new JButton (ACTION_CANCEL);
	buttonCancel.addActionListener(this);
	panel.add(buttonCancel);
	panel.add(buttonHelp);

	return panel;
	// return panelButtons;
    }

    private void setNextPage() {
	JPanel panelPage;
	
	if ( panelCurPage != null && !onPageDone(panelCurPage) )
	    return;

	panelPage = getNextPage(panelCurPage);
	setPage(panelPage);
    }

    private void setPrevPage() {
	JPanel panelPage;
	
	panelPage = getPrevPage(panelCurPage);
	setPage (panelPage);
    }

    private void setPage(JPanel panelPage) {
	if (panelPage == null) 
	    return;
	
	oldPage = panelCurPage;

	panelCurPage = panelPage;
	onPageActivate(panelCurPage);

	/*
	panelPages.add(panelCurPage, panelCurPage.getName());
	panelPages.setSelectedComponent(panelCurPage);
	*/
	
	if ( oldPage != null){
	    //System.out.println("do remove page");
	    panelPages.remove(oldPage);
	    // oldPage.removeNotify();
	}

	// System.out.println("panelCurPage = " + panelCurPage);
	panelPages.add("Center", panelCurPage);

	panelPages.validate();
	panelPage.repaint();
	
	if (showHelp) {
	    int index = vectorPages.indexOf(panelCurPage);
	    helpDlg.setHelpPage(index);
	}

	if ( isFirstPage(panelCurPage)) {
	    buttonBack.setEnabled(false);
	    if (getFocusOwner() == buttonBack)
		buttonNext.requestFocus();
	} else {
	    buttonBack.setEnabled(true);
	}

	if ( isLastPage(panelCurPage)) {
	    buttonNext.setEnabled(false);
	    buttonFinish.setEnabled(true);
	    if ( getFocusOwner() == buttonNext )
		buttonFinish.requestFocus();
	} else {
	    buttonNext.setEnabled (true);
	    buttonFinish.setEnabled(false);
	}

	this.validate();
	// panelPages.validate();
	// panelCurPage.validate();
    }

    protected boolean onPageDone (JPanel panelPage) {
	if ( panelPage instanceof GeneralPane) {
	    resultFunc = ((GeneralPane)vectorPages.elementAt(GENERAL)).getState();
	    if ( ! ((GeneralPane)panelPage).checking(resultFunc, this)) {
		release =((GeneralPane)panelPage).getRelease(); 
		return false;
	    }
	    release =((GeneralPane)panelPage).getRelease();
	} else if ( panelPage instanceof ProtocolPane) {
	    resultProtocol = ((ProtocolPane)vectorPages.elementAt(PROTOCOL)).getState();
	    resultProtocol[ProtocolPane.RTP] |= resultProtocol[ProtocolPane.RTSP];
	} else if ( panelPage instanceof MFormatPane) {
	    resultMFormat = ((MFormatPane)vectorPages.elementAt(MFORMAT)).getState();
	} else if ( panelPage instanceof CodecPane) {
	    resultCodec= ((CodecPane)vectorPages.elementAt(CODEC)).getState();
	} else if ( panelPage instanceof RendererPane) {
	    resultRndr = ((RendererPane)vectorPages.elementAt(RENDERER)).getState();
	} else if ( panelPage instanceof MuxPane) {
	    resultMux = ((MuxPane)vectorPages.elementAt(MUX)).getState();
	}
	
	return true;
    }

    protected boolean onPageActivate( JPanel panelPage) {
	if ( panelPage instanceof ProtocolPane) {
	    ((ProtocolPane)panelPage).setHighlight(resultFunc, release);
	} else if ( panelPage instanceof MFormatPane) {
	    ((MFormatPane)panelPage).setHighlight(resultFunc, resultProtocol, release);
	} else if ( panelPage instanceof CodecPane) {
	    resultFunc = ((GeneralPane)vectorPages.elementAt(GENERAL)).getState();
	    resultMFormat = ((MFormatPane)vectorPages.elementAt(MFORMAT)).getState();
	    resultMux = ((MuxPane)vectorPages.elementAt(MUX)).getState();	    
	    ((CodecPane)panelPage).setHighlight(resultFunc, resultProtocol, resultMFormat, resultMux, release);
	} else if ( panelPage instanceof RendererPane) {
	    ((RendererPane)panelPage).enableAll(release);
	} else if ( panelPage instanceof MuxPane) {
	    ((MuxPane)panelPage).enableAll();
	}

	return true;
    }

    protected int[] figureoutSelected() {
	
	resultFunc = ((GeneralPane)vectorPages.elementAt(GENERAL)).getState();
	resultProtocol = ((ProtocolPane)vectorPages.elementAt(PROTOCOL)).getState();
	resultProtocol[ProtocolPane.RTP] |= resultProtocol[ProtocolPane.RTSP];
	resultMFormat = ((MFormatPane)vectorPages.elementAt(MFORMAT)).getState();
	resultCodec= ((CodecPane)vectorPages.elementAt(CODEC)).getState();
	resultRndr = ((RendererPane)vectorPages.elementAt(RENDERER)).getState();
	resultMux = ((MuxPane)vectorPages.elementAt(MUX)).getState();

	int[] result = new int[192];
	int p = 0;
	boolean sinkfile = false;

	// main function
	if (resultFunc[GeneralPane.GUI]) 
	    result[p++] = GUI;
	if (resultFunc[GeneralPane.STUDIO]) {
	    result[p++] = STUDIO;
	    result[p++] = PLYRBEAN;
	    result[p++] = MPLYR;
	}

	if ( resultFunc[GeneralPane.BEAN] ){
	    result[p++] = PLYRBEAN;
	    result[p++] = MPLYR;
	}
	
	if ( resultFunc[GeneralPane.RTPREC] ||
	     resultFunc[GeneralPane.CAPTURE] ||
	     resultFunc[GeneralPane.RTPTRANS] ||
	     resultFunc[GeneralPane.TRANSCODE] )
	    result[p++] = NOTPB;

	if (resultFunc[GeneralPane.RTPTRANS] && resultProtocol[ProtocolPane.RTP])
	    result[p++] = SINKRTP;
	
	// wpp or spp?
	//if ( release == 2 )
	//   result[p++] = SPP;

	//if ( release == 3)
	//  result[p++] = WPP;

	// protocol
	boolean vcb1 = false;
	boolean acb1 = false;

	if (resultProtocol[ProtocolPane.FILE])
	    result[p++] = DSFILE;

	if (resultProtocol[ProtocolPane.HTTP])
	    result[p++] = DSHTTP;

	if (resultProtocol[ProtocolPane.HTTPS])
	    result[p++] = DSHTTPS;

	if (resultProtocol[ProtocolPane.FTP])
	    result[p++] = DSFTP;

	if (resultProtocol[ProtocolPane.JAVASOUND]) {
	    result[p++] = DSJS;
	    acb1 = true;
	}

	if (resultProtocol[ProtocolPane.RTP])
	    result[p++] = RTP;

	if (resultProtocol[ProtocolPane.RTSP])
	    result[p++] = RTSP;
	
	if (resultProtocol[ProtocolPane.VFW]) {
	    result[p++] = VFW;
	    result[p++] = VCMND;
	    vcb1 = true;
	}

	if (resultProtocol[ProtocolPane.DSOUND]) {
	    result[p++] = DSOUND;
	    acb1 = true;
	}

	if (resultProtocol[ProtocolPane.SVDO]) {
	    result[p++] = SVDO;
	    vcb1 = true;
	}

	if (resultProtocol[ProtocolPane.SVDOPLS]) {
	    result[p++] = SVDOPLS;
	    vcb1 = true;
	}

	if (resultProtocol[ProtocolPane.CLNMGDS] ||
	    ( vcb1 && acb1) )
	    result[p++] = CLNMGDS;

	// media format -- parser
	if ( resultMFormat[MFormatPane.AU]) 
	    result[p++] = AUPSR;

	if ( resultMFormat[MFormatPane.AIFF]) 
	    result[p++] = AIFFPSR;

	if ( resultMFormat[MFormatPane.GSM]) 
	    result[p++] = GSMPSR;

	if ( resultMFormat[MFormatPane.MP2] || resultMFormat[MFormatPane.MP3]) 
	    result[p++] = MPEGPSR;

	if ( resultMFormat[MFormatPane.WAV]) 
	    result[p++] = WAVPSR;

	if ( resultMFormat[MFormatPane.MOV]) 
	    result[p++] = QTPSR;

	if ( resultMFormat[MFormatPane.AVI]) 
	    result[p++] = AVIPSR;

	if ( resultMFormat[MFormatPane.MPEG]) 
	    result[p++] = MPEGPSR;

	if ( resultMFormat[MFormatPane.MVR]) 
	    result[p++] = MVR;

	if ( resultMFormat[MFormatPane.MIDI]) 
	    result[p++] = MIDI;

	if ( resultMFormat[MFormatPane.RMF]) 
	    result[p++] = RMF;
	
	if (resultMFormat[MFormatPane.CDAUDIO])
	    result[p++] = CDAUDIO;

	// codec Decoders
	if ( resultCodec[CodecPane.MPAJD] )
	    result[p++] = MPAJD;
	if ( resultCodec[CodecPane.ULAWJD] || resultRndr[RendererPane.SUNAUDIO])
	    result[p++] = ULAWJD;
	if ( resultCodec[CodecPane.MSGSMJD] )
	    result[p++] = MSGSMJD;
	if ( resultCodec[CodecPane.GSMJD] )
	    result[p++] = GSMJD;
	if ( resultCodec[CodecPane.ALAWJD] )
	    result[p++] = ALAWJD;
	if ( resultCodec[CodecPane.MSIMA4JD] )
	    result[p++] = MSIMA4JD;
	if ( resultCodec[CodecPane.IMA4JD] )
	    result[p++] = IMA4JD;
	if ( resultCodec[CodecPane.DVIJD] )
	    result[p++] = DVIJD;
	if ( resultCodec[CodecPane.G723JD] )
	    result[p++] = G723JD;
	if ( resultCodec[CodecPane.MSADPCMJD] )
	    result[p++] = MSADPCMJD;
	if ( resultCodec[CodecPane.MPAND] )
	    result[p++] = MPAND;
	if ( resultCodec[CodecPane.MSGSMND] )
	    result[p++] = MSGSMND;
	if ( resultCodec[CodecPane.GSMND] )
	    result[p++] = GSMND;
	if ( resultCodec[CodecPane.G723ND] )
	    result[p++] = G723ND;
	if ( resultCodec[CodecPane.ACMND] )
	    result[p++] = ACM;
	if ( resultCodec[CodecPane.CINEPAKJD] )
	    result[p++] = CINEPAKJD;
	if ( resultCodec[CodecPane.H263JD] )
	    result[p++] = H263JD;
	if ( resultCodec[CodecPane.CINEPAKND] )
	    result[p++] = CINEPAKND;
	if ( resultCodec[CodecPane.H263ND] )
	    result[p++] = H263ND;
	if ( resultCodec[CodecPane.H261ND] )
	    result[p++] = H261ND;
	if ( resultCodec[CodecPane.JPEGND] )
	    result[p++] = JPEGND;
	if ( resultCodec[CodecPane.MPEGND] )
	    result[p++] = MPEGND;
	if ( resultCodec[CodecPane.IV32ND] && release == 3 ) {
	    result[p++] = IV32ND;
	    result[p++] = VCMND;
	}
	if ( resultCodec[CodecPane.MPEGPLY] && release == 2 ) {
	    result[p++] = SMPEGPLY;
	    // jpmx always use javasound renderer
	    result[p++] = JSRENDER;
	}

	if ( resultCodec[CodecPane.MPEGPLY] && release == 3 )
	    result[p++] = WMPEGPLY;
	if ( resultCodec[CodecPane.VCMND] )
	    result[p++] = VCMND;

	// codec Encoders
	if ( resultCodec[CodecPane.ULAWJE] || resultRndr[RendererPane.SUNAUDIO] )
	    result[p++] = ULAWJE;
	if ( resultCodec[CodecPane.MSGSMJE] )
	    result[p++] = MSGSMJE;
	if ( resultCodec[CodecPane.GSMJE] )
	    result[p++] = GSMJE;
	if ( resultCodec[CodecPane.MSIMA4JE] )
	    result[p++] = MSIMA4JE;
	if ( resultCodec[CodecPane.IMA4JE] )
	    result[p++] = IMA4JE;
	if ( resultCodec[CodecPane.DVIJE] )
	    result[p++] = DVIJE;
	if ( resultCodec[CodecPane.MPANE] )
	    result[p++] = MPANE;
	if ( resultCodec[CodecPane.MSGSMNE] )
	    result[p++] = MSGSMNE;
	if ( resultCodec[CodecPane.GSMNE] )
	    result[p++] = GSMNE;
	if ( resultCodec[CodecPane.G723NE] )
	    result[p++] = G723NE;
	if ( resultCodec[CodecPane.ACMNE] )
	    result[p++] = ACM;
	if ( resultCodec[CodecPane.CINEPAKPRONE] )
	    result[p++] = CINEPAKPRONE;
	if ( resultCodec[CodecPane.H263NE] )
	    result[p++] = H263NE;
	if ( resultCodec[CodecPane.JPEGNE] )
	    result[p++] = JPEGNE;
	if ( resultCodec[CodecPane.VCMNE] )
	    result[p++] = VCMNE;

	// codec depaketizer
	if ( resultCodec[CodecPane.MPADRTP] )
	    result[p++] = MPADRTP;
	if ( resultCodec[CodecPane.ULAWDRTP] )
	    result[p++] = ULAWDRTP;
	if ( resultCodec[CodecPane.GSMDRTP] )
	    result[p++] = GSMDRTP;
	if ( resultCodec[CodecPane.DVIDRTP] )
	    result[p++] = DVIDRTP;
	if ( resultCodec[CodecPane.G723DRTP] )
	    result[p++] = G723DRTP;
	if ( resultCodec[CodecPane.H263DRTP] )
	    result[p++] = H263DRTP;
	if ( resultCodec[CodecPane.H261DRTP] )
	    result[p++] = H261DRTP;
	if ( resultCodec[CodecPane.JPEGDRTP] )
	    result[p++] = JPEGDRTP;
	if ( resultCodec[CodecPane.MPEGDRTP] )
	    result[p++] = MPEGDRTP;

	// codec packetizer
	if ( resultCodec[CodecPane.MPAPRTP] )
	    result[p++] = MPAPRTP;
	if ( resultCodec[CodecPane.ULAWPRTP] )
	    result[p++] = ULAWPRTP;
	if ( resultCodec[CodecPane.GSMPRTP] )
	    result[p++] = GSMPRTP;
	if ( resultCodec[CodecPane.DVIPRTP] )
	    result[p++] = DVIPRTP;
	if ( resultCodec[CodecPane.G723PRTP] )
	    result[p++] = G723PRTP;
	if ( resultCodec[CodecPane.H263PRTP] )
	    result[p++] = H263PRTP;
	if ( resultCodec[CodecPane.JPEGPRTP] )
	    result[p++] = JPEGPRTP;
	if ( resultCodec[CodecPane.MPEGPRTP] )
	    result[p++] = MPEGPRTP;

	// renderer
	if ( resultRndr[RendererPane.SUNAUDIO])
	    result[p++] = SUNARENDER;

	if ( resultRndr[RendererPane.JAVASOUND])
	    result[p++] = JSRENDER;

	if ( resultRndr[RendererPane.DAUDIO])
	    result[p++] = DARENDER;

	if ( resultRndr[RendererPane.AWT])
	    result[p++] = AWTRNDR;

	if ( resultRndr[RendererPane.JPEG])
	    result[p++] = JPEGRNDR;

	if ( resultRndr[RendererPane.XLIB])
	    result[p++] = XLIBRNDR;

	if ( resultRndr[RendererPane.XIL])
	    result[p++] = XILRNDR;

	if ( resultRndr[RendererPane.SUNRAY])
	    result[p++] = SUNRAYRNDR;

	if ( resultRndr[RendererPane.DDRAW])
	    result[p++] = DDRNDR;

	if ( resultRndr[RendererPane.GDI])
	    result[p++] = GDIRNDR;

	// mux
	if (resultMux[MuxPane.GSM]) {
	    sinkfile = true;
	    result[p++] = GSMMUX;
	}

	if (resultMux[MuxPane.MP2] || resultMux[MuxPane.MP3]){ 
	    sinkfile = true;
	    result[p++] = MPEGMUX;
	}

	if (resultMux[MuxPane.WAV]) {
	    sinkfile = true;
	    result[p++] = WAVMUX;
	}

	if (resultMux[MuxPane.AIFF]) {
	    sinkfile = true;
	    result[p++] = AIFFMUX;
	}

	if (resultMux[MuxPane.AU]) {
	    sinkfile = true;
	    result[p++] = AUMUX;
	}

	if (resultMux[MuxPane.MOV]) {
	    sinkfile = true;
	    result[p++] = QTMUX;
	}

	if (resultMux[MuxPane.AVI]) {
	    sinkfile = true;
	    result[p++] = AVIMUX;
	}

	// transcode
	if (sinkfile && resultFunc[GeneralPane.TRANSCODE])
	    result[p++] = SINKFILE;

	
	// final params to ProcessJAR
	int[] selected = new int[p];

	for ( int i = 0; i < p; i++) {
	    selected[i] = result[i];
	}
	
	return (selected);
    }
    
    protected boolean onFinish() {
	int[] selected;
	String srcJARName, dstJARName;
	boolean twojars = false;

	selected = figureoutSelected();
	srcJARName = ((GeneralPane)vectorPages.elementAt(GENERAL)).getSrcJARName();
	dstJARName = ((GeneralPane)vectorPages.elementAt(GENERAL)).getDstJARName();
	twojars = ((GeneralPane)vectorPages.elementAt(GENERAL)).genTwoJars();

	if ( !twojars ) {
	    File dstFileHandle = new File(dstJARName);
	    if (dstFileHandle.exists() ) {
		int answer =  JOptionPane.showConfirmDialog(this, I18N.getResource("customizer.DlgQuestion"), I18N.getResource("customizer.DlgTitle"), JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE );
		
		if (answer == JOptionPane.NO_OPTION)
		    return false;
	    }
	} else {
	    String pathname, filename;
	    String dst1Name, dst2Name;
	    File dstHandle = new File(dstJARName);
	    pathname = dstHandle.getParent() + File.separator;
	    filename = dstHandle.getName();
	    dst1Name = pathname + "core_" + filename;
	    dst2Name = pathname + "plugin_" + filename;
	    //System.out.println("1 name = " + dst1Name);
	    //System.out.println("2 name = " + dst2Name);

	    File dstHandle1 = new File(dst1Name);
	    File dstHandle2 = new File(dst2Name);

	    if ( dstHandle1.exists() || dstHandle2.exists() ) {
		int answer =  JOptionPane.showConfirmDialog(this, I18N.getResource("customizer.DlgQuestion"), I18N.getResource("customizer.DlgTitle"), JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE );
		
		if (answer == JOptionPane.NO_OPTION)
		    return false;
	    }
	}

	    
	ProgressDiag pd = new ProgressDiag(this, srcJARName, dstJARName, twojars);
	ProcessJAR processjar = new ProcessJAR(srcJARName, dstJARName, selected, pd, theRegistry, release, twojars);
	processjar.start();
	pd.show();

	return true;
    }

    protected JPanel getFirstPage() {
	JPanel panelPage = null;

    	if ( vectorPages != null  &&  !vectorPages.isEmpty() ) {
    	    panelPage = (JPanel) vectorPages.firstElement();
    	}
    	return ( panelPage );
    }

    protected JPanel getLastPage () {
    	JPanel	panelPage = null;

    	if ( vectorPages != null  &&  !vectorPages.isEmpty() ) {
    	    panelPage = (JPanel) vectorPages.lastElement();
    	}
    	return ( panelPage );
    }

    protected JPanel getNextPage ( JPanel panelPage ) {
    	int		nIndex;
    	JPanel	panelPageNext = null;

    	if ( panelPage == null ) {
    	    panelPageNext = getFirstPage ();
    	}
    	else if ( vectorPages != null  &&  !vectorPages.isEmpty() ) {

	    if ( panelPage instanceof ProtocolPane) {
		resultFunc = ((GeneralPane)vectorPages.elementAt(GENERAL)).getState();
		// don't want to load media from stored files
		if ( !resultFunc[GeneralPane.MFILE]) {
		    ((MFormatPane)vectorPages.elementAt(MFORMAT)).disableAll();
		    bypassMFormat = true;

		    // don't want to transcode
		    if (!resultFunc[GeneralPane.TRANSCODE]) {
			bypassMux = true;
			panelPageNext = (CodecPane)vectorPages.elementAt(CODEC);
		    } else {
			panelPageNext = (MuxPane)vectorPages.elementAt(MUX);
		    }
		    return (panelPageNext);
		}

	    }

	    if ( panelPage instanceof MFormatPane) {
		resultFunc = ((GeneralPane)vectorPages.elementAt(GENERAL)).getState();
		// don't want to transcode
		if ( !resultFunc[GeneralPane.TRANSCODE]) {
		    ((MuxPane)vectorPages.elementAt(MUX)).disableAll();
		    panelPageNext = (CodecPane)vectorPages.elementAt(CODEC);
		    bypassMux = true;
		    return (panelPageNext);
		}

	    }


    	    nIndex = vectorPages.indexOf ( panelPage );
    	    if ( nIndex >= 0  &&  nIndex < vectorPages.size() - 1 )
    	    	panelPageNext = (JPanel) vectorPages.elementAt( nIndex + 1 );
    	}
    	return ( panelPageNext );
    }

    protected JPanel getPrevPage ( JPanel panelPage ) {
    	int		nIndex;
    	JPanel	panelPagePrev = null;

    	if ( panelPage == null ) {
    	    panelPagePrev = getLastPage ();
    	} 
    	else if ( vectorPages != null  &&  !vectorPages.isEmpty() ) {
    	    nIndex = vectorPages.indexOf ( panelPage );

	    switch (nIndex) {
	    case MUX:
		if (bypassMFormat) {
		    panelPagePrev = (JPanel) vectorPages.elementAt(PROTOCOL);
		    bypassMFormat = false;
		} else {
		    panelPagePrev = (JPanel) vectorPages.elementAt(MFORMAT);  
		}
		break;

	    case CODEC:
		if ( bypassMFormat && bypassMux ) {
		    bypassMFormat = bypassMux = false;
		    panelPagePrev = (JPanel) vectorPages.elementAt(PROTOCOL);
		} else {
		    if ( bypassMux && !bypassMFormat) {
			bypassMux = false;
			panelPagePrev = (JPanel) vectorPages.elementAt(MFORMAT);
		    } else {
			// if ( bypassMFormat && !bypassMux) {
			// bypassMFormat = false;
			panelPagePrev = (JPanel) vectorPages.elementAt(MUX);
		    }
		}
		break;

	    case GENERAL:
		panelPagePrev = panelPage;
		break;
		
	    case PROTOCOL:
	    case MFORMAT:
	    case RENDERER:
		panelPagePrev = (JPanel) vectorPages.elementAt( nIndex - 1 );
		break;
	    }

		    /*
	    if ((nIndex == vectorPages.size()-1) && bypassRender) {
		panelPagePrev = (JPanel) vectorPages.elementAt(CODEC);
		bypassRender = false;
	    } else if ( bypassMFormat && (panelPage instanceof CodecPane)) {
		panelPagePrev = (JPanel) vectorPages.elementAt(PROTOCOL);
		bypassMFormat = false;
	    } else if ( nIndex > 0  &&  nIndex < vectorPages.size()) {
    	    	panelPagePrev = (JPanel) vectorPages.elementAt( nIndex - 1 );
	    }
	    */
    	}
    	return ( panelPagePrev ); 
    }

    protected boolean isFirstPage ( JPanel panelPage ) {
    	boolean		boolResult;

    	boolResult = (panelPage == getFirstPage());
    	return ( boolResult );
    }

    protected boolean isLastPage ( JPanel panelPage ) {
    	boolean		boolResult;
	
	// if don't want play, then codec is the last page
	if ( panelPage instanceof CodecPane) {
	  resultFunc = ((GeneralPane)vectorPages.elementAt(GENERAL)).getState();
	  if (!resultFunc[GeneralPane.PLAY]){
              ((RendererPane)vectorPages.elementAt(RENDERER)).disableAll();
	      return true;
	  }
	}

    	boolResult = (panelPage == getLastPage());
    	return ( boolResult );
    }

    public void actionPerformed ( ActionEvent event ) {
    	String		strCmd;

    	strCmd = event.getActionCommand ();
	if ( strCmd.equals(ACTION_CANCEL)) {
	    System.exit(0);
	} else if (strCmd.equals(ACTION_FINISH)) {
	    if ( onPageDone(panelCurPage) == false )
		return;
	    onFinish();
	    setPage(getFirstPage());
	    return;
	} else if ( strCmd.equals(ACTION_BACK) ) {
    	    setPrevPage ();
    	} else if ( strCmd.equals(ACTION_NEXT) ) {
    	    setNextPage ();
    	} else if ( strCmd.equals(ACTION_HELP) ) {
	    showHelp = true;
	    buttonHelp.setEnabled(false);
	    int idx = vectorPages.indexOf ( panelCurPage );
	    if (idx < 0 || idx > 5 )
		idx = 0;
	    helpDlg = new HelperDiag(this, idx);
	    helpDlg.setLocation(50,50);
	    helpDlg.setSize(320,400);
	    helpDlg.setResizable(true);
	    helpDlg.show();
	}
	    
    }

    public void dismissHelp() {
	showHelp = false;
	buttonHelp.setEnabled(true);
    }


}





	
       


			       
	
	
	
	

	

    


    
