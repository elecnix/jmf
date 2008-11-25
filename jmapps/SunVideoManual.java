/*
 * @(#)SunVideoManual.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import com.sun.media.protocol.sunvideo.*;

public class SunVideoManual extends Frame {

    private static String DEVICE_PREFIX = "/dev/rtvc";
    private static String PROTOCOL = "sunvideo";
    private static String LOCATOR_PREFIX = PROTOCOL + "://";
    CaptureDeviceInfo [] devices = null;
    int currentID = -1;
    
    public SunVideoManual() {
	super("Register SunVideo Caputure Devices");

	setLayout(new FlowLayout());
	int nDevices = 0;
	for (int i = 0; i < 7; i++) {
	    File fl = new File(DEVICE_PREFIX + i);
	    if (fl.exists()) {
		Button device = new Button(DEVICE_PREFIX + i);
		add(device);
		device.addActionListener( new DBListener(i) );
		nDevices++;
	    }
	}

	devices = new CaptureDeviceInfo[nDevices];
	
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
		CaptureDeviceInfo cdi = fd.getDeviceInfo();
		if (cdi != null) {
		    System.err.println("CaptureDeviceInfo = " + cdi.getName() + " " +
				       cdi.getLocator());
		}
		
		currentID = -1;
	    }
	} );
	t.start();
	// Create a CaptureDeviceInfo....
    }

    class FormatsDialog extends Frame {

	int id;
	Button bVideoFormat, bDone, bCancel;
	CheckboxGroup cPortGroup;
	Checkbox cPort1, cPort2, cSvhs;
	Checkbox cRGB, cJpeg;
	Checkbox cFull, cHalf, cQuarter;
	Vector formats = new Vector();
	XILCapture xilCap = null;
	Integer lock = new Integer(1);
	boolean finished = false;
	boolean needVideoFormat = false;
	
	public FormatsDialog(int id) {
	    super("sunvideo " + DEVICE_PREFIX + id);
	    this.id = id;
	    setLayout(new BorderLayout());
	    cPortGroup = new CheckboxGroup();
	    cPort1 = new Checkbox("1", true, cPortGroup);
	    cPort2 = new Checkbox("2", false, cPortGroup);
	    cSvhs = new Checkbox("S-VHS", false, cPortGroup);
	    cJpeg = new Checkbox("Jpeg", true);
	    cRGB = new Checkbox("RGB", false);
	    cFull = new Checkbox("1", false);
	    cHalf = new Checkbox("1/2", true);
	    cQuarter = new Checkbox("1/4", false);
	    bVideoFormat = new Button("VideoFormat");
	    bDone = new Button("Done");
	    bCancel = new Button("Cancel");
	    
	    xilCap = new XILCapture(null);
	    if (!xilCap.connect(id)) {
		throw new Error("Unable to connect to device");
	    }
	    
	    Panel p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(new Label("Port"));
	    p.add(cPort1);
	    p.add(cPort2);
	    p.add(cSvhs);
	    add("North", p);
	    
	    Panel cp = new Panel();
	    cp.setLayout(new BorderLayout());

	    p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(new Label("Formats"));
	    p.add(cJpeg);
	    p.add(cRGB);
	    cp.add("North", p);

	    p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(new Label("Sizes"));
	    p.add(cFull);
	    p.add(cHalf);
	    p.add(cQuarter);
	    cp.add("South", p);
	    add("Center", cp);

	    p = new Panel();
	    p.setLayout(new FlowLayout());
	    p.add(bVideoFormat);
	    p.add(bDone);
	    p.add(bCancel);
	    add("South", p);
	    pack();

	    addWindowListener( new WindowAdapter() {
		public void windowClosing(WindowEvent we) {
		    synchronized (lock) {
			finished = true;
			lock.notify();
		    }
		}
	    } );

	    bDone.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
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

	    bVideoFormat.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    needVideoFormat = true;
		    synchronized (lock) {
			lock.notify();
		    }
		}
	    } );
	}

	private void addFormat(Format fin) {
	    Enumeration enum = formats.elements();
	    while (enum.hasMoreElements()) {
		Format f = (Format) enum.nextElement();
		if (f.equals(fin))
		    return;
	    }

	    System.err.println("New format = " + fin);
	    formats.addElement(fin);
	}

	public void mydispose() {
	    xilCap.disconnect();
	    dispose();
	    System.err.println("Disconnected driver");
	}
	
	public void doVideoFormat() {
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
	    if (!xilCap.setPort(port)) {
		System.err.println("Video source not recognized on port");
		return;
	    }
	    if (cHalf.getState()) {
		xilCap.setScale(2);
		getFormats();
	    }
	    if (cQuarter.getState()) {
		xilCap.setScale(4);
		getFormats();
	    }
	    if (cFull.getState()) {
		xilCap.setScale(1);
		getFormats();
	    }
	    
	}

	private void getFormats() {
	    if (cJpeg.getState())
		getJpegFormat();
	    if (cRGB.getState())
		getRGBFormat();
	}

	private void getRGBFormat() {
	    if (!xilCap.setCompress("RGB"))
		return;
	    // To get the real values, start the device
	    if (xilCap.start()) {
		Dimension size = new Dimension(xilCap.getWidth(),
						xilCap.getHeight());
		int stride = xilCap.getLineStride();
		int maxbuf = stride * size.width;
		addFormat(new RGBFormat(size, maxbuf, byte[].class,
					15f,
					24,
					3, 2, 1, 3, stride,
					Format.FALSE,
					Format.NOT_SPECIFIED));
	    }
	    xilCap.stop();
	}

	private void getJpegFormat() {
	    if (!xilCap.setCompress("Jpeg"))
		return;
	    // To get the real values, start the device
	    if (xilCap.start()) {
		Dimension size = new Dimension(xilCap.getWidth(),
						xilCap.getHeight());
		// approximate the max for high quality
		int maxbuf = 3 * size.width * size.height;
		addFormat(new VideoFormat(VideoFormat.JPEG, size, maxbuf,
					  byte[].class, 15f));
	    }
	    xilCap.stop();
	}



	public CaptureDeviceInfo getDeviceInfo() {
	    try {
		while (!finished) {
		    synchronized (lock) {
			lock.wait();
		    }
		    
		    if (needVideoFormat) {
			needVideoFormat = false;
			doVideoFormat();
		    }
		}
	    } catch (InterruptedException ie) {
		mydispose();
		return null;
	    }
	    String name = "SunVideo device " + id;
	    String locator = LOCATOR_PREFIX + id;
	    mydispose();
	    
	    if (formats == null || formats.size() < 1)
		return null;
	    else {

		Format [] farray = new Format[formats.size()];
		Enumeration enum = formats.elements();
		int i = 0;
		while (enum.hasMoreElements()) {
		    Format f = (Format) enum.nextElement();
		    farray[i++] = f;
		}
		// First remove any old entries
		CaptureDeviceInfo cdi = CaptureDeviceManager.getDevice(name);
		if (cdi != null)
		    CaptureDeviceManager.removeDevice(cdi);
		cdi = new CaptureDeviceInfo(name, new MediaLocator(locator),
							      farray);
		CaptureDeviceManager.addDevice(cdi);
		try {
		    CaptureDeviceManager.commit();
		    System.err.println("SunVideoManual: Committed ok");
		} catch (java.io.IOException ioe) {
		    System.err.println("SunVideoManual: error committing cdm");
		}
		return cdi;
	    }
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

    public static void main(String [] args) {
	SunVideoManual m = new SunVideoManual();
	m.setVisible(true);
    }
}

