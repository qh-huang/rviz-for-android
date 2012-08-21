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
package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;

import android.opengl.GLES20;

/**
 * A shape capable of being drawn in any OpenGL ES 2.0 draw mode, specified at construction. Each vertex can optionally have an associated color.
 * This class is capable of being drawn as a series of triangles, triangle strip, triangle fan, lines, line strip, or points.
 * @author azimmerman
 *
 */
public class GenericColoredShape extends BaseShape {

	protected final FloatBuffer vertices;
	protected final FloatBuffer colors;
	protected int vertexCount = 0;
	protected boolean useVertexColors = false;
	private final int drawMode;

	public GenericColoredShape(Camera cam, int drawMode, float[] vertices, float[] colors) {
		super(cam);
		this.drawMode = drawMode;
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.colors = Vertices.toFloatBuffer(colors);
		
		vertexCount = vertices.length / 3;
		
		useVertexColors = true;
		super.setProgram(GLSLProgram.ColoredVertex());
	}
	
	public GenericColoredShape(Camera cam, int drawMode, float[] vertices) {
		super(cam);
		this.drawMode = drawMode;
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.colors = null;
		
		vertexCount = vertices.length / 3;
		
		useVertexColors = false;
		super.setProgram(GLSLProgram.FlatColor());		
	}
	
	@Override
	public void draw(GL10 glUnused) {
		super.draw(glUnused);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		calcMVP();
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, vertices);
		
		if(useVertexColors) {
			GLES20.glEnableVertexAttribArray(ShaderVal.ATTRIB_COLOR.loc);
			GLES20.glVertexAttribPointer(ShaderVal.ATTRIB_COLOR.loc, 4, GLES20.GL_FLOAT, false, 0, colors);
		} else {
			GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
		}
		
		GLES20.glDrawArrays(drawMode, 0, vertexCount);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
	}
}
