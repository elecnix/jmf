#ifndef OPI_H
#define OPI_H


/* The following values are or'ed together and returned as 
 * the capabilities of the device   
 */
#define CAP_CAPTURE         0x01
#define CAP_COMPRESSION     0x02
#define CAP_DECOMPRESSION   0x04
#define CAP_OUTPUT          0x08

#define VID_SHIFT 0
#define CAP_VID_CAPTURE         (CAP_CAPTURE << VID_SHIFT)
#define CAP_VID_COMPRESSION     (CAP_COMPRESSION << VID_SHIFT)
#define CAP_VID_DECOMPRESSION   (CAP_DECOMPRESSION << VID_SHIFT)
#define CAP_VID_OUTPUT          (CAP_OUTPUT << VID_SHIFT)

#define AUD_SHIFT 4
#define CAP_AUD_CAPTURE         (CAP_CAPTURE << AUD_SHIFT)
#define CAP_AUD_COMPRESSION     (CAP_COMPRESSION << AUD_SHIFT)
#define CAP_AUD_DECOMPRESSION   (CAP_DECOMPRESSION << AUD_SHIFT)
#define CAP_AUD_OUTPUT          (CAP_OUTPUT << AUD_SHIFT)

#define OTHER_SHIFT 8
#define CAP_OTHER_CAPTURE         (CAP_CAPTURE << OTHER_SHIFT)
#define CAP_OTHER_COMPRESSION     (CAP_COMPRESSION << OTHER_SHIFT)
#define CAP_OTHER_DECOMPRESSION   (CAP_DECOMPRESSION << OTHER_SHIFT)
#define CAP_OTHER_OUTPUT          (CAP_OUTPUT << OTHER_SHIFT)

#if defined(__DECCXX) || defined(__DECC)
#include <limits.h>
#include <X11/Xlib.h>
#include <poll.h>
#elif SOLARIS
#include <limits.h>
#include <X11/Xlib.h>
#include <poll.h>
#include <thread.h>
#include <synch.h>
#elif _WIN32
#include <windows.h>
#endif


#if defined(__DECCXX) || defined(__DECC)
#define DllExport
#define FncExport
#define CALLBACK
#define OPISleep(x) do { struct pollfd myfd[1]; myfd[0].fd = 0; myfd[0].events = 0; poll(myfd,1,x); } while (0)
#elif SOLARIS
#define DllExport
#define FncExport
#define CALLBACK
#define OPISleep(x) poll(0,0,x)
#elif _WIN32
#define DllExport __declspec( dllexport )  
#define FncExport
#define OPISleep(x) Sleep(x)
#endif


#if defined(__DECCXX) || defined(__DECC)
typedef struct
{
  Display *xdisplay;
  Window xwindow;
} WND;

typedef void * HWND;

#endif /* __DECCXX  or __DECC */

typedef unsigned char OPIByte;
typedef int OPIBool;

#ifndef FALSE
#define FALSE 0
#define TRUE 1
#endif

typedef void* OTIHANDLE;     

#if defined(__cplusplus) &&  !defined(OPI_C_BINDINGS)                                     
// Some errors can be checked with return values. 
// Other Errors are handled with exceptions
class OPIMemoryError { };  // Exception for out of memory   
class OPIBufferError { };  // Exception for buffer misuse
class OPIInternalError {}; // Exception for internal library errors
class OPIUserError {}; // Exception for user errors (bad parameters, etc)
class OPITimeOut {};   // Time out while waiting on an event.

class DllExport OPIObject 
{
public:
  OTIHANDLE HND;
  ~OPIObject();
};


class DllExport OPINameList: public OPIObject{
public:
  OPINameList();
  int getNumItems();
  char* getItemName(int i);
  int nameNdx(const char* name);
};

class DllExport OPIFormat {
public:
  enum { Video, Audio } formatType;
  OPIBool externalInput;
  OPIBool externalOutput;
  char* compression;
  void *param;        // Additional parameter dependent on compression format
  char *deviceName;   // Force which device to use
  virtual OPIFormat* dup() = 0;
};

