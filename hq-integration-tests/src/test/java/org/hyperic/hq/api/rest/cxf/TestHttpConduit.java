/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
 *
 */ 
package org.hyperic.hq.api.rest.cxf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.systest.servlet.GetMethodQueryWebRequest;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.HttpInternalErrorException;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.MessageBodyWebRequest.InputStreamMessageBody;
import com.meterware.servletunit.ServletRunner;

public class TestHttpConduit extends HTTPConduit { 
	
	private static final String CONTENT_TYPE_HEADER = "Content-Type" ; 
	private HTTPConduit delegate ; 
	private Method setupURLMethod  ; 
	private ServletRunner serviceRunner ; 
	
	public TestHttpConduit(final Bus bus, final HTTPConduit delegate, final EndpointInfo endpointInfo,
            final EndpointReferenceType target, final ServletRunner serviceRunner) throws IOException{
		super(bus, endpointInfo, target) ; 
		this.delegate = delegate ;
		this.serviceRunner = serviceRunner ; 
		
		try{ 
			this.setupURLMethod = this.delegate.getClass().getDeclaredMethod("setupURL", Message.class) ; 
			this.setupURLMethod.setAccessible(true) ; 
		}catch(Throwable t){ 
			throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ; 
		}//EO catch block 
	}//EOM 
	
	@Override
	public void prepare(Message message) throws IOException {
		//this.delegate.prepare(message) ; 
		//final HttpURLConnection connection = (HttpURLConnection) message.get(KEY_HTTP_CONNECTION) ;
		try{ 
			final URL url = (URL) this.setupURLMethod.invoke(this.delegate, message) ; 
			//final String sURL = url.toString() ;
			
			message.put(KEY_HTTP_CONNECTION, new TestHttpURLConnection(url, (String)message.get(CONTENT_TYPE_HEADER))) ;
			
			message.setContent(OutputStream.class, new ByteArrayOutputStream()) ; 
		}catch(Throwable t) { 
			throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;
		}//EO catch block 
		
	}//EOM 
	
	@Override
	public void close(Message msg) throws IOException {
		WebResponse response = null ;
		InputStream inputStream  = null ; 
		
		final Exchange exchange = msg.getExchange();
		Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);
        
        ByteArrayOutputStream messagePayload = null ;
        final HttpURLConnection urlConnection = (HttpURLConnection) msg.get(KEY_HTTP_CONNECTION) ; 
        final String sURL = urlConnection.getURL().toString();
		try{ 
			 
			final String httpRequestMethod = (String)msg.get(Message.HTTP_REQUEST_METHOD);        
			WebRequest req = null ; 
			
			if(httpRequestMethod.equals("GET")) { 
				req = new GetMethodQueryWebRequest(sURL) ;
			}else if(httpRequestMethod.equals("PUT")) { 
				messagePayload = (ByteArrayOutputStream) msg.getContent(java.io.OutputStream.class) ;
				messagePayload.flush() ; 
				final ByteArrayInputStream bis = new ByteArrayInputStream(messagePayload.toByteArray()) ;   
				req = new PutMethodWebRequest(sURL, bis, (String) msg.get(CONTENT_TYPE_HEADER)) ;
            }else if(httpRequestMethod.equals("POST")) { 
                messagePayload = (ByteArrayOutputStream) msg.getContent(java.io.OutputStream.class) ;
                messagePayload.flush() ; 
                final ByteArrayInputStream bis = new ByteArrayInputStream(messagePayload.toByteArray()) ;   
                String urlString ="", queryString = "";
                int pos = sURL.indexOf('?');
                if (pos>0) {
                    urlString = sURL.substring(0,pos);
                    queryString = sURL.substring(pos + 1);
                } else {
                    urlString = sURL;
                }
                req = new PostMethodWebRequestThatWorks(urlString, queryString, bis, (String) msg.get(CONTENT_TYPE_HEADER));
			}else{ 
				throw new UnsupportedOperationException(httpRequestMethod + " is unsupported!") ; 
			}//EO if post 
			req.setHeaderField(CONTENT_TYPE_HEADER, (String)msg.get(CONTENT_TYPE_HEADER)) ;
			req.setHeaderField("Accept", (String)msg.get(CONTENT_TYPE_HEADER)) ;
			
			//extract the headers map from the msg (stored against the Message.PROTOCOL_HEADERS) 
			final MultivaluedMap<String,String> protocolHeadersMap = (MultivaluedMap<String,String>) msg.get(org.apache.cxf.message.Message.PROTOCOL_HEADERS) ;
			if(protocolHeadersMap != null) { 
				final String authorizationToken = protocolHeadersMap.getFirst(HttpHeaders.AUTHORIZATION) ;
				if(authorizationToken != null) { 
					req.setHeaderField(HttpHeaders.AUTHORIZATION, authorizationToken)  ;
				}//EO if authorizationToken was not null
			}//EO if the protocol headers were defined 
			
			response = serviceRunner.newClient().getResponse(req);
			System.out.println(response.getText());
			//this is the response 
			inputStream = response.getInputStream() ; 
			
			//TODO: NYI
            //new Headers(inMessage).readFromConnection(connection);
            final String ct = response.getContentType() ;
            
            inMessage.put(Message.RESPONSE_CODE, response.getResponseCode());
            exchange.put(Message.RESPONSE_CODE, response.getResponseCode());
            inMessage.put(Message.CONTENT_TYPE, ct) ; 
            String charset = HttpHeaderHelper.findCharset(ct);
            String normalizedEncoding = HttpHeaderHelper.mapCharset(charset);
           
            if (normalizedEncoding == null) {
            	
            	final Logger LOG = LogUtils.getL7dLogger(HTTPConduit.class);
                String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG",
                		LogUtils.getL7dLogger(HTTPConduit.class), charset).toString();
                LOG.log(Level.WARNING, m);
                throw new IOException(m);   
            }
            
            inMessage.put(Message.ENCODING, normalizedEncoding);
            
            //TODO: NYI
            //cookies.readFromConnection(connection);
		}catch(Exception t) {
		    HttpException httpe = null  ; 
		    if(t instanceof HttpException) httpe = (HttpException) t;  
		    else { 
		        httpe = new HttpInternalErrorException(new URL(sURL), t) ;
		    }//EO if not instance of http exception 
		    
		    exchange.put(Message.RESPONSE_CODE, httpe.getResponseCode());
		    ((TestHttpURLConnection)urlConnection).setError(t) ; 
		}finally{ 
			if(messagePayload != null) messagePayload.close() ;
		}//EO catch block 
		
		inMessage.setContent(InputStream.class, inputStream);
        incomingObserver.onMessage(inMessage);
	}//EOM 
	
}//EOC 


class PostMethodWebRequestThatWorks extends PostMethodWebRequest {
    private String queryString;
    
    public PostMethodWebRequestThatWorks( String urlString, String queryString, InputStream source, String contentType) {    
        super(urlString, source, contentType );
        this.queryString = queryString;
    }

    public String getQueryString() {
        return this.queryString;
    }                    
}

