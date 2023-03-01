/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.data.hierarchy;

import java.util.Collections;
import java.util.function.Function;

import horizon.base.AbstractComponent;

/**Utility to convert objects of a hierarchy to string representation.
 * <p>For a Stringify to convert objects of a hierarchy, you should
 * <ul><li>Set an {@link #setInstruction(Instruction) Instruction} for steps to convert the objects</li>
 * 	   <li>Call {@link #get(Iterable)} with the objects as an argument</li>
 * </ul>
 * </p>
 * <p>An {@link Instruction} is a set of steps for a Stringify to perform the conversion.<br />
 * For convenience, a Stringify has methods that sets the steps to an internal instruction.
 * </p>
 */
class Stringify<T> extends AbstractComponent {
	/**A functional interface that converts an object to a string
	 * @param <T> an object type
	 */
	@FunctionalInterface
	public interface ToString<T> {
		/**Converts t to a string.
		 * @param t		an object
		 * @param level	0-based level or depth of t in a hierarchy
		 * @param index	0-based index of t
		 * @return string representation of t
		 */
		String get(T t, int level, int index);
	}

	private Instruction<T> instruction;

	/**Returns indentation with s appended n times.
	 * @param s indentation string
	 * @param n number of times to append indentation string
	 * @return indentation string appended n time
	 */
	public static String indent(String s, int n) {
		String result = "";
		for (int i = 0; i < n; ++i) {
			result += s;
		}
		return result;
	}

	private Instruction<T> instruction() {
		return instruction != null ? instruction : (instruction = new Instruction<>());
	}

	/**Sets an instruction to convert objects to string representation.
	 * @param instruction instruction to convert objects to string representation
	 * @return this Stringify
	 */
	public Stringify<T> setInstruction(Instruction<T> instruction) {
		this.instruction = instruction;
		return this;
	}

	/**See {@link Instruction#beginElement(ToString)}
	 * @return this Stringify
	 */
	public Stringify<T> beginElement(ToString<T> func) {
		instruction().beginElement(func);
		return this;
	}

	/**See {@link Instruction#endElement(ToString)}
	 * @return this Stringify
	 */
	public Stringify<T> endElement(ToString<T> func) {
		instruction().endElement(func);
		return this;
	}

	/**See {@link Instruction#beginChildren(ToString)}
	 * @return this Stringify
	 */
	public Stringify<T> beginChildren(ToString<T> func) {
		instruction().beginChildren(func);
		return this;
	}

	/**See {@link Instruction#endChildren(ToString)}
	 * @return this Stringify
	 */
	public Stringify<T> endChildren(ToString<T> func) {
		instruction().endChildren(func);
		return this;
	}

	/**See {@link Instruction#getChildren(Function)}
	 * @return this Stringify
	 */
	public Stringify<T> getChildren(Function<T, Iterable<T>> func) {
		instruction().getChildren(func);
		return this;
	}

	/**Converts elements to string representation.
	 * @param elements objects of a hierarchy
	 * @return string representation of the elements
	 */
	public String get(Iterable<T> elements) {
		if (isEmpty(elements)) return "";

		StringBuilder buff = new StringBuilder();
		toString(elements, buff, 0);
		return buff.toString();
	}

	private void toString(Iterable<T> elements, StringBuilder buff, int level) {
		Instruction<T> instr = instruction();
		int index = 0;

		for (T e: elements) {
			buff.append(instr.beginElement().get(e, level, index));

			Iterable<T> children = instr.getChildren().apply(e);
			int sublevel = level + 1;
			if (!isEmpty(children)) {
				buff.append(instr.beginChildren().get(e, sublevel, index));
				toString(children, buff, sublevel);
				buff.append(instr.endChildren().get(e, sublevel, index));
			}
			buff.append(instr.endElement().get(e, level, index));
			++index;
		}
	}

	/**Is a set of steps to convert objects of a hierarchy to string representation.
	 * <p>With an Instruction, you specify how to
	 * <ul><li>{@link #beginElement(ToString) Mark the beginning of an element} with a string</li>
	 * 	   <li>{@link #endElement(ToString) Mark the end of an element} with a string</li>
	 * 	   <li>{@link #getChildren(Function) Get child elements} of an element</li>
	 * 	   <li>{@link #beginChildren(ToString) Mark the beginning of child elements} with a string</li>
	 * 	   <li>{@link #endChildren(ToString) Mark the end of child elements} with a string</li>
	 * </ul>
	 * </p>
	 * @param <T> an object type
	 */
	public static class Instruction<T> {
		private ToString<T>
			beginElement,
			endElement,
			beginChildren,
			endChildren;
		private Function<T, Iterable<T>> getChildren;

		/**Creates a new Instruction.
		 */
		public Instruction() {
			clear();
		}

		ToString<T> beginElement() {
			return beginElement;
		}

		/**Sets a function that returns a string to mark the beginning of an element.
		 * @param func function that returns a string to mark the beginning of an element
		 * @return this Instruction
		 */
		public Instruction<T> beginElement(ToString<T> func) {
			this.beginElement = func;
			return this;
		}

		ToString<T> endElement() {
			return endElement;
		}

		/**Sets a function that returns a string to mark the end of an element.
		 * @param func function that returns a string to mark the end of an element
		 * @return this Instruction
		 */
		public Instruction<T> endElement(ToString<T> func) {
			this.endElement = func;
			return this;
		}

		ToString<T> beginChildren() {
			return beginChildren;
		}

		/**Sets a function that returns a string to mark the beginning of child elements.
		 * @param func function that returns a string to mark the beginning of child elements
		 * @return this Instruction
		 */
		public Instruction<T> beginChildren(ToString<T> func) {
			this.beginChildren = func;
			return this;
		}

		ToString<T> endChildren() {
			return endChildren;
		}

		/**Sets a function that returns a string to mark the end of child elements.
		 * @param func function that returns a string to mark the end of child elements
		 * @return this Instruction
		 */
		public Instruction<T> endChildren(ToString<T> func) {
			this.endChildren = func;
			return this;
		}

		Function<T, Iterable<T>> getChildren() {
			return getChildren;
		}

		/**Sets a function that returns child elements of the current element.
		 * @param getChildren function that returns child elements of the current element
		 * @return this Instruction
		 */
		public Instruction<T> getChildren(Function<T, Iterable<T>> getChildren) {
			this.getChildren = getChildren;
			return this;
		}

		/**Clears the Instruction to the initial state.
		 * @return this Instruction
		 */
		public Instruction<T> clear() {
			ToString<T> empty = (e, level, index) -> "";
			beginElement = empty;
			endElement = empty;
			beginChildren = empty;
			endChildren = empty;
			getChildren = t -> Collections.emptyList();
			return this;
		}
	}
}
