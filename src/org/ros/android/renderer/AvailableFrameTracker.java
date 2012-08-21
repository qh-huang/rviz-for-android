package org.ros.android.renderer;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class AvailableFrameTracker {
	public interface FrameAddedListener {
		public void informFrameAdded(Set<String> newFrames);
	}

	private SortedSet<String> availableFrames = new TreeSet<String>();
	private Set<FrameAddedListener> listeners = new HashSet<FrameAddedListener>();

	public void receivedMessage(geometry_msgs.TransformStamped transformStamped) {
		receivedFrame(transformStamped.getChildFrameId());
		receivedFrame(transformStamped.getHeader().getFrameId());
	}
	
	public void receivedFrame(String frame) {
		addFrame(frame);
	}
	
	private void addFrame(String frame) {
		synchronized(availableFrames) {
			if(!availableFrames.contains(frame)) {
				availableFrames.add(frame);
				notifyListeners();
			}
		}
	}

	private void notifyListeners() {
		for(FrameAddedListener l : listeners)
			l.informFrameAdded(availableFrames);
	}
	
	public void addListener(FrameAddedListener l) {
		listeners.add(l);
	}
	
	public void removeListener(FrameAddedListener l) {
		listeners.remove(l);
	}
	
	
	public Set<String> getAvailableFrames() {
		return availableFrames;
	}
	
}