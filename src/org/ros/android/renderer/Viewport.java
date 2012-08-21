/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.renderer;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Viewport {

  private final int width;
  private final int height;
  
  private float[] mProjection = new float[16];

  public Viewport(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public void apply(GL10 glUnused) {
	GLES20.glViewport(0, 0, width, height);
    
    float zNear = 0.1f;
    float zFar = 1000;
    float fov = 45.0f;
    float aspectRatio = (float)width/(float)height;
    
    float fW, fH;
    fH = (float) (Math.tan(fov/360*Math.PI) * zNear);
    fW = fH*aspectRatio;

    Matrix.frustumM(mProjection, 0, -fW, fW, -fH, fH, zNear, zFar);
  }
  
  public float[] getProjectionMatrix() {
	  return mProjection;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}
