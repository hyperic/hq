package org.hyperic.hq.context;
import java.util.Collection;

import org.hyperic.hibernate.Util;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;



public class Bootstrap {

    private static final String[] APP_CONTEXT_FILES = new String[] { "classpath*:/META-INF/spring/dao-context.xml" };
    
    private static final String[] EJB_APP_CONTEXT_FILES = new String[] { "classpath*:/META-INF/spring/ejb-*context.xml" };

    private static ApplicationContext APP_CONTEXT;
   

    public synchronized static ApplicationContext getContext() throws Exception {
        if (APP_CONTEXT == null) {
            //Needed to bind SessionFactory to JNDI through static initialize
            Class.forName(Util.class.getName());
            APP_CONTEXT = new ClassPathXmlApplicationContext(APP_CONTEXT_FILES);
        }
        return APP_CONTEXT;
    }
    
    public static synchronized void loadEJBApplicationContext() {
        try {
            APP_CONTEXT = new ClassPathXmlApplicationContext(EJB_APP_CONTEXT_FILES,APP_CONTEXT);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T getBean(Class<T> beanClass)  {
        try {
            Collection<T> beans = getContext().getBeansOfType(beanClass).values();
            if(beans.isEmpty() && getContext().getParent() != null) {
                beans = getContext().getParent().getBeansOfType(beanClass).values();
            }
            T bean = beans.iterator().next();
            if (bean == null) {
                throw new IllegalArgumentException("Couldn't locate bean of " + beanClass + " type");
            }
            return bean;
        } catch (BeansException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}