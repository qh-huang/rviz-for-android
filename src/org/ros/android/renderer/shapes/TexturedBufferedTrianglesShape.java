package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;
import android.opengl.GLES20;

public class TexturedBufferedTrianglesShape extends BaseShape implements Cleanable {
	public static enum TextureSmoothing {Linear, Nearest};
	private static final Color baseColor = new Color(1f, 1f, 1f, 1f);

	private List<Integer> texIDArray = new ArrayList<Integer>();
	private Map<String, ETC1Texture> textures;
	protected int count;

	private boolean texturesLoaded = false;
	private TextureSmoothing smoothing = TextureSmoothing.Linear;
	
	private boolean bufferPrepared = false;
	private FloatBuffer vertexBuffer;
	
	public TexturedBufferedTrianglesShape(Camera cam, float[] vertices, float[] normals, float[] uvs, ETC1Texture diffuseTexture) {
		super(cam);
		super.setColor(baseColor);
		this.textures = new HashMap<String, ETC1Texture>();
		this.textures.put("diffuse", diffuseTexture);
		vertexBuffer = packBuffer(vertices,normals,uvs);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
		super.setProgram(GLSLProgram.TexturedShaded());
	}
	
	public TexturedBufferedTrianglesShape(Camera cam, float[] vertices, float[] normals, float[] uvs, Map<String, ETC1Texture> textures) {
		super(cam);
		super.setColor(baseColor);
		this.textures = textures;
		vertexBuffer = packBuffer(vertices,normals,uvs);
		setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
		super.setProgram(GLSLProgram.TexturedShaded());
	}
	
	private static final int FLOAT_SIZE = Float.SIZE/8;
	private static final int NUM_VERTEX = 3;
	private static final int NUM_NORMAL = 3;
	private static final int NUM_UV = 2;
	private static final int FLOATS_PER_VERTEX = NUM_VERTEX+NUM_NORMAL+NUM_UV;
	private static final int STRIDE = FLOATS_PER_VERTEX*FLOAT_SIZE;
	
	private FloatBuffer packBuffer(float[] vertices, float[] normals, float[] uvs) {
		if(vertices.length != normals.length || vertices.length/3 != uvs.length/2)
			throw new IllegalArgumentException("Vertex, normal, and UV arrays must describe the same number of vertices");
		
		bufferPrepared = false;
		int numVertices = vertices.length/3;
		count = vertices.length / 3;
		float[] packedBuffer = new float[numVertices*FLOATS_PER_VERTEX];

		int vIdx = 0, nIdx = 0, uIdx = 0;
		
		for(int i = 0; i < numVertices; i++) {
			int idx = i*FLOATS_PER_VERTEX;
			packedBuffer[idx+0] = vertices[vIdx++];
			packedBuffer[idx+1] = vertices[vIdx++];
			packedBuffer[idx+2] = vertices[vIdx++];
			
			packedBuffer[idx+3] = normals[nIdx++];
			packedBuffer[idx+4] = normals[nIdx++];
			packedBuffer[idx+5] = normals[nIdx++];
			
			packedBuffer[idx+6] = uvs[uIdx++];
			packedBuffer[idx+7] = uvs[uIdx++];
		}

		return Vertices.toFloatBuffer(packedBuffer);
	}
	
	public void setTextureSmoothing(TextureSmoothing s) {
		this.smoothing = s;
	}
	
	private int[] tmp = new int[1];
	private void loadTextures(GL10 glUnused) {	
		for(String s : textures.keySet()) {
			// Remove the texture from the map. Once it's loaded to the GPU, it isn't needed anymore
			ETC1Texture tex = textures.remove(s);
			
			if(tex != null) {
				// Generate a texture ID, append it to the list
				GLES20.glGenTextures(1, tmp, 0);
				texIDArray.add(tmp[0]);
				
				// Bind and load the texture
		        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tmp[0]);
		        GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, 0, ETC1.ETC1_RGB8_OES, tex.getWidth(), tex.getHeight(), 0, tex.getData().capacity(), tex.getData());
		        
		        // UV mapping parameters
		        if(smoothing == TextureSmoothing.Linear) {
		        	GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		        } else if(smoothing == TextureSmoothing.Nearest) {
		    	    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		    	    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		        }
			    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			}
		}
		
		texturesLoaded = true;
		textures = null;
	}
	
	private int createVertexBuffer(GL10 glUnused) {
		final int[] buffers = new int[1];
		GLES20.glGenBuffers(1, buffers, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity()*FLOAT_SIZE, vertexBuffer, GLES20.GL_STATIC_DRAW);
		
		bufferPrepared = true;
		return buffers[0];
	}
	
	private int bufferIdx;
	private static final int VERTEX_OFFSET = 0;
	private static final int NORMAL_OFFSET = NUM_VERTEX*FLOAT_SIZE;
	private static final int UV_OFFSET = (NUM_NORMAL+NUM_VERTEX)*FLOAT_SIZE;
	private volatile boolean cleanUp = false;
	
	@Override
	public void draw(GL10 glUnused) {	
		cam.pushM();
		if(cleanUp) {
			clearBuffers(glUnused);
			return;
		}	
		if(!bufferPrepared)
			bufferIdx = createVertexBuffer(glUnused);
		if(!texturesLoaded)
			loadTextures(glUnused);
		
		super.draw(glUnused);
		calcMVP();
		// Uniforms
		GLES20.glUniform3f(getUniform(ShaderVal.LIGHTVEC), lightVector[0], lightVector[1], lightVector[2]);
		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		calcNorm();
		GLES20.glUniformMatrix3fv(getUniform(ShaderVal.NORM_MATRIX), 1, false, NORM, 0);
		
		// Attributes
		GLES20.glEnableVertexAttribArray(ShaderVal.TEXCOORD.loc);
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		GLES20.glEnableVertexAttribArray(ShaderVal.NORMAL.loc);
		
		GLES20.glBindBuffer(GL11.GL_ARRAY_BUFFER, bufferIdx);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, STRIDE, VERTEX_OFFSET);
		GLES20.glVertexAttribPointer(ShaderVal.NORMAL.loc, 3, GLES20.GL_FLOAT, false, STRIDE, NORMAL_OFFSET);
		GLES20.glVertexAttribPointer(ShaderVal.TEXCOORD.loc, 2, GLES20.GL_FLOAT, false, STRIDE, UV_OFFSET);
		
		// Bind texture(s)
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		for(int i : texIDArray)
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, i);
		
		// Draw
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
		
		// Unbind the buffer
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		cam.popM();
	}

	@Override
	public void selectionDraw(GL10 glUnused) {		
		cam.pushM();
		if(!bufferPrepared)
			bufferIdx = createVertexBuffer(glUnused);
		if(!texturesLoaded)
			loadTextures(glUnused);
		
		super.selectionDraw(glUnused);

		GLES20.glUniform4f(getUniform(ShaderVal.UNIFORM_COLOR), getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor().getAlpha());
		GLES20.glUniformMatrix4fv(getUniform(ShaderVal.MVP_MATRIX), 1, false, MVP, 0);
		GLES20.glEnableVertexAttribArray(ShaderVal.POSITION.loc);
		
		GLES20.glBindBuffer(GL11.GL_ARRAY_BUFFER, bufferIdx);
		GLES20.glVertexAttribPointer(ShaderVal.POSITION.loc, 3, GLES20.GL_FLOAT, false, STRIDE, VERTEX_OFFSET);
		
		// Draw
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
		
		// Unbind the buffer
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		cam.popM();
		super.selectionDrawCleanup();
	}

	private boolean cleaned = false;
	
	private void clearBuffers(GL10 glUnused) {
		if(!cleaned) {
			int[] tmp = new int[1];
			tmp[0] = bufferIdx;
			GLES20.glDeleteBuffers(1,tmp,0);
			for(int i : texIDArray) {
				tmp[0] = i;
				GLES20.glDeleteTextures(1, tmp, 0);
			}
			texIDArray.clear();
			cleaned = true;
		}
	}

	@Override
	public void cleanup() {
		cleanUp = true;
	}
}
