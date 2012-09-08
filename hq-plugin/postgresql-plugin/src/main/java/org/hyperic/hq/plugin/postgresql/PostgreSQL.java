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
package org.hyperic.hq.plugin.postgresql;

import java.util.List;
import java.util.Properties;
import org.hyperic.hq.product.PluginException;

public class PostgreSQL {

    protected static final String PROP_TABLE = "table";
    protected static final String PROP_INDEX = "index";
    protected static final String PROP_SCHEMA = "schema";
    protected static final String PROP_DB = "db";
    protected static final String PROP_USER = "postgresql.user";
    protected static final String PROP_PASS = "postgresql.pass";
    protected static final String PROP_HOST = "postgresql.host";
    protected static final String PROP_PORT = "postgresql.port";
    protected static final String PROP_DFDB = "postgresql.dfdb";
    protected static final String PROP_DATA = "postgresql.pgdata";
    protected static final String PROP_TABLE_REG = "postgresql.table.regex";
    protected static final String PROP_INDEX_REG = "postgresql.index.regex";
    protected static final String PROP_PROGRAM = "postgresql.program";
    protected static final String PROP_PREFIX = "postgresql.prefix";
    protected static final String PROP_TIMEOUT = "postgresql.timeout";
    // localhost:5432
    protected static final String SERVER_NAME = System.getProperty("postgresql.server.name.format", "${" + PROP_HOST + "}:${" + PROP_PORT + "}");
    // DataBase localhost:5432 database
    protected static final String DB_NAME = System.getProperty("postgresql.database.name.format", "DataBase ${" + PROP_DB + "}");
    // Table localhost:5432 database.schema.table
    protected static final String TABLE_NAME = System.getProperty("postgresql.table.name.format", "Table ${" + PROP_DB + "}.${" + PROP_SCHEMA + "}.${" + PROP_TABLE + "}");
    // Index localhost:5432 database.schema.index
    protected static final String INDEX_NAME = System.getProperty("postgresql.index.format", "Index ${" + PROP_DB + "}.${" + PROP_SCHEMA + "}.${" + PROP_INDEX + "}");
    // HQ special names
    protected static final String HQ_SERVER_NAME = "";
    protected static final String HQ_DB_NAME = "DataBase ${" + PROP_DB + "}";
    protected static final String HQ_TABLE_NAME = "Table ${" + PROP_TABLE + "}";
    protected static final String HQ_INDEX_NAME = "Index ${" + PROP_INDEX + "}";

    protected static String prepareUrl(Properties config, String db) throws PluginException {
        String host = config.getProperty(PostgreSQL.PROP_HOST);
        String port = config.getProperty(PostgreSQL.PROP_PORT);
        if (db == null) {
            db = config.getProperty(PostgreSQL.PROP_DFDB);
        }

        if ((db == null) || (host == null) || (port == null)) {
            throw new PluginException("invalid configuration.");
        }
        return "jdbc:postgresql://" + host + ":" + port + "/" + db;
    }

    protected static String listToString(List list, String glue) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(glue);
            }
            sb.append("'").append(list.get(i)).append("'");
        }
        return sb.toString();
    }
}
