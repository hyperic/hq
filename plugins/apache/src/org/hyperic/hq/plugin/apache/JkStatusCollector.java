package org.hyperic.hq.plugin.apache;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.plugin.netservices.HTTPCollector;
import org.hyperic.hq.product.PluginException;

public class JkStatusCollector extends HTTPCollector {
    private static final Log log =
        LogFactory.getLog(JkStatusCollector.class.getName());
    private static final String MIME_FLAG = "?mime=prop";

    protected void init() throws PluginException {
        super.init();
    
        setMethod(METHOD_GET);

        String url = getURL();
        if (!url.endsWith(MIME_FLAG)) {
            setURL(url + MIME_FLAG);
        }
    }

    private void parse(HttpMethod method) throws IOException {
        InputStream is =
            method.getResponseBodyAsStream();
        //XXX
    }

    protected void parseResults(HttpMethod method) {
        try {
            parse(method);
        } catch (IOException e) {
            log.error("Exception parsing: " + getURL(), e);
        }
    }
}
