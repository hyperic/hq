package org.hyperic.hq.api.rest.cxf;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestHttpURLConnection extends HttpURLConnection {

	private List<String> contentTypes ; 
	private HttpURLConnection delegate ; 
	
	protected TestHttpURLConnection(HttpURLConnection delegate, final String contentType) {
		this(delegate.getURL(), contentType) ; 
		this.delegate = delegate  ;
	}//EOM
	
	protected TestHttpURLConnection(final URL url, final String contentType) {
		super(url); 
		this.contentTypes = Arrays.asList(new String[]{ contentType }) ; 
	}//EOM
	
	@Override
	public void connect() throws IOException {
		//DO nothing
		//this.delegate.connect() ; 
	}//EOM 
	
	@Override
	public void disconnect() {
		//DO nothing 
		//this.delegate.disconnect() ; 
	}//EOM 

	@Override
	public final Map<String, List<String>> getHeaderFields() {
//		final Map<String, List<String>> headerFields = new HashMap<String,List<String>>(this.delegate.getHeaderFields()) ;
		final Map<String, List<String>> headerFields = new HashMap<String,List<String>>() ;
		headerFields.put("content-type",  contentTypes);
		headerFields.put("Content-type",  contentTypes);
		return headerFields ; 
	}//EOM 

	@Override
	public boolean usingProxy() {
		return this.delegate.usingProxy() ; 
	}//EOC 
	
}//EOC TestHttpURLConnection
