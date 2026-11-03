package org.irods.irods4j.authentication;

import org.irods.irods4j.input_validation.Preconditions;

/**
 * This class is a container meant for storing a session token string.
 *
 * @since 0.8.0
 */
public final class SessionToken {

	String value;

	/**
	 * Constructs a new SessionToken instance.
	 *
	 * @param value The session token string.
	 *
	 * @since 0.8.0
	 */
	public SessionToken(String value) {
		Preconditions.notNull(value, "String is null");
		this.value = value;
	}

	/**
	 * Return the session token string.
	 *
	 * @since 0.8.0
	 */
	public String getValue() {
		return value;
	}

}
