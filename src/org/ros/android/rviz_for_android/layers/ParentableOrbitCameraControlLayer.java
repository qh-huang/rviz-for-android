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

import java.util.HashSet;
import java.util.Set;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Camera.AvailableFixedFrameListener;
import org.ros.android.renderer.OrbitCamera;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;
import org.ros.android.rviz_for_android.prop.GraphNameProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.ViewProperty;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;

public class ParentableOrbitCameraControlLayer extends OrbitCameraControlLayer implements LayerWithProperties {

	private ViewProperty prop = new ViewProperty("null", null, null);
	private GraphNameProperty fixedFrameSelector;
	private OrbitCamera cam;
	
	public ParentableOrbitCameraControlLayer(Context context, Camera cam) {
		super(context, cam);
		fixedFrameSelector = new GraphNameProperty("Fixed", null, cam, null);
		prop.addSubProperty(fixedFrameSelector);
	}

	@Override
	public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
		return super.onTouchEvent(view, event);
	}

	private Set<String> availableFixedFrames = new HashSet<String>();
	
	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, final Camera camera) {
		if(!(camera instanceof OrbitCamera))
			throw new IllegalArgumentException("Can not use a ParentableOrbitCameraControlLayer with a Camera that isn't a subclass of OrbitCamera");

		cam = (OrbitCamera) camera;
		
		fixedFrameSelector.setDefaultItem(camera.getFixedFrame().toString(), false);
		fixedFrameSelector.setValue(camera.getFixedFrame());
		fixedFrameSelector.addUpdateListener(new PropertyUpdateListener<GraphName>() {
			public void onPropertyChanged(GraphName newval) {
				if(newval == null)
					camera.resetFixedFrame();
				else
					camera.setFixedFrame(newval);
			}
		});

		cam.setAvailableFixedFrameListener(new AvailableFixedFrameListener() {
			@Override
			public void newFixedFrameAvailable(String newFrame) {
				if(availableFixedFrames.add(newFrame))
					fixedFrameSelector.addToDefaultList(newFrame);
			}
		});

		super.onStart(connectedNode, handler, frameTransformTree, camera);
	}
	
	public Property<?> getProperties() {
		return prop;
	}

	public void setTargetFrame(String newval) {
		if(newval == null) {
			cam.resetTargetFrame();
			super.enableScrolling = true;
		} else {
			cam.setTargetFrame(GraphName.of(newval));
			super.enableScrolling = false;
		}
	}

	@Override
	public AvailableLayerType getType() {
		return null;
	}
}