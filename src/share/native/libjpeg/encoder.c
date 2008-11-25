/*
 * @(#)encoder.c	1.15 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

/*
 * encoder.c
 *
 * This file illustrates how to use the IJG code as a subroutine library
 * to read or write JPEG image files.  You should look at this code in
 * conjunction with the documentation file libjpeg.doc.
 *
 * This code will not do anything useful as-is, but it may be helpful as a
 * skeleton for constructing routines that call the JPEG library.  
 *
 * We present these routines in the same coding style used in the JPEG code
 * (ANSI function definitions, etc); but you are of course free to code your
 * routines in a different style if you prefer.
 */

#include <stdio.h>
#include "com_sun_media_codec_video_jpeg_NativeEncoder.h"
#include "jpeglib.h"
#include <setjmp.h>
#include "jni-util.h"


/****************************************************************
 * Structs for JPEG encoder
 ****************************************************************/

typedef struct {
    char * data;
    char * tmp_data;
    void * jerr;
    int length;
} jmf_dest_data;

typedef struct {
    struct jpeg_destination_mgr pub; /* public fields */
    
    JOCTET *buffer;		/* start of buffer */
} jmf_destination_mgr;

typedef jmf_destination_mgr * jmf_dest_ptr;

struct jmf_error_mgr {
    struct jpeg_error_mgr pub;	/* "public" fields */
    jmp_buf setjmp_buffer;	/* for return to caller */
};

typedef struct jmf_error_mgr * jmf_error_ptr;

#define JMF_OUTPUT_BUF_SIZE  4096	/* choose an efficiently fwrite'able size */



/****************************************************************
 * Destination manager implementation for encoder
 ****************************************************************/

METHODDEF(void)
jmf_init_destination (j_compress_ptr cinfo)
{
    jmf_dest_ptr dest = (jmf_dest_ptr) cinfo->dest;

    /* Allocate the output buffer --- it will be released when done with image */
    dest->buffer = (JOCTET *) ((jmf_dest_data *) (cinfo->client_data))->tmp_data;

    dest->pub.next_output_byte = dest->buffer;
    dest->pub.free_in_buffer = JMF_OUTPUT_BUF_SIZE;
}


METHODDEF(boolean)
jmf_empty_output_buffer (j_compress_ptr cinfo)
{
    jmf_dest_ptr dest = (jmf_dest_ptr) cinfo->dest;
    jmf_dest_data *destData = (jmf_dest_data*) cinfo->client_data;

    /* Copy the buffer contents. */
    memcpy(destData->data + destData->length, dest->buffer, JMF_OUTPUT_BUF_SIZE);
    destData->length += JMF_OUTPUT_BUF_SIZE;
    
    dest->pub.next_output_byte = dest->buffer;
    dest->pub.free_in_buffer = JMF_OUTPUT_BUF_SIZE;

    return TRUE;
}

METHODDEF(void)
jmf_term_destination (j_compress_ptr cinfo)
{
    jmf_dest_ptr dest = (jmf_dest_ptr) cinfo->dest;
    size_t datacount = JMF_OUTPUT_BUF_SIZE - dest->pub.free_in_buffer;
    jmf_dest_data *destData = (jmf_dest_data*) cinfo->client_data;

    /* Write any data remaining in the buffer */
    if (datacount > 0) {
	memcpy(destData->data + destData->length, dest->buffer, datacount);
	destData->length += datacount;
    }
}


/****************************************************************
 * Error Manager implementation for encoder
 ****************************************************************/

METHODDEF(void)
jmf_error_exit (j_common_ptr cinfo)
{
    /* cinfo->err really points to a jmf_error_mgr struct, so coerce pointer */
    jmf_error_ptr myerr = (jmf_error_ptr) cinfo->err;
    (myerr->pub.output_message)(cinfo);    
    longjmp(myerr->setjmp_buffer, 1);
}

/****************************************************************
 * encoder creation, invocation and destruction methods
 ****************************************************************/

