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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.layer.InteractiveObject;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.BaseShapeInterface;
import org.ros.android.renderer.shapes.Cleanable;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.drawable.loader.ColladaLoader;
import org.ros.android.rviz_for_android.urdf.InvalidXMLException;
import org.ros.android.rviz_for_android.urdf.ServerConnection;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.rosjava_geometry.Transform;

public class ColladaMesh implements BaseShapeInterface, UrdfDrawable, Cleanable {
	protected static final ColladaLoader loader = new ColladaLoader();

	/**
	 * @param filename
	 *            The name of the DAE file to be loaded, parsed directly from the URDF, contains the "package://" or "html://" piece
	 * @return a Collada mesh
	 */
	public static ColladaMesh newFromFile(String filename, Camera cam) {
		List<BaseShape> retval = null;

		// Download the .DAE file if it doesn't exist
		String loadedFilename = ServerConnection.getInstance().getFile(filename);

		if(loadedFilename == null)
			throw new RuntimeException("Unable to download the file!");

		// Get the image prefix
		String imgPrefix = ServerConnection.getInstance().getPrefix(filename);

		synchronized(loader) {
			loader.setCamera(cam);
			try {
				loader.readDae(ServerConnection.getInstance().getContext().openFileInput(loadedFilename), imgPrefix);
			} catch(IOException e) {
				return null;
			} catch(InvalidXMLException e) {
				return null;
			}
			retval = loader.getGeometries();
		}
		return new ColladaMesh(cam, retval);
	}

	protected List<BaseShape> geometries;
	protected Camera cam;

	protected ColladaMesh(Camera cam, List<BaseShape> geometries) {
		this.cam = cam;
		this.geometries = geometries;
	}

	private float[] scale = new float[] { 1f, 1f, 1f };
	private Transform transform = Transform.identity();

	@Override
	public void draw(GL10 glUnused, Transform transform, float[] scale) {
		cam.pushM();
		this.scale = scale;
		this.transform = transform;
		cam.scaleM(scale[0], scale[1], scale[2]);
		cam.applyTransform(transform);

		for(BaseShape g : geometries)
			g.draw(glUnused);

		cam.popM();
	}

	@Override
	public void draw(GL10 glUnused) {
		cam.pushM();
		cam.scaleM(scale[0], scale[1], scale[2]);
		cam.applyTransform(transform);
		for(BaseShape g : geometries)
			g.draw(glUnused);

		cam.popM();
	}

	@Override
	public void selectionDraw(GL10 glUnused) {
		cam.pushM();
		cam.scaleM(scale[0], scale[1], scale[2]);
		cam.applyTransform(transform);
		for(BaseShape g : geometries)
			g.selectionDraw(glUnused);

		cam.popM();
	}

	@Override
	public void cleanup() {
		for(BaseShapeInterface g : geometries) {
			if(g instanceof Cleanable) {
				Cleanable cs = (Cleanable) g;
				cs.cleanup();
			}
		}
	}

	@Override
	public void setSelected(boolean isSelected) {
		// TODO Auto-generated method stub
	}

	@Override
	public Map<String, String> getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerSelectable() {
		for(BaseShape g : geometries)
			g.registerSelectable();
	}

	@Override
	public void removeSelectable() {
		for(BaseShape g : geometries)
			g.removeSelectable();
	}

	@Override
	public InteractiveObject getInteractiveObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProgram(GLSLProgram shader) {
		// TODO Auto-generated method stub
	}

	private Color color = new Color(0.25f, 0.6f, 0.15f, 1.0f);

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public void setColor(Color color) {
		this.color = color;
		for(BaseShape g : geometries)
			g.setColor(color);
	}

	@Override
	public Transform getTransform() {
		return transform;
	}

	@Override
	public void setTransform(Transform pose) {
		this.transform = pose;
	}

	@Override
	public void setInteractiveObject(InteractiveObject io) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isSelected() {
		// TODO Auto-generated method stub
		return false;
	}

}
