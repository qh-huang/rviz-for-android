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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.util.Log;

public class GLSLProgram {

	private String vertexProgram;
	private String fragmentProgram;
	private int programID = 0;
	private int fShaderHandle = 0;
	private int vShaderHandle = 0;
	private boolean compiled = false;

	public static enum ShaderVal {
		// Attributes - location refers to OpenGL index
		POSITION(false, 0), ATTRIB_COLOR(false, 1), TEXCOORD(false, 2), NORMAL(false, 3), AX(false, 4), AY(false, 5), AZ(false, 6), A_EXTRA(false,7),
		// Uniforms - location refers to uniform int array
		MVP_MATRIX(true, 0), TIME(true, 1), UNIFORM_COLOR(true, 3), MV_MATRIX(true, 4), LIGHTPOS(true, 5), M_MATRIX(true, 6), LIGHTVEC(true, 7), TEXTURE(true, 8), EXTRA(true, 9), EXTRA_2(true, 10), EXTRA_3(true, 11), NORM_MATRIX(true, 12);

		private boolean isUniform = false;
		public int loc = -1;

		ShaderVal(boolean isUniform) {
			this.isUniform = isUniform;
		}

		ShaderVal(boolean isUniform, int attribLocation) {
			this.isUniform = isUniform;
			this.loc = attribLocation;
		}
	};

	// Create an array to store handles to uniforms. The array must be the right size to hold all uniforms described in ShaderVal
	private static int maxUniformLocation = 0;
	static {
		for(ShaderVal s : ShaderVal.values())
			if(s.isUniform)
				maxUniformLocation = Math.max(s.loc, maxUniformLocation);
	}
	private int[] uniformHandles = new int[maxUniformLocation + 1];
	private Map<ShaderVal, String> shaderValNames = new EnumMap<ShaderVal, String>(ShaderVal.class);
	// Static factory methods. These create/return singleton instances
	private static final GLSLProgram FlatColorInstance = MakeFlatColor();
	private static final GLSLProgram FlatShadedInstance = MakeFlatShaded();
	private static final GLSLProgram ColoredVertexInstance = MakeColoredVertex();
	private static final GLSLProgram TexturedShadedInstance = MakeTexturedShaded();

	public static GLSLProgram FlatColor() {
		return FlatColorInstance;
	}

	public static GLSLProgram FlatShaded() {
		return FlatShadedInstance;
	}

	public static GLSLProgram ColoredVertex() {
		return ColoredVertexInstance;
	}

	public static GLSLProgram TexturedShaded() {
		return TexturedShadedInstance;
	}

