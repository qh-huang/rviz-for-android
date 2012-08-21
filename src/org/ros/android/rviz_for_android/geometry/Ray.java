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
package org.ros.android.rviz_for_android.geometry;

import org.ros.rosjava_geometry.Vector3;

public class Ray {
	private Vector3 start;
	private Vector3 direction;

	public Ray(Vector3 start, Vector3 direction) {
		super();
		this.start = start;
		this.direction = direction;
	}
	
	public static Ray constructRay(Vector3 start, Vector3 end) {
		return new Ray(start, end.subtract(start));
	}

	public Vector3 getStart() {
		return start;
	}

	public Vector3 getDirection() {
		return direction;
	}

	public Vector3 getPoint(double length) {
		return start.add(new Vector3(direction.getX()*length, direction.getY()*length, direction.getZ()*length));
	}
	
	/**
	 * Finds the closest point on this ray to another ray. Assumes that this is the target ray
	 * 
	 * @param other
	 * @return
	 */
	public Vector3 getClosestPoint(Ray other) {
		Vector3 v13 = this.getStart().subtract(other.getStart());
		Vector3 v43 = other.getDirection();
		Vector3 v21 = this.getDirection();

		double d1343 = v13.dotProduct(v43);
		double d4321 = v43.dotProduct(v21);
		double d1321 = v13.dotProduct(v21);
		double d4343 = v43.dotProduct(v43);
		double d2121 = v21.dotProduct(v21);

		double denom = d2121 * d4343 - d4321 * d4321;

		if(Math.abs(denom) < 1e-6)
			return null;

		double numer = d1343 * d4321 - d1321 * d4343;
		double mua = numer / denom;
		
		return getPoint(mua);
	}

	@Override
	public String toString() {
		return "Ray [start=" + start + ", direction=" + direction + "]";
	}
}
