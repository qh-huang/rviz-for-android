package org.ros.android.renderer;

import org.ros.namespace.GraphName;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

public interface Camera {

	public abstract void apply();

	/**
	 * Moves the camera.
	 * 
	 * <p>
	 * The distances are given in viewport coordinates, not in world coordinates.
	 * 
	 * @param xDistance
	 *          distance in x to move
	 * @param yDistance
	 *          distance in y to move
	 */
	public abstract void moveCameraScreenCoordinates(float xDistance, float yDistance);

	public abstract void setCamera(Vector3 newCameraPoint);

	public abstract Vector3 getCamera();
	
	public abstract float[] getViewMatrix();

	public abstract void zoomCamera(float factor);

	public abstract GraphName getFixedFrame();

	public abstract void setFixedFrame(GraphName fixedFrame);

	public abstract void resetFixedFrame();

	public abstract void setTargetFrame(GraphName frame);

	public abstract void resetTargetFrame();

	public abstract GraphName getTargetFrame();

	public abstract Viewport getViewport();

	public abstract void setViewport(Viewport viewport);

	public abstract float getZoom();

	public abstract void setZoom(float zoom);
	
	public abstract float[] getModelMatrix();
	
	public abstract void setScreenDisplayOffset(int dx, int dy);
	
	public abstract int[] getScreenDisplayOffset();
	
	public abstract void pushM();
	
	public abstract void popM();
	
	public abstract void translateM(float x, float y, float z);
	
	public abstract void scaleM(float sx, float sy, float sz);
	
	public abstract void rotateM(float a, float x, float y, float z);
	
	public abstract void rotateM(Quaternion q);
	
	public abstract void loadIdentityM();
	
	public abstract void applyTransform(Transform t);
	
	public abstract void loadMatrixM(float[] matrix);
	
	public abstract SelectionManager getSelectionManager();
	
	public abstract void addFixedFrameListener(FixedFrameListener l);
	
	public abstract void removeFixedFrameListener(FixedFrameListener l);
	
	public AvailableFrameTracker getFrameTracker();
	
	public interface FixedFrameListener {
		public void fixedFrameChanged(GraphName newFrame);
	}
	
	public interface AvailableFixedFrameListener {
		public void newFixedFrameAvailable(String newFrame);
	}
	
	public abstract void setAvailableFixedFrameListener(AvailableFixedFrameListener l);
	public abstract void informNewFixedFrame(String f);
}