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

import org.ros.android.rviz_for_android.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * A property which displays no controls
 * @author azimmerman
 */
public class ViewProperty extends Property<Object> {

	private TextView tvTitle;
	private CheckBox cb;
	
	public ViewProperty(String name, Object value, PropertyUpdateListener<Object> updateListener) {
		super(name, value, updateListener);
	}

	@Override
	public View getUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_boolean, parent, false);
		tvTitle = (TextView) convertView.findViewById(R.id.tvProp_Boolean_Name);
		if(title != null)
			tvTitle.setText(title);
		else
			tvTitle.setText(super.name);
		cb = (CheckBox) convertView.findViewById(R.id.cbProp_Checkbox);
		cb.setFocusable(false);
		cb.setEnabled(false);
		cb.setVisibility(CheckBox.INVISIBLE);
		return convertView;
	}

	@Override
	public void fromPreferences(String val) {
		// Do nothing
	}

	@Override
	public String toPreferences() {
		return null;
	}

}
