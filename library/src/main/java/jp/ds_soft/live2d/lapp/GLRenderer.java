/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package jp.ds_soft.live2d.lapp;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    private String modelName;

    public void setModelName(String name) {
        this.modelName = name;
    }

    // Called at initialization (when the drawing context is lost and recreated).
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        LAppDelegate.getInstance().onSurfaceCreated(modelName);
    }

    // Mainly called when switching between landscape and portrait.
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        LAppDelegate.getInstance().onSurfaceChanged(width, height);
    }

    // Called repeatedly for drawing.
    @Override
    public void onDrawFrame(GL10 unused) {
        android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT | android.opengl.GLES20.GL_DEPTH_BUFFER_BIT);
        LAppDelegate.getInstance().run();
    }
}


