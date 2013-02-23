package org.hyperic.util.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.hyperic.util.security.DefaultSSLProviderImpl;
import org.hyperic.util.security.KeystoreConfig;
import org.hyperic.util.security.SSLProvider;
import org.springframework.util.Assert;

public class HQHttpClient extends DefaultHttpClient {
    private Log log;
    
    public HQHttpClient(final KeystoreConfig keyConfig, final HttpConfig config, final boolean acceptUnverifiedCertificates) {
    	super();
    	log = LogFactory.getLog(HQHttpClient.class);
        log.debug("Keystore info: Alias="+keyConfig.getAlias()+
            ", acceptUnverifiedCert="+acceptUnverifiedCertificates);

   		SSLProvider sslProvider = new DefaultSSLProviderImpl(keyConfig, acceptUnverifiedCertificates);
        Scheme sslScheme = new Scheme("https", 443, sslProvider.getSSLSocketFactory());
			
		getConnectionManager().getSchemeRegistry().register(sslScheme);
			
		if (config != null) {
			getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, config.getSocketTimeout());
			getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, config.getConnectionTimeout());
	        
			// configure proxy, if appropriate
			String proxyHost = config.getProxyHostname();
			int proxyPort = config.getProxyPort();
	        
			if (proxyHost != null & proxyPort != -1) {
				HttpHost proxy = new HttpHost(proxyHost, proxyPort);

				getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			}
		}
    }
    
    public HttpResponse post(String url, Map<String, String> params) throws ClientProtocolException, IOException {
    	return post(url, null, params);
    }
    
    public HttpResponse post(String url, Map<String, String> headers, Map<String, String> params) throws ClientProtocolException, IOException {
    	Assert.hasText(url);
    	
    	HttpPost post = new HttpPost(url);
        
    	if (headers != null && !headers.isEmpty()) {
    		for (Map.Entry<String, String> entry : headers.entrySet()) {
    			post.addHeader(entry.getKey(), entry.getValue());
    		}
    	}
    	
    	if (params != null && !params.isEmpty()) {
	    	List<NameValuePair> postParams = new ArrayList<NameValuePair>();
	    	
	    	for (Map.Entry<String, String> entry : params.entrySet()) {
	    		postParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
	    	}
        
	    	post.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));
    	}
    	
        return execute(post);
    }
}