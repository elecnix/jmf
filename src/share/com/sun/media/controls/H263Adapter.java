/*
 * @(#)H263Adapter.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;


/**
 * Implementation for H263Control
 */
public class H263Adapter implements javax.media.control.H263Control {

    boolean isSetable;
    Codec owner=null;
    boolean advancedPrediction=false;
    boolean arithmeticCoding=false;
    boolean errorCompensation=false;
    boolean pbFrames=false;
    boolean unrestrictedVector=false;
    int hrd_B=-1;
    int bppMaxKb=-1;

    Component component=null;
    String CONTROL_ADVANCEDPREDICTION_STRING="Advanced Prediction";
    String CONTROL_ARITHMETICCODING_STRING="Arithmetic Coding";
    String CONTROL_ERRORCOMPENSATION_STRING="Error Compensation";
    String CONTROL_PBFRAMES_STRING="PB Frames";
    String CONTROL_UNRESTRICTEDVECTOR_STRING="Unrestricted Vector";
    String CONTROL_HRD_B_STRING="Hrd B";
    String CONTROL_BPPMAXKB_STRING="Bpp Max Kb";




    public H263Adapter(  Codec newOwner,
                                boolean newAdvancedPrediction,
                                boolean newArithmeticCoding,
                                boolean newErrorCompensation,
                                boolean newPBFrames,
                                boolean newUnrestrictedVector,
                                int newHrd_B,
                                int newBppMaxKb,
                                boolean newIsSetable
			      ) {
        advancedPrediction  = newAdvancedPrediction;
        arithmeticCoding    = newArithmeticCoding;
        errorCompensation   = newErrorCompensation;
        pbFrames            = newPBFrames;
        unrestrictedVector  = newUnrestrictedVector;
        hrd_B               = newHrd_B;
        bppMaxKb            = newBppMaxKb;

        owner = newOwner;
        isSetable = newIsSetable;
    }


    /**
     * Returns if unrestricted vector extension is supported
     * @return if unrestricted vector extension is supported
     */
    public boolean isUnrestrictedVectorSupported() {
        return unrestrictedVector;
    }

    /**
     * Sets the unrestricted vector mode
     * @param newUnrestrictedVectorMode the requested unrestricted vector
     * mode
     * @return the actual unrestricted vector mode that was set
     */
    public boolean setUnrestrictedVector(boolean newUnrestrictedVectorMode){
        return unrestrictedVector;
    }

    /**
     * Returns if unrestricted vector was enabled
     * @return if unrestricted vector was enabled
     */
    public boolean getUnrestrictedVector(){
        return unrestrictedVector;
    }


    /**
     * Returns if arithmeticc coding extension is supported
     * @return if arithmeticc coding extension is supported
     */
    public boolean isArithmeticCodingSupported(){
        return arithmeticCoding;
    }


    /**
     * Sets the arithmeticc coding mode
     * @param newArithmeticCodingMode the requested arithmeticc coding
     * mode
     * @return the actual arithmeticc coding mode that was set
     */
    public boolean setArithmeticCoding(boolean newArithmeticCodingMode){
        return arithmeticCoding;
    }

    /**
     * Returns if arithmeticc coding was enabled
     * @return if arithmeticc coding was enabled
     */
    public boolean getArithmeticCoding(){
        return arithmeticCoding;
    }


    /**
     * Returns if advanced prediction extension is supported
     * @return if advanced prediction extension is supported
     */
    public boolean isAdvancedPredictionSupported(){
        return advancedPrediction;
    }

    /**
     * Sets the advanced prediction mode
     * @param newAdvancedPredictionMode the requested advanced prediction
     * mode
     * @return the actual advanced prediction mode that was set
     */
    public boolean setAdvancedPrediction(boolean newAdvancedPredictionMode){
        return advancedPrediction;
    }

    /**
     * Returns if advanced prediction was enabled
     * @return if advanced prediction was enabled
     */
    public boolean getAdvancedPrediction(){
        return advancedPrediction;
    }


    /**
     * Returns if PB Frames extension is supported
     * @return if PB Frames extension is supported
     */
    public boolean isPBFramesSupported(){
        return pbFrames;
    }

    /**
     * Sets the PB Frames mode
     * @param newPBFramesMode the requested PB Frames
     * mode
     * @return the actual PB Frames mode that was set
     */
    public boolean setPBFrames(boolean newPBFramesMode){
        return pbFrames;
    }

    /**
     * Returns if PB Frames was enabled
     * @return if PB Frames was enabled
     */
    public boolean getPBFrames(){
        return pbFrames;
    }


    /**
     * Returns if error compensation extension is supported
     * @return if error compensation extension is supported
     */
    public boolean isErrorCompensationSupported(){
        return errorCompensation;
    }

    /**
     * Sets the error compensation mode
     * @param newtErrorCompensationMode the requested error compensation
     * mode
     * @return the actual error compensation mode that was set
     */
    public boolean setErrorCompensation(boolean newtErrorCompensationMode){
        return errorCompensation;
    }

    /**
     * Returns if error compensation was enabled
     * @return if error compensation was enabled
     */
    public boolean getErrorCompensation(){
        return errorCompensation;
    }

