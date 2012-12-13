package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.hyperic.tools.dbmigrate.Utils;
import org.hyperic.util.jdbc.DBUtil;
import org.jasypt.encryption.pbe.PBEStringEncryptor;

public class SST_IntegrityValidator extends SchemaSpecTask {

    private static final String ENABLE_INTEGRITY_VALIDATION_KEY = "enable.validation.integrity" ;  
    private String encryption ; 
    
    public final void setEncryption(final String encryption) { 
        this.encryption = encryption ; 
    }//EOM 
    
    @Override
    public void execute() throws BuildException {
        //first determine whether the validation was disabled if if so abort 
        final String validationEnabled = this.getProject().getProperty(ENABLE_INTEGRITY_VALIDATION_KEY) ; 
        
        if(validationEnabled != null && validationEnabled.trim().equalsIgnoreCase("FALSE")) { 
            this.log(">>>>> Integrity Validation was disabled, aborting.", Project.MSG_WARN) ;
            return ; 
        }//EO if validation is disabled 
        
        try{ 
            this.validateEncryptionItegrity() ;
            
            this.log("Overall Integrity Validation was successful.", Project.MSG_INFO) ; 
        }catch(Throwable t) { 
            throw ( (t instanceof BuildException) ? (BuildException)t: new BuildException(t)) ; 
        }//EO catch block 
    }//EOM 
    
    private final void validateEncryptionItegrity() throws Throwable { 
        Statement stmt = null ; 
        ResultSet rs = null ;
        
        try{ 
            final String stmtTemplate = "select %2$s.%1$s from %2$s where %1$s like 'ENC(%%' limit 2" ; 
            stmt = this._conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY) ; 
            
            final String[] qualifiedColumnNames = this.encryption.split("[,]") ;
            String[] tableColumntuples = null ; 
            final int length = qualifiedColumnNames.length ;
            
            final StringBuilder stmtBuilder = new StringBuilder() ; 
            
            for(int i=0; i < length; i++) {
                tableColumntuples = qualifiedColumnNames[i].split("\\.") ; 
                stmtBuilder.append(String.format(stmtTemplate, tableColumntuples[1], tableColumntuples[0])) ; 
                if(i < length-1) stmtBuilder.append(";") ;
            }//EO while there are more table-column tuples 
            
            final boolean resultSetExists = stmt.execute(stmtBuilder.toString()) ;
            if(resultSetExists) { 
                
                final PBEStringEncryptor decryptor = this.newEncryptor() ;
                
                String encryptedValue = null, qualifiedColumnName = null ;
                int rsCounter = 0 ; 
                try{ 
                    do { 
                       rs = stmt.getResultSet() ;
                       qualifiedColumnName = qualifiedColumnNames[rsCounter++] ; 
                       this.log("Validating Encyrption Integrity for Table " + qualifiedColumnName + "...") ;
                       while(rs.next()) { 
                           encryptedValue = rs.getString(1) ; 
                           //now attempt to decrypt the value 
                           decryptor.decrypt(encryptedValue) ;
                       }//EO while there are more records 
                    }while(stmt.getMoreResults()) ; 
                }catch(SQLException sqle) { 
                    throw sqle ; 
                }catch(Throwable t) {
                    final String errorMsg = "\n\n>>>>>>>>>>>> INTEGRITY ERROR: " + 
                            ":\nFailed to decrypt value(s) from " + 
                            qualifiedColumnName +  
                          " table.\n" +
                          "This means that the hq-server.conf#server.encryption-key " + 
                          "used for encryption is not the same as the one used for decryption.\n" + 
                          "Please refer to the documentation for manual recovery procedure.\n" +
                          "Aborting Installation/Startup!\n" +
                          "<<<<<<<<<<<<<\n\n" ; 
                    this.log(errorMsg, Project.MSG_ERR) ; 
                    throw new BuildException(errorMsg, t) ; 
                }//EO inner catch block 
            
            }//EO if resultset(s) exist
            
            this.log(": Encryption Integrity Validation was successful.", Project.MSG_INFO) ; 
        }catch(Throwable t) { 
            t.printStackTrace() ; 
            throw t ; 
        }finally{ 
            DBUtil.closeJDBCObjects(this._ctx, null/*connection*/, stmt, rs) ; 
        }//EO catch block 
        
    }//EOM 
    
    private static final void injectValues(final Connection conn, final DBUpgrader upgrader) throws Throwable{
        final PBEStringEncryptor encryptor = upgrader.newEncryptor() ;
        final String encryptedValue = encryptor.encrypt("value1" + System.currentTimeMillis()) ;   
        Utils.executeUpdate(conn, "insert into enc_test values(1,'" + encryptedValue + "')") ;
        Utils.executeUpdate(conn, "insert into enc_test1 values(1,'" + encryptedValue + "')") ;
    }//EOM 
    
}//EOM 
