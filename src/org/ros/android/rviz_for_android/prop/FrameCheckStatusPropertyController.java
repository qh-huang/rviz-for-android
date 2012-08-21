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
package org.ros.android.rviz_for_android.prop;

import java.util.Set;

import org.ros.android.renderer.AvailableFrameTracker.FrameAddedListener;
import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Camera.FixedFrameListener;
import org.ros.android.renderer.shapes.Cleanable;
import org.ros.android.rviz_for_android.prop.ReadOnlyProperty.StatusColor;
import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.FrameTransformTree;

import android.util.Log;

/**
 * An extension of {@link StatusPropertyController} which automatically updates the status if a transform from the fixed frame to the selected target frame isn't available.
 * 
 * @author azimmerman
 */
public class FrameCheckStatusPropertyController extends StatusPropertyController implements Cleanable {

	private GraphName targetFrame = null;

	private FrameAddedListener fttListener;
	private FixedFrameListener camListener;

	private boolean useFrameCheck = true;
	private boolean transformExists = false;
	private final FrameTransformTree ftt;
	private final Camera cam;

	public FrameCheckStatusPropertyController(ReadOnlyProperty prop, Camera cam, FrameTransformTree ftt) {
		super(prop);
		this.cam = cam;
		this.ftt = ftt;

		camListener = new FixedFrameListener() {
			@Override
			public void fixedFrameChanged(GraphName newFrame) {
				checkFrameExists();
			}
		};
		cam.addFixedFrameListener(camListener);

		fttListener = new FrameAddedListener() {
			@Override
			public void informFrameAdded(Set<String> newFrames) {
				// There's no need to check if a transform exists if neither frame changed and it already exists
				if(!transformExists)
					checkFrameExists();
			}
		};

		cam.getFrameTracker().addListener(fttListener);
	}

	/**
	 * Set the current target frame to check against
	 * @param newFrame
	 */
	public void setTargetFrame(GraphName newFrame) {
		// Only check for existence if the target frame changed
		if(targetFrame == null || !newFrame.equals(targetFrame)) {
			targetFrame = newFrame;
			checkFrameExists();
		}
	}

	/**
	 * Enable/disable checking the existence of a transform between fixed and target frames
	 * @param frameCheck
	 */
	public void setFrameChecking(boolean frameCheck) {
		// Only run a frame check on the "rising edge" of useFrameCheck
		if(frameCheck && !useFrameCheck) {
			useFrameCheck = true;
			checkFrameExists();
		}
		useFrameCheck = frameCheck;
	}

	public boolean getFrameChecking() {
		return useFrameCheck;
	}

	protected void checkFrameExists() {
		if(useFrameCheck) {
			Log.i("SPC", "Checking transform existence: " + cam.getFixedFrame() + " -> " + targetFrame);
			if(targetFrame == null || (ftt.transform(cam.getFixedFrame(), targetFrame) != null)) {
				Log.i("SPC", "Transform exists");
				transformExists = true;
				super.setOk();
			} else {
				transformExists = false;
				Log.i("SPC", "Transform doesn't exist");
				super.setStatus("No transform from " + cam.getFixedFrame() + " to " + targetFrame, StatusColor.WARN);
			}
		}
	}

	@Override
	public void cleanup() {
		cam.removeFixedFrameListener(camListener);
		cam.getFrameTracker().removeListener(fttListener);
	}

}
