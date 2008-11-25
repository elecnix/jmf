/*
 * @(#)SunVideoPlusManual.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import javax.media.*;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import javax.media.format.AudioFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import com.sun.media.protocol.sunvideoplus.*;

public class SunVideoPlusManual extends Frame {

    private static String DEVICE_PREFIX = "/dev/o1k";
    CaptureDeviceInfo [] devices = null;
    int currentID = -1;
    
    public SunVideoPlusManual() {
	super("SunVideoPlus");

	Font lblFont = new Font("Dialog", Font.BOLD + Font.ITALIC, 12);
	setLayout(new BorderLayout());
	BorderPanel bp = new BorderPanel();
	bp.setLayout(new BorderLayout(0, 0));

	Panel p = new Panel();
	p.setLayout(new FlowLayout(FlowLayout.LEFT));
	Label lbl = new Label("Configure Capture Devices");
	lbl.setFont(lblFont);
	p.add(lbl);
	bp.add(BorderLayout.NORTH, p);

	p = new Panel();
	p.setLayout(new FlowLayout());

	int nDevices = 0;
	for (int i = 0; i < 10; i++) {
	    File fl = new File(DEVICE_PREFIX + i);
	    if (fl.exists()) {
		Button device = new Button(DEVICE_PREFIX + i);
		p.add(device);
		device.addActionListener( new DBListener(i) );
		nDevices++;
	    }
	}
	bp.add(BorderLayout.CENTER, p);
	add(BorderLayout.NORTH, bp);

	devices = new CaptureDeviceInfo[nDevices];

	p = new Panel();
	p.setLayout(new FlowLayout());
	Button bDone = new Button("Done");
	bDone.addActionListener( new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		if (currentID != -1) {
		    System.err.println("Need to close other window first");
		    return;
		}
		dispose();
		System.exit(0);
	    }
	} );
	p.add(bDone);
	add(BorderLayout.SOUTH, p);

	pack();
	addWindowListener( new WindowAdapter() {
	    public void windowClosing(WindowEvent we) {
		if (currentID != -1) {
		    System.err.println("Need to close other window first");
		    return;
		}
		dispose();
		System.exit(0);
	    }
	} );
    }

    public void createDialog(int id) {
	if (currentID == id)
	    return;
	currentID = id;
	Thread t = new Thread( new Runnable() {
	    public void run() {
		FormatsDialog fd = new FormatsDialog(currentID);
		fd.setVisible(true);
		Vector cdiv = fd.getDeviceInfo();
		if (cdiv != null && cdiv.size() > 0) {
		    for (int i = 0; i < cdiv.size(); i++) {
			CaptureDeviceInfo cdi = 
					(CaptureDeviceInfo) cdiv.elementAt(i);
			// At the moment, the name and locator are identical
			System.err.println("CaptureDeviceInfo = "
						+ cdi.getName());
//			System.err.println("CaptureDeviceInfo = "
//						+ cdi.getName() + " "
//						+ cdi.getLocator());
		    }
		}
		
		currentID = -1;
	    }
	} );
	t.start();
	// Create a CaptureDeviceInfo....
    }

    class FormatsDialog extends Frame {

	int id;

	Button bFormat, bCancel;
	CheckboxGroup cAnalogGroup;
	Checkbox cNTSC, cPAL;
	CheckboxGroup cPortGroup;
	Checkbox cPort1, cPort2, cSvhs;
	Checkbox cH261, cH263, cJpeg, cRGB, cYUV;
	Checkbox cFull, cCif, cQuarter;

	boolean fullVideo = false;
	boolean anyVideo = true;

	String sAnalog, sPort, sVideoFormat, sSize;

	Hashtable videoFormats = new Hashtable();

	OPICapture opiVidCap = null;

	Integer lock = new Integer(1);
	boolean finished = false;
	boolean needFormat = false;
	
	public FormatsDialog(int id) {
	    super("sunvideoplus:" + DEVICE_PREFIX + id);
	    this.id = id;
	    setLayout(new BorderLayout());
	    cAnalogGroup = new CheckboxGroup();
	    cNTSC = new Checkbox("NTSC", true, cAnalogGroup);
	    cPAL = new Checkbox("PAL", false, cAnalogGroup);
	    cPortGroup = new CheckboxGroup();
	    cPort1 = new Checkbox("1", true, cPortGroup);
	    cPort2 = new Checkbox("2", false, cPortGroup);
	    cSvhs = new Checkbox("S-VHS", false, cPortGroup);

	    ItemListener videoListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    fullVideo = (cRGB.getState() | cYUV.getState());
		    anyVideo = (cH261.getState() | cH263.getState()
				| cJpeg.getState() | fullVideo);
		    cFull.setEnabled(fullVideo);
		    cCif.setEnabled(anyVideo);
		    cQuarter.setEnabled(anyVideo);
		}
	    };

	    cH261 = new Checkbox("h.261", false);
	    cH261.addItemListener(videoListener);
	    cH263 = new Checkbox("h.263", false);
	    cH263.addItemListener(videoListener);
	    cJpeg = new Checkbox("Jpeg", true);
	    cJpeg.addItemListener(videoListener);
	    cRGB = new Checkbox("RGB", false);
	    cRGB.addItemListener(videoListener);
	    cYUV = new Checkbox("YUV", false);
	    cYUV.addItemListener(videoListener);
	    cFull = new Checkbox("full cif", false);
	    cFull.setEnabled(false);
	    cCif = new Checkbox("cif", true);
	    cQuarter = new Checkbox("quarter cif", false);


	    bFormat = new Button("Add Formats");
	    bCancel = new Button("Cancel");
	    
	    opiVidCap = new OPICapture(null);
	    if (!opiVidCap.connect(id)) {
		throw new Error("Unable to connect to device");
	    }

	    Font lblFont = new Font("Dialog", Font.BOLD + Font.ITALIC, 12);

	    BorderPanel bp = new BorderPanel();
	    bp.setLayout(new BorderLayout(0, 0));

	    Panel p = new Panel();
	    p.setLayout(new FlowLayout(FlowLayout.LEFT));
	    Label lbl = new Label("Video Input Formats");
	    lbl.setFont(lblFont);
	    p.add(lbl);
	    bp.add(BorderLayout.NORTH, p);

	    Panel cp = new Panel();
	    cp.setLayout(new BorderLayout());

	    p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(new Label("Analog"));
	    p.add(cNTSC);
	    p.add(cPAL);
	    cp.add(BorderLayout.NORTH, p);

	    p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(new Label("Port"));
	    p.add(cPort1);
	    p.add(cPort2);
	    p.add(cSvhs);
	    cp.add(BorderLayout.SOUTH, p);
	    bp.add(BorderLayout.CENTER, cp);
	    
	    cp = new Panel();
	    cp.setLayout(new BorderLayout());

	    p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(new Label("Formats"));
	    p.add(cH261);
	    p.add(cH263);
	    p.add(cJpeg);
	    p.add(cRGB);
	    p.add(cYUV);
	    cp.add(BorderLayout.CENTER, p);

	    p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(new Label("Sizes"));
	    p.add(cFull);
	    p.add(cCif);
	    p.add(cQuarter);
	    cp.add(BorderLayout.SOUTH, p);
	    bp.add(BorderLayout.SOUTH, cp);
	    add(BorderLayout.NORTH, bp);

	    p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(bFormat);
	    p.add(bCancel);
	    add(BorderLayout.SOUTH, p);
	    pack();

	    addWindowListener( new WindowAdapter() {
		public void windowClosing(WindowEvent we) {
		    synchronized (lock) {
			finished = true;
			lock.notify();
		    }
		}
	    } );

	    bCancel.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    synchronized (lock) {
			finished = true;
			lock.notify();
		    }
		}
	    } );

	    bFormat.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    needFormat = true;
		    synchronized (lock) {
			lock.notify();
		    }
		}
	    } );
	}

	private void addVideoFormat(Format fin) {
	    String sVideo = sPort + "/" + sVideoFormat + "/"
					+ sSize + "/"
					+ sAnalog;
	    System.err.println("New format " + sVideo + " = " + fin);
	    videoFormats.put(sVideo, fin);
	}

	public void mydispose() {
	    opiVidCap.disconnect();
	    dispose();
	    System.err.println("Disconnected driver");
	}
	
	public void doFormat() {
	    if (!checkVideoFormats())
		return;

	    if (anyVideo) {
		doVideoFormats();
	    }
	    finished = true;
	}

	public boolean checkVideoFormats() {
	    if (!anyVideo)
		return true;
	    if (cCif.getState() | cQuarter.getState() |
				    (cFull.getState() && cFull.isEnabled()))
		return true;
	    // Issue error popup dialog
	    issueError("Must specify at least one Video size");
	    return false;
	}

	public void doVideoFormats() {
	    if (!anyVideo) {
		// add a dummy format entry
		videoFormats.put("off", new VideoFormat(VideoFormat.RGB));
	    }

	    Checkbox cAnalog = cAnalogGroup.getSelectedCheckbox();
	    sAnalog = "";
	    if (cAnalog == cNTSC) {
		sAnalog = "ntsc";
	    } else if (cAnalog == cPAL) {
		sAnalog = "pal";
	    } else {
		System.err.println("No analog signal specified");
		return;
	    }
	    if (!opiVidCap.setSignal(sAnalog)) {
		System.err.println("Video analog signal not recognized");
		return;
	    }
	    Checkbox cPort = cPortGroup.getSelectedCheckbox();
	    int port = 1;
	    if (cPort == cPort1) {
		port = 1;
	    } else if (cPort == cPort2) {
		port = 2;
	    } else if (cPort == cSvhs) {
		port = 0;
	    } else {
		System.err.println("No port specified");
		return;
	    }
	    if (!opiVidCap.setPort(port)) {
		System.err.println("Video source not recognized on port");
		return;
	    }
	    sPort = "" + port;
	    if (cCif.getState()) {
		opiVidCap.setScale(2);
		sSize = "cif";
		getVideoFormats();
	    }
	    if (cQuarter.getState()) {
		opiVidCap.setScale(4);
		sSize = "qcif";
		getVideoFormats();
	    }
	    if (cFull.getState() && cFull.isEnabled()) {
		opiVidCap.setScale(1);
		sSize = "fcif";
		getVideoFormats();
	    }
	    
	}

	private void getVideoFormats() {
	    if (cH261.getState()) {
		if (!(sSize.equals("fcif"))) {
		    sVideoFormat = "h261";
		    getH261Format();
		}
	    }
	    if (cH263.getState()) {
		if (!(sSize.equals("fcif"))) {
		    sVideoFormat = "h263";
		    getH263Format();
		}
	    }
	    if (cJpeg.getState()) {
		if (!(sSize.equals("fcif"))) {
		    sVideoFormat = "jpeg";
		    getJpegFormat();
		}
	    }
	    if (cRGB.getState()) {
		sVideoFormat = "rgb";
		getRGBFormat();
	    }
	    if (cYUV.getState()) {
		sVideoFormat = "yuv";
		getYUVFormat();
	    }
	}

	private void getRGBFormat() {
	    if (!opiVidCap.setCompress("RGB"))
		return;
	    /*
	     * If sizes are wanted, the only valid sizes are
	     *	NTSC
	     *		fcif	(640 x 480)
	     *		cif	(320 x 240)
	     *		qcif	(160 x 120)
	     *	PAL
	     *		fcif	(768 x 576)
	     *		cif	(384 x 288)
	     *		qcif	(192 x 144)
	     */
	    Dimension size = new Dimension(opiVidCap.getWidth(),
						opiVidCap.getHeight());
	    addVideoFormat(new RGBFormat(size, Format.NOT_SPECIFIED,
					Format.byteArray,
					Format.NOT_SPECIFIED,
					16,
					0xF800, 0x7E0, 0x1F, 2,
					Format.NOT_SPECIFIED,
					Format.FALSE,
					Format.NOT_SPECIFIED));
	}

	private void getYUVFormat() {
	    if (!opiVidCap.setCompress("YUV"))
		return;
	    /*
	     * If sizes are wanted, the only valid sizes are
	     *	NTSC
	     *		fcif	(640 x 480)
	     *		cif	(320 x 240)
	     *		qcif	(160 x 120)
	     *	PAL
	     *		fcif	(768 x 576)
	     *		cif	(384 x 288)
	     *		qcif	(192 x 144)
	     *
	     * The capture stream is actually interleaved YVYU format.
	     * This is defined in the offset values below.
	     */
	    Dimension size = new Dimension(opiVidCap.getWidth(),
						opiVidCap.getHeight());
	    addVideoFormat(new YUVFormat(size, Format.NOT_SPECIFIED,
					Format.byteArray,
					Format.NOT_SPECIFIED,
					YUVFormat.YUV_YUYV,
					Format.NOT_SPECIFIED,
					Format.NOT_SPECIFIED,
					0, 3, 1));
	}

	private void getJpegFormat() {
	    if (!opiVidCap.setCompress("Jpeg"))
		return;
	    /*
	     * If sizes are wanted, the only valid sizes are
	     *	NTSC
	     *		cif	(320 x 240)
	     *		qcif	(160 x 120)
	     *	PAL
	     *		cif	(384 x 288)
	     *		qcif	(192 x 144)
	     */
	    Dimension size = new Dimension(opiVidCap.getWidth(),
						opiVidCap.getHeight());
	    addVideoFormat(new VideoFormat(VideoFormat.JPEG, size,
	    				Format.NOT_SPECIFIED,
					Format.byteArray,
					Format.NOT_SPECIFIED));
	}

	private void getH261Format() {
	    if (!opiVidCap.setCompress("H261"))
		return;
	    /*
	     * If sizes are wanted, the only valid sizes are
	     *		cif	(352 x 288)
	     *		qcif	(176 x 144)
	     */
	    Dimension size = new Dimension(opiVidCap.getWidth(),
						opiVidCap.getHeight());
	    addVideoFormat(new VideoFormat(VideoFormat.H261, size,
	    				Format.NOT_SPECIFIED,
					Format.byteArray,
					Format.NOT_SPECIFIED));
	}

	private void getH263Format() {
	    if (!opiVidCap.setCompress("H263"))
		return;
	    /*
	     * If sizes are wanted, the only valid sizes are
	     *		cif	(352 x 288)
	     *		qcif	(176 x 144)
	     */
	    Dimension size = new Dimension(opiVidCap.getWidth(),
						opiVidCap.getHeight());
	    addVideoFormat(new VideoFormat(VideoFormat.H263, size,
	    				Format.NOT_SPECIFIED,
					Format.byteArray,
					Format.NOT_SPECIFIED));
	}


	public void issueError(String err) {
	    System.err.println(err);
	    Toolkit.getDefaultToolkit().beep();
	}

	public Enumeration sortedFormats(Hashtable formats) {
	    Vector sorted = new Vector();
	    keyloop: for (Enumeration en = formats.keys();
						en.hasMoreElements(); ) {
		String key = (String) en.nextElement();
		for (int i = 0; i < sorted.size(); i++) {
		    if (key.compareTo((String)sorted.elementAt(i)) < 0) {
			sorted.insertElementAt(key, i);
			continue keyloop;
		    }
		}
		sorted.addElement(key);
	    }
	    return sorted.elements();
	}



	public Vector getDeviceInfo() {
	    try {
		while (!finished) {
		    synchronized (lock) {
			lock.wait();
		    }
		    
		    if (needFormat) {
			needFormat = false;
			doFormat();
		    }
		}
	    } catch (InterruptedException ie) {
		mydispose();
		return null;
	    }
	    String locatorPrefix = "sunvideoplus://" + id;
	    mydispose();
	    
	    /*
	     * First remove any old entries
	     */
	    Vector devices = (Vector) CaptureDeviceManager.
						getDeviceList(null).clone();
	    Enumeration enum = devices.elements();
	    while (enum.hasMoreElements()) {
		CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
		String devName = cdi.getName();
		if (devName.startsWith(locatorPrefix))
		    CaptureDeviceManager.removeDevice(cdi);
	    }

	    devices = new Vector();
	    if (anyVideo) {


		for (Enumeration ve = sortedFormats(videoFormats);
						ve.hasMoreElements(); ) {
		    String vKey = (String) ve.nextElement();
		    Format vForm = (VideoFormat)videoFormats.get(vKey);
		    Format[] farray = null;
		    farray = new Format[1];
		    farray[0] = vForm;
		    String name = locatorPrefix + "/" + vKey;
		    CaptureDeviceInfo cdi = new CaptureDeviceInfo(name,
					new MediaLocator(name), farray);
		    CaptureDeviceManager.addDevice(cdi);
		    devices.addElement(cdi);
		}
	    }
	    try {
		CaptureDeviceManager.commit();
		System.err.println("SunVideoPlusManual: Committed ok");
	    } catch (java.io.IOException ioe) {
		System.err.println("SunVideoPlusManual: error committing cdm");
	    }
	    return devices;
	}

    }

    class DBListener implements ActionListener {
	
	int id;

	public DBListener(int id) {
	    this.id = id;
	}

	public void actionPerformed(ActionEvent ae) {
	    createDialog(id);
	}
    }

    class BorderPanel extends Panel {
	int insets = 7;

	public BorderPanel() {
	}
	
	public BorderPanel(int insets) {
	    this.insets = insets;
	}
	
	public Insets getInsets() {
	    return new Insets(insets, insets, insets, insets);
	}

	public void paint(Graphics g) {
	    if (insets != 0) {
		Color bc = getBackground();
		super.paint(g);
		Dimension size = getSize();
		g.setColor(bc.darker());
		g.drawLine(2, 2, size.width - 3, 2);
		g.drawLine(2, 2, 2, size.height - 3);
		g.setColor(bc.brighter());
		g.drawLine(size.width - 3, 2, size.width - 3, size.height - 3);
		g.drawLine(2, size.height - 3, size.width - 3, size.height - 3);
	    }
	}
    } 

    public static void main(String [] args) {
	SunVideoPlusManual m = new SunVideoPlusManual();
	m.setVisible(true);
    }
}

