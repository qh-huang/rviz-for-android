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
package org.ros.android.rviz_for_android;

import org.ros.android.renderer.AngleControlView;
import org.ros.android.renderer.AngleControlView.OnAngleChangeListener;
import org.ros.android.renderer.Translation2DControlView;
import org.ros.android.renderer.TranslationControlView;
import org.ros.android.renderer.TranslationControlView.OnMouseUpListener;
import org.ros.android.renderer.TranslationControlView.OnMoveListener;
import org.ros.android.renderer.layer.InteractiveObject;
import org.ros.android.rviz_for_android.drawable.InteractiveMarkerControl.InteractionMode;
import org.ros.android.rviz_for_android.geometry.Vector2;

import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;

public class InteractiveControlManager {

	private static final int MSG_SHOW = 0;
	private static final int MSG_HIDE = 1;
	private static final int MSG_MOVE = 2;
	private static final int MSG_HIDE_ALL = 3;

	private static volatile InteractionMode interactionMode = InteractionMode.NONE;
	private static View activeControl;
	private static AngleControlView angleControl;
	private static TranslationControlView translateControl;
	private static Translation2DControlView translateControl2D;

	private static final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(activeControl == null)
				return;

			switch(msg.what) {
			case MSG_MOVE:
				if(interactionMode == InteractionMode.MOVE_ROTATE && msg.obj != null)
					activeControl = (View) msg.obj;
				activeControl.setX(msg.arg1 - (activeControl.getWidth() / 2));
				activeControl.setY(msg.arg2 - (activeControl.getHeight() / 2));
				break;
			case MSG_SHOW:
				if(interactionMode == InteractionMode.MOVE_ROTATE && msg.obj != null)
					activeControl = (View) msg.obj;
				activeControl.setVisibility(View.VISIBLE);
				break;
			case MSG_HIDE:
				activeControl.setVisibility(View.INVISIBLE);
				break;
			case MSG_HIDE_ALL:
				angleControl.setVisibility(AngleControlView.INVISIBLE);
				translateControl.setVisibility(TranslationControlView.INVISIBLE);
				translateControl2D.setVisibility(Translation2DControlView.INVISIBLE);
				break;
			}

			if(interactionMode == InteractionMode.MOVE_AXIS && msg.obj != null)
				setTranslationAngle((Vector2) msg.obj);
		}
	};

	private static void setTranslationAngle(Vector2 screenvec) {
		if(screenvec.length() > 10)
			translateControl.setDrawAngle(Math.PI / 2 + Math.atan2(screenvec.getY(), screenvec.getX()));
	}

	private InteractiveObject activeObject;
	
	public boolean isMoving = false;

	public InteractiveControlManager(AngleControlView acView, TranslationControlView tcView, Translation2DControlView tcView2D) {
		angleControl = acView;
		translateControl = tcView;
		translateControl2D = tcView2D;

		acView.setOnAngleChangeListener(new OnAngleChangeListener() {
			@Override
			public void angleChange(float newAngle, float delta) {
				activeObject.rotate(delta);
				if(interactionMode == InteractionMode.MOVE_ROTATE) {
					activeControl = translateControl2D;
					handler.sendEmptyMessage(MSG_HIDE);
					isMoving = true;
				}
			}
		});

		acView.setOnMouseUpListener(new OnMouseUpListener() {
			@Override
			public void mouseUp(MotionEvent e) {
				if(interactionMode == InteractionMode.MOVE_ROTATE) {
					activeControl = translateControl2D;
					handler.sendEmptyMessage(MSG_SHOW);
					isMoving = false;
				}
			}
		});

		tcView.setOnMoveListener(new OnMoveListener() {
			@Override
			public void onMove(float X, float Y) {
				isMoving = true;
				translateControl.setVisibility(TranslationControlView.INVISIBLE);
				activeObject.translate(X, Y);
			}

			@Override
			public void onMoveStart() {
				activeObject.translateStart();
			}
		});

		tcView.setOnMouseUpListener(new OnMouseUpListener() {
			@Override
			public void mouseUp(MotionEvent e) {
				translateControl.setVisibility(TranslationControlView.VISIBLE);
				isMoving = false;
			}
		});

		tcView2D.setOnMoveListener(new OnMoveListener() {
			@Override
			public void onMove(float X, float Y) {
				translateControl2D.setVisibility(Translation2DControlView.INVISIBLE);
				activeObject.translate(X, Y);
				isMoving = true;
			}
			@Override
			public void onMoveStart() {
			}
		});

		tcView2D.setOnMouseUpListener(new OnMouseUpListener() {
			@Override
			public void mouseUp(MotionEvent e) {
				translateControl2D.setVisibility(Translation2DControlView.VISIBLE);
				isMoving = false;
			}
		});
	}

	public void showInteractiveController(InteractiveObject activeObject) {
		// Hide all other controls
		handler.sendEmptyMessage(MSG_HIDE_ALL);

		// Determine which control to use for the current selected object
		this.activeObject = activeObject;
		interactionMode = activeObject.getInteractionMode();
		switch(interactionMode) {
		case MENU:
			break;
		case ROTATE_AXIS:
			activeControl = angleControl;
			handler.sendEmptyMessage(MSG_SHOW);
			break;
		case MOVE_AXIS:
			activeControl = translateControl;
			handler.obtainMessage(MSG_SHOW, activeObject.getScreenMotionVector()).sendToTarget();
			break;
		case MOVE_PLANE:
			activeControl = translateControl2D;
			handler.sendEmptyMessage(MSG_SHOW);
			break;
		case MOVE_ROTATE:
			activeControl = translateControl2D;
			handler.obtainMessage(MSG_SHOW, angleControl).sendToTarget();
			handler.obtainMessage(MSG_SHOW, translateControl2D).sendToTarget();
		}
	}

	public void moveInteractiveController(int x, int y) {
		switch(interactionMode) {
		case MOVE_AXIS:
			handler.obtainMessage(MSG_MOVE, x, y, activeObject.getScreenMotionVector()).sendToTarget();
			break;
		case MOVE_ROTATE:
			handler.obtainMessage(MSG_MOVE, x, y, angleControl).sendToTarget();
			handler.obtainMessage(MSG_MOVE, x, y, translateControl2D).sendToTarget();
			break;
		default:
			handler.obtainMessage(MSG_MOVE, x, y).sendToTarget();
		}
	}

	public void hideInteractiveController() {
		handler.sendEmptyMessage(MSG_HIDE_ALL);
	}
}
