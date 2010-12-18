package org.hyperic.hq.api.representation;

public class ErrorResponse {
	private String exception;
	private String message;
	
	public ErrorResponse(String exception, String message) {
		this.exception = exception;
		this.message = message;
	}
	
	public ErrorResponse(Exception e) {
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

