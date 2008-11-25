You can build for 3 platforms -- solaris, win32 and linux

Modify buildjmf.sh-template (for Solaris and Linux) or
buildjmf.bat-template (windows).
Depending upon the platform, you have to set values for 
JAVAHOME, JMFHOME, PLATFORM, MSDEVHOME etc.

Run the build script.

When the build is done, you will find
the shared libraries, jmf.jar, sound.jar in the
build/<platform>/image/lib directory.

To complete the build, run the jmfinit script in the 
build/<platform>/image/bin directory. This will query the capture devices
and update the jmf.properties file.
