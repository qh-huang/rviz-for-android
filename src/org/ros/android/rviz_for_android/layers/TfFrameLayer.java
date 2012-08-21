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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.AvailableFrameTracker;
import org.ros.android.renderer.AvailableFrameTracker.FrameAddedListener;
import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Utility;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;
import org.ros.android.rviz_for_android.drawable.Axis;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.FloatProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;

public class TfFrameLayer extends DefaultLayer implements LayerWithProperties {

	private Axis axis;
	private FrameAddedListener listener;
	private FrameTransformTree ftt;
	private Set<GraphName> frames = Collections.newSetFromMap(new ConcurrentHashMap<GraphName, Boolean>());
	private BoolProperty prop;
	private float scale = 1f;

	public TfFrameLayer(Camera cam) {
		super(cam);
		axis = new Axis(cam);

		prop = new BoolProperty("Enabled", true, null);
		prop.addSubProperty(new FloatProperty("Scale", scale, new Property.PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				scale = newval;
			}
		}));
	}

	@Override
	public void draw(GL10 glUnused) {
		for(GraphName g : frames) {
			camera.pushM();
			camera.applyTransform(Utility.newTransformIfPossible(ftt, g, camera.getFixedFrame()));
			camera.scaleM(scale, scale, scale);
			axis.draw(glUnused);
			camera.popM();
		}
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		ftt = frameTransformTree;
		listener = new AvailableFrameTracker.FrameAddedListener() {
			@Override
			public void informFrameAdded(Set<String> newFrames) {
				for(String s : newFrames)
					frames.add(GraphName.of(s));
			}
		};
		camera.getFrameTracker().addListener(listener);
		for(String s : camera.getFrameTracker().getAvailableFrames())
			frames.add(GraphName.of(s));
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		camera.getFrameTracker().removeListener(listener);
		super.onShutdown(view, node);
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}

	@Override
	public AvailableLayerType getType() {
		return AvailableLayerType.TFLayer;
	}
}
