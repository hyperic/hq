package org.hyperic.hq.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestBootstrap {

    private static final String[] APP_CONTEXT_FILES = new String[] { "test-context.xml" };

    private static ApplicationContext APP_CONTEXT;

    public synchronized static ApplicationContext getContext() {
        if (APP_CONTEXT == null) {
            APP_CONTEXT = new ClassPathXmlApplicationContext(APP_CONTEXT_FILES);
        }
        return APP_CONTEXT;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return getContext().getBeansOfType(beanClass).values().iterator().next();
    }
}
