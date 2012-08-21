/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.renderer.layer;

import java.util.Collection;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.RenderRequestListener;
import org.ros.android.renderer.VisualizationView;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;
import android.view.MotionEvent;

import com.google.common.collect.Lists;

/**
 * Base class for visualization layers.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public abstract class DefaultLayer implements Layer {

	private final Collection<RenderRequestListener> renderListeners;

	protected String layerName = "Unnamed Layer";
	protected Camera camera;

	public DefaultLayer(Camera cam) {
		renderListeners = Lists.newArrayList();
		this.camera = cam;
	}

	public void setName(String name) {
		layerName = name;
	}

	public String getName() {
		return layerName;
	}

	@Override
	public void draw(GL10 glUnused) {
	}

	@Override
	public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
		return false;
	}

	@Override
	public void onStart(ConnectedNode connectedNode, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
		this.camera = camera;
	}

	@Override
	public void onShutdown(VisualizationView view, Node node) {
	}

	@Override
	public void addRenderListener(RenderRequestListener listener) {
		renderListeners.add(listener);
	}

	@Override
	public void removeRenderListener(RenderRequestListener listener) {
	}

	protected void requestRender() {
		for(RenderRequestListener listener : renderListeners) {
			listener.onRenderRequest();
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
