package org.hyperic.hq.ui.contributions.ajax;

import org.apache.hivemind.ServiceImplementationFactory;
import org.apache.hivemind.ServiceImplementationFactoryParameters;
import org.apache.tapestry.javascript.JavascriptManager;

public class DojoDelegateFactory implements ServiceImplementationFactory {

    private JavascriptManager _javascriptManager;

    public void setJavascriptManager(JavascriptManager jsMan) {
        _javascriptManager = jsMan;
    }

    public Object createCoreServiceImplementation(
            ServiceImplementationFactoryParameters params) {
        return new DojoOnePointOneShellDelegate(_javascriptManager);
    }

}