struct jpeg_compress_struct *
RGB_To_JPEG_init(int width, int height, int quality, int decimation)
{
    struct jmf_error_mgr *jerr;
    struct jpeg_compress_struct *cinfo;
    jmf_destination_mgr *jmf_dest;
    jmf_dest_data *clientData;
    
    clientData = (jmf_dest_data*) malloc(sizeof(jmf_dest_data)); /* Alloc 1 */
    
    clientData->tmp_data = (char *) malloc(JMF_OUTPUT_BUF_SIZE); /* Alloc 2 */
    //clientData->data = (char *) outData;
    //clientData->length = 0;

    /* Step 1: allocate and initialize JPEG compression object */
    cinfo = (struct jpeg_compress_struct *)
	malloc(sizeof(struct jpeg_compress_struct));/* Alloc 3 */

    /* Initialize error parameters */
    jerr = (struct jmf_error_mgr *) malloc(sizeof(struct jmf_error_mgr)); /* Alloc 4 */
    clientData->jerr = (void *) jerr;
    
    cinfo->err = jm_jpeg_std_error(&(jerr->pub));
    (jerr->pub).error_exit = jmf_error_exit;

    /* Establish the setjmp return context for jmf_error_exit to use. */
    if (setjmp(jerr->setjmp_buffer)) {
	/* If we get here, the JPEG code has signaled an error.
	 * We need to clean up the JPEG object, close the input file, and return.
	 */
	jm_jpeg_destroy_compress(cinfo);
	free(jerr);
	free(clientData->tmp_data);
	free(clientData);
	free(cinfo);
	printf("JPEG encoding error!\n");
	return 0;
    }

    /* Now we can initialize the JPEG compression object. */
    jpeg_create_compress(cinfo);

    /* Set up my own destination manager. */
    jmf_dest = (jmf_destination_mgr *) malloc(sizeof(jmf_destination_mgr));/* Alloc 5 */
    
    jmf_dest->pub.init_destination = jmf_init_destination;
    jmf_dest->pub.empty_output_buffer = jmf_empty_output_buffer;
    jmf_dest->pub.term_destination = jmf_term_destination;

    cinfo->dest = (struct jpeg_destination_mgr *) jmf_dest;

    /* Set up client data */
    cinfo->client_data = clientData;
    
    /* Step 3: set parameters for compression */
    
    /* First we supply a description of the input image.
     * Four fields of the cinfo struct must be filled in:
     */
    cinfo->image_width = width; 	/* image width and height, in pixels */
    cinfo->image_height = height;
    cinfo->input_components = 3;		/* # of color components per pixel */
    cinfo->in_color_space = JCS_RGB; 	/* colorspace of input image */

    /* Tell the library to set default parameters */
    jm_jpeg_set_defaults(cinfo);
    /* Default decimation is YUV 4:2:0. If we need 422 or 444, we
       modify the h_samp_factor and v_samp_factor for U and V components. */
    if (decimation >= 1) {
	int hs, vs;
	switch (decimation) {
	case 1: hs = 2; vs = 2; break;
	case 2: hs = 2; vs = 1; break;
	case 4: hs = 1; vs = 1;
	    break;
	}
	(cinfo->comp_info[0]).v_samp_factor = vs;
	(cinfo->comp_info[0]).h_samp_factor = hs;
	(cinfo->comp_info[1]).v_samp_factor = 1;
	(cinfo->comp_info[2]).v_samp_factor = 1;
	(cinfo->comp_info[1]).h_samp_factor = 1;
	(cinfo->comp_info[2]).h_samp_factor = 1;
    }

    /* Now you can set any non-default parameters you wish to.
     * Here we just illustrate the use of quality (quantization table) scaling:
     */
    jm_jpeg_set_quality(cinfo, quality, TRUE /* limit to baseline-JPEG values */
		     );

    return cinfo;
}

void
RGB_To_JPEG_free(struct jpeg_compress_struct *cinfo)
{
    jm_jpeg_destroy_compress(cinfo);
    free(((jmf_dest_data*)cinfo->client_data)->tmp_data);
    free(((jmf_dest_data*)cinfo->client_data)->jerr);
    free(cinfo->client_data);
    free(cinfo->dest);
    free(cinfo);
}