	private static GLSLProgram MakeFlatColor() {
		String vertexShader = "uniform mat4 u_MVPMatrix;\n" + "uniform vec4 u_Color;\n" + "attribute vec4 a_Position;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "   v_Color = u_Color;\n" + "   gl_PointSize = 3.0;\n" + "   gl_Position = u_MVPMatrix * a_Position;\n" + "}\n";
		String fragmentShader = "precision mediump float;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "   gl_FragColor = v_Color;\n" + "}";

		GLSLProgram retval = new GLSLProgram(vertexShader, fragmentShader);
		retval.setAttributeName(ShaderVal.POSITION, "a_Position");
		retval.setAttributeName(ShaderVal.UNIFORM_COLOR, "u_Color");
		retval.setAttributeName(ShaderVal.MVP_MATRIX, "u_MVPMatrix");
		return retval;
	}

	private static GLSLProgram MakeFlatShaded() {
//		String vertexShader = "uniform mat4 u_MVPMatrix;\n uniform vec4 u_Color;\n uniform vec3 u_lightVector;\n uniform mat4 u_MMatrix;\n uniform mat3 u_NormMatrix;\n attribute vec4 a_Position;\n" + "attribute vec3 a_Normal;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "   vec3 modelViewNormal = vec3(u_MMatrix * vec4(a_Normal,0.0));\n" + "   float diffuse = max(dot(modelViewNormal, u_lightVector), 0.4);\n" + "   v_Color = vec4(diffuse*u_Color.xyz, u_Color[3]);\n" + "   gl_PointSize = 3.0;\n" + "   gl_Position = u_MVPMatrix * a_Position;\n" + "}";
		String vertexShader = "uniform mat4 u_MVPMatrix;\n uniform vec4 u_Color;\n uniform vec3 u_lightVector;\n uniform mat3 u_NormMatrix;\n attribute vec4 a_Position;\n" + "attribute vec3 a_Normal;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "   vec3 modelViewNormal = normalize(u_NormMatrix * a_Normal);\n" + "   float diffuse = max(dot(modelViewNormal, u_lightVector), 0.4);\n" + "   v_Color = vec4(diffuse*u_Color.xyz, u_Color[3]);\n" + "   gl_PointSize = 3.0;\n" + "   gl_Position = u_MVPMatrix * a_Position;\n" + "}";
		
		String fragmentShader = "precision mediump float;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "   gl_FragColor = v_Color;\n" + "}";

		GLSLProgram retval = new GLSLProgram(vertexShader, fragmentShader);
		// Attributes
		retval.setAttributeName(ShaderVal.POSITION, "a_Position");
		retval.setAttributeName(ShaderVal.NORMAL, "a_Normal");
		// Uniforms
		retval.setAttributeName(ShaderVal.UNIFORM_COLOR, "u_Color");
		retval.setAttributeName(ShaderVal.MVP_MATRIX, "u_MVPMatrix");
		retval.setAttributeName(ShaderVal.LIGHTVEC, "u_lightVector");
		retval.setAttributeName(ShaderVal.NORM_MATRIX, "u_NormMatrix");
		return retval;
	}

	private static GLSLProgram MakeColoredVertex() {
		String vertexShader = "uniform mat4 u_MVPMatrix;\n" + "attribute vec4 a_Position;\n" + "attribute vec4 a_Color;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "   v_Color = a_Color;\n" + "   gl_PointSize = 3.0;\n" + "   gl_Position = u_MVPMatrix * a_Position;\n" + "}";
		String fragmentShader = "precision mediump float;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "   gl_FragColor = v_Color;\n" + "}";

		GLSLProgram retval = new GLSLProgram(vertexShader, fragmentShader);
		retval.setAttributeName(ShaderVal.POSITION, "a_Position");
		retval.setAttributeName(ShaderVal.ATTRIB_COLOR, "a_Color");
		retval.setAttributeName(ShaderVal.MVP_MATRIX, "u_MVPMatrix");
		return retval;
	}

	private static GLSLProgram MakeTexturedShaded() {
		String vertexShader = "attribute vec2 a_texCoord;\n" + "attribute vec4 a_Position;\n" + "attribute vec3 a_Normal;\n" + "uniform vec4 u_Color;\n" + "uniform mat4 u_MVPMatrix;\n" + "uniform mat3 u_NormMatrix;\n" + "uniform vec3 u_lightVector;\n" + "varying vec2 v_texCoord;\n" + "varying float v_diffuse;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "		v_texCoord = a_texCoord;\n" + "		v_Color = u_Color;\n" + "		vec3 modelViewNormal = normalize(u_NormMatrix * a_Normal);\n" + "		v_diffuse = min(max(dot(modelViewNormal, u_lightVector), 0.45),1.0);\n" + "		gl_Position = u_MVPMatrix * a_Position;\n" + "}";
		String fragmentShader = "precision mediump float;\n" + "uniform sampler2D u_texture;\n" + "varying vec2 v_texCoord;\n" + "varying float v_diffuse;\n" + "varying vec4 v_Color;\n" + "void main()\n" + "{\n" + "		vec4 color = texture2D(u_texture, v_texCoord);\n" + "		gl_FragColor = v_Color*vec4(v_diffuse*color.xyz, color[3]);\n" + "}";
		GLSLProgram retval = new GLSLProgram(vertexShader, fragmentShader);
		// Attributes
		retval.setAttributeName(ShaderVal.POSITION, "a_Position");
		retval.setAttributeName(ShaderVal.TEXCOORD, "a_texCoord");
		retval.setAttributeName(ShaderVal.NORMAL, "a_Normal");
		// Uniform
		retval.setAttributeName(ShaderVal.MVP_MATRIX, "u_MVPMatrix");
		retval.setAttributeName(ShaderVal.NORM_MATRIX, "u_NormMatrix");
		retval.setAttributeName(ShaderVal.LIGHTVEC, "u_lightVector");
		retval.setAttributeName(ShaderVal.TEXTURE, "u_texture");
		retval.setAttributeName(ShaderVal.UNIFORM_COLOR, "u_Color");
		return retval;
	}

	public GLSLProgram(String vertex, String fragment) {
		if(vertex == null || fragment == null)
			throw new IllegalArgumentException("Vertex/fragment shader program cannot be null!");

		this.vertexProgram = vertex;
		this.fragmentProgram = fragment;
		Arrays.fill(uniformHandles, -1);
	}

	public boolean compile(GL10 glUnused) {
		programID = GLES20.glCreateProgram();

		// Check that attributes are in place
		if(shaderValNames.isEmpty())
			throw new IllegalArgumentException("Must program shader value names");

		// Load and compile
		vShaderHandle = loadShader(glUnused, vertexProgram, GLES20.GL_VERTEX_SHADER);
		fShaderHandle = loadShader(glUnused, fragmentProgram, GLES20.GL_FRAGMENT_SHADER);

		if(vShaderHandle == 0 || fShaderHandle == 0) {
			Log.e("GLSL", "Unable to compile shaders!");
			return false;
		}

		GLES20.glAttachShader(programID, vShaderHandle);
		GLES20.glAttachShader(programID, fShaderHandle);

		// Bind all attributes. This gives each attribute the same handle in all shaders
		for(ShaderVal s : shaderValNames.keySet()) {
			if(!s.isUniform) {
				GLES20.glBindAttribLocation(programID, s.loc, shaderValNames.get(s));
				Log.i("GLSL", "Bound attribute " + shaderValNames.get(s) + " to index " + s.loc);
			}
		}

		// Link program
		int[] linkStatus = new int[1];
		GLES20.glLinkProgram(programID);
		GLES20.glGetProgramiv(programID, GLES20.GL_LINK_STATUS, linkStatus, 0);

		if(linkStatus[0] != GLES20.GL_TRUE) {
			Log.e("GLSL", "Unable to link program:");
			Log.e("GLSL", GLES20.glGetProgramInfoLog(programID));
			cleanup(glUnused);
			return false;
		} else {
			Log.d("GLSL", "Linking ok!");
		}

		// Fetch all attribute and shader locations
		for(ShaderVal s : shaderValNames.keySet()) {
			if(s.isUniform) {
				uniformHandles[s.loc] = GLES20.glGetUniformLocation(programID, shaderValNames.get(s));
				Log.i("GLSL", "Fetched uniform " + shaderValNames.get(s) + " = " + uniformHandles[s.loc]);
			}
		}

		Log.d("GLSL", "Shader ID " + programID + " compiled successfully!");

		compiled = true;
		return true;
	}

	public boolean isCompiled() {
		return compiled;
	}

	public void use(GL10 glUnused) {
		GLES20.glUseProgram(programID);
	}

	public void setAttributeName(ShaderVal val, String name) {
		shaderValNames.put(val, name);
	}

	public int[] getUniformHandles() {
		return uniformHandles;
	}

	/* load a Vertex or Fragment shader */
	private int loadShader(GL10 glUnused, String source, int shaderType) {
		int shader = GLES20.glCreateShader(shaderType);
		if(shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if(compiled[0] == 0) {
				Log.e("GLSL", "Could not compile shader " + shaderType + ":");
				Log.e("GLSL", GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
				throw new RuntimeException("Unable to compile shader!");
			}
		}
		Log.i("GLSL", "shader compiled: " + shader);
		return shader;
	}

	public void cleanup(GL10 glUnused) {
		if(programID > 0)
			GLES20.glDeleteProgram(programID);
		if(vShaderHandle > 0)
			GLES20.glDeleteShader(vShaderHandle);
		if(fShaderHandle > 0)
			GLES20.glDeleteShader(fShaderHandle);

		fShaderHandle = 0;
		vShaderHandle = 0;
		programID = 0;
	}
}
