package org.irods.irods4j.input_validation;

public class Preconditions {

	/**
	 * Checks if the object is null.
	 * 
	 * @param o       The object to check for null.
	 * @param message The precondition violation message.
	 * 
	 * @throws PreconditionViolationException If the object is null.
	 * 
	 * @since 0.1.0
	 */
	public static void notNull(Object o, String message) {
		if (null == o) {
			throw new PreconditionViolationException(message);
		}
	}

	/**
	 * Checks if a string is null or empty.
	 * 
	 * @param s       The string to check.
	 * @param message The precondition violation message.
	 * 
	 * @throws PreconditionViolationException If the object is null.
	 * 
	 * @since 0.1.0
	 */
	public static void notNullOrEmpty(String s, String message) {
		if (null == s || s.isEmpty()) {
			throw new PreconditionViolationException(message);
		}
	}

	/**
	 * Checks if an integer is greater than or equal to a threshold.
	 * 
	 * @param value     The integer to test.
	 * @param threshold The integer to test against.
	 * @param message   The precondition violation message.
	 * 
	 * @throws PreconditionViolationException If the object is null.
	 * 
	 * @since 0.1.0
	 */
	public static void greaterThanOrEqualToValue(long value, long threshold, String message) {
		if (value < threshold) {
			throw new PreconditionViolationException(message);
		}
	}

}
