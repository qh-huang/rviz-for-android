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

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Utility;
import org.ros.android.renderer.layer.InteractiveObject;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.BaseShapeInterface;
import org.ros.android.renderer.shapes.Cleanable;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.geometry.Ray;
import org.ros.android.rviz_for_android.geometry.Vector2;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import visualization_msgs.InteractiveMarkerFeedback;
import android.opengl.Matrix;
import android.util.Log;

/**
 * @author azimmerman
 * 
 */
public class InteractiveMarkerControl implements InteractiveObject, Cleanable {

	public static enum InteractionMode {
		MENU(visualization_msgs.InteractiveMarkerControl.MENU, InteractiveMarkerFeedback.MENU_SELECT), MOVE_AXIS(visualization_msgs.InteractiveMarkerControl.MOVE_AXIS, InteractiveMarkerFeedback.POSE_UPDATE), ROTATE_AXIS(visualization_msgs.InteractiveMarkerControl.ROTATE_AXIS, InteractiveMarkerFeedback.POSE_UPDATE), MOVE_PLANE(visualization_msgs.InteractiveMarkerControl.MOVE_PLANE, InteractiveMarkerFeedback.POSE_UPDATE), MOVE_ROTATE(visualization_msgs.InteractiveMarkerControl.MOVE_ROTATE, InteractiveMarkerFeedback.POSE_UPDATE), NONE(visualization_msgs.InteractiveMarkerControl.NONE, InteractiveMarkerFeedback.KEEP_ALIVE);

		public byte val;
		public byte feedbackType;

		InteractionMode(byte val, byte feedbackType) {
			this.val = val;
			this.feedbackType = feedbackType;
		}

		public static InteractionMode fromByte(byte val) {
			for(InteractionMode im : InteractionMode.values())
				if(im.val == val)
					return im;
			return null;
		}
	};

	public static enum OrientationMode {
		FIXED(visualization_msgs.InteractiveMarkerControl.FIXED), INHERIT(visualization_msgs.InteractiveMarkerControl.INHERIT), VIEW_FACING(visualization_msgs.InteractiveMarkerControl.VIEW_FACING);

		public byte val;

		OrientationMode(byte val) {
			this.val = val;
		}

		public static OrientationMode fromByte(byte val) {
			for(OrientationMode im : OrientationMode.values())
				if(im.val == val)
					return im;
			return null;
		}
	}

	private List<Marker> markers = new ArrayList<Marker>();
	private final Camera cam;

	private InteractiveMarker parentControl;
	private InteractionMode interactionMode;
	private OrientationMode orientationMode;

	// private Transform drawTransform = Transform.identity();
	private Vector3 drawPosition = Vector3.zero();
	private Quaternion drawOrientation;
	private Quaternion myOrientation;

	private Vector3 myAxis = Vector3.xAxis();
	private Vector3 myXaxis = Vector3.xAxis();

	private boolean captureScreenPosition = false;

	public InteractiveMarkerControl(visualization_msgs.InteractiveMarkerControl msg, Camera cam, FrameTransformTree ftt, InteractiveMarker parentControl) {
		this.cam = cam;
		this.parentControl = parentControl;
		this.name = msg.getName();
		Log.d("InteractiveMarker", "Created interactive marker control " + name);

		interactionMode = InteractionMode.fromByte(msg.getInteractionMode());
		orientationMode = OrientationMode.fromByte(msg.getOrientationMode());
		isViewFacing = !msg.getIndependentMarkerOrientation() && orientationMode == OrientationMode.VIEW_FACING;

		// Normalize control orientation
		myOrientation = Quaternion.fromQuaternionMessage(msg.getOrientation());
		myOrientation = Utility.correctQuaternion(myOrientation);
		myOrientation = Utility.normalize(myOrientation);
		myXaxis = Utility.quatX(myOrientation);

		drawOrientation = myOrientation;

		for(visualization_msgs.Marker marker : msg.getMarkers()) {
			Marker m = new Marker(marker, cam, ftt);
			m.setViewFacing(isViewFacing);
			if(msg.getInteractionMode() != visualization_msgs.InteractiveMarkerControl.NONE)
				m.setInteractive(this);
			markers.add(m);
		}

		if(msg.getMarkers().isEmpty())
			autoCompleteMarker(msg);

		setParentPosition(parentControl.getPosition());
		setParentOrientation(parentControl.getOrientation());
	}

