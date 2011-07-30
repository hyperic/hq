package org.hyperic.hq.plugin.netservices;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.util.security.DefaultSSLProviderImpl;

public class LDAPSSLSocketFactory extends SSLSocketFactory {
	private SSLSocketFactory socketFactory;
	
	public LDAPSSLSocketFactory() {
		// TODO need to make this configure at some point...
		DefaultSSLProviderImpl sslProvider = new DefaultSSLProviderImpl(new AgentKeystoreConfig(), true);
		
		socketFactory = sslProvider.getSSLContext().getSocketFactory();
	}

	public static SocketFactory getDefault(){
	    return new LDAPSSLSocketFactory();
	}

	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose)
			throws IOException {
		return socketFactory.createSocket(s, host, port, autoClose);
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return socketFactory.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return socketFactory.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
		return socketFactory.createSocket(address, port, localAddress, localPort);
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return socketFactory.createSocket(host, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
			throws IOException, UnknownHostException {
		return socketFactory.createSocket(host, port, localHost, localPort);
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException,
			UnknownHostException {
		return socketFactory.createSocket(host, port);
	}
}