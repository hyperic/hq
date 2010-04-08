/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.sybase;

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;

/**
 *
 * @author laullon
 */
public class SysbaseProductPlugin extends ProductPlugin {

    private static boolean originalAIID = true;
    Log log = getLog();

    protected static boolean isOriginalAIID() {
        return originalAIID;
    }

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        Properties props = manager.getProperties();
        originalAIID = "true".equals(props.getProperty("sysbase.aiid.orginal", "true").toLowerCase());
    }
}
