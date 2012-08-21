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

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A read only property used to display status messages to the user. Messages can optionally have an associated warning level (Ok, Warn, or Error) determined by setting
 * the text color to a certain {@link StatusColor}
 * 
 * @author azimmerman
 * 
 */
public class ReadOnlyProperty extends Property<String> {

	public static enum StatusColor {
		OK(R.drawable.status_ok, Color.GREEN), WARN(R.drawable.status_warn, Color.YELLOW), ERROR(R.drawable.status_err, Color.RED), INVISIBLE(R.drawable.transparent, Color.BLACK), NO_ICON(R.drawable.transparent, Color.WHITE);
		private int drawable;
		private int color;

		StatusColor(int drawable, int color) {
			this.drawable = drawable;
			this.color = color;
		}

		public int getColor() {
			return color;
		}

		public int getDrawable() {
			return drawable;
		}
	}

	private TextView display;
	private ImageView statusIcon;
	private StatusColor textColor = StatusColor.OK;

	public ReadOnlyProperty(String name, String value, PropertyUpdateListener<String> updateListener) {
		super(name, value, updateListener);
	}

	public void setTextColor(StatusColor textColor) {
		this.textColor = textColor;
		// Trigger a redraw by informing the update listeners of a change
		super.setValue(super.value);
	}

	@Override
	public View getUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		convertView = inflater.inflate(R.layout.row_property_readonly, parent, false);
		final View accessView = convertView;
		tvTitle = (TextView) convertView.findViewById(R.id.tvProp_ReadOnly_Name);
		if(title != null)
			tvTitle.setText(title);
		else
			tvTitle.setText(super.name);

		statusIcon = (ImageView) convertView.findViewById(R.id.imgStatus);
		statusIcon.setImageResource(textColor.getDrawable());

		display = (TextView) convertView.findViewById(R.id.tvProp_ReadOnly_Value);
		display.setText(super.value);
		display.setTextColor(textColor.getColor());

		// When the layer updates the read only property, update the view
		// This must be done by posting a runnable to the view, which is executed by the UI thread
		super.addUpdateListener(new PropertyUpdateListener<String>() {
			@Override
			public void onPropertyChanged(final String newval) {
				accessView.post(new Runnable() {
					@Override
					public void run() {
						display.setText(newval);
						display.setTextColor(textColor.getColor());
						statusIcon.setImageResource(textColor.getDrawable());
					}
				});
			}
		});
		return convertView;
	}

	@Override
	public void fromPreferences(String val) {
		// Do nothing, status messages shouldn't be loaded from saved preferences
	}

	@Override
	public String toPreferences() {
		return null;
	}	
}
