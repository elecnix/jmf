/**
 * @(#)acm.c	1.29 01/02/23
 */

#include <stdio.h> /* to be removed */

#include <windows.h>
#include <windowsx.h>
#include <mmreg.h>
#include <msacm.h>

#include "com_ibm_media_codec_audio_ACMCodec.h"

/* definitions of new formats */
#define WAVE_FORMAT_MSNAUDIO 0x32
#define WAVE_FORMAT_MSG723 0x42
#define WAVE_FORMAT_MPEG_LAYER3 0x55
#define WAVE_FORMAT_VOXWARE_AC8 0x70
#define WAVE_FORMAT_VOXWARE_AC10 0x71
#define WAVE_FORMAT_VOXWARE_AC16 0x72
#define WAVE_FORMAT_VOXWARE_AC20 0x73
#define WAVE_FORMAT_VOXWARE_METAVOICE 0x74
#define WAVE_FORMAT_VOXWARE_METASOUND 0x75
#define WAVE_FORMAT_VOXWARE_RT29H 0x76
#define WAVE_FORMAT_VOXWARE_VR12 0x77
#define WAVE_FORMAT_VOXWARE_VR18 0x78
#define WAVE_FORMAT_VOXWARE_TQ40 0x79
#define WAVE_FORMAT_VOXWARE_TQ60 0x81
#define WAVE_FORMAT_MSRT24 0x82

typedef struct {
	JNIEnv* env; /* JVM environment */
	jobject obj; /* the object itself */
	LPWAVEFORMATEX inputFormat; /* native format tag */
	BOOL inputFlag; /* is enumaration of input formats */
} Env;



/*
 * A function to translate a WAVEFORMATEX into a Java AudioFormat
 */
