package org.hyperic.hq.plugin.websphere;

import javax.management.ObjectName;

import org.hyperic.hq.product.PluginException;

import com.ibm.websphere.management.AdminClient;

public class EJBCollector extends WebsphereCollector {
    private ObjectName name;

    protected void init(AdminClient mServer) throws PluginException {
        super.init(mServer);

        String module = getModuleName();
        int ix = module.indexOf('#');
        if (ix == -1) {
            throw new PluginException("Malformed ejb name '" + module + "'");
        }
        String app = module.substring(0, ix);
        String ejb = module.substring(ix+1);

        this.name =
            newObjectNamePattern("j2eeType=EJBModule," +
                                 "J2EEApplication=" + app + "," +
                                 "name=" + ejb + "," +
                                 getProcessAttributes());
        
        this.name = resolve(mServer, this.name);
    }

    public void collect() {
        Object ejbs =
            getAttribute(getMBeanServer(), this.name, "ejbs");

        if (ejbs == null) {
            setAvailability(false);
        }
        else {
            setAvailability(true);
        }
    }
}
