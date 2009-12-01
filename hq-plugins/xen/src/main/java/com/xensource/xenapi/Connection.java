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
         * true if the connection is to the Rio edition of XenServer. Certain function calls are not allowed.
         */
        public Boolean rioConnection=false;
	
	private String sessionReference;    //  the opaque reference to the session used by this connection
	private final XmlRpcClient client;  //  as seen by the xmlrpc library. From our point of view it's a server

	/**
	 * Create a connection to a particular server using a given username and password. This object
	 * can then be passed in to any other API calls.
         *
         * To login to a miami box we call login_with_password(username, password, "1.2")
         * on rio this call fails and we should use login_with_password(username,password) instead, and note that we are talking to a rio host
         * so that we can refuse to make certain miami-specific calls
	 */
	public Connection (String client, String username, String password) 
		throws java.net.MalformedURLException, org.apache.xmlrpc.XmlRpcException, Types.BadServerResponse, Types.SessionAuthenticationFailed
	{
	        final String ApiVersion = "1.2";
		this.client = getClientFromURL(client);
                try{
                    //first try to login the modern way
		    this.sessionReference = loginWithPassword(this.client, username, password, ApiVersion);
                } catch (Types.BadServerResponse e) {
                    //oops, something went wrong
                    Object[] errDesc = (Object[]) e.response.get("ErrorDescription");
                    //was the problem that the host was running rio? If so it will have complained that it got three parameters
                    //instead of two. Let us carefully verify the details of this complaint
                    if (   (0 == ((String) errDesc[0]).compareTo("MESSAGE_PARAMETER_COUNT_MISMATCH"))
                        && (0 == ((String) errDesc[1]).compareTo("session.login_with_password"))
                        && (0 == ((String) errDesc[2]).compareTo("2"))
                        && (0 == ((String) errDesc[3]).compareTo("3")) ) {
                        //and if so, we can have another go, using the older login method, and see how that goes.
                        this.sessionReference = loginWithPassword(this.client, username, password);       
                        //success!. Note that we are talking to an old host on this connection
                        this.rioConnection=true;
                    } else {
                        //Hmm... Can't solve this here. Let upstairs know about the problem.
                        throw e;
                    }
                }
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

 
	private static String loginWithPassword(XmlRpcClient client, String username, String password, String ApiVersion) throws
		Types.BadServerResponse,
		XmlRpcException,
		Types.SessionAuthenticationFailed
	{
		String method_call = "session.login_with_password";
		Object[] method_params = {Marshalling.toXMLRPC(username), Marshalling.toXMLRPC(password), Marshalling.toXMLRPC(ApiVersion)};
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
