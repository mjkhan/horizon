/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.base;

/**Is an operation using a resource and it may throw a Throwable.
 * @param <T> a resource type
 * @param <E> a Throwable type
 */
@FunctionalInterface
public interface Try<T, E extends Throwable> {
	/**Performs an operation using the resource.
	 * @param resource resource used to perform the operation
	 * @throws Throwable
	 */
	void attempt(T resource) throws E;
}