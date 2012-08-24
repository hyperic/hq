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

package org.hyperic.hq.plugin.alfresco;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;

public class AlfrescoMeasurementPlugin 
    extends JDBCMeasurementPlugin
{
    private static Log log = LogFactory.getLog(AlfrescoMeasurementPlugin.class);

    private static final String
                        JDBC_DRIVER = AlfrescoPluginUtil.JDBC_DRIVER,
                        DEFAULT_URL = AlfrescoPluginUtil.DEFAULT_URL,
                        PROP_IDENTIFIER = AlfrescoPluginUtil.PROP_IDENTIFIER,
                        PROP_PROTOCOL   = AlfrescoPluginUtil.PROP_PROTOCOL,
                        PROP_USERS      = AlfrescoPluginUtil.PROP_USERS,
                        PROP_TRANSACTIONS = AlfrescoPluginUtil.PROP_TRANSACTIONS;

    private static final String TRANSACTIONS_SQL = 
                         "SELECT count(*) as transactions "+
                         "FROM alf_transaction";

    private static final String USERS_SQL = 
                         "SELECT count(*) as users "+
                         "FROM alf_access_control_entry c, alf_node n "+
                         "WHERE c.acl_id = n.acl_id "+
                         "AND type_qname like '%person'";

    public AlfrescoMeasurementPlugin() {
        setName("alfresco");
    }

    protected void initQueries()
    {
    }

    protected void getDriver()
        throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
    }

    protected String getDefaultURL() {
        return DEFAULT_URL;
    }

    @Override
    public String getPassword(Metric jdsn) {
        String pass = super.getPassword(jdsn);
        pass = (pass == null) ? "" : pass;
        pass = (pass.matches("^\\s*$")) ? "" : pass;
        return pass;
    }

    /**
     * Override the JDBCMeasurementPlugin getConfigSchema so that we only
     * generate config schema questions for the server types.  The service
     * types will use server config
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        if (info.getType() == TypeInfo.TYPE_SERVICE) {
            SchemaBuilder builder = new SchemaBuilder(config);
            // User instances require an additional user argument
            return builder.getSchema();
        }
        return new ConfigSchema();
    }
     */

    protected String getQuery(Metric metric)
    {
        String alias      = metric.getAttributeName();
        if (alias.indexOf(PROP_USERS) != -1)
            return USERS_SQL;

        else // if (alias.indexOf(PROP_TRANSACTIONS) != -1)
            return TRANSACTIONS_SQL;
    }
}
