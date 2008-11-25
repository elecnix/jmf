/*
 * @(#)PerTypePanel.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.registry;

import java.lang.reflect.Method;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

import javax.media.*;
import javax.media.format.*;
import javax.media.renderer.*;
import javax.media.PlugInManager;

import com.sun.media.util.Registry;
import com.sun.media.util.JMFI18N;
import com.sun.media.util.DynamicPlugIn;

import jmapps.ui.*;


public class PerTypePanel extends JMPanel implements VectorEditor {

    VectorPanel         panelPlugIns;
    Vector              pluginVector;
    Vector              pluginArray;
    TextArea            textArea;
    int                 type;

    private static boolean      jdkInit = false;
    private static Method       forName3ArgsM;
    private static Method       getSystemClassLoaderM;
    private static ClassLoader  systemClassLoader;
    private static Method       getContextClassLoaderM;

    static final String [] pluginTypes = { JMFI18N.getResource("jmfregistry.plugin"),
			      JMFI18N.getResource("jmfregistry.plugin.demultiplexer"),
			      JMFI18N.getResource("jmfregistry.plugin.codec"),
			      JMFI18N.getResource("jmfregistry.plugin.effect"),
			      JMFI18N.getResource("jmfregistry.plugin.renderer"),
			      JMFI18N.getResource("jmfregistry.plugin.multiplexer") };

    
    public PerTypePanel ( int type ) {
        super();

        Panel   panel;
        Label   label;

        this.type = type;
        setLayout ( new GridLayout(1,2,6,6) );

        panelPlugIns = new VectorPanel ( pluginTypes[type], (VectorEditor)this, type );
        add ( panelPlugIns );

        panel = new Panel ( new BorderLayout() );
        add ( panel );

        label = new Label ( JMFI18N.getResource("jmfregistry.plugin.details.label") );
        panel.add ( label, BorderLayout.NORTH );
        textArea = new TextArea ();
        textArea.setEditable ( false );
        panel.add ( textArea, BorderLayout.CENTER );
    }


    private static boolean checkIfJDK12() {
        if (jdkInit)
            return (forName3ArgsM != null);

        jdkInit = true;
        try {
            forName3ArgsM = Class.class.getMethod ( "forName", new Class[] {
                String.class, boolean.class, ClassLoader.class
		    });
            getSystemClassLoaderM = ClassLoader.class.getMethod ( "getSystemClassLoader", null );
            // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
            systemClassLoader = (ClassLoader) getSystemClassLoaderM.invoke ( ClassLoader.class, null );
            getContextClassLoaderM = Thread.class.getMethod ( "getContextClassLoader", null );
        }
        catch (Throwable t) {
            forName3ArgsM = null;
            return false;
        }
        return true;
    }

    // This is a Package private class
    // Currently used by a few classes in this package
    // Not used in this class as we can use the generic Class.forName
    static Class getClassForName ( String className )
                throws ClassNotFoundException {
        /**
         *  Note: if we don't want this functionality
         *  just replace it with Class.forName(className)
         */

        try {
            return Class.forName(className);
        }
        catch (Exception e) {
            if (!checkIfJDK12())
                throw new ClassNotFoundException(e.getMessage());
        }
        catch (Error e) {
            if ( !checkIfJDK12() )
                throw e;
        }

        /**
         *  In jdk1.2 application, when you have jmf.jar in the ext directory and
         *  you want to access a class that is not in jmf.jar but is in the CLASSPATH,
         *  you have to load it using the the system class loader.
         */
        try {
            return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
                className, new Boolean(true), systemClassLoader
            });
        }
        catch (Throwable e) {
        }

        /**
         *  In jdk1.2 applet, when you have jmf.jar in the ext directory and
         *  you want to access a class that is not in jmf.jar but applet codebase,
         *  you have to load it using the the context class loader.
         */
        try {
            // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
            ClassLoader contextClassLoader = (ClassLoader) getContextClassLoaderM.invoke ( Thread.currentThread(), null );
            return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
                className, new Boolean(true), contextClassLoader
            });
        }
        catch (Exception e) {
            throw new ClassNotFoundException ( e.getMessage() );
        }
        catch (Error e) {
            throw e;
        }
    }

    public Vector stringArrayToVector(String [] array) {
        Vector v = new Vector();
        if (array != null)
            for (int i = 0; i < array.length; i++)
                v.addElement(array[i]);
        return v;
    }

    public String [] vectorToStringArray(Vector v) {
        String [] sa = new String[v.size()];
        Enumeration e = v.elements();
        int i = 0;
        while (e.hasMoreElements())
            sa[i++] = (String) e.nextElement();
        return sa;
    }

    public Vector getList(int code) {
        pluginVector = PlugInManager.getPlugInList(null, null, type);
        if (pluginVector == null)
            pluginVector = new Vector(1);
        return pluginVector;
    }

    public void setList(int code, Vector v) {
        pluginVector = v;
    }

    public void commit(int code) {
        PlugInManager.setPlugInList(pluginVector, type);
        try {
            PlugInManager.commit();
        }
        catch (IOException ioe) {
            System.err.println("PlugInManager.commit() - " + ioe);
        }
    }

    public boolean addToList(int code, String value) {
        // Register the plugin
        try {
            String className = value;
            // Class pic = Class.forName(className);
            Class pic = getClassForName(className);
            Object instance = pic.newInstance();
            Format [] inputs = null;
            Format [] outputs = null;
            int type;
	    
            if (instance instanceof Demultiplexer) {
                type = PlugInManager.DEMULTIPLEXER;
                inputs = ((Demultiplexer)instance).getSupportedInputContentDescriptors();
                outputs = new Format[0];
            }
            else if (instance instanceof javax.media.Renderer) {
                type = PlugInManager.RENDERER;
                inputs = ((javax.media.Renderer)instance).getSupportedInputFormats();
                outputs = new Format[0];
            }
            else if (instance instanceof Multiplexer) {
                type = PlugInManager.MULTIPLEXER;
                inputs = new Format[0];
                outputs = ((Multiplexer)instance).getSupportedOutputContentDescriptors(null);
            }
            else if (instance instanceof Effect) {
                type = PlugInManager.EFFECT;
                inputs = ((Effect)instance).getSupportedInputFormats();
                outputs = ((Effect)instance).getSupportedOutputFormats(null);
            }
            else if (instance instanceof Codec) {
                type = PlugInManager.CODEC;
                inputs = ((Codec)instance).getSupportedInputFormats();
                outputs = ((Codec)instance).getSupportedOutputFormats(null);
            }
            else
                type = 0;

            if (instance instanceof DynamicPlugIn) {
                inputs = ((DynamicPlugIn)instance).getBaseInputFormats();
                outputs = ((DynamicPlugIn)instance).getBaseOutputFormats();
            }
	    
            if (type != 0) {
                if ( PlugInManager.addPlugIn(className, inputs, outputs, type) )
                    return true;
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }
	
        return false;
    }

    public void selectedIndex(int code, int index) {
        String  name = null;
        String  text = "";
        Object  input = null;
        Object  output = null;

        if ( index >= 0 )
            name = (String) pluginVector.elementAt(index);

        if ( name != null )
            input = PlugInManager.getSupportedInputFormats(name, type);
        if (input != null) {
            text += JMFI18N.getResource("jmfregistry.details.informats")
                                + "---->\n\n" + printFormats(input) + "\n\n";
        }

        if ( name != null )
            output = PlugInManager.getSupportedOutputFormats(name, type);
        if (output != null) {
            text += JMFI18N.getResource("jmfregistry.details.outformats")
                                + "--->\n\n" + printFormats(output) + "\n\n";
        }
      	textArea.setText(text);
    }

    private String printFormats(Object fa) {
        if ( !(fa instanceof Format[]) ) {
            return "null";
        }
        else {
            Format [] formats = (Format []) fa;
            String text = "";
            for (int i = 0; i < formats.length; i++) {
                text += i + ". " + formats[i].getClass().getName() + "\n  " + formats[i] + "\n";
            }
            return text;
        }
    }
    
}
    