jobject WAVEFORMATEX2AudioFormat(JNIEnv* env, LPWAVEFORMATEX pFormat, Env* pEnv) {
	jclass audioFormatClass, wavAudioFormatClass, formatClass, byteArrayClass;
	jobject jmfFormatObj;
	jfieldID encodingID, byteArrayID;
	jstring encoding;
	jbyteArray codecHeader;
	jmethodID constructorID;
	jint isSigned;

        BOOL wavInputRequired = FALSE;

	audioFormatClass = (*env)->FindClass(env, "javax/media/format/AudioFormat");
	wavAudioFormatClass = (*env)->FindClass(env, "com/sun/media/format/WavAudioFormat");

	switch (pFormat->wFormatTag) {
	
	case WAVE_FORMAT_PCM:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "LINEAR", "Ljava/lang/String;"); 
		break;
	case WAVE_FORMAT_ALAW:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "ALAW", "Ljava/lang/String;"); 
	  	/* Temp. disabled alaw support */
		return NULL;
	case WAVE_FORMAT_MULAW:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "ULAW", "Ljava/lang/String;"); 
	  	/* Temp. disabled ulaw support */
		return NULL;
	case WAVE_FORMAT_GSM610:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "GSM_MS", "Ljava/lang/String;"); 
		wavInputRequired = TRUE;
	  	/* Temp. disabled support */
		return NULL;
	case WAVE_FORMAT_ADPCM:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "MSADPCM", "Ljava/lang/String;");
		wavInputRequired = TRUE;
	  	/* Temp. disabled decoding support */
		if (pEnv->inputFlag)
			return NULL;
		break;
	case WAVE_FORMAT_DVI_ADPCM:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "IMA4_MS", "Ljava/lang/String;");
		wavInputRequired = TRUE;
		return NULL;
	case WAVE_FORMAT_DSPGROUP_TRUESPEECH:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "TRUESPEECH", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_MSNAUDIO:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "MSNAUDIO", "Ljava/lang/String;");
		break;
		/*	case WAVE_FORMAT_MSG723:
			encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "G723", "Ljava/lang/String;");
			break;
		*/
	case WAVE_FORMAT_MPEG_LAYER3:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "MPEGLAYER3", "Ljava/lang/String;");
		/* Temp. disabled support */
		return NULL;
	case WAVE_FORMAT_VOXWARE_AC8:
		if (pEnv->inputFormat != NULL &&
		    pEnv->inputFormat->nSamplesPerSec != 8000)
		    return NULL;
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREAC8", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_VOXWARE_AC10:
		if (pEnv->inputFormat != NULL &&
		    pEnv->inputFormat->nSamplesPerSec != 8000)
		    return NULL;
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREAC10", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_VOXWARE_AC16:
		if (pEnv->inputFormat != NULL &&
		    pEnv->inputFormat->nSamplesPerSec != 8000)
		    return NULL;
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREAC16", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_VOXWARE_AC20:
		if (pEnv->inputFormat != NULL &&
		    pEnv->inputFormat->nSamplesPerSec != 8000)
		    return NULL;
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREAC20", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_VOXWARE_METAVOICE:
	/*
		Disabled, doesn't work.  -ivg
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREMETAVOICE", "Ljava/lang/String;");
		break;
	*/
		return NULL;
	case WAVE_FORMAT_VOXWARE_METASOUND:
	/*
		Disabled, doesn't work.  -ivg
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREMETASOUND", "Ljava/lang/String;");
		break;
	*/
		return NULL;
	case WAVE_FORMAT_VOXWARE_RT29H:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWARERT29H", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_VOXWARE_VR12:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREVR12", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_VOXWARE_VR18:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREVR18", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_VOXWARE_TQ40:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWARETQ40", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_VOXWARE_TQ60:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWARETQ60", "Ljava/lang/String;");
		break;
	case WAVE_FORMAT_MSRT24:
		encodingID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "MSRT24", "Ljava/lang/String;");
		break;

	/* new wFormatTag to be supported by the ACM wrapper should be added here as above */

	default: /* unknown wFormatTag */
		return NULL;
	}
 
	/* get the Java encoding string according to it's ID */
	encoding = (*env)->GetStaticObjectField(env, wavAudioFormatClass, encodingID); 

	/* The following is not used since the generation of WAVEFORMATEX by ACM has empty extra information
		which cann't be compared correctly with the format generated by the parser */

	/* create a Java array for the codec header */
	if (pFormat->cbSize != 0 && pFormat->wFormatTag != WAVE_FORMAT_PCM) {
	  codecHeader = (*env)->NewByteArray(env, pFormat->cbSize);
	  (*env)->SetByteArrayRegion(env, codecHeader, 0, pFormat->cbSize, 
				     &((jbyte*)pFormat)[sizeof(WAVEFORMATEX)]);
	}
	else
		codecHeader = NULL;

	formatClass = (*env)->FindClass(env, "javax/media/Format");
	byteArrayID = (*env)->GetStaticFieldID(env, formatClass, "byteArray", "Ljava/lang/Class;"); 
	byteArrayClass = (*env)->GetStaticObjectField(env, formatClass, byteArrayID); 
	if ((jint)pFormat->wBitsPerSample > 8)
	  isSigned = 1; /* SIGNED */
	else
	  isSigned = 0; /* UNSOGNED */

	if (pEnv->inputFlag && !wavInputRequired) {

	    /* get Java audioFormat constructor id */
	    constructorID = (*env)->GetMethodID(env, audioFormatClass, 
					    "<init>", 
					    "(Ljava/lang/String;DIIIIIDLjava/lang/Class;)V"); 
	
	    /* create the Java audioFormat object */
	    jmfFormatObj = (*env)->NewObject(env, audioFormatClass, 
					constructorID, encoding, 
					(jdouble)pFormat->nSamplesPerSec, 
					(jint)pFormat->wBitsPerSample, 
					(jint)pFormat->nChannels,
					(jint)0 /* LITTLE_ENDIAN */, 
					isSigned,
					pFormat->nBlockAlign * 8, 
					(jdouble)pFormat->nAvgBytesPerSec,
					byteArrayClass);
	} else {
		
	    /* get Java WavAudioFormat constructor id */
	    constructorID = (*env)->GetMethodID(env, wavAudioFormatClass, 
					    "<init>", 
					    "(Ljava/lang/String;DIIIIIIFLjava/lang/Class;[B)V"); 
	
	    /* create the Java WavAudioFormat object */
	    jmfFormatObj = (*env)->NewObject(env, wavAudioFormatClass, 
					 constructorID, encoding, 
					 (jdouble)pFormat->nSamplesPerSec, 
					 (jint)pFormat->wBitsPerSample, 
					 (jint)pFormat->nChannels,
					 pFormat->nBlockAlign * 8, 
					 (jint)pFormat->nAvgBytesPerSec,
					 (jint)0 /* LITTLE_ENDIAN */, isSigned,
					 (jdouble)-1/* NOT_SPECIFIED */, 
					 byteArrayClass, codecHeader);
	}
	
	return jmfFormatObj;
}

