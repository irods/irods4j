package org.irods.irods4j.authentication;

import org.irods.irods4j.input_validation.Preconditions;

/**
 * This class is a container meant for storing a password string.
 *
 * @since 0.8.0
 */
public final class Password {

	String value;

	/**
	 * Constructs a new Password instance.
	 *
	 * @param value The password string.
	 *
	 * @since 0.8.0
	 */
	public Password(String value) {
		Preconditions.notNull(value, "String is null");
		this.value = value;
	}

	/**
	 * Return the password string.
	 *
	 * @since 0.8.0
	 */
	public String getValue() {
		return value;
	}

}
