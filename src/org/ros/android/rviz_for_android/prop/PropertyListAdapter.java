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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

/**
 * List adapter which displays a set of properties in an expandable list
 * @author azimmerman
 */
public class PropertyListAdapter extends BaseExpandableListAdapter {
	
	private List<LayerWithProperties> layers;
	private List<ArrayList<Property<?>>> props = new ArrayList<ArrayList<Property<?>>>();
	private LayoutInflater inflater;
		
	public PropertyListAdapter(List<LayerWithProperties> layers, Context context) {
		super();
		inflater = LayoutInflater.from(context);
		this.layers = layers;
		generateContents();
	}
	
	private void generateContents() {
		props.clear();
		for(int i = 0; i < layers.size(); i++) {
			LayerWithProperties lwp = layers.get(i);
			props.add(i, new ArrayList<Property<?>>(lwp.getProperties().getPropertyCollection()));
		}
		for(ArrayList<Property<?>> alp : props)
			for(Property<?> p : alp)
				p.registerPropListAdapter(this);
	}

	public Object getChild(int groupPosition, int childPosition) {
		return props.get(groupPosition).get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		return props.get(groupPosition).get(childPosition).getPropertyUi(convertView, parent, inflater, null);
	}

	public int getChildrenCount(int groupPosition) {
		return props.get(groupPosition).size();
	}

	public Object getGroup(int groupPosition) {
		return layers.get(groupPosition);
	}

	@Override
	public void notifyDataSetChanged() {
		generateContents();
		super.notifyDataSetChanged();
	}

	public int getGroupCount() {
		return layers.size();
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		return layers.get(groupPosition).getProperties().getPropertyUi(convertView, parent, inflater, layers.get(groupPosition).getName());
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}
