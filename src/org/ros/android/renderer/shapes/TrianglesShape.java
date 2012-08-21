package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.opengl.GLES20;

public class TrianglesShape extends BaseShape {

	protected final FloatBuffer normals;
	protected final FloatBuffer vertices;
	protected final ShortBuffer indices;
	private boolean useIndices = false;
	protected int count;

	/**
	 * @param vertices
	 *            an array of vertices as defined by OpenGL's GL_TRIANGLES method
	 * @param color
	 *            the {@link Color} of the {@link Shape}
	 */
	public TrianglesShape(Camera cam, float[] vertices, float[] normals, Color color) {
		super(cam);
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);
		this.indices = null;
		useIndices = false;

		count = this.vertices.limit() / 3;

		init(color);
	}

	public TrianglesShape(Camera cam, float[] vertices, float[] normals, short[] indices, Color color) {
		super(cam);
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);
		this.indices = Vertices.toShortBuffer(indices);
		useIndices = true;

		count = this.indices.limit();

		init(color);
	}

	private void init(Color color) {
		setColor(color);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
		super.setProgram(GLSLProgram.FlatShaded());
	}

	@Override
	public void draw(GL10 glUnused) {
		super.draw(glUnused);

		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());

		calcMVP();
		calcNorm();
		GLES20.glUniformMatrix3fv(getUniform(ShaderVal.NORM_MATRIX), 1, false, NORM, 0);
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glUniform3f(getUniform(ShaderVal.LIGHTVEC), lightVector[0], lightVector[1], lightVector[2]);

		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glEnableVertexAttribArray(ShaderVal.NORMAL.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, vertices);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, normals);

		if(useIndices)
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, indices);
		else
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
	}

	@Override
	public void selectionDraw(GL10 glUnused) {
		super.selectionDraw(glUnused);
		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());

		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);

		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, vertices);

		if(useIndices)
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, indices);
		else
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);	
		super.selectionDrawCleanup();
	}
	
}
