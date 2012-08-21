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
package org.ros.android.renderer.layer;

import org.ros.android.rviz_for_android.drawable.InteractiveMarkerControl.InteractionMode;
import org.ros.android.rviz_for_android.geometry.Vector2;

public interface InteractiveObject {
			
	public void rotate(float dTheta);
	
	public void translate(float X, float Y);
	
	public Vector2 getScreenMotionVector();

	public void mouseDown();

	public void mouseUp();
	
	public int[] getPosition();

	public InteractionMode getInteractionMode();
}
