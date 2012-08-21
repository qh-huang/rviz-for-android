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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;

import android.opengl.GLES20;

public class Axis extends BaseShape {

	private static final float VERTICES[] = { 0, 0, 0, 0, 0, 1, 0, .25f, .75f, 0, -.25f, .75f,
	0, 0, 0, 0, 1, 0, .25f, .75f, 0, -.25f, .75f, 0,
	0, 0, 0, 1, 0, 0, .75f, .25f, 0, .75f, -.25f, 0 };

	private static final float COLORS[] = { 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1,
	0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
	1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, };

	private static final byte INDEX[] = { 0, 1, 1, 2, 1, 3,
	4, 5, 5, 6, 5, 7,
	8, 9, 9, 10, 9, 11 };

	private FloatBuffer vertexBuffer;
	private FloatBuffer colorBuffer;
	private ByteBuffer indexBuffer;

	private float scale = 1f;
	
	public Axis(Camera cam) {
		super(cam);
		
		super.setProgram(GLSLProgram.ColoredVertex());
		vertexBuffer = Vertices.toFloatBuffer(VERTICES);
		colorBuffer = Vertices.toFloatBuffer(COLORS);
		indexBuffer = Vertices.toByteBuffer(INDEX);
	}
	
	public void setScale(float scale) {
		this.scale = scale;
	}
	
	@Override
	protected void scale(Camera cam) {
		cam.scaleM(scale, scale, scale);
	}

	@Override
	public void draw(GL10 glUnused) {
		cam.pushM();
		super.draw(glUnused);

		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

		GLES20.glEnableVertexAttribArray(ShaderVal.ATTRIB_COLOR.loc);
		GLES20.glVertexAttribPointer(ShaderVal.ATTRIB_COLOR.loc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
		
		calcMVP();
		GLES20.glUniformMatrix4fv(uniformHandles[ShaderVal.MVP_MATRIX.loc], 1, false, MVP, 0);
		GLES20.glDrawElements(GLES20.GL_LINES, 18, GLES20.GL_UNSIGNED_BYTE, indexBuffer);
		cam.popM();
	}
	
}
