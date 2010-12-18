package org.hyperic.hq.api.representation;

public class SuccessResponse {
	private Object data;
	private String message;
	
	public SuccessResponse() {
		this(null, null);
	}
	
	public SuccessResponse(Object data) {
		this(data, null);
	}
	
	public SuccessResponse(Object data, String message) {
		this.data = data;
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public String getMessage() {
		return message;
	}
}