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

import java.io.FileNotFoundException;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.shapes.BaseShapeInterface;
import org.ros.android.renderer.shapes.BufferedTrianglesShape;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.drawable.loader.StlLoader;
import org.ros.android.rviz_for_android.urdf.ServerConnection;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.rosjava_geometry.Transform;

public class StlMesh extends BufferedTrianglesShape implements UrdfDrawable, BaseShapeInterface {

	private static final StlLoader loader = new StlLoader();
	
	public static StlMesh newFromFile(String filename, Camera cam) {
		float[] v;
		float[] n;
		
		// Download the .DAE file if it doesn't exist
		String loadedFilename = ServerConnection.getInstance().getFile(filename);
		
		synchronized(loader) {
			try {
				loader.load(ServerConnection.getInstance().getContext().openFileInput(loadedFilename));
			} catch(FileNotFoundException e) {
				e.printStackTrace();
				return null;
			}
			v = loader.getVertices();
			n = loader.getNormals();
		}
		return new StlMesh(cam, v, n, new Color(0,1,1,1));
	}
	
	private StlMesh(Camera cam, float[] vertices, float[] normals, Color color) {
		super(cam, vertices, normals, color);
	}
	
	private float[] scale;
	
	@Override
	public void draw(GL10 glUnused, Transform transform, float[] scale) {
		super.transform = transform;
		this.scale = scale;
		super.draw(glUnused);
	}

	@Override
	public void setSelected(boolean isSelected) {
		super.setSelected(isSelected);
	}

	@Override
	protected void scale(Camera cam) {
		cam.scaleM(scale[0], scale[1], scale[2]);
	}
}