#define OPI_PACKET_LOSS (1)
class DllExport OPIBuffer {
public: 
  OPIBuffer* pNext;   // reserved for internal library use
  void* reserved;     // reserved for internal library use
  OPIBuffer(int numBytes = 0);
  ~OPIBuffer();
  OPIByte* data;
  long fsize;         // size of data in buffer
private:
  long size;	      // size of actual buffer
public:
  long frame_slices;  // -1 means unknown or partial
  long frame_count;
  void* user_data;    // can be used by the user
  unsigned long flags;
  long bufferSize() { return size; };
  void setSize(long s) { size = s; };
};
 
 

typedef void (CALLBACK *dataDoneCallBack)(OPIBuffer *buffer, void* userData);  // done w/ input buffer
typedef void (CALLBACK *dataReadyCallBack)(OPIBuffer *buffer, void* userData); // output buffer ready

#endif

#if defined(__cplusplus) &&  !defined(OPI_C_BINDINGS)
enum OPIStreamType { INSTREAM = 1, OUTSTREAM = 2, PIPE = 3 }; 
#else
typedef enum { INSTREAM = 1, OUTSTREAM = 2, PIPE = 3 } OPIStreamType;
#endif

#if defined(__cplusplus) &&  !defined(OPI_C_BINDINGS)

class DllExport OPIStream : public OPIObject
{
public:
  class IllegalUse { } ;  // Exception: Using an instream as an out or vice versa

  // Create a Stream using internal or external buffers.
  // If using external buffers, it is the user's responsibility 
  // to call setBuffer() nbuffer times to set up the proper data pointers.
  OPIStream(OPIStreamType stype,
             long num_buffers, 
             long buffer_size, 
             OPIBool internal = TRUE, 
             OPIBool parse = TRUE);
  ~OPIStream();

  void setBuffer(OPIByte* data, long numBytes);     // Set a buffer's data area    
 
  // Input routines
  OPIBuffer* getBuffer();     // get a buffer to put data in
  OPIBool addData(OPIBuffer *buffer); // give the buffer to the actor
  // If the DataDoneCallback is set, then then no need to call getBuffer after
  // getting the initial buffers.
  void setDataDoneCallback(dataDoneCallBack cb, void* userData);  
  
  // Output routines
  // The data may be polled or driven by callbacks.  
  // If the dataReadyCallback is set, then
  // the data is gotten through callbacks.
  void setDataReadyCallback(dataReadyCallBack cb, void* userData);
  OPIBuffer* getData();
  void returnBuffer(OPIBuffer *buffer);// done with this buffer, 
	OPIBool isInternal();
};

class OPIDevice;

// TODO:  ??? Should we add a tag to avoid misuse of types?
#endif

#if defined(__cplusplus) &&  !defined(OPI_C_BINDINGS)
enum OPIAttributeType { PTR_ATTRIBUTE, STRING_ATTRIBUTE, 
  DOUBLE_ATTRIBUTE, INT_ATTRIBUTE };
#else
typedef enum { PTR_ATTRIBUTE, STRING_ATTRIBUTE, 
  DOUBLE_ATTRIBUTE, INT_ATTRIBUTE } OPIAttributeType;
#endif

#if defined(__cplusplus) &&  !defined(OPI_C_BINDINGS)

class OPIAttribute {
public:
  union {
    const void* ptr;
    const char* s;
    double d;
    long i;
  };

  OPIAttribute() { };
  OPIAttribute(const char* ss) { s = ss; };
  OPIAttribute(const void* pp) { ptr = pp; };
  OPIAttribute(const long ii) { i = ii; };
  OPIAttribute(const double dd) { d = dd; };
};

#define ATTR_MIN (OPIAttribute((long)LONG_MIN))
#define ATTR_MAX (OPIAttribute((long)LONG_MAX))

