/*
 * @(#)MediaEvent.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;
import java.util.EventObject;

/**
 * <code>MediaEvent</code> is the base interface for media events.
 * <p>
 * This is similar to the JMF 1.0 
 * <a href = http://www.javasoft.com/products/java-media/jmf/forDevelopers/playerapi/javax.media.MediaEvent.html> MediaEvent </a> 
 * interface, except it's a class.
 * <p>
 * <h2>Java Beans support </h2>
 *
 * In order to support the Java Beans event model an implementation
 * of MediaEvent is required to sub-class java.util.EventObject.
 * If an implementation is designed to support the 1.0.2 JDK then
 * it may alternatively sub-class sunw.util.EventObject to provide
 * the support appropriate support.
 * 
 **/
public class MediaEvent extends java.util.EventObject {

    public MediaEvent(Object source) {
	super(source);
    }

}
