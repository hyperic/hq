package org.hyperic.hq.ui.rendit;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class encapsulates the data which is passed from RenditServer into 
 * the groovy dispatcher.
 */
public class InvocationBindings {
    private List                _requestPath;
    private File                _pluginDir;
    private HttpServletRequest  _request;
    private HttpServletResponse _response; 
    
    InvocationBindings(List requestPath, File pluginDir,
                       HttpServletRequest request, HttpServletResponse response)
    {
        _requestPath = requestPath;
        _pluginDir   = pluginDir;
        _request     = request;
        _response    = response;
    }
    
    public List getRequestPath() {
        return Collections.unmodifiableList(_requestPath);
    }
    
    public File getPluginDir() {
        return _pluginDir;
    }
    
    public HttpServletRequest getRequest() {
        return _request;
    }
    
    public HttpServletResponse getResponse() {
        return _response;
    }
}
