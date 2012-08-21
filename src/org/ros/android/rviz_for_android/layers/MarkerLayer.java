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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;
import org.ros.android.rviz_for_android.drawable.Marker;
import org.ros.android.rviz_for_android.prop.ButtonProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty.StatusColor;
import org.ros.android.rviz_for_android.urdf.ServerConnection;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;

public class MarkerLayer extends EditableStatusSubscriberLayer<visualization_msgs.Marker> implements LayerWithProperties {

	// List of all received namespaces and all namespaces to draw
	private List<String> namespaceList = new LinkedList<String>();
	private List<String> enabledNamespaces = new LinkedList<String>();

	// Map from namespace to map from ID to marker
	private Map<String, HashMap<Integer, Marker>> markers = new HashMap<String, HashMap<Integer, Marker>>();

	private FrameTransformTree ftt;
	private long nextPruneTime;
	private static final long PRUNE_PERIOD = 300; // Milliseconds
	private Object lockObj = new Object();
	private final ServerConnection serverConnection;

	public MarkerLayer(Camera cam, GraphName topicName) {
		super(topicName, visualization_msgs.Marker._TYPE, cam);
		this.serverConnection = ServerConnection.getInstance();
		nextPruneTime = System.currentTimeMillis() + PRUNE_PERIOD;
		super.prop.addSubProperty(new ButtonProperty("Namespaces ", "Select", new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				showNamespaceSelectDialog();
			}
		}));
	}

	@Override
	protected void onMessageReceived(visualization_msgs.Marker msg) {
		super.onMessageReceived(msg);
		String ns = msg.getNs();
		int id = msg.getId();

		synchronized(lockObj) {
			switch(msg.getAction()) {
			case visualization_msgs.Marker.ADD:
				if(!markers.containsKey(ns)) {
					markers.put(ns, new HashMap<Integer, Marker>());
					enabledNamespaces.add(ns);
					namespaceList.add(ns);
				}
				markers.get(ns).put(id, new Marker(msg, super.camera, ftt));
				break;
			case visualization_msgs.Marker.DELETE:
				Log.i("MarkerLayer", "Deleting marker " + ns + ":" + id);
				if(markers.containsKey(ns))
					markers.get(ns).remove(id);
				break;
			default:
				Log.e("MarkerLayer", "Received a message with unknown action " + msg.getAction());
				return;
			}
		}
	}

	@Override
	public void draw(GL10 glUnused) {
		synchronized(lockObj) {
			for(String namespace : enabledNamespaces)
				for(Marker marker : markers.get(namespace).values())
					marker.draw(glUnused);
			
			if(System.currentTimeMillis() >= nextPruneTime)
				pruneMarkers();
		}
	}

	private void pruneMarkers() {
		boolean error = false;
		
		// Prune markers which have expired
		for(HashMap<Integer, Marker> hm : markers.values()) {
			List<Integer> removeIds = new LinkedList<Integer>();
			for(Integer i : hm.keySet()) {
				Marker marker = hm.get(i);
				if(marker.isExpired())
					removeIds.add(i);
				else if(marker.isError()) {
					super.statusController.setFrameChecking(false);
					super.statusController.setStatus("Marker " + marker.getId() + " in " + marker.getNamespace() + " is invalid", StatusColor.WARN);
					error = true;
				}
			}
			for(Integer i : removeIds) {
				hm.remove(i);
			}
		}

		if(!error && super.messageCount > 0)
			super.statusController.setFrameChecking(true);

		nextPruneTime += PRUNE_PERIOD;

	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		super.onStart(connectedNode, handler, frameTransformTree, camera);
		this.ftt = frameTransformTree;
	}

	@Override
	public Property<?> getProperties() {
		return super.prop;
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}

	@Override
	protected String getMessageFrameId(visualization_msgs.Marker msg) {
		return msg.getHeader().getFrameId();
	}

	protected void showNamespaceSelectDialog() {
		int count = namespaceList.size();
		boolean[] selected = new boolean[count];
		CharSequence[] namespacesArray = new CharSequence[count];

		for(int i = 0; i < count; i++) {
			selected[i] = enabledNamespaces.contains(namespaceList.get(i));
			namespacesArray[i] = namespaceList.get(i);
		}

		DialogInterface.OnMultiChoiceClickListener coloursDialogListener = new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				synchronized(lockObj) {
					if(isChecked)
						enabledNamespaces.add(namespaceList.get(which));
					else
						enabledNamespaces.remove(namespaceList.get(which));
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(serverConnection.getContext());
		builder.setTitle("Select Namespaces");
		builder.setMultiChoiceItems(namespacesArray, selected, coloursDialogListener);
		builder.setNeutralButton("Ok", null);

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public AvailableLayerType getType() {
		return AvailableLayerType.Marker;
	}
}
