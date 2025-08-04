package org.irods.irods4j.high_level.vfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides support methods for operating on iRODS logical paths.
 * <p>The methods exposed by this class assume paths are non-null.</p>
 *
 * @since 0.3.0
 */
public final class LogicalPath {

	private LogicalPath() {
	}

	/**
	 * Returns whether the logical path represents an absolute path.
	 *
	 * @param path The logical path to operate on.
	 * @since 0.3.0
	 */
	public static boolean isAbsolute(String path) {
		return path.startsWith("/");
	}

	/**
	 * Returns the parent path of the given logical path.
	 * <p>Returns null if the path is relative or no forward slash is found.</p>
	 *
	 * @param path The logical path to operate on.
	 * @since 0.3.0
	 */
	public static String parentPath(String path) {
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash <= 0) {
			return isAbsolute(path) ? "/" : null;
		}
		return path.substring(0, lastSlash);
	}

	/**
	 * Returns the farthest path element of the given logical path.
	 * <p>Returns the given path if no forward slash is found.</p>
	 *
	 * @param path The logical path to operate on.
	 * @since 0.3.0
	 */
	public static String objectName(String path) {
		int lastSlash = path.lastIndexOf('/');
		return (lastSlash == -1) ? path : path.substring(lastSlash + 1);
	}

	/**
	 * Returns the list of segments which make up the logical path.
	 *
	 * @param path The logical path to operate on.
	 * @since 0.3.0
	 */
	public static List<String> segments(String path) {
		if ("/".equals(path)) {
			List<String> parts = new ArrayList<>();
			parts.add("/");
			return parts;
		}

		List<String> parts = Arrays.stream(path.split("/")).collect(Collectors.toList());

		if (isAbsolute(path)) {
			parts.set(0, "/");
		}

		if (path.endsWith("/")) {
			parts.add("");
		}

		return parts;
	}

}
