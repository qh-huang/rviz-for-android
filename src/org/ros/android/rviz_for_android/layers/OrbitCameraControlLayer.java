/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.rviz_for_android.layers;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.OrbitCamera;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.rviz_for_android.geometry.Vector2;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OrbitCameraControlLayer extends DefaultLayer {

	private final Context context;
	protected boolean enableScrolling = true;
	private static final float TOUCH_ORBIT_COEFFICIENT = 0.25f;

	private GestureDetector gestureDetector;
	private ScaleGestureDetector scaleGestureDetector;

	private Vector2 prevScaleCenter = Vector2.zero();

	public OrbitCameraControlLayer(Context context, Camera cam) {
		super(cam);
		this.context = context;
	}

	@Override
	public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
		if(gestureDetector != null) {
			if(camera.getSelectionManager().getInteractiveControlManager().isMoving)
				return false;
			
			if(gestureDetector.onTouchEvent(event)) {
				return true;
			}
			return scaleGestureDetector.onTouchEvent(event);
		}
		return false;
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, final Camera camera) {
		if(!(camera instanceof OrbitCamera))
			throw new IllegalArgumentException("OrbitCameraControlLayer can only be used with OrbitCamera objects!");
		final OrbitCamera cam = (OrbitCamera) camera;

		handler.post(new Runnable() {
			@Override
			public void run() {
				gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onDoubleTap(MotionEvent e) {
						if(enableScrolling && !cam.getSelectionManager().interactiveMode()) {
							cam.resetTargetFrame();
							cam.resetLookTarget();
							cam.resetZoom();
							cam.getSelectionManager().signalCameraMoved();
						}
						return true;
					}

					@Override
					public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
						cam.moveOrbitPosition(distanceX * TOUCH_ORBIT_COEFFICIENT, distanceY * TOUCH_ORBIT_COEFFICIENT);
						cam.getSelectionManager().signalCameraMoved();
						requestRender();
						return true;
					}

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
						if(!cam.getSelectionManager().interactiveMode())
							cam.flingCamera(velocityX, velocityY);
						cam.getSelectionManager().signalCameraMoved();
						return true;
					}

					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						cam.getSelectionManager().beginSelectionDraw((int) e.getX(), (int) e.getY());
						return super.onSingleTapUp(e);
					}
				});

				scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
					@Override
					public boolean onScaleBegin(ScaleGestureDetector detector) {
						prevScaleCenter = new Vector2(detector.getFocusX(), detector.getFocusY());
						return true;
					}

					@Override
					public boolean onScale(ScaleGestureDetector detector) {						
						Vector2 diff = prevScaleCenter.subtract(new Vector2(detector.getFocusX(), detector.getFocusY()));
						if(enableScrolling)
							cam.moveCameraScreenCoordinates((float) diff.getX() / 50, (float) diff.getY() / 50);

						prevScaleCenter = new Vector2(detector.getFocusX(), detector.getFocusY());

						camera.zoomCamera(detector.getScaleFactor());

						cam.getSelectionManager().signalCameraMoved();

						requestRender();

						return true;
					}
				});
			}
		});
	}
}
