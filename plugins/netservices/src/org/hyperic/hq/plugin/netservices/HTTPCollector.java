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
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.security.UntrustedSSLProtocolSocketFactory;

public class HTTPCollector extends SocketChecker {

    private boolean isPingCompat;
    private String url;
    private String method;
    private String hosthdr;
    private Pattern pattern;
    private ArrayList matches = new ArrayList();
    private String proxyHost = null;
    private int proxyPort = 8080;

    protected void init() throws PluginException {
        super.init();
        Properties props = getProperties();

        boolean isSSL = isSSL();

        String protocol =
            props.getProperty(PROP_PROTOCOL,
                              isSSL ? PROTOCOL_HTTPS : PROTOCOL_HTTP);

        //back compat w/ old url.availability templates
        this.isPingCompat =
            protocol.equals("ping");

        if (this.isPingCompat) {
            return;
        }

        this.method =
            props.getProperty(PROP_METHOD, METHOD_HEAD);

        this.hosthdr =
            props.getProperty("hostheader");

        this.url = 
            protocol + "://" + getHostname() + ":" + getPort() + getPath();

        //for log_track
        setSource(this.url);

        //to allow self-signed server certs
        if (isSSL) {
            UntrustedSSLProtocolSocketFactory.register();
        }

        String pattern = props.getProperty("pattern");
        if (pattern != null) {
            this.pattern = Pattern.compile(pattern);
            this.method = METHOD_GET;
        }

        String proxy = props.getProperty("proxy");

        if (proxy != null) {
            setSource(getSource() + " [via " + proxy + "]");
            int ix = proxy.indexOf(':');
            if (ix != -1) {
                this.proxyPort =
                    Integer.parseInt(proxy.substring(ix+1));
                proxy = proxy.substring(0, ix);
            }
            this.proxyHost = proxy;
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

    private double getAvail(int code)  {
        // There are too many options to list everything that is
        // successful.  So, instead we are going to call out the
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

    //allow response to have metrics, must be:
    //Content-Type: text/plain
    //Content-Length: <= 8192
    //XXX flag to always disable and/or change these checks
    protected void parseResults(HttpMethod method) {
        Header length =
            method.getResponseHeader("Content-Length");
        Header type =
            method.getResponseHeader("Content-Type");

        if (type == null) {
            return;
        }

        if (!type.getValue().equals("text/plain")) {
            return;
        }

        if (length == null) {
            return;
        }

        try {
            if (Integer.parseInt(length.getValue()) > 8192) {
                return;
            }
        } catch (NumberFormatException e) {
            return;
        }

        try {
            String body = method.getResponseBodyAsString();
            parseResults(body);
        } catch (Exception e) { 
            //throws IOException in commons-httpclient/3.0, nada in 2.0
            setErrorMessage("Exception parsing response: " +
                            e.getMessage());
        }
    }

    private boolean matchResponse(HttpMethod method) {
        String body;
        try {
            body = method.getResponseBodyAsString();
        } catch (Exception e) {
            setErrorMessage("Exception getting response body: " +
                            e.getMessage());
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
                //skip group(0):
                //"Group zero denotes the entire pattern by convention"
                for (int i=1; i<=count; i++) {
                    this.matches.add(matcher.group(i));
                }
            }
            if (matches) {
                return true;
            }
            setWarningMessage("Response (length=" + body.length() +
                              ") does not match " + this.pattern);
        } catch (Exception e) {
            setErrorMessage("Exception matching response: " +
                            e.getMessage(), e);
        }
        return false;
    }

    public void collect() {
        if (this.isPingCompat) {
            //back compat w/ old url.availability templates
            super.collect();
            return;
        }

        this.matches.clear();
        HttpMethod method;
        boolean isHEAD =
            getMethod().equals(METHOD_HEAD);
        HttpClient client = new HttpClient();
        HttpClientParams params = new HttpClientParams();
        String userAgent = "Hyperic-HQ-Agent/" + ProductProperties.getVersion();
        params.setParameter(HttpMethodParams.USER_AGENT, userAgent);
        client.setParams(params);
        client.setConnectionTimeout(getTimeoutMillis());
        client.setTimeout(getTimeoutMillis());

        if (this.proxyHost != null) {
            client.getHostConfiguration().setProxy(this.proxyHost,
                                                   this.proxyPort);
        }

        if (isHEAD) {
            HeadMethod head = new HeadMethod(getURL());
            head.setBodyCheckTimeout(-1);
            method = head;
        }
        else {
            GetMethod get = new GetMethod(getURL());
            method = get;
        }        

        method.setFollowRedirects(isFollow());

        if (hasCredentials()) {
            UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(getUsername(),
                                                getPassword());
            String realm = getProperties().getProperty("realm", "");

            if (realm.length() == 0) {
                //send header w/o challenge
                String auth =
                    BasicScheme.authenticate(credentials,
                                             method.getParams().getCredentialCharset());

                method.addRequestHeader(new Header("Authorization", auth));
            }
            else {
                client.getState().setCredentials(realm,
                                                 getHostname(),
                                                 credentials);

                method.setDoAuthentication(true);
            }
        }

        if (this.hosthdr != null) {
            method.setRequestHeader("Host", this.hosthdr);
        }

        double avail;
        int rc;
        
        try {
            startTime();
            rc = client.executeMethod(method);
            endTime();

            setResponseCode(rc);

            avail = getAvail(rc);

            String msg =
                method.getStatusCode() +
                " " +
                method.getStatusText();

            Header header =
                method.getResponseHeader("Server");
            if (header != null) {
                msg += " (" + header.getValue() + ")";
            }

            long lastModified = 0;
            header =
                method.getResponseHeader("Last-Modified");

            if (header != null) {
                try {
                    lastModified =
                        Date.parse(header.getValue());
                } catch (Exception e) {
                    
                }
            }
            else if (rc == 200) {
                lastModified = System.currentTimeMillis();
            }

            if (lastModified != 0) {
                setValue("LastModified", lastModified);
            }

            if (!isHEAD && (avail == Metric.AVAIL_UP)) {
                if (this.pattern != null) {
                    if (!matchResponse(method)) {
                        avail = Metric.AVAIL_WARN;
                    }
                    else if (matches.size() != 0) {
                        msg += " match results=" + this.matches;
                    }
                }
                else {
                    parseResults(method);
                }
            }

            if (avail == Metric.AVAIL_UP) {
                if (this.matches.size() != 0) {
                    setInfoMessage(msg);
                }
                else {
                    setDebugMessage(msg);
                }
            }
            else if (avail == Metric.AVAIL_WARN) {
                setWarningMessage(msg);
            }
            else {
                setErrorMessage(msg);
            }
        } catch (IOException e) {
            avail = Metric.AVAIL_DOWN;
            setErrorMessage(e.toString());
        } finally {
            method.releaseConnection();
        }

        setAvailability(avail);
        
        netstat();
    }
}
