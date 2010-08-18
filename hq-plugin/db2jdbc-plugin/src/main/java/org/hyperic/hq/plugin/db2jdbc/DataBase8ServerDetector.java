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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hyperic.hq.plugin.db2jdbc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class DataBase8ServerDetector extends DataBaseServerDetector {

    protected List discoverServices(ConfigResponse config) throws PluginException {
        List res = new ArrayList();
        String type = getTypeInfo().getName();
        //String dbName = config.getValue("db2.jdbc.database");

        /**
         * Table
         */
        String schema = config.getValue("db2.jdbc.user").toUpperCase();
        Iterator tbl = getList(config, "select TABLE_NAME from table (SNAPSHOT_TABLE('sample', -2)) as T").iterator(); // XXX revisar si se pueden sacar de otro sitio WHERE TABSCHEMA='" + schema + "'");
        while (tbl.hasNext()) {
            String tbName = (String) tbl.next();
//            if (!tbName.toUpperCase().startsWith("SYS")) {
            ServiceResource tb = new ServiceResource();
            tb.setType(type + " Table");
            tb.setServiceName("Table " + schema + "." + tbName);

            ConfigResponse conf = new ConfigResponse();
            conf.setValue("table", tbName);
            conf.setValue("schema", schema);
            setProductConfig(tb, conf);
            tb.setMeasurementConfig();
            tb.setResponseTimeConfig(new ConfigResponse());
            tb.setControlConfig();

            res.add(tb);
//            }
        }

        /**
         * Table Space
         */
        Iterator tbspl = getList(config, "select TABLESPACE_NAME from table (SNAPSHOT_TBS('sample', -2)) as T").iterator();
        while (tbspl.hasNext()) {
            String tbspName = (String) tbspl.next();
            ServiceResource bpS = new ServiceResource();
            bpS.setType(type + " Table Space");
            bpS.setServiceName("Table Space " + tbspName);

            ConfigResponse conf = new ConfigResponse();
            conf.setValue("tablespace", tbspName);
            setProductConfig(bpS, conf);
            bpS.setMeasurementConfig();
            bpS.setResponseTimeConfig(new ConfigResponse());
            bpS.setControlConfig();

            res.add(bpS);
        }

        /**
         * Buffer Pool
         */
        Iterator bpl = getList(config, "select BP_NAME from table (SNAPSHOT_BP('sample', -2)) as T").iterator();
        while (bpl.hasNext()) {
            String bpName = (String) bpl.next();
            ServiceResource bpS = new ServiceResource();
            bpS.setType(type + " Buffer Pool");
            bpS.setServiceName("Buffer Pool " + bpName);

            ConfigResponse conf = new ConfigResponse();
            conf.setValue("bufferpool", bpName);
            setProductConfig(bpS, conf);
            bpS.setMeasurementConfig();
            bpS.setResponseTimeConfig(new ConfigResponse());
            bpS.setControlConfig();

            res.add(bpS);
        }

        /**
         * Mempory Pool
         */
        /*List<String> mpl = getList(config, "SELECT concat(concat(POOL_ID, '|'), COALESCE(POOL_SECONDARY_ID,'')) as name FROM SYSIBMADM.SNAPDB_MEMORY_POOL where POOL_SECONDARY_ID is NULL or POOL_ID='BP'");
        for (String mpN : mpl) {
        String[] names = mpN.split("\\|");
        String mpId=names[0].trim();
        String mpSId=(names.length==2)?names[1].trim():"";
        String mpName=(mpId+" "+mpSId).trim();

        ServiceResource mpS = new ServiceResource();
        mpS.setType(type + " Memory Pool");
        mpS.setServiceName("Memory Pool " + mpName);

        ConfigResponse conf = new ConfigResponse();
        conf.setValue("pool_id", mpId);
        conf.setValue("sec_pool_id", mpSId);
        setProductConfig(mpS, conf);
        mpS.setMeasurementConfig();
        mpS.setResponseTimeConfig(new ConfigResponse());
        mpS.setControlConfig();

        res.add(mpS);
        }*/

        return res;
    }



    public List getServerResources(ConfigResponse pconf) {
        List res = new ArrayList();
        /*String name="tets";
        String iPath="/";
        getLog().debug("[createDataBase] name='" + name + "' iPath='" + iPath + "'");
        ServerResource server = new ServerResource();
        server.setType(getTypeInfo().getName());
        //res.setName(getPlatformName() + " " + getTypeInfo().getName() + " " + name);
        server.setName(getPlatformName() + " DB2 " + name);
        server.setInstallPath(iPath);
        server.setIdentifier(server.getName());

        ConfigResponse conf = new ConfigResponse();
        conf.setValue("db2.jdbc.database", name);
        conf.setValue("db2.jdbc.version", getTypeInfo().getVersion());
        setProductConfig(server, conf);

        res.add(server);*/
        return res;
    }

}
