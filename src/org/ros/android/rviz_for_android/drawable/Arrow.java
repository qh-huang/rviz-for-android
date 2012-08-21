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

import android.opengl.GLES20;
import android.util.FloatMath;

public class Arrow extends BaseShape {
	private static final Color DEFAULT_COLOR = new Color(0.6f, 0.25f, 0.72f, 1f);
	private static final float TWO_PI = (float) (2 * Math.PI);
	private final FloatBuffer cylSideVerticesBuf;
	private final FloatBuffer cylSideNormalsBuf;
	private final FloatBuffer cylBottomVerticesBuf;
	private final FloatBuffer cylBottomNormalsBuf;
	private final FloatBuffer coneSideVerticesBuf;
	private final FloatBuffer coneSideNormalsBuf;
	private final FloatBuffer coneBottomVerticesBuf;
	private final FloatBuffer coneBottomNormalsBuf;
	private final int coneFanTriangleCount;
	private final int coneStripTriangleCount;
	private final int cylFanTriangleCount;
	private final int cylStripTriangleCount;
	
	public static Arrow newArrow(Camera cam, float cylRadius, float coneRadius, float cylHeight, float coneHeight) {
		
		int coneStripTriangleCount;
		int coneFanTriangleCount;

		FloatBuffer coneSideVerticesBuf;
		FloatBuffer coneSideNormalsBuf;

		FloatBuffer coneBottomVerticesBuf;
		FloatBuffer coneBottomNormalsBuf;

		int sides = 17;
		float CONE_Z_OFFSET = cylHeight; // 1f
		float CONE_RADIUS = coneRadius; //0.05
		float CONE_HEIGHT = coneHeight;  //0.3
		
		double dTheta = TWO_PI / sides;
		float[] sideVertices = new float[(sides + 1) * 6];
		float[] sideNormals = new float[(sides + 1) * 6];
		int sideVidx = 0;
		int sideNidx = 0;
		float[] bottomVertices = new float[(sides + 2) * 3];
		float[] bottomNormals = new float[(sides + 2) * 3];
		int capVidx = 3;
		int capNidx = 3;
		bottomVertices[0] = 0f;
		bottomVertices[1] = 0f;
		bottomVertices[2] = CONE_Z_OFFSET;
		bottomNormals[0] = 0f;
		bottomNormals[1] = 0f;
		bottomNormals[2] = -1f;

		for(float theta = 0; theta <= (TWO_PI + dTheta); theta += dTheta) {
			sideVertices[sideVidx++] = 0f; // X
			sideVertices[sideVidx++] = 0f; // Y
			sideVertices[sideVidx++] = CONE_Z_OFFSET+CONE_HEIGHT; // Z

			sideVertices[sideVidx++] = FloatMath.cos(theta)*CONE_RADIUS; // X
			sideVertices[sideVidx++] = FloatMath.sin(theta)*CONE_RADIUS; // Y
			sideVertices[sideVidx++] = CONE_Z_OFFSET; // Z

			sideNormals[sideNidx++] = FloatMath.cos(theta) / 1.224f; // X
			sideNormals[sideNidx++] = FloatMath.sin(theta) / 1.224f; // Y
			sideNormals[sideNidx++] = .5f / 1.224f;// 0f; // Z

			sideNormals[sideNidx++] = FloatMath.cos(theta) / 1.224f; // X
			sideNormals[sideNidx++] = FloatMath.sin(theta) / 1.224f; // Y
			sideNormals[sideNidx++] = .5f / 1.224f;// 0f; // Z

			// X
			bottomVertices[capVidx++] = FloatMath.cos(TWO_PI - theta)*CONE_RADIUS;
			// Y
			bottomVertices[capVidx++] = FloatMath.sin(TWO_PI - theta)*CONE_RADIUS;
			// Z
			bottomVertices[capVidx++] = CONE_Z_OFFSET;

			// Normals
			bottomNormals[capNidx++] = 0f;
			bottomNormals[capNidx++] = 0f;
			bottomNormals[capNidx++] = -1f;
		}
		coneStripTriangleCount = sideVertices.length / 3;
		coneFanTriangleCount = sides + 2;
		coneSideVerticesBuf = Vertices.toFloatBuffer(sideVertices);
		coneSideNormalsBuf = Vertices.toFloatBuffer(sideNormals);
		coneBottomVerticesBuf = Vertices.toFloatBuffer(bottomVertices);
		coneBottomNormalsBuf = Vertices.toFloatBuffer(bottomNormals);

		// Generate cylinder

		int cylStripTriangleCount;
		int cylFanTriangleCount;
		FloatBuffer cylSideVerticesBuf;
		FloatBuffer cylSideNormalsBuf;
		FloatBuffer cylBottomVerticesBuf;
		FloatBuffer cylBottomNormalsBuf;

		float CYL_HEIGHT = cylHeight; //1f
		float CYL_Z_OFFSET = 0.0f;
		float CYL_RADIUS = cylRadius; //0.025f;
		sideVertices = new float[(sides + 1) * 6];
		sideNormals = new float[(sides + 1) * 6];
		sideVidx = 0;
		sideNidx = 0;
		bottomVertices = new float[(sides + 2) * 3];
		bottomNormals = new float[(sides + 2) * 3];
		capVidx = 3;
		capNidx = 3;

		bottomVertices[0] = 0f;
		bottomVertices[1] = 0f;
		bottomVertices[2] = CYL_Z_OFFSET;
		bottomNormals[0] = 0f;
		bottomNormals[1] = 0f;
		bottomNormals[2] = -1f;

		for(float theta = 0; theta <= (TWO_PI + dTheta); theta += dTheta) {
			sideVertices[sideVidx++] = FloatMath.cos(theta)*CYL_RADIUS; // X
			sideVertices[sideVidx++] = FloatMath.sin(theta)*CYL_RADIUS; // Y
			sideVertices[sideVidx++] = CYL_HEIGHT+CYL_Z_OFFSET; // Z

			sideVertices[sideVidx++] = FloatMath.cos(theta)*CYL_RADIUS; // X
			sideVertices[sideVidx++] = FloatMath.sin(theta)*CYL_RADIUS; // Y
			sideVertices[sideVidx++] = CYL_Z_OFFSET; // Z

			sideNormals[sideNidx++] = FloatMath.cos(theta); // X
			sideNormals[sideNidx++] = FloatMath.sin(theta); // Y
			sideNormals[sideNidx++] = 0f; // Z

			sideNormals[sideNidx++] = FloatMath.cos(theta); // X
			sideNormals[sideNidx++] = FloatMath.sin(theta); // Y
			sideNormals[sideNidx++] = 0f; // Z

			// X
			bottomVertices[capVidx++] = FloatMath.cos(TWO_PI - theta)*CYL_RADIUS;
			// Y
			bottomVertices[capVidx++] = FloatMath.sin(TWO_PI - theta)*CYL_RADIUS;
			// Z
			bottomVertices[capVidx++] = CYL_Z_OFFSET;

			// Normals
			bottomNormals[capNidx++] = 0f;
			bottomNormals[capNidx++] = 0f;
			bottomNormals[capNidx++] = -1f;
		}
		cylStripTriangleCount = sideVertices.length / 3;
		cylFanTriangleCount = sides + 2;
		cylSideVerticesBuf = Vertices.toFloatBuffer(sideVertices);
		cylSideNormalsBuf = Vertices.toFloatBuffer(sideNormals);
		cylBottomVerticesBuf = Vertices.toFloatBuffer(bottomVertices);
		cylBottomNormalsBuf = Vertices.toFloatBuffer(bottomNormals);
		
		return new Arrow(cam,cylStripTriangleCount, cylFanTriangleCount, coneStripTriangleCount, coneFanTriangleCount, cylSideVerticesBuf,cylSideNormalsBuf, cylBottomVerticesBuf, cylBottomNormalsBuf, coneSideVerticesBuf, coneSideNormalsBuf, coneBottomVerticesBuf, coneBottomNormalsBuf);
	}
	
