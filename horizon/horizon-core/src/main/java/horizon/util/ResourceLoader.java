/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.util;

import java.io.FileInputStream;
import java.io.InputStream;

import horizon.base.AbstractComponent;

/**Utility to load a resource on the system's classpath and/or file path.
 */
public class ResourceLoader extends AbstractComponent {

	/**Finds the resource of the given path either from classpath or from file path.
	 * @param path	path to the resource
	 * @return
	 * <ul><li>InputStream for the resource</li>
	 * 	   <li>null if not found</li>
	 * </ul>
	 */
	public static final InputStream find(String path) {
		InputStream res = ifEmpty(findFromFilepath(path), () -> findFromClasspath(path));
		if (res == null)
			log(ResourceLoader.class).debug(() -> path + " not found");
		return res;
	}

	/**Loads the resource of the given path either from classpath or from file path.
	 * @param path	path to the resource
	 * @return InputStream for the resource
	 * @throws RuntimeException if not found
	 */
	public static final InputStream load(String path) {
		InputStream res = find(path);
		if (res == null)
			throw new RuntimeException("Failed to load " + path);
		return res;
	}

	/**Finds the resource of the given path from file path.
	 * @param path path to the resource
	 * @return
	 * <ul><li>InputStream for the resource</li>
	 * 	   <li>null if not found</li>
	 * </ul>
	 */
	public static final InputStream findFromFilepath(String path) {
		try {
			InputStream res = new FileInputStream(path);
			log(ResourceLoader.class).debug(() -> path + " is loaded from file system");
			return res;
		} catch (Exception e) {
			return null;
		}
	}

	/**Finds the resource of the given path from classpath.
	 * @param path path to the resource
	 * @return
	 * <ul><li>InputStream for the resource</li>
	 * 	   <li>null if not found</li>
	 * </ul>
	 */
	public static final InputStream findFromClasspath(String path) {
		InputStream res = getResourceStream(Thread.currentThread().getContextClassLoader(), path);
		if (res == null)
			res = getResourceStream(ResourceLoader.class.getClassLoader(), path);
		if (res == null)
			res = getResourceStream(ClassLoader.getSystemClassLoader(), path);
		/*
		  if (res == null) res =
		  getResourceStream(ClassLoader.getPlatformClassLoader(), path);
		 */
		if (res != null)
			log(ResourceLoader.class).debug(() -> path + " is loaded from classpath");
		return res;
	}

	private static InputStream getResourceStream(ClassLoader classLoader, String path) {
		return classLoader != null ? classLoader.getResourceAsStream(path) : null;
	}
/*
	private static String getDir(String pattern) {
		if (isEmpty(pattern)) return pattern;

		int pos = pattern.lastIndexOf("/");
		if (pos < 0)
			pos = pattern.lastIndexOf("\\");
		if (pos < 0)
			return pattern;
		else
			return pattern.substring(0, pos);
	}

	public static List<String> pathsOf(String pathPattern) {
		String dir = getDir(pathPattern);
		InputStream in = find(dir);
		if (in == null)
			return Collections.emptyList();

		if (dir.equals(pathPattern))
			pathPattern += "/*.*";
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pathPattern);
		ArrayList<String> result = new ArrayList<>();
		try (Scanner scanner = new Scanner(in);) {
			while (scanner.hasNext()) {
				String path = dir + "/" + scanner.nextLine();
				Path p = Paths.get(path);
				if (matcher.matches(p))
					result.add(path);
			}
			return result;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}
*/
	private ResourceLoader() {}
}