/*
 * A function to translate a a Java AudioFormat into a WAVEFORMATEX
 */
LPWAVEFORMATEX AudioFormat2WAVEFORMATEX(JNIEnv* env, jobject audioFormat) {

	LPWAVEFORMATEX pFormat;
	jclass audioFormatClass, wavAudioFormatClass;
	jmethodID getEncodingID, getChannelsID, getSampleSizeInBitsID, getSampleRateID, 
			getFrameSizeInBitsID, getAverageBytesPerSecondID, getCodecSpecificHeaderID;
	jfieldID formatID; 
	jstring encoding, format; /* format is a format constant in the AudioFormat object */
	const char* encodingString;
	const char* formatString;
	jint channels, sampleSizeInBits, frameSizeInBits, averageBytesPerSecond;
	jbyteArray codecSpecificHeader;
	LPBYTE specificHeaderBytes;
	jdouble sampleRate;
	int extraSize = 0;

	audioFormatClass = (*env)->FindClass(env, "javax/media/format/AudioFormat");
	wavAudioFormatClass = (*env)->FindClass(env, "com/sun/media/format/WavAudioFormat");

	if ((*env)->IsInstanceOf(env, audioFormat, wavAudioFormatClass)) {
	    // WavAudioFormat class

	    /* get encoding */
	    getEncodingID = (*env)->GetMethodID(env, wavAudioFormatClass, "getEncoding", 
		"()Ljava/lang/String;"); 
	    encoding = (*env)->CallObjectMethod(env, audioFormat, getEncodingID);
	    encodingString = (*env)->GetStringUTFChars(env, encoding, JNI_FALSE); 

	    /* get no of channels */
	    getChannelsID = (*env)->GetMethodID(env, wavAudioFormatClass, "getChannels", "()I"); 
	    channels = (*env)->CallIntMethod(env, audioFormat, getChannelsID);
	
	    /* get sample size */
	    getSampleSizeInBitsID = (*env)->GetMethodID(env, wavAudioFormatClass, "getSampleSizeInBits", 
		"()I"); 
	    sampleSizeInBits = (*env)->CallIntMethod(env, audioFormat, getSampleSizeInBitsID);

	    /* get sample rate */
	    getSampleRateID = (*env)->GetMethodID(env, wavAudioFormatClass, "getSampleRate", "()D"); 
	    sampleRate = (*env)->CallDoubleMethod(env, audioFormat, getSampleRateID);

	    /* get frame size */
	    getFrameSizeInBitsID = (*env)->GetMethodID(env, wavAudioFormatClass, "getFrameSizeInBits", "()I"); 
	    frameSizeInBits = (*env)->CallIntMethod(env, audioFormat, getFrameSizeInBitsID);

	    /* get avg bytes per second */
	    getAverageBytesPerSecondID = (*env)->GetMethodID(env, wavAudioFormatClass, "getAverageBytesPerSecond", "()I");
	    averageBytesPerSecond = (*env)->CallIntMethod(env, audioFormat, getAverageBytesPerSecondID);

	    /* get codec specific header */
	    getCodecSpecificHeaderID = (*env)->GetMethodID(env, wavAudioFormatClass, "getCodecSpecificHeader", "()[B"); 
	    codecSpecificHeader = (*env)->CallObjectMethod(env, audioFormat, getCodecSpecificHeaderID);

	    if (codecSpecificHeader != NULL) 
		extraSize = (*env)->GetArrayLength(env, codecSpecificHeader);

	} else {
	    // AudioFormat class.

	    /* get encoding */
	    getEncodingID = (*env)->GetMethodID(env, audioFormatClass, "getEncoding", 
		"()Ljava/lang/String;"); 
	    encoding = (*env)->CallObjectMethod(env, audioFormat, getEncodingID);
	    encodingString = (*env)->GetStringUTFChars(env, encoding, JNI_FALSE); 

	    /* get no of channels */
	    getChannelsID = (*env)->GetMethodID(env, audioFormatClass, "getChannels", "()I"); 
	    channels = (*env)->CallIntMethod(env, audioFormat, getChannelsID);
	
	    /* get sample size */
	    getSampleSizeInBitsID = (*env)->GetMethodID(env, audioFormatClass, "getSampleSizeInBits", 
		"()I"); 
	    sampleSizeInBits = (*env)->CallIntMethod(env, audioFormat, getSampleSizeInBitsID);

	    /* get sample rate */
	    getSampleRateID = (*env)->GetMethodID(env, audioFormatClass, "getSampleRate", "()D"); 
	    sampleRate = (*env)->CallDoubleMethod(env, audioFormat, getSampleRateID);

	    /* get avg bytes per second */
	    getAverageBytesPerSecondID = (*env)->GetMethodID(env, audioFormatClass, "getFrameRate", "()D");
	    averageBytesPerSecond = (jint)(*env)->CallDoubleMethod(env, audioFormat, getAverageBytesPerSecondID);

	    /* get frame size */
	    getFrameSizeInBitsID = (*env)->GetMethodID(env, audioFormatClass, "getFrameSizeInBits", "()I"); 
	    frameSizeInBits = (*env)->CallIntMethod(env, audioFormat, getFrameSizeInBitsID);
	}

	pFormat = (LPWAVEFORMATEX)malloc(sizeof(WAVEFORMATEX) + extraSize);
	pFormat->nChannels = (WORD)channels;
	pFormat->nSamplesPerSec = (DWORD)sampleRate;
	pFormat->wBitsPerSample = (WORD)sampleSizeInBits;
	pFormat->nBlockAlign = frameSizeInBits / 8;
	pFormat->nAvgBytesPerSec = averageBytesPerSecond;/*pFormat->nSamplesPerSec * pFormat->nBlockAlign; */
	pFormat->cbSize = extraSize;
	if (extraSize > 0) {
		specificHeaderBytes = (*env)->GetByteArrayElements(env, codecSpecificHeader, JNI_FALSE); 
		memcpy(&(((char*)pFormat)[sizeof(WAVEFORMATEX)]), specificHeaderBytes, extraSize);
		(*env)->ReleaseByteArrayElements(env, codecSpecificHeader, specificHeaderBytes, 0);
	}

	/* compare to PCM */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "LINEAR", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_PCM;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}
	
	/* compare to GSM */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "GSM_MS", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_GSM610;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to ALAW */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "ALAW", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_ALAW;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}
	
	/* compare to MULAW */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "ULAW", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
	  pFormat->wFormatTag = WAVE_FORMAT_MULAW;
	  (*env)->ReleaseStringUTFChars(env, format, formatString);
	  return pFormat;
	}
	
	/* compare to MSADPCM */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "MSADPCM", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_ADPCM;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to DVIADPCM  */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "IMA4_MS", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_DVI_ADPCM;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to TRUESPEECH */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "TRUESPEECH", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_DSPGROUP_TRUESPEECH;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to MSNAUDIO */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "MSNAUDIO", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_MSNAUDIO;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to G723 */
	/* formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "G723", "Ljava/lang/String;"); 
	   format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	   formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	   if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
	   pFormat->wFormatTag = WAVE_FORMAT_MSG723;
	   (*env)->ReleaseStringUTFChars(env, format, formatString);
	   return pFormat;
	   }
	*/

	/* compare to MPEGLAYER3 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "MPEGLAYER3", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_MPEG_LAYER3;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to VOXWAREAC8 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREAC8", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_AC8;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}
	
	/* compare to VOXWAREAC10 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREAC10", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_AC10;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}
	
	/* compare to VOXWAREAC16 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREAC16", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_AC16;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}
	
	/* compare to VOXWAREAC20 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREAC20", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_AC20;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to VOXWAREMETAVOICE */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREMETAVOICE", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_METAVOICE;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}
	
	/* compare to VOXWAREMETASOUND */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREMETASOUND", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_METASOUND;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to VOXWARERT29H */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWARERT29H", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_RT29H;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to VOXWAREVR12 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREVR12", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_VR12;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to VOXWAREVR18 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWAREVR18", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_VR18;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to VOXWARETQ40 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWARETQ40", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_TQ40;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to VOXWARETQ60 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "VOXWARETQ60", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_VOXWARE_TQ60;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* compare to MSRT24 */
	formatID = (*env)->GetStaticFieldID(env, wavAudioFormatClass, "MSRT24", "Ljava/lang/String;"); 
	format = (*env)->GetStaticObjectField(env, wavAudioFormatClass, formatID);
	formatString = (*env)->GetStringUTFChars(env, format, JNI_FALSE); 
	if (strcmp((const char*)formatString, (const char*)encodingString) == 0) {
		pFormat->wFormatTag = WAVE_FORMAT_MSRT24;
		(*env)->ReleaseStringUTFChars(env, format, formatString);
		return pFormat;
	}

	/* new encodings to be supported by the ACM wrapper should be added here as above */

	(*env)->ReleaseStringUTFChars(env, encoding, encodingString);
	free(pFormat);

	return NULL;
}


