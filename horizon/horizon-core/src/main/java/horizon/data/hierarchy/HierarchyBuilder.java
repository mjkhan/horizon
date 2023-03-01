/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.data.hierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import horizon.base.AbstractComponent;

/**Builds an object hierarchy.
 * <p>For a HierarchyBuilder to build an object hierarchy, you should set
 * <ul><li>{@link #setElements(Collection) objects} used to make up a hierarchy</li>
 * 	   <li>an {@link #setInstruction(Instruction) instruction} for steps to build the hierarchy</li>
 * </ul>
 * </p>
 * <p>An {@link Instruction} is a set of steps for a HierarchyBuilder to perform in Hierarchy construction.<br />
 * For convenience, a HierarchyBuilder has methods that sets the steps to an internal instruction.
 * </p>
 * <p> With elements and an instruction to work with, the HierarchyBuilder {@link #build() builds} up an object hierarchy.<br />
 * By default, a HierarchyBuilder returns an instance of the {@link Hierarchy} class.<br />
 * To get an instance of a Hierarchy extension, use the {@link #build(Supplier)} method.<br />
 * You can also get either {@link #get() top elements} or a {@link #get(Object) root element} without an instance of a Hierarchy.
 * </p>
 * <p>Following is an example of a HierarchyBuilder creating a hierarchy of objects.
 * <pre class="shade">{@code Collection<MyObject> elements = ...;
 * HierarchyBuilder.Instruction<MyObject> instruction = new HierarchyBuilder.Instruction<>()
 *     .getID(e -> e.getId())
 *     .getParentID(e -> e.getParentId())
 *     .addChild((parent, child) -> parent.add(child))
 *     .atTop(e -> e.getParentId() == null || e.getId().equals(e.getParentId()));
 * HierarchyBuilder<MyObject> builder = new HierarchyBuilder<>()
 *     .setInstruction(instruction);
 *     .setElements(elements);
 * Hierarchy<MyObject> hierarchy = builder.build();
 * List<MyObject> tops = hierarchy.topElements();
 * MyObject root = hierarchy.getTop();}</pre>
 * You can make the code shorter like this:
 * <pre class="shade">{@code ...
 * Hierarchy<MyObject> hierarchy = new HierarchyBuilder<MyObject>()
 *     .getID(e -> e.getId()) //Sets the steps to the internal instruction
 *     .getParentID(e -> e.getParentId())
 *     .addChild((parent, child) -> parent.add(child))
 *     .atTop(e -> e.getParentId() == null || e.getId().equals(e.getParentId()))
 *     .setElements(elements)
 *     .build();
 * ...}</pre>
 * </p>
 */
public class HierarchyBuilder<T> extends AbstractComponent {
	private Collection<T> elements;
	private HashMap<Object, T> index;
	private Instruction<T> instruction;

	/**Sets elements that are used to build up a hierarchy.
	 * @param elements elements that are used to build up a hierarchy
	 * @return this HierarchyBuilder
	 */
	public HierarchyBuilder<T> setElements(Collection<T> elements) {
		if (elements == null)
			throw new IllegalArgumentException("elements: null");
		this.elements = elements;
		createIndex();
		return this;
	}

	private Instruction<T> instruction() {
		return instruction != null ? instruction : (instruction = new Instruction<>());
	}

	/**Sets the instruction to build a hierarchy.
	 * @param instruction instruction to build a hierarchy
	 * @return this HierarchyBuilder
	 */
	public HierarchyBuilder<T> setInstruction(Instruction<T> instruction) {
		this.instruction = instruction;
		return this;
	}

	/**See {@link Instruction#atTop(Predicate)}.
	 * @return this HierarchyBuilder
	 */
	public HierarchyBuilder<T> atTop(Predicate<T> test) {
		instruction().atTop(test);
		return this;
	}

	/**See {@link Instruction#getKey(Function)}.
	 * @return this HierarchyBuilder
	 */
	public HierarchyBuilder<T> getKey(Function<T, Object> func) {
		instruction().getKey(func);
		return this;
	}

	/**See {@link Instruction#getParentKey(Function)}.
	 * @return this HierarchyBuilder
	 */
	public HierarchyBuilder<T> getParentKey(Function<T, Object> func) {
		instruction().getParentKey(func);
		return this;
	}

	/**See {@link Instruction#addChild(BiConsumer)}.
	 * @return this HierarchyBuilder
	 */
	public HierarchyBuilder<T> addChild(BiConsumer<T, T> func) {
		instruction().addChild(func);
		return this;
	}

	private void createIndex() {
		index = new HashMap<Object, T>();
		for (T e: elements)
			index.put(instruction.getKey.apply(e), e);
	}

	/**Returns the index of the elements in the hierarchy.
	 * @return index of the elements in the hierarchy
	 */
	public Map<Object, T> getIndex() {
		return index;
	}

	private void add(T parent, T child) {
		if (parent == null || child == null || parent == child) return;

		instruction.addChild.accept(parent, child);
	}

