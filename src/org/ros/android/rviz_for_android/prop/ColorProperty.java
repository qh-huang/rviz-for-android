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
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.R;
import org.ros.android.rviz_for_android.color.ColorPickerDialog;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A color property which displays a button GUI that opens a color selection dialog.
 * @author azimmerman
 */
public class ColorProperty extends Property<Color> {

	public ColorProperty(String name, Color value, PropertyUpdateListener<Color> updateListener) {
		super(name, value, updateListener);
	}

	private Button btn;

	@Override
	public View getUi(View convertView, final ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_button, parent, false);
		tvTitle = (TextView) convertView.findViewById(R.id.tvProp_Button_Name);
		if(title != null)
			tvTitle.setText(title);
		else
			tvTitle.setText(super.name);

		btn = (Button) convertView.findViewById(R.id.btProp_Button);
		btn.setText(" ");
		btn.setBackgroundColor(Utility.ColorToIntRGB(value));

		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
				final ColorPickerDialog d = new ColorPickerDialog(parent.getContext(), prefs.getInt("dialog", Utility.ColorToIntRGB(value)));
				d.setAlphaSliderVisible(true);
				d.setButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						setValue(Utility.IntToColor(d.getColor()));

						setButtonBackground();
					}
				});
				d.show();
			}
		});
		btn.setText("Pick Color");
		btn.setEnabled(super.enabled);
		setButtonBackground();
		return convertView;
	}

	private void setButtonBackground() {
		if(btn != null) {
			if(Utility.ColorToBrightness(value) > 0.5f)
				btn.setTextColor(android.graphics.Color.BLACK);
			else
				btn.setTextColor(android.graphics.Color.WHITE);
		}
	}

	@Override
	protected void informListeners(Color newvalue) {
		if(btn != null)
			btn.setBackgroundColor(Utility.ColorToIntRGB(newvalue));
		super.informListeners(newvalue);
	}

	@Override
	public void setEnabled(boolean enabled) {
		if(btn != null)
			btn.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	@Override
	public void fromPreferences(String val) {
		if(val == null)
			return;
		String[] parts = val.split(" ");
		if(parts.length != 4)
			return;
		
		float[] colorComponents = new float[4];
		for(int i = 0; i < 4; i ++) {
			try {
				colorComponents[i] = Float.parseFloat(parts[i]);
			} catch(NumberFormatException e) {
				return;
			}
		}
		setValue(new Color(colorComponents[0], colorComponents[1], colorComponents[2], colorComponents[3]));
	}

	@Override
	public String toPreferences() {
		if(value == null)
			return "0 0 0 0";
		return value.getRed() + " " + value.getGreen() + " " + value.getBlue() + " " + value.getAlpha();
	}
}
