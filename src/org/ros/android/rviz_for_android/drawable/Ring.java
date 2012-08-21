/*
 * Copyright (c) 2012, Willow Garage, Inc.
 * All rights reserved.
 *
 * Willow Garage licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.ros.android.rviz_for_android.drawable;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.renderer.shapes.TriangleStripShape;

import android.opengl.GLES20;
import android.util.FloatMath;

public class Ring extends TriangleStripShape {
	private static final Color DEFAULT_COLOR = new Color(1f, 0f, 0f, 1f);
	
	public static Ring newRing(Camera cam, float Rinner, float Router, int segments) {
		if(Rinner > Router || segments < 3)
			return null;
		
		int nVertices = 3*(segments*2 + 2);
		float[] vertices = new float[nVertices];
		float[] normals = new float[nVertices];
		float dTheta = (float) (2*Math.PI/(float)segments);
		
		int idx = 0;
		int nIdx = 0;
		// Count backwards from 2*pi to 0 to wind the triangles CCW
		for(float theta = (float) (2f*Math.PI); theta >= 0; theta -= dTheta) {
			// Outer
			vertices[idx++] = 0f;
			vertices[idx++] = FloatMath.sin(theta) * Router;
			vertices[idx++] = FloatMath.cos(theta) * Router;
			
			
			// Inner
			vertices[idx++] = 0f;
			vertices[idx++] = FloatMath.sin(theta) * Rinner;
			vertices[idx++] = FloatMath.cos(theta) * Rinner;
			
			// Normals
			normals[nIdx++] = 1f;
			normals[nIdx++] = 0f;
			normals[nIdx++] = 0f;
			normals[nIdx++] = 1f;
			normals[nIdx++] = 0f;
			normals[nIdx++] = 0f;
		}
		
		// Append the last two points if needed
		if(idx < nVertices) {
			// Outer
			vertices[idx++] = 0f;
			vertices[idx++] = 0f;
			vertices[idx++] = Router;
			
			
			// Inner			
			vertices[idx++] = 0f;
			vertices[idx++] = 0f;
			vertices[idx++] = Rinner;
			
			// Normals
			normals[nIdx++] = 1f;
			normals[nIdx++] = 0f;
			normals[nIdx++] = 0f;
			normals[nIdx++] = 1f;
			normals[nIdx++] = 0f;
			normals[nIdx++] = 0f;
		}

		return new Ring(cam, vertices, normals);
	}
	
	private Ring(Camera cam, float[] vertices, float[] normals) {
		super(cam, vertices, normals, DEFAULT_COLOR);
	}

	@Override
	public void draw(GL10 glUnused) {
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		super.draw(glUnused);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
	}

}
