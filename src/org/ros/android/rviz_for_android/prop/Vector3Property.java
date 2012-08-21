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

import org.ros.android.renderer.Utility;
import org.ros.android.rviz_for_android.R;
import org.ros.rosjava_geometry.Vector3;

import android.content.Context;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A Vector3 property using a TextView GUI which parses three numeric values for a Vector3 object from comma or space separated strings
 * 
 * @author azimmerman
 * 
 */
public class Vector3Property extends Property<Vector3> {

	private Vector3 newVector;
	private EditText et;

	public Vector3Property(String name, Vector3 value, PropertyUpdateListener<Vector3> updateListener) {
		super(name, value, updateListener);
		newVector = value;
	}

	@Override
	public View getUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_textfield, parent, false);
		final InputMethodManager imm = (InputMethodManager) parent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		tvTitle = (TextView) convertView.findViewById(R.id.tvProp_TextField_Name);
		if(title != null)
			tvTitle.setText(title);
		else
			tvTitle.setText(super.name);
		et = (EditText) convertView.findViewById(R.id.etProp_TextField_Value);
		et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_TEXT);
		et.setText(newVector.getX() + ", " + newVector.getY() + ", " + newVector.getZ());
		et.setSelectAllOnFocus(true);
		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_ENTER) {
					newVector = Utility.newVector3FromString(et.getText().toString());
					if(newVector != null)
						setValue(newVector);
					else
						newVector = value;
					et.setText(newVector.getX() + ", " + newVector.getY() + ", " + newVector.getZ());
					imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
					return true;
				}
				return false;
			}
		});
		et.setEnabled(super.enabled);
		return convertView;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if(et != null)
			et.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	@Override
	public void fromPreferences(String val) {
		try {
			setValue(Utility.newVector3FromString(val));
		} catch(NumberFormatException e) {
			return;
		}
	}

	@Override
	public String toPreferences() {
		return newVector.getX() + " " + newVector.getY() + " " + newVector.getZ();
	}
}
