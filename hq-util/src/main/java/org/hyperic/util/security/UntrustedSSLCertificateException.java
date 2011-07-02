package org.hyperic.util.security;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;

public class UntrustedSSLCertificateException extends SSLException {
	private static final long serialVersionUID = 1L;
	
	private X509Certificate[] certChain;
	
	public UntrustedSSLCertificateException(Throwable throwable, X509Certificate[] certChain) {
		super(throwable);
		
		this.certChain = certChain;
	}
	
	public UntrustedSSLCertificateException(String reason, X509Certificate[] certChain) {
		super(reason);
		
		this.certChain = certChain;
	}
	
	public UntrustedSSLCertificateException(String reason, Throwable throwable, X509Certificate[] certChain) {
		super(reason, throwable);
		
		this.certChain = certChain;
	}

	public X509Certificate[] getCertChain() {
		return certChain;
	}
}