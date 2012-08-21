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

package org.ros.android.rviz_for_android.drawable.loader;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.ros.android.renderer.Camera;
import org.ros.android.renderer.shapes.BaseShape;
import org.ros.android.renderer.shapes.BufferedTrianglesShape;
import org.ros.android.renderer.shapes.Color;
import org.ros.android.renderer.shapes.TexturedBufferedTrianglesShape;
import org.ros.android.renderer.shapes.TrianglesShape;
import org.ros.android.rviz_for_android.urdf.InvalidXMLException;
import org.ros.android.rviz_for_android.urdf.ServerConnection;
import org.ros.android.rviz_for_android.urdf.VTDXmlReader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;
import android.util.Log;

import com.tiffdecoder.TiffDecoder;
import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;

public class ColladaLoader extends VTDXmlReader {
	private static enum semanticType {
		POSITION(3), NORMAL(3), TEXCOORD(2);

		private int mul;

		semanticType(int mul) {
			this.mul = mul;
		}

		public int numElements(int vertexCount) {
			return mul * vertexCount;
		}

	};

	private List<BaseShape> geometries;

	private ServerConnection serverConnection;
	private String imgPrefix;
	private Camera cam;

	public ColladaLoader() {
		super();
		serverConnection = ServerConnection.getInstance();
	}

	public void setCamera(Camera cam) {
		this.cam = cam;
	}

	public void readDae(InputStream fileStream, String imgPrefix) throws InvalidXMLException {
		if(fileStream == null)
			throw new IllegalArgumentException("Invalid DAE file contents passed to ColladaLoader");
		this.imgPrefix = imgPrefix;
		
		boolean result = false;
		try {
			result = super.parse(IOUtils.toString(fileStream));
		} catch(IOException e) {
			e.printStackTrace();
		} 
		if(result)
			parseDae();
		else
			throw new InvalidXMLException();
	}

	private void parseDae() {
		// Get the ID of each geometry section
		List<String> nodes = super.getAttributeList("/COLLADA/library_geometries/geometry/@id");

		for(int i = 0; i < nodes.size(); i++) {
			String ID = nodes.get(i);
			Log.d("DAE", "Parsing geometry " + ID);
			parseGeometry(ID);
		}
	}

	private static enum TYPES {
		triangles(3, 0), tristrips(1, -2), trifans(1, -2);
		private int mul;
		private int sub;

		TYPES(int b, int i) {
			this.mul = b;
			this.sub = i;
		}

		public int getVertexCount(int elementCount) {
			return (mul * elementCount) + sub;
		}
	};

	// Each geometry can have multiple associated triangles in different
	// configurations using different materials
	// They all use the same vertices and normals though. If they don't, they
	// aren't supported by this loader currently.
	private void parseGeometry(String id) {
		geometries = new ArrayList<BaseShape>();
		String prefix = "/COLLADA/library_geometries/geometry[@id='" + id + "']/mesh";

		// If the selected geometry doesn't contain one of the types, return
		// null. We're not interested in lines or any other shenanigans
		boolean acceptableGeometry = false;
		for(TYPES t : TYPES.values()) {
			if(nodeExists(prefix, "", t.toString())) {
				acceptableGeometry = true;
				break;
			}
		}
		if(!acceptableGeometry) {
			return;
		}

		// For each submesh inside the mesh tag, parse its vertices, normals, and texture data
		for(TYPES type : TYPES.values()) {
			List<String> nodes = super.getNodeList(prefix, type.toString());
			for(int i = 1; i <= nodes.size(); i++) {
				geometries.add(parseSubMesh(prefix, type, i));
			}
		}
	}

	private static Color defaultColor = new Color(1f, 1f, 1f, 1);

