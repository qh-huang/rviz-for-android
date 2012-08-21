package org.ros.android.rviz_for_android.drawable.loader;
import java.util.Arrays;

public class FloatVector {
	private float[] values;
	private int idx = 0;

	public FloatVector(float[] array) {
		values = array;
		idx = array.length;
	}

	public FloatVector(int capacity) {
		values = new float[capacity];
		idx = 0;
	}

	public void add(float f) {
		values[idx++] = f;
	}
	
	public void addAll(float[] f) {
		for(Float fl : f) {
			add(fl);
		}
	}
	
	public float get(int i) {
		return values[i];
	}

	public float remove() {
		return values[--idx];
	}

	public int getIdx() {
		return idx;
	}

	public float[] getArray() {
		return values;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + idx;
		result = prime * result + Arrays.hashCode(values);
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
		FloatVector other = (FloatVector) obj;
		if(idx != other.idx)
			return false;
		if(!Arrays.equals(values, other.values))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FloatVector [values=" + Arrays.toString(values) + "]";
	}
}