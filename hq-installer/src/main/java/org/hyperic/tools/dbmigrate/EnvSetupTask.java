/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.tools.dbmigrate;

import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hyperic.util.security.MarkedStringEncryptor;
import org.hyperic.util.security.SecurityUtil;

/**
 * Responsible for decrypting the database password and re-storing against the {@value EnvSetupTask#SERVER_DB_PASSWORD_KEY} key 
 * @author guy
 *
 */
public class EnvSetupTask extends Task{
    
    private static final String SERVER_DB_PASSWORD_KEY = "server.database-password";
    private static final String DECRYPTION_KEY_KEY = "server.encryption-key";

    /**
     * Uses the {@value EnvSetupTask#DECRYPTION_KEY_KEY} property value to decrypt the server password and re-set it into the 
     *  {@value EnvSetupTask#SERVER_DB_PASSWORD_KEY} property 
     */
    public void execute() throws BuildException {
        final Project proj = getProject();
      
        @SuppressWarnings("rawtypes")
        Hashtable env = proj.getProperties();
        String dbpassword = (String) env.get(SERVER_DB_PASSWORD_KEY);

        if (SecurityUtil.isMarkedEncrypted(dbpassword)) {
            String decryptionKey = (String) env.get(DECRYPTION_KEY_KEY);
            final MarkedStringEncryptor decryptor = new MarkedStringEncryptor(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, decryptionKey);
            
            String decryptedDBPassword = decryptor.decrypt(dbpassword);
            proj.setUserProperty(SERVER_DB_PASSWORD_KEY, decryptedDBPassword);
        }//EO if encrypted 
    }//EOM 
}//EOC 
