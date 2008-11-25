/*
 * @(#)MediaPlayerMediaLocationEditor.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
 
// Last Updated: 02/26/99
// Notes: N/A  
 
  
package javax.media.bean.playerbean;

import java.beans.*;  
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;



/**
  *   Special case property editor for the  media location property
  *   of a <CODE>MediaPlayer</CODE> bean.
  *
  *   Invoked by Java Beans application builders.
  *
  *   @version 1.0
  */
  
public class MediaPlayerMediaLocationEditor extends Panel
      implements java.beans.PropertyEditor, 
                 ActionListener,
                 ItemListener

{
  PropertyChangeSupport support = new PropertyChangeSupport(this);;
  String mediaLocationString;
  TextField mediaLocationTextField= new TextField(mediaLocationString, 60);    
  private String  browseString    = "...";
  private Button  browseB         = new Button(browseString);
//  private String  readyString     = MediaPlayerResource.getString("READY");
//  private Button  readyB          = new Button(readyString);
  private Choice  protocolChooser = new Choice();
  private String  httpString      = MediaPlayerResource.getString("HTTP");
  private String  httpsString      = MediaPlayerResource.getString("HTTPS");
  private String  fileString      = MediaPlayerResource.getString("FILE");
  private String  rtpString       = MediaPlayerResource.getString("RTP");
  private String  ftpString       = MediaPlayerResource.getString("FTP");
  private String  codeString      = MediaPlayerResource.getString("CODEBASE");
  private String  chooseOneString = MediaPlayerResource.getString("CHOOSE_ONE");
  private boolean isFile = false;
  
  Panel editPanel = new Panel();
  
	/**
	  *   Default constructor that adds the <CODE>KeyListener</CODE> for the
	  *   visual text field.
	  */
  public MediaPlayerMediaLocationEditor() 
  {
    setLayout(new BorderLayout());
    editPanel.setLayout(new BorderLayout());
    editPanel.add("Center", mediaLocationTextField);
    editPanel.add("East", browseB);
    browseB.addActionListener(this);
    browseB.setEnabled(false);
    protocolChooser.add(chooseOneString);
    protocolChooser.add(fileString);
    protocolChooser.add(httpString);
    protocolChooser.add(httpsString);
    protocolChooser.add(rtpString);
    protocolChooser.add(ftpString);
    protocolChooser.add(codeString);
    protocolChooser.addItemListener(this);
    editPanel.add("West", protocolChooser);
    
    //Ready button originally was designed for WebRunner's BeanTool.  
    //For other IDEs, when OK button is clicked, the event will be
    //generated, and MediaLocation value will get set.  This is true
    //for Symantec Cafe, VisualAge for Java, and JBuilder.  For other
    //IDEs, if event doesn't get generated, it is user's responsibility
    //to make sure the value will be set when OK button is clicked.
    
    //   editPanel.add("South",readyB);
    //   readyB.addActionListener(this);
    //   readyB.setEnabled(false);
    
    add("Center", editPanel);
    KeyListener l = new KeyAdapter()
    {
    
      /**
      *   The actions to take when a key is typed in the
      *   medial location property.
      *   
      * @param keyEvent The <CODE>KeyEvent</CODE> for the key just typed.
      *
      */
      public void keyReleased(KeyEvent keyEvent)
      {
	      mediaLocationString = mediaLocationTextField.getText();
	      /*
	      if (mediaLocationString.length()==0)
	        readyB.setEnabled(false);
	      else  
	        readyB.setEnabled(true);
	      */
      }   
    };
  
		mediaLocationTextField.addKeyListener(l);
		mediaLocationTextField.setEnabled(false);
  }

	/**
	  *
      *   Generates a code fragment that can be used
	  *   to initialize a variable with the current mediaLocation 
	  *   property value.
	  *
    * @return  The initialization <CODE>String</CODE>.
	  * 
	  */
  public String getJavaInitializationString() 
  {
		String initString;
		
		if (mediaLocationString.length() == 0) 
		{
			initString = "new java.lang.String(\"\\\")";
		}
		else 
		{
			initString = "new java.lang.String(\""
						+ mediaLocationString
						+ "\")";
		}
		return initString;
  }

	/**
	  *
	  *  Gets the preferred dimensions for this media location editor.
	  *
	  * @return A <CODE>Dimension</CODE> that contains the preferred dimensions.
	  */
  public Dimension getPreferredSize() 
  {
		return new Dimension(400, 100); 
  }


	/**
	  *
	  *  Sets the value for the media location property.
	  *
	  * @param o  The new media location property object.
	  * 
	  */
  public void setValue(Object o) 
  {
    setAsText(o.toString());
	}

	/**
	  *
	  *   Gets the value from the media location property.
	  *
	  * @return  The media location property value.
	  */
	public Object getValue() {
    return getAsText();
	}

	/**
	  *  Sets the property value to the specified string.
	  *
	  * @param s  The String  to set the media location property value to.
	  */
	public void setAsText(String s)
	{ 
		mediaLocationString = s;
	}

	/**
	  *
	  *  Gets the mediaLocation property.
	  *  
	  * @return The value of the media location property as a <CODE>String</CODE>.
	  *
	  */
	public String getAsText() 
	{
		return this.mediaLocationString;
	}


	/**
	  *
	  *   Determines whether or not this class  honors
	  *   the <CODE>paintValue</CODE> method. Used by bean builders.
	  *
	  * @return <CODE>true</CODE> if it honors the <CODE>paintValue</CODE> method, 
	  * <CODE>false</CODE> if it does not.
  */	  
  public boolean isPaintable() 
  {
		return true;
  }

	/**
	  *
	  *  Paints the media location's editor box.
	  * 
	  * @param g The graphics context in which to paint the editor box.
	  * @param area The area in which the editor box should be rendered.
	  *
    */
  public void paintValue(Graphics g, Rectangle area) 
  {
		Color c = g.getColor();
		g.setColor(Color.black);
		g.drawString(mediaLocationString,
					area.x, area.y + area.height - 6);
		g.setColor(c);
	}

	/**
	  *
	  *   Determines whether or not this is a tagged value. 
	  * @return null to tell the bean builder that this
	  *   is not a tagged value property.
	  *
    */	  
	public String[] getTags()
	{
		return null;
	}

	/**
	  *
	  *   Gets the panel that supports the media location
	  *   editing.
	  * @param The panel as a <CODE>Component</CODE>.
	  *
	  */
	public Component getCustomEditor() 
	{
	  return editPanel;
	}

	/**
	  *  Determines whether or not this property editor supports a custom editor.
	  *   @return true since the media location property 
	  *   can provide a custom editor. 
	  *
	  */
	public boolean supportsCustomEditor() 
	{
		return true;
	}


	/**
	  *
	  *   Registers a listener for the <CODE>PropertyChangeEvent</CODE> . 
	  *
  	  * @param listener An object to be invoked when a <CODE>PropertyChangeEvent</CODE> 
  	  *    is fired. 
  	  *
	  */
  public void addPropertyChangeListener(PropertyChangeListener listener) 
  {
		support.addPropertyChangeListener(listener);
  }

	/**
	  *
	  *   Removes the specified listener from the property change listener list. 
	  *
  	* @param listener The <CODE>PropertyChangeListener</CODE> to be removed.
  	*
	  */
  public void removePropertyChangeListener(PropertyChangeListener listener) 
  {
		support.removePropertyChangeListener(listener);
  }
  
  /**
	  *
	  *   Determines which controls have had the specified
	  *  action performed and acts accordingly.
	  *
  	* @param evt The <CODE>ActionEvent</CODE>.
  	*
	  */
  public void actionPerformed(ActionEvent evt)
  {
    String command = evt.getActionCommand();
    String filename;
    if (command.equals(browseString))
    {
      if (isFile==true)
      {
        FileDialog fd = new FileDialog(getFrame(editPanel), 
                        MediaPlayerResource.getString("SET_MEDIA_LOCATION"),
                        FileDialog.LOAD);
        fd.setDirectory("c:\\");
        fd.setTitle(MediaPlayerResource.getString("SET_MEDIA_LOCATION"));
        fd.show();
        filename = fd.getFile();
        if (filename != null && fd.getDirectory() != null) 
        {
          filename = fd.getDirectory() + filename;
        }
        if (filename != null) {
          filename = filename.replace('\\', '/');
          String tmp = "file:///"+filename;
          mediaLocationTextField.setText(tmp);
          setAsText(tmp);
      //   readyB.setEnabled(true);
        }
      }
      else
      {
        MediaPlayerRTPDialog rtpDlg= new MediaPlayerRTPDialog(getFrame(editPanel));
        rtpDlg.show();
        filename = rtpDlg.getRTPAdr(); 
        if (filename !=null) 
        {
          String tmp = "rtp://"+filename;
          mediaLocationTextField.setText(tmp);
          setAsText(tmp);
        }
      }
    }
    //The code below is to handle Ready button
    /*
    else if (command.equals(readyString))
    {
      String tmp = mediaLocationString;  
      String choiceSelection = protocolChooser.getSelectedItem();
      setAsText(tmp);
		  support.firePropertyChange("mediaLocation",
									null,
									mediaLocationString);
    } 
    */
  }
  
  /**
	  *
	  * Determines which protocol is selected and acts accordingly.
	  *
  	* @param evt The <CODE>ItemEvent</CODE>.
  	*
	  */
  public void itemStateChanged(ItemEvent evt)
  {
    String item = (String)evt.getItem();
    if (!item.equals(chooseOneString) ) 
    {
      mediaLocationTextField.setEnabled(true);
      if (item.equals(fileString))
      {
        browseB.setEnabled(true);
        isFile=true;
      }
      else if (item.equals(rtpString))
      {
        browseB.setEnabled(true);
      }
      else
      {
        browseB.setEnabled(false);
      }
      if (!item.equals(codeString))
      {
        mediaLocationTextField.setText(item);
	    }
	    else
	    {
	      mediaLocationTextField.setText("");
	    }
      //    readyB.setEnabled(false);
	    mediaLocationString = mediaLocationTextField.getText();
	   }
    else
    {
      mediaLocationTextField.setEnabled(false);
      browseB.setEnabled(false);
      //    readyB.setEnabled(false);
    }
  }
  
  /**
    *   Sets up a <CODE>Frame</CODE> at the position where the specified
    *   <CODE><CODE>Component</CODE></CODE> located.
    *
    * @param comp The <CODE>Component</CODE> where the <CODE>Frame</CODE> will be located.
    *
    */
  Frame getFrame(Component comp) 
  {
    Point p = comp.getLocationOnScreen();
    Frame f = new Frame();
    f.setLocation(p);
    return f;
  }
}

