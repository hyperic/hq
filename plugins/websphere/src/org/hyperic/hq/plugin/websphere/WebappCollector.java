package org.hyperic.hq.plugin.websphere;

import org.hyperic.hq.product.PluginException;

import com.ibm.websphere.management.AdminClient;

public class WebappCollector extends WebsphereCollector {

    protected void init(AdminClient mServer) throws PluginException {
        super.init(mServer);

        String module = getModuleName();
        int ix = module.indexOf('#');
        if (ix == -1) {
            throw new PluginException("Malformed webapp name '" + module + "'");
        }
        String app = module.substring(0, ix);
        String war = module.substring(ix+1);

        this.name =
            newObjectNamePattern("j2eeType=WebModule," +
                                 "J2EEApplication=" + app + "," +
                                 "name=" + war + "," +
                                 getProcessAttributes());
        
        this.name = resolve(mServer, this.name);
    }

    public void collect() {
        Object servlets =
            getAttribute(getMBeanServer(), this.name, "servlets");

        if (servlets == null) {
            setAvailability(false);
        }
        else {
            setAvailability(true);
        }
    }
}
