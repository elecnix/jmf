/*
 * @(#)MediaPlayerRTPDialog.java	1.0 99/04/15
 *
 *  Licensed Materials - Property of IBM
 *  "Restricted Materials of IBM"
 *  5648-B81
 *  (c) Copyright IBM Corporation 1997,1999 All Rights Reserved
 *  US Government Users Restricted Rights - Use, duplication or
 *  disclosure restricted by GSA ADP Schedule Contract with
 *  IBM Corporation.
 *
 */

  
package javax.media.bean.playerbean;

import java.awt.*;
import java.awt.event.*;


/**
  * A <CODE>Dialog</CODE> for setting up RTP parameters. 
  * Invoked  by Java Beans Special Editor MediaPlayerMediaLocationEditor.java
  */  
  
public class MediaPlayerRTPDialog extends Dialog 
                                                       
{
  TextField  IPAdrFld, PortFld, ttlFld; 
  Choice  cbFormat;   
  Button  OKButton, CancelButton;
  String  rtpString=null;    
  
/**
  * Constructs an RTP dialog using the specified <CODE>Frame</CODE>.
  * @param frame The <CODE>Frame</CODE> for the dialog.
  */  
    
  public MediaPlayerRTPDialog(Frame frame) 
  {
    super (frame, true);
    setTitle("Setup RTP Session");
    setBackground(Color.lightGray);
    addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		    cancel();
	    }
  	});
	
	setLayout(new BorderLayout());
	Panel row1 = new Panel();
	row1.setLayout(new FlowLayout());
	row1.add(new Label("IP Address"));
	IPAdrFld = new TextField("",24);
	row1.add(IPAdrFld);
	row1.add(new Label("Media Type"),Label.RIGHT);
	cbFormat= new Choice();
	cbFormat.removeAll();
	row1.add(cbFormat);
	cbFormat.addItem("audio");
	cbFormat.addItem("video");
	Dimension d= cbFormat.getSize();
	d.width=40;
	cbFormat.setSize(d);
	add("North", row1);
	Panel row2 = new Panel();
	add("Center", row2);
	row2.setLayout(new FlowLayout());
	row2.add(new Label("Port "));
	PortFld = new TextField("",8);
	row2.add(PortFld);
	row2.add(new Label("Time to live"));
	ttlFld = new TextField("", 3);
	row2.add(ttlFld);
	Panel row3 = new Panel();
	add("South", row3);
	row3.setLayout(new FlowLayout());
	OKButton = new Button("OK");
	row3.add(OKButton);
	CancelButton = new Button("Cancel");
	row3.add(CancelButton);
	OKButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		close();
	    }
	});
	
	CancelButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		cancel();
	    }
	});
	setLocation(200,300);
	setResizable(false);
	pack();
  } 
  
  private void setRTPAdr()
  {
    rtpString=IPAdrFld.getText()+":"+PortFld.getText()+"/"+cbFormat.getSelectedItem()+"/"+ttlFld.getText();
   }
  
  /**
  * Gets the RTP address for the dialog.
  * @return A <CODE>String</CODE> that contains the RTP address.
  */  
  
  public String getRTPAdr()
  {
    return rtpString;
  }
  private void close()
  {
    setRTPAdr();
    dispose();
  }
 
  private void cancel()
  {
    rtpString = "";
    dispose();
  }
}
