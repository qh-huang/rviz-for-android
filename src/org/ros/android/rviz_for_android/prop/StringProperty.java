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

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A string property which displays a TextView GUI
 * 
 * @author azimmerman
 * 
 */
public class StringProperty extends Property<String> {
	private String newText;

	private EditText et;

	public interface StringPropertyValidator {
		/**
		 * Given a string, determine if the string is a valid value for the given property. If so, return true. Otherwise, return false.
		 * 
		 * @param newval
		 *            the potential new value for the string property
		 * @return accept/reject the new value
		 */
		public boolean isAcceptable(String newval);
	}

	// The default validator accepts any strings
	private static final StringPropertyValidator DEFAULT_VALIDATOR = new StringPropertyValidator() {
		@Override
		public boolean isAcceptable(String newval) {
			return true;
		}
	};

	private StringPropertyValidator validator;

	/**
	 * Provide the property with a new string validator to use. The old validator will be discarded.
	 * 
	 * @param validator
	 *            the new validator to use. If null, the default validator which accepts all strings will be used instead.
	 */
	public void setValidator(StringPropertyValidator validator) {
		if(validator == null)
			this.validator = DEFAULT_VALIDATOR;
		else
			this.validator = validator;
	}

	public StringProperty(String name, String value, PropertyUpdateListener<String> updateListener) {
		super(name, value, updateListener);
		newText = value;
		validator = DEFAULT_VALIDATOR;
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
		et.setText(newText);
		et.setSelectAllOnFocus(true);
		et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				newText = et.getText().toString();
				if(keyCode == KeyEvent.KEYCODE_ENTER && validator.isAcceptable(newText)) {
					setValue(newText);
					imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
					return true;
				} else if(!validator.isAcceptable(newText)) {
					et.setText(getValue());
					imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
					return true;
				}
				return false;
			}
		});
		et.setEnabled(super.enabled);
		super.addUpdateListener(new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(String newval) {
				et.setText(newval);
			}
		});
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
		setValue(val);
		newText = value;
	}

	@Override
	public String toPreferences() {
		return value;
	}
}
