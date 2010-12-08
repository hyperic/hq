package org.hyperic.hq.api;

public class ErrorRepresentation {
	private String exception;
	private String message;
	
	public ErrorRepresentation(String exception, String message) {
		this.exception = exception;
		this.message = message;
	}
	
	public ErrorRepresentation(Exception e) {
		this.exception = e.getClass().getName();
		this.message = e.getMessage();
	}

	public String getException() {
		return exception;
	}

	public String getMessage() {
		return message;
	}
}

