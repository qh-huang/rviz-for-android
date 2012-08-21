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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Utility;
import org.ros.android.renderer.Vertices;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;
import org.ros.android.rviz_for_android.drawable.PCShaders.ColorMode;

import sensor_msgs.ChannelFloat32;
import android.opengl.GLES20;
import android.util.Log;

public class PointCloudGL extends BaseShape {
	// Point cloud data	
	private FloatBuffer selectedChannelBuffer;
	private static final float[][] DEFAULT_CHANNELS = new float[0][0];
	private float[][] channels = DEFAULT_CHANNELS;
	private float[] channelMin;
	private float[] channelMax;
	
	private float minRange = 0f;
	private float maxRange = 1f;
	private boolean autoRange = true;
	
	public void setAutoRanging(boolean ar) {
		autoRange = ar;
	}
	public void setManualRange(float min, float max) {
		if(min == max)
			throw new IllegalArgumentException("Min and max can't be equal!");
		minRange = min;
		maxRange = max;
	}
	
	private int channelSelected = 0;
	private List<String> channelNames = new ArrayList<String>();
	private FloatBuffer points;
	private int cloudSize;
	private volatile boolean drawCloud = false;
	private ColorMode mode = ColorMode.FLAT_COLOR;
	
	public PointCloudGL(Camera cam) {
		super(cam);
		super.setProgram(PCShaders.getProgram(mode));
	}
	
	@Override
	public void draw(GL10 glUnused) {
		if(drawCloud) {
			super.draw(glUnused);
			
			calcMVP();
			
			if(mode == ColorMode.CHANNEL) {
				GLES20.glEnableVertexAttribArray(ShaderVal.ATTRIB_COLOR.loc);
				GLES20.glVertexAttribPointer(ShaderVal.ATTRIB_COLOR.loc, 1, GLES20.GL_FLOAT, false, 0, selectedChannelBuffer);
				if(autoRange) {
					GLES20.glUniform1f(getUniform(ShaderVal.EXTRA), channelMin[channelSelected]);
					GLES20.glUniform1f(getUniform(ShaderVal.EXTRA_2), channelMax[channelSelected]);
				} else {
					GLES20.glUniform1f(getUniform(ShaderVal.EXTRA), minRange);
					GLES20.glUniform1f(getUniform(ShaderVal.EXTRA_2), maxRange);
				}
			} else {
				GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
				GLES20.glUniform1i(getUniform(ShaderVal.EXTRA), mode.extraInfo);
			}
			
			GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
			GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
			GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, points);
			GLES20.glDrawArrays(GLES20.GL_POINTS, 0, cloudSize);
		}
	}

	public void setData(float[] points, List<sensor_msgs.ChannelFloat32> channels) {
		drawCloud = false;
		if(points == null || points.length == 0) {
			return;
		}
		
		// Determine which channels of data are available
		if(channels != null && channels.size() > 0) {
			channelNames.clear();
			channelMin = new float[channels.size()];
			channelMax = new float[channels.size()];
			this.channels = new float[channels.size()][points.length/3];
			int idx = 0;
			for(ChannelFloat32 cf : channels) {
				channelNames.add(cf.getName());
				this.channels[idx] = cf.getValues();
				channelMin[idx] = Utility.arrayMin(this.channels[idx]);
				channelMax[idx] = Utility.arrayMax(this.channels[idx]);
				idx++;
			}
			selectedChannelBuffer = moveToBuffer(this.channels[channelSelected], selectedChannelBuffer);
		} else {
			// If no channels are available
			this.channels = DEFAULT_CHANNELS;
			mode = ColorMode.FLAT_COLOR;
			channelSelected = 0;
		}
		
		if(channelSelected > this.channels.length) {
			mode = ColorMode.FLAT_COLOR;
			channelSelected = 0;
		}
		
		this.points = moveToBuffer(points, this.points);
		cloudSize = points.length / 3;
		drawCloud = (cloudSize > 0);
	}
	
	private FloatBuffer moveToBuffer(float[] points, FloatBuffer buffer) {
		if(buffer == null || points.length > buffer.capacity()) {
			Log.i("PointCloud","Allocating a new buffer!");
			buffer = null;
			buffer = Vertices.toFloatBuffer(points);
		} else {
			buffer.position(0);
			buffer.put(points);
			buffer.position(0);
		}
		return buffer;
	}
	
	public void setChannelSelection(int selected) {
		channelSelected = selected;
		selectedChannelBuffer = Vertices.toFloatBuffer(channels[channelSelected]);
	}
	
	public void setColorMode(int selected) {
		this.mode = ColorMode.values()[selected];
		if(PCShaders.getProgram(mode) != null)
			super.setProgram(PCShaders.getProgram(mode));
		
		if(channelSelected > channels.length)
			channelSelected = 0;
		
		if(mode == ColorMode.CHANNEL && channels.length == 0) {
			this.mode = ColorMode.FLAT_COLOR;
			super.setProgram(PCShaders.getProgram(mode));
		}
	}
	
	public ColorMode getColorMode() {
		return mode;
	}
	
	public List<String> getChannelNames() {
		return channelNames;
	}
}