	private static final Vector3 FIRST_ARROW_TRANSLATE = new Vector3(0.5, 0d, 0d);
	private static final Quaternion FIRST_ARROW_ROTATE = Quaternion.identity();
	private static final Vector3 SECOND_ARROW_TRANSLATE = new Vector3(-0.5, 0d, 0d);
	private static final Quaternion SECOND_ARROW_ROTATE = Quaternion.fromAxisAngle(Vector3.zAxis(), Math.PI);
	private static final Transform FIRST_ARROW_TRANSFORM = new Transform(FIRST_ARROW_TRANSLATE, FIRST_ARROW_ROTATE);
	private static final Transform SECOND_ARROW_TRANSFORM = new Transform(SECOND_ARROW_TRANSLATE, SECOND_ARROW_ROTATE);

	private boolean isViewFacing = false;

	private void autoCompleteMarker(visualization_msgs.InteractiveMarkerControl msg) {
		// Generate a control marker corresponding to the control type
		switch(msg.getInteractionMode()) {
		case visualization_msgs.InteractiveMarkerControl.MOVE_ROTATE:
		case visualization_msgs.InteractiveMarkerControl.MOVE_PLANE:
		case visualization_msgs.InteractiveMarkerControl.ROTATE_AXIS:
			Log.i("InteractiveMarker", "Rotate axis (RING)");
			Ring ring = Ring.newRing(cam, .5f, .65f, 20);
			Marker marker = instantiateControlMarker(ring, generateColor(myOrientation), cam);
			markers.add(marker);
			break;
		case visualization_msgs.InteractiveMarkerControl.MOVE_AXIS:
			Log.i("InteractiveMarker", "Move axis (ARROWS)");
			BaseShape arrowOne = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			arrowOne.setTransform(FIRST_ARROW_TRANSFORM);
			BaseShape arrowTwo = Arrow.newArrow(cam, .08f, .15f, .2f, .2f);
			arrowTwo.setTransform(SECOND_ARROW_TRANSFORM);

			markers.add(instantiateControlMarker(arrowOne, generateColor(myOrientation), cam));
			markers.add(instantiateControlMarker(arrowTwo, generateColor(myOrientation), cam));
			break;
		case visualization_msgs.InteractiveMarkerControl.MENU:
			markers.add(instantiateControlMarker(new Cube(cam), generateColor(myOrientation), cam));
			break;
		default:
			return;
		}
	}

	private Marker instantiateControlMarker(BaseShapeInterface shape, Color color, Camera cam) {
		Marker m = new Marker(shape, color, cam, null);
		m.setInteractive(this);
		m.setViewFacing(isViewFacing);
		return m;
	}

	public void draw(GL10 glUnused) {
		cam.pushM();
		cam.translateM((float) drawPosition.getX(), (float) drawPosition.getY(), (float) drawPosition.getZ());
		cam.rotateM(drawOrientation);

		if(captureScreenPosition)
			captureScreenPosition();
		for(Marker m : markers)
			m.draw(glUnused);
		cam.popM();
	}

	public void selectionDraw(GL10 glUnused) {
		cam.pushM();
		cam.translateM((float) drawPosition.getX(), (float) drawPosition.getY(), (float) drawPosition.getZ());
		cam.rotateM(drawOrientation);

		captureScreenPosition();
		for(Marker m : markers)
			m.selectionDraw(glUnused);
		cam.popM();
	}

	private float[] M = new float[16];
	private float[] MV = new float[16];
	private float[] MVP = new float[16];

	private void captureScreenPosition() {
		System.arraycopy(cam.getModelMatrix(), 0, M, 0, 16);
		Matrix.multiplyMM(MV, 0, cam.getViewMatrix(), 0, cam.getModelMatrix(), 0);
		Matrix.multiplyMM(MVP, 0, cam.getViewport().getProjectionMatrix(), 0, MV, 0);
	}

	/**
	 * Generates a color based on the orientation of the marker
	 * 
	 * @param orientation
	 * @return Color based on marker orientation
	 */
	private Color generateColor(Quaternion orientation) {
		double x, y, z, w;
		x = orientation.getX();
		y = orientation.getY();
		z = orientation.getZ();
		w = orientation.getW();

		double mX, mY, mZ;

		mX = Math.abs(1 - 2 * y * y - 2 * z * z);
		mY = Math.abs(2 * x * y + 2 * z * w);
		mZ = Math.abs(2 * x * z - 2 * y * w);

		double max_xy = mX > mY ? mX : mY;
		double max_yz = mY > mZ ? mY : mZ;
		double max_xyz = max_xy > max_yz ? max_xy : max_yz;

		return new Color(Utility.cap((float) (mX / max_xyz), 0f, 1f), Utility.cap((float) (mY / max_xyz), 0f, 1f), Utility.cap((float) (mZ / max_xyz), 0f, 1f), 0.7f);
	}

	@Override
	public void cleanup() {
		for(Marker m : markers)
			m.cleanup();
	}

