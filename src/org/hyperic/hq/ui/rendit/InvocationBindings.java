package org.hyperic.hq.ui.rendit;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class encapsulates the data which is passed from RenditServer into 
 * the groovy dispatcher.
 */
public class InvocationBindings {
    private File                _pluginDir;
    private HttpServletRequest  _request;
    private HttpServletResponse _response; 
    
    InvocationBindings(File pluginDir, HttpServletRequest request, 
                       HttpServletResponse response)
    {
        _pluginDir   = pluginDir;
        _request     = request;
        _response    = response;
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
