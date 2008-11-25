/*
 * @(#)BasicJMD.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.lang.*;
import javax.media.*;
import javax.media.Format;
import com.sun.media.*;
import java.awt.*;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.event.*;

public final class BasicJMD extends Panel
implements com.sun.media.JMD, java.awt.event.WindowListener
{
    Vector modList = new Vector();
    Vector conList = new Vector();
    boolean graphic = true;
    Panel center;
    Label status;
    Frame frame = null;
    boolean activated = false;
    Button button = null;
    Dimension preferredSize = new Dimension(512, 140);
    
    public BasicJMD(String title) {
	setLayout(new BorderLayout());
	setBackground(Color.lightGray);
	center = new Panel() {
	    public Dimension getPreferredSize() {
		return preferredSize;
	    }
	};
	center.setLayout(null);
	add("North", center);
	status = new Label();
	add("South", status);
	setSize(512, 200);
    }

    public Component getControlComponent() {
	if (button == null) {
	    button = new Button("PlugIn Viewer") {
		public void removeNotify() {
		    super.removeNotify();
		    dispose();
		}
	    };
	    button.setName("PlugIns");
	    button.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    BasicJMD.this.setVisible( true );
		}
	    } );
	}
	return button;
    }

    public synchronized  void dispose() {
	if (frame != null) {
	    frame.dispose();
	    frame = null;
	}
    }

    public synchronized void setVisible(boolean visible) {
	if (getParent() == null) {
	    if (visible) {
		if (frame == null) {
		    frame = new Frame("PlugIn Viewer");
		    frame.setLayout( new BorderLayout() );
		    frame.add("Center", this);
		    frame.addWindowListener(this);
		    frame.pack();
		    frame.setVisible(true);
		}
	    }
	} else if (getParent() == frame) {
	    frame.setVisible(visible);
	} else
	    super.setVisible(visible);
    }

    int ro, col;
    int colMax = 1;
    int roMax = 1;
    int wrapWidth = 200;
    int wrapHeight = 50;
    int offX = 0, offY = 0;
    int fill = 10;
    int cSize = 10;
    
    public void initGraph(BasicModule source) {
        center.removeAll();
        modList = new Vector();
        conList = new Vector();
        drawGraph(source);
        ro = 0;
        col = 0;
	preferredSize = new Dimension((colMax + 1) * wrapWidth + offX * 2,
				      (roMax) * wrapHeight + offY * 2);
	//pack();
    }

    public void drawGraph(BasicModule source) {
        Module m;
	OutputConnector oc;
	InputConnector ic;
	String names[];
        
	names = source.getOutputConnectorNames();
	int height = names.length;
	if (height == 0)
	    height = 1;
	createModuleWrap(source, ro, col, height);
	if (roMax < names.length)
	    roMax = names.length;
	for (int i = 0; i < names.length; i++) {
	    oc = source.getOutputConnector(names[i]);
	    if ((ic = oc.getInputConnector()) == null) {
		if (col == 0)
		    ro++;
		continue;
	    }
	    if ((m = ic.getModule()) == null) {
		if (col == 0)
		    ro++;
		continue;
	    }
	    col++;
	    if (col > colMax)
		colMax = col;
	    drawGraph((BasicModule)m);
	    col--;
	    if (col == 0)
		ro++;
	}
    }
    
    public void createModuleWrap(BasicModule m, int row, int column, int h) {
        //System.err.println(m + " " + row + " " + column);
        Object plugin = m;
        if (m instanceof BasicSourceModule)
            plugin = ((BasicSourceModule)m).getDemultiplexer();
        else if (m instanceof BasicFilterModule)
            plugin = ((BasicFilterModule)m).getCodec();
        else if (m instanceof BasicRendererModule)
            plugin = ((BasicRendererModule)m).getRenderer();
	else if (m instanceof BasicMuxModule)
	    plugin = ((BasicMuxModule)m).getMultiplexer();
	
        String name = ((PlugIn)plugin).getName();
        //name = name.substring(name.lastIndexOf(".") + 1);
        Button b = new ModButton(name, m, (PlugIn)plugin);
        b.setName("M" + m.hashCode());
        modList.addElement(b);
        b.setBackground(new Color(192, 192, 128));
        b.setForeground(Color.black);
        center.add(b);
        b.setBounds(offX + column * wrapWidth + fill, 
                    offY + row * wrapHeight + fill, 
                    wrapWidth - fill * 2, h * wrapHeight - fill * 2);
        b.setVisible(true);
        center.invalidate();
    }
    
    public void moduleIn(BasicModule bm, int index, Buffer d, boolean here) {
        updateConnector(bm, index, d, here, 0);
    }
    
    public void updateConnector(BasicModule bm, int index, 
                                Buffer d, boolean here,
                                int inOut) {
	if (!activated)
	    return;
        Con c = findConnector(bm, index, inOut);
	if (c == null)
	    return;
        c.setData(d);
        // System.out.println("upd " + c.getName());
        if (true) {
            if (here) {
		if (d.isEOM())
                    c.flash(Color.red);
                else if (d.isDiscard())
                    c.flash(Color.yellow);
                else 
                    c.flash(Color.green);
            } else
                c.flash(Color.gray);
                
            // c.repaint();
        }
    }
    
    public void moduleOut(BasicModule bm, int index, Buffer d, boolean here) {
        updateConnector(bm, index, d, here, 1);
    }
    
    public Con findConnector(BasicModule bm, int index,
                             int inOut) 
    {
        String name = "C" + bm.hashCode() + index + inOut;
        Enumeration e = conList.elements();
        while (e.hasMoreElements()) {
            Con c = (Con) e.nextElement();
            if (c.getName().equals(name))
                return c;
        }
	
	Component m = findModule(bm);
	if (m == null)
	    return null;
        Point p = m.getLocation();
        Con c = new Con();
        center.add(c);
        c.setBounds(p.x - fill + (wrapWidth - fill) * inOut, 
                    p.y + (wrapHeight-2*fill-cSize) / 2 + wrapHeight * index,
                    cSize, cSize);
        c.setName(name);
        conList.addElement(c);
        return c;
    }
    
    public Component findModule(BasicModule bm) {
        String name = "M" + bm.hashCode();
        Enumeration e = modList.elements();
        while (e.hasMoreElements()) {
            Component c = (Component) e.nextElement();
            if (c.getName().equals(name))
                return c;
        }
        return null;
    }
        
    public void windowActivated(WindowEvent we) {
	activated = true;
    }

    public void windowOpened(WindowEvent we) {
    }

    public void windowIconified(WindowEvent we) {
    }

    public void windowDeiconified(WindowEvent we) {
    }

    public void windowClosing(WindowEvent we) {
	setVisible(false);
    }

    public void windowClosed(WindowEvent we) {
    }

    public void windowDeactivated(WindowEvent we) {
	activated = false;
    }
    
    class ModButton extends Button {

	BasicModule module;
	boolean mouseHere = false;
	PlugIn plugin;
	
	public String cropName(String name) {
	    int         length;
	    boolean     appendDots;
	    int         box_width;
	    
	    
	    box_width= 120;
	    
	    FontMetrics fm= getFontMetrics( new Font( "Dialog", Font.PLAIN, 11));
	    
	    String cropped= name;
	    
	    int width= fm.stringWidth( cropped);
	    
	    if( width > box_width) {
		appendDots= true;
	    } else {
		appendDots= false;
	    }
	    
	    while( width > box_width) {
		length= cropped.length();
		
		cropped= name.substring( 0, length - 1);
		
		width= fm.stringWidth( cropped);
	    }
	    
	    if( appendDots) {
		cropped= cropped + "...";
	    }
	    
	    return cropped;
	}
	
	public ModButton(String name, BasicModule m, PlugIn p) {
	    super();
	    
		name= cropName( name);

		super.setLabel( name);

	    this.module = m;
	    this.plugin = p;
	    
	    addMouseListener(new MouseAdapter() {
		public void mouseEntered(MouseEvent me) {
		    updateStatus();
		    mouseHere = true;
		}
		
		public void mouseExited(MouseEvent me) {
		    mouseHere = false;
		}
	    }
			     );
	    
	}
	
	public void updateStatus() {
	    status.setText(plugin.getClass().getName() + " , " + plugin.getName());
	}
    }
    
    class Con extends Button {
        Graphics g = null;
        Buffer data = null;
        boolean mouseHere = false;


        public Con() {
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent me) {
                    updateStatus();
		    mouseHere = true;
                }

		public void mouseExited(MouseEvent me) {
		    mouseHere = false;
		}
            });
        }

	public void flash(Color c) {
	    Graphics g = getGraphics();
	    if (g == null)
		return;
	    g.setColor(c);
	    g.fillRect(1, 1, cSize - 2, cSize - 2);
        }
	
        public Graphics getGraphics() {
            //if (g == null)
                g = super.getGraphics();
            return g;
        }

	public void paint(Graphics g) {
	    g.setColor(Color.black);
	    g.drawRect(0, 0, cSize - 1, cSize - 1);
	    g.setColor(Color.gray);
	    g.fillRect(1, 1, cSize - 2, cSize - 2);
	}
        
        public void setData(Buffer d) {
	    if (mouseHere)
		updateStatus();
            this.data = d;
        }

	void updateStatus() {
	    String s;
	    Format f = data.getFormat();
	    if (f == null)
		s = "null";
	    else
		s = f.toString();
	    status.setText(s + ", Length = " + data.getLength());
	}
    }
}
