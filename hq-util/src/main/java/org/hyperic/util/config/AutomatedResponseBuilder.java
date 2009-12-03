/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.util.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.List;

import org.hyperic.util.PropertyUtil;

public class AutomatedResponseBuilder extends InteractiveResponseBuilder {

    private Properties props = null;
    private String requiredProp = null;
    private boolean hasRequiredProp = false;

    public AutomatedResponseBuilder (InteractiveResponseBuilder_IOHandler io,
                                     Properties props,
                                     String requiredProp) {
        super(io);
        this.props = props;
        this.requiredProp = requiredProp;
        hasRequiredProp = (props.getProperty(requiredProp) != null);
    }

    public AutomatedResponseBuilder (InteractiveResponseBuilder_IOHandler io,
                                     File propFile,
                                     String requiredProp) throws IOException {
        super(io);
        this.props = PropertyUtil.loadProperties(propFile.getPath());
        this.requiredProp = requiredProp;
        hasRequiredProp = (props.getProperty(requiredProp) != null);
    }

    public ConfigResponse processConfigSchema(ConfigSchema schema) 
        throws EncodingException, IOException, InvalidOptionException, 
               EarlyExitException {

        // Silently bail if required prop is not here.  This allows a 
        // property-file driven install to skip parts (like agent/shell)
        // that are not defined in the setup properties file.
        if (!hasRequiredProp) {
            throw new SkipConfigException("skipping config due to missing "
                                          + "required property: " 
                                          + requiredProp);
        }

        List options = schema.getOptions();
        int nOptions = options.size();
        ConfigResponse res;
        res = new ConfigResponse(schema);
        for (int i=0; i<nOptions; i++){
            ConfigOption opt = (ConfigOption)options.get(i);
            String optName = opt.getName();
            try {
                String prop = props.getProperty(optName);
                res.setValue(optName, prop);
            } catch(InvalidOptionValueException exc){
                sendToErrStream(exc.getMessage());
                throw new IllegalStateException("Error setting option "
                                                +" value for " + optName + ", "
                                                + "cannot continue: " + exc);
            }
        }
        
        return res;
    }
}
