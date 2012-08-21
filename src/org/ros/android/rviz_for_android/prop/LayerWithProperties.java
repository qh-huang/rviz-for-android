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

import org.ros.android.renderer.layer.Layer;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;

// This interface name probably violates some naming convention. Feel free to suggest alternate names! 
public interface LayerWithProperties extends Layer {
	public Property<?> getProperties();
	public String getName();
	public AvailableLayerType getType();
}
