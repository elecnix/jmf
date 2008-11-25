/*
 * @(#)vcm.c	1.11 03/04/25
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <windows.h>
#include <vfw.h>
#include <jni-util.h>
#include "com_sun_media_codec_video_vcm_VCM.h"

/*#define DEBUG*/

static unsigned int
fourCCToInt(char *sz)
{
    return *((int*)sz);
}

static unsigned int
jfourCCToInt(JNIEnv *env, jstring jfourcc)
{
    char *szTemp;
    int fourcc;
    
    szTemp = (char *) (*env)->GetStringUTFChars(env, jfourcc, 0);
    if (strcmp(szTemp, "RGB") == 0)
	fourcc = BI_RGB; /* BI_RGB */
    else if (strcmp(szTemp, "RLE8") == 0)
	fourcc = BI_RLE8;
    else if (strcmp(szTemp, "RLE4") == 0)
	fourcc = BI_RLE4;
    else if (strcmp(szTemp, "BITF") == 0)
	fourcc = BI_BITFIELDS;
    else
	fourcc = *((int*)szTemp);
    (*env)->ReleaseStringUTFChars(env, jfourcc, szTemp);
    
    return fourcc;
}

static char *
intToFourCC(unsigned int value, char *sz)
{
    char *p = (char *) &value;
    sz[0] = *p++;
    sz[1] = *p++;
    sz[2] = *p++;
    sz[3] = *p++;
    sz[4] = 0;
    return sz;
}

static void
ICINFO_JICInfo(JNIEnv *env, ICINFO *icInfo, jobject jicInfo)
{
    char szTemp[256];
    jstring tempString;
    int result;

    /* fccType */
    szTemp[0] = 0;
    intToFourCC(icInfo->fccType, szTemp);
    tempString = (*env)->NewStringUTF(env, szTemp);
    SetObjectField(env, jicInfo, "fccType", "Ljava/lang/String;", tempString);

    /* fccHandler */
    szTemp[0] = 0;
    intToFourCC(icInfo->fccHandler, szTemp);
    tempString = (*env)->NewStringUTF(env, szTemp);
    SetObjectField(env, jicInfo, "fccHandler", "Ljava/lang/String;", tempString);

    SetIntField(env, jicInfo, "dwFlags", icInfo->dwFlags);
    SetIntField(env, jicInfo, "dwVersion", icInfo->dwVersion);
    SetIntField(env, jicInfo, "dwVersionICM", icInfo->dwVersionICM);

    /* szName */
    szTemp[0] = 0;
    if (wcslen(icInfo->szName) > 0) {
	result = WideCharToMultiByte(CP_ACP, 0,
				     icInfo->szName, wcslen(icInfo->szName),
				     szTemp, 16, NULL, NULL);
	if (result >= 0)
	    szTemp[result] = 0;
    }
    tempString = (*env)->NewStringUTF(env, szTemp);
    SetObjectField(env, jicInfo, "szName", "Ljava/lang/String;", tempString);
 
    /* szDescription */
    szTemp[0] = 0;
    if (wcslen(icInfo->szDescription) > 0) {
	result = WideCharToMultiByte(CP_ACP, 0, 
				     icInfo->szDescription, wcslen(icInfo->szDescription),
				     szTemp, 128, NULL, NULL);
	if (result >= 0)
	    szTemp[result] = 0;
    }
    tempString = (*env)->NewStringUTF(env, szTemp);
    SetObjectField(env, jicInfo, "szDescription", "Ljava/lang/String;", tempString);
 
    /* szDriver */
    szTemp[0] = 0;
    if (wcslen(icInfo->szDriver) > 0) {
	result = WideCharToMultiByte(CP_ACP, 0, 
				     icInfo->szDriver, wcslen(icInfo->szDriver),
				     szTemp, 128, NULL, NULL);
	if (result >= 0)
	    szTemp[result] = 0;
    }
    tempString = (*env)->NewStringUTF(env, szTemp);
    SetObjectField(env, jicInfo, "szDriver", "Ljava/lang/String;", tempString); 
}

