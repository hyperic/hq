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