class OPIAttributeInfo {
public:
  OPIAttributeType getAttributeType() { return type; };
  OPIBool getGettable() { return gettable; };
  OPIBool getSettable() { return settable; };
  OPIAttributeType getType() { return type; };
  OPIAttribute getMin() { return min; };
  OPIAttribute getMax() { return max; };

  OPIAttributeInfo(OPIBool get, OPIBool set, OPIAttributeType typep, 
    OPIAttribute minp = OPIAttribute((long)0), OPIAttribute maxp = OPIAttribute((long)0))
  { gettable = get;
    settable = set;
    type = typep;
    min = minp;
    max = maxp;
  }

private:
  OPIBool gettable;
  OPIBool settable;
  OPIAttributeType type;
  OPIAttribute min;
  OPIAttribute max;
};
  


class DllExport OPIDeviceInstance : public OPIObject
{

public:
  // Cannot be directly created by application.
  // Must come from OPISystem
  OPIDeviceInstance(OPIFormat* inFmt, OPIFormat* outFmt, 
               OPIFormat* monitorFormat = NULL);
  OPIDeviceInstance();
  OPIDeviceInstance(OTIHANDLE hnd);
  ~OPIDeviceInstance();
public:
  OPIDevice* getDevice();
  char* getName();
  OPIBool setAttribute(char* name, OPIAttribute value);
  OPIBool getAttribute(char* name, OPIAttribute* value);
  OPIBool getAttributeInfo(char* name, OPIAttributeInfo** info);
  OPIFormat *inFmt;
  OPIFormat *outFmt;
  OPINameList availableAttributes;
};

// A device represents the type of device.  For instance, the
// name of a Osprey-1000 may be "o1k".
// The name of a device instance represents which version of the device
// For instance, the second Osprey-1000 in a system might have a device
// instance name of "/dev/o1k1"
class DllExport OPIDevice : public OPIObject
{
protected:
  OPIDevice();
public:
  virtual ~OPIDevice();
  char* getName();

  OPIDeviceInstance* getInstanceByName(const char* name);
  OPIDeviceInstance* getInstanceByIndex(int index);
  OPINameList instances;
  // TODO:  Add ability to list available attributes, types, and ranges

  long getVideoCapabilities();
  long getAudioCapabilities();

  void getCoreVersion(char *);
  void getVersion(char *);
};
 
typedef void (*ErrorHandler)(int);        
class DllExport OPISystem : public OPIObject
{
public:
  OPISystem();
  ~OPISystem();
  OPIDevice* getDeviceByName(const char* device_name);
  OPIDeviceInstance* getControlInstance(const char* device_name, 
    const char* instance_name);

  OPINameList devices;
  void setDebugLevel(int level);
  void getVersion(char *);
};


// OPI stages may only be referenced by pointer.
// There is no external definition for OPIStage
typedef void OPIStage;
#endif

#if defined(__cplusplus) &&  !defined(OPI_C_BINDINGS)

class DllExport OPIPipeline : public OPIObject
{
public:
  OPIPipeline(OPISystem* msys);
  ~OPIPipeline();
  OPIStream* getInStream();
  OPIStream* getOutStream(OPIStage* sh = NULL);
  void setInStream(OPIStream *);
  void setOutStream(OPIStream *, OPIStage* sh = NULL);
  // Operations that affect the pipeline execution
  void start();
  void pause();
  void resume();
  void stop();                                    
  // Creating a pipeline
  OPIStage* firstStage(int capability, OPIFormat *inFmt, OPIFormat *outFmt);
  // The addStage routine can be used to split a pipe as well.
  // Just addStage to a stage that has already been added to.
  OPIStage* addStage(OPIStage* sh, int capability, OPIFormat *inFmt, OPIFormat *outFmt) ;
  
  //  After constructing a pipeline, you map it to the appropriate devices.
  //  Then, optionally, you can specify buffers explicitly.
  //  Then, when finishConstruction is called, any implicitly defined
  //  buffers are set up.
  OPIBool mapToDevices();
  OPIBool finishConstruction();

