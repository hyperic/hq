package org.hyperic.hq.notifications;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.security.ServerKeystoreConfig;
import org.hyperic.util.http.HQHttpClient;

public class HttpEndpoint extends NotificationEndpoint {

    private static final Log log = LogFactory.getLog(HttpEndpoint.class);
    private static final String DEFAULT_CONTENT_TYPE = "application/xml";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private Integer port;
    private String hostname;
    private String scheme;
    private String username;
    private String password;
    private String contentType;
    private String encoding;
    private URL url;
    private ServerKeystoreConfig keystoreConfig;

    public HttpEndpoint(long registrationId, String url, String username, String password, String contentType,
                        String encoding) {
        super(registrationId);
        keystoreConfig = Bootstrap.getBean(ServerKeystoreConfig.class);
        try {
            this.url = new URL(url);
            this.hostname = this.url.getHost();
            this.scheme = this.url.getProtocol();
            this.port = this.url.getPort();
            this.username = username;
            this.password = password;
            this.contentType = contentType == null ? DEFAULT_CONTENT_TYPE : contentType;
            this.encoding = encoding == null ? DEFAULT_ENCODING : encoding;
        } catch (MalformedURLException e) {
            throw new SystemException(e);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("[registrationId=").append(getRegistrationId())
            .append(",scheme=").append(scheme)
            .append(",hostname=").append(hostname)
            .append(",port=").append(port)
            .append(",url=").append(url)
            .append("]")
            .toString();
    }

    @Override
    public EndpointStatus publishMessagesInBatch(Collection<InternalAndExternalNotificationReports> messages, List<InternalNotificationReport> failedReports) {
        DefaultHttpClient client = null;
        try {
            if (scheme.equalsIgnoreCase("https")) {
                client = new HQHttpClient(keystoreConfig, null, true);
            } else {
                client = new DefaultHttpClient();
            }
            final HttpHost targetHost = new HttpHost(hostname, port, scheme);
            final AuthScope scope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
            final UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
            client.getCredentialsProvider().setCredentials(scope, creds);
            client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
            client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
            final AuthCache authCache = new BasicAuthCache();
            final BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);
            final BasicHttpContext localcontext = new BasicHttpContext();
            localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
            EndpointStatus batchPostingStatus = new EndpointStatus();
            for (final InternalAndExternalNotificationReports message : messages) {
                BasePostingStatus status =publishMessage(client, message.getExternalReport(), targetHost, localcontext);
                batchPostingStatus.add(status);
                if (!status.isSuccessful()) {
                    failedReports.add(message.getInternalReport());
                }
            }
            return batchPostingStatus;
        } finally {
            if (client != null) client.getConnectionManager().shutdown();
        }
    }

    private BasePostingStatus publishMessage(DefaultHttpClient client, String message, HttpHost targetHost,
                                BasicHttpContext localcontext) {
        final boolean debug = log.isDebugEnabled();
        final HttpPost post = new HttpPost(url.getPath() + "5");
        HttpEntity entity;
        long time = System.currentTimeMillis();
        try {
            entity = new StringEntity(message, contentType, encoding);
            post.setEntity(entity);
            if (debug) log.debug(post.getRequestLine());
            final HttpResponse resp = client.execute(targetHost, post, localcontext);
            HttpEntity httpRes = resp.getEntity();
            // The entire response stream must be read if another connection to the server is made with the current 
            // client object
            final String respBuf= EntityUtils.toString(httpRes);
            if (debug) {
                try {
                    log.debug(resp.getStatusLine() + ", response=[" + respBuf + "]");
                } catch (Exception e) {
                    log.debug(e,e);
                }
            }
            int status = resp.getStatusLine().getStatusCode();
            return new HTTPStatus(time,status);
        }catch(IOException e1) {
            log.error(e1,e1);
            return new PostingStatus(time,false);
        }
    }

    @Override
    public boolean canPublish() {
        return true;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public ServerKeystoreConfig getKeystoreConfig() {
        return keystoreConfig;
    }

    public void setKeystoreConfig(ServerKeystoreConfig keystoreConfig) {
        this.keystoreConfig = keystoreConfig;
    }

}