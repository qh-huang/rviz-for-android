package org.ros.android.rviz_for_android.geometry;

import android.util.FloatMath;

public class Vector2 {
	private final float x;
	private final float y;

	private static final Vector2 ZERO = new Vector2(0, 0);

	public static Vector2 zero() {
		return ZERO;
	}

	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2(float[] xy) {
		x = xy[0];
		y = xy[1];
	}

	public Vector2(int[] xy) {
		x = xy[0];
		y = xy[1];
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public Vector2 normalize() {
		float length = FloatMath.sqrt(x * x + y * y);
		return new Vector2(x / length, y / length);
	}

	public float dot(Vector2 other) {
		return x * other.getX() + y * other.getY();
	}

	public Vector2 add(Vector2 other) {
		return new Vector2(x + other.getX(), y + other.getY());
	}

	public Vector2 subtract(Vector2 other) {
		return new Vector2(x - other.getX(), y - other.getY());
	}

	public Vector2 negate() {
		return new Vector2(-x, -y);
	}

	public Vector2 scalarMultiply(float scalar) {
		return new Vector2(x * scalar, y * scalar);
	}

	public float length() {
		return FloatMath.sqrt(x * x + y * y);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}