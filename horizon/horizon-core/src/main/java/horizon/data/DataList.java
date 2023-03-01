/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.data;

import java.util.ArrayList;

import horizon.base.Assert;
/**A list of elements with information for pagination.
 * <p>You can use the DataList
 * when you have a large set of elements and want to provide them in small chunks.<br />
 * If, for example, you have a query result of thousands of rows
 * and want to provide it 20 rows a time, you can use a DataList.</p>
 * <p>For this to work, you should get a chunk from all the elements yourself and populate a DataList with it.<br />
 * Then set the DataList with
 * <ul><li>{@link #setTotalSize(Number) total size} of the original elements</li>
 * 	   <li>{@link #setFetchSize(int) fetch size}, or the number of elements that are fetched at once</li>
 * 	   <li>{@link #setStart(int) start} index of the elements the DataList actually contains</li>
 * </ul>
 * <p>In traversing the elements, however, you should use 0-based index local to the DataList.</p>
 * <p>A DataList provides information whether it has {@link #hasMore() more elements}
 * to fetch {@link #hasPrevious() backwardly} or {@link #hasNext() forwardly}.</p>
 *
 * @param <E> element type
 */
public class DataList<E> extends ArrayList<E> {
	private static final long serialVersionUID = 1L;

	public static Fetch getFetch(Number totalSize, int start, int fetchSize) {
		return new Fetch(totalSize.intValue(), start, fetchSize);
	}

	private int
		fetchSize,
		totalSize,
		start;

	/**Returns a fetch size, or the number of elements that are fetched at once.
	 * @return fetch size
	 */
	public int getFetchSize() {
		return fetchSize;
	}

	/**Sets the fetch size, or the number of elements that are fetched at once.
	 * @param size fetch size
	 * @return the DataList
	 */
	public <T extends DataList<E>> T  setFetchSize(int size) {
		if (size < 0)
			throw new IllegalArgumentException("fetchSize < 0");
		this.fetchSize = size;
		return self();
	}

	/**Returns the number of all elements the list should contain.
	 * @return number of all elements the list should contain
	 */
	public int getTotalSize() {
		return isEmpty() ? 0 :
			   totalSize < 1 ? size() : totalSize;
	}

	/**Sets the number of all elements the list should contain.
	 * @param totalSize number of all elements the list should contain
	 * @return the DataList
	 */
	public <T extends DataList<E>> T setTotalSize(Number totalSize) {
		this.totalSize = Assert.notEmpty(totalSize, "totalSize").intValue();
		return self();
	}

	/**Returns the start index of the list's elements
	 * @return 0-based index
	 */
	public int getStart() {return isEmpty() ? -1 : start;}

	/**Sets the start index of the elements the list actually contains.
	 * @param start	0-based index
	 * @return the DataList
	 */
	public <T extends DataList<E>> T setStart(int start) {
		this.start = totalSize < 1 ? -1 : start;
		return self();
	}

	/**Returns the end index of the list's elements
	 * @return 0-based index
	 */
	public int getEnd() {
		return isEmpty() ? -1 : start + size() - 1;
	}

	/**Returns whether the list has more elements to fetch.
	 * @return
	 * <ul><li>true if the list has more elements to fetch.</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean hasMore() {
		return !isEmpty() && size() < getTotalSize();
	}

	/**Returns whether the list has more elements to fetch backwardly.
	 * @return
	 * <ul><li>true if the list has more elements to fetch backwardly.</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean hasPrevious() {
		return hasMore() && start > 0;
	}

	/**Returns whether the list has more elements to fetch forwardly.
	 * @return
	 * <ul><li>true if the list has more elements to fetch forwardly.</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean hasNext() {
		return hasMore() && getEnd() < getTotalSize() - 1;
	}

	@Override
	public boolean add(E e) {
		if (e == null || contains(e)) return false;

		boolean result = super.add(e);
		++totalSize;
		return result;
	}

	@Override
	public void add(int index, E e) {
		super.add(index, e);
		++totalSize;
	}

	@Override
	public E remove(int index) {
		E e = super.remove(index);
		if (e != null)
			--totalSize;
		return e;
	}

	@Override
	public void clear() {
		super.clear();
		totalSize  = 0;
		start = -1;
	}

	/**Initializes this list.
	 * @return the DataList
	 */
	public <T extends DataList<E>> T init() {
		totalSize = size();
		return self();
	}

	@SuppressWarnings("unchecked")
	protected <T extends DataList<E>> T self() {
		DataList<E> obj = this;
		return (T)obj;
	}

	/**Utility to help calculate numbers for pagination*/
	public static class Fetch {
		public static final int
			ALL = 0,
			NONE = -1;

		private int
			totalSize,
			start,
			fetchSize;

		private Fetch(int totalSize, int start, int fetchSize) {
			this.totalSize = totalSize;
			this.start = totalSize > 0 ? start : -1;
			this.fetchSize = fetchSize;
		}

		/**Sets the fetch info to the list
		 * @param <T> a DataList type
		 * @param list a DataList
		 * @return the list
		 */
		public <T extends DataList<?>> T set(T list) {
			if (list != null) {
				list.setTotalSize(totalSize)
				.setStart(start)
				.setFetchSize(fetchSize);
			}
			return list;
		}

		/**Returns the number of fetches needed to provide all elements.
		 * @param elementCount	number of all elements
		 * @param size			size or number of elements in a fetch
		 * @return number of fetches needed to provide all elements
		 */
		static final int count(int elementCount, int size) {
			if (elementCount == 0 || size == ALL) return 1;
			return (elementCount / size) + ((elementCount % size) == 0 ? 0 : 1);
		}
		/**Returns the end index of the elements starting from the start index.
		 * @param elementCount	number of all elements
		 * @param size			size or number of elements in a fetch
		 * @param start 		0-based start index
		 * @return 0-based end index of the
		 */
		static final int end(int elementCount, int size, int start) {
			if (size < ALL)
				throw new IllegalArgumentException("Invalid size: " + size);
			if (elementCount < 0)
				throw new IllegalArgumentException("Invalid elementCount: " + elementCount);

			int last = elementCount - 1;
			if (size == ALL) return last;
			return Math.min(last, start + size -1);
		}
		/**Returns the page index.
		 * @param current start index of the items
		 * @param count fetch count
		 * @return page index
		 */
		static final int page(int current, int count) {
			return count < 1 ? 0 : current / count;
		}
		/**Returns the band count.
		 * @param page page index
		 * @param visibleLinks number of visible links
		 * @return band count
		 */
		static final int band(int page, int visibleLinks) {
			return visibleLinks < 1 ? 0 : page / visibleLinks;
		}
	}
}