  // Modifying components of a pipeline.
  OPIDeviceInstance* getDeviceFromHandle(OPIStage* sh);
  void setThreadPriority(int priority);
};

class DllExport OPIFormatVideo : public OPIFormat {
public:
  OPIFormatVideo();
  int width;
  int height;
  int depth;
  OPIFormat* dup();
};

class DllExport OPIFormatAudio : public OPIFormat {
public:
  OPIFormatAudio();
  OPIFormat* dup();
};

class DllExport OPICompressionDevice : public OPIDeviceInstance{
protected:
  OPICompressionDevice(OTIHANDLE hnd);
private:
  char*		      getCompType();
};

class DllExport OPICompressionDeviceVideo : public OPICompressionDevice{
protected:
  OPICompressionDeviceVideo(OTIHANDLE hnd);
public:
  int           getQuality();
  void          setQuality(int q);
  void          setBitRate(int bitspersecond);
  int           getBitRate();
  void          requestFastPictureUpdate();
  OPIBool       intraEncoded();
};

class DllExport OPICompressionDeviceAudio : public OPICompressionDevice{
protected:
  OPICompressionDeviceAudio(OTIHANDLE hnd);
public:
  int           getSampleBlockSize();
};

class DllExport OPICompressionDeviceH261: public OPICompressionDeviceVideo{
protected:
  OPICompressionDeviceH261(OTIHANDLE hnd);
public:
  void          setBlockRefreshPeriod(int milliseconds);
  int           getBlockRefreshPeriod();
  void          setPIP(OPIBool PIP);
  OPIBool       getPIP();
  void          setPIPLocation(int location);
  int           getPIPLocation();
};
class DllExport OPICompressionDeviceH261P: public OPICompressionDeviceVideo{
protected:
  OPICompressionDeviceH261P(OTIHANDLE hnd);
public:
  void          setBlockRefreshPeriod(int milliseconds);
  int           getBlockRefreshPeriod();
  void          setPIP(OPIBool PIP);
  OPIBool       getPIP();
  void          setPIPLocation(int location);
  int           getPIPLocation();
};

class DllExport OPICompressionDeviceJPEG: public OPICompressionDeviceVideo{
protected:
  OPICompressionDeviceJPEG(OTIHANDLE hnd);
public:
};

class DllExport OPICompressionDeviceH263: public OPICompressionDeviceVideo{
protected:
  OPICompressionDeviceH263(OTIHANDLE hnd);
public:
};
class DllExport OPICompressionDeviceH263P: public OPICompressionDeviceVideo{
protected:
  OPICompressionDeviceH263P(OTIHANDLE hnd);
public:
};

class DllExport OPIDecompressionDevice : public OPIDeviceInstance{  
protected:
  OPIDecompressionDevice(OTIHANDLE hnd);
private:
  char* getCompType();
};

class DllExport OPIDecompressionDeviceVideo : public OPIDecompressionDevice{  
protected:
  OPIDecompressionDeviceVideo(OTIHANDLE hnd);
private:
  OPIBool intraEncoded();
};

class DllExport OPIDecompressionDeviceAudio : public OPIDecompressionDevice{  
protected:
  OPIDecompressionDeviceAudio(OTIHANDLE hnd);
public:
  int           getSampleBlockSize();
private:
};

class DllExport OPIDecompressionDeviceH261 : public OPIDecompressionDeviceVideo{
protected:
  OPIDecompressionDeviceH261(OTIHANDLE hnd);
};

class DllExport OPIDecompressionDeviceH261P : public OPIDecompressionDeviceVideo{
protected:
  OPIDecompressionDeviceH261P(OTIHANDLE hnd);
};

class DllExport OPIDecompressionDeviceH263 : public OPIDecompressionDeviceVideo{
protected:
  OPIDecompressionDeviceH263(OTIHANDLE hnd);
};
class DllExport OPIDecompressionDeviceH263P : public OPIDecompressionDeviceVideo{
protected:
  OPIDecompressionDeviceH263P(OTIHANDLE hnd);
};

