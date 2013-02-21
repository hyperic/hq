package org.hyperic.hq.notifications;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.hyperic.hq.common.SystemException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class HttpEndpoint extends NotificationEndpoint {

    private static final Log log = LogFactory.getLog(HttpEndpoint.class);
    private static final String DEFAULT_CONTENT_TYPE = "application/xml";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private HttpHost targetHost;
    private DefaultHttpClient client;
    private BasicHttpContext localcontext;
    private Integer port;
    private String hostname;
    private String scheme;
    private String username;
    private String password;
    private String method;
    private String contentType;
    private String encoding;
    private URL url;

    public HttpEndpoint(long registrationId) {
        super(registrationId);
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("scheme=").append(scheme)
            .append(" hostname=").append(hostname)
            .append(" port=").append(port)
            .append(" url=").append(url)
            .append(" method=").append(method)
            .toString();
    }

/* XXX remove!
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof HttpEndpoint) {
            HttpEndpoint hep = (HttpEndpoint) o;
            return  hep.port.equals(port) &&
                    hep.url.equals(url) &&
                    hep.hostname.equals(hostname) &&
                    hep.scheme.equals(scheme) &&
                    hep.method.equals(method);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode != null) {
            return hashCode;
        }
        hashCode = 7 + (7*hostname.hashCode()) + (7*port.hashCode()) + (7*method.hashCode()) + (7*scheme.hashCode())
                     + (7*url.hashCode());
        return hashCode;
    }

    private int getInt(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) {
            throw new SystemException(key + " cannot be null for http endpoint");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new SystemException(key + " must be numeric for http endpoint", e);
        }
    }
*/

    @Override
    public void setValues(Map<String, String> values) {
        try {
            final String urlbuf = getString(values, "url");
            final URL url = new URL(urlbuf);
            hostname = url.getHost();
            scheme = url.getProtocol();
            port = url.getPort();
            username = getString(values, "username");
            password = getString(values, "password");
            method = getString(values, "method");
            contentType = getString(values, "content-type", DEFAULT_CONTENT_TYPE);
            encoding = getString(values, "encoding", DEFAULT_ENCODING);
        } catch (MalformedURLException e) {
            throw new SystemException(e);
        }
    }

    private String getString(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        if (value == null && defaultValue == null) {
            throw new SystemException(key + " cannot be null for http endpoint");
        } else if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private String getString(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) {
            throw new SystemException(key + " cannot be null for http endpoint");
        }
        return value;
    }

    @Override
    public void init() {
        targetHost = new HttpHost(hostname, port, scheme);
        AuthScope scope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
        client.getCredentialsProvider().setCredentials(scope, creds);
        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);
        localcontext = new BasicHttpContext();
        localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
    }

    @Override
    public void publishMessages(List<String> messages) {
        if (method.equalsIgnoreCase("post")) {
            post(messages);
        } else {
            throw new SystemException("method=" + method + " not supported");
        }
    }

    private void post(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        final boolean debug = log.isDebugEnabled();
        final HttpPost post = new HttpPost(url.getPath());
        final SAXBuilder builder = new SAXBuilder();
        final XMLOutputter out = new XMLOutputter();
        out.setFormat(Format.getCompactFormat());
        final Document document = new Document();
        try {
            final Element rootElement = new Element("notificationReport");
            document.setRootElement(rootElement);
            Integer numElements = 0;
            for (final String message : messages) {
                if (messages == null) {
                    continue;
                }
                numElements++;
                final Document d = builder.build(new StringReader(message));
                final Element root = d.getRootElement();
                document.addContent(root);
            }
            rootElement.setAttribute("numMessages", numElements.toString());
            String body = out.outputString(document);
            final HttpEntity entity = new StringEntity(body, contentType, encoding);
            post.setEntity(entity);
            if (debug) log.debug(post.getRequestLine());
            final HttpResponse resp = client.execute(targetHost, post, localcontext);
            if (debug) {
                String respBuf= EntityUtils.toString(resp.getEntity());
                log.debug(resp.getStatusLine() + ", response=[" + respBuf + "]");
            }
        } catch (JDOMException e) {
            throw new SystemException(e);
        } catch (IOException e) {
// XXX probably want to retry here
            throw new SystemException(e);
        }
    }

}