/*
 * Class:     com_ibm_media_codec_audio_ACMCodec
 * Method:    openACMStream
 * Signature: (Ljavax/media/format/audio/AudioFormat;Ljavax/media/format/audio/AudioFormat;)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_media_codec_audio_ACMCodec_openACMStream
(JNIEnv* env, jobject obj, jobject inputFormat, jobject outputFormat) {

	MMRESULT result;
	LPWAVEFORMATEX pInputFormat, pOutputFormat;
	HACMSTREAM has;
	jclass acmCodecClass;

	/* convert the formats to a WAVEFORMATEX and open the codec */
	pInputFormat = AudioFormat2WAVEFORMATEX(env, inputFormat);
	pOutputFormat = AudioFormat2WAVEFORMATEX(env, outputFormat);
	
	if (pInputFormat == NULL || pOutputFormat == NULL) {
		printf("Can't open ACM codec - either input or ouput formats were not set\n");
		return FALSE;
	}

	result = acmStreamOpen(&has, NULL, pInputFormat, pOutputFormat, NULL, 
		(DWORD)NULL, (DWORD)NULL, ACM_STREAMOPENF_NONREALTIME); 

	if (result != MMSYSERR_NOERROR) {
		printf("ERROR in opening ACM stream: %d\n",result);
		//printf("failed input: %x, output: %x\n", pInputFormat->wFormatTag, pOutputFormat->wFormatTag);
		return FALSE;
	}

	//printf("ACMstream opened\n");

	return (jlong)has;
}

