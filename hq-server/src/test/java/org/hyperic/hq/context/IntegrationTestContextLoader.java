package org.hyperic.hq.context;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.support.GenericXmlContextLoader;

public class IntegrationTestContextLoader extends GenericXmlContextLoader {
	private final Log log = LogFactory.getLog(IntegrationTestContextLoader.class);
	
    @Override
    protected void customizeContext(GenericApplicationContext context) {
        Bootstrap.appContext = context;
        try {
        	//Find the sigar libs on the test classpath
			File sigarBin = new File(context.getResource("/libsigar-sparc64-solaris.so").getFile().getParent());
			log.info("Setting sigar path to : " + sigarBin.getAbsolutePath());
			System.setProperty("org.hyperic.sigar.path",sigarBin.getAbsolutePath());
		} catch (IOException e) {
			log.error("Unable to initiailize sigar path",e);
		}
    }
}
