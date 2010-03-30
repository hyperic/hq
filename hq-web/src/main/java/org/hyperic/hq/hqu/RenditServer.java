package org.hyperic.hq.hqu;

import java.io.File;
import java.io.Writer;
import java.util.Map;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.hqu.server.session.Attachment;

public interface RenditServer {
    void renderTemplate(File template, Map params, Writer output) throws Exception;

    void handleRequest(String pluginName, RequestInvocationBindings b) throws Exception;

    AttachmentDescriptor getAttachmentDescriptor(String pluginName, Attachment a, Resource ent,
                                                 AuthzSubject u);

    void addPluginDir(File path) throws Exception;

    void removePluginDir(String pluginName);

}
