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

/**
 * Numeric property which holds a float value
 * 
 * @author azimmerman
 */
public class FloatProperty extends Property<Float> {

	private float newFloat;
	private float min = Float.MIN_VALUE;
	private float max = Float.MAX_VALUE;

	private EditText et;

	public FloatProperty(String name, Float value, PropertyUpdateListener<Float> updateListener) {
		super(name, value, updateListener);
		newFloat = value;

		this.addUpdateListener(new PropertyUpdateListener<Float>() {
			public void onPropertyChanged(Float newval) {
				if(et != null)
					et.setText(Float.toString(newval));
			}
		});
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
		et.setSelectAllOnFocus(true);
		et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);

		et.setText(Float.toString(newFloat));
		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				try {
					newFloat = Float.parseFloat(et.getText().toString());
				} catch(NumberFormatException e) {
					newFloat = value;
				}
				if(keyCode == KeyEvent.KEYCODE_ENTER) {
					setValue(newFloat);
					imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
					return true;
				}
				return false;
			}
		});

		et.setEnabled(super.enabled);
		return convertView;
	}

	/**
	 * Set the valid range of input values. If an entered value is outside of this range, onPropertyChanged will not be called and the previous valid value will be used.
	 * 
	 * @param min
	 * @param max
	 * @return this
	 */
	public FloatProperty setValidRange(float min, float max) {
		this.min = min;
		this.max = max;
		return this;
	}

	@Override
	public void setValue(Float value) {
		if(Utility.inRange(value, min, max))
			super.setValue(value);
		else {
			super.setValue(super.value);
			if(et != null)
				et.setText(Float.toString(super.value));
		}
	}

	@Override
	protected void informListeners(Float newvalue) {
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
			setValue(Float.valueOf(val));
		} catch(NumberFormatException e) {
			return;
		}
	}

	@Override
	public String toPreferences() {
		return super.value.toString();
	}
}
