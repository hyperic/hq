/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.weblogic;

import org.hyperic.hq.product.ServerResource;

/**
 *
 * @author administrator
 */
public class WeblogicDetectorManaged extends WeblogicDetector {

    @Override
    public boolean isValidProc(String[] args) {
        boolean res = false;
        for (int j = 0; j < args.length; j++) {
            String arg = args[j];
            if (arg.startsWith(WeblogicDetector.PROP_MX_SERVER)) {
                res = true;
            }
        }
        return res && WeblogicProductPlugin.NEW_DISCOVERY;
    }

    @Override
    void setIdentifier(ServerResource server, String name) {
        server.setIdentifier(name);
    }
}