class DllExport OPIDecompressionDeviceJPEG : public OPIDecompressionDeviceVideo{
protected:
  OPIDecompressionDeviceJPEG(OTIHANDLE hnd);
};

class DllExport OPICaptureDevice : public OPIDeviceInstance{
protected:
  OPICaptureDevice(OTIHANDLE hnd);
private:
  char *getCompType();
};

class DllExport OPICaptureDeviceVideo : public OPICaptureDevice{
protected:
  OPICaptureDeviceVideo(OTIHANDLE hnd);
public:
  void setFrameRate(int msecPerFrame);
  int  getFrameRate();
  void setOverlayRate(int msecPerFrame);
  int  getOverlayRate();
};

class DllExport OPICaptureDeviceAudio : public OPICaptureDevice{
protected:
  OPICaptureDeviceAudio(OTIHANDLE hnd);
public:
  void setSampleRate(int hz);
  int  getSampleRate();
  void setChannels(int channels);
  int  getChannels();
};

class DllExport OPIOutputDevice : public OPIDeviceInstance{ 
protected:
  OPIOutputDevice(OTIHANDLE hnd);
};  

class DllExport OPIOutputDeviceVideo : public OPIOutputDevice{ 
protected:
  OPIOutputDeviceVideo(OTIHANDLE hnd);
};  

class DllExport OPIOutputDeviceAudio : public OPIOutputDevice{ 
protected:
  OPIOutputDeviceAudio(OTIHANDLE hnd);
public:
  void setSampleRate(int hz);
  int  getSampleRate();
  void setChannels(int channels);
  int  getChannels();
};  

// Definition of OPIColorcube class
//
//  An OPIColorcube can be used to set or get values for
//  an X Colormap (for X systems), or to get the values
//  used in an RGB8 capture (for X or Windows).
//
//  Colorcubes are defined by 3 bands, the starting colorspace,
//  and the offset (i.e. RGB 5:8:5, offset 10, would be an
//  RGB colorcube with 8 values of G for each 5 of R and 5 of B.
//  The colorcube will start at offset <offset>.

//  Each band can have a limit range (i.e. 10 - 230, instead
//  of 0-255).  This allows the values to be allocated across
//  a smaller range, providing better quality in the mid range.
//
//  The getBand call only returns the valid values.
//  For instance, if you have an offset of 10, then the band0[0]
//  would be the value assigned to pixel index 10 for red.

class DllExport OPIColorcube : public OPIObject {
public:
  enum ColorSpace{ YUV, RGB, GRAY };
  OPIColorcube(ColorSpace cs, int offset, int mult0, int mult1, int mult2);
  ~OPIColorcube();
  int getOffset();
  OPIColorcube::ColorSpace getColorspace();
  void getMult(int &mult0, int &mult1, int &mult2);
  void setLimits(int band, int min, int max);
  void getLimits(int band, int &min, int &max);
  unsigned char* getColors(int i);
  int getSize();

  // There are standard system defined colorcubes that are used
  // if you do not specify otherwise.  These can be gotten
  // with this call.
  static OPIColorcube* getDefaultColorcube(ColorSpace cs);
};

#ifdef SOLARIS
typedef struct
{
  Display *xdisplay;
  Window xwindow;
} WND;
typedef void * HWND;
#endif
//
// Definition of OPI Window class
//
// OPIWindow is a representation of a window.  An application
// uses OPIWindow to define	where stream output should be displayed
//
class DllExport OPIWindow	: public OPIObject
{
public:
  OPIWindow(int x, int y, int width, int height, char *name);
  OPIWindow(void *window);
	OPIWindow(struct HWND__ *window);

  ~OPIWindow();
  HWND getHandle();
  OPIBool isInternal();
  void resize();

  // If you have an 8bit XWindow and do not want to use the
  // default colorcube, use setColorcube.
  void setColorcube(OPIColorcube* cc);

  // To find out what colorcube OPI assigned to a window, use
  // getColorcube.
  OPIColorcube* getColorcube();
};


#endif 
#endif


