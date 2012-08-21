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
package org.ros.android.rviz_for_android.prop;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ros.android.renderer.AvailableFrameTracker;
import org.ros.android.renderer.Camera;
import org.ros.android.rviz_for_android.R;
import org.ros.namespace.GraphName;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * A graph name selection property which displays a spinner of all available TF frames. The list of frames is automatically populated with all frames stored in the renderer's FrameTransformTree
 * 
 * @author azimmerman
 * 
 */
public class GraphNameProperty extends Property<GraphName> {

	private static final ArrayList<String> defaultFrameList = new ArrayList<String>(Arrays.asList(new String[] { "<Fixed Frame>" }));
	private Set<Integer> elementsToIgnore = new HashSet<Integer>();
	private List<String> defaultList;
	private int defaultListSize = 1;

	private List<String> framesToList = new ArrayList<String>();
	private List<String> spinnerFrameList;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(aa != null) {
				generateSpinnerContents();
				aa.notifyDataSetChanged();
			}
		}
	};

	private ArrayAdapter<String> aa;

	private int selection = 0;
	private Spinner spin;
	private final Camera cam;

	public GraphNameProperty(String name, GraphName value, Camera cam, PropertyUpdateListener<GraphName> updateListener) {
		super(name, value, updateListener);
		this.cam = cam;

		this.addUpdateListener(new PropertyUpdateListener<GraphName>() {
			public void onPropertyChanged(GraphName newval) {
				if(newval == null) {
					selection = 0;
				}
			}
		});
		setDefaultList(defaultFrameList, 0);
		spinnerFrameList = defaultList;

		cam.getFrameTracker().addListener(new AvailableFrameTracker.FrameAddedListener() {
			public void informFrameAdded(Set<String> newFrames) {
				handler.sendEmptyMessage(0);
			}
		});
	}

	private void generateSpinnerContents() {
		framesToList.clear();
		framesToList.addAll(0, defaultList);
		Set<String> framesFromFtt = cam.getFrameTracker().getAvailableFrames();
		synchronized(framesFromFtt) {
			for(String s : framesFromFtt) {
				if(!framesToList.contains(s))
					framesToList.add(s);
			}
		}
		spinnerFrameList = framesToList;
	}

	/**
	 * Provide a default list of options to show. All available GraphNames will be listed AFTER these default options.
	 * 
	 * @param def
	 *            the list of options to show
	 * @param toIgnore
	 *            optional, the indices of elements in the list to "ignore". When items at these indices are selected, the value will be set to null. Useful for making '<Fixed Frame>', an invalid GraphName, an option.
	 * @return self
	 */
	public GraphNameProperty setDefaultList(List<String> def, int... toIgnore) {
		defaultList = def;
		defaultListSize = def.size();
		elementsToIgnore.clear();
		for(int i : toIgnore) {
			if(i > defaultListSize - 1)
				throw new InvalidParameterException("Can not ignore a frame that's not in your default list!");
			elementsToIgnore.add(i);
		}
		return this;
	}

	/**
	 * Add an item to the default list
	 * 
	 * @param item
	 *            the item to add
	 * @return self
	 */
	public GraphNameProperty addToDefaultList(String item) {
		defaultList.add(item);
		setDefaultList(defaultList);
		generateSpinnerContents();
		handler.sendEmptyMessage(0);
		return this;
	}

	/**
	 * Set the item to be selected by default. Optionally ignore this item when it is selected. If ignored, the property value will be null when this item is selected
	 * 
	 * @param defaultItem
	 *            the item to set as default
	 * @param toIgnore
	 *            enable/disable ignoring this item
	 * @return self
	 */
	public GraphNameProperty setDefaultItem(String defaultItem, boolean toIgnore) {
		ArrayList<String> newDefault = new ArrayList<String>();
		newDefault.add(defaultItem);
		if(toIgnore)
			return this.setDefaultList(newDefault, 0);
		else
			return this.setDefaultList(newDefault);
	}

	@Override
	public View getUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_spinner, parent, false);

		tvTitle = (TextView) convertView.findViewById(R.id.tvProp_Spinner_Name);
		if(title != null)
			tvTitle.setText(title);
		else
			tvTitle.setText(super.name);

		generateSpinnerContents();
		if(aa == null) {
			aa = new ArrayAdapter<String>(parent.getContext(), android.R.layout.simple_spinner_item, spinnerFrameList);
			aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

		spin = (Spinner) convertView.findViewById(R.id.spProp_Spinner);

		spin.setAdapter(aa);
		spin.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View v, int position, long id) {
				selection = position;
				if(elementsToIgnore.contains(selection))
					setValue(null);
				else {
					setValue(GraphName.of(spinnerFrameList.get(selection)));
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spin.setSelection(selection);
		spin.setEnabled(super.enabled);
		return convertView;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if(spin != null)
			spin.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	@Override
	public void fromPreferences(String val) {
		// This doesn't effectively save/restore the selected graph name. Upon loading, the frame transform tree is empty and
		// a nonexistent transform can't be selected in a GraphName property.
		if(val == null)
			return;
		setValue(GraphName.of(val));
	}

	@Override
	public String toPreferences() {
		if(super.value == null)
			return null;
		return super.value.toString();
	}
}
