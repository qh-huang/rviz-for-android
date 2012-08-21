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

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.rosjava_geometry.Transform;

import android.opengl.GLES20;
import android.util.FloatMath;

public class Cylinder extends BaseShape implements UrdfDrawable {
	private static final Color DEFAULT_COLOR = new Color(0.6f, 0.25f, 0.72f, 1f);
	private static final float TWO_PI = (float) (2 * Math.PI);
	private static int stripTriangleCount;
	private static int fanTriangleCount;

	private static FloatBuffer sideVerticesBuf;
	private static FloatBuffer sideNormalsBuf;

	private static FloatBuffer topVerticesBuf;
	private static FloatBuffer topNormalsBuf;

	private static FloatBuffer bottomVerticesBuf;
	private static FloatBuffer bottomNormalsBuf;

	static {
		int sides = 17;
		double dTheta = TWO_PI / sides;

		float[] sideVertices = new float[(sides + 1) * 6];
		float[] sideNormals = new float[(sides + 1) * 6];

		int sideVidx = 0;
		int sideNidx = 0;

		float[] topVertices = new float[(sides + 2) * 3];
		float[] topNormals = new float[(sides + 2) * 3];
		float[] bottomVertices = new float[(sides + 2) * 3];
		float[] bottomNormals = new float[(sides + 2) * 3];

		int capVidx = 3;
		int capNidx = 3;

		topVertices[0] = 0f;
		topVertices[1] = 0f;
		topVertices[2] = .5f;
		topNormals[0] = 0f;
		topNormals[1] = 0f;
		topNormals[2] = 1f;
		bottomVertices[0] = 0f;
		bottomVertices[1] = 0f;
		bottomVertices[2] = -.5f;
		bottomNormals[0] = 0f;
		bottomNormals[1] = 0f;
		bottomNormals[2] = -1f;

		for(float theta = 0; theta <= (TWO_PI + dTheta); theta += dTheta) {
			sideVertices[sideVidx++] = FloatMath.cos(theta); // X
			sideVertices[sideVidx++] = FloatMath.sin(theta); // Y
			sideVertices[sideVidx++] = 0.5f; // Z

			sideVertices[sideVidx++] = FloatMath.cos(theta); // X
			sideVertices[sideVidx++] = FloatMath.sin(theta); // Y
			sideVertices[sideVidx++] = -0.5f; // Z

			sideNormals[sideNidx++] = FloatMath.cos(theta); // X
			sideNormals[sideNidx++] = FloatMath.sin(theta); // Y
			sideNormals[sideNidx++] = 0f; // Z

			sideNormals[sideNidx++] = FloatMath.cos(theta); // X
			sideNormals[sideNidx++] = FloatMath.sin(theta); // Y
			sideNormals[sideNidx++] = 0f; // Z

			// X
			topVertices[capVidx] = FloatMath.cos(theta);
			bottomVertices[capVidx++] = FloatMath.cos(TWO_PI - theta);
			// Y
			topVertices[capVidx] = FloatMath.sin(theta);
			bottomVertices[capVidx++] = FloatMath.sin(TWO_PI - theta);
			// Z
			topVertices[capVidx] = 0.5f;
			bottomVertices[capVidx++] = -0.5f;

			// Normals
			topNormals[capNidx] = 0f;
			bottomNormals[capNidx++] = 0f;
			topNormals[capNidx] = 0f;
			bottomNormals[capNidx++] = 0f;
			topNormals[capNidx] = 1f;
			bottomNormals[capNidx++] = -1f;
		}
		stripTriangleCount = sideVertices.length / 3;
		fanTriangleCount = sides + 2;
		sideVerticesBuf = Vertices.toFloatBuffer(sideVertices);
		sideNormalsBuf = Vertices.toFloatBuffer(sideNormals);
		topVerticesBuf = Vertices.toFloatBuffer(topVertices);
		topNormalsBuf = Vertices.toFloatBuffer(topNormals);
		bottomVerticesBuf = Vertices.toFloatBuffer(bottomVertices);
		bottomNormalsBuf = Vertices.toFloatBuffer(bottomNormals);
	}
	
	private float radius;
	private float length;

	public Cylinder(Camera cam, float radius, float length) {
		super(cam);
		super.setProgram(GLSLProgram.FlatShaded());
		super.setColor(DEFAULT_COLOR);
		
		this.radius = radius;
		this.length = length; 
	}

	@Override
	public void draw(GL10 glUnused) {
		draw(glUnused, transform, length, radius);
	}

	public void draw(GL10 glUnused, Transform transform, float length, float radius) {
		setTransform(transform);
		this.radius = radius;
		this.length = length;
		cam.pushM();
		super.draw(glUnused);

		cam.scaleM(this.radius, this.radius, this.length);
		calcMVP();
		calcNorm();
		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glUniformMatrix3fv(getUniform(ShaderVal.NORM_MATRIX), 1, false, NORM, 0);
		GLES20.glUniform3f(getUniform(ShaderVal.LIGHTVEC), lightVector[0], lightVector[1], lightVector[2]);

		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glEnableVertexAttribArray(ShaderVal.NORMAL.loc);

		// Draw sides
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, sideVerticesBuf);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, sideNormalsBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, stripTriangleCount);

		// Draw top
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, topVerticesBuf);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, topNormalsBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, fanTriangleCount);

		// Draw bottom
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, bottomVerticesBuf);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, bottomNormalsBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, fanTriangleCount);

		cam.popM();
	}

	@Override
	public void selectionDraw(GL10 glUnused) {
		cam.pushM();
		super.selectionDraw(glUnused);

		cam.scaleM(this.radius, this.radius, this.length);

		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);

		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);

		// Draw sides
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, sideVerticesBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, stripTriangleCount);

		// Draw top
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, topVerticesBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, fanTriangleCount);

		// Draw bottom
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, bottomVerticesBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, fanTriangleCount);

		cam.popM();
		super.selectionDrawCleanup();
	}

	@Override
	public void draw(GL10 glUnused, Transform transform, float[] scale) {
		cam.pushM();
		cam.scaleM(scale[0], scale[1], scale[2]);
		draw(glUnused, transform, radius, length);
		cam.popM();
	}
	
	
}
