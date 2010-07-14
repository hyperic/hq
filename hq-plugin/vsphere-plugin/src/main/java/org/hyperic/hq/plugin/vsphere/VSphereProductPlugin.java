package org.hyperic.hq.plugin.vsphere;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TypeInfo;

public class VSphereProductPlugin extends ProductPlugin {

    public GenericPlugin getPlugin(String type, TypeInfo entity)
    {  
        if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY) &&
            entity.getName().equals(AuthzConstants.serverPrototypeVmwareVcenter)) {
            return new VCenterDetector();
        }
        return super.getPlugin(type, entity);
    }
}