	/**Creates a hierarchy and returns its root element.
	 * @param rootKey Key of the root element
	 * @return root element of the created hierarchy
	 */
	public T get(Object rootKey) {
		if (isEmpty(elements)) return null;

		T root = index.get(rootKey);
		if (root == null)
			throw new RuntimeException("Root element not found: " + rootKey);

		for (T element: elements) {
			if (element == null) continue;
			Object parentID = instruction.getParentKey.apply(element);
			T parent = parentID !=null ? index.get(parentID) : null;
			add(parent, element);
		}
		return root;
	}

	/**Creates a hierarchy and returns a list of top elements.
	 * @return list of top elements
	 */
	public List<T> get() {
		if (elements == null) return Collections.emptyList();

		ArrayList<T> tops = new ArrayList<T>();
		for (T e: elements) {
			boolean top = instruction.atTop != null && instruction.atTop.test(e);
			if (top) {
				if (!tops.contains(e))
					tops.add(e);
			} else {
				Object parentID = instruction.getParentKey.apply(e);
				T parent = parentID !=null ? index.get(parentID) : null;
				if (parent == null) {
					if (!tops.contains(e))
						tops.add(e);
				} else {
					add(parent, e);
				}
			}
		}
		return tops;
	}

	/**Creates and returns a Hierarchy of objects.<br />
	 * @return Hierarchy a Hierarchy
	 */
	public Hierarchy<T> build() {
		return build(Hierarchy::new);
	}

	/**Creates and returns a Hierarchy of objects.<br />
	 * The Hierarchy is an instance of the class that the factory supplies.
	 * @param factory supplier of a Hierarchy
	 * @return Hierarchy
	 */
	public <H extends Hierarchy<T>> H build(Supplier<H> factory) {
		List<T> tops = get();
		H h = factory.get();
		h.setElements(elements).setIndex(index).setTops(tops);
		return h;
	}

	/**Clears up the HierarchyBuilder to the initial state.
	 */
	public void clear() {
		if (index != null)
			index.clear();
		if (instruction != null)
			instruction.clear();
		elements = null;
	}

	/**Is a set of steps for a HierarchyBuilder to perform in Hierarchy construction.
	 * <p>With an Instruction, you specify how to
	 * <ul><li>{@link #getKey(Function) Get the key of an object}</li>
	 * 	   <li>{@link #getParentKey(Function) Get the key of an object's parent}</li>
	 * 	   <li>{@link #addChild(BiConsumer) Add an object to other object} as its child</li>
	 * 	   <li>{@link #atTop(Predicate) Determine whether an object is a top element} of the hierarchy</li>
	 * </ul>
	 * </p>
	 * @param <T> an element type
	 */
	public static class Instruction<T> {
		private Function<T, Object>
			getKey,
			getParentKey;
		private BiConsumer<T, T> addChild;
		private Predicate<T> atTop;

		/**Creates a new Instruction.
		 */
		public Instruction() {
			clear();
		}

		/**Sets a function that returns the key of an object.<br />
		 * The key may or may not be the actual ID of the object.<br />
		 * It is good enough as long as a HierarchyBuilder uses it to identify an object.
		 * @param func function that returns the key of an object
		 * @return this Instruction
		 */
		public Instruction<T> getKey(Function<T, Object> func) {
			this.getKey = func;
			return this;
		}

		/**Sets a function that returns the key of an object's parent.<br />
		 * The parent key returned may be found from the keys obtained from the {@link #getKey(Function)} method.
		 * @param func function that returns the key of an object's parent
		 * @return this Instruction
		 */
		public Instruction<T> getParentKey(Function<T, Object> func) {
			this.getParentKey = func;
			return this;
		}

		/**Sets a consumer that adds a child object to a parent object
		 * @param consumer consumer that adds a child object to a parent object.<br />
		 * 				   the first argument is for the parent, and the second for the child.
		 * @return this Instruction
		 */
		public Instruction<T> addChild(BiConsumer<T, T> consumer) {
			this.addChild = consumer;
			return this;
		}

		/**Sets a predicate to determine whether an object is a top element of a hierarchy or not.
		 * A HierarchyBuilder treats an object as a top element
		 * if the test evaluates to be true or it cannot find the object's parent.
		 * @param test predicate to determine whether an object is a top element of a hierarchy or not
		 * @return this Instruction
		 */
		public Instruction<T> atTop(Predicate<T> test) {
			this.atTop = test;
			return this;
		}

		/**Clears the steps to the initial state.
		 * @return this Instruction
		 */
		public Instruction<T> clear() {
			getKey = t -> "";
			getParentKey = t -> "";
			addChild = (t1, t2) -> {};
			atTop = t -> {
				Object parentKey = getParentKey.apply(t);
				return isEmpty(parentKey) || parentKey.equals(getKey.apply(t));
			};
			return this;
		}
	}
}