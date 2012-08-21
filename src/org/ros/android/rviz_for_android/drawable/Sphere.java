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
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.rosjava_geometry.Transform;

import android.util.FloatMath;

public class Sphere extends TriangleStripShape implements UrdfDrawable {

	private static float[] vertexData;
	private static float[] normalData;

	static {
		int vIndex = 0, nIndex = 0, m_Stacks = 17, m_Slices = 14;
		float m_Scale = 0.98f, m_Squash = 1.0f;
		
		vertexData = new float[3 * ((m_Slices * 2 + 2) * m_Stacks)];
		normalData = new float[(3 * (m_Slices * 2 + 2) * m_Stacks)];

		int phiIdx, thetaIdx;

		for(phiIdx = 0; phiIdx < m_Stacks; phiIdx++) {
			float phi0 = (float) Math.PI * ((float) (phiIdx + 0) * (1.0f / (float) (m_Stacks)) - 0.5f);
			float phi1 = (float) Math.PI * ((float) (phiIdx + 1) * (1.0f / (float) (m_Stacks)) - 0.5f);

			float cosPhi0 = FloatMath.cos(phi0);
			float sinPhi0 = FloatMath.sin(phi0);
			float cosPhi1 = FloatMath.cos(phi1);
			float sinPhi1 = FloatMath.sin(phi1);

			float cosTheta, sinTheta;

			for(thetaIdx = 0; thetaIdx < m_Slices; thetaIdx++) {
				float theta = (float) (2.0f * (float) Math.PI * ((float) thetaIdx) * (1.0 / (float) (m_Slices - 1)));
				cosTheta = FloatMath.cos(theta);
				sinTheta = FloatMath.sin(theta);

				vertexData[vIndex + 0] = m_Scale * cosPhi0 * cosTheta;
				vertexData[vIndex + 1] = m_Scale * (sinPhi0 * m_Squash);
				vertexData[vIndex + 2] = m_Scale * (cosPhi0 * sinTheta);

				vertexData[vIndex + 3] = m_Scale * cosPhi1 * cosTheta;
				vertexData[vIndex + 4] = m_Scale * (sinPhi1 * m_Squash);
				vertexData[vIndex + 5] = m_Scale * (cosPhi1 * sinTheta);

				normalData[nIndex + 0] = cosPhi0 * cosTheta;
				normalData[nIndex + 1] = sinPhi0;
				normalData[nIndex + 2] = cosPhi0 * sinTheta;

				normalData[nIndex + 3] = cosPhi1 * cosTheta;
				normalData[nIndex + 4] = sinPhi1;
				normalData[nIndex + 5] = cosPhi1 * sinTheta;

				vIndex += 2 * 3;
				nIndex += 2 * 3;
			}

			vertexData[vIndex + 0] = vertexData[vIndex + 3] = vertexData[vIndex - 3];
			vertexData[vIndex + 1] = vertexData[vIndex + 4] = vertexData[vIndex - 2];
			vertexData[vIndex + 2] = vertexData[vIndex + 5] = vertexData[vIndex - 1];
		}
	}

	public Sphere(Camera cam, float radius) {
		super(cam, vertexData, normalData, new Color(0.5f,0.5f,1f,1f));
		this.radius = radius;
	}

	private float radius = 1f;

	public void draw(GL10 glUnused, Transform transform, float radius) {
		this.radius = radius;
		super.setTransform(transform);
		super.draw(glUnused);
	}

	@Override
	protected void scale(Camera cam) {
		cam.scaleM(radius, radius, radius);
	}

	@Override
	public void draw(GL10 glUnused, Transform transform, float[] scale) {
		this.radius = scale[0];
		super.setTransform(transform);
		super.draw(glUnused);
	}
}