/*
 * Class:     com_ibm_media_codec_audio_ACMCodec
 * Method:    closeACMStream
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_codec_audio_ACMCodec_closeACMStream
(JNIEnv* rnv, jobject obj, jlong nativeHandle) {
	
	MMRESULT result;
	jclass acmCodecClass;
	jfieldID nativeHandleFieldID;

	result = acmStreamClose((HACMSTREAM)nativeHandle, (DWORD)NULL);
	
	if (result != MMSYSERR_NOERROR) {
		printf("ERROR in closing ACM stream: %d\n",result);
		return;
	}

	//printf("ACMstream closed\n");
}

/*
 * Class:     com_ibm_media_codec_audio_ACMCodec
 * Method:    resetACMStream
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_codec_audio_ACMCodec_resetACMStream
(JNIEnv* env, jobject obj, jlong nativeHandle) {

	MMRESULT result;
	jclass acmCodecClass;
	jfieldID nativeHandleFieldID;

	result = acmStreamReset((HACMSTREAM)nativeHandle, (DWORD)NULL); 

	if (result != MMSYSERR_NOERROR) {
		printf("ERROR in reseting ACM stream: %d\n",result);
		return;
	}

	//printf("ACMstream reset\n");
}

/*
 * A callback for acmFormatEnum, each format recieved is added to the list of the supported 
 * formats 
 */
