package org.hyperic.hq.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
public class Bootstrap {

    private static final String[] APP_CONTEXT_FILES=new String[] {"META-INF/spring/dao-context.xml"};
    
    
    public static ApplicationContext getContext() {
        return new ClassPathXmlApplicationContext(APP_CONTEXT_FILES);
    }
    
    public static<T> T getBean(Class<T> beanClass) {
        return getContext().getBeansOfType(beanClass).values().iterator().next();
    }
    
}
