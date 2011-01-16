package org.hyperic.hq.hqu.grails.plugins;

import grails.spring.BeanBuilder;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin;

public class DefaultHQUGrailsPlugin extends DefaultGrailsPlugin implements HQUGrailsPlugin {

	private final static Log log = LogFactory.getLog(DefaultHQUGrailsPlugin.class);
	
	public DefaultHQUGrailsPlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
	}

	public void doWithRuntimeConfigurationOnce(RuntimeSpringConfiguration springConfig) {
        if(this.pluginBean.isReadableProperty("doWithSpringOnce")) {
            if(log.isDebugEnabled()) {
                log.debug("Plugin " + this + " is participating in Spring configuration with single execution...");
            }
            GroovyObject plugin = (GroovyObject)this.pluginBean.getWrappedInstance();
            Closure c = (Closure)plugin.getProperty("doWithSpringOnce");
            BeanBuilder bb = new BeanBuilder(getParentCtx(),springConfig, application.getClassLoader());
            Binding b = new Binding();
//            b.setVariable("application", application);
//            b.setVariable("manager", getManager());
            b.setVariable("plugin", this);
            b.setVariable("parentCtx", getParentCtx());
            b.setVariable("resolver", getResolver());
            bb.setBinding(b);
            c.setDelegate(bb);
            bb.invokeMethod("beans", new Object[]{c});
        }

	}

}
