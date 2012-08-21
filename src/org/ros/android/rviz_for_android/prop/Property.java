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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ros.android.rviz_for_android.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A base class for layer properties
 * 
 * @author azimmerman
 * @param <T>
 *            The type of value stored by this property
 */
public abstract class Property<T> {

	public interface PropertyUpdateListener<T> {
		void onPropertyChanged(T newval);
	}

	protected T value;
	protected String name;
	protected String description = "Not implemented! ಠ_ಠ";
	protected boolean enabled;
	protected boolean visible;
	protected boolean indented;
	protected TextView tvTitle;
	protected LinkedList<PropertyUpdateListener<T>> updateListeners = new LinkedList<PropertyUpdateListener<T>>();

	private PropertyListAdapter propAdapter;
	private List<Property<?>> propList = new ArrayList<Property<?>>();
	private int propListIdx = 0;
	protected Map<String, Integer> subProps = new HashMap<String, Integer>();

	public Property(String name, T value, PropertyUpdateListener<T> updateListener) {
		this.name = name;
		this.value = value;
		this.enabled = true;
		this.visible = true;
		this.indented = false;
		addUpdateListener(updateListener);
	}

	/**
	 * Add the provided update listener
	 * 
	 * @param updateListener
	 */
	public void addUpdateListener(PropertyUpdateListener<T> updateListener) {
		if(updateListener != null)
			updateListeners.add(updateListener);
	}

	/**
	 * Set the value of the property
	 * 
	 * @param value
	 */
	public void setValue(T value) {
		if(this.value == null || !this.value.equals(value)) {
			this.value = value;
			informListeners(value);
		}
	}

	/**
	 * Inform all update listeners of a new value. This only informs all listeners, it does not change the property value.
	 * 
	 * @param newvalue
	 */
	protected void informListeners(T newvalue) {
		for(PropertyUpdateListener<T> pul : updateListeners) {
			if(pul != null)
				pul.onPropertyChanged(newvalue);
		}
	}

	/**
	 * Register a PropertyListAdapter with this property. The PLA is informed when a redraw is required
	 * 
	 * @param pla
	 */
	public void registerPropListAdapter(PropertyListAdapter pla) {
		this.propAdapter = pla;
	}

	/**
	 * Trigger the registered PropertyListAdapter to redraw the list of properties
	 */
	protected void redraw() {
		if(propAdapter != null)
			propAdapter.notifyDataSetChanged();
	}

	public View getPropertyUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title) {
		if(visible) {
			View retval = getUi(convertView, parent, inflater, title);
			if(indented && tvTitle != null)
				tvTitle.setText("    " + tvTitle.getText());
			return retval;
		} else {
			convertView = inflater.inflate(R.layout.row_property_hidden, parent, false);
			return convertView;
		}
	}

	public abstract View getUi(View convertView, ViewGroup parent, LayoutInflater inflater, String title);

	public T getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	/**
	 * Enable/disable the property. This has no impact unless the subclass of property chooses to use the enabled value.
	 * 
	 * @param isEnabled
	 */
	public void setEnabled(boolean isEnabled) {
		this.enabled = isEnabled;
	}

	/**
	 * Hide/show the current property. This has no effect if a PropertyListAdapter hasn't been registered.
	 * 
	 * @param isVisible
	 */
	public void setVisible(boolean isVisible) {
		if(isVisible != this.visible) {
			this.visible = isVisible;
			redraw();
		}
	}

	/**
	 * Enable/disable indenting the property title.
	 * 
	 * @param isIndented
	 */
	public void setIndented(boolean isIndented) {
		indented = isIndented;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Property<?> other = (Property<?>) obj;
		if(description == null) {
			if(other.description != null)
				return false;
		} else if(!description.equals(other.description))
			return false;
		if(name == null) {
			if(other.name != null)
				return false;
		} else if(!name.equals(other.name))
			return false;
		return true;
	}

	// Nested property access functions
	@SuppressWarnings({ "unchecked", "hiding" })
	public <R extends Property<?>> R getProperty(String... levels) {
		Property<?> cur = this;
		for(int i = 0; i < levels.length; i++) {
			cur = (Property<?>) propList.get(cur.subProps.get(levels[i]));
		}
		return (R) cur;
	}

	public void addSubProperty(Property<?> p, String... levels) {
		p.setIndented(true);
		Property<?> cur = this;
		for(int i = 0; i < levels.length; i++) {
			cur = (Property<?>) propList.get(cur.subProps.get(levels[i]));
		}
		cur.propList.add(cur.propListIdx, p);
		cur.subProps.put(p.getName(), cur.propListIdx++);
	}

	public List<Property<?>> getPropertyCollection() {
		return propList;
	}
	
	public abstract void fromPreferences(String val);
	
	public abstract String toPreferences();
}
