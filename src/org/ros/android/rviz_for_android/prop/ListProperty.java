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

import java.util.ArrayList;
import java.util.List;

import org.ros.android.rviz_for_android.R;

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
 * Display a spinner containing a list of options. The integer value refers to the index of the selected item.
 * 
 * @author azimmerman
 */
public class ListProperty extends Property<Integer> {

	private List<String> list = new ArrayList<String>();
	private Spinner spin;
	private ArrayAdapter<String> aa;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(aa != null) {
				aa.notifyDataSetChanged();
			}
		}
	};

	public ListProperty(String name, Integer value, PropertyUpdateListener<Integer> updateListener) {
		super(name, value, updateListener);
		if(getValue() < 0)
			setValue(0);
		super.addUpdateListener(new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				if(spin != null)
					spin.setSelection(newval);
			}
		});
	}

	@Override
	public View getUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_spinner, parent, false);

		tvTitle = (TextView) convertView.findViewById(R.id.tvProp_Spinner_Name);
		if(title != null)
			tvTitle.setText(title);
		else
			tvTitle.setText(super.name);

		if(aa == null) {
			aa = new ArrayAdapter<String>(parent.getContext(), android.R.layout.simple_spinner_item, this.list);
			aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

		spin = (Spinner) convertView.findViewById(R.id.spProp_Spinner);

		spin.setAdapter(aa);
		spin.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View v, int position, long id) {
				setValue(position);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spin.setSelection(getValue());
		spin.setEnabled(super.enabled);
		return convertView;
	}

	public ListProperty setList(List<String> list) {
		this.list.clear();
		for(String s : list)
			this.list.add(s);
		handler.sendEmptyMessage(0);
		return this;
	}

	public ListProperty setList(String[] list) {
		this.list.clear();
		for(String s : list)
			this.list.add(s);
		handler.sendEmptyMessage(0);
		return this;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if(spin != null)
			spin.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	@Override
	public void fromPreferences(String val) {
		if(val == null)
			return;
		int selected = 0;
		try {
			selected = Integer.parseInt(val);
		} catch(NumberFormatException e) {
			return;
		}
		
		// Must make sure that the loaded value is a valid option in the list
		if(selected < list.size()) {
			setValue(selected);
			spin.setSelection(selected);
		}
	}

	@Override
	public String toPreferences() {
		return null;
	}
}
