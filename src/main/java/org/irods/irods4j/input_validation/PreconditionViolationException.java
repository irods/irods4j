package org.irods.irods4j.input_validation;

public class PreconditionViolationException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public PreconditionViolationException(String message) {
		super(message);
	}

}
