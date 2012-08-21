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

public class IntProperty extends Property<Integer> {

	private int newInt;
	private int min = Integer.MIN_VALUE;
	private int max = Integer.MAX_VALUE;

	private EditText et;

	public IntProperty(String name, int value, PropertyUpdateListener<Integer> updateListener) {
		super(name, value, updateListener);
		newInt = value;
	}

	@Override
	public View getUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_numericfield, parent, false);
		final InputMethodManager imm = (InputMethodManager) parent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

		tvTitle = (TextView) convertView.findViewById(R.id.tvProp_NumericField_Name);
		if(title != null)
			tvTitle.setText(title);
		else
			tvTitle.setText(super.name);

		// Show the numeric input field
		et = (EditText) convertView.findViewById(R.id.etProp_NumericField_DecimalValue);
		et.setVisibility(EditText.VISIBLE);
		et.setText(Integer.toString(newInt));
		et.setSelectAllOnFocus(true);
		et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

		et.setEnabled(super.enabled);

		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				try {
					newInt = Integer.parseInt(et.getText().toString());
				} catch(NumberFormatException e) {
					newInt = value;
				}
				if(keyCode == KeyEvent.KEYCODE_ENTER) {
					setValue(newInt);
					imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
					return true;
				}
				return false;
			}
		});

		this.addUpdateListener(new PropertyUpdateListener<Integer>() {
			public void onPropertyChanged(Integer newval) {
				et.setText(Integer.toString(newval));
			}
		});
		return convertView;
	}

	public IntProperty setValidRange(int min, int max) {
		this.min = min;
		this.max = max;
		return this;
	}

	@Override
	public void setValue(Integer value) {
		if(Utility.inRange(value, min, max))
			super.setValue(value);
		else
			super.setValue(super.value);
	}

	@Override
	protected void informListeners(Integer newvalue) {
		if(et != null)
			et.setText(newvalue.toString());
		super.informListeners(newvalue);
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
			super.value = Integer.valueOf(val);
		} catch(NumberFormatException e) {
			return;
		}
	}

	@Override
	public String toPreferences() {
		return super.value.toString();
	}
}
