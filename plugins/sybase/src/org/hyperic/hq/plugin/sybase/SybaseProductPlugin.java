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

package org.hyperic.hq.plugin.sybase;

import org.hyperic.hq.product.TypeBuilder;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.ProductPlugin;

public class SybaseProductPlugin extends ProductPlugin {

    static final String NAME = "sybase";

    static final String SERVER_NAME = "Sybase";
    static final String SERVER_DESC = "Database Server";
    static final String INSTANCE = "Instance";

    static final String VERSION_12 = "12.x";

    static final String[] SERVICES = {
        INSTANCE
    };

    public SybaseProductPlugin() {
        setName(NAME);
    }

    public GenericPlugin getPlugin(String type, TypeInfo entity)
    {
        if (type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            return new SybaseMeasurementPlugin();
        }

        return null;
    }

    public TypeInfo[] getTypes() {
        TypeBuilder types = new TypeBuilder(SERVER_NAME, SERVER_DESC);

        ServerTypeInfo server;

        server = types.addServer(VERSION_12);
        types.addServices(server, SERVICES);

        return types.getTypes();
    }
}
