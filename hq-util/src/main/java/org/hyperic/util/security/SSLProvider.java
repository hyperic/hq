package org.hyperic.util.security;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLSocketFactory;

public interface SSLProvider {
	public SSLContext getSSLContext();
	public SSLSocketFactory getSSLSocketFactory();
}

