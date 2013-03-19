/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */


package org.hyperic.hq.bizapp.shared;

import java.util.Properties;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.util.ConfigPropertyException;

/**
 * Local interface for ConfigBoss.
 */
public interface ConfigBoss {
    /**
     * Get the top-level configuration properties
     */
    public Properties getConfig() throws ConfigPropertyException;

    /**
     * Get the configuration properties for a specified prefix
     */
    public Properties getConfig(String prefix) throws ConfigPropertyException;

    /**
     * Set the top-level configuration properties
     */
    public void setConfig(int sessId, Properties props) throws ApplicationException,
        ConfigPropertyException;

    /**
     * Set the configuration properties for a prefix
     */
    public void setConfig(int sessId, String prefix, Properties props) throws ApplicationException,
        ConfigPropertyException;

    public void setConfig(AuthzSubject subject, Properties props) throws ApplicationException, ConfigPropertyException;

}
