package org.ros.android.rviz_for_android.drawable.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.io.IOUtils;
import org.ros.android.renderer.Utility;
import org.ros.rosjava_geometry.Vector3;

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

public class StlLoader {

	private final ByteBuffer bb = ByteBuffer.allocateDirect(4);
	private ByteArrayInputStream in;

	private float[] vertex;
	private float[] normal;

	public StlLoader() {
	}

	private Vector3 normalVec = Vector3.zero();
	private Vector3[] vertexVec = { Vector3.zero(), Vector3.zero(), Vector3.zero(), Vector3.zero() };

	public void load(InputStream stream) {
		try {
			byte[] data = IOUtils.toByteArray(stream);
			in = new ByteArrayInputStream(data);
		} catch(IOException e) {
			e.printStackTrace();
		}
		bb.order(ByteOrder.nativeOrder());

		// Skip 80 byte header
		in.skip(80);

		// Get number of triangles to load
		int nTriangles = getInt();

		vertex = new float[nTriangles * 9];
		normal = new float[nTriangles * 9];

		int vidx = 0;
		int nidx = 0;

		for(int i = 0; i < nTriangles; i++) {
			// Load the normal, check that it's properly formed
			normalVec = new Vector3(getFloat(), getFloat(), getFloat()).normalize();

			// Store the normalized normal
			for(int j = 0; j < 3; j++) {
				normal[nidx++] = (float) normalVec.getX();
				normal[nidx++] = (float) normalVec.getY();
				normal[nidx++] = (float) normalVec.getZ();
			}

			// Load and store the triangle vertices
			// Swap the order if necessary
			for(int b = 0; b < 3; b++) {
				vertexVec[b] = new Vector3(getFloat(), getFloat(), getFloat());
			}
			if(Utility.crossProduct(vertexVec[1].subtract(vertexVec[0]), vertexVec[2].subtract(vertexVec[0])).dotProduct(normalVec) < 0) {
				vertexVec[3] = vertexVec[2];
				vertexVec[2] = vertexVec[1];
				vertexVec[1] = vertexVec[3];
			}
			for(int b = 0; b < 3; b++) {
				vertex[vidx++] = (float) vertexVec[b].getX();
				vertex[vidx++] = (float) vertexVec[b].getY();
				vertex[vidx++] = (float) vertexVec[b].getZ();
			}

			// Skip the footer
			in.skip(2);
		}
	}

	public float[] getVertices() {
		return vertex;
	}

	public float[] getNormals() {
		return normal;
	}

	private byte[] readBytes(int nbytes) {
		byte[] retval = new byte[nbytes];
		in.read(retval, 0, nbytes);
		return retval;
	}

	private int getInt() {
		bb.clear();
		bb.put(readBytes(4));
		bb.position(0);
		return bb.getInt();
	}

	private float getFloat() {
		bb.clear();
		bb.put(readBytes(4));
		bb.position(0);
		return bb.getFloat();
	}
}
