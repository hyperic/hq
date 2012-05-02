package org.hyperic.hq.caf;

import java.util.ArrayList;
import java.util.Collection;

public class CafResponse {

	private String clientIdStr;
	private String requestIdStr;
	private String pmeIdStr;
	private String errorMessage;
	private final Collection<String>providerStdout = new ArrayList<String>();
	private final Collection<String>providerStderr = new ArrayList<String>();
	
	
	public String getClientIdStr() {
		return clientIdStr;
	}
	
	public void setClientIdStr(String clientIdStr) {
		this.clientIdStr = clientIdStr;
	}
	
	public String getRequestIdStr() {
		return requestIdStr;
	}
	
	public void setRequestIdStr(String requestIdStr) {
		this.requestIdStr = requestIdStr;
	}
	
	public String getPmeIdStr() {
		return pmeIdStr;
	}
	
	public void setPmeIdStr(String pmeIdStr) {
		this.pmeIdStr = pmeIdStr;
	}
	
	public Collection<String> getProviderStdout() {
		return providerStdout;
	}
	
	public Collection<String> getProviderStderr() {
		return providerStderr;
	}
	
	public void addToStdOut(String line) {
		providerStdout.add(line);
	}
	
	public void addToStdErr(String line) {
		providerStderr.add(line);
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	@Override
	public String toString() {
		if (null != errorMessage) {
			return "Error executing CAF request - " +  errorMessage;
		}
		String toString = "Results for request '" + requestIdStr + "' - \n";
		toString += "Provider stdOut : \n";
		for (String line : providerStdout) {
			toString += line + "\n";
		}
		toString += "Provider stdErr : \n";
		for (String line : providerStderr) {
			toString += line + "\n";
		}
		return toString;
	}

	
}
