package com.nerd3c.renderer.shapes;

/**
 * Implemented by objects which require cleanup when they're no longer needed
 * @author azimmerman
 *
 */
public interface Cleanable {
	public void cleanup();
}
