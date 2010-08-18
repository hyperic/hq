/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
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

package org.hyperic.tools.ant.dbupgrade;

import java.sql.DriverManager;

/**
 * Can be used to test the {@link HQJBossServerRemover}. Not enabled for
 * automated build as it requires an older DB schema to be installed first.
 * Should be part of systemic automated upgrade testing.
 * @author jhickey
 * 
 */
public class HQJBossServerRemoverTester {

    private HQJBossServerRemover remover = new HQJBossServerRemover();

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hqtest";

    private static final String DB_PASSWORD = "hq";

    private static final String DB_USER = "hq";

    private static final String UPGRADE_DIR = "/Applications/UpTest/server-4.3.0-EE";

   
    public void testExecute() throws Exception {
        remover.setConnection(DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD));
        remover.setUpgradeDir(UPGRADE_DIR);
        remover.execute();
    }
}