BOOL CALLBACK acmFormatEnumCallback(HACMDRIVERID hadid, LPACMFORMATDETAILS pafd,	
				    DWORD pEnv, DWORD fdwSupport) {
	
	jclass byteClass, vectorClass, acmCodecClass;
	jmethodID addElementID, containsID;
	jfieldID supportedInputFormatsID, supportedOutputFormatsID;
	jstring format;
	jobject supportedInputFormatsObj, supportedOutputFormatsObj, jmfFormatObj;

	/* get all needed Java classes */
	byteClass = (*((Env*)pEnv)->env)->FindClass(((Env*)pEnv)->env, "java/lang/Byte");
	vectorClass = (*((Env*)pEnv)->env)->FindClass(((Env*)pEnv)->env, "java/util/Vector");
	acmCodecClass = (*((Env*)pEnv)->env)->GetObjectClass(((Env*)pEnv)->env, ((Env*)pEnv)->obj);

	/* get all needed Java members' ID */
	addElementID = (*((Env*)pEnv)->env)->GetMethodID(((Env*)pEnv)->env, vectorClass, "addElement", 
		"(Ljava/lang/Object;)V");
	containsID = (*((Env*)pEnv)->env)->GetMethodID(((Env*)pEnv)->env, vectorClass, "contains", 
		"(Ljava/lang/Object;)Z");
	supportedInputFormatsID = (*((Env*)pEnv)->env)->GetFieldID(((Env*)pEnv)->env, acmCodecClass, 
		"supportedInputFormats", "Ljava/util/Vector;"); 
	supportedOutputFormatsID = (*((Env*)pEnv)->env)->GetFieldID(((Env*)pEnv)->env, acmCodecClass, 
		"supportedOutputFormats", "Ljava/util/Vector;"); 

	/* get all needed Java objects */
	supportedInputFormatsObj = (*((Env*)pEnv)->env)->GetObjectField(((Env*)pEnv)->env, 
		((Env*)pEnv)->obj, supportedInputFormatsID); 	
	supportedOutputFormatsObj = (*((Env*)pEnv)->env)->GetObjectField(((Env*)pEnv)->env, 
		((Env*)pEnv)->obj, supportedOutputFormatsID); 

	jmfFormatObj = WAVEFORMATEX2AudioFormat(((Env*)pEnv)->env, pafd->pwfx, (Env*)pEnv);

	if (jmfFormatObj == NULL) /* format isn't supported by ACM wrapper */
		return TRUE;

	if (((Env*)pEnv)->inputFlag) {
		/* check if format already exists in Vector */
		if (!(*((Env*)pEnv)->env)->CallBooleanMethod(((Env*)pEnv)->env, supportedInputFormatsObj,
				containsID, jmfFormatObj))
			/* add format to Vector */
			(*((Env*)pEnv)->env)->CallVoidMethod(((Env*)pEnv)->env, supportedInputFormatsObj, 
				addElementID, jmfFormatObj);
	}
	else {
		/* check if format already exists in Vector */
		if (!(*((Env*)pEnv)->env)->CallBooleanMethod(((Env*)pEnv)->env, supportedOutputFormatsObj, 
				containsID, jmfFormatObj))
			/* add format to Vector */
			(*((Env*)pEnv)->env)->CallVoidMethod(((Env*)pEnv)->env, supportedOutputFormatsObj, 
				addElementID, jmfFormatObj);
	}

	return TRUE;
}

/*
 * A callback for acmDrivarEnum, for each ACM driver recieved it's supported format are enumerated 
 */
