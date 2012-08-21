package org.ros.android.renderer.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;

import android.opengl.GLES20;

public class TriangleStripShape extends BaseShape {
	private final FloatBuffer normals;
	private final FloatBuffer vertices;
	private final ShortBuffer indices;

	/**
	 * @param vertices
	 *            an array of vertices as defined by OpenGL's GL_TRIANGLE_FAN method
	 * @param color
	 *            the {@link Color} of the {@link Shape}
	 */
	public TriangleStripShape(Camera cam, float[] vertices, short[] indices, float[] normals, Color color) {
		super(cam);
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);

		ByteBuffer bb_idx = ByteBuffer.allocateDirect(indices.length * 2);
		bb_idx.order(ByteOrder.nativeOrder());
		this.indices = bb_idx.asShortBuffer();
		this.indices.put(indices);
		this.indices.position(0);

		setColor(color);
		super.setProgram(GLSLProgram.FlatShaded());
	}
	
	public TriangleStripShape(Camera cam, float[] vertices, float[] normals, Color color) {
		super(cam);
		this.vertices = Vertices.toFloatBuffer(vertices);
		this.normals = Vertices.toFloatBuffer(normals);
		
		short[] indicesArray = new short[vertices.length/3];
		for(int i = 0; i < indicesArray.length; i++)
			indicesArray[i] = (short) i;
		this.indices = Vertices.toShortBuffer(indicesArray);
		
		setColor(color);
		super.setProgram(GLSLProgram.FlatShaded());
	}

	@Override
	public void draw(GL10 glUnused) {
		cam.pushM();
		super.draw(glUnused);
		
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, vertices);
		
		GLES20.glEnableVertexAttribArray(ShaderVal.NORMAL.loc);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, 0, normals);
		
		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		
		calcMVP();
		calcNorm();
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glUniformMatrix3fv(getUniform(ShaderVal.NORM_MATRIX), 1, false, NORM, 0);
		GLES20.glUniform3f(getUniform(ShaderVal.LIGHTVEC), lightVector[0], lightVector[1], lightVector[2]);
		
		GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indices.limit(), GLES20.GL_UNSIGNED_SHORT, indices);
		
		cam.popM();	
	}

	@Override
	public void selectionDraw(GL10 glUnused) {		
		cam.pushM();
		super.selectionDraw(glUnused);
		
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, 0, vertices);

		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());

		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indices.limit(), GLES20.GL_UNSIGNED_SHORT, indices);
		
		cam.popM();
		super.selectionDrawCleanup();
	}
	
	
}
