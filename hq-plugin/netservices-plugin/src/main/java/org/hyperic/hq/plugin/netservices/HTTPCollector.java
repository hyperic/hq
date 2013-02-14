/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], VMware, Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
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
    private static final Log log = LogFactory.getLog(HTTPCollector.class);
    private AtomicBoolean isPingCompat = new AtomicBoolean();
    private AtomicReference<String> url = new AtomicReference<String>();
    private AtomicReference<String> method = new AtomicReference<String>();
    private AtomicReference<String> hosthdr = new AtomicReference<String>();
    private AtomicReference<String> useragent = new AtomicReference<String>();
    private AtomicReference<Pattern> pattern = new AtomicReference<Pattern>();
    private List<String> matches = new ArrayList<String>();
    private AtomicReference<String> proxyHost = new AtomicReference<String>();
    private AtomicInteger proxyPort = new AtomicInteger(8080);
    private AtomicReference<Map<String, String>> params = new AtomicReference<Map<String,String>>();

    protected void init() throws PluginException {
        super.init();
        Properties props = getProperties();
        boolean isSSL = isSSL();
        String protocol = props.getProperty(PROP_PROTOCOL, isSSL ? PROTOCOL_HTTPS : PROTOCOL_HTTP);
        // back compat w/ old url.availability templates
        isPingCompat.set(protocol.equals("ping"));
        if (isPingCompat.get()) {
            return;
        }
        method.set(props.getProperty(PROP_METHOD, METHOD_HEAD));
        hosthdr.set(props.getProperty("hostheader"));
        setParams(props);

        try {
            URL url = new URL(protocol, getHostname(), getPort(), getPath());
            this.url.set(url.toString());
        } catch (MalformedURLException e) {
            throw new PluginException(e);
        }

        useragent.set(getPlugin().getManagerProperty("http.useragent"));

        if (useragent.get() == null || useragent.get().trim().length() == 0) {
            useragent.set("Hyperic-HQ-Agent/" + ProductProperties.getVersion());
        }

        // for log_track
        setSource(url.get());

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
            this.pattern.set(Pattern.compile(pattern));
        }

        String proxy = props.getProperty("proxy");

        if (proxy != null) {
            setSource(getSource() + " [via " + proxy + "]");

            int ix = proxy.indexOf(':');

            if (ix != -1) {
                this.proxyPort.set(Integer.parseInt(proxy.substring(ix + 1)));
                proxy = proxy.substring(0, ix);
            }

            this.proxyHost.set(proxy);
        }

        collect();
        if (getLogLevel() == LogTrackPlugin.LOGLEVEL_ERROR) {
            throw new PluginException(getMessage());
        }
    }

    protected String getURL() {
        return url.get();
    }

    protected void setURL(String url) {
        this.url.set(url);
    }

    protected String getMethod() {
        return method.get();
    }

    protected void setMethod(String method) {
        this.method.set(method);
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
            body = body == null ? "" : body;
            if (log.isDebugEnabled()) {
                log.debug("attempting to match pattern=" + pattern.get() + " against response body=" + body);
            }
        } catch (ParseException e) {
            setErrorMessage("Exception parsing response: " + e, e);
            return false;
        } catch (IOException e) {
            setErrorMessage("Exception reading response stream: " + e, e);
            return false;
        }
        Matcher matcher = this.pattern.get().matcher(body);
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
            if (log.isDebugEnabled()) {
                log.debug("pattern='" + pattern.get() + "' matches");
            }
            return true;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("pattern='" + pattern.get() + "' does not match");
            }
            setWarningMessage("Response (length=" + body.length() + ") does not match " + pattern.get());
            return false;
        }
    }

    public void collect() {
        if (isPingCompat.get()) {
            // back compat w/ old url.availability templates
            super.collect();
            return;
        }
        this.matches.clear();
        HttpConfig config = new HttpConfig(getTimeoutMillis(), getTimeoutMillis(), proxyHost.get(), proxyPort.get());
        AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig();
        log.debug("isAcceptUnverifiedCert:"+keystoreConfig.isAcceptUnverifiedCert());
        HQHttpClient client = new HQHttpClient (keystoreConfig, config, keystoreConfig.isAcceptUnverifiedCert());
        HttpParams params = client.getParams();
        params.setParameter(CoreProtocolPNames.USER_AGENT, useragent.get());
        if (this.hosthdr != null) {
            params.setParameter(ClientPNames.VIRTUAL_HOST, this.hosthdr);
        }
        HttpRequestBase request;
        double avail = 0;
        try {
            if (getMethod().equals(HttpHead.METHOD_NAME)) {
                request = new HttpHead(getURL());
                addParams(request, this.params.get());
            } else if (getMethod().equals(HttpPost.METHOD_NAME)) {
                HttpPost httpPost = new HttpPost(getURL());
                request = httpPost;
                addParams(httpPost, this.params.get());
            } else {
                request = new HttpGet(getURL());
                addParams(request, this.params.get());
            }
            request.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, isFollow());
            addCredentials(request, client);
            startTime();
            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            int tries = 0;
            while (statusCode == HttpURLConnection.HTTP_MOVED_TEMP && tries < 3) {
                tries++;
                Header header = response.getFirstHeader("Location");
                String url = header.getValue();
                String[] toks = url.split(";");
                String[] t = toks.length > 1 ? toks[1].split("\\?") : new String[0];
                response = getRedirect(toks[0], t.length > 1 ? t[1] : "");
                statusCode = response.getStatusLine().getStatusCode();
            }
            endTime();
            setResponseCode(statusCode);
            avail = getAvail(statusCode);
            StringBuilder msg = new StringBuilder(String.valueOf(statusCode));
            Header header = response.getFirstHeader("Server");
            msg.append(header != null ? " (" + header.getValue() + ")" : "");
            setLastModified(response, statusCode);
            avail = checkPattern(response, avail, msg);
            setAvailMsg(avail, msg);
        } catch (UnsupportedEncodingException e) {
            log.error("unsupported encoding: " + e, e);
        } catch (IOException e) {
            avail = Metric.AVAIL_DOWN;
            setErrorMessage(e.toString());
        } finally {
            setAvailability(avail);
        }
        netstat();
    }

    private void setAvailMsg(double avail, StringBuilder msg) {
        if (avail == Metric.AVAIL_UP) {
            if (this.matches.size() != 0) {
                setInfoMessage(msg.toString());
            } else {
                setDebugMessage(msg.toString());
            }
        } else if (avail == Metric.AVAIL_WARN) {
            setWarningMessage(msg.toString());
        } else {
            setErrorMessage(msg.toString());
        }
    }

    private void setLastModified(HttpMessage response, int statusCode) {
        long lastModified = 0;
        Header header = response.getFirstHeader("Last-Modified");
        if (header != null) {
            try {
                DateFormat format = new SimpleDateFormat();
                // TODO lock down the expected format (wasn't specified in orig code...
                lastModified = format.parse(header.getValue()).getTime();
            } catch (java.text.ParseException e) {
                log.error(e, e);
            }
        } else if (statusCode == 200) {
            lastModified = System.currentTimeMillis();
        }
        if (lastModified != 0) {
            setValue("LastModified", lastModified);
        }
    }

    private double checkPattern(HttpResponse response, double avail, StringBuilder msg) {
        if (!getMethod().equals(HttpHead.METHOD_NAME) && (avail == Metric.AVAIL_UP)) {
            if (pattern.get() != null) {
                if (!matchResponse(response)) {
                    avail = Metric.AVAIL_WARN;
                } else if (matches.size() != 0) {
                    msg.append(" match results=").append(matches);
                }
            } else {
                parseResults(response);
            }
        }
        return avail;
    }
    
    private Map<String, String> getArgs(String args) {
        Map<String, String> params = new HashMap<String, String>();
        String[] toks = args.split("&");
        for (String t : toks) {
            String[] pair = t.split("=");
            if (pair.length < 2) {
                continue;
            }
            params.put(pair[0], pair[1]);
        }
        return params;
    }

    private HttpResponse getRedirect(String url, String args) throws ClientProtocolException, IOException {
        HttpConfig config = new HttpConfig(getTimeoutMillis(), getTimeoutMillis(), proxyHost.get(), proxyPort.get());
        AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig();
        HQHttpClient client = new HQHttpClient (keystoreConfig, config, keystoreConfig.isAcceptUnverifiedCert());
        HttpParams params = client.getParams();
        params.setParameter(CoreProtocolPNames.USER_AGENT, useragent.get());
        if (this.hosthdr != null) {
            params.setParameter(ClientPNames.VIRTUAL_HOST, this.hosthdr);
        }
        HttpRequestBase request;
        request = new HttpGet(url);
        addParams(request, getArgs(args));
        addCredentials(request, client);
        return client.execute(request);
    }
    
    private void addCredentials(HttpMessage request, DefaultHttpClient client) {
        if (hasCredentials()) {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(getUsername(), getPassword());
            String realm = getProperties().getProperty("realm", "");
            if (realm.length() == 0) {
                // send header w/o challenge
                boolean isProxied = (StringUtils.hasText(proxyHost.get()) && proxyPort.get() != -1);
                Header authenticationHeader = BasicScheme.authenticate(credentials, "UTF-8", isProxied);
                request.addHeader(authenticationHeader);
            } else {
                String authenticationHost = (hosthdr.get() == null) ? getHostname() : hosthdr.get();
                AuthScope authScope = new AuthScope(authenticationHost, -1, realm);
                ((DefaultHttpClient) client).getCredentialsProvider().setCredentials(authScope, credentials);
                request.getParams().setParameter(ClientPNames.HANDLE_AUTHENTICATION, true);
            }
        }
    }

    private void setParams(Properties props) {
        String secretparams = props.getProperty("secretrequestparams", "");
        String params = props.getProperty("requestparams", "") + "," + secretparams;
        params = params.trim();
        if (params.length() == 0 || params.equals(",")) {
            this.params.set(null);
            return;
        }
        this.params.set(new HashMap<String, String>());
        String[] toks = params.split(",");
        for (String tok : toks) {
            String[] pair = tok.split("=");
            if (pair.length != 2) {
                log.warn("specified params do not match the proper pattern: " + tok);
                continue;
            }
            this.params.get().put(pair[0], pair[1]);
        }
    }

    private void addParams(HttpRequestBase request, Map<String, String> params)
    throws UnsupportedEncodingException {
        if (params != null && !params.isEmpty()) {
            BasicHttpParams prms = new BasicHttpParams();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                prms.setParameter(entry.getKey(), entry.getValue());
            }
            request.setParams(prms);
        }
    }

    private void addParams(HttpEntityEnclosingRequestBase request, Map<String, String> params)
    throws UnsupportedEncodingException {
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            BasicHttpParams prms = new BasicHttpParams();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                postParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                prms.setParameter(entry.getKey(), entry.getValue());
            }
            request.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));
            request.setParams(prms);
        }
    }

}
