/*
 * @(#)README.txt	1.9 02/08/21
 *
 * Copyright 1996-2000 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */


Customizer is a convenient tool to reduce JMF 2.x's runtime footprint,
by throwing away undesired plugins. The GUI guides the users to select
their desired plugins step by step and finally generates a new jar file 
which is a subset of original jmf jar file.

To run it, please include customizer.jar and jmf.jar in the classpath,
then type:

      java JMFCustomizer [-d <working-dir>] [-p <javac-path>]

Where, the <working-dir> is the directory to which Customizer dumps some
temporary files. Its default value is ./cuswork.
The <javac-path> is the directory where javac locates. Its default value
is $JAVAHOME/bin.


Note:
1) It relies on jdk1.1.x (or above) and swing1.1 (or above).
2) Customizer can customize the pure java version, Solaris performance
   pack and Windows performance pack.
   For the performance pack, customizer will generate registry file
   new_jmf.properties in the <working-dir> and print out the necessary
   native libs on console.
3) If you choose the option of generating two jar files, it will create two target
   jar files: core_custom.jar and plugin_custom.jar. The former one contains all the
   jmf core classes and the later one contains all the selected plugable classes.
   Then you have to include both jar files into your classpath when you use your
   customized jmf.
   This feature will benefit Java Web Start users.
3) For more information, please visit:
   http://java.sun.com/products/java-media/jmf/index.html	
 