	private BaseShape parseSubMesh(String prefix, TYPES type, int submeshIndex) {
		// Load all necessary data (vertices, normals, texture coordinates, etc
		Map<String, InputData> data = null;
		try {
			data = getDataFromAllInputs(prefix, type.toString());
		} catch(Exception e) {
			e.printStackTrace();
		}

		// Load indices
		short[] indices = toShortArray(getSingleAttribute(prefix, type + "[" + submeshIndex + "]/p"));

		// Find the triangle count
		int triCount = Integer.parseInt(getSingleAttribute(prefix, type.toString(), "@count"));

		Log.d("DAE", triCount + " triangles.");

		boolean textured = false;
		Map<String, ETC1Texture> textures = null;

		// Load the images if the mesh is textured. Otherwise, if the normals and positions are the only
		// values included AND they have the same offset, there's no need to deindex, can return a mesh immediately
		if(data.containsKey("TEXCOORD")) {
			textures = getTextures(prefix);
			textured = true;
		} else if(data.size() == 2 && data.containsKey("NORMAL") && data.containsKey("POSITION") && (data.get("NORMAL").getOffset() == data.get("POSITION").getOffset())) {
			Log.d("DAE", "Deindexing is not necessary for this mesh!");
			return new TrianglesShape(cam, data.get("POSITION").getData().getArray(), data.get("NORMAL").getData().getArray(), indices, defaultColor);
		}

		// Find the scale of the mesh (if present)
		if(nodeExists("/COLLADA/library_visual_scenes//scale/text()")) {
			float[] scales = toFloatArray(super.existResult);
			float[] vertices = data.get("POSITION").getData().getArray();
			for(int i = 0; i < vertices.length; i++) {
				vertices[i] = vertices[i] * scales[i % 3];
			}
		}

		// Deindex
		Map<String, FloatVector> results = deindex(data, indices, type.getVertexCount(triCount));

		Log.i("DAE", "The following information is available for each vertex: " + results.keySet());

		if(!textured) {
			switch(type) {
			case triangles:
				return new BufferedTrianglesShape(cam, results.get("POSITION").getArray(), results.get("NORMAL").getArray(), defaultColor);
			case tristrips:
			case trifans:
			default:
				return null;
			}
		} else {
			switch(type) {
			case triangles:
				return new TexturedBufferedTrianglesShape(cam, results.get("POSITION").getArray(), results.get("NORMAL").getArray(), results.get("TEXCOORD").getArray(), textures);
			case tristrips:
			case trifans:
			default:
				return null;
			}
		}
	}

	private enum textureType {
		diffuse// , bump
	};

	private Map<String, ETC1Texture> getTextures(String prefix) {
		Map<String, ETC1Texture> retval = new HashMap<String, ETC1Texture>();

		// Find which types of acceptable textures are present (diffuse, bump, etc)
		for(textureType t : textureType.values()) {
			if(attributeExists("/COLLADA/library_effects/", t.toString(), "texture/@texture")) {
				String texPointer = super.existResult;

				// Locate the image ID from the texture pointer
				String imgID = getSingleAttribute("/COLLADA/library_effects//newparam[@sid='" + texPointer + "']/sampler2D/source");

				// Locate the image name
				String imgName = getSingleAttribute("/COLLADA/library_effects//newparam[@sid='" + imgID + "']/surface/init_from");

				// Locate the filename
				String filename = getSingleAttribute("/COLLADA/library_images/image[@id='" + imgName + "']/init_from");

				// If a cached compressed copy exists, load that. Otherwise, download, compress, and save the image
				String compressedFilename = "COMPRESSED_" + serverConnection.getSanitizedPrefix(imgPrefix) + filename;
				if(!serverConnection.fileExists(compressedFilename)) {
					Log.i("DAE", "No compressed cached copy exists.");

					// Load the uncompressed image
					Bitmap uncompressed = openTextureFile(serverConnection.getContext().getFilesDir().toString() + "/", serverConnection.getFile(imgPrefix + filename));

					// Flip the image
					Matrix flip = new Matrix();
					flip.postScale(1f, -1f);
					Bitmap uncompressed_two = Bitmap.createBitmap(uncompressed, 0, 0, uncompressed.getWidth(), uncompressed.getHeight(), flip, true);
					uncompressed.recycle();

					// Compress the image
					ETC1Texture compressed = compressBitmap(uncompressed_two);

					// Save the compressed texture
					try {
						BufferedOutputStream bout = new BufferedOutputStream(serverConnection.getContext().openFileOutput(compressedFilename, Context.MODE_WORLD_READABLE));
						bout.write(compressed.getData().array());
						bout.close();
					} catch(FileNotFoundException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}

					// Add the compressed texture to the return map
					retval.put(t.toString(), compressed);
				} else {
					Log.i("DAE", "A compressed cached copy exists!");

					// Load the existing compressed texture
					try {
						byte[] dataArray = IOUtils.toByteArray(serverConnection.getContext().openFileInput(compressedFilename));
						// Determine the dimensions of the image
						int bytes = 2 * dataArray.length;
						int width = 1024;
						int height = 1024;

						while((width * height) > bytes && (width * height) >= 1) {
							width /= 2;
							height /= 2;
						}

						Log.i("DAE", "Compressed size determined to be " + width + " x " + height);

						ByteBuffer dataBuffer = ByteBuffer.allocateDirect(dataArray.length).order(ByteOrder.nativeOrder());
						dataBuffer.put(dataArray);
						dataBuffer.position(0);
						ETC1Texture compressed = new ETC1Texture(width, height, dataBuffer);
						retval.put(t.toString(), compressed);
					} catch(FileNotFoundException e) {
						Log.e("DAE", "Compressed texture not found!");
						e.printStackTrace();
					} catch(IOException e) {
						Log.e("DAE", "IOException!");
						e.printStackTrace();
					}
				}
			}
		}
		return retval;
	}

