package org.hyperic.hq.bizapp.server.session;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.product.server.MBeanUtil;

public class BossStartupListener 
    implements StartupListener
{
    private final Log _log = LogFactory.getLog(BossStartupListener.class);
    
    public void hqStarted() {
        EventsBossEJBImpl.getOne().startup();
        UpdateBossEJBImpl.getOne().startup();
        ProductBossEJBImpl.getOne().preload();
        
        try {
            createStatsMBean();
        } catch(Exception e) {
            _log.warn("Unable to create stats mbean", e);
        }
    }
    
    private void createStatsMBean()
        throws MalformedObjectNameException, InstanceAlreadyExistsException,
                MBeanRegistrationException, NotCompliantMBeanException
    {
        MBeanServer server = MBeanUtil.getMBeanServer();

        ObjectName on =
            new ObjectName("hyperic.jmx:name=HQInternal");
        HQInternalService mbean = new HQInternalService();
        server.registerMBean(mbean, on);
        _log.info("HQ Internal Statistics MBean registered: " + on);
    }
}
