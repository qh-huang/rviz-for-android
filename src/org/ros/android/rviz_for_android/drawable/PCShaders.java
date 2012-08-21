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

import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;

public class PCShaders {
	private static final String vChannelShader = "attribute vec2 aChannel;\n" + "attribute vec4 aPosition;\n" + "uniform mat4 uMvp;\n" + "uniform float minVal;\n" + "uniform float maxVal;\n" + "varying vec4 vColor;\n" + "void main() {\n" + "	gl_Position = uMvp * aPosition;\n" + "	float mixlevel = max(min((aChannel.x - minVal)/(maxVal-minVal),1.0),0.0);\n" + "	vColor = mix(vec4(0.0, 0.0, 0.0, 1.0), vec4(1.0,1.0,1.0,1.0), mixlevel);\n" + "	gl_PointSize = 3.0;\n" + "}";
	private static final ShaderVal[] channelParamTypes = new ShaderVal[] { ShaderVal.POSITION, ShaderVal.MVP_MATRIX, ShaderVal.ATTRIB_COLOR, ShaderVal.EXTRA, ShaderVal.EXTRA_2 };
	private static final String[] channelParamNames = new String[] { "aPosition", "uMvp", "aChannel", "minVal", "maxVal" };

	private static final String hToRGB = "vec4 hToRGB(float h) {\n" + "   float hs = 2.0*h;\n" + "	float hi = floor(hs);\n" + "   float f = (hs) - floor(hs);\n" + "	float q = 1.0 - f;\n" + "	if (hi <= 0.0)\n" + "return vec4(1.0, f, 0.0, 1.0);\n" + "	if (hi <= 1.0)\n" + "return vec4(q, 1.0, 0.0, 1.0);\n" + "	if (hi <= 2.0)\n" + "return vec4(0.0, 1.0, f, 1.0);\n" + "	if (hi <= 3.0)\n" + "return vec4(0.0, q, 1.0, 1.0);\n" + "	if (hi <= 4.0)\n" + "return vec4(f, 0.0, 1.0, 1.0);\n" + "	else\n" + "return vec4(1.0, 0.0, q, 1.0);\n" + "}\n";
	private static final String vFlatColorShader = "precision mediump float;\n" + "uniform mat4 uMvp;\n" + "uniform vec4 uColor;\n" + "attribute vec4 aPosition;\n" + "varying vec4 vColor;\n" + "void main() {\n" + "	gl_Position = uMvp * aPosition;\n" + "	vColor = uColor;\n" + "	gl_PointSize = 3.0;\n" + "}";
	private static final ShaderVal[] flatColorParamTypes = new ShaderVal[] { ShaderVal.POSITION, ShaderVal.UNIFORM_COLOR, ShaderVal.MVP_MATRIX };
	private static final String[] flatColorParamNames = new String[] { "aPosition", "uColor", "uMvp" };

	private static final String vGradientShader = "precision mediump float;\n" + "uniform mat4 uMvp;\n" + "uniform vec4 uColor;\n" + "uniform int uDirSelect;\n" + "attribute vec4 aPosition;\n" + "varying vec4 vColor;\n" + hToRGB + "void main() {\n" + "	gl_Position = uMvp * aPosition;\n" + "	vColor = hToRGB(mod(abs(aPosition[uDirSelect]),3.0));\n" + "	gl_PointSize = 3.0;\n" + "}";
	private static final ShaderVal[] gradientParamTypes = new ShaderVal[] { ShaderVal.POSITION, ShaderVal.UNIFORM_COLOR, ShaderVal.MVP_MATRIX, ShaderVal.EXTRA };
	private static final String[] gradientParamNames = new String[] { "aPosition", "uColor", "uMvp", "uDirSelect" };

	private static final String fShader = "precision mediump float;\n" + "varying vec4 vColor;\n" + "void main()\n" + "{\n" + "	gl_FragColor = vColor;\n" + "}";

	// This was originally done in an enum, but Dalvik seems to have some strange enum issues which caused exceptions
	private static final GLSLProgram flatInstance = new GLSLProgram(vFlatColorShader, fShader);
	private static final GLSLProgram gradientInstance = new GLSLProgram(vGradientShader, fShader);
	private static final GLSLProgram channelInstance = new GLSLProgram(vChannelShader, fShader);

	static {
		for(int i = 0; i < flatColorParamTypes.length; i++)
			flatInstance.setAttributeName(flatColorParamTypes[i], flatColorParamNames[i]);

		for(int i = 0; i < gradientParamTypes.length; i++)
			gradientInstance.setAttributeName(gradientParamTypes[i], gradientParamNames[i]);

		for(int i = 0; i < channelParamTypes.length; i++)
			channelInstance.setAttributeName(channelParamTypes[i], channelParamNames[i]);
	}

	public static final String[] shaderNames = new String[] { "Flat Color", "Gradient X", "Gradient Y", "Gradient Z", "Channel" };
	public static final int[] shaderPos = new int[] { 0, 1, 1, 1, 2 };
	public static final int[] extraInfo = new int[] { -1, 0, 1, 2, -1 };
	public static GLSLProgram[] programs = new GLSLProgram[] { flatInstance, gradientInstance, channelInstance };

	public static GLSLProgram getProgram(int p) {
		return programs[p];
	}

	public static int getExtraInfo(int p) {
		return extraInfo[p];
	}

	public static GLSLProgram getProgram(ColorMode cm) {
		return programs[cm.pos];
	}

	public static enum ColorMode {
		FLAT_COLOR("Flat Color", 0, -1), GRADIENT_X("Gradient X", 1, 0), GRADIENT_Y("Gradient Y", 1, 1), GRADIENT_Z("Gradient Z", 1, 2), CHANNEL("Channel", 2, -1);
		public String name;
		public int pos;
		public int extraInfo = -1;

		ColorMode(String name, int arrayPos, int extra) {
			this.pos = arrayPos;
			this.extraInfo = extra;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private PCShaders() {
	}
}
