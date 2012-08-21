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

package org.ros.android.rviz_for_android.layers;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.renderer.VisualizationView;
import org.ros.android.renderer.layer.DefaultLayer;
import org.ros.android.renderer.layer.TfLayer;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.rviz_for_android.MainActivity.AvailableLayerType;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;
import org.ros.android.rviz_for_android.prop.BoolProperty;
import org.ros.android.rviz_for_android.prop.ColorProperty;
import org.ros.android.rviz_for_android.prop.FloatProperty;
import org.ros.android.rviz_for_android.prop.FrameCheckStatusPropertyController;
import org.ros.android.rviz_for_android.prop.GraphNameProperty;
import org.ros.android.rviz_for_android.prop.IntProperty;
import org.ros.android.rviz_for_android.prop.LayerWithProperties;
import org.ros.android.rviz_for_android.prop.Property;
import org.ros.android.rviz_for_android.prop.Property.PropertyUpdateListener;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty;
import org.ros.android.rviz_for_android.prop.Vector3Property;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Vector3;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;

public class GridLayer extends DefaultLayer implements LayerWithProperties, TfLayer {

	private int nLines;
	private float vertices[];
	private short indices[];
	private FloatBuffer vbb;
	private ShortBuffer ibb;

	private BoolProperty prop;
	private boolean ready = false;

	private float xOffset = 0f;
	private float yOffset = 0f;
	private float zOffset = 0f;
	
	private GLSLProgram gridShader;
	private FrameCheckStatusPropertyController statusController;
	private GraphNameProperty propParent;

	public GridLayer(Camera cam, int cells, float spacing) {
		super(cam);

		prop = new BoolProperty("enabled", true, null);
		prop.addSubProperty(new ReadOnlyProperty("Status", "OK", null));
		propParent = new GraphNameProperty("Parent", null, cam, new PropertyUpdateListener<GraphName>() {
			@Override
			public void onPropertyChanged(GraphName newval) {
				if(statusController != null)
					statusController.setTargetFrame(newval);
			}
		});
		prop.addSubProperty(propParent);

		prop.addSubProperty(new IntProperty("Cells", cells, new PropertyUpdateListener<Integer>() {
			@Override
			public void onPropertyChanged(Integer newval) {
				onValueChanged();
			}
		}).setValidRange(1, 1000));
		prop.addSubProperty(new FloatProperty("Spacing", spacing, new PropertyUpdateListener<Float>() {
			@Override
			public void onPropertyChanged(Float newval) {
				onValueChanged();
			}
		}).setValidRange(0.01f, 10000f));
		prop.addSubProperty(new Vector3Property("Offset", new Vector3(0, 0, 0), new PropertyUpdateListener<Vector3>() {
			@Override
			public void onPropertyChanged(Vector3 newval) {
				xOffset = (float) newval.getX();
				yOffset = (float) newval.getY();
				zOffset = (float) newval.getZ();
			}

		}));
		prop.addSubProperty(new ColorProperty("Color", drawColor, new PropertyUpdateListener<Color>() {
			@Override
			public void onPropertyChanged(Color newval) {
				drawColor = newval;
			}
		}));
		
		initGrid();
		gridShader = GLSLProgram.FlatColor();
		uniformHandles = gridShader.getUniformHandles();
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, final FrameTransformTree frameTransformTree, final Camera camera) {
		statusController = new FrameCheckStatusPropertyController(prop.<ReadOnlyProperty>getProperty("Status"), camera, frameTransformTree);
	}

	private void onValueChanged() {
		initGrid();
		requestRender();
	}

	private void initGrid() {
		ready = false;
		int cells = prop.<IntProperty> getProperty("Cells").getValue();
		float spacing = prop.<FloatProperty> getProperty("Spacing").getValue();

		nLines = 2 * cells + 2 + (2 * ((cells + 1) % 2));
		vertices = new float[6 * nLines];
		indices = new short[2 * nLines];

		float max = (spacing * cells) / 2f;
		float min = -max;

		int idx = -1;

		for(float pos = min; pos <= 0; pos += spacing) {
			// Vertical lines
			vertices[++idx] = pos;
			vertices[++idx] = min;
			vertices[++idx] = 0;

			vertices[++idx] = pos;
			vertices[++idx] = max;
			vertices[++idx] = 0;

			vertices[++idx] = -pos;
			vertices[++idx] = min;
			vertices[++idx] = 0;

			vertices[++idx] = -pos;
			vertices[++idx] = max;
			vertices[++idx] = 0;

			// Horizontal lines
			vertices[++idx] = min;
			vertices[++idx] = pos;
			vertices[++idx] = 0;

			vertices[++idx] = max;
			vertices[++idx] = pos;
			vertices[++idx] = 0;

			vertices[++idx] = min;
			vertices[++idx] = -pos;
			vertices[++idx] = 0;

			vertices[++idx] = max;
			vertices[++idx] = -pos;
			vertices[++idx] = 0;
		}

		for(int i = 0; i < 2* nLines; i++) {
			indices[i] = (short) i;
		}

		// Pack the vertices into a byte array
		vbb = Vertices.toFloatBuffer(vertices);
		ibb = Vertices.toShortBuffer(indices);

		ready = true;
		requestRender();
	}

	Color drawColor = new Color(1f, 1f, 1f, 1f);
	
	private int[] uniformHandles;
	private float[] MVP = new float[16];
	
	private void calcMVP() {
		Matrix.multiplyMM(MVP, 0, camera.getViewMatrix(), 0, camera.getModelMatrix(), 0);
		Matrix.multiplyMM(MVP, 0, camera.getViewport().getProjectionMatrix(), 0, MVP, 0);
	}
	
	@Override
	public void draw(GL10 glUnused) {
		if(!gridShader.isCompiled()) {
			gridShader.compile(glUnused);
			uniformHandles = gridShader.getUniformHandles();
		}
			
		if(prop.getValue() && ready) {
			camera.pushM();
			camera.translateM(xOffset, yOffset, zOffset);
			calcMVP();
			gridShader.use(glUnused);
			
			GLES20.glUniform4f(uniformHandles[ShaderVal.UNIFORM_COLOR.loc], drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), drawColor.getAlpha());
			
			GLES20.glUniformMatrix4fv(uniformHandles[ShaderVal.MVP_MATRIX.loc], 1, false, MVP,0);
			
			GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
			GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, vbb);
			
			GLES20.glDrawElements(GLES20.GL_LINES, 2*nLines, GLES20.GL_UNSIGNED_SHORT, ibb);
			camera.popM();
		}
	}

	public Property<?> getProperties() {
		return prop;
	}

	public GraphName getFrame() {
		return propParent.getValue();
	}

	@Override
	public boolean isEnabled() {
		return prop.getValue();
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
		statusController.cleanup();
		super.onShutdown(view, node);
	}

	@Override
	public AvailableLayerType getType() {
		return AvailableLayerType.Grid;
	}

}
