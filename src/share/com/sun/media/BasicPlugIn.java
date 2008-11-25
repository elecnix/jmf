/*
 * @(#)BasicPlugIn.java	1.16 03/04/24
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.lang.reflect.Method;
import java.util.Vector;
import javax.media.format.*;
import javax.media.*;

/**
 * basic implementation for the PlugIn interface
 **/
public abstract class BasicPlugIn implements PlugIn {
    private static final boolean DEBUG=false;
    protected Object [] controls = new Control[0];

    // Commented out the next 3 definitions because of a weird
    // bug when running in jdk1.2 (No problem in jdk1.1.x).
    // Reference to byte[].class in any file causes runtime
    // error java.lang.IllegalAccessError: try to access field
    // com/sun/media/BasicPlugIn.array$B
    // Similarly for short[].class and int[].class
    // Happens even if the next 3 definitions are made public
    // and final.
    // Babu Srinivasan 10/30/98

    /** class type for byte  array **/
    // protected static Class byteArrayClass = byte[].class;

    /** class type for short array **/
    // protected static Class shortArrayClass= short[].class;

    /** class type for int   array **/
    // protected static Class intArrayClass  = int[].class;


    /**
     * throws RuntimeException
     **/

    private static boolean jdkInit = false;
    private static Method forName3ArgsM;
    private static Method getSystemClassLoaderM;
    private static ClassLoader systemClassLoader;
    private static Method getContextClassLoaderM;

    protected void error(){
        throw new RuntimeException(getClass().getName() + " PlugIn error");
    }

    /**
     * no controls
     **/
    public Object[] getControls() {
        return (Object[]) controls;
    }

    /**
     * Return the control based on a control type for the PlugIn.
     */
    public Object getControl(String controlType) {
       try {
          Class  cls = Class.forName(controlType);
          Object cs[] = getControls();
          for (int i = 0; i < cs.length; i++) {
             if (cls.isInstance(cs[i]))
                return cs[i];
          }
          return null;

       } catch (Exception e) {   // no such controlType or such control
         return null;
       }
    }

    /**
     * Utility to perform format matching.
     */
    public static Format matches(Format in, Format outs[]) {
	   for (int i = 0; i < outs.length; i++) {
	      if (in.matches(outs[i]))
		      return outs[i];
	   }

	   return null;
    }

    // utilities for allocating data buffers
    // =====================================

    /**
     * validate that the Buffer object's data size is at least newSize.
     * @return array with sufficient capacity
     **/
    protected byte[] validateByteArraySize(Buffer buffer,int newSize) {
        Object objectArray=buffer.getData();
        byte[] typedArray;

        if (objectArray instanceof byte[]) {     // is correct type AND not null
            typedArray=(byte[])objectArray;
            if (typedArray.length >= newSize ) { // is sufficient capacity
                return typedArray;
            }

            byte[] tempArray=new byte[newSize];  // re-alloc array
            System.arraycopy(typedArray,0,tempArray,0,typedArray.length);
            typedArray = tempArray;
        } else {
            typedArray = new byte[newSize];
        }

        if (DEBUG) System.out.println(getClass().getName()+
	        " : allocating byte["+newSize+"] data");

        buffer.setData(typedArray);
        return typedArray;
    }
    
    /**
     * validate that the Buffer object's data size is at least newSize.
     * @return array with sufficient capacity
     **/
    protected short[] validateShortArraySize(Buffer buffer,int newSize) {
        Object objectArray=buffer.getData();
        short[] typedArray;

        if (objectArray instanceof short[]) {     // is correct type AND not null
            typedArray=(short[])objectArray;
            if (typedArray.length >= newSize ) { // is sufficient capacity
                return typedArray;
            }

            short[] tempArray=new short[newSize];  // re-alloc array
            System.arraycopy(typedArray,0,tempArray,0,typedArray.length);
            typedArray = tempArray;
        } else {
            typedArray = new short[newSize];
        }

        if (DEBUG) System.out.println(getClass().getName()+
	        " : allocating short["+newSize+"] data");

        buffer.setData(typedArray);
        return typedArray;
    }
    /**
     * validate that the Buffer object's data size is at least newSize.
     * @return array with sufficient capacity
     **/
    protected int[] validateIntArraySize(Buffer buffer,int newSize) {
        Object objectArray=buffer.getData();
        int[] typedArray;

        if (objectArray instanceof int[]) {     // is correct type AND not null
            typedArray=(int[])objectArray;
            if (typedArray.length >= newSize ) { // is sufficient capacity
                return typedArray;
            }

            int[] tempArray=new int[newSize];  // re-alloc array
            System.arraycopy(typedArray,0,tempArray,0,typedArray.length);
            typedArray = tempArray;
        } else {
            typedArray = new int[newSize];
        }

        if (DEBUG) System.out.println(getClass().getName()+
	        " : allocating int["+newSize+"] data");

        buffer.setData(typedArray);
        return typedArray;
    }

