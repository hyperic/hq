package org.hyperic.tools.dbmigrate;

import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hyperic.util.security.MarkedStringEncryptor;
import org.hyperic.util.security.SecurityUtil;

public class EnvSetupTask extends Task{
    
    private static final String SERVER_DB_PASSWORD_KEY = "server.database-password";
    private static final String DECRYPTION_KEY_KEY = "server.encryption-key";

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
