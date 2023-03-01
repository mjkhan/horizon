/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import horizon.base.AbstractComponent;
/**Utility to help reading XML elements.
 */
public class Xmlement extends AbstractComponent {
	private static Xmlement obj;

	/**Returns the singleton Xmlement.
	 * @return an Xmlement
	 */
	public static final Xmlement get() {
		return obj != null ? obj : (obj = new Xmlement());
	}

	/**Returns the document element read from the input.
	 * @param input an InputStream
	 * @return document element
	 */
	public Element getDocument(InputStream input) {
		return getDocument(input, true);
	}

	/**Returns the document element read from the input.
	 * @param input an InputStream
	 * @param validateDtd
	 * <ul><li>true to perform dtd validation</li>
	 * 	   <li>false to ignore dtd validation</li>
	 * </ul>
	 * @return document element
	 */
	public Element getDocument(InputStream input, boolean validateDtd) {
		if (input == null) return null;

		try (InputStream in = input) {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			if (!validateDtd)
				builder.setEntityResolver(new IgnoreDtd());
			return builder.parse(in).getDocumentElement();
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	/**Finds the document element from the resource on the path.
	 * @param path	path to the resource
	 * @return
	 * <ul><li>document element</li>
	 * 	   <li>null if the resource is not found on the path</li>
	 * </ul>
	 */
	public Element findDocument(String path) {
		InputStream input = ResourceLoader.find(path);
		return input != null ? getDocument(input) : null;
	}

	/**Loads the document element from the resource on the path.
	 * @param path	path to the resource
	 * @return document element
	 * @throws RuntimeException if the resource is not found on the path
	 */
	public Element getDocument(String path) {
		return getDocument(ResourceLoader.load(path));
	}

	/**Returns the parent node's child element with the tag.
	 * @param parent a parent node
	 * @param tag	 tag name of the required child element
	 * @return parent element's child element with the tag
	 */
	public Element getChild(Node parent, String tag) {
		List<Element> children = getChildren(parent, tag);
		return !children.isEmpty() ? children.get(0) : null;
	}

	private List<Element> getElements(NodeList nodes, String tag) {
		ArrayList<Element> elements = new ArrayList<>();
		for (int i = 0, count = nodes == null ? 0 : nodes.getLength(); i < count; ++i) {
			Node node = nodes.item(i);
			if (Node.ELEMENT_NODE != node.getNodeType()) continue;
			if (isEmpty(tag))
				elements.add(Element.class.cast(node));
			else {
				if (tag.equals(node.getNodeName()))
					elements.add(Element.class.cast(node));
			}
		}
		return elements;
	}

	/**Returns the parent element's child elements with the tag.
	 * If the tag is empty, returns all the child elements of the parent element.
	 * @param parent a parent node
	 * @param tag	 tag name of the required child element
	 * @return
	 * <ul><li>parent element's child elements with the tag</li>
	 * 	   <li>If the tag is empty, all the child elements of the parent element</li>
	 * </ul>
	 */
	public final List<Element> getChildren(Node parent, String tag) {
		return getElements(parent.getChildNodes(), tag);
	}

	/**Returns the text content of the tagged child element of the parent element.
	 * @param parent a parent element
	 * @param tag	 tag name of the required child element
	 * @return text content of the tagged child element of the parent element
	 */
	public String childContent(Node parent, String tag) {
		Element child = getChild(parent, tag);
		return content(child);
	}

	/**Returns the text content of the node.
	 * @param node a node
	 * @return text content of the node
	 */
	public String content(Node node) {
		return node != null ? node.getTextContent() : null;
	}

	/**Returns the value of the named attribute of the node.
	 * @param e a node
	 * @param name attribute name
	 * @return value of the named attribute
	 */
	public String attribute(Node e, String name) {
		if (e == null) return null;

		NamedNodeMap nodemap = e.getAttributes();
		Node attr = nodemap != null ? nodemap.getNamedItem(name) : null;
		return attr == null ? null : attr.getNodeValue();
	}

	/**Returns the inner xml of the node using the serializer.
	 * @param serializer the LSSerializer from the owner document of the node.
	 * @param node a node
	 * @return inner xml of the node
	 */
	public String innerXml(LSSerializer serializer, Node node) {
		return serializer.writeToString(node).trim();
	}

	/**Returns the inner xml of the node.
	 * @param node a node
	 * @return inner xml of the node
	 */
	public String innerXml(Node node) {
		if (node == null) return "";

		DOMImplementationLS ls = (DOMImplementationLS)node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
		LSSerializer serializer = ls.createLSSerializer();
		serializer.getDomConfig().setParameter("xml-declaration", false);
		return innerXml(serializer, node);
	}

	private static class IgnoreDtd implements EntityResolver {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			return systemId.toLowerCase().contains(".dtd") ?
				new InputSource(new StringReader("")):
				null;
		}
	}
}