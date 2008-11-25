/*
 * @(#)jmlaunch.c	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <direct.h>
#include <winsock.h>
#define getcwd _getcwd

#define SLEEP(time) Sleep((time) * 1000)

#ifndef JMFINIT
#define WINMAIN 1
#endif

#include <stdio.h>
#include <signal.h>

#define TRUE 1
#define FALSE 0
#define PATH_MAX 4000

#define MSG(s) printf(s)

static int start_server(int port);
static void setup_environment();
static void usage();

#ifdef JMSTUDIO
static char *program = "JMStudio";
#endif
#ifdef JMFREGISTRY
static char *program = "JMFRegistry";
#endif
#ifdef JMFINIT
static char *program = "JMFInit";
#endif
#ifdef JMFCUST
static char *program = "JMFCustomizer";
#endif

#ifdef WINMAIN
static char *JVM = "javaw";
#else 
static char *JVM = "java";
#endif /* WINMAIN */

#ifndef WINMAIN
main(int argc, char **argv)
#else 
win_main(int argc, char **argv)
#endif
{
    int length;
    int result;
    int i;
    char URL[2048];
    
    /* Parse the command line arguments */
    i = 1;
    while (i < argc) {
       if (strcmp(argv[i], "-jre") == 0) {
	    /* Use "jre" instead of "java" to start the VM */
	    i++;
	    if (i >= argc) {
		usage();
		exit(-1);
	    } else {
		JVM = argv[i];
	    }
	}

	else if ((strcmp(argv[i], "-help") == 0) || 
		 (strcmp(argv[i], "-h") == 0) ||
		 (strcmp(argv[i], "-?") == 0)) {
	    usage();
	    exit(0);
	}

	else {
	    /* End of the normal command line, the rest must be URL's */
	    break;
	}
	i++;
    }
    
    URL[0] = 0;
    while ( i < argc ) {
	strcat(URL, argv[i]);
	strcat(URL, " ");
	i++;
    }

    setup_environment();
    start_server(URL);
    exit(0);
}

static int 
start_server(char * URLs) 
{
    char command[1024];

    {
	int result;
	STARTUPINFO startupInfo;
	PROCESS_INFORMATION processInfo;

	sprintf(command, "%s %s %s",
		JVM, program, URLs);

	GetStartupInfo(&startupInfo);
	result = CreateProcess(NULL, command, NULL, NULL, FALSE, 0, NULL,
			       NULL, &startupInfo, &processInfo);
#ifdef JMFINIT
	if (result) {
	    HANDLE hProcess;
	    DWORD status;
	    
	    hProcess = processInfo.hProcess;
	    while (1) {
		Sleep(500);
		GetExitCodeProcess(hProcess, &status);
		if (status != STILL_ACTIVE)
		    break;
	    }
	}
#endif
    }
}

void 
setup_environment() {
    char loaddir[MAX_PATH];
    char *classpath, *cp;
    char *path, *p;

    GetModuleFileName(NULL, loaddir, MAX_PATH);
    *(strrchr(loaddir, '\\')) = '\0';

    classpath = getenv("CLASSPATH");
    if (classpath == NULL) {
	classpath = "";
    }
    cp = (char *)malloc(7 * strlen(loaddir) + strlen(classpath) + 200);
#ifndef JMFCUST
    sprintf(cp, "CLASSPATH=%s\\..\\lib\\jmf.jar;%s\\..\\lib\\sound.jar;%s",
	    loaddir, loaddir, classpath);
#else
    sprintf(cp, "CLASSPATH=%s\\..\\lib\\customizer.jar;%s\\..\\lib\\jmf.jar;%s\\..\\lib\\sound.jar;%s",
	    loaddir, loaddir, loaddir, classpath);
#endif
    putenv(cp);

    path = getenv("PATH");
    if (path == NULL) {
	path = "";
    }
    p = (char *)malloc(strlen(loaddir) + strlen(path) + 200);
    sprintf(p, "PATH=%s\\..\\lib;%s", loaddir, path);
    putenv(p);
}

static void
usage()
{
    fprintf(stdout, "Usage: jmlaunch [-jre <path-to-java>]\n"
	    "\t[ <URL> | <file> | <MRL> ] ...\n");
}

#ifdef WINMAIN
int WINAPI
WinMain(HINSTANCE inst, HINSTANCE previnst, LPSTR cmdline, int cmdshow)
{
    return win_main(__argc, __argv);
}
#endif