BOOL CALLBACK acmDriverEnumCallback(HACMDRIVERID hadid, DWORD pEnv, DWORD fdwSupport) {

	HACMDRIVER had; 
	WAVEFORMATEX wfx;
	ACMFORMATDETAILS afd;
	ACMDRIVERDETAILS dd;
	DWORD size;
	MMRESULT result;
	
	dd.cbStruct = sizeof(dd);
	result = acmDriverDetails(hadid, &dd, 0);
	if (result != MMSYSERR_NOERROR) {
	  printf("ERROR while querying ACM driver's information\n");
	  return TRUE;
	}

	if (dd.szShortName == NULL)
	  return TRUE;
	
	// we don't use PCM->PCM driver
	if (strcmp(dd.szShortName, "MS-PCM") == 0 )
	  return TRUE;

	// Only output formats from the following drivers.
	if ((strncmp(dd.szShortName, "L&H", 3) != 0) &&	  /* Voxware AC */
	    (strncmp(dd.szShortName, "TrueSpeech", 10) != 0))
	  return TRUE;

	//printf("shortname: %s\n", dd.szShortName);

	result = acmDriverOpen(&had, hadid, 0); 
	if (result != MMSYSERR_NOERROR) {
	  printf("ERROR while opening driver: %d\n",result);
	  return FALSE;
	}
	
	size = 0;
	result = acmMetrics((HACMOBJ)had, ACM_METRIC_MAX_SIZE_FORMAT, &size); 
	if (result != MMSYSERR_NOERROR) {
	  printf("ERROR while metrics driver: %d\n",result);
	  return FALSE;
	}
	if (size < sizeof(WAVEFORMATEX))  
	  size = sizeof(WAVEFORMATEX);  // for MS-PCM
	memset(&afd, 0, sizeof(afd));
	afd.cbStruct= sizeof(afd);
	afd.cbwfx = size;
	afd.pwfx = (LPWAVEFORMATEX)GlobalAllocPtr(GHND, size);
	if (((Env*)pEnv)->inputFlag) {
	  afd.dwFormatTag = WAVE_FORMAT_UNKNOWN;
	  memset(afd.pwfx, 0, size);
	  afd.pwfx->cbSize = LOWORD(size) - sizeof(WAVEFORMATEX);
	  afd.pwfx->wFormatTag = WAVE_FORMAT_UNKNOWN;
	}
	else {
	  afd.dwFormatTag = ((Env*)pEnv)->inputFormat->wFormatTag;
	  memcpy(afd.pwfx, ((Env*)pEnv)->inputFormat, size);
	}

	result = acmFormatEnum(had, &afd, acmFormatEnumCallback, pEnv, 
			       (((Env*)pEnv)->inputFlag) ? (DWORD)NULL: ACM_FORMATENUMF_CONVERT);
	
	if (result != MMSYSERR_NOERROR) 
	  printf("ERROR while format enumeration: %d\n",result);
	
	result = acmDriverClose(had, 0); 
	if (result != MMSYSERR_NOERROR) {
	  printf("ERROR while closing driver: %d\n",result);
	  return FALSE;;
	}
	
	GlobalFreePtr(afd.pwfx);
	return TRUE;
}

/*
 * Class:     com_ibm_media_codec_audio_ACMCodec
 * Method:    fillSupportedInputFormats
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_codec_audio_ACMCodec_fillSupportedInputFormats
(JNIEnv* env, jobject obj) {
	
	Env jenv;
	MMRESULT result;
	
	jenv.env = env;
	jenv.obj = obj;
	jenv.inputFormat = NULL; 
	jenv.inputFlag = TRUE;

	result = acmDriverEnum(acmDriverEnumCallback, (DWORD)&jenv, (DWORD)NULL); 
	if (result != MMSYSERR_NOERROR)
		printf("ERROR: NO ACM DRIVERS INSTALLED\n");
}

/*
 * Class:     com_ibm_media_codec_audio_ACMCodec
 * Method:    fillSupportedOutputFormats
 * Signature: (Ljavax/media/Format;)V
 */
JNIEXPORT void JNICALL Java_com_ibm_media_codec_audio_ACMCodec_fillSupportedOutputFormats
(JNIEnv* env, jobject obj, jobject input) {
	
	Env jenv;
	MMRESULT result;
	
	jenv.env = env;
	jenv.obj = obj;
	jenv.inputFlag = FALSE;

	jenv.inputFormat = AudioFormat2WAVEFORMATEX(env, input);

	if (jenv.inputFormat == NULL) {
		printf("Input format isn't supported by ACM wrapper\n");
		return;
	}

	result = acmDriverEnum(acmDriverEnumCallback, (DWORD)&jenv, (DWORD)NULL); 
	if (result != MMSYSERR_NOERROR)
		printf("ERROR: NO ACM DRIVERS INSTALLED\n");

	LocalFree(jenv.inputFormat);

}

