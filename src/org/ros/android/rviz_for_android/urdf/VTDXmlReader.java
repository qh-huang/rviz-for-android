package org.ros.android.rviz_for_android.urdf;

import java.util.LinkedList;
import java.util.List;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

/**
 * An abstract XML parsing class using the VTD library to efficiently parse XML documents
 * 
 * @author azimmerman
 */
public abstract class VTDXmlReader {

	protected AutoPilot ap;
	protected VTDNav vn;

	public VTDXmlReader() {
	}

	/**
	 * Build a VTD model of the given XML document
	 * 
	 * @param xml
	 *            the XML document in string form
	 * @return true if parsing succeeded, false if the XML is malformed
	 */
	protected boolean parse(String xml) {
		final VTDGen vg = new VTDGen();
		vg.setDoc(xml.getBytes());
		try {
			vg.parse(false);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}

		vn = vg.getNav();
		ap = new AutoPilot(vn);
		return true;
	}

	/**
	 * Evaluate the given XPath expression
	 * 
	 * @param xPathExpression
	 *            the XPath expression to evaluate
	 */
	protected void getExpression(String... xPathExpression) {
		try {
			ap.selectXPath(Compose(xPathExpression));
		} catch(XPathParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Build an XPath navigable string from the provided node names
	 * 
	 * @param pieces
	 * @return an XPath navigable string combining all provided node names in order
	 */
	protected String Compose(String... pieces) {
		if(pieces.length == 1)
			return pieces[0];

		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < pieces.length; i++) {
			sb.append(pieces[i]);
			if(i < pieces.length - 1) {
				sb.append("/");
			}
		}
		return sb.toString();
	}

	/**
	 * Find all attributes associated with the node specified by an XPath expression
	 * 
	 * @param xPathExpression
	 * @return all attributes of the given node
	 */
	protected List<String> getAttributeList(String... xPathExpression) {
		List<String> retval = new LinkedList<String>();
		getExpression(xPathExpression);
		int i;
		try {
			while((i = ap.evalXPath()) != -1) {
				retval.add(vn.toString(i + 1));
			}
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return retval;
	}

	/**
	 * Find all child nodes of the node specified by an XPath expression
	 * 
	 * @param xPathExpression
	 * @return all child nodes of the given node
	 */
	protected List<String> getNodeList(String... xPathExpression) {
		List<String> retval = new LinkedList<String>();
		getExpression(xPathExpression);
		int i;
		try {
			while((i = ap.evalXPath()) != -1) {
				retval.add(vn.toString(i));
			}
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return retval;
	}

	/**
	 * Find the value of a single attribute of specified by an XPath expression.
	 * 
	 * @param xPathExpression
	 *            the XPath expression specifying a single attribute. If multiple attributes match the XPath expression an IllegalArgumentException will be thrown.
	 * @return the attribute requested
	 */
	protected String getSingleAttribute(String... xPathExpression) {
		getExpression(xPathExpression);
		String result = null;
		try {
			result = vn.toString(ap.evalXPath() + 1);
			if(ap.evalXPath() != -1)
				throw new IllegalArgumentException("Expression returned multiple results!");
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return result;
	}

	/**
	 * Return the contents of a single node specified by an XPath expression
	 * 
	 * @param xPathExpression
	 *            the XPath expression specifying a single node. If multiple attributes match the XPath expression an IllegalArgumentException will be thrown.
	 * @return the node contents
	 */
	protected String getSingleContents(String... xPathExpression) {
		getExpression(xPathExpression);
		String result = null;
		try {
			result = vn.toString(ap.evalXPath());
			if(ap.evalXPath() != -1)
				throw new IllegalArgumentException("Expression returned multiple results!");
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return result;
	}

	/**
	 * Count the number of attributes associated with a node
	 * 
	 * @param xPathExpression
	 * @return
	 */
	protected int nodeCount(String... xPathExpression) {
		return getAttributeList(xPathExpression).size();
	}

	protected String existResult;

	/**
	 * Determine if a node exists. Store the results of the specified XPath expression in the protected String variable existResult
	 * 
	 * @param xPathExpression
	 * @return boolean indicating the existence of the requested node.
	 */
	protected boolean nodeExists(String... xPathExpression) {
		boolean result = false;
		getExpression(xPathExpression);
		try {
			int res = ap.evalXPath();
			result = (res != -1);
			if(result)
				existResult = vn.toString(res);
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return result;
	}

	/**
	 * Determine if an attribute exists. Store the results of the specified XPath expression in the protected String variable existResult
	 * 
	 * @param xPathExpression
	 * @return boolean indicating the existence of the requested attribute.
	 */
	protected boolean attributeExists(String... xPathExpression) {
		boolean result = false;
		getExpression(xPathExpression);
		try {
			int res = ap.evalXPath();
			result = (res != -1);
			existResult = vn.toString(res + 1);
		} catch(XPathEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ap.resetXPath();
		return result;
	}

	protected short[] toShortArray(String str) {
		String[] pieces = str.split(" ");
		short[] retval = new short[pieces.length];
		for(int i = 0; i < pieces.length; i++) {
			retval[i] = Short.parseShort(pieces[i]);
		}
		return retval;
	}

	protected float[] toFloatArray(String str) {
		String[] pieces = str.split(" ");
		float[] retval = new float[pieces.length];
		for(int i = 0; i < pieces.length; i++) {
			retval[i] = Float.parseFloat(pieces[i]);
		}
		return retval;
	}
}