    /**
     *  Returns the refernce decoder parameter HRD_B
     *  @return the refernce decoder parameter HRD_B
     **/
    public int getHRD_B() {
        return hrd_B;
    }

    /**
     *  Returns the refernce decoder parameter BppMaxKb
     *  @return the refernce decoder parameter BppMaxKb
     **/
    public int getBppMaxKb(){
        return bppMaxKb;
    }

    public Component getControlComponent() {
        if (component ==null ) {
	    try {
		Class[] booleanArray= {boolean.class};
		
		Panel componentPanel=new Panel();
		componentPanel.setLayout(new VFlowLayout(1) );
		
		Panel tempPanel=new Panel();
		tempPanel.setLayout(new BorderLayout() );
		tempPanel.add("Center",new Label(CONTROL_ADVANCEDPREDICTION_STRING,Label.CENTER) );
		Checkbox cb=new Checkbox(null,null,advancedPrediction);
		cb.setEnabled(isSetable);
		cb.addItemListener( (ItemListener)
				    new H263AdapterListener(cb,this,
				        getClass().getMethod("setAdvancedPrediction",booleanArray )));
		tempPanel.add("East",cb );
		tempPanel.invalidate();
		componentPanel.add(tempPanel);
		
		tempPanel=new Panel();
		tempPanel.setLayout(new BorderLayout() );
		tempPanel.add("Center",new Label(CONTROL_ARITHMETICCODING_STRING,Label.CENTER) );
		cb=new Checkbox(null,null,arithmeticCoding);
		cb.setEnabled(isSetable);
		cb.addItemListener( (ItemListener)
				    new H263AdapterListener(cb,this,
					getClass().getMethod("setArithmeticCoding",booleanArray )));
		tempPanel.add("East",cb );
		tempPanel.invalidate();
		componentPanel.add(tempPanel);
		
		tempPanel=new Panel();
		tempPanel.setLayout(new BorderLayout() );
		tempPanel.add("Center",new Label(CONTROL_ERRORCOMPENSATION_STRING,Label.CENTER) );
		cb=new Checkbox(null,null,errorCompensation);
		cb.setEnabled(isSetable);
		cb.addItemListener( (ItemListener)
				    new H263AdapterListener(cb,this,
					getClass().getMethod("setErrorCompensation",booleanArray )));
		tempPanel.add("East",cb );
		tempPanel.invalidate();
		componentPanel.add(tempPanel);
		
		tempPanel=new Panel();
		tempPanel.setLayout(new BorderLayout() );
		tempPanel.add("Center",new Label(CONTROL_PBFRAMES_STRING,Label.CENTER) );
		cb=new Checkbox(null,null,pbFrames);
		cb.setEnabled(isSetable);
		cb.addItemListener( (ItemListener)
				    new H263AdapterListener(cb,this,
					getClass().getMethod("setPBFrames",booleanArray )));
		tempPanel.add("East",cb );
		tempPanel.invalidate();
		componentPanel.add(tempPanel);
		
		tempPanel=new Panel();
		tempPanel.setLayout(new BorderLayout() );
		tempPanel.add("Center",new Label(CONTROL_UNRESTRICTEDVECTOR_STRING,Label.CENTER) );
		cb=new Checkbox(null,null,unrestrictedVector);
		cb.setEnabled(isSetable);
		cb.addItemListener( (ItemListener)
				    new H263AdapterListener(cb,this,
					getClass().getMethod("setUnrestrictedVector",booleanArray )));
		tempPanel.add("East",cb );
		tempPanel.invalidate();
		componentPanel.add(tempPanel);
		
		tempPanel=new Panel();
		tempPanel.setLayout(new BorderLayout() );
		tempPanel.add("Center",new Label(CONTROL_HRD_B_STRING,Label.CENTER) );
		tempPanel.add("East",new Label(hrd_B+"",Label.CENTER) );
		tempPanel.invalidate();
		componentPanel.add(tempPanel);
		
		tempPanel=new Panel();
		tempPanel.setLayout(new BorderLayout() );
		tempPanel.add("Center",new Label(CONTROL_BPPMAXKB_STRING,Label.CENTER) );
		tempPanel.add("East",new Label(bppMaxKb+"",Label.CENTER) );
		tempPanel.invalidate();
		componentPanel.add(tempPanel);
		
		component=componentPanel;
	    } catch (Exception exception) {
	    }
	    
        }
        return (Component)component;
    }
    
    class H263AdapterListener implements java.awt.event.ItemListener  {
	Checkbox cb;
	Method   m;
	H263Adapter owner;
	
	public H263AdapterListener(Checkbox source,H263Adapter h263adaptor,Method action ) {
	    cb=source;
	    m=action;
	    owner=h263adaptor;
	}
	
	public void itemStateChanged(ItemEvent e) {
	    Object result=null;
	    
	    try {
		boolean newState = cb.getState() ;
		Boolean[] operands= {newState ? Boolean.TRUE : Boolean.FALSE};
		//DEBUG                 System.out.println("newState "+newState+" -> "+m);
		
		result=m.invoke(owner, (Object[])operands );
		//DEBUG                 System.out.println("newState2 "+result);
	    } catch (Exception exception) {
	    }
	    
	    cb.setState(result.equals(Boolean.TRUE));
	    
	}
    }
}
