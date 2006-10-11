package org.hyperic.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.SessionFactory;
import org.hibernate.Interceptor;
import org.hibernate.EmptyInterceptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * <p/>
 * from hibernate caveat emptor with modifications to optimize initial context lookup
 */
public class Util
{
    private static Log log = LogFactory.getLog(Util.class);

    private static final String INTERCEPTOR_CLASS = "hibernate.util.interceptor_class";

    private static Configuration configuration;
    private static SessionFactory sessionFactory;

    static {
        // Create the initial SessionFactory from the default configuration files
        try {
            // Replace with Configuration() if you don't use annotations or JDK 5.0
            configuration = new Configuration();

            // Read not only hibernate.properties, but also hibernate.cfg.xml
            configuration.configure("META-INF/hibernate.cfg.xml");

            // Set global interceptor from configuration
            setInterceptor(configuration, null);

            String jndiName = configuration.getProperty(Environment.SESSION_FACTORY_NAME);
            if (jndiName != null) {
                // Let Hibernate bind the factory to JNDI
                configuration.buildSessionFactory();
            } else {
                // or use static variable handling
                sessionFactory = configuration.buildSessionFactory();
            }
        } catch (Throwable ex) {
            // We have to catch Throwable, otherwise we will miss
            // NoClassDefFoundError and other subclasses of Error
            log.error("Building SessionFactory failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Returns the original Hibernate configuration.
     *
     * @return Configuration
     */
    public static Configuration getConfiguration()
    {
        return configuration;
    }

    /**
     * Returns the global SessionFactory.
     *
     * @return SessionFactory
     */
    public static SessionFactory getSessionFactory()
    {
        SessionFactory sf;
        String sfName = configuration.getProperty(Environment.SESSION_FACTORY_NAME);
        if (sfName != null) {
            if (log.isDebugEnabled()) {
                log.debug("Looking up SessionFactory JNDI = " + sfName);
            }
            try {
                sf = (SessionFactory) new InitialContext().lookup(sfName);
            } catch (NamingException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            sf = sessionFactory;
        }
        if (sf == null)
            throw new IllegalStateException("SessionFactory not available.");
        return sf;
    }

    /**
     * Closes the current SessionFactory and releases all resources.
     * <p/>
     * The only other method that can be called on HibernateUtil
     * after this one is rebuildSessionFactory(Configuration).
     */
    public static void shutdown()
    {
        log.debug("Shutting down Hibernate.");
        // Close caches and connection pools
        getSessionFactory().close();

        // Clear static variables
        configuration = null;
        sessionFactory = null;
    }

    public static Interceptor getInterceptor()
    {
        return configuration.getInterceptor();
    }

    /**
     * Resets global interceptor to default state.
     */
    public static void resetInterceptor()
    {
        log.debug("Resetting global interceptor to configuration setting");
        setInterceptor(configuration, null);
    }

    /**
     * Either sets the given interceptor on the configuration or looks
     * it up from configuration if null.
     */
    private static void setInterceptor(Configuration configuration, Interceptor interceptor)
    {
        String interceptorName = configuration.getProperty(INTERCEPTOR_CLASS);
        if (interceptor == null && interceptorName != null) {
            try {
                Class interceptorClass =
                        Util.class.getClassLoader().loadClass(interceptorName);
                interceptor = (Interceptor) interceptorClass.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Could not configure interceptor: " + interceptorName, ex);
            }
        }
        if (interceptor != null) {
            configuration.setInterceptor(interceptor);
        } else {
            configuration.setInterceptor(EmptyInterceptor.INSTANCE);
        }
    }

    public static String getDialect()
    {
        return configuration.getProperty(Environment.DIALECT);
    }
}
