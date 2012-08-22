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
import org.ros.android.renderer.layer.TfLayer;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;
import org.ros.android.rviz_for_android.drawable.PointCloud2GL;
import org.ros.android.rviz_for_android.prop.ButtonProperty;
import org.ros.android.rviz_for_android.prop.ColorProperty;
import org.ros.android.rviz_for_android.prop.FloatProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.ListProperty;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.namespace.GraphName;

import sensor_msgs.PointCloud2;
import android.content.Context;

public class PointCloud2Layer extends EditableStatusSubscriberLayer<sensor_msgs.PointCloud2> implements TfLayer, LayerWithProperties {
	private static final String[] COLOR_MODES = new String[]{"Flat Color", "Channel"};
	private ListProperty propChannelSelect;
	private PointCloud2GL pc;
	
	public PointCloud2Layer(GraphName topicName, Camera cam, Context context) {
		super(topicName, sensor_msgs.PointCloud2._TYPE, cam);
		
		pc = new PointCloud2GL(cam, context);
		
		// Color mode selection property
		final ListProperty propColorMode = new ListProperty("Color Mode", 0, null).setList(COLOR_MODES);
		propChannelSelect = new ListProperty("Channel", 0, new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				if(pc != null)
					pc.setChannelColorMode(newval);
			}
		});
		// Flat color selection
		final ColorProperty propColorSelect = new ColorProperty("Flat Color", pc.getColor(), new PropertyUpdateListener<Color>() {
			@Override
			public void onPropertyChanged(Color newval) {
				if(pc != null)
					pc.setFlatColorMode(newval);
			}
		});
		// Channel coloring range bounds
		final FloatProperty propMinRange = new FloatProperty("Min", 0f, null);
		final FloatProperty propMaxRange = new FloatProperty("Max", 1f, null);
		// Range calculation button
		final ButtonProperty propCalcRange = new ButtonProperty("Compute Range", "Compute", new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				if(pc != null) {
					float[] range = pc.computeRange();
					propMinRange.setValue(range[0]);
					propMaxRange.setValue(range[1]);
				}
			}
		});

		propMaxRange.addUpdateListener(new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				propMinRange.setValidRange(Float.NEGATIVE_INFINITY, newval - Float.MIN_NORMAL);
				if(pc != null)
					pc.setRange(propMinRange.getValue(), newval);
			}
		});		
		
		propMinRange.addUpdateListener(new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				propMinRange.setValidRange(newval + Float.MIN_NORMAL, Float.POSITIVE_INFINITY);
				if(pc != null)
					pc.setRange(newval, propMaxRange.getValue());
			}
		});
		
		propColorMode.addUpdateListener(new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				boolean isChannelColor = (newval == 1);
				propColorSelect.setVisible(!isChannelColor);
				if(!isChannelColor && pc != null) {
					pc.setFlatColorMode(propColorSelect.getValue());
				} else {
					propChannelSelect.setValue(0);
					pc.setChannelColorMode(0);
				}
				propCalcRange.setVisible(isChannelColor);
				propChannelSelect.setVisible(isChannelColor);
				propMinRange.setVisible(isChannelColor);
				propMaxRange.setVisible(isChannelColor);
			}
		});		

		prop.addSubProperty(propColorMode);
		prop.addSubProperty(propChannelSelect);
		prop.addSubProperty(propColorSelect);
		prop.addSubProperty(propCalcRange);
		prop.addSubProperty(propMinRange);
		prop.addSubProperty(propMaxRange);
		
		// Set the initial visibilities
		boolean isChannelColor = false;
		propColorSelect.setVisible(!isChannelColor);
		propCalcRange.setVisible(isChannelColor);
		propChannelSelect.setVisible(isChannelColor);
		propMinRange.setVisible(isChannelColor);
		propMaxRange.setVisible(isChannelColor);
	}
	
	@Override
	public void draw(GL10 glUnused) {
		super.draw(glUnused);
		pc.draw(glUnused);
	}

	@Override
	public GraphName getFrame() {
		return frame;
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

	@Override
	protected String getMessageFrameId(PointCloud2 msg) {
		return msg.getHeader().getFrameId();
	}

	@Override
	public void onMessageReceived(PointCloud2 msg) {
		super.onMessageReceived(msg);
		pc.setData(msg);
		propChannelSelect.setList(pc.getChannelNames());
	}

	@Override
	public AvailableLayerType getType() {
		return AvailableLayerType.PointCloud2;
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}
}