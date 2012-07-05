/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.api.rest.cxf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestHttpURLConnection extends HttpURLConnection {

	private List<String> contentTypes ; 
	private HttpURLConnection delegate ; 
	private Throwable error ; 
	
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
		final Map<String, List<String>> headerFields = new HashMap<String,List<String>>() ;
		headerFields.put("content-type",  contentTypes);
		headerFields.put("Content-type",  contentTypes);
		return headerFields ; 
	}//EOM 

	@Override
	public boolean usingProxy() {
		return this.delegate.usingProxy() ; 
	}//EOC
	
	public final void setError(final Throwable t) { 
	    this.error = t ; 
	}//EOM 
	
	@Override
	public final InputStream getErrorStream() { 
	    try{ 
	        return this.exceptionToInputStream(this.error) ; 
	    }catch(IOException t) { 
	        throw new RuntimeException(t) ; 
	    }//EO catch block 
	}//EOM 
	
	private final InputStream exceptionToInputStream(final Throwable t) throws IOException{
	    if(this.error == null) return null ; 
	    final StringWriter sr = new StringWriter() ;  
	    try{ 
	        this.error.printStackTrace(new PrintWriter(sr)) ;
	    }finally{ 
	        sr.flush()  ;
            sr.close() ;
	    }//EO catch block 
	    
	    return new ByteArrayInputStream(sr.toString().getBytes()) ; 

	}//EOM 

}//EOC TestHttpURLConnection
