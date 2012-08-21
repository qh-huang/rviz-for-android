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
package org.ros.android.rviz_for_android.layers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Utility;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.renderer.layer.Selectable;
import org.ros.android.renderer.layer.SelectableLayer;
import org.ros.android.renderer.shapes.Cleanable;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;
import org.ros.android.rviz_for_android.drawable.ColladaMesh;
import org.ros.android.rviz_for_android.drawable.Cube;
import org.ros.android.rviz_for_android.drawable.Cylinder;
import org.ros.android.rviz_for_android.drawable.Sphere;
import org.ros.android.rviz_for_android.drawable.StlMesh;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.ButtonProperty;
import org.ros.android.rviz_for_android.prop.FrameCheckStatusPropertyController;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty.StatusColor;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.android.rviz_for_android.urdf.Component;
import org.ros.android.rviz_for_android.urdf.InvalidXMLException;
import org.ros.android.rviz_for_android.urdf.ServerConnection;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.android.rviz_for_android.urdf.UrdfLink;
import org.ros.android.rviz_for_android.urdf.UrdfReader;
import org.ros.android.rviz_for_android.urdf.UrdfReader.UrdfReadingProgressListener;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class RobotModelLayer extends DefaultLayer implements LayerWithProperties, SelectableLayer {

	private static final String DEFAULT_PARAM_VALUE = "/robot_description";
	private BoolProperty prop = new BoolProperty("Enabled", true, null);
	private FrameTransformTree ftt;
	private Camera cam;
	private UrdfReader reader;
	private ParameterTree params;
	private String parameter = DEFAULT_PARAM_VALUE;
	
	private volatile boolean readyToDraw = false;
	private List<UrdfLink> urdf;

	// The visual and collision draw options exist both as properties and booleans in the RobotModel layer
	// Boolean access times are required to properly draw the model
	private volatile boolean drawVis = true;
	private volatile boolean drawCol = false;

	private Activity context;
	private ServerConnection serverConnection = ServerConnection.getInstance();

	private FrameCheckStatusPropertyController statusController;

	public RobotModelLayer(Camera cam) {
		super(cam);

		this.context = serverConnection.getContext();
		this.serverConnection = ServerConnection.getInstance();

		reader = new UrdfReader();

		prop.addSubProperty(new ReadOnlyProperty("Status", "OK", null));

		prop.addSubProperty(new StringProperty("Parameter", DEFAULT_PARAM_VALUE, new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				if(newval.length() > 0) {
					parameter = newval;
					reloadUrdf();
				}
			}
		}));
		prop.addSubProperty(new ButtonProperty("Refresh", "Reload URDF", new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				reloadUrdf();
			}
		}));
		prop.addSubProperty(new BoolProperty("Visual", drawVis, new PropertyUpdateListener<Boolean>() {
			@Override
			public void onPropertyChanged(Boolean newval) {
				drawVis = newval;
				requestRender();
			}
		}));
		prop.addSubProperty(new BoolProperty("Collision", drawCol, new PropertyUpdateListener<Boolean>() {
			@Override
			public void onPropertyChanged(Boolean newval) {
				drawCol = newval;
				requestRender();
			}
		}));

		cyl = new Cylinder(cam, 1f, 1f);
		cube = new Cube(cam);
		sphere = new Sphere(cam, 1f);
	}

	private Component vis;
	private Component col;

	private Cylinder cyl;
	private Cube cube;
	private Sphere sphere;

	private Map<Component, UrdfDrawable> meshes = new HashMap<Component, UrdfDrawable>();

	@Override
	public void draw(GL10 glUnused) {
		if(!readyToDraw || ftt == null || urdf == null)
			return;

		for(UrdfLink ul : urdf) {
			vis = ul.getVisual();
			col = ul.getCollision();

			cam.pushM();
			// Transform to the URDF link's frame
			cam.applyTransform(Utility.newTransformIfPossible(ftt, ul.getName(), cam.getFixedFrame()));

			// Draw the shape
			if(drawVis && vis != null) {
				drawComponent(glUnused, vis);
			}
			if(drawCol && col != null) {
				drawComponent(glUnused, col);
			}

			cam.popM();
		}
	}

	private void drawComponent(GL10 glUnused, Component com) {
		switch(com.getType()) {
		case BOX:
			cube.setColor(com.getMaterial_color());
			cube.draw(glUnused, com.getOrigin(), com.getSize());
			break;
		case CYLINDER:
			cyl.setColor(com.getMaterial_color());
			cyl.draw(glUnused, com.getOrigin(), com.getLength(), com.getRadius());
			break;
		case SPHERE:
			sphere.setColor(com.getMaterial_color());
			sphere.draw(glUnused, com.getOrigin(), com.getRadius());
			break;
		case MESH:
			UrdfDrawable ud = meshes.get(com);
			if(ud != null)
				ud.draw(glUnused, com.getOrigin(), com.getSize());
			break;
		}
	}

	private boolean loadMesh(String meshResourceName, Component com) {
		// Don't reload the mesh if we already have a copy
		if(meshes.containsKey(com))
			return true;

		UrdfDrawable ud;
		if(meshResourceName.toLowerCase().endsWith(".dae")) {
			ColladaMesh cm = ColladaMesh.newFromFile(meshResourceName, cam);
			if(cm == null)
				return false;
			cm.registerSelectable();
			ud = (UrdfDrawable) cm;
		} else if(meshResourceName.toLowerCase().endsWith(".stl")) {
			StlMesh sm = StlMesh.newFromFile(meshResourceName, cam);
			sm.registerSelectable();
			if(sm == null)
				return false;
			ud = (UrdfDrawable) sm;
		} else {
			Log.e("RobotModel", "Unknown mesh type! " + meshResourceName);
			return false;
		}

		meshes.put(com, (UrdfDrawable) ud);

		return true;
	}

	@Override
	public void onStart(final ConnectedNode node, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		this.ftt = frameTransformTree;
		this.cam = camera;
		this.params = node.getParameterTree();

		parameter = DEFAULT_PARAM_VALUE;
		reloadUrdf();

		statusController = new FrameCheckStatusPropertyController(prop.<ReadOnlyProperty> getProperty("Status"), camera, frameTransformTree);
	}

	private void reloadUrdf() {
		final LoadUrdf lu = new LoadUrdf();
		Log.d("RobotModel", "Reloading URDF");
		clearMeshes();

		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				lu.execute(parameter);
			}
		});
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue() && (drawVis || drawCol);
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

	private class LoadUrdf extends AsyncTask<String, String, Void> {

		private Toast progressToast;

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			progressToast.setText(values[0]);
			progressToast.show();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			readyToDraw = false;
			progressToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);

			reader.setListener(new UrdfReadingProgressListener() {
				@Override
				public void readLink(int linkNumber, int linkCount) {
					StringBuilder sb = new StringBuilder();
					sb.append("URDF Loading: [");
					double percent = 25.0 * linkNumber / linkCount;
					int markers = 0;
					for(int i = 0; i < percent; i++) {
						sb.append('|');
						markers++;
					}
					for(int i = markers; i < 25; i++) {
						sb.append(' ');
					}
					sb.append(']');
					publishProgress(sb.toString());
				}
			});
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			readyToDraw = true;
		}

		@Override
		protected Void doInBackground(String... parameters) {
			String param = parameters[0];

			// Parse the URDF
			statusController.setFrameChecking(false);
			String urdf_xml = null;
			if(params.has(param)) {
				urdf_xml = params.getString(param);
				statusController.setStatus("Loading URDF...", StatusColor.OK);
			} else {
				publishProgress("Invalid parameter " + param);
				statusController.setStatus("Invalid parameter", StatusColor.ERROR);
				return null;
			}

			try {
				reader.readUrdf(urdf_xml);
			} catch(InvalidXMLException e1) {
				publishProgress("Improperly formatted URDF!");
				statusController.setStatus("URDF is improperly formatted!", StatusColor.ERROR);
				return null;
			}

			urdf = Collections.synchronizedList(reader.getUrdf());

			// Load any referenced models
			statusController.setStatus("Loading meshes and textures...", StatusColor.OK);
			for(UrdfLink ul : urdf) {
				for(Component c : ul.getComponents()) {
					if(c.getType() == Component.GEOMETRY.MESH && !meshes.containsKey(c)) {
						if(loadMesh(c.getMesh(), c))
							publishProgress("Loaded " + c.getMesh());
						else {
							// If the model failed to load, show an error message and leave it visible for long enough for the user to know
							publishProgress("Error loading " + c.getMesh() + "!");
							try {
								Thread.sleep(500);
							} catch(InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}

			statusController.setFrameChecking(true);
			return null;
		}
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		super.onShutdown(view, node);
		readyToDraw = false;
		clearMeshes();
		statusController.cleanup();
	}

	private void clearMeshes() {
		Log.i("RobotModel", "Clearing meshes and URDF");

		if(meshes != null) {
			for(UrdfDrawable ud : meshes.values()) {
				if(ud instanceof Cleanable)
					((Cleanable) ud).cleanup();
			}
			meshes.clear();
		}

		urdf = null;
	}

	@Override
	public void selectionDraw(GL10 glUnused) {
		if(!readyToDraw || ftt == null || urdf == null)
			return;

		for(UrdfLink ul : urdf) {
			vis = ul.getVisual();
			col = ul.getCollision();

			cam.pushM();
			// Transform to the URDF link's frame
			cam.applyTransform(Utility.newTransformIfPossible(ftt, ul.getName(), cam.getFixedFrame()));

			// Draw the shape
			if(drawVis && vis != null) {
				selectDrawComponent(glUnused, vis);
			}
			if(drawCol && col != null) {
				selectDrawComponent(glUnused, col);
			}

			cam.popM();
		}
	}

	private void selectDrawComponent(GL10 glUnused, Component com) {
		switch(com.getType()) {
		case BOX:
			cube.setColor(com.getMaterial_color());
			cube.selectionDraw(glUnused);
			break;
		case CYLINDER:
			cyl.setColor(com.getMaterial_color());
			cyl.selectionDraw(glUnused);
			break;
		case SPHERE:
			sphere.setColor(com.getMaterial_color());
			sphere.selectionDraw(glUnused);
			break;
		case MESH:
			UrdfDrawable ud = meshes.get(com);
			if(ud != null)
				ud.selectionDraw(glUnused);
			break;
		}
	}

	@Override
	public Set<Selectable> getSelectables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AvailableLayerType getType() {
		return AvailableLayerType.RobotModel;
	}
}
