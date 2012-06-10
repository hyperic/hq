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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;

public class TestHttpConduit extends HTTPConduit { 
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
			throw new RuntimeException(t) ; 
		}//EO catch block 
	}//EOM 
	
	@Override
	public void prepare(Message message) throws IOException {
		this.delegate.prepare(message) ; 
		final HttpURLConnection connection = (HttpURLConnection) message.get(KEY_HTTP_CONNECTION) ; 
		message.put(KEY_HTTP_CONNECTION, new TestHttpURLConnection(connection, (String)message.get("Content-Type"))) ; 
	}//EOM 
	
	@Override
	public void close(Message msg) throws IOException {
		
		WebResponse response = null ;
		InputStream inputStream  = null ; 
		
		final Exchange exchange = msg.getExchange();
		Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);
        
		try{ 
			final URL url = (URL) this.setupURLMethod.invoke(this.delegate, msg) ; 
			final String sURL = url.toString() ;
			
			final String httpRequestMethod = (String)msg.get(Message.HTTP_REQUEST_METHOD);        
			WebRequest req = null ; 
			if(httpRequestMethod.equals("GET")) { 
				req = new GetMethodWebRequest(sURL) ;
			}else { 
				req = new PostMethodWebRequest(sURL) ; 
			}//EO if post 
			
			req.setHeaderField("Content-Type", (String)msg.get("Content-Type")) ;
			req.setHeaderField("Accept", (String)msg.get("Content-Type")) ;
			response = serviceRunner.newClient().getResponse(req);
			System.out.println(response.getText());
			//this is the response 
			inputStream = response.getInputStream() ; 
			
			//TODO: NYI
            //new Headers(inMessage).readFromConnection(connection);
            final String ct = response.getContentType() ;
            
            //final HttpURLConnection connection =  (HttpURLConnection) msg.get(KEY_HTTP_CONNECTION) ;
            
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
			exchange.put(Exception.class, t);  
		}//EO catch block 
		
		inMessage.setContent(InputStream.class, inputStream);
        incomingObserver.onMessage(inMessage);
	}//EOM 
	
}//EOC 
