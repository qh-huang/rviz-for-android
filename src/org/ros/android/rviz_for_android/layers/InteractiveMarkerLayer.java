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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.renderer.layer.Selectable;
import org.ros.android.renderer.layer.SelectableLayer;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;
import org.ros.android.rviz_for_android.drawable.InteractiveMarker;
import org.ros.android.rviz_for_android.drawable.InteractiveMarkerControl;
import org.ros.android.rviz_for_android.layers.InteractiveMarkerSubscriptionManager.InteractiveMarkerCallback;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.StringProperty;
import org.ros.message.Time;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Vector3;

import visualization_msgs.InteractiveMarkerFeedback;
import visualization_msgs.InteractiveMarkerInit;
import visualization_msgs.InteractiveMarkerPose;
import visualization_msgs.InteractiveMarkerUpdate;
import android.os.Handler;
import android.util.Log;

public class InteractiveMarkerLayer extends DefaultLayer implements LayerWithProperties, SelectableLayer {

	private static final String FEEDBACK_SUFFIX = "/feedback";

	// Listen for updates
	private InteractiveMarkerSubscriptionManager subscriber;

	// Publish feedback
	private Publisher<visualization_msgs.InteractiveMarkerFeedback> publisher;
	private ConnectedNode connectedNode;
	private int feedbackSeq = 0;
	private String cliendId;

	public interface MarkerFeedbackPublisher {
		public void publishFeedback(InteractiveMarker interactiveMarker, InteractiveMarkerControl control, byte type);
	}

	private MarkerFeedbackPublisher pubCallback = new MarkerFeedbackPublisher() {
		@Override
		public void publishFeedback(InteractiveMarker interactiveMarker, InteractiveMarkerControl control, byte type) {
			if(publisher != null) {
				InteractiveMarkerFeedback msg = publisher.newMessage();

				geometry_msgs.Pose markerPose = getPoseMessage(interactiveMarker.getPosition(), interactiveMarker.getOrientation());
				
				msg.setHeader(getHeaderMessage(interactiveMarker));
				msg.setClientId(cliendId);
				msg.setControlName(control.getName());
				msg.setEventType(type);
				msg.setMarkerName(interactiveMarker.getName());
				msg.setMenuEntryId(interactiveMarker.getMenuSelection());
				msg.setMousePoint(markerPose.getPosition());
				msg.setMousePointValid(true); // WHY NOT
				msg.setPose(markerPose);
				
				publisher.publish(msg);
			}
		}
	};
	
	private std_msgs.Header getHeaderMessage(InteractiveMarker marker) {
		std_msgs.Header msg = connectedNode.getTopicMessageFactory().newFromType(std_msgs.Header._TYPE);
		msg.setFrameId(marker.getFrame());
		msg.setSeq(feedbackSeq++);
		msg.setStamp(Time.fromMillis(System.currentTimeMillis()));
		return msg;
	}
	
	private geometry_msgs.Pose getPoseMessage(Vector3 v, Quaternion q) {
		geometry_msgs.Pose msg = connectedNode.getTopicMessageFactory().newFromType(geometry_msgs.Pose._TYPE);
		v.toPointMessage(msg.getPosition());
		q.toQuaternionMessage(msg.getOrientation());
		return msg;
	}

	// Markers
	private Map<String, InteractiveMarker> markers = new HashMap<String, InteractiveMarker>();
	private final Object lockObject = new Object();
	private FrameTransformTree ftt;

	// Layer properties
	private final BoolProperty prop = new BoolProperty("Enabled", true, null);
	private final StringProperty propTopic = new StringProperty("Topic", "/basic_controls", new PropertyUpdateListener<String>() {
		@Override
		public void onPropertyChanged(String newval) {
			subscriber.setTopic(newval);
			publisher.shutdown();
			publisher = connectedNode.newPublisher(newval + FEEDBACK_SUFFIX, visualization_msgs.InteractiveMarkerFeedback._TYPE);
		}
	});

	public InteractiveMarkerLayer(Camera cam) {
		super(cam);

		subscriber = new InteractiveMarkerSubscriptionManager("/basic_controls", cam, new InteractiveMarkerCallback() {
			@Override
			public void receiveUpdate(InteractiveMarkerUpdate msg) {

				for(String s : msg.getErases())
					markers.remove(s);

				for(visualization_msgs.InteractiveMarker im : msg.getMarkers())
					markers.put(im.getName(), new InteractiveMarker(im, camera, ftt, pubCallback));

				for(InteractiveMarkerPose p : msg.getPoses()) {
					InteractiveMarker stored = markers.get(p.getName());
					if(stored != null)
						stored.update(p);
					else {
						Log.e("InteractiveMarker", "Didn't have a marker with name " + p.getName());
					}
				}
			}

			@Override
			public void receiveInit(InteractiveMarkerInit msg) {
				synchronized(lockObject) {
					markers.clear();
					for(visualization_msgs.InteractiveMarker im : msg.getMarkers())
						markers.put(im.getName(), new InteractiveMarker(im, camera, ftt, pubCallback));
				}
			}

			@Override
			public void clear() {
				synchronized(lockObject) {
					markers.clear();
					camera.getSelectionManager().clearSelection();
				}
			}
		});
		prop.addSubProperty(propTopic);
	}

	@Override
	public void draw(GL10 glUnused) {
		synchronized(lockObject) {
			for(InteractiveMarker marker : markers.values())
				marker.draw(glUnused);
		}
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		ftt = frameTransformTree;
		subscriber.onStart(connectedNode, handler, frameTransformTree, camera);

		publisher = connectedNode.newPublisher(propTopic.getValue() + FEEDBACK_SUFFIX, visualization_msgs.InteractiveMarkerFeedback._TYPE);
		this.connectedNode = connectedNode;
		cliendId = connectedNode.getName().toString() + "/Interactive Markers";
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		super.onShutdown(view, node);
		subscriber.onShutdown(view, node);
		publisher.shutdown();
		
		for(InteractiveMarker im : markers.values())
			im.cleanup();
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}

	@Override
	public Property<?> getProperties() {
		return prop;
	}

	@Override
	public void selectionDraw(GL10 glUnused) {
		synchronized(lockObject) {
			for(InteractiveMarker marker : markers.values())
				marker.selectionDraw(glUnused);
		}
	}

	@Override
	public Set<Selectable> getSelectables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AvailableLayerType getType() {
		return AvailableLayerType.InteractiveMarker;
	}

}
