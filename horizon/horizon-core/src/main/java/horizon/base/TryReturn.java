/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.base;

/**Is an operation using a resource that returns a result and it may throw a Throwable.
 * @param <T> a resource type
 * @param <R> a result type
 * @param <E> a Throwable type
 */
@FunctionalInterface
public interface TryReturn<T, R, E extends Throwable> {
	/**Performs an operation using the resource and returns the result of it.
	 * @param resource	resource used to perform the operation
	 * @return result of the operation
	 * @throws Throwable
	 */
	R attempt(T resource) throws E;
}