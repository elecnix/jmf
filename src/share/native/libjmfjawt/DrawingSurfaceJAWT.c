/*
 * @(#)DrawingSurfaceJAWT.c	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include <stdio.h>
#include <malloc.h>
#include <jni.h>
#include <jawt_md.h>
#include "com_sun_media_DrawingSurfaceJAWT.h"

/* Inaccessible static: avail */
/*
 * Class:     DrawingSurfaceJAWT
 * Method:    freeResourse
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_DrawingSurfaceJAWT_freeResource
    (JNIEnv *env, jclass jcls, jint awtObj, jint dsObj) {
    
    JAWT *awt = (JAWT*)awtObj;
    JAWT_DrawingSurface *ds = (JAWT_DrawingSurface*)dsObj;
    
    ds->env = env;
    awt->FreeDrawingSurface(ds);
}

/*
 * Class:     DrawingSurfaceJAWT
 * Method:    getAWT
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_DrawingSurfaceJAWT_getAWT
    (JNIEnv *env, jobject jobj){
  JAWT *awt;
  awt = (JAWT*)malloc(sizeof(JAWT));
  if ( awt == NULL ) {
    printf("malloc failed \n");
    return 0;
  }

  awt->version = JAWT_VERSION_1_3;

  if ( JAWT_GetAWT(env, awt) == JNI_FALSE) {
      /*printf("AWT not found\n");*/
    return 0;
  }

  return (jint)awt;
}


/*
 * Class:     DrawingSurfaceJAWT
 * Method:    getDrawingSurface
 * Signature: (Ljava/awt/Component;I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_DrawingSurfaceJAWT_getDrawingSurface
    (JNIEnv *env, jobject jobj, jobject canvas, jint awtObj) {

  JAWT *awt = (JAWT*)awtObj;
  JAWT_DrawingSurface *ds;

  ds = awt->GetDrawingSurface(env, canvas);
  if ( ds == NULL ) {
      /*printf("NULL drawing surface \n");*/
    return 0;
  }

  return (jint)ds;
}
    
/*
 * Class:     DrawingSurfaceJAWT
 * Method:    getDrawingSurfaceDisplay
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_DrawingSurfaceJAWT_getDrawingSurfaceDisplay
    (JNIEnv *env, jobject jobj, jint dsObj) {
#ifdef WIN32
   return 0;
#else
  JAWT_DrawingSurface *ds = (JAWT_DrawingSurface*)dsObj;
  JAWT_DrawingSurfaceInfo *dsi;

  JAWT_X11DrawingSurfaceInfo *dsi_x11;
  jint lock, display;


  lock = ds->Lock(ds);
  if ( (lock & JAWT_LOCK_ERROR) != 0 ) {
      /*printf("Error locking surface \n");*/
    return 0;
  }

  dsi = ds->GetDrawingSurfaceInfo(ds);
  if ( dsi == NULL ) {
    /*printf("Error getDrawingsurfaceinfo \n");*/
    ds->Unlock(ds);
    return 0;
  }
  
  dsi_x11 = (JAWT_X11DrawingSurfaceInfo*)dsi->platformInfo;
  display = (jint)dsi_x11->display;

  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  return display;
#endif
}

/*
 * Class:     DrawingSurfaceJAWT
 * Method:    getDrawingSurfaceWinID
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_DrawingSurfaceJAWT_getDrawingSurfaceWinID
(JNIEnv *env, jobject jobj, jint dsObj) {
  JAWT_DrawingSurface *ds = (JAWT_DrawingSurface*)dsObj;
  JAWT_DrawingSurfaceInfo *dsi;
#ifdef WIN32
  JAWT_Win32DrawingSurfaceInfo *dsi_win;
#else
  JAWT_X11DrawingSurfaceInfo *dsi_x11;
#endif

  jint lock, xid;
  
  lock = ds->Lock(ds);
  if ( (lock & JAWT_LOCK_ERROR) != 0 ) {
    /*printf("Error locking surface \n");*/
    return 0;
  }
  
  dsi = ds->GetDrawingSurfaceInfo(ds);
  if ( dsi == NULL ) {
    /*printf("Error getDrawingsurfaceinfo \n");*/
    ds->Unlock(ds);
    return 0;
  }

#ifdef WIN32
  dsi_win = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
  xid = (jint)dsi_win->hwnd;
#else
  dsi_x11 = (JAWT_X11DrawingSurfaceInfo*)dsi->platformInfo;
  xid = (jint)dsi_x11->drawable;
#endif

  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  return xid;
}

/*
 * Class:     DrawingSurfaceJAWT
 * Method:    getWindowHandle
 * Signature: (Ljava/awt/Component;)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_DrawingSurfaceJAWT_getWindowHandle
    (JNIEnv *env, jclass jcls, jobject jcomponent) {

  JAWT awt;
  JAWT_DrawingSurface *ds;
  JAWT_DrawingSurfaceInfo *dsi;
#ifdef WIN32
  JAWT_Win32DrawingSurfaceInfo *dsi_win;
#else
  JAWT_X11DrawingSurfaceInfo *dsi_x11;
#endif

  jint xid = 0;

  jint lock;
  /*printf("in dsJAWT getWindowHandle\n");*/

  awt.version = JAWT_VERSION_1_3;
  if ( JAWT_GetAWT(env, &awt) == JNI_FALSE) {
      /*printf("JAWT is not found \n");*/
    return 0;
  }

  ds = awt.GetDrawingSurface(env, jcomponent);
  if ( ds == NULL ) {
      /*printf("NULL drawing surface \n");*/
    return 0;
  }

  lock = ds->Lock(ds);
  if ( (lock & JAWT_LOCK_ERROR) != 0 ) {
      /*printf("Error in locking surface \n");*/
    awt.FreeDrawingSurface(ds);
    return 0;
  }

  dsi = ds->GetDrawingSurfaceInfo(ds);
  if ( dsi == NULL) {
      /*printf("Error in getting surface info\n");*/
    ds->Unlock(ds);
    awt.FreeDrawingSurface(ds);
    return 0;
  }

#ifdef WIN32
  dsi_win = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
  xid = (jint)dsi_win->hwnd;
#else
  dsi_x11 = (JAWT_X11DrawingSurfaceInfo*)dsi->platformInfo;
  xid = (jint)dsi_x11->drawable;
#endif
 
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);
  
  /*printf("in dsJAWT.c xid = %d \n", xid);*/
  return xid;
}

/*
 * Class:     DrawingSurfaceJAWT
 * Method:    lockAWT
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_media_DrawingSurfaceJAWT_lockAWT
    (JNIEnv *env, jclass jcls, jint dsObj) {
  JAWT_DrawingSurface *ds = (JAWT_DrawingSurface*)dsObj;
  jint lock;

  ds->env = env;
  lock=ds->Lock(ds);
  
  if ((lock & JAWT_LOCK_ERROR) != 0 ) {
    return JNI_FALSE;
  } else if ((lock & JAWT_LOCK_SURFACE_CHANGED) != 0 ) {
    ds->Unlock(ds);
    return JNI_FALSE;
  } else {
    return JNI_TRUE;
  }

}

/*
 * Class:     DrawingSurfaceJAWT
 * Method:    unlockAWT
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_DrawingSurfaceJAWT_unlockAWT
    (JNIEnv *env, jclass jcls, jint dsObj) {
  JAWT_DrawingSurface *ds = (JAWT_DrawingSurface*)dsObj;
  ds->Unlock(ds);
  return;
}
