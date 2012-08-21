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
package org.ros.android.rviz_for_android.drawable;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.renderer.shapes.TrianglesShape;

public class Arrow2D extends TrianglesShape {

	private static final float[] VERTICES = new float[] {
		-.15f, 0, 0,
		.15f, 0, 0,
		.15f, .66f, 0,
		
		.15f, .66f, 0,
		-.15f, .66f, 0,
		-.15f, 0, 0,
		
		-.5f, .66f, 0,
		.5f, .66f, 0,
		0,1,0
	};
	
	private static final float[] NORMALS =  new float[] {
		0,0,1,
		0,0,1,
		0,0,1,
		
		0,0,1,
		0,0,1,
		0,0,1,
		
		0,0,1,
		0,0,1,
		0,0,1
	};
	
	private static final Color DEFAULT_COLOR = new Color(0.25f, 0.75f, 0.33f, 1f);
	
	public Arrow2D(Camera cam) {
		super(cam, VERTICES, NORMALS, DEFAULT_COLOR);
	}
}
