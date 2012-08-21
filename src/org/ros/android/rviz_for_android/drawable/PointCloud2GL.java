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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Utility;
import org.ros.android.renderer.Vertices;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;

import sensor_msgs.PointField;
import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

public class PointCloud2GL extends BaseShape {

	private GLSLProgram program;

	private ByteBuffer data;

	private boolean drawCloud = false;

	private int xOffset = -1;
	private int yOffset = -1;
	private int zOffset = -1;

	private float minVal = 0f;
	private float maxVal = 1f;

	private int arrayOffset = 0;
	private int currentChannel = 0;
	private int drawOffset = 0;
	private volatile boolean flatColorMode = true;
	private int stride = 0;
	private int pointCount = 0;
	private List<String> channelNames = new ArrayList<String>();

	private Object dataSync = new Object();

	private static final List<PointField> DEFAULT_LIST = new ArrayList<PointField>();
	private List<PointField> fields = DEFAULT_LIST;

	public PointCloud2GL(Camera cam, Context context) {
		super(cam);
		String vsh = Utility.assetToString(context, "PointCloud2Shader.vsh");
		String fsh = Utility.assetToString(context, "PointCloud2Shader.fsh");

		program = new GLSLProgram(vsh, fsh);
		program.setAttributeName(ShaderVal.AX, "aX");
		program.setAttributeName(ShaderVal.AY, "aY");
		program.setAttributeName(ShaderVal.AZ, "aZ");
		program.setAttributeName(ShaderVal.A_EXTRA, "aChannel");

		program.setAttributeName(ShaderVal.MVP_MATRIX, "uMvp");
		program.setAttributeName(ShaderVal.UNIFORM_COLOR, "uColor");
		program.setAttributeName(ShaderVal.EXTRA, "uColorMode");
		program.setAttributeName(ShaderVal.EXTRA_2, "uMinVal");
		program.setAttributeName(ShaderVal.EXTRA_3, "uMaxVal");
		super.setProgram(program);
	}

	public void setFlatColorMode(Color color) {
		super.setColor(color);
		flatColorMode = true;
		Log.i("PointCloud2", "Color mode set to flat color: " + color);
	}

	public void setChannelColorMode(int channel) {
		if(channel >= 0 && channel < fields.size()) {
			Log.i("PointCloud2", "Color mode set to channel " + channel);
			currentChannel = channel;
			flatColorMode = false;
			drawOffset = arrayOffset + fields.get(channel).getOffset();
		} else {
			currentChannel = 0;
		}
	}

	public List<String> getChannelNames() {
		return channelNames;
	}

	public synchronized void setData(sensor_msgs.PointCloud2 msg) {
		drawCloud = false;
		channelNames.clear();
		this.fields = msg.getFields();
		this.stride = msg.getPointStep();
		pointCount = (msg.getData().capacity() / stride);

		Log.d("PointCloud", "Updated data with " + pointCount + " points. Data byte capacity is " + msg.getData().capacity() + " with offset " + msg.getData().arrayOffset());
		arrayOffset = msg.getData().arrayOffset();
		
		synchronized(dataSync) {
			if(data == null || data.capacity() < msg.getData().array().length) {
				data = null;
				data = Vertices.toByteBuffer(msg.getData().array());
			} else {
				data.position(0);
				data.put(msg.getData().array());
			}
		}

		for(PointField pf : fields) {
			channelNames.add(pf.getName());
			String name = pf.getName().toLowerCase();
			if(name.equals("x"))
				xOffset = arrayOffset + pf.getOffset();
			else if(name.equals("y"))
				yOffset = arrayOffset + pf.getOffset();
			else if(name.equals("z"))
				zOffset = arrayOffset + pf.getOffset();
		}
		
		if(currentChannel >= fields.size()) {
			currentChannel = 0;
		}

		data.position(0);
		drawCloud = (pointCount > 0);
	}

	/**
	 * Iterate through the data for the latest received message and determine the range of the data for the selected channel. This sets the current range to the computed range.
	 * 
	 * @return two element float array [min, max]
	 */
	public float[] computeRange() {
		if(fields == null || currentChannel < 0 || currentChannel >= fields.size())
			return new float[] { 0f, 1f };

		byte datatype = fields.get(currentChannel).getDatatype();
		int offset = fields.get(currentChannel).getOffset();

		double max = Float.NEGATIVE_INFINITY;
		double min = Float.POSITIVE_INFINITY;
		double val = 0;

		// Iterate through the data and find the range of the selected channel
		synchronized(dataSync) {
			for(int i = 0; i < pointCount; i++) {
				int readPos = arrayOffset + offset + (i * stride);

				switch(datatype) {
				case PointField.FLOAT32:
					val = data.getFloat(readPos);
					break;
				case PointField.FLOAT64:
					val = data.getDouble(readPos);
					break;
				case PointField.INT8:
					val = data.get(readPos);
					break;
				case PointField.INT16:
					val = data.getInt(readPos) & 0x00ffff;
					break;
				case PointField.INT32:
					val = data.getInt(readPos);
					break;
				case PointField.UINT8:
					val = data.get(readPos) & 0xff;
					break;
				case PointField.UINT16:
					val = data.getChar(readPos);
					break;
				case PointField.UINT32:
					val = data.getInt(readPos) & 0xffffffffL;
					break;
				default:
					val = 0;
				}

				max = Math.max(val, max);
				min = Math.min(val, min);
			}
		}

		minVal = (float) min;
		maxVal = (float) max;
		Log.d("PointCloud2", "Computed data range: " + minVal + " -> " + maxVal);
		return new float[] { (float) min, (float) max };
	}

	public void setRange(float min, float max) {
		minVal = min;
		maxVal = max;
	}

	@Override
	public void draw(GL10 glUnused) {
		if(drawCloud) {
			synchronized(dataSync) {
				super.draw(glUnused);

				calcMVP();
				GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);

				GLES20.glEnableVertexAttribArray(ShaderVal.AX.loc);
				data.position(xOffset);
				GLES20.glVertexAttribPointer(ShaderVal.AX.loc, 1, GLES20.GL_FLOAT, false, stride, data);

				GLES20.glEnableVertexAttribArray(ShaderVal.AY.loc);
				data.position(yOffset);
				GLES20.glVertexAttribPointer(ShaderVal.AY.loc, 1, GLES20.GL_FLOAT, false, stride, data);

				GLES20.glEnableVertexAttribArray(ShaderVal.AZ.loc);
				data.position(zOffset);
				GLES20.glVertexAttribPointer(ShaderVal.AZ.loc, 1, GLES20.GL_FLOAT, false, stride, data);

				if(flatColorMode) {
					GLES20.glUniform1i(getUniform(ShaderVal.EXTRA), 1);
					GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
				} else {
					GLES20.glUniform1i(getUniform(ShaderVal.EXTRA), 0);
					GLES20.glEnableVertexAttribArray(ShaderVal.A_EXTRA.loc);
					data.position(drawOffset);
					GLES20.glVertexAttribPointer(ShaderVal.A_EXTRA.loc, 1, GLES20.GL_FLOAT, false, stride, data);
					GLES20.glUniform1f(getUniform(ShaderVal.EXTRA_2), minVal);
					GLES20.glUniform1f(getUniform(ShaderVal.EXTRA_3), maxVal);
				}

				GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointCount);
			}
		}
	}
}
