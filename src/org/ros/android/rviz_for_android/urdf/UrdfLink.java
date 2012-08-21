package org.ros.android.rviz_for_android.urdf;

import java.util.LinkedList;
import java.util.List;

import org.ros.namespace.GraphName;

public class UrdfLink {
	private Component visual;
	private Component collision;
	private GraphName name;
	private LinkedList<Component> componentList = new LinkedList<Component>();

	public UrdfLink(Component visual, Component collision, String name) {
		this.visual = visual;
		this.collision = collision;
		this.name = GraphName.of(name);
		
		if(visual != null)
			componentList.add(visual);
		if(collision != null)
			componentList.add(collision);
	}

	public Component getVisual() {
		return visual;
	}

	public Component getCollision() {
		return collision;
	}

	public GraphName getName() {
		return name;
	}
	
	public List<Component> getComponents() {
		return componentList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collision == null) ? 0 : collision.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((visual == null) ? 0 : visual.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		UrdfLink other = (UrdfLink) obj;
		if(collision == null) {
			if(other.collision != null)
				return false;
		} else if(!collision.equals(other.collision))
			return false;
		if(name == null) {
			if(other.name != null)
				return false;
		} else if(!name.equals(other.name))
			return false;
		if(visual == null) {
			if(other.visual != null)
				return false;
		} else if(!visual.equals(other.visual))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UrdfLink [visual=" + visual + ", collision=" + collision + ", name=" + name + "]";
	}
	
}