/*
 * Class:     com_ibm_media_codec_audio_ACMCodec
 * Method:    getDestinationBufferSize
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_media_codec_audio_ACMCodec_getDestinationBufferSize
(JNIEnv* env, jobject obj, jlong nativeHandle, jint inputSize) {

	DWORD outputSize;
	MMRESULT result;

	result = acmStreamSize((HACMSTREAM)nativeHandle, inputSize, &outputSize, ACM_STREAMSIZEF_SOURCE); 
	if (result != MMSYSERR_NOERROR) {
		//printf("ERROR while checking stream size: %d\n", result);
		return 0;
	}

	return outputSize;
}

/*
 * Class:     com_ibm_media_codec_audio_ACMCodec
 * Method:    ACMProcess
 * Signature: (J[BIILjavax/media/Buffer;[BILjavax/media/Buffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_media_codec_audio_ACMCodec_ACMProcess
(JNIEnv* env, jobject obj, jlong nativeHandle, jbyteArray input, 
 jint inputOffset, jint inputLength, jobject inputData, jbyteArray output, 
 jint outputLength, jobject outputData) {

	MMRESULT result;
	ACMSTREAMHEADER ash;
	jclass dataClass;
	jmethodID setOffsetID, setLengthID;
	LPBYTE inputBuffer, copyInputBuffer;
	LPBYTE outputBuffer;
	jfieldID nativeHandleFieldID;

	inputBuffer = (*env)->GetByteArrayElements(env, input, JNI_FALSE);
	copyInputBuffer = inputBuffer;
	inputBuffer = &inputBuffer[inputOffset]; 
	outputBuffer = (*env)->GetByteArrayElements(env, output, JNI_FALSE); 

	ash.cbStruct = sizeof(ACMSTREAMHEADER);
	ash.fdwStatus = 0;
	ash.dwUser = (DWORD)NULL;
	ash.pbSrc = inputBuffer;
	ash.cbSrcLength = inputLength;
	ash.cbSrcLengthUsed = 0;
	ash.dwSrcUser = (DWORD)NULL;
	ash.pbDst = outputBuffer;
	ash.cbDstLength = outputLength; 
	ash.cbDstLengthUsed = 0;
	ash.dwDstUser = (DWORD)NULL;
	
	result = acmStreamPrepareHeader((HACMSTREAM)nativeHandle, &ash, 0); 
	if (result != MMSYSERR_NOERROR) {
		//printf("ERROR while prepering header: %d\n", result);
		return FALSE;
	}

	result = acmStreamConvert((HACMSTREAM)nativeHandle, &ash, /*(DWORD)NULL */ACM_STREAMCONVERTF_BLOCKALIGN); 
	if (result != MMSYSERR_NOERROR) {
		printf("ERROR while converting stream: %d\n", result);
		return FALSE;
	} 

	if (ash.cbSrcLengthUsed != ash.cbSrcLength) {
	  dataClass = (*env)->GetObjectClass(env, inputData); 
	  setOffsetID = (*env)->GetMethodID(env, dataClass, "setOffset", "(I)V"); 
	  (*env)->CallVoidMethod(env, inputData, setOffsetID, 
				 inputOffset + ash.cbSrcLengthUsed);
	  setLengthID = (*env)->GetMethodID(env, dataClass, "setLength", "(I)V");
	  (*env)->CallVoidMethod(env, inputData, setLengthID, 
				 ash.cbSrcLength - ash.cbSrcLengthUsed);
	}
		
	if (ash.cbDstLengthUsed != ash.cbDstLength) {
	  dataClass = (*env)->GetObjectClass(env, outputData); 
	  setLengthID = (*env)->GetMethodID(env, dataClass, "setLength", "(I)V"); 
	  (*env)->CallVoidMethod(env, outputData, setLengthID, 
				 ash.cbDstLengthUsed);
	}
	
	result = acmStreamUnprepareHeader((HACMSTREAM)nativeHandle, &ash, (DWORD)NULL);
	if (result != MMSYSERR_NOERROR) {
	  printf("ERROR while unprepering header: %d\n", result);
	  return FALSE;
	}
	
	(*env)->ReleaseByteArrayElements(env, input, copyInputBuffer, JNI_ABORT); 
	(*env)->ReleaseByteArrayElements(env, output, outputBuffer, 0);
	
	return TRUE;
}


