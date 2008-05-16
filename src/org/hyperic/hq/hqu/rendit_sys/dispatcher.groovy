package org.hyperic.hq.hqu.rendit

import org.codehaus.groovy.runtime.InvokerHelper
import groovy.lang.Script

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.server.session.Attachment
import org.hyperic.hq.hqu.server.session.UIPlugin
import org.hyperic.hq.hqu.rendit.IDispatcher
import org.hyperic.hq.hqu.rendit.IHQUPlugin
import org.hyperic.hq.hqu.rendit.InvokeMethodInvocationBindings
import org.hyperic.hq.hqu.rendit.RequestInvocationBindings
import org.hyperic.hq.hqu.rendit.PluginLoadException
import org.hyperic.hq.hqu.rendit.metaclass.AuthzSubjectCategory
import org.hyperic.hq.hqu.rendit.metaclass.AlertCategory
import org.hyperic.hq.hqu.rendit.metaclass.AlertDefinitionCategory
import org.hyperic.hq.hqu.rendit.metaclass.AppdefCategory
import org.hyperic.hq.hqu.rendit.metaclass.DownCategory
import org.hyperic.hq.hqu.rendit.metaclass.EscalationCategory
import org.hyperic.hq.hqu.rendit.metaclass.MapCategory
import org.hyperic.hq.hqu.rendit.metaclass.MetricCategory
import org.hyperic.hq.hqu.rendit.metaclass.ResourceCategory
import org.hyperic.hq.hqu.rendit.metaclass.ResourceGroupCategory
import org.hyperic.hq.hqu.rendit.metaclass.RoleCategory
import org.hyperic.hq.hqu.rendit.metaclass.StringCategory

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * The Dispatcher is the direct invocation target called from the HQ
 * RenditServer.  
 *
 * It uses the InvocationBindings to determine what type of action to take
 * (load a plugin, request a page, etc.)
 */
class Dispatcher implements IDispatcher {
    final int API_MAJOR = 0
    final int API_MINOR = 1
    
    private final CATEGORIES = [AuthzSubjectCategory, AlertCategory, 
                                AlertDefinitionCategory, AppdefCategory,
                                DownCategory, EscalationCategory, MapCategory,
                                MetricCategory, ResourceCategory,
                                ResourceGroupCategory, RoleCategory,
                                StringCategory]   
    
    private Log log = LogFactory.getLog(Dispatcher.class);

    private invokeArgs
	private IHQUPlugin  plugin
	
    def Dispatcher() {
    }
	
    Object invokeMethod(InvokeMethodInvocationBindings invokeArgs) {
        def dispatcher = new InvokeMethodDispatcher()
        use (*CATEGORIES) {
        	return dispatcher.invoke(plugin.pluginDir, invokeArgs)
        }
    }
    
    Properties loadPlugin(File pluginDir) {
        def parentLoader = Thread.currentThread().contextClassLoader
        def cl           = new GroovyClassLoader(parentLoader) 

        cl.addURL(new File(pluginDir, 'app').toURL())
        cl.addURL(new File(pluginDir, 'etc').toURL())
        
        Class c = cl.parseClass(new File(pluginDir, 'Plugin.groovy'))
        plugin  = c.newInstance()
        plugin.initialize(pluginDir)
        plugin.descriptor
    }

    void deploy(UIPlugin p) {
        use (*CATEGORIES) {
        	plugin.deploy(p)
        }
    }
    
    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r,
                                                 AuthzSubject u) 
    {
        use (*CATEGORIES) {
        	plugin.getAttachmentDescriptor(a, r, u)
        }
    }

    /**
     * Attempt to invoke a controller.  If the path looks like:
     *   plugin/controller/action
     * then attempt to locate the controller and associated action.
     */
    void handleRequest(RequestInvocationBindings invokeArgs) {
        def dispatcher = new DefaultControllerDispatcher()
		
		use (*CATEGORIES) {
        	return dispatcher.invoke(plugin, invokeArgs)
        }
     }
}
