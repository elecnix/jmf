
/* Generates BuildInfo.java */

#include <stdio.h>
#include <windows.h>

main(int argc, char **argv)
{
    char date[100];
    SYSTEMTIME st;
    GetLocalTime(&st);
    sprintf(date, "%d/%d/%d %d:%d",
	    st.wMonth, st.wDay, st.wYear,
	    st.wHour, st.wMinute);
    /* Get rid of trailing CR and line feeds */
    while (date[strlen(date)-1] < 32)
	date[strlen(date)-1] = 0;

    /* Output the BuildInfo.java to stdout */
    printf("package com.sun.media;\n"

	   "public class BuildInfo {\n"
	   "    public static String date = \"%s\";\n"
	   "    public static void main(String [] args) {\n"
	   "        System.out.println(\"JMF Version : \" + javax.media.Manager.getVersion() + \"\\n\" +\n"
	   "                           \"Build Date  : \" + date);\n"
	   "    }\n"
	   "}\n", date);
    return 0;
}

