package org.hyperic.hq.plugin.websphere;

import javax.management.ObjectName;

import org.hyperic.hq.product.PluginException;

import com.ibm.websphere.management.AdminClient;

public class ApplicationCollector extends WebsphereCollector {

    private ObjectName name;

    protected void init(AdminClient mServer) throws PluginException {
        super.init(mServer);

        this.name =
            newObjectNamePattern("type=J2EEApplication," +
                                 "name=" + getModuleName() + "," +
                                 getProcessAttributes());
        
        this.name = resolve(mServer, this.name);
    }

    public void collect() {
        Object state =
            getAttribute(getMBeanServer(), this.name, "state");

        if (state == null) {
            setAvailability(false);
        }
        else {
            setAvailability(true);
        }
    }
}
