/*
 * @(#)V4LCapture.c	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>

#include <com_sun_media_protocol_v4l_V4LCapture.h>
#include <linux/videodev.h>


#define GRAB_METHOD_MMAP 1
#define GRAB_METHOD_READ 2

/* V4LCapture Structure */

typedef struct _v4lcapture {

    int fd;
    char *buf;
    struct video_capability vcap;
    struct video_mmap mmap;
    struct video_mbuf mbuf;
    int palette;
    int depth;
    int width;
    int height;
    float framerate;
    int grabMethod;
    int frameSize;
    int currentFrame;
    struct video_mmap mm[VIDEO_MAX_FRAME];
} V4LCapture;

/* JNI Utilities */

JNIEXPORT jint 
JNIGetIntField(JNIEnv *env, jobject obj, char *field)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "I");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "GetIntField() failed: %s\n", field);
	return -1;
    }
    return (*env)->GetIntField(env, obj, fid);
}


JNIEXPORT void 
JNISetIntField(JNIEnv *env, jobject obj, char *field, int val)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "I");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "SetIntField() failed: %s\n", field);
	return;
    }
    (*env)->SetIntField(env, obj, fid, val);
}


JNIEXPORT jlong 
JNIGetLongField(JNIEnv *env, jobject obj, char *field)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "J");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "GetLongField() failed: %s\n", field);
	return -1;
    }
    return (*env)->GetLongField(env, obj, fid);
}


JNIEXPORT void 
JNISetLongField(JNIEnv *env, jobject obj, char *field, jlong val)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "J");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "SetLongField() failed: %s\n", field);
	return;
    }
    (*env)->SetLongField(env, obj, fid, val);
}


JNIEXPORT jfloat
JNIGetFloatField(JNIEnv *env, jobject obj, char *field)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "F");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "GetFloatField() failed: %s\n", field);
	return -1;
    }
    return (*env)->GetFloatField(env, obj, fid);
}


JNIEXPORT void 
JNISetFloatField(JNIEnv *env, jobject obj, char *field, jfloat val)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, "F");
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "SetFloatField() failed: %s\n", field);
	return;
    }
    (*env)->SetFloatField(env, obj, fid, val);
}


JNIEXPORT jobject 
JNIGetObjectField(JNIEnv *env, jobject obj, char *field, char *type)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, type);
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "GetObjectField() failed: %s, %s\n", field, type);
	return NULL;
    }
    return (*env)->GetObjectField(env, obj, fid);
}


JNIEXPORT void 
JNISetObjectField(JNIEnv *env, jobject obj, char *field, char *type, jobject val)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, field, type);
    (*env)->DeleteLocalRef(env, cls);
    if (fid == NULL) {
	fprintf(stderr, "SetObjectField() failed: %s, %s\n", field, type);
	return;
    }
    (*env)->SetObjectField(env, obj, fid, val);
}

JNIEXPORT void
JNISetStringField(JNIEnv *env, jobject obj, char *field, char *string)
{
    jstring newstring = (*env)->NewStringUTF(env, string);
    JNISetObjectField(env, obj, field, "Ljava/lang/String;", newstring);
}

