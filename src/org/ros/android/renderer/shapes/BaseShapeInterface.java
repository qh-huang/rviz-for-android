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
package org.ros.android.renderer.shapes;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.layer.InteractiveObject;
import org.ros.android.renderer.layer.Selectable;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.rosjava_geometry.Transform;

public interface BaseShapeInterface extends Selectable {

	public void setProgram(GLSLProgram shader);

	public void draw(GL10 glUnused);

	public Color getColor();

	public void setColor(Color color);

	public Transform getTransform();

	public void setTransform(Transform pose);

	public void setSelected(boolean isSelected);
	
	public void registerSelectable();
	
	public void removeSelectable();
	
	public void setInteractiveObject(InteractiveObject io);

}