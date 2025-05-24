package org.irods.irods4j.common;

/**
 * Provides utilities for version-related operations.
 *
 * @since 0.2.0
 */
public class Versioning {

	/**
	 * Compares iRODS version strings.
	 * <p>
	 * Version strings MUST follow the pattern MAJOR.MINOR.PATCH.
	 *
	 * @param v1 The first version string to compare.
	 * @param v2 The second version string to compare.
	 * @return The value {@code 0} if {@code v1 == v2};
	 * a value less than {@code 0} if {@code v1 < v2};
	 * and a value greater than {@code 0} if {@code v1 > v2}.
	 * @since 0.2.0
	 */
	public static int compareVersions(String v1, String v2) {
		String[] parts1 = v1.split("\\.");
		String[] parts2 = v2.split("\\.");

		int length = Math.max(parts1.length, parts2.length);
		for (int i = 0; i < length; ++i) {
			int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
			int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
			if (num1 != num2) {
				return Integer.compare(num1, num2);
			}
		}

		return 0;
	}

}