	private ETC1Texture compressBitmap(Bitmap uncompressedBitmap) {
		// Rescale the bitmap to be half it's current size
		Bitmap uncompressedBitmapResize = Bitmap.createScaledBitmap(uncompressedBitmap, uncompressedBitmap.getWidth() / 4, uncompressedBitmap.getHeight() / 4, true);
		uncompressedBitmap.recycle();

		// Copy the bitmap to a byte buffer
		ByteBuffer uncompressedBytes = ByteBuffer.allocateDirect(uncompressedBitmapResize.getByteCount()).order(ByteOrder.nativeOrder());
		uncompressedBitmapResize.copyPixelsToBuffer(uncompressedBytes);
		uncompressedBytes.position(0);

		int width = uncompressedBitmapResize.getWidth();
		int height = uncompressedBitmapResize.getHeight();

		// Compress the texture
		int encodedSize = ETC1.getEncodedDataSize(width, height);
		ByteBuffer compressed = ByteBuffer.allocateDirect(encodedSize).order(ByteOrder.nativeOrder());
		ETC1.encodeImage(uncompressedBytes, width, height, 2, 2 * width, compressed);

		ETC1Texture retval = new ETC1Texture(width, height, compressed);

		// We're done with the uncompressed bitmap, release it
		uncompressedBitmapResize.recycle();

		return retval;
	}

