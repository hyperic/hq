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
public class WeblogicDetectorAdmin extends WeblogicDetector {

    @Override
    public boolean isValidProc(String[] args) {
        boolean res = true;
        for (int j = 0; j < args.length; j++) {
            String arg = args[j];
            if (arg.startsWith(WeblogicDetector.PROP_MX_SERVER)) {
                res = false;
            }
        }
        return res;
    }

    @Override
    void setIdentifier(ServerResource server, String name) {
        server.setIdentifier(server.getInstallPath());
    }
}
