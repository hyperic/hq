package org.hyperic.util.http;

public class HttpConfig {
	int connectionTimeout;
	int socketTimeout;
	String proxyHostname;
	int proxyPort;
	
	public HttpConfig() {
	}
	
	public HttpConfig(int connectionTimeout, int socketTimeout, String proxyHostname, int proxyPort) {
		this.connectionTimeout = connectionTimeout;
		this.proxyHostname = proxyHostname;
		this.proxyPort = proxyPort;
		this.socketTimeout = socketTimeout;
	}
	
	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	public int getSocketTimeout() {
		return socketTimeout;
	}
	
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	
	public String getProxyHostname() {
		return proxyHostname;
	}
	
	public void setProxyHostname(String proxyHostname) {
		this.proxyHostname = proxyHostname;
	}
	
	public int getProxyPort() {
		return proxyPort;
	}
	
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
}