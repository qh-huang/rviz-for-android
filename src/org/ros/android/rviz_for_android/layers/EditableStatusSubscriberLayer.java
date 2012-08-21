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

import org.ros.android.renderer.Camera;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.FrameCheckStatusPropertyController;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty.StatusColor;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;

public abstract class EditableStatusSubscriberLayer<T extends org.ros.internal.message.Message> extends EditableSubscriberLayer<T> {

	private String messageTypeName;
	private StringProperty propTopic;
	private final ReadOnlyProperty propStatus = new ReadOnlyProperty("Status", "OK", null);

	protected final BoolProperty prop = new BoolProperty("Enabled", true, null);
	protected FrameCheckStatusPropertyController statusController;
	protected GraphName frame;

	public EditableStatusSubscriberLayer(GraphName topicName, String messageType, Camera cam) {
		super(topicName, messageType, cam);

		propTopic = new StringProperty("Topic", topicName.toString(), new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				EditableStatusSubscriberLayer.this.changeTopic(newval);
			}
		});

		prop.addSubProperty(propStatus);
		prop.addSubProperty(propTopic);

		messageTypeName = messageType.substring(messageType.indexOf("/") + 1);
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		statusController = new FrameCheckStatusPropertyController(propStatus, camera, frameTransformTree);
		statusController.setFrameChecking(false);
		statusController.setStatus("No " + messageTypeName + " messages received", StatusColor.WARN);
	}

	@Override
	protected void changeTopic(String topic) {
		super.changeTopic(topic);
		if(statusController == null)
			return;
		statusController.setFrameChecking(false);
		statusController.setStatus("No " + messageTypeName + " messages received", StatusColor.WARN);
	}

	protected abstract String getMessageFrameId(T msg);

	@Override
	protected void onMessageReceived(T msg) {
		String msgFrame = getMessageFrameId(msg);
		camera.informNewFixedFrame(msgFrame);
		if(frame == null || !frame.toString().equals(msgFrame)) {
			frame = GraphName.of(msgFrame);
			statusController.setTargetFrame(frame);
		}
		if(messageCount == 1)
			statusController.setFrameChecking(true);
	}

}