int
RGB_To_JPEG_encode(struct jpeg_compress_struct *cinfo,
		   char *inData, char *outData, int flipped, int quality, int decimation)
{
    struct jmf_error_mgr *jerr = (struct jmf_error_mgr *) cinfo->err;
    JSAMPROW row_pointer[1];	/* pointer to JSAMPLE row[s] */
    int row_stride;		/* physical row width in image buffer */
    jmf_destination_mgr *jmf_dest = (jmf_destination_mgr *) cinfo->dest;
    jmf_dest_data *clientData = (jmf_dest_data *) cinfo->client_data;
    int direction = 1;
    int start = 0;
    
    clientData->data = (char *) outData;
    clientData->length = 0;
    /* Establish the setjmp return context for jmf_error_exit to use. */
    if (setjmp(jerr->setjmp_buffer)) {
	/* If we get here, the JPEG code has signaled an error. */
	return 0;
    }

    jmf_dest->buffer = (JOCTET *) clientData->tmp_data;
    if (quality >= 0) {
	jm_jpeg_set_quality(cinfo, quality, TRUE);
    }

    /* Default decimation is YUV 4:2:0. If we need 422 or 444, we
       modify the h_samp_factor and v_samp_factor for U and V components. */
    if (decimation >= 1) {
	int hs, vs;
	switch (decimation) {
	case 1: hs = 2; vs = 2; break;
	case 2: hs = 2; vs = 1; break;
	case 4: hs = 1; vs = 1;
	    break;
	}
	(cinfo->comp_info[0]).v_samp_factor = vs;
	(cinfo->comp_info[0]).h_samp_factor = hs;
	(cinfo->comp_info[1]).v_samp_factor = 1;
	(cinfo->comp_info[2]).v_samp_factor = 1;
	(cinfo->comp_info[1]).h_samp_factor = 1;
	(cinfo->comp_info[2]).h_samp_factor = 1;
    }

    jm_jpeg_start_compress(cinfo, TRUE);
    row_stride = cinfo->image_width * 3;	/* JSAMPLEs per row in image_buffer */
    if (flipped) {
	direction = -1;
	start = (cinfo->image_height - 1) * row_stride;
    }
    
    while (cinfo->next_scanline < cinfo->image_height) {
	row_pointer[0] = (unsigned char *) &inData[start +
						  direction * cinfo->next_scanline * row_stride];
	(void) jm_jpeg_write_scanlines(cinfo, row_pointer, 1);
    }

    jm_jpeg_finish_compress(cinfo);
    
    return clientData->length;
}

/****************************************************************
 * JNI methods
 ****************************************************************/

/*
 * Class:     com_sun_media_codec_video_jpeg_NativeEncoder
 * Method:    initJPEGEncoder
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_jpeg_NativeEncoder_initJPEGEncoder(JNIEnv *env,
							    jobject jencoder,
							    jint width, jint height,
							    jint quality, jint decimation)
{
    /* Allocate space for a temporary buffer */
    int peer = (int) RGB_To_JPEG_init(width, height, quality, decimation);
    return (jint) peer;
}

/*
 * Class:     com_sun_media_codec_video_jpeg_NativeEncoder
 * Method:    encodeJPEG
 * Signature: (I[BII[BII)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_jpeg_NativeEncoder_encodeJPEG(JNIEnv *env,
							     jobject jencoder,
							     jint jpeer,
							     jobject jinData,
							     jlong jinBytes,
							     jint jwidth,
							     jint jheight,
							     jbyteArray joutData,
							     jint joutLength,
							     jint jquality,
							     jint decimation,
							     jboolean flipped)
{
    char *inData = (char*) jinBytes;
    char *outData = (char *) (*env)->GetByteArrayElements(env, joutData, 0);
    int result;

    if (jinBytes == 0)
	inData = (char *) (*env)->GetByteArrayElements(env, (jbyteArray) jinData, 0);

    result = RGB_To_JPEG_encode((struct jpeg_compress_struct*) jpeer,
				inData, outData, flipped,
				(int)jquality, (int) decimation);

    (*env)->ReleaseByteArrayElements(env, joutData, (signed char*) outData, 0);
    if (jinBytes == 0)
	(*env)->ReleaseByteArrayElements(env, (jbyteArray) jinData,
					 (signed char*) inData, JNI_ABORT);
    return result;    
}

/*
 * Class:     com_sun_media_codec_video_jpeg_NativeEncoder
 * Method:    freeJPEGEncoder
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_jpeg_NativeEncoder_freeJPEGEncoder(JNIEnv *env,
							    jobject jencoder,
							    jint jpeer)
{
    if (jpeer != 0)
	RGB_To_JPEG_free((struct jpeg_compress_struct*) jpeer);
    return (jboolean) 1;
}
