/*
 * @(#)I18N.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.util.*;

public class I18N {
    static public java.util.ResourceBundle bundle = null;

    static public String getResource(String key){
       //$$ Testing - cania
       //$$ java.util.Locale.setDefault(Locale.GERMANY);
       Locale currentLocale = java.util.Locale.getDefault();

       if (bundle == null) {
           try{
               bundle = java.util.ResourceBundle.getBundle("com.sun.media.customizer.Props", currentLocale);
           } catch(java.util.MissingResourceException e){
	       e.printStackTrace();
               System.out.println("Could not load Resources");
               System.exit(0);
           }
        }
        String value = new String("");
        try{
            value = (String) bundle.getObject(key);
        } catch (java.util.MissingResourceException e){
	    System.out.println("Could not find " + key);}
        return value;
    }
}
    
