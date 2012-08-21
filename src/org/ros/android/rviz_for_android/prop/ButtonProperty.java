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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A string property which displays a button GUI. The property value is used as the button text. Clicking the button will call onPropertyChanged for
 * all listeners.
 * @author azimmerman
 */
public class ButtonProperty extends Property<String> {

	public ButtonProperty(String name, String value, PropertyUpdateListener<String> updateListener) {
		super(name, value, updateListener);
	}

	private Button btn;

	@Override
	public View getUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_button, parent, false);
		tvTitle = (TextView) convertView.findViewById(R.id.tvProp_Button_Name);
		if(title != null)
			tvTitle.setText(title);
		else
			tvTitle.setText(super.name);

		btn = (Button) convertView.findViewById(R.id.btProp_Button);

		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ButtonProperty.this.informListeners("");
			}
		});
		btn.setText(super.value);
		btn.setEnabled(super.enabled);
		return convertView;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if(btn != null)
			btn.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	@Override
	public void fromPreferences(String val) {
		// Don't trigger a button press by using super.value instead of setValue()
		super.value = val;
	}

	@Override
	public String toPreferences() {
		return value;
	}

}
