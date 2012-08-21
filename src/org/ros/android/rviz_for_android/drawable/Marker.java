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

import geometry_msgs.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.SelectionManager;
import org.ros.android.renderer.Utility;
import org.ros.android.renderer.layer.InteractiveObject;
import org.ros.android.renderer.shapes.BaseShapeInterface;
import org.ros.android.renderer.shapes.Cleanable;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.renderer.shapes.GenericColoredShape;
import org.ros.android.rviz_for_android.urdf.ServerConnection;
import org.ros.android.rviz_for_android.urdf.UrdfDrawable;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class Marker implements Cleanable {
	public static enum DrawType {
		PRIMITIVE, // Triangle list, line list, line strip, and points
		LIST, // Cube list and sphere list
		MESH, // A mesh resource
		SHAPE, // Cube, sphere, cylinder, etc.
		ERROR // An invalid marker
	};

	private static final float[] UNIT_SCALE = new float[] { 1f, 1f, 1f };

	// Mesh loading and storage
	private static Map<String, BaseShapeInterface> loadedMeshes = new HashMap<String, BaseShapeInterface>();
	private ServerConnection serverConnection;

	// Drawing
	protected Camera cam;
	private BaseShapeInterface shape;
	private Color color;
	private float[] scale = UNIT_SCALE;
	private GraphName frame;
	private FrameTransformTree ftt;
	private boolean isViewFacing = false;

	// Marker properties
	// Identification
	protected String namespace;
	protected int id;
	// Duration
	private long endTime;
	private int duration;
	// Type
	protected int markerMessageType;
	protected DrawType markerDrawType = DrawType.SHAPE;
	private boolean useMeshMaterials = false;
	private Transform shapeTransform = Transform.identity();

	// Shape array
	private int shapeArraySize;
	private List<Point> shapeArrayPositions = new ArrayList<Point>();
	private List<Color> shapeArrayColors = new ArrayList<Color>();
	private boolean useIndividualShapeArrayColors;

	public Marker(visualization_msgs.Marker msg, Camera cam, FrameTransformTree ftt) {
		namespace = msg.getNs();
		id = msg.getId();
		this.ftt = ftt;

		this.cam = cam;
		this.serverConnection = ServerConnection.getInstance();
		markerMessageType = msg.getType();
		frame = msg.getFrameLocked() ? GraphName.of(msg.getHeader().getFrameId()) : null;
		scale = new float[] { (float) msg.getScale().getX(), (float) msg.getScale().getY(), (float) msg.getScale().getZ() };
		duration = msg.getLifetime().secs * 1000;
		endTime = System.currentTimeMillis() + duration;
		color = new Color(msg.getColor().getR(), msg.getColor().getG(), msg.getColor().getB(), msg.getColor().getA());
		useMeshMaterials = msg.getMeshUseEmbeddedMaterials();

		initMarker(msg);
	}

	public Marker(BaseShapeInterface shape, Color color, Camera cam, FrameTransformTree ftt) {
		namespace = "none";
		id = -99;
		this.ftt = ftt;

		this.cam = cam;
		this.serverConnection = null;
		markerMessageType = 0;
		markerDrawType = DrawType.SHAPE;
		this.shape = shape;
		this.color = color;
		shape.setColor(color);
		duration = 0;
		endTime = 0;
	}

	private void initMarker(visualization_msgs.Marker msg) {
		switch(markerMessageType) {
		case visualization_msgs.Marker.CUBE:
			markerDrawType = DrawType.SHAPE;
			shape = new Cube(cam);
			break;
		case visualization_msgs.Marker.SPHERE:
			markerDrawType = DrawType.SHAPE;
			shape = new Sphere(cam, 0.5f);
			break;
		case visualization_msgs.Marker.CYLINDER:
			markerDrawType = DrawType.SHAPE;
			shape = new Cylinder(cam, 0.5f, 1f);
			break;
		case visualization_msgs.Marker.ARROW:
			markerDrawType = DrawType.SHAPE;
			shape = Arrow.newDefaultArrow(cam);
			break;
		case visualization_msgs.Marker.MESH_RESOURCE:
			markerDrawType = DrawType.MESH;
			if(!loadedMeshes.containsKey(msg.getMeshResource())) {
				shape = (BaseShapeInterface) loadMesh(msg.getMeshResource());
				loadedMeshes.put(msg.getMeshResource(), shape);
			} else {
				shape = loadedMeshes.get(msg.getMeshResource());
			}
			break;
		case visualization_msgs.Marker.CUBE_LIST:
			markerDrawType = DrawType.LIST;
			shape = new Cube(cam);
			initArray(msg);
			if(!useIndividualShapeArrayColors)
				shape.setColor(color);
			break;
		case visualization_msgs.Marker.SPHERE_LIST:
			markerDrawType = DrawType.LIST;
			shape = new Sphere(cam, 0.5f);
			initArray(msg);
			if(!useIndividualShapeArrayColors)
				shape.setColor(color);
			break;
		case visualization_msgs.Marker.LINE_LIST:
			markerDrawType = DrawType.PRIMITIVE;
			initArray(msg);
			float[] vertices = initArrayPositions();
			if(vertices.length % 2 == 0) {
				if(useIndividualShapeArrayColors)
					shape = new GenericColoredShape(cam, GLES20.GL_LINES, vertices, initArrayColors());
				else
					shape = new GenericColoredShape(cam, GLES20.GL_LINES, vertices);
			} else {
				markerDrawType = DrawType.ERROR;
			}
			break;
		case visualization_msgs.Marker.LINE_STRIP:
			markerDrawType = DrawType.PRIMITIVE;
			initArray(msg);
			vertices = initArrayPositions();
			if(vertices.length % 2 == 0) {
				if(useIndividualShapeArrayColors)
					shape = new GenericColoredShape(cam, GLES20.GL_LINE_STRIP, vertices, initArrayColors());
				else
					shape = new GenericColoredShape(cam, GLES20.GL_LINE_STRIP, vertices);

			} else {
				markerDrawType = DrawType.ERROR;
			}
			break;
		case visualization_msgs.Marker.TRIANGLE_LIST:
			markerDrawType = DrawType.PRIMITIVE;
			initArray(msg);
			vertices = initArrayPositions();

			if(vertices.length % 3 == 0) {
				if(useIndividualShapeArrayColors)
					shape = new GenericColoredShape(cam, GLES20.GL_TRIANGLES, vertices, initArrayColors());
				else
					shape = new GenericColoredShape(cam, GLES20.GL_TRIANGLES, vertices);

			} else {
				markerDrawType = DrawType.ERROR;
			}
			break;
		case visualization_msgs.Marker.POINTS:
			markerDrawType = DrawType.PRIMITIVE;
			initArray(msg);
			vertices = initArrayPositions();

			if(useIndividualShapeArrayColors)
				shape = new GenericColoredShape(cam, GLES20.GL_POINTS, vertices, initArrayColors());
			else
				shape = new GenericColoredShape(cam, GLES20.GL_POINTS, vertices);
			
			break;
		default:
			Log.e("MarkerLayer", "Unknown marker type: " + msg.getType());
			markerDrawType = DrawType.ERROR;
		}

		if(markerDrawType != DrawType.ERROR) {
			shapeTransform = Utility.correctTransform(Transform.fromPoseMessage(msg.getPose()));
			shape.setTransform(shapeTransform);
			shape.setColor(color);
		}
	}

	private void initArray(visualization_msgs.Marker msg) {
		shapeArrayPositions = msg.getPoints();
		shapeArraySize = shapeArrayPositions.size();
		useIndividualShapeArrayColors = (shapeArraySize == msg.getColors().size());

		if(useIndividualShapeArrayColors) {
			for(std_msgs.ColorRGBA c : msg.getColors())
				shapeArrayColors.add(new Color(c.getR(), c.getG(), c.getB(), c.getA()));
		}
	}

	private float[] initArrayPositions() {
		float[] vertices = new float[shapeArraySize * 3];
		int idx = 0;
		for(Point p : shapeArrayPositions) {
			vertices[idx++] = (float) p.getX();
			vertices[idx++] = (float) p.getY();
			vertices[idx++] = (float) p.getZ();
		}
		return vertices;
	}

	private float[] initArrayColors() {
		float[] colors = new float[shapeArraySize * 4];
		int idx = 0;
		for(Color c : shapeArrayColors) {
			colors[idx++] = c.getRed();
			colors[idx++] = c.getGreen();
			colors[idx++] = c.getBlue();
			colors[idx++] = c.getAlpha();
		}
		return colors;
	}

	private static final Color COLOR_WHITE = new Color(1.0f, 1.0f, 1.0f, 1.0f);
	private float[] modelview = new float[16];

	public void draw(GL10 glUnused) {
		if(markerDrawType == DrawType.ERROR)
			return;

		cam.pushM();

		if(frame != null)
			cam.applyTransform(Utility.newTransformIfPossible(ftt, cam.getFixedFrame(), frame));

		cam.scaleM(scale[0], scale[1], scale[2]);

		if(isViewFacing) {
			Matrix.multiplyMM(modelview, 0, cam.getViewMatrix(), 0, cam.getModelMatrix(), 0);
			// Let's try this...
			cam.rotateM((float) -Math.toDegrees(Math.acos(modelview[2])), 0, modelview[10], -modelview[6]);
			// Based on math from http://www.opengl.org/discussion_boards/showthread.php/152761-Q-how-to-draw-a-disk-(gluDisk)-always-facing-the-user
		}

		if(markerDrawType == DrawType.MESH) {
			if(markerMessageType != visualization_msgs.Marker.MESH_RESOURCE || !useMeshMaterials)
				shape.setColor(color);
			else
				shape.setColor(COLOR_WHITE);
			shape.setTransform(shapeTransform);
			shape.draw(glUnused);
		} else if(markerDrawType == DrawType.LIST) {
			cam.applyTransform(shapeTransform);
			for(int i = 0; i < shapeArraySize; i++) {
				cam.pushM();
				Point p = shapeArrayPositions.get(i);
				cam.translateM((float) p.getX(), (float) p.getY(), (float) p.getZ());
				if(useIndividualShapeArrayColors)
					shape.setColor(shapeArrayColors.get(i));
				shape.draw(glUnused);
				cam.popM();
			}
		} else {
			shape.draw(glUnused);
		}
		cam.popM();
	}

	public void selectionDraw(GL10 glUnused) {
		if(markerDrawType == DrawType.ERROR)
			return;

		cam.pushM();

		if(frame != null)
			cam.applyTransform(Utility.newTransformIfPossible(ftt, cam.getFixedFrame(), frame));

		cam.scaleM(scale[0], scale[1], scale[2]);

		if(isViewFacing) {
			// Based on math from http://www.opengl.org/discussion_boards/showthread.php/152761-Q-how-to-draw-a-disk-(gluDisk)-always-facing-the-user
			Matrix.multiplyMM(modelview, 0, cam.getViewMatrix(), 0, cam.getModelMatrix(), 0);
			cam.rotateM((float) -Math.toDegrees(Math.acos(modelview[2])), 0, modelview[10], -modelview[6]);
		}

		if(markerDrawType == DrawType.MESH) {
			if(markerMessageType != visualization_msgs.Marker.MESH_RESOURCE || !useMeshMaterials)
				shape.setColor(color);
			else
				shape.setColor(COLOR_WHITE);
			shape.setTransform(shapeTransform);
			shape.selectionDraw(glUnused);
		} else if(markerDrawType == DrawType.LIST) {
			cam.applyTransform(shapeTransform);
			for(int i = 0; i < shapeArraySize; i++) {
				cam.pushM();
				Point p = shapeArrayPositions.get(i);
				cam.translateM((float) p.getX(), (float) p.getY(), (float) p.getZ());
				shape.selectionDraw(glUnused);
				cam.popM();
			}
		} else {
			shape.selectionDraw(glUnused);
		}
		cam.popM();
	}

	public boolean isExpired() {
		if(duration == 0)
			return false;
		else
			return (System.currentTimeMillis() > endTime);
	}

	private UrdfDrawable loadMesh(String meshResourceName) {
		UrdfDrawable ud;
		if(meshResourceName.toLowerCase().endsWith(".dae")) {
			ud = ColladaMesh.newFromFile(meshResourceName, cam);
		} else if(meshResourceName.toLowerCase().endsWith(".stl")) {
			ud = StlMesh.newFromFile(meshResourceName, cam);
		} else {
			Log.e("MarkerLayer", "Unknown mesh type! " + meshResourceName);
			return null;
		}

		return ud;
	}

	public float[] getScale() {
		return scale;
	}

	public GraphName getFrame() {
		return frame;
	}

	public DrawType getMarkerDrawType() {
		return markerDrawType;
	}

	public boolean isError() {
		return (markerDrawType == DrawType.ERROR);
	}

	public String getNamespace() {
		return namespace;
	}

	public int getId() {
		return id;
	}

	public void setViewFacing(boolean isViewFacing) {
		this.isViewFacing = isViewFacing;
	}

	/**
	 * Register this marker as a selectable interactive marker with the provided InteractiveObject
	 * 
	 * @param io
	 */
	public void setInteractive(InteractiveObject io) {
		if(shape != null) {
			shape.registerSelectable();
			shape.setInteractiveObject(io);
		}
	}

	@Override
	public void cleanup() {
		if(shape != null)
			shape.removeSelectable();
	}

	/**
	 * Colors the marker as though it were selected
	 * @param selected enable/disable coloring. If false, restores the original color of the marker
	 */
	public void setColorAsSelected(boolean selected) {
		if(shape.isSelected())
			return;
		
		if(selected) {
			shape.setColor(SelectionManager.selectedColor);
		} else {
			shape.setColor(color);
		}
	}

}