	// ******************
	// Selection handling
	// ******************
	@Override
	public void mouseDown() {
		parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.MOUSE_DOWN);
		parentControl.setSelected(true);
		Log.i("InteractiveMarker", "Mouse down!");
		if(interactionMode == InteractionMode.MENU) {
			cam.getSelectionManager().clearSelection();
			parentControl.showMenu(this);
		} else {
			captureScreenPosition = true;
			setMarkerSelection(true);
			parentControl.controlInAction(true);
		}

		switch(orientationMode) {
		case FIXED:
			myAxis = myXaxis;
			break;
		case INHERIT:
			myAxis = parentControl.getOrientation().rotateAndScaleVector(myXaxis);
			break;
		case VIEW_FACING:
			myAxis = getCameraVector(drawPosition).invert();
			break;
		}

	}

	@Override
	public void mouseUp() {
		parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.MOUSE_UP);
		parentControl.setSelected(false);
		Log.i("InteractiveMarker", "Mouse up!");
		captureScreenPosition = false;

		setMarkerSelection(false);
		parentControl.controlInAction(false);
	}

	private static final Vector3 ORIGIN = Vector3.zero();
	private static final Vector3 XAXIS = Vector3.xAxis();
	private float[] resultVec = new float[4];
	private float[] positionVec = new float[4];
	private float x3d, y3d, w3d;

	private double[] getScreenPosition(Vector3 position) {
		positionVec[0] = (float) position.getX();
		positionVec[1] = (float) position.getY();
		positionVec[2] = (float) position.getZ();
		positionVec[3] = 1f;

		Matrix.multiplyMV(resultVec, 0, MVP, 0, positionVec, 0);
		x3d = resultVec[0];
		y3d = resultVec[1];
		w3d = resultVec[2];
		double[] retval = new double[2];
		retval[0] = (x3d * cam.getViewport().getWidth()) / (2.0 * w3d) + (cam.getViewport().getWidth() / 2.0);
		retval[1] = cam.getViewport().getHeight() - ((y3d * cam.getViewport().getHeight()) / (2.0 * w3d) + (cam.getViewport().getHeight() / 2.0));
		return retval;
	}

	@Override
	public int[] getPosition() {
		double[] retval = getScreenPosition(ORIGIN);
		return new int[] { (int) retval[0], (int) retval[1] };
	}

	@Override
	public InteractionMode getInteractionMode() {
		return interactionMode;
	}

	private Quaternion deltaQuaternion;

	@Override
	public void rotate(float dTheta) {
		// Update axis of rotation
		if(orientationMode == OrientationMode.VIEW_FACING) {
			myAxis = getCameraVector(drawPosition).invert();
		} else if(myAxis.dotProduct(getCameraVector(drawPosition)) > 0) {
			Log.d("InteractiveMarker", "Invert rotation axis");
			myAxis = myAxis.invert();
		}

		// Compute quaternion of rotation
		deltaQuaternion = Quaternion.fromAxisAngle(myAxis, Math.toRadians(dTheta));
		parentControl.childRotate(deltaQuaternion);
		parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.POSE_UPDATE);
	}

	private void setMarkerSelection(boolean selected) {
		for(Marker m : markers)
			m.setColorAsSelected(selected);
	}

	private String name;

	public String getName() {
		return name;
	}

	private Vector3 getCameraVector(Vector3 position) {
		return new Vector3(cam.getCamera().getX() * 2 - position.getX(), cam.getCamera().getY() * 2 - position.getY(), cam.getCamera().getZ() * 2 - position.getZ());
	}

	@Override
	public void translate(float X, float Y) {
		if(interactionMode == InteractionMode.MOVE_AXIS) {
			// Step 1: Find nearest point on the screen ray to the mouse touch point
			Vector2 mousePt = new Vector2(X, Y);
			float num = (mousePt.subtract(screenRayStart)).dot(screenRayDir);
			float den = screenRayDir.dot(screenRayDir);
			Vector2 mouseActionPoint = screenRayStart.add(screenRayDir.scalarMultiply(num / den));

			// Step 2: Project the mouse action point into a 3D ray
			Ray mouseRay = getMouseRay(mouseActionPoint.getX(), mouseActionPoint.getY());

			if(Utility.containsNaN(mouseRay.getDirection()) || Utility.containsNaN(mouseRay.getStart())) {
				Log.e("InteractiveMarker", "NaN");
				return;
			}

			Vector3 axisRayStart = new Vector3(M[12], M[13], M[14]);
			Vector3 axisRayEnd = new Vector3(M[0] + M[12], M[1] + M[13], M[2] + M[14]);
			Ray axisRay = Ray.constructRay(axisRayStart, axisRayEnd);

			// Step 3: Find the intersection point on the plane of motion (constrained to the projected axis of motion) to the mouse ray
			Vector3 result = axisRay.getClosestPoint(mouseRay);

			if(result == null || Utility.containsNaN(result)) {
				Log.e("InteractiveMarker", "Rays are parallel or malformed!");
				return;
			}

			parentControl.childTranslate(result);
			parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.POSE_UPDATE);
		} else if(interactionMode == InteractionMode.MOVE_PLANE || interactionMode == InteractionMode.MOVE_ROTATE) {
			// Step 1: Construct the plane of motion
			Ray motionPlane;
			if(isViewFacing) {
				// Find the camera view ray (ray from center of the camera forward)
				int centerX = cam.getViewport().getWidth() / 2;
				int centerY = cam.getViewport().getHeight() / 2;
				Ray cameraRay = getMouseRay(centerX, centerY);
				motionPlane = new Ray(parentControl.getPosition(), cameraRay.getDirection());
			} else {
				// The default action plane is the YZ plane (+X normal direction) rotated by the control orientation
				motionPlane = new Ray(parentControl.getPosition(), myOrientation.rotateAndScaleVector(Vector3.xAxis()));
			}

			// Step 2: Construct the mouse ray
			Ray mouseRay = getMouseRay(X, Y);

			// Step 3: Determine the intersection of the mouse ray and motion plane
			// I don't like the math rviz does, instead using math from
			// http://www.cs.princeton.edu/courses/archive/fall00/cs426/lectures/raycast/sld017.htm
			double t = motionPlane.getDirection().dotProduct(motionPlane.getStart().subtract(mouseRay.getStart())) / motionPlane.getDirection().dotProduct(mouseRay.getDirection());

			Vector3 pointInPlane = mouseRay.getPoint(t);

			parentControl.childTranslate(pointInPlane);
			parentControl.publish(this, visualization_msgs.InteractiveMarkerFeedback.POSE_UPDATE);
		}
	}

	@Override
	public Vector2 getScreenMotionVector() {
		double[] startpt = getScreenPosition(ORIGIN);
		double[] endpt = getScreenPosition(XAXIS);

		Vector2 screenRayDir = new Vector2((float) (endpt[0] - startpt[0]), (float) (endpt[1] - startpt[1]));
		return screenRayDir;
	}

	private int[] viewport = new int[4];
	private float[] project_start = new float[3];
	private float[] project_end = new float[3];

	private Ray getMouseRay(float x, float y) {
		// Step 3: Project the mouse action point into a 3D ray
		viewport[0] = 0;
		viewport[1] = 0;
		viewport[2] = cam.getViewport().getWidth();
		viewport[3] = cam.getViewport().getHeight();

		// Flip the Y coordinate of the mouse action point to put it in OpenGL pixel space
		Utility.unProject(x + cam.getScreenDisplayOffset()[0], viewport[3] - y + cam.getScreenDisplayOffset()[1], 0f, cam.getViewMatrix(), 0, cam.getViewport().getProjectionMatrix(), 0, viewport, 0, project_start, 0);
		Utility.unProject(x + cam.getScreenDisplayOffset()[0], viewport[3] - y + cam.getScreenDisplayOffset()[1], 1f, cam.getViewMatrix(), 0, cam.getViewport().getProjectionMatrix(), 0, viewport, 0, project_end, 0);

		Vector3 mouseray_start = new Vector3(project_start[0], project_start[1], project_start[2]);
		Vector3 mouseray_dir = new Vector3(project_end[0] - project_start[0], project_end[1] - project_start[1], project_end[2] - project_start[2]);

		return new Ray(mouseray_start, mouseray_dir.normalize());
	}

	public void setParentPosition(Vector3 position) {
		drawPosition = position;
	}

	public void setParentOrientation(Quaternion orientation) {
		if(orientationMode != OrientationMode.FIXED)
			drawOrientation = orientation.multiply(myOrientation);
	}

	Vector2 screenRayDir;
	Vector2 screenRayStart;

	@Override
	public void translateStart() {
		// Step 1: Project axis of motion to a ray on the screen (only if this hasn't been done yet)
		double[] startpt = getScreenPosition(ORIGIN);
		double[] endpt = getScreenPosition(XAXIS);
		screenRayDir = new Vector2((float) (endpt[0] - startpt[0]), (float) (endpt[1] - startpt[1])).normalize();
		screenRayStart = new Vector2((float) startpt[0], (float) startpt[1]);

		// If the axis isn't long enough, abort
		if(screenRayDir.length() <= 0.5) {
			Log.e("InteractiveMarker", "screen ray too short, aborting move axis");
			return;
		}

		// motionPlane = new Ray(axisRayStart, rotateSecond.rotateAndScaleVector(rotateFirst.rotateAndScaleVector(axisRay.getDirection()))); //Ray(parentControl.getPosition(), Vector3.xAxis());
		// System.out.println("Axis: " + axisRay);
		// System.out.println("Motion plane normal: " + motionPlane);
	}
}
