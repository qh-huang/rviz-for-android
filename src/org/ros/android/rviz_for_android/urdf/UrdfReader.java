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
package org.ros.android.rviz_for_android.urdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ros.android.renderer.shapes.Color;

import android.util.Log;

/**
 * Read a URDF file and produce a UrdfLink object for each link in the file
 * 
 * @author azimmerman
 * 
 */
public class UrdfReader extends VTDXmlReader {

	public interface UrdfReadingProgressListener {
		public void readLink(int linkNumber, int linkCount);
	}

	private UrdfReadingProgressListener listener;

	public void setListener(UrdfReadingProgressListener l) {
		listener = l;
	}

	private void publishProgress(int link, int count) {
		if(listener != null)
			listener.readLink(link, count);
	}

	private List<UrdfLink> urdf = new ArrayList<UrdfLink>();

	public UrdfReader() {
		super();
	}

	public void readUrdf(String urdf) throws InvalidXMLException {
		this.urdf.clear();
		if(super.parse(urdf)) {
			try {
				parseUrdf();
			} catch(IllegalArgumentException e) {
				Log.e("URDF", "Can't parse URDF: " + e.getMessage());
				e.printStackTrace();
				throw new InvalidXMLException(e.getMessage());
			}
			buildColors();
		} else {
			throw new InvalidXMLException("Improper XML formatting");
		}
	}

	private void buildColors() {
		Log.i("URDF", "Building colors...");
		Map<String, Color> colors = new HashMap<String, Color>();

		// Not all URDF files store the color/name pairs under the links. Must rebuild the color library searching the entire URDF file for color tags
		List<String> colorNames = getAttributeList("//material/@name");
		for(String name : colorNames) {
			if(!colors.containsKey(name)) {
				String rgbaQuery = "//material[@name='" + name + "']/color/@rgba";
				if(attributeExists(rgbaQuery)) {
					float[] color = toFloatArray(super.existResult);
					colors.put(name, new Color(color[0], color[1], color[2], color[3]));
					Log.i("URDF", "    Built color " + name);
				}
			}
		}

		for(UrdfLink ul : urdf) {
			for(Component c : ul.getComponents()) {
				if(c.getMaterial_name() != null && colors.containsKey(c.getMaterial_name())) {
					c.setMaterial_color(colors.get(c.getMaterial_name()));
				}
			}
		}
	}

	public List<UrdfLink> getUrdf() {
		return urdf;
	}

	private void parseUrdf() {
		List<String> links = getAttributeList("/robot/link/@name");

		int nodeLength = links.size();

		for(int i = 0; i < nodeLength; i++) {
			Log.i("URDF", "Parsing node " + (i + 1) + " of " + nodeLength);

			// Link name
			String name = links.get(i);
			String prefix = "/robot/link[@name='" + name + "']";

			Component visual = null;
			Component collision = null;

			// Check for visual component
			if(nodeExists(prefix, "visual")) {
				String vprefix = prefix + "/visual";

				// Get geometry type
				String gtype = getSingleContents(vprefix, "geometry/*");

				Component.Builder visBuilder = new Component.Builder(gtype);

				switch(visBuilder.getType()) {
				case BOX: {
					String size = getSingleAttribute(vprefix, "/box/@size");
					visBuilder.setSize(toFloatArray(size));
				}
					break;
				case CYLINDER: {
					float radius = Float.parseFloat(getSingleAttribute(vprefix, "/cylinder/@radius"));
					float length = Float.parseFloat(getSingleAttribute(vprefix, "/cylinder/@length"));
					visBuilder.setRadius(radius);
					visBuilder.setLength(length);
				}
					break;
				case SPHERE: {
					float radius = Float.parseFloat(getSingleAttribute(vprefix, "/sphere/@radius"));
					visBuilder.setRadius(radius);
				}
					break;
				case MESH:
					visBuilder.setMesh(getSingleAttribute(vprefix, "/mesh/@filename"));
					if(attributeExists(vprefix, "/mesh/@scale"))
						visBuilder.setMeshScale(super.toFloatArray(existResult));
					break;
				}

				// OPTIONAL - get origin
				if(attributeExists(vprefix, "/origin/@xyz")) {
					visBuilder.setOffset(toFloatArray(existResult));
				}
				if(attributeExists(vprefix, "/origin/@rpy")) {
					visBuilder.setRotation(toFloatArray(existResult));
				}

				// OPTIONAL - get material
				if(attributeExists(vprefix, "/material/@name")) {
					visBuilder.setMaterialName(existResult);
				}
				if(attributeExists(vprefix, "/material/color/@rgba")) {
					visBuilder.setMaterialColor(toFloatArray(existResult));
				}
				visual = visBuilder.build();
			}

			// Check for collision component
			if(nodeExists(prefix, "collision")) {
				String vprefix = prefix + "/collision";

				// Get geometry type
				String gtype = getSingleContents(vprefix, "geometry/*");

				Component.Builder colBuilder = new Component.Builder(gtype);

				switch(colBuilder.getType()) {
				case BOX: {
					String size = getSingleAttribute(vprefix, "/box/@size");
					colBuilder.setSize(toFloatArray(size));
				}
					break;
				case CYLINDER: {
					float radius = Float.parseFloat(getSingleAttribute(vprefix, "/cylinder/@radius"));
					float length = Float.parseFloat(getSingleAttribute(vprefix, "/cylinder/@length"));
					colBuilder.setRadius(radius);
					colBuilder.setLength(length);
				}
					break;
				case SPHERE: {
					float radius = Float.parseFloat(getSingleAttribute(vprefix, "/sphere/@radius"));
					colBuilder.setRadius(radius);
				}
					break;
				case MESH:
					colBuilder.setMesh(getSingleAttribute(vprefix, "/mesh/@filename"));
					break;
				}

				// OPTIONAL - get origin
				if(attributeExists(vprefix, "/origin/@xyz"))
					colBuilder.setOffset(toFloatArray(existResult));
				if(attributeExists(vprefix, "/origin/@rpy"))
					colBuilder.setRotation(toFloatArray(existResult));

				collision = colBuilder.build();
			}

			UrdfLink newLink = new UrdfLink(visual, collision, name);
			urdf.add(newLink);
			publishProgress(i + 1, nodeLength);
		}
	}
}