	public static Arrow newDefaultArrow(Camera cam) {
		return Arrow.newArrow(cam, 0.025f, 0.05f, 1f, 0.3f);
	}

	private Arrow(Camera cam, int cylStripTriangleCount, int cylFanTriangleCount, int coneStripTriangleCount, int coneFanTriangleCount, FloatBuffer cylSideVerticesBuf, FloatBuffer cylSideNormalsBuf, FloatBuffer cylBottomVerticesBuf, FloatBuffer cylBottomNormalsBuf, FloatBuffer coneSideVerticesBuf, FloatBuffer coneSideNormalsBuf, FloatBuffer coneBottomVerticesBuf, FloatBuffer coneBottomNormalsBuf) {
		super(cam);
		this.cylStripTriangleCount = cylStripTriangleCount;
		this.cylFanTriangleCount = cylFanTriangleCount;
		this.coneStripTriangleCount = coneStripTriangleCount;
		this.coneFanTriangleCount = coneFanTriangleCount;
		this.cylSideVerticesBuf = cylSideVerticesBuf;
		this.cylSideNormalsBuf = cylSideNormalsBuf;
		this.cylBottomVerticesBuf = cylBottomVerticesBuf;
		this.cylBottomNormalsBuf = cylBottomNormalsBuf;
		this.coneSideVerticesBuf = coneSideVerticesBuf;
		this.coneSideNormalsBuf = coneSideNormalsBuf;
		this.coneBottomVerticesBuf = coneBottomVerticesBuf;
		this.coneBottomNormalsBuf = coneBottomNormalsBuf;
		super.setProgram(GLSLProgram.FlatShaded());
		super.setColor(DEFAULT_COLOR);
	}

	@Override
	public void draw(GL10 glUnused) {
		cam.pushM();
				
		super.draw(glUnused);
		cam.rotateM(90, 0, 1, 0);
		
		calcMVP();
		calcNorm();
		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glUniformMatrix3fv(getUniform(ShaderVal.NORM_MATRIX), 1, false, NORM, 0);
		GLES20.glUniform3f(getUniform(ShaderVal.LIGHTVEC), lightVector[0], lightVector[1], lightVector[2]);

		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glEnableVertexAttribArray(ShaderVal.NORMAL.loc);

		// Draw cylinder
		// Draw sides
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, cylSideVerticesBuf);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, cylSideNormalsBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, cylStripTriangleCount);

		// Draw bottom
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, cylBottomVerticesBuf);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, cylBottomNormalsBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, cylFanTriangleCount);

		// Draw cone
		// Draw sides
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, coneSideVerticesBuf);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, coneSideNormalsBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, coneStripTriangleCount);

		// Draw bottom
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, coneBottomVerticesBuf);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, coneBottomNormalsBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, coneFanTriangleCount);
		cam.popM();
	}

	@Override
	public void selectionDraw(GL10 glUnused) {
		cam.pushM();
		super.selectionDraw(glUnused);
		cam.rotateM(90, 0, 1, 0);
		calcMVP();
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);

		// Draw cylinder
		// Draw sides
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, cylSideVerticesBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, cylStripTriangleCount);

		// Draw bottom
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, cylBottomVerticesBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, cylFanTriangleCount);

		// Draw cone
		// Draw sides
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, coneSideVerticesBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, coneStripTriangleCount);

		// Draw bottom
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, coneBottomVerticesBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, coneFanTriangleCount);
		cam.popM();
		
		super.selectionDrawCleanup();
	}
	
	
}
