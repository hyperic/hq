package org.hyperic.hq.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.Bootstrap;




public class ProductProperties {
    
    private static final Log  _log = 
        LogFactory.getLog(ProductProperties.class);

    public static Object getPropertyInstance(String key) {
        String beanName = org.hyperic.hq.common.shared.ProductProperties.getProperty(key);
        if (beanName != null) {
            try {
                _log.debug("Property " + key + " implemented by " + beanName);
                //TODO get rid of this whole thing and use app context to instantiate and inject
                return Bootstrap.getBean(beanName);
            } catch (Exception e) {
                _log.error("Unable to instantiate bean " + beanName +
                           " for property " + key, e);
            }
        }
        return null;
    }
}