static BITMAPINFOHEADER *
JBitMapInfo_To_BITMAPINFO(JNIEnv *env, jobject jbi, BITMAPINFOHEADER *bi)
{
    jstring jfourCC;
    BITMAPINFOHEADER *temp;
    int biSize;
    
    if (jbi == NULL) {
	return NULL;
    }

    biSize = sizeof(BITMAPINFOHEADER) + (int) GetIntField(env, jbi, "extraSize");
    
    if (bi == NULL) {
	temp = (BITMAPINFOHEADER *) malloc(biSize);
    } else {
	temp = bi;
    }
    
    temp->biWidth = (int) GetIntField(env, jbi, "biWidth");
    temp->biHeight = (int) GetIntField(env, jbi, "biHeight");
    temp->biPlanes = (int) GetIntField(env, jbi, "biPlanes");
    temp->biBitCount = (int) GetIntField(env, jbi, "biBitCount");
    temp->biSizeImage = (int) GetIntField(env, jbi, "biSizeImage");
    temp->biXPelsPerMeter = (int) GetIntField(env, jbi, "biXPelsPerMeter");
    temp->biYPelsPerMeter = (int) GetIntField(env, jbi, "biYPelsPerMeter");
    temp->biClrUsed = (int) GetIntField(env, jbi, "biClrUsed");
    temp->biClrImportant = (int) GetIntField(env, jbi, "biClrImportant");
    temp->biSize = biSize;

    jfourCC = (jstring) GetObjectField(env, jbi, "fourcc", "Ljava/lang/String;");
    temp->biCompression = jfourCCToInt(env, jfourCC);
    if (temp->biSize > sizeof(BITMAPINFOHEADER)) {
	jbyteArray jba = (jbyteArray) GetObjectField(env, jbi, "extraBytes", "[B");
	char * bits = (char *) (*env)->GetByteArrayElements(env, jba, 0);
	memcpy((char*) bi + sizeof(BITMAPINFOHEADER),
	       bits,
	       temp->biSize - sizeof(BITMAPINFOHEADER));
	(*env)->ReleaseByteArrayElements(env, jba, (signed char *) bits, 0);
    }
    return temp;
}

static void
BITMAPINFO_To_JBitMapInfo(JNIEnv *env, BITMAPINFOHEADER *bi, jobject jbi)
{
    char szTemp[5] = {0, 0, 0, 0, 0};
    jstring jfourcc;
    
    if (bi == NULL || jbi == NULL)
	return;

    if (bi->biCompression > 255) {
	intToFourCC(bi->biCompression, szTemp);
    } else {
	switch (bi->biCompression) {
	case BI_RGB:
	    strcpy(szTemp, "RGB"); break;
	case BI_RLE4:
	    strcpy(szTemp, "RLE4"); break;
	case BI_RLE8:
	    strcpy(szTemp, "RLE8"); break;
	case BI_BITFIELDS:
	    strcpy(szTemp, "BITF"); break;
	}
	
    }

    jfourcc = (*env)->NewStringUTF(env, szTemp);
    SetObjectField(env, jbi, "fourcc", "Ljava/lang/String;", jfourcc);
    SetIntField(env, jbi, "biWidth", bi->biWidth);
    SetIntField(env, jbi, "biHeight", bi->biHeight);
    SetIntField(env, jbi, "biPlanes", bi->biPlanes);
    SetIntField(env, jbi, "biBitCount", bi->biBitCount);
    SetIntField(env, jbi, "biSizeImage", bi->biSizeImage);
    SetIntField(env, jbi, "biXPelsPerMeter", bi->biXPelsPerMeter);
    SetIntField(env, jbi, "biYPelsPerMeter", bi->biYPelsPerMeter);
    SetIntField(env, jbi, "biClrUsed", bi->biClrUsed);
    SetIntField(env, jbi, "biClrImportant", bi->biClrImportant);
    if (bi->biSize > sizeof(BITMAPINFOHEADER)) {
	int size = bi->biSize - sizeof(BITMAPINFOHEADER);
	jbyteArray array = (*env)->NewByteArray(env, size);
	if (array != NULL) {
	    SetIntField(env, jbi, "extraSize", size);
	    SetObjectField(env, jbi, "extraBytes", "[B", array);
	}
    }
}

