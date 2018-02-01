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
package com.nerd3c.renderer.layer;

import com.nerd3c.rviz_for_android.drawable.InteractiveMarkerControl.InteractionMode;
import com.nerd3c.rviz_for_android.geometry.Vector2;

public interface InteractiveObject {
			
	void rotate(float dTheta);
	
	void translate(float X, float Y);
	
	Vector2 getScreenMotionVector();

	void mouseDown();

	void mouseUp();
	
	int[] getPosition();

	InteractionMode getInteractionMode();

	void translateStart();
}
