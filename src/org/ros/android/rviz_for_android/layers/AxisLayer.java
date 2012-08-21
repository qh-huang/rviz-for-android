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

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.renderer.layer.TfLayer;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;
import org.ros.android.rviz_for_android.drawable.Axis;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.FloatProperty;
import org.ros.android.rviz_for_android.prop.FrameCheckStatusPropertyController;
import org.ros.android.rviz_for_android.prop.GraphNameProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;

public class AxisLayer extends DefaultLayer implements LayerWithProperties, TfLayer {

	private Axis axis;
	private BoolProperty prop;
	private GraphNameProperty propParent;

	private FrameCheckStatusPropertyController statusController;

	public AxisLayer(Camera cam) {
		super(cam);
		axis = new Axis(cam);
		prop = new BoolProperty("enabled", true, null);
		prop.addSubProperty(new ReadOnlyProperty("Status", "OK", null));
		prop.addSubProperty(new FloatProperty("Scale", 1f, new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				axis.setScale(newval);
			}
		}).setValidRange(0.001f, 10000f));
		
		propParent = new GraphNameProperty("Parent", null, cam, new PropertyUpdateListener<GraphName>() {
			@Override
			public void onPropertyChanged(GraphName newval) {
				if(statusController != null)
					statusController.setTargetFrame(newval);
			}
		});
		prop.addSubProperty(propParent);
	}

	@Override
	public void draw(GL10 glUnused) {
		axis.draw(glUnused);
	}
	
	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, final FrameTransformTree frameTransformTree, final Camera camera) {	
		statusController = new FrameCheckStatusPropertyController(prop.<ReadOnlyProperty> getProperty("Status"), camera, frameTransformTree);
	}

	public Property<?> getProperties() {
		return prop;
	}

	public GraphName getFrame() {
		return propParent.getValue();
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		if(statusController != null)
			statusController.cleanup();
		super.onShutdown(view, node);
	}

	@Override
	public AvailableLayerType getType() {
		return AvailableLayerType.Axis;
	}
}