	private Bitmap openTextureFile(String path, String filename) {
		Bitmap retval = null;
		if(filename.toLowerCase().endsWith(".tif")) {
			Log.d("DAE", "Loading TIF image: " + path + filename);
			TiffDecoder.nativeTiffOpen(path + filename);
			int[] pixels = TiffDecoder.nativeTiffGetBytes();
			int width = TiffDecoder.nativeTiffGetWidth();
			int height = TiffDecoder.nativeTiffGetHeight();
			retval = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565);
			TiffDecoder.nativeTiffClose();
		} else {
			Log.d("DAE", "Loading non-TIF image: " + path + filename);
			retval = BitmapFactory.decodeFile(path + filename);
		}
		return retval;
	}

	private Map<String, FloatVector> deindex(Map<String, InputData> data, short[] indices, int vertexCount) {
		Map<String, FloatVector> retval = new HashMap<String, FloatVector>();

		List<InputData> sources = new ArrayList<InputData>(data.values());

		int inputCount = -99;
		for(InputData id : sources) {
			inputCount = Math.max(inputCount, id.getOffset());
			retval.put(id.getSemantic(), new FloatVector(id.getFloatElements(vertexCount)));
		}

		int curOffset = 0;
		for(Short s : indices) {
			for(InputData id : sources) {
				if(curOffset == id.getOffset()) {
					FloatVector reciever = retval.get(id.getSemantic());
					id.appendData(reciever, s);
				}
			}

			if(curOffset == inputCount)
				curOffset = 0;
			else
				curOffset++;
		}

		return retval;
	}

	private Map<String, InputData> getDataFromAllInputs(String prefix, String subMeshType) throws NumberFormatException, NavException, XPathEvalException {
		Map<String, InputData> retval = new HashMap<String, InputData>();

		getExpression(prefix, subMeshType, "input");
		int i;
		List<Integer> inputNodeLocations = new LinkedList<Integer>();
		while((i = ap.evalXPath()) != -1) {
			inputNodeLocations.add(i);
		}

		for(Integer b : inputNodeLocations) {
			vn.recoverNode(b);
			String semantic = vn.toString(vn.getAttrVal("semantic"));
			String sourceID = vn.toString(vn.getAttrVal("source")).substring(1);
			int offset = Integer.parseInt(vn.toString(vn.getAttrVal("offset")));
			List<InputData> returned = getDataFromInput(prefix, semantic, sourceID);
			for(InputData id : returned) {
				id.setOffset(offset);
				retval.put(id.getSemantic(), id);
			}
		}

		return retval;
	}

	private List<InputData> getDataFromInput(String prefix, String semantic, String sourceID) {
		List<InputData> retval = new ArrayList<InputData>();

		// Find whatever node has the requested ID
		String nodetype = getSingleContents(prefix, "/*[@id='" + sourceID + "']");

		// If it's a vertices node, get the data from the inputs it references
		if(nodetype.equals("vertices")) {
			List<String> inputs = super.getAttributeList(prefix, "/vertices[@id='" + sourceID + "']/input/@semantic");
			for(String subSemantic : inputs) {
				retval.addAll(getDataFromInput(prefix, subSemantic, getSingleAttribute(prefix, "/vertices[@id='" + sourceID + "']/input[@semantic='" + subSemantic + "']/@source").substring(1)));
			}

		} else
		// If it's a source, grab its float_array data
		if(nodetype.equals("source")) {
			retval.add(new InputData(semantic, new FloatVector(toFloatArray(getSingleContents(prefix, "/source[@id='" + sourceID + "']/float_array/text()")))));
			return retval;
		} else {
			Log.e("DAE", "ERR! UNKNOWN NODE TYPE: " + nodetype);
		}

		return retval;
	}

	private class InputData {
		private semanticType sType;
		private int offset = -1;
		private FloatVector data;

		public InputData(String semantic, FloatVector data) {
			super();
			this.sType = semanticType.valueOf(semantic);
			this.data = data;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}

		public String getSemantic() {
			return sType.toString();
		}

		public int getOffset() {
			return offset;
		}

		public FloatVector getData() {
			return data;
		}

		public int getFloatElements(int vertexCount) {
			return sType.numElements(vertexCount);
		}

		@Override
		public String toString() {
			return "InputData [semantic=" + sType.toString() + ", offset=" + offset + ", data size=" + data.getIdx() + "]";
		}

		public void appendData(FloatVector destination, int idx) {
			switch(sType) {
			case TEXCOORD:
				for(int b = (idx * 2); b < (idx * 2) + 2; b++)
					destination.add(data.get(b));
				break;
			case POSITION:
				for(int b = (idx * 3); b < (idx * 3) + 3; b++)
					destination.add(data.get(b));
				break;
			case NORMAL:
				// Normalize the loaded normal
				int offset = idx * 3;
				float x = data.get(offset++);
				float y = data.get(offset++);
				float z = data.get(offset++);
				float len = (float) Math.sqrt(x * x + y * y + z * z);

				destination.add(x / len);
				destination.add(y / len);
				destination.add(z / len);
				break;
			}
		}
	}

	public List<BaseShape> getGeometries() {
		return geometries;
	}
}
