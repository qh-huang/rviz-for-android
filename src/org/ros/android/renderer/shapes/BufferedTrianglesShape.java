package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;
import org.ros.rosjava_geometry.Transform;

import android.opengl.GLES20;

/**
 * A triangles shape which uses vertex buffers to cache geometry on the GPU. Vertices and normals are stored in a packed buffer: Xv,Yv,Zv, Xn,Yn,Zn, ...
 * @author azimmerman
 *
 */
public class BufferedTrianglesShape extends BaseShape {
	private FloatBuffer packedBuffer;

	private boolean bufferPrepared = false;
	protected int count;

	public BufferedTrianglesShape(Camera cam, float[] vertices, float[] normals, Color color) {
		super(cam);
		packedBuffer = packBuffer(vertices, normals);
		
		count = vertices.length / 3;
		super.setColor(color);
		super.setTransform(Transform.identity());
		super.setProgram(GLSLProgram.FlatShaded());
	}
	
	private FloatBuffer packBuffer(float[] vertices, float[] normals) {
		if(vertices.length != normals.length)
			throw new IllegalArgumentException("Vertex array and normal array must be the same length!");
		
		bufferPrepared = false;
		float[] packedBuffer;
		
		int bufferLength = vertices.length*2;
		int arrayElements = vertices.length/3;
		int vIdx = 0, nIdx = 0;
		
		packedBuffer = new float[bufferLength];
		for(int i = 0; i < arrayElements; i++) {
			int idx = i*6;
			packedBuffer[idx+0] = vertices[vIdx++];
			packedBuffer[idx+1] = vertices[vIdx++];
			packedBuffer[idx+2] = vertices[vIdx++];
			
			packedBuffer[idx+3] = normals[nIdx++];
			packedBuffer[idx+4] = normals[nIdx++];
			packedBuffer[idx+5] = normals[nIdx++];
		}

		return Vertices.toFloatBuffer(packedBuffer);
	}

	private static final int POSITION_DATA_SIZE = 3;
	private static final int NORMAL_DATA_SIZE = 3;
	private int bufferIdx = -1;
	private static final int FLOAT_SIZE = Float.SIZE/8;
	private static final int STRIDE = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE)*FLOAT_SIZE; 
	private static final int NORMAL_OFFSET = POSITION_DATA_SIZE*FLOAT_SIZE;
	private static final int POSITION_OFFSET = 0;
	
	@Override
	public void draw(GL10 glUnused) {	
		cam.pushM();
		if(!bufferPrepared)
			bufferIdx = createVertexBuffer(glUnused);
		
		super.draw(glUnused);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIdx);
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE, POSITION_OFFSET);
		
		GLES20.glEnableVertexAttribArray(ShaderVal.NORMAL.loc);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE, NORMAL_OFFSET);
		
		calcMVP();
		calcNorm();
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glUniformMatrix3fv(getUniform(ShaderVal.NORM_MATRIX), 1, false, NORM, 0);
		
		GLES20.glUniform3f(getUniform(ShaderVal.LIGHTVEC), lightVector[0], lightVector[1], lightVector[2]);
		
		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		cam.popM();
	}
	
	@Override
	public void selectionDraw(GL10 glUnused) {
		cam.pushM();
		if(!bufferPrepared)
			bufferIdx = createVertexBuffer(glUnused);
		
		super.selectionDraw(glUnused);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIdx);
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE, POSITION_OFFSET);
		
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);

		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		
		super.selectionDrawCleanup();
		cam.popM();
	}

	private int createVertexBuffer(GL10 glUnused) {
		final int[] buffers = new int[1];
		GLES20.glGenBuffers(1, buffers, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
		packedBuffer.position(0);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, packedBuffer.capacity()*FLOAT_SIZE, packedBuffer, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		bufferPrepared = true;
		return buffers[0];
	}
}
