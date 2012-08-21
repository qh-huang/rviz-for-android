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

import java.util.ArrayList;
import java.util.Set;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.renderer.AngleControlView;
import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Translation2DControlView;
import org.ros.android.renderer.TranslationControlView;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.renderer.layer.Layer;
import org.ros.android.rviz_for_android.layers.AxisLayer;
import org.ros.android.rviz_for_android.layers.GridLayer;
import org.ros.android.rviz_for_android.layers.InteractiveMarkerLayer;
import org.ros.android.rviz_for_android.layers.MapLayer;
import org.ros.android.rviz_for_android.layers.MarkerLayer;
import org.ros.android.rviz_for_android.layers.ParentableOrbitCameraControlLayer;
import org.ros.android.rviz_for_android.layers.PointCloud2Layer;
import org.ros.android.rviz_for_android.layers.PointCloudLayer;
import org.ros.android.rviz_for_android.layers.RobotModelLayer;
import org.ros.android.rviz_for_android.layers.TfFrameLayer;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.PropertyListAdapter;
import org.ros.android.rviz_for_android.urdf.ServerConnection;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends RosActivity {
	private VisualizationView visualizationView;
	private static Context context;

	// GUI elements
	private ExpandableListView elv;
	private ArrayList<LayerWithProperties> layers = new ArrayList<LayerWithProperties>();
	private PropertyListAdapter propAdapter;
	private Toast msgToast;

	// Tracking layers
	public static enum AvailableLayerType {
		Axis("Axis"), 
		Grid("Grid"), 
		RobotModel("Robot Model"), 
		Map("Map"), 
		PointCloud("Point Cloud"), 
		PointCloud2("Point Cloud2"), 
		TFLayer("TF"), 
		Marker("Marker"), 
		InteractiveMarker("Interactive Marker");

		private String printName;
		private int count = 0;

		AvailableLayerType(String printName) {
			this.printName = printName;
		}

		@Override
		public String toString() {
			return printName;
		}

		public int getCount() {
			return count++;
		}
	};

	private static final AvailableLayerType[] availableLayers = AvailableLayerType.values();
	private static CharSequence[] availableLayerNames;
	static {
		availableLayerNames = new CharSequence[availableLayers.length];
		int i = 0;
		for(AvailableLayerType aln : availableLayers) {
			availableLayerNames[i++] = aln.toString();
		}
	}
	private CharSequence[] liveLayers;

	// Adding and removing layers
	private static AlertDialog.Builder addLayerDialogBuilder;
	private static AlertDialog.Builder remLayerDialogBuilder;
	private static AlertDialog addLayerDialog;
	private static AlertDialog remLayerDialog;
	private Button addLayer;
	private Button remLayer;
	private Button nameLayer;

	// Show and hide the layer selection panel
	private LinearLayout ll;
	private boolean showLayers = false;

	// Enable/disable following
	boolean following = false;
	ParentableOrbitCameraControlLayer camControl;

	// Interactive marker controls
	private InteractiveControlManager icm;

	public MainActivity() {
		super("Rviz", "Rviz");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_menu, menu);

		// Configure the action bar
		ActionBar ab = getActionBar();
		ab.setDisplayShowHomeEnabled(false);
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowCustomEnabled(true);
		menu.setGroupEnabled(R.id.unfollowGroup, following);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_layertoggle:
			if(showLayers) {
				ll.setVisibility(LinearLayout.GONE);
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(ll.getWindowToken(), 0);
			} else {
				ll.setVisibility(LinearLayout.VISIBLE);
			}
			showLayers = !showLayers;
			break;
		case R.id.menu_follow:
			showTFSelectDialog();
			break;
		case R.id.menu_unfollow:
			camControl.setTargetFrame(null);
			item.setEnabled(false);
			following = false;
			break;
		case R.id.clear_model_cache:
			int clearedCount = ServerConnection.getInstance().clearCache();
			showToast("Cleared " + clearedCount + " items in model cache");
			propAdapter.notifyDataSetChanged();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showToast(String msg) {
		msgToast.setText(msg);
		msgToast.show();
	}

	private void showTFSelectDialog() {
		Set<String> frameset = visualizationView.getCamera().getFrameTracker().getAvailableFrames();
		final String[] tfFrames = (String[]) frameset.toArray(new String[frameset.size()]);

		if(tfFrames.length > 0) {
			AlertDialog.Builder selTfFrame = new AlertDialog.Builder(this);
			selTfFrame.setTitle("Select a frame");
			selTfFrame.setItems(tfFrames, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					camControl.setTargetFrame(tfFrames[item]);
					following = true;
					invalidateOptionsMenu();
				}
			});
			AlertDialog dialog = selTfFrame.create();
			dialog.show();
		} else {
			showToast("No TF frames to follow!");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.e("MainActivity", "OnCreate called");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		MainActivity.context = this;
		msgToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

		createLayerDialogs();
		configureGUI();

		ll = ((LinearLayout) findViewById(R.id.layer_layout));
		ll.setVisibility(LinearLayout.GONE);

		visualizationView = (VisualizationView) findViewById(R.id.visualization);

		camControl = new ParentableOrbitCameraControlLayer(this, visualizationView.getCamera());
		camControl.setName("Camera");
		layers.add(camControl);

		for(Layer l : layers)
			visualizationView.addLayer(l);

		visualizationView.setPreserveEGLContextOnPause(true);

		elv = (ExpandableListView) findViewById(R.id.expandableListView1);
		propAdapter = new PropertyListAdapter(layers, getApplicationContext());
		elv.setAdapter(propAdapter);
		elv.setItemsCanFocus(true);
		elv.setOnGroupExpandListener(new OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
				int len = propAdapter.getGroupCount();
				for(int i = 0; i < len; i++) {
					if(i != groupPosition) {
						elv.collapseGroup(i);
					}
				}
			}
		});

		icm = new InteractiveControlManager((AngleControlView) findViewById(R.id.acAngleControl), (TranslationControlView) findViewById(R.id.tcTranslationControl), (Translation2DControlView) findViewById(R.id.tcTranslationControl2D));
		visualizationView.getCamera().getSelectionManager().setInteractiveControlManager(icm);
	}

	@Override
	protected void onDestroy() {
		Log.e("MainActivity", "OnDestroy has been called.");
		saveLayers();
		super.onDestroy();
		// TODO: This is a total hack to fix a strange bug related to the Android application lifecycle conflicting with OpenGL ES 2!
		// If the application isn't shut down completely, the renderer will not restart properly
		System.exit(0);
	}

	@Override
	protected void onPause() {
		Log.e("MainActivity", "OnPause called.");
		visualizationView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.e("MainActivity", "OnResume called.");
		visualizationView.onResume();
		super.onResume();
	}

	public static Context getAppContext() {
		return MainActivity.context;
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		ServerConnection.initialize("http://" + getMasterUri().getHost().toString() + ":44644", this);
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());
		nodeMainExecutor.execute(visualizationView, nodeConfiguration.setNodeName("android/rviz"));

		// Once the server connection and node executor are initialized, it's safe to load previously saved layers
		// This must be done on the UI thread
		runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				loadLayers();
			}
		});
	}

	private DefaultLayer addNewLayer(AvailableLayerType layertype) {
		Camera cam = visualizationView.getCamera();
		if(visualizationView.getCamera() == null)
			throw new IllegalArgumentException("Can not instantiate new layer, camera is null!");

		DefaultLayer newLayer = null;
		switch(layertype) {
		case Axis:
			newLayer = new AxisLayer(cam);
			break;
		case Grid:
			newLayer = new GridLayer(cam, 10, 1f);
			break;
		case RobotModel:
			newLayer = new RobotModelLayer(cam);
			break;
		case Map:
			newLayer = new MapLayer(cam, GraphName.of("/map"), this);
			break;
		case PointCloud:
			newLayer = new PointCloudLayer(cam, GraphName.of("/lots_of_points"));
			break;
		case PointCloud2:
			newLayer = new PointCloud2Layer(GraphName.of("/lots_of_points2"), cam, this);
			break;
		case TFLayer:
			newLayer = new TfFrameLayer(cam);
			break;
		case Marker:
			newLayer = new MarkerLayer(cam, GraphName.of("/markers"));
			break;
		case InteractiveMarker:
			newLayer = new InteractiveMarkerLayer(cam);
			break;
		}

		if(newLayer != null) {
			newLayer.setName(layertype + " " + layertype.getCount());
			if(newLayer instanceof LayerWithProperties) {
				layers.add((LayerWithProperties) newLayer);
				propAdapter.notifyDataSetChanged();
			}
			visualizationView.addLayer(newLayer);
		} else {
			showToast("Invalid selection!");
		}
		
		return newLayer;
	}

	private void removeLayer(int item) {
		Layer toRemove = layers.get(item + 1);

		if(toRemove != null) {
			visualizationView.removeLayer(toRemove);
			layers.remove(toRemove);
			propAdapter.notifyDataSetChanged();
		} else {
			showToast("Unable to remove selected layer " + liveLayers[item]);
		}
	}

	private CharSequence[] listLiveLayers() {
		liveLayers = new CharSequence[layers.size() - 1];
		for(int i = 1; i < layers.size(); i++) {
			liveLayers[i - 1] = layers.get(i).getName();
		}
		return liveLayers;
	}

	private void createLayerDialogs() {
		// Build a layer selection dialog for adding layers
		addLayerDialogBuilder = new AlertDialog.Builder(context);
		addLayerDialogBuilder.setTitle("Select a Layer");
		addLayerDialogBuilder.setItems(availableLayerNames, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				addNewLayer(availableLayers[item]);
			}
		});
		addLayerDialog = addLayerDialogBuilder.create();

		// Build a layer selection dialog for removing layers
		remLayerDialogBuilder = new AlertDialog.Builder(context);
		remLayerDialogBuilder.setTitle("Select a Layer");
	}

	private void configureGUI() {
		addLayer = (Button) findViewById(R.id.add_layer);
		remLayer = (Button) findViewById(R.id.remove_layer);
		nameLayer = (Button) findViewById(R.id.rename_layer);
		addLayer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				addLayerDialog.show();
			}
		});
		remLayer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(layers.size() > 0) {
					remLayerDialogBuilder.setItems(listLiveLayers(), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							removeLayer(item);
						}
					});
					remLayerDialog = remLayerDialogBuilder.create();
					remLayerDialog.show();
				} else {
					showToast("No layers to delete!");
				}
			}
		});
		nameLayer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				remLayerDialogBuilder.setItems(listLiveLayers(), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						renameLayer(item);
					}
				});
				remLayerDialog = remLayerDialogBuilder.create();
				remLayerDialog.show();
			}
		});
	}

	private void renameLayer(int item) {
		final int selectedItem = item + 1;
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Rename Layer");
		alert.setMessage("New layer name");

		final EditText input = new EditText(this);
		input.setText(liveLayers[item]);
		input.setSelectAllOnFocus(true);
		input.setSingleLine(true);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String newName = input.getText().toString();
				((DefaultLayer) layers.get(selectedItem)).setName(newName);
				propAdapter.notifyDataSetChanged();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		alert.show();
	}
	
	private void loadLayers() {
		SharedPreferences prefs = this.getPreferences(Activity.MODE_PRIVATE);
		
		// Fetch the number of saved layers
		int layerCount = prefs.getInt("LAYER_COUNT", 0);
		
		// For each layer, determine it's layer type
		for(int i = 0; i < layerCount; i++) {
			String layerType = prefs.getString("TYPE_"+i, null);
			if(layerType == null)
				continue;
			DefaultLayer newLayer = addNewLayer(AvailableLayerType.valueOf(layerType));
			newLayer.setName(prefs.getString("NAME_"+i, newLayer.getName()));
			
			// Restore layer properties
			if(newLayer instanceof LayerWithProperties) {
				LayerWithProperties newLayerProp = (LayerWithProperties) newLayer;
				Property<?> prop = newLayerProp.getProperties();
				prop.fromPreferences(prefs.getString(i+"_"+prop.getName(), prop.toPreferences()));
				
				for(Property<?> p : prop.getPropertyCollection())
					p.fromPreferences(prefs.getString(i+"_"+p.getName(), p.toPreferences()));
			}
		}
	}
	
	private void saveLayers() {
		SharedPreferences prefs = this.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		
		// Store the layer count (excluding the camera controller)
		editor.putInt("LAYER_COUNT", layers.size() - 1);
		
		int idx = 0;
		for(LayerWithProperties layer : layers) {
			// Ignore the camera controller or any layer that doesn't report a type
			if(layer == camControl || layer.getType() == null)
				continue;
			editor.putString("TYPE_"+idx, layer.getType().name());
			editor.putString("NAME_"+idx, layer.getName());
			
			// Save layer properties
			Property<?> prop = layer.getProperties();
			editor.putString(idx + "_" + prop.getName(), prop.toPreferences());
			for(Property<?> p : prop.getPropertyCollection()) {
				editor.putString(idx + "_" + p.getName(), p.toPreferences());
			}
			
			idx++;
		}
		
		editor.commit();
	}
}