JNIEXPORT void
JNIGetStringField(JNIEnv *env, jobject obj, char *field, char *string)
{
    char *cstring;
    jstring javastring = JNIGetObjectField(env, obj, field, "Ljava/lang/String;");
    cstring = (*env)->GetStringUTFChars(env, javastring, 0);
    strcpy(string, cstring);
    (*env)->ReleaseStringUTFChars(env, javastring, cstring);
}

    
/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    nOpen
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_nOpen(JNIEnv *env,
						 jobject javapeer,
						 jint devNo)
{
    char devname[80];
    int fd;
    V4LCapture * capture;

    /* Map the device number to a device path */
    sprintf(devname, "/dev/video%i", devNo);
    /* Try to open the device */
    fd = open(devname, O_RDWR);

    if (fd <= 0)
	return 0;
    else {
	/* Allocate a structure for the native peer and return the pointer */
	capture = (V4LCapture *) malloc(sizeof(V4LCapture));
	capture->fd = fd;
	/* Don't know which method to use yet */
	capture->grabMethod = 0;
	/* Get the device capabilities */
	ioctl(capture->fd, VIDIOCGCAP, &(capture->vcap));

	return (jint) capture;
    }
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    nClose
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_nClose(JNIEnv *env,
						  jobject javapeer)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    if (capture == NULL)
	return -1;

    /* Close the device */
    close(capture->fd);

    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    getCapability
 * Signature: (Lcom/sun/media/protocol/v4l/VCapability;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_getCapability(JNIEnv *env,
							 jobject javapeer,
							 jobject jvcap)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    
    if (capture == NULL)
	return -1;

    /* Copy the device properties to the java class jvcap */
    JNISetIntField(env, jvcap, "type", capture->vcap.type);
    JNISetIntField(env, jvcap, "channels", capture->vcap.channels);
    JNISetIntField(env, jvcap, "audios", capture->vcap.audios);
    JNISetIntField(env, jvcap, "maxwidth", capture->vcap.maxwidth);
    JNISetIntField(env, jvcap, "maxheight", capture->vcap.maxheight);
    JNISetIntField(env, jvcap, "minwidth", capture->vcap.minwidth);
    JNISetIntField(env, jvcap, "minheight", capture->vcap.minheight);
    JNISetStringField(env, jvcap, "name", capture->vcap.name);

    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    getChannel
 * Signature: (Lcom/sun/media/protocol/v4l/VChannel;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_getChannel(JNIEnv *env,
						      jobject javapeer,
						      jobject jvchan)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    struct video_channel vchan;
    
    if (capture == NULL)
	return -1;

    vchan.channel = JNIGetIntField(env, jvchan, "channel");
    
    if (ioctl(capture->fd, VIDIOCGCHAN, &vchan) < 0) {
	return -1;
    }

    JNISetIntField(env, jvchan, "channel", vchan.channel);
    JNISetIntField(env, jvchan, "tuners", vchan.tuners);
    JNISetIntField(env, jvchan, "flags", vchan.flags);
    JNISetIntField(env, jvchan, "type", vchan.type);
    JNISetIntField(env, jvchan, "norm", vchan.norm);
    JNISetStringField(env, jvchan, "name", vchan.name);
    
    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    setChannel
 * Signature: (Lcom/sun/media/protocol/v4l/VChannel;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_setChannel(JNIEnv *env,
						      jobject javapeer,
						      jobject jvchan)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    struct video_channel vchan;
    
    if (capture == NULL)
	return -1;

    /* Copy the java structure to the C structure */
    vchan.channel = JNIGetIntField(env, jvchan, "channel");
    vchan.tuners = JNIGetIntField(env, jvchan, "tuners");
    vchan.flags = JNIGetIntField(env, jvchan, "flags");
    vchan.type = JNIGetIntField(env, jvchan, "type");
    vchan.norm = JNIGetIntField(env, jvchan, "norm");
    JNIGetStringField(env, jvchan, "name", vchan.name);

    /* Set the channel information using V4L */
    if (ioctl(capture->fd, VIDIOCSCHAN, &vchan) < 0) {
	return -1;
    }
    
    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    getPicture
 * Signature: (Lcom/sun/media/protocol/v4l/VPicture;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_getPicture(JNIEnv *env,
						      jobject javapeer,
						      jobject jvpict)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    struct video_picture vpict;
    
    if (capture == NULL)
	return -1;

    if (ioctl(capture->fd, VIDIOCGPICT, &vpict))
	return -1;

    JNISetIntField(env, jvpict, "brightness", vpict.brightness);
    JNISetIntField(env, jvpict, "hue", vpict.hue);
    JNISetIntField(env, jvpict, "colour", vpict.colour);
    JNISetIntField(env, jvpict, "contrast", vpict.contrast);
    JNISetIntField(env, jvpict, "whiteness", vpict.whiteness);
    JNISetIntField(env, jvpict, "depth", vpict.depth);
    JNISetIntField(env, jvpict, "palette", vpict.palette);

    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    setPicture
 * Signature: (Lcom/sun/media/protocol/v4l/VPicture;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_setPicture(JNIEnv *env,
						      jobject javapeer,
						      jobject jvpict)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    struct video_picture vpict;
    
    if (capture == NULL)
	return -1;

    vpict.brightness = JNIGetIntField(env, jvpict, "brightness");
    vpict.hue        = JNIGetIntField(env, jvpict, "hue");
    vpict.colour     = JNIGetIntField(env, jvpict, "colour");
    vpict.contrast   = JNIGetIntField(env, jvpict, "contrast");
    vpict.whiteness  = JNIGetIntField(env, jvpict, "whiteness");
    vpict.depth      = JNIGetIntField(env, jvpict, "depth");
    vpict.palette    = JNIGetIntField(env, jvpict, "palette");

    if (ioctl(capture->fd, VIDIOCSPICT, &vpict) < 0)
	return -1;
    
    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    getTuner
 * Signature: (Lcom/sun/media/protocol/v4l/VTuner;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_getTuner(JNIEnv *env,
						    jobject javapeer,
						    jobject jvtuner)
{
    /* TODO */
    return -1;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    setTuner
 * Signature: (Lcom/sun/media/protocol/v4l/VTuner;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_setTuner(JNIEnv *env,
						    jobject javapeer,
						    jobject jvtuner)
{
    /* TODO */
    return -1;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    getFrequency
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_getFrequency(JNIEnv *env,
							jobject	javapeer)
{
    /* TODO */
    return -1;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    setFrequency
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_setFrequency(JNIEnv *env,
							jobject javapeer,
							jint jfreq)
{
    /* TODO */
    return -1;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    setFormat
 * Signature: (IIIIF)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_setFormat(JNIEnv *env,
						     jobject javapeer,
						     jint jdepth,
						     jint jpalette,
						     jint jwidth,
						     jint jheight,
						     jfloat jrate)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    struct video_picture vpict;
    
    if (capture == NULL)
	return -1;

    /* Filter out bad values */
    if ( jwidth > capture->vcap.maxwidth ||
	 jwidth < capture->vcap.minwidth ||
	 jheight > capture->vcap.maxheight ||
	 jheight < capture->vcap.minheight ||
	 jrate > 30.0 ||
	 jrate < 0.5 )
	return -1;

    /* Get the current video_picture values */
    if (ioctl(capture->fd, VIDIOCGPICT, &vpict) < 0)
	return -1;
	
    /* Check if device accepts the depth and palette */
    vpict.palette = jpalette;
    vpict.depth = jdepth;
    if (ioctl(capture->fd, VIDIOCSPICT, &vpict) < 0)
	return -1;

    /* format accepted, keep copies of current values */

    capture->depth = jdepth;
    capture->palette = jpalette;
    capture->width = jwidth;
    capture->height = jheight;
    capture->framerate = jrate;

    capture->frameSize = (jwidth * jheight * jdepth) / 8;

    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    start
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_start(JNIEnv *env,
						 jobject javapeer)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    int i;
    
    if (capture == NULL)
	return -1;

    if (ioctl(capture->fd, VIDIOCGMBUF, &(capture->mbuf)) < 0) {
	/* MBUF not working , assume we have to use read() */
	capture->grabMethod = GRAB_METHOD_READ;
    } else {
	capture->grabMethod = GRAB_METHOD_MMAP;
	/* mmap and get the buffer address */
	capture->buf = (char *) mmap(0, capture->mbuf.size,
				     PROT_READ | PROT_WRITE,
				     MAP_SHARED, capture->fd, 0);
	/* Update the video_mmap structure for each frame and start the capture */
	for (i = 0; i < capture->mbuf.frames; i++) {
	    capture->mm[i].frame = i;
	    capture->mm[i].height = capture->height;
	    capture->mm[i].width = capture->width;
	    capture->mm[i].format = capture->palette;

	    if (ioctl(capture->fd, VIDIOCMCAPTURE, &(capture->mm[i])) < 0) {
		return -1;
	    }
	}
	capture->currentFrame = 0;
    }

    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    stop
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_stop(JNIEnv *env,
						jobject javapeer)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    int i;
    
    if (capture == NULL)
	return -1;

    if (capture->grabMethod == GRAB_METHOD_MMAP) {
	/* Free all the frames */
	for (i = 0; i < capture->mbuf.frames; i++) {
	    ioctl(capture->fd, VIDIOCSYNC, &i);
	}
	/* unmap the memory */
	munmap(capture->buf, capture->mbuf.size);
	capture->grabMethod = 0;
    }

    return 0;
}

/*
 * Class:     com_sun_media_protocol_v4l_V4LCapture
 * Method:    readNextFrame
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_protocol_v4l_V4LCapture_readNextFrame(JNIEnv *env,
							 jobject javapeer,
							 jbyteArray jbuffer,
							 jint joffset,
							 jint jlength)
{
    V4LCapture *capture = (V4LCapture*) JNIGetIntField(env, javapeer, "nativePeer");
    char *outbuf;
    
    if (capture == NULL)
	return -1;

    if (capture->grabMethod == GRAB_METHOD_MMAP) {
	if (ioctl(capture->fd, VIDIOCSYNC, &(capture->currentFrame)) < 0) {
	    /*fprintf(stderr, "Error in VIDIOCSYNC\n");*/
	    /*capture->currentFrame = (capture->currentFrame + 1) % capture->mbuf.frames;*/
	    return -1;
	} else {
	    outbuf = (char *) (*env)->GetByteArrayElements(env, jbuffer, 0);
	    if (outbuf == NULL)
		return -1;
	    memcpy(outbuf + joffset,
		   capture->buf + capture->mbuf.offsets[capture->currentFrame],
		   capture->frameSize);
	    (*env)->ReleaseByteArrayElements(env, jbuffer, (jbyte *) outbuf, 0);

	    if (ioctl(capture->fd, VIDIOCMCAPTURE, &(capture->mm[capture->currentFrame])) < 0)
		fprintf(stderr, "Error in VIDIOCMCAPTURE\n");
	    
	    capture->currentFrame = (capture->currentFrame + 1) % capture->mbuf.frames;
	    return capture->frameSize;
	}
    } else if (capture->grabMethod == GRAB_METHOD_READ) {
	int bytesRead;
	
	outbuf = (char *) (*env)->GetByteArrayElements(env, jbuffer, 0);
	if (outbuf == NULL)
	    return -1;
	bytesRead = read(capture->fd, outbuf + joffset, jlength);
	(*env)->ReleaseByteArrayElements(env, jbuffer, (jbyte *) outbuf, 0);
	return bytesRead;
    }
    
    return -1;

}
