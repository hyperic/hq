package org.hyperic.hq.api;

public class NotFoundException extends Exception {
	public NotFoundException() {
		super();
	}
	
	public NotFoundException(String msg) {
		super(msg);
	}
	
	public NotFoundException(Throwable t) {
		super(t);
	}
	
	public NotFoundException(String msg, Throwable t) {
		super(msg, t);
	}
}