/*
 * Class:     com_sun_media_vfw_VCM
 * Method:    icLocate
 * Signature: (Ljava/lang/String;Ljava/lang/String;Lcom/sun/media/vfw/BitMapInfo;Lcom/sun/media/vfw/BitMapInfo;I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icLocate(JNIEnv *env,
				    jclass  vcm,
				    jstring jfccType,
				    jstring jfccHandler,
				    jobject jbiIn,
				    jobject jbiOut,
				    jint    jicMode)
{
    DWORD fccType = (DWORD) jfourCCToInt(env, jfccType);
    DWORD fccHandler = (DWORD) jfourCCToInt(env, jfccHandler);
    HIC handle;
    BITMAPINFOHEADER *biIn, *biOut = NULL;
    int clrUsed, size;
    
    clrUsed = GetIntField(env, jbiIn, "biClrUsed");

    size = sizeof(BITMAPINFOHEADER) + GetIntField(env, jbiIn, "extraSize");

    biIn = (BITMAPINFOHEADER *) malloc( size );

    JBitMapInfo_To_BITMAPINFO(env, jbiIn, biIn);

    if (jbiOut != NULL) {
	size = sizeof(BITMAPINFOHEADER) + GetIntField(env, jbiOut, "extraSize");
	biOut = (BITMAPINFOHEADER *) malloc( size );
	JBitMapInfo_To_BITMAPINFO(env, jbiOut, biOut);
    }

    handle = ICLocate(fccType, fccHandler, (BITMAPINFOHEADER *) biIn,
		      (BITMAPINFOHEADER *) biOut, (WORD) jicMode);
    
    free(biIn);
    if (biOut)
	free(biOut);
    return (jint) handle;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icOpen
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icOpen(JNIEnv *env,
				  jclass  vcm,
				  jstring jfccType,
				  jstring jfccHandler,
				  jint    jicMode)
{
    DWORD fccType = (DWORD) jfourCCToInt(env, jfccType);
    DWORD fccHandler = (DWORD) jfourCCToInt(env, jfccHandler);
    UINT icMode = (UINT) jicMode;
    HIC handle;

    handle = ICOpen(fccType, fccHandler, icMode);

    return (jint) handle;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icClose
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icClose(JNIEnv *env,
				   jclass  vcm,
				   jint    handle)
{
    jboolean returnVal = (jboolean) 0;
    
    if (handle != 0) {
	// Close the handler
	int result = ICClose((HIC) handle);
	if (result == ICERR_OK)
	    returnVal = (jboolean) 1;
    }
    
    return returnVal;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icGetInfo
 * Signature: (ILcom/sun/media/vfw/ICInfo;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icGetInfo(JNIEnv *env,
				     jclass  vcm,
				     jint    jhandle,
				     jobject jicInfo)
{
    HIC handle = (HIC) jhandle;
    ICINFO icInfo;

    if (handle != 0) {
	if (ICGetInfo(handle, &icInfo, sizeof(icInfo)) != 0) {
	    ICINFO_JICInfo(env, &icInfo, jicInfo);
	    return 1;
	}
    }
    return 0;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icInfo
 * Signature: (Ljava/lang/String;Ljava/lang/String;Lcom/sun/media/vfw/ICInfo;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icInfo(JNIEnv *env,
				  jclass  vcm,
				  jstring jfccType,
				  jstring jfccHandler,
				  jobject jicInfo)
{
    DWORD fccType = (DWORD) jfourCCToInt(env, jfccType);
    DWORD fccHandler = (DWORD) jfourCCToInt(env, jfccHandler);
    ICINFO icInfo;
    int result;
    
    icInfo.dwSize = sizeof(icInfo);

    result = (int) ICInfo(fccType, fccHandler, &icInfo);

    if (result > 0) {
	ICINFO_JICInfo(env, &icInfo, jicInfo);
	return 1;
    }

    return 0;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icInfoEnum
 * Signature: (Ljava/lang/String;ILcom/sun/media/vfw/ICInfo;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icInfoEnum(JNIEnv *env,
				      jclass  vcm,
				      jstring jfccType,
				      jint    enm,
				      jobject jicInfo)
{
    DWORD fccType = jfourCCToInt(env, jfccType);
    ICINFO icInfo;
    int result;
    
    icInfo.dwSize = sizeof(icInfo);

    result = (int) ICInfo(fccType, (DWORD) enm, &icInfo);
    if (result > 0) {
	ICINFO_JICInfo(env, &icInfo, jicInfo);
	return 1;
    }

    return 0;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icDecompressBegin
 * Signature: (ILcom/sun/media/vfw/BitMapInfo;Lcom/sun/media/vfw/BitMapInfo;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icDecompressBegin(JNIEnv *env,
					     jclass  vcm,
					     jint    jhandle,
					     jobject jbiIn,
					     jobject jbiOut)
{
    HIC handle = (HIC) jhandle;
    BITMAPINFOHEADER *biIn, *biOut;
    int size, result;

    if (handle == 0)
	return 0;

    /* Input Format */
    size = sizeof(BITMAPINFOHEADER) +
	GetIntField(env, jbiIn, "extraSize");
    biIn = (BITMAPINFOHEADER *) malloc(size);

    /* Output Format */
    size = sizeof(BITMAPINFOHEADER) +
	GetIntField(env, jbiOut, "extraSize");
    biOut = (BITMAPINFOHEADER *) malloc(size);

    /* Convert them */
    JBitMapInfo_To_BITMAPINFO(env, jbiIn, biIn);
    JBitMapInfo_To_BITMAPINFO(env, jbiOut, biOut);
    
    result = ICDecompressBegin(handle, biIn, biOut);

    if (biIn) free(biIn);
    if (biOut) free(biOut);
    
    if (result == 0)
	return 1;
    else
	return 0;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icDecompressQuery
 * Signature: (ILcom/sun/media/vfw/BitMapInfo;Lcom/sun/media/vfw/BitMapInfo;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icDecompressQuery(JNIEnv *env,
					     jclass  vcm,
					     jint    jhandle,
					     jobject jbiIn,
					     jobject jbiOut)
{
    HIC handle = (HIC) jhandle;
    BITMAPINFOHEADER *biIn = NULL, *biOut = NULL;
    int size, result;

    if (handle == 0)
	return 0;

    /* Input Format */
    biIn = JBitMapInfo_To_BITMAPINFO(env, jbiIn, NULL);

    /* Output Format */
    biOut = JBitMapInfo_To_BITMAPINFO(env, jbiOut, NULL);

    result = ICDecompressQuery(handle, biIn, biOut);

    if (biIn) free(biIn);
    if (biOut) free(biOut);

    if (result == 0)
	return 1;
    else
	return 0;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icDecompress
 * Signature: (IILcom/sun/media/vfw/BitMapInfo;Ljava/lang/Object;Lcom/sun/media/vfw/BitMapInfo;Ljava/lang/Object;)Z
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icDecompress(JNIEnv *env,
					jclass  vcm,
					jint    jhandle,
					jlong   flags,
					jobject jbiIn,
					jobject jbufIn,
					jlong   inBytes,
					jobject jbiOut,
					jobject jbufOut,
					jlong   outBytes,
					jint    outType)
{
    HIC handle = (HIC) jhandle;
    BITMAPINFOHEADER *biIn, *biOut;
    int size, result;
    void *inData = (void *) inBytes, *outData = (void*) outBytes;

    if (handle == 0)
	return 0;

    /* Input Format */
    size = sizeof(BITMAPINFOHEADER) +
	GetIntField(env, jbiIn, "extraSize");
    biIn = (BITMAPINFOHEADER *) malloc(size);

    /* Output Format */
    size = sizeof(BITMAPINFOHEADER) +
	GetIntField(env, jbiIn, "extraSize");
    biOut = (BITMAPINFOHEADER *) malloc(size);

    /* Convert them */
    JBitMapInfo_To_BITMAPINFO(env, jbiIn, biIn);
    JBitMapInfo_To_BITMAPINFO(env, jbiOut, biOut);

    if (inBytes == 0)
	inData = (void *) (*env)->GetByteArrayElements(env, (jbyteArray) jbufIn, 0);
    if (outBytes == 0) {
	switch (outType) {
	case 1:
	    outData = (void *) (*env)->GetByteArrayElements(env, (jbyteArray) jbufOut,
							    0);
	    break;
	case 2:
	    outData = (void *) (*env)->GetShortArrayElements(env, (jshortArray) jbufOut,
							     0);
	    break;
	case 4:
	    outData = (void *) (*env)->GetIntArrayElements(env, (jintArray) jbufOut, 0);
	}
    }

    result = ICDecompress(handle, flags, biIn, inData, biOut, outData);

    if (inBytes == 0)
	(*env)->ReleaseByteArrayElements(env, (jbyteArray) jbufIn,
					 (signed char *)inData, 0);

    if (outBytes == 0) {
	switch (outType) {
	case 1:
	    (*env)->ReleaseByteArrayElements(env, (jbyteArray) jbufOut,
					     (signed char *) outData, 0);
	    break;
	case 2:
	    (*env)->ReleaseShortArrayElements(env, (jshortArray) jbufOut,
					      (short *) outData, 0);
	    break;
	case 4:
	    (*env)->ReleaseIntArrayElements(env, (jintArray) jbufOut,
					    (long *) outData, 0);
	}
    }
    /* printf("decompress result = %d\n", result); */
    free(biIn);
    free(biOut);
    return result;

}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icDecompressEnd
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icDecompressEnd(JNIEnv *env,
					   jclass  vcm,
					   jint    jhandle)
{
    HIC handle = (HIC) jhandle;

    if (handle == 0 || ICDecompressEnd(handle) < 0)
	return 0;
    else
	return 1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icDecompressGetFormat(JNIEnv *env,
						 jclass  vcm,
						 jint    jhandle,
						 jobject jbiIn,
						 jobject jbiOut)
{
    HIC handle = (HIC) jhandle;
    BITMAPINFOHEADER *biIn, *biOut;
    int biOutSize = 0;
    int clrUsed, size;
    char szTemp[5] = {0, 0, 0, 0, 0};
    
    if (handle == 0) {
	printf("icDecompressGetFormat - handle is 0\n");
	return 0;
    }

    clrUsed = GetIntField(env, jbiIn, "biClrUsed");

    size = sizeof(BITMAPINFOHEADER) + GetIntField(env, jbiIn, "extraSize");
    
    biIn = (BITMAPINFOHEADER *) malloc( size );

    JBitMapInfo_To_BITMAPINFO(env, jbiIn, biIn);

    biOutSize = ICDecompressGetFormatSize(handle, (BITMAPINFOHEADER *) biIn);

#ifdef DEBUG
    printf("Format Size = %d\n", biOutSize);
#endif
    
    if (biOutSize > 0) {	
	biOut = (BITMAPINFOHEADER *) malloc(biOutSize);
	ICDecompressGetFormat(handle, (BITMAPINFOHEADER *) biIn,
			      (BITMAPINFOHEADER *) biOut);
	BITMAPINFO_To_JBitMapInfo(env, biOut, jbiOut);
	free(biOut);
	free(biIn);
	return 1;
    }

    free(biIn);

    return 0;
}
/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icCompressBegin
 * Signature: (ILcom/sun/media/vfw/BitMapInfo;Lcom/sun/media/vfw/BitMapInfo;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icCompressBegin(JNIEnv *env,
					   jclass vcm,
					   jint handle,
					   jobject jbiIn,
					   jobject jbiOut)
{
    BITMAPINFOHEADER *biIn, *biOut;
    int returnVal;
    
    biIn = JBitMapInfo_To_BITMAPINFO(env, jbiIn, NULL);
    biOut = JBitMapInfo_To_BITMAPINFO(env, jbiOut, NULL);

    if (biIn != NULL) {
	returnVal = ICCompressBegin((HIC) handle,
				    biIn,
				    biOut);
    }

    if (biIn != NULL) free(biIn);
    if (biOut != NULL) free(biOut);
    
    return (returnVal == ICERR_OK);
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icCompress
 * Signature: (IILcom/sun/media/vfw/BitMapInfo;Ljava/lang/Object;Lcom/sun/media/vfw/BitMapInfo;Ljava/lang/Object;[I[IIIILcom/sun/media/vfw/BitMapInfo;Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icCompress(JNIEnv *env,
				      jclass vcm,
				      jint jhandle,
				      jint jinFlags,
				      jobject jbiOut,
				      jobject jdataOut,
				      jobject jbiIn,
				      jobject jdataIn,
				      jintArray jckid,
				      jintArray joutFlags,
				      jint jframeNo,
				      jint jreqFrameSize,
				      jint jquality,
				      jobject jbiPrev,
				      jobject jdataPrev)
{
    int *ckid = (*env)->GetIntArrayElements(env, jckid, 0);
    int *outFlags = (*env)->GetIntArrayElements(env, joutFlags, 0);
    BITMAPINFOHEADER *biIn, *biOut, *biPrev;
    int returnVal;
    char *dataIn = NULL, *dataOut = NULL, *dataPrev = NULL;
    
    biIn = JBitMapInfo_To_BITMAPINFO(env, jbiIn, NULL);
    biOut = JBitMapInfo_To_BITMAPINFO(env, jbiOut, NULL);
    /* NOTE: biPrev will be null if jbiPrev was null */
    biPrev = JBitMapInfo_To_BITMAPINFO(env, jbiPrev, NULL);

    dataIn = (char *) (*env)->GetByteArrayElements(env, jdataIn, 0);
    dataOut = (char *) (*env)->GetByteArrayElements(env, jdataOut, 0);
    if (jdataPrev != NULL)
	dataPrev = (char *) (*env)->GetByteArrayElements(env, jdataPrev, 0);

    returnVal = ICCompress((HIC) jhandle, jinFlags, biOut, dataOut, biIn, dataIn,
			   ckid, outFlags, jframeNo, jreqFrameSize, jquality,
			   biPrev, dataPrev);
    
    if (jbiOut) {
	SetIntField(env, jbiOut, "biSizeImage", biOut->biSizeImage);
    }
    if (biIn) free(biIn);
    if (biOut) free(biOut);
    if (biPrev) free(biPrev);

    if (dataIn) (*env)->ReleaseByteArrayElements(env, jdataIn, dataIn, 0);
    if (dataOut) (*env)->ReleaseByteArrayElements(env, jdataOut, dataOut, 0);
    if (dataPrev) (*env)->ReleaseByteArrayElements(env, jdataPrev, dataPrev, 0);
    
    (*env)->ReleaseIntArrayElements(env, jckid, (signed int *) ckid, 0);
    (*env)->ReleaseIntArrayElements(env, joutFlags, (signed int *) outFlags, 0);
    return returnVal;
    
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icCompressEnd
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icCompressEnd(JNIEnv *env,
					 jclass vcm,
					 jint handle)
{
    return ICCompressEnd(handle);
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icCompressGetFormat
 * Signature: (ILcom/sun/media/vfw/BitMapInfo;Lcom/sun/media/vfw/BitMapInfo;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icCompressGetFormat(JNIEnv *env,
					       jclass vcm,
					       jint handle,
					       jobject jbiIn,
					       jobject jbiOut)
{
    BITMAPINFOHEADER *biIn;
    BITMAPINFOHEADER *biOut;
    int size;
    int returnVal;

    biIn = JBitMapInfo_To_BITMAPINFO(env, jbiIn, NULL);
    size = ICCompressGetFormatSize(handle, biIn);

    if (size < 0) {
	free(biIn);
	return 0;
    }
    
    biOut = (BITMAPINFOHEADER *) malloc(size);
    biOut->biSize = size;
    returnVal = ICCompressGetFormat(handle, biIn, biOut);
    BITMAPINFO_To_JBitMapInfo(env, biOut, jbiOut);
    free(biOut);
    free(biIn);
    return returnVal;
}

/*
 * Class:     com_sun_media_codec_video_vcm_VCM
 * Method:    icCompressGetFormatSize
 * Signature: (ILcom/sun/media/vfw/BitMapInfo;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_media_codec_video_vcm_VCM_icCompressGetFormatSize(JNIEnv *env,
						   jclass vcm,
						   jint handle,
						   jobject jbiIn)
{
    BITMAPINFOHEADER *biIn;
    int size;

    if (handle == 0)
	return 0;
    
    biIn = JBitMapInfo_To_BITMAPINFO(env, jbiIn, NULL);
    size = ICCompressGetFormatSize(handle, biIn);
    if (biIn) free(biIn);
    return size;
}

