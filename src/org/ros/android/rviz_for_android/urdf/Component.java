package org.ros.android.rviz_for_android.urdf;

import java.util.Arrays;

import org.ros.android.renderer.Utility;
import org.ros.android.renderer.shapes.Color;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

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

public class Component {
	public static enum GEOMETRY {
		CYLINDER, SPHERE, BOX, MESH
	};

	private GEOMETRY type;
	// Sphere & cylinder
	private float radius;
	private float length;
	// Box
	private float[] size;
	// Mesh
	private String mesh;

	// Origin
	private Transform origin;

	// Material
	private String material_name;
	private Color material_color;

	private long createTime;

	public GEOMETRY getType() {
		return type;
	}

	public float getRadius() {
		return radius;
	}

	public float getLength() {
		return length;
	}

	public float[] getSize() {
		return size;
	}

	public String getMesh() {
		return mesh;
	}

	public Transform getOrigin() {
		return origin;
	}

	public String getMaterial_name() {
		return material_name;
	}

	public Color getMaterial_color() {
		return material_color;
	}

	public void setMaterial_color(Color color) {
		material_color = color;
	}

	private Component() {
		createTime = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "Component [type=" + type + ", radius=" + radius + ", length=" + length + ", size=" + Arrays.toString(size) + ", mesh=" + mesh + ", scale=" + size[0] + ", origin=" + origin + ", material_name=" + material_name + ", material_color=" + material_color + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (createTime ^ (createTime >>> 32));
		result = prime * result + Float.floatToIntBits(length);
		result = prime * result + ((material_color == null) ? 0 : material_color.hashCode());
		result = prime * result + ((material_name == null) ? 0 : material_name.hashCode());
		result = prime * result + ((mesh == null) ? 0 : mesh.hashCode());
		result = prime * result + ((origin == null) ? 0 : origin.hashCode());
		result = prime * result + Float.floatToIntBits(radius);
		result = prime * result + Arrays.hashCode(size);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Component other = (Component) obj;
		if(createTime != other.createTime)
			return false;
		if(Float.floatToIntBits(length) != Float.floatToIntBits(other.length))
			return false;
		if(material_color == null) {
			if(other.material_color != null)
				return false;
		} else if(!material_color.equals(other.material_color))
			return false;
		if(material_name == null) {
			if(other.material_name != null)
				return false;
		} else if(!material_name.equals(other.material_name))
			return false;
		if(mesh == null) {
			if(other.mesh != null)
				return false;
		} else if(!mesh.equals(other.mesh))
			return false;
		if(origin == null) {
			if(other.origin != null)
				return false;
		} else if(!origin.equals(other.origin))
			return false;
		if(Float.floatToIntBits(radius) != Float.floatToIntBits(other.radius))
			return false;
		if(!Arrays.equals(size, other.size))
			return false;
		if(type != other.type)
			return false;
		return true;
	}

	public static class Builder {
		private GEOMETRY type;
		private float radius = -1;
		private float length = -1;
		private float[] size = new float[] { 1f, 1f, 1f };
		private String mesh;
		private Quaternion originOrientation = Quaternion.identity();
		private Vector3 originTranslation = Vector3.zero();
		private String material_name;
		private Color material_color;

		public Builder(GEOMETRY type) {
			this.type = type;
		}

		public Builder(String type) {
			this.type = GEOMETRY.valueOf(type.toUpperCase());
		}

		public void setRadius(float radius) {
			if(this.type == GEOMETRY.CYLINDER || this.type == GEOMETRY.SPHERE) {
				this.radius = radius;
			} else {
				throw new IllegalArgumentException("Can't set radius!");
			}
		}

		public void setLength(float length) {
			if(this.type == GEOMETRY.CYLINDER) {
				this.length = length;
			} else {
				throw new IllegalArgumentException("Can't set length!");
			}
		}

		public void setSize(float[] size) {
			if(this.type == GEOMETRY.BOX && size.length == 3) {
				this.size = size;
			} else {
				throw new IllegalArgumentException("Can't set size!");
			}
		}

		public void setMesh(String mesh) {
			if(this.type == GEOMETRY.MESH) {
				this.mesh = mesh;
			} else {
				throw new IllegalArgumentException("Can't set mesh!");
			}
		}

		public void setMeshScale(float[] scale) {
			if(this.type == GEOMETRY.MESH && scale.length == 3) {
				this.size = scale;
			} else {
				throw new IllegalArgumentException("Can't set mesh scale!");
			}
		}

		public void setOffset(float[] offset) {
			if(offset.length == 3) {
				this.originTranslation = new Vector3(offset[0], offset[1], offset[2]);
			} else {
				throw new IllegalArgumentException("Can't set offset!");
			}
		}

		public void setRotation(float[] rotation) {
			if(rotation.length == 3) {
				for(int i = 0; i < 3; i++)
					this.originOrientation = Utility.rpyToQuaternion(rotation[0], rotation[1], rotation[2]);
			} else {
				throw new IllegalArgumentException("Can't set rotation!");
			}
		}

		public void setMaterialName(String material_name) {
			this.material_name = material_name;
		}

		public void setMaterialColor(float[] color) {
			if(color.length == 4)
				this.material_color = new Color(color[0], color[1], color[2], color[3]);
			else
				throw new IllegalArgumentException("Can't set material color!");
		}

		public Component build() {
			Component retval = new Component();

			switch(type) {
			case MESH:
				if(mesh == null)
					throw new IllegalArgumentException("Never set a mesh name!");
				break;
			case BOX:
				if(size == null)
					throw new IllegalArgumentException("Never set a box size!");
				break;
			case CYLINDER:
				if(length < 0)
					throw new IllegalArgumentException("Never set a proper length!");
			case SPHERE:
				if(radius < 0)
					throw new IllegalArgumentException("Never set a proper radius!");
				break;
			}

			if(material_color != null && material_name == null)
				throw new IllegalArgumentException("Forgot to name the color " + material_color);

			if(material_color == null)
				material_color = new Color(1f, .5f, 0.15f, 1f);

			retval.type = type;
			retval.radius = radius;
			retval.length = length;
			retval.size = size;
			retval.mesh = mesh;
			retval.origin = new Transform(originTranslation, originOrientation);
			retval.material_name = material_name;
			retval.material_color = material_color;
			return retval;
		}

		public GEOMETRY getType() {
			return type;
		}
	}
}