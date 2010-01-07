package org.hyperic.hq.context;

import javax.servlet.ServletContext;

import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

public class BootstrapContextLoaderListener extends ContextLoaderListener {

    @Override
    protected void customizeContext(ServletContext servletContext, ConfigurableWebApplicationContext applicationContext) {
        Bootstrap.appContext = applicationContext;
    }

}
