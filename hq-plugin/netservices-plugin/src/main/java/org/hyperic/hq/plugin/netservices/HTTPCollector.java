/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.plugin.netservices;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;
import org.springframework.util.StringUtils;

public class HTTPCollector extends SocketChecker {
	private boolean isPingCompat;
	private String url;
	private String method;
	private String hosthdr;
	private String useragent;
	private Pattern pattern;
	private List<String> matches = new ArrayList<String>();
	private String proxyHost = null;
	private int proxyPort = 8080;
	private Log log;
    private String postargs;

	protected void init() throws PluginException {
		super.init();
		log = LogFactory.getLog(HTTPCollector.class);
		Properties props = getProperties();

		boolean isSSL = isSSL();

		String protocol = props.getProperty(PROP_PROTOCOL,
				isSSL ? PROTOCOL_HTTPS : PROTOCOL_HTTP);

		// back compat w/ old url.availability templates
		this.isPingCompat = protocol.equals("ping");

		if (this.isPingCompat) {
			return;
		}

		this.method = props.getProperty(PROP_METHOD, METHOD_HEAD);
		this.hosthdr = props.getProperty("hostheader");
		this.postargs = props.getProperty("postargs");
		
		try {
			URL url = new URL(protocol, getHostname(), getPort(), getPath());
		
			this.url = url.toString();
		} catch (MalformedURLException e) {
			throw new PluginException(e);
		}

		this.useragent = getPlugin().getManagerProperty("http.useragent");

		if (this.useragent == null || this.useragent.trim().length() == 0) {
			this.useragent = "Hyperic-HQ-Agent/" + ProductProperties.getVersion();
		}

		// for log_track
		setSource(this.url);

		// to allow self-signed server certs
		if (isSSL) {
			// Try to get grab and accept the certificate
			try {
				getSocketWrapper(true);
			} catch (IOException e) {
				log.warn(e);
				// ...log it but probably going to be a problem later...
			}
		}

		String pattern = props.getProperty("pattern");
		
		if (pattern != null) {
			this.pattern = Pattern.compile(pattern);
		}

		String proxy = props.getProperty("proxy");

		if (proxy != null) {
			setSource(getSource() + " [via " + proxy + "]");
			
			int ix = proxy.indexOf(':');
			
			if (ix != -1) {
				this.proxyPort = Integer.parseInt(proxy.substring(ix + 1));
				proxy = proxy.substring(0, ix);
			}
			
			this.proxyHost = proxy;
		}

        collect();
        if (getLogLevel() == LogTrackPlugin.LOGLEVEL_ERROR) {
            throw new PluginException(getMessage());
        }
	}

	protected String getURL() {
		return this.url;
	}

	protected void setURL(String url) {
		this.url = url;
	}

	protected String getMethod() {
		return this.method;
	}

	protected void setMethod(String method) {
		this.method = method;
	}

	private double getAvail(int code) {
		// There are too many options to list everything that is
		// successful. So, instead we are going to call out the
		// things that should be considered failure, everything else
		// is OK.
		switch (code) {
		case HttpURLConnection.HTTP_BAD_REQUEST:
		case HttpURLConnection.HTTP_FORBIDDEN:
		case HttpURLConnection.HTTP_NOT_FOUND:
		case HttpURLConnection.HTTP_BAD_METHOD:
		case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
		case HttpURLConnection.HTTP_CONFLICT:
		case HttpURLConnection.HTTP_PRECON_FAILED:
		case HttpURLConnection.HTTP_ENTITY_TOO_LARGE:
		case HttpURLConnection.HTTP_REQ_TOO_LONG:
		case HttpURLConnection.HTTP_INTERNAL_ERROR:
		case HttpURLConnection.HTTP_NOT_IMPLEMENTED:
		case HttpURLConnection.HTTP_UNAVAILABLE:
		case HttpURLConnection.HTTP_VERSION:
		case HttpURLConnection.HTTP_BAD_GATEWAY:
		case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
			return Metric.AVAIL_DOWN;
		default:
		}

		if (hasCredentials()) {
			if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
				return Metric.AVAIL_DOWN;
			}
		}