    protected final long getNativeData(Object data) {
	if (data instanceof NBA)
	    return ((NBA)data).getNativeData();
	else
	    return 0;
    }

    protected Object getInputData(Buffer inBuffer) {
	Object inData = null;
	if (inBuffer instanceof ExtBuffer) {
	    ((ExtBuffer)inBuffer).setNativePreferred(true);
	    inData = ((ExtBuffer)inBuffer).getNativeData();
	}
	if (inData == null)
	    inData = inBuffer.getData();
	return inData;
    }

    protected Object getOutputData(Buffer buffer) {
	Object data = null;
	if (buffer instanceof ExtBuffer) {
	    data = ((ExtBuffer)buffer).getNativeData();
	}
	if (data == null)
	    data = buffer.getData();
	return data;
    }

    protected Object validateData(Buffer buffer, int length, boolean allowNative) {
	Format format = buffer.getFormat();
	Class dataType = format.getDataType();
	
	if (length < 1 && format != null) {
	    if (format instanceof VideoFormat)
		length = ((VideoFormat)format).getMaxDataLength();
	}
	
	if (allowNative && buffer instanceof ExtBuffer &&
	    ((ExtBuffer)buffer).isNativePreferred()) {
	    ExtBuffer extb = (ExtBuffer) buffer;
	    if (extb.getNativeData() == null ||
		extb.getNativeData().getSize() < length)
		extb.setNativeData(new NBA(format.getDataType(), length));
	    return extb.getNativeData();
	} else {
	    if (dataType == Format.byteArray)
		return validateByteArraySize(buffer, length);
	    else if (dataType == Format.shortArray)
		return validateShortArraySize(buffer, length);
	    else if (dataType == Format.intArray)
		return validateIntArraySize(buffer, length);
	    else {
		System.err.println("Error in validateData");
		return null;
	    }
	}
    }

    private static boolean checkIfJDK12() {
	if (jdkInit)
	    return (forName3ArgsM != null);
	jdkInit = true;
	try {
	    forName3ArgsM = Class.class.getMethod("forName",
						  new Class[] {
		String.class, boolean.class, ClassLoader.class
		    });
	    
	    getSystemClassLoaderM = ClassLoader.class.getMethod("getSystemClassLoader", null);

	    // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
	    systemClassLoader = (ClassLoader) getSystemClassLoaderM.invoke(ClassLoader.class, null);

	    getContextClassLoaderM = Thread.class.getMethod("getContextClassLoader", null);

	    return true;
	} catch (Throwable t) {
	    forName3ArgsM = null;
	    return false;
	}
    }

    // This is a Package private class
    // Currently used by a few classes in this package
    // Not used in this class as we can use the generic Class.forName
    static public Class getClassForName(String className) 
                throws ClassNotFoundException {
	/**
	 *  Note: if we don't want this functionality
	 *  just replace it with Class.forName(className)
	 */

	try {
	    return Class.forName(className);
	} catch (Exception e) {
	    if (!checkIfJDK12()) {
		throw new ClassNotFoundException(e.getMessage());
	    }
	} catch (Error e) {
	    if (!checkIfJDK12()) {
		throw e;
	    }
	}

	/**
	 *  In jdk1.2 application, when you have jmf.jar in the ext directory and
	 *  you want to access a class that is not in jmf.jar but is in the CLASSPATH,
	 *  you have to load it using the the system class loader.
	 */
	try {
	    return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
		className, new Boolean(true), systemClassLoader});
	} catch (Throwable e) {
	}

	/**
	 *  In jdk1.2 applet, when you have jmf.jar in the ext directory and
	 *  you want to access a class that is not in jmf.jar but applet codebase,
	 *  you have to load it using the the context class loader.
	 */
	try {
	    // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
	    ClassLoader contextClassLoader =
		(ClassLoader) getContextClassLoaderM.invoke(Thread.currentThread(), null);
	    return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
		className, new Boolean(true), contextClassLoader});
	} catch (Exception e) {
	    throw new ClassNotFoundException(e.getMessage());
	} catch (Error e) {
	    throw e;
	}
    }

    /**
     * Check to see if a particular plugin exists in the registry.
     */
    static public boolean plugInExists(String name, int type) {
	Vector cnames = PlugInManager.getPlugInList(null, null, type);
	for (int i = 0; i < cnames.size(); i++) {
	    if (name.equals((String)(cnames.elementAt(i))))
		return true;
	}
	return false;
    }
}
