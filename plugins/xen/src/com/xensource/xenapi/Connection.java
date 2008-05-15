/*
 *============================================================================
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of version 2.1 of the GNU Lesser General Public
 * License as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *============================================================================
 * Copyright (C) 2007 XenSource Inc.
 *============================================================================
 */
package com.xensource.xenapi;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class Connection {
  /**
   * The magic value that shall be used as the "version" parameter to
   * Session.loginWithPassword().
   */
	public static final String ApiVersion = "1.2";
	
	private String sessionReference;    //  the opaque reference to the session used by this connection
	private final XmlRpcClient client;  //  as seen by the xmlrpc library. From our point of view it's a server

	/**
	 * Create a connection to a particular server using a given username and password. This object
	 * can then be passed in to any other API calls.
	 */
	public Connection (String client, String username, String password) 
		throws java.net.MalformedURLException, org.apache.xmlrpc.XmlRpcException, Types.BadServerResponse, Types.SessionAuthenticationFailed
	{
		this.client = getClientFromURL(client);
		this.sessionReference = loginWithPassword(this.client, username, password);
	}
  
  protected void finalize()
    throws Throwable
  {
    dispose();
    super.finalize();
  }
  
  public void dispose()
		throws Types.BadServerResponse,
		       XmlRpcException
  {
    if (sessionReference != null)
    {
  		String method_call = "session.logout";
  		Object[] method_params = {Marshalling.toXMLRPC(this.sessionReference)};
  		Map response = (Map) client.execute(method_call, method_params);
  		sessionReference = null;
      if(response.get("Status").equals("Success")) {
  			return;
  		}
  		throw new Types.BadServerResponse(response);
  	}
  }
  
	private static String loginWithPassword(XmlRpcClient client, String username, String password) throws
		Types.BadServerResponse,
		XmlRpcException,
		Types.SessionAuthenticationFailed
	{
		String method_call = "session.login_with_password";
		//XXX 4.0.1 server throws MESSAGE_PARAMETER_COUNT_MISMATCH
		//Object[] method_params = {Marshalling.toXMLRPC(username), Marshalling.toXMLRPC(password), Marshalling.toXMLRPC(ApiVersion)};
		Object[] method_params = {Marshalling.toXMLRPC(username), Marshalling.toXMLRPC(password)};
		Map response = (Map) client.execute(method_call, method_params);
    if(response.get("Status").equals("Success")) {
			return (String) response.get("Value");
		} else if(response.get("Status").equals("Failure")) {
			Object[] error = (Object[]) response.get("ErrorDescription");
			if(error[0].equals("SESSION_AUTHENTICATION_FAILED")) {
				throw new Types.SessionAuthenticationFailed();
			}
		}
		throw new Types.BadServerResponse(response);
	}

	/*
	 * Annoying boilerplate.
	 */
	private XmlRpcClient getClientFromURL(String s) throws java.net.MalformedURLException
	{
		URL url = new URL(s);
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(url);
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		return client;
	}
	
	/*
	 * Because the binding calls are constructing their own parameter lists, they need to be able to get to 
	 * the session reference directly. This is all rather ugly and needs redone
	 */
	String getSessionReference(){
		return this.sessionReference;
	}
	
	/*
	 * Similarly, we allow (auto-generated parts of) the bindings direct access to the XML rpc calling mechanism.
	 */
  Map dispatch(String method_call, Object[] method_params) throws XmlRpcException {
    return (Map) client.execute(method_call, method_params);
  }
	
}
