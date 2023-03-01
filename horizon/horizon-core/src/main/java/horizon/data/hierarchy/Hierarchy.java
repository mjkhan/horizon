/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.data.hierarchy;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import horizon.base.AbstractComponent;

/**Is a collection of elements that comprises a hierarchy.<br />
 * It is created by a {@link HierarchyBuilder HierarchyBuilder}.
 * With a Hierarchy, you can get
 * <ul><li>{@link #getElements() elements of the Hierarchy}</li>
 *     <li>{@link #getIndex() index of the elements}</li>
 *     <li>{@link #topElements() top elements}</li>
 *     <li>{@link #getTop() root element}</li>
 *     <li>{@link #get(Object) an element} with a key</li>
 * </ul>
 * @param <T> an element type
 */
public class Hierarchy<T> extends AbstractComponent implements Serializable {
	private static final long serialVersionUID = 1L;

	/**Returns a simple string representation of objs.<br />
	 * In building the string representation,
	 * the <code>getChildren</code> function is used to get child elements of the current element.
	 * @param objs			objects for string representation
	 * @param getChildren	function that returns the child elements of the current element
	 * @return string representation of objs
	 */
	public static <T> String toString(Iterable<T> objs, Function<T, Iterable<T>> getChildren) {
		return new Stringify<T>()
			.beginElement((e, level, index) -> {
				String str = index == 0 && level == 0 ? "" : "\n";
				if (level > 0)
					str += Stringify.indent("  ", level) + "+-";
				return str += e;
			})
			.getChildren(getChildren)
			.get(objs);
	}

	private Collection<T> elements;
	private List<T> tops;
	private Map<Object, T> index;

	/**Returns whether the Hierarchy is empty.
	 * @return
	 * <ul><li>true if the Hierarchy is empty</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean isEmpty() {
		return isEmpty(index) || isEmpty(elements);
	}

	/**Returns the index of the Hierarchy's elements.
	 * @return index of the Hierarchy's elements
	 */
	public Map<Object, T> getIndex() {
		return ifEmpty(index, () -> Collections.emptyMap());
	}

	Hierarchy<T> setIndex(Map<Object, T> index) {
		this.index = index;
		return this;
	}

	/**Returns the elements of the Hierarchy.
	 * @return elements of the Hierarchy
	 */
	public Collection<T> getElements() {
		return elements;
	}

	Hierarchy<T> setElements(Collection<T> elements) {
		this.elements = elements;
		return this;
	}

	/**Returns the top elements of the Hierarchy.
	 * @return top elements of the Hierarchy
	 */
	public List<T> topElements() {
		return ifEmpty(tops, () -> Collections.emptyList());
	}

	/**Returns the root or first top element of the Hierarchy.
	 * @return root element of the Hierarchy
	 */
	public T getTop() {
		return !isEmpty() ? tops.iterator().next() : null;
	}

	Hierarchy<T> setTops(List<T> tops) {
		this.tops = tops;
		return this;
	}

	/**Returns the object with the given key.
	 * @param key key for an object
	 * @return object with the given key
	 */
	public T get(Object key) {
		return !isEmpty() ? index.get(key) : null;
	}
}