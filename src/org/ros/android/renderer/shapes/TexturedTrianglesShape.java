package org.ros.android.renderer.shapes;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import org.ros.android.renderer.Camera;
import org.ros.android.renderer.Vertices;
import org.ros.android.rviz_for_android.drawable.GLSLProgram;
import org.ros.android.rviz_for_android.drawable.GLSLProgram.ShaderVal;

import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;
import android.opengl.GLES20;

/**
 * @author azimmerman
 *
 */
public class TexturedTrianglesShape extends TrianglesShape implements Cleanable {
	public static enum TextureSmoothing {Linear, Nearest};
	private static final Color baseColor = new Color(1f, 1f, 1f, 1f);

	protected final FloatBuffer uv;
	private List<Integer> texIDArray = new ArrayList<Integer>();
	private Map<String, ETC1Texture> textures;

	private boolean texturesLoaded = false;
	private TextureSmoothing smoothing = TextureSmoothing.Linear;

	private boolean cleanUp = false;
	
	public TexturedTrianglesShape(Camera cam, float[] vertices, float[] normals, float[] uvs, ETC1Texture diffuseTexture) {
		super(cam, vertices, normals, baseColor);
		uv = Vertices.toFloatBuffer(uvs);
		this.textures = new HashMap<String, ETC1Texture>();
		this.textures.put("diffuse", diffuseTexture);
		init();
	}
	
	public TexturedTrianglesShape(Camera cam, float[] vertices, float[] normals, float[] uvs, Map<String, ETC1Texture> textures) {
		super(cam, vertices, normals, baseColor);
		uv = Vertices.toFloatBuffer(uvs);
		this.textures = textures;
		init();
	}
	
	private void init() {
		super.setProgram(GLSLProgram.TexturedShaded());
	}
	
	/**
	 * Determines which texture smoothing (TEXTURE_MIN_FILTER and TEXTURE_MAG_FILTER) modes are used for this shape.
	 * This must be called before the shape is first drawn to have any effect.
	 * @param s The smoothing mode to use
	 */
	public void setTextureSmoothing(TextureSmoothing s) {
		this.smoothing = s;
	}

	@Override
	public void draw(GL10 glUnused) {		
		if(cleanUp) {
			clearBuffers(glUnused);
			return;
		}
		
		cam.pushM();
		if(!texturesLoaded)
			loadTextures(glUnused);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		for(Integer i : texIDArray)
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, i);
		
		GLES20.glEnableVertexAttribArray(ShaderVal.TEXCOORD.loc);
		GLES20.glVertexAttribPointer(ShaderVal.TEXCOORD.loc, 2, GLES20.GL_FLOAT, false, 0, uv);
		super.draw(glUnused);
		
		cam.popM();
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
	
	public void cleanup() {
		cleanUp = true;
	}
	
	/**
	 * Clear the buffered textures which have been loaded
	 * @param gl
	 */
	private boolean cleaned = false;
	private void clearBuffers(GL10 glUnused) {
		if(!cleaned) {
			for(Integer i : texIDArray) {
				tmp[0] = i;
				GLES20.glDeleteTextures(1, tmp, 0); 
			}
			texIDArray.clear();
			cleaned = true;
		}
	}
}