		return Metric.AVAIL_UP;
	}

	// allow response to have metrics, must be:
	// Content-Type: text/plain
	// Content-Length: <= 8192   DRC: Why the limitation?
	// XXX flag to always disable and/or change these checks
	protected void parseResults(HttpResponse response) {
		Header length = response.getFirstHeader("Content-Length");
		Header type = response.getFirstHeader("Content-Type");

		if (type == null || !type.getValue().equals("text/plain")) {
			return;
		}

		if (length != null) {
			try {
				if (Integer.parseInt(length.getValue()) > 8192) {
					return;
				}
			} catch (NumberFormatException e) {
				return;
			}
		}

		try {
			parseResults(EntityUtils.toString(response.getEntity(), "UTF-8"));
		} catch (ParseException e) {
			setErrorMessage("Exception parsing response: " + e.getMessage(), e);
		} catch (IOException e) {
			setErrorMessage("Exception reading response stream: " + e.getMessage(), e);
		}
	}

	private boolean matchResponse(HttpResponse response) {
		String body;
		
		try {
			body = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (ParseException e) {
			setErrorMessage("Exception parsing response: " + e.getMessage(), e);
			
			return false;
		} catch (IOException e) {
			setErrorMessage("Exception reading response stream: " + e.getMessage(), e);
			
			return false;
		}

		if (body == null) {
			body = "";
		}

		try {
			Matcher matcher = this.pattern.matcher(body);
			boolean matches = false;

			while (matcher.find()) {
				matches = true;
				
				int count = matcher.groupCount();
				// skip group(0):
				// "Group zero denotes the entire pattern by convention"
				for (int i = 1; i <= count; i++) {
					this.matches.add(matcher.group(i));
				}
			}
			
			if (matches) {
				return true;
			}
			
			setWarningMessage("Response (length=" + body.length() + ") does not match " + this.pattern);
		} catch (Exception e) {
			setErrorMessage("Exception matching response: " + e.getMessage(), e);
		}
		return false;
	}

	public void collect() {
		if (this.isPingCompat) {
			// back compat w/ old url.availability templates
			super.collect();
			
			return;
		}

		this.matches.clear();
		
		HttpConfig config = new HttpConfig(getTimeoutMillis(), getTimeoutMillis(), proxyHost, proxyPort);

		AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig();
        log.debug("isAcceptUnverifiedCert:"+keystoreConfig.isAcceptUnverifiedCert());

		HttpClient client = new HQHttpClient (keystoreConfig, config, keystoreConfig.isAcceptUnverifiedCert());
		HttpParams params = client.getParams();
		
		params.setParameter(CoreProtocolPNames.USER_AGENT, this.useragent);
		
		if (this.hosthdr != null) {
			params.setParameter(ClientPNames.VIRTUAL_HOST, this.hosthdr);
		}

		HttpRequestBase request;
                if (getMethod().equals(HttpHead.METHOD_NAME)) {
                    request = new HttpHead(getURL());
                } else if (getMethod().equals(HttpPost.METHOD_NAME)) {
                    try {
                        HttpPost httpPost = new HttpPost(getURL());
                        if(postargs!=null){
                            httpPost.setEntity(preparePostArgs(postargs));
                        }
                        request = httpPost;
                    } catch (UnsupportedEncodingException ex) {
                        log.debug(ex,ex);
			setErrorMessage(ex.getLocalizedMessage());
                        setAvailability(Metric.AVAIL_DOWN);
                        return;
                    }
                } else {
                    request = new HttpGet(getURL());
                }

		request.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, isFollow());

		if (hasCredentials()) {
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(getUsername(), getPassword());
			String realm = getProperties().getProperty("realm", "");

			if (realm.length() == 0) {
				// send header w/o challenge
				boolean isProxied = (StringUtils.hasText(proxyHost) && proxyPort != -1);
				Header authenticationHeader = BasicScheme.authenticate(credentials, "UTF-8", isProxied);

				request.addHeader(authenticationHeader);
			} else {
				String authenticationHost = (this.hosthdr == null) ? getHostname() : this.hosthdr;
				AuthScope authScope = new AuthScope(authenticationHost, -1, realm);
				
				((DefaultHttpClient) client).getCredentialsProvider().setCredentials(authScope, credentials);

				request.getParams().setParameter(ClientPNames.HANDLE_AUTHENTICATION, true);
			}
		}

		double avail;
		
		try {
			startTime();
			
			HttpResponse response = client.execute(request);
			
			endTime();

			int statusCode = response.getStatusLine().getStatusCode();
			
			setResponseCode(statusCode);

			avail = getAvail(statusCode);

			String msg = String.valueOf(statusCode);
			
			Header header = response.getFirstHeader("Server");
			
			if (header != null) {
				msg += " (" + header.getValue() + ")";
			}

			long lastModified = 0;
			
			header = response.getFirstHeader("Last-Modified");

			if (header != null) {
				try {
					DateFormat format = new SimpleDateFormat();
					
					// TODO lock down the expected format (wasn't specified in orig code...
					lastModified = format.parse(header.getValue()).getTime();
				} catch (Exception e) {
					// TODO probably should do something a bit more useful here...
				}
			} else if (statusCode == 200) {
				lastModified = System.currentTimeMillis();
			}

			if (lastModified != 0) {
				setValue("LastModified", lastModified);
			}

			if (!getMethod().equals(HttpHead.METHOD_NAME) && (avail == Metric.AVAIL_UP)) {
				if (this.pattern != null) {
					if (!matchResponse(response)) {
						avail = Metric.AVAIL_WARN;
					} else if (matches.size() != 0) {
						msg += " match results=" + this.matches;
					}
				} else {
					parseResults(response);
				}
			}

			if (avail == Metric.AVAIL_UP) {
				if (this.matches.size() != 0) {
					setInfoMessage(msg);
				} else {
					setDebugMessage(msg);
				}
			} else if (avail == Metric.AVAIL_WARN) {
				setWarningMessage(msg);
			} else {
				setErrorMessage(msg);
			}
		} catch (IOException e) {
			avail = Metric.AVAIL_DOWN;
			setErrorMessage(e.toString());
		}

		setAvailability(avail);

		netstat();
	}

    private HttpEntity preparePostArgs(String args) throws UnsupportedEncodingException {
        HttpEntity res;
        if (args.contains("=")) {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            String[] argslist = args.split("&");
            for (int i = 0; i < argslist.length; i++) {
                String[] arg = argslist[i].split("=");
                if (arg.length == 2) {
                    nameValuePairs.add(new BasicNameValuePair(arg[0], arg[1]));
                } else {
                    nameValuePairs.add(new BasicNameValuePair(arg[0], ""));
                }
            }
            log.debug("[preparePostArgs] args="+nameValuePairs);
            res = new UrlEncodedFormEntity(nameValuePairs);
        } else {
            res = new StringEntity(args);
        }
        return res;
    }
}
