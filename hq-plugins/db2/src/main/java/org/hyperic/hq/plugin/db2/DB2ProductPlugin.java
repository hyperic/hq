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

package org.hyperic.hq.plugin.db2;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.TypeBuilder;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

public class DB2ProductPlugin 
    extends ProductPlugin
{
    public static final String NAME     = "db2";

    static final String SERVER_NAME     = "DB2";
    static final String VERSION_7       = "7.x";
    static final String VERSION_8       = "8.x";
    static final String VERSION_9       = "9.x";

    static final String DATABASE        = "Database";
    static final String TABLE           = "Table";
    static final String TABLESPACE      = "Tablespace";

    static final String FULL_SERVER_NAME_V7 =
        TypeBuilder.composeServerTypeName(SERVER_NAME, VERSION_7);

    static final String FULL_DATABASE_NAME_V7 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V7, 
                                           DATABASE);

    static final String FULL_TABLE_NAME_V7 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V7, 
                                           TABLE);

    static final String FULL_TABLESPACE_NAME_V7 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V7, 
                                           TABLESPACE);

    static final String FULL_SERVER_NAME_V8 =
        TypeBuilder.composeServerTypeName(SERVER_NAME, VERSION_8);

    static final String FULL_DATABASE_NAME_V8 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V8, 
                                           DATABASE);

    static final String FULL_TABLE_NAME_V8 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V8, 
                                           TABLE);

    static final String FULL_TABLESPACE_NAME_V8 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V8, 
                                           TABLESPACE);

    static final String FULL_SERVER_NAME_V9 =
        TypeBuilder.composeServerTypeName(SERVER_NAME, VERSION_9);

    static final String FULL_DATABASE_NAME_V9 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V9, 
                                           DATABASE);

    static final String FULL_TABLE_NAME_V9 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V9, 
                                           TABLE);

    static final String FULL_TABLESPACE_NAME_V9 =
        TypeBuilder.composeServiceTypeName(FULL_SERVER_NAME_V9, 
                                           TABLESPACE);

    static final String SERVICES[] = {
        DATABASE,
        TABLE,
        TABLESPACE,
    };

    // Config schema attributes
    static final String PROP_NODENAME      = "nodename";
    static final String PROP_USER          = "user";
    static final String PROP_PASSWORD      = "password";
    static final String PROP_DATABASE      = "database";
    static final String PROP_TABLE         = "table";
    static final String PROP_TABLESPACE    = "tablespace";
    static final String PROP_MON_ENABLE    = "enableMon";

    public DB2ProductPlugin() {
        this.setName(NAME);
    }

    public GenericPlugin getPlugin(String type, TypeInfo entity)
    {
        if (type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            return new DB2MeasurementPlugin();
        } else if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY) &&
                   entity.getType() == TypeInfo.TYPE_SERVER)
        {
            return new DB2ServerDetector(entity.getVersion());
        }

        return null;
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {

        SchemaBuilder schema = new SchemaBuilder(config);
        ConfigOption opt;

        switch (info.getType()) {
        case TypeInfo.TYPE_SERVER:
            opt = schema.add(PROP_NODENAME, "DB2 Node Name", "");
            opt.setOptional(true);
            opt = schema.add(PROP_USER, "Username", "");
            opt.setOptional(true);
            opt = schema.addSecret(PROP_PASSWORD, "Password");
            opt.setOptional(true);

            schema.add(PROP_MON_ENABLE,
                       "Enable monitoring switches",
                       false);
            break;

        case TypeInfo.TYPE_SERVICE:
            // All services require a database alias
            schema.add(PROP_DATABASE, "Database Alias", "SAMPLE");
            
            if (info.isService(DB2ProductPlugin.TABLE)) {
                schema.add(PROP_TABLE, "Table Name", "STAFF");
            } else if (info.isService(DB2ProductPlugin.TABLESPACE)) {
                schema.add(PROP_TABLESPACE, "Tablespace Name", "SYSCATSPACE");
            }
        }

        return schema.getSchema();
    }

    public TypeInfo[] getTypes() {

        TypeBuilder types = new TypeBuilder(SERVER_NAME, SERVER_NAME);
        ServerTypeInfo server;
        
        // 7.x Types
        server = types.addServer(VERSION_7);
        types.addServices(server, SERVICES);

        // 8.x Types
        server = types.addServer(VERSION_8);
        types.addServices(server, SERVICES);

        // 9.x Types
        server = types.addServer(VERSION_9);
        types.addServices(server, SERVICES);

        return types.getTypes();
    }
}
