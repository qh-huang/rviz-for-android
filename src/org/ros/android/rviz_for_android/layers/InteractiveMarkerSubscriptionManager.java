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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ros.android.renderer.Camera;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;

import visualization_msgs.InteractiveMarkerInit;
import visualization_msgs.InteractiveMarkerUpdate;
import android.os.Handler;
import android.util.Log;

public class InteractiveMarkerSubscriptionManager extends EditableSubscriberLayer<visualization_msgs.InteractiveMarkerInit> {
	private static final String UPDATE_SUFFIX = "/update";
	private static final String UPDATE_FULL_SUFFIX = "/update_full";
	private static final int UPDATE_QUEUE_SIZE = 20;

	// Root topic name
	private String updateTopicName;
	private String updateFullTopicName;

	// Stage tracking
	private static enum Stage {
		WAIT_FOR_INIT, WAIT_FOR_UPDATE, RECEIVE_UPDATES
	};
	
	// Message timeout tracking
	private ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

	private long lastUpdateTime = 0l;
	private Stage stage = Stage.WAIT_FOR_INIT;

	// Expected sequence number
	long expectedSequenceNumber = -1;

	// Update message subscriber
	private Subscriber<visualization_msgs.InteractiveMarkerUpdate> updateSubscriber;
	private final MessageListener<visualization_msgs.InteractiveMarkerUpdate> updateListener = new MessageListener<visualization_msgs.InteractiveMarkerUpdate>() {
		@Override
		public void onNewMessage(visualization_msgs.InteractiveMarkerUpdate msg) {
			receiveUpdateMsg(msg);
		}
	};
	
	// Message receiver callback
	public interface InteractiveMarkerCallback {
		public void receiveUpdate(visualization_msgs.InteractiveMarkerUpdate msg);
		public void receiveInit(visualization_msgs.InteractiveMarkerInit msg);
		public void clear();
	}
	private InteractiveMarkerCallback callback;

	public InteractiveMarkerSubscriptionManager(String topicName, Camera cam, final InteractiveMarkerCallback callback) {
		super(GraphName.of(topicName + UPDATE_FULL_SUFFIX), visualization_msgs.InteractiveMarkerInit._TYPE, cam);
		updateFullTopicName = topicName + UPDATE_FULL_SUFFIX;
		updateTopicName = topicName + UPDATE_SUFFIX;

		this.callback = callback;
		
		Log.d("IMSM", "Created subscription manager");
		Log.d("IMSM", "Listening for InteractiveMarkerInit messages on " + updateFullTopicName);
		
		// Schedule an executor to determine if updates are no longer being received
		exec.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if(stage == Stage.RECEIVE_UPDATES && (System.currentTimeMillis() - lastUpdateTime) > 1000) {
					Log.e("IMSM", "Interactive marker messages timed out!");
					setStage(Stage.WAIT_FOR_INIT);
					clearUpdateSubscriber();
					initSubscriber(updateFullTopicName);
					callback.clear();
				}
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	public void setTopic(String topicName) {
		updateTopicName = topicName + UPDATE_SUFFIX;
		updateFullTopicName = topicName + UPDATE_FULL_SUFFIX;

		// Reset state machine and subscriptions
		stage = Stage.WAIT_FOR_INIT;
		clearUpdateSubscriber();
		super.changeTopic(updateFullTopicName);
		Log.d("IMSM", "Subscription manager changed topics");
	}

	protected void receiveUpdateMsg(InteractiveMarkerUpdate msg) {
		lastUpdateTime = System.currentTimeMillis();
		if(msg.getType() == InteractiveMarkerUpdate.UPDATE)
			expectedSequenceNumber++;

		switch(stage) {
		case WAIT_FOR_UPDATE:
			if(msg.getSeqNum() == expectedSequenceNumber) {
				super.clearSubscriber();
				setStage(Stage.RECEIVE_UPDATES);
				Log.i("IMSM", "Update received, stopping init msg listener");
			} else {
				Log.e("IMSM", "Invalid sequence number!");
				setStage(Stage.WAIT_FOR_INIT);
				clearUpdateSubscriber();
				break;
			}
		case RECEIVE_UPDATES:
			if(msg.getSeqNum() != expectedSequenceNumber) {
				Log.e("IMSM", "Invalid sequence number!");
				setStage(Stage.WAIT_FOR_INIT);
				clearUpdateSubscriber();
				super.initSubscriber(updateFullTopicName);
				break;
			}

			if(msg.getType() == InteractiveMarkerUpdate.UPDATE)
				callback.receiveUpdate(msg);
		}
	}

	@Override
	protected void onMessageReceived(InteractiveMarkerInit msg) {
		switch(stage) {
		case WAIT_FOR_UPDATE:
		case WAIT_FOR_INIT:
			if(updateSubscriber == null)
				initUpdateSubscriber();
			setStage(Stage.WAIT_FOR_UPDATE);
			expectedSequenceNumber = msg.getSeqNum();
			callback.receiveInit(msg);
			break;
		}
	}

	private void initUpdateSubscriber() {
		updateSubscriber = connectedNode.newSubscriber(GraphName.of(updateTopicName), visualization_msgs.InteractiveMarkerUpdate._TYPE);
		updateSubscriber.addMessageListener(updateListener, UPDATE_QUEUE_SIZE);
	}

	private void clearUpdateSubscriber() {
		if(updateSubscriber != null) {
			updateSubscriber.shutdown();
			updateSubscriber = null;
		}
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
	}

	private void setStage(Stage stage) {
		Log.d("IMSM", "Stage: " + stage.toString());
		this.stage = stage;
	}
}
