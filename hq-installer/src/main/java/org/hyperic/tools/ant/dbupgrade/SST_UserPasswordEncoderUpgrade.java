package org.hyperic.tools.ant.dbupgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;



public class SST_UserPasswordEncoderUpgrade extends SchemaSpecTask {
    private String encryption ; 
    
    public final void setEncryption(final String encryption) { 
        this.encryption = encryption ; 
    }//EOM 
    
    @Override
    public void execute() throws BuildException {
       
        try{ 
            this.passwordUpgrade() ;
            
            this.log("Overall Password Encoding upgrade was successful.", Project.MSG_INFO) ; 
        }catch(Throwable t) { 
            throw ( (t instanceof BuildException) ? (BuildException)t: new BuildException(t)) ; 
        }//EO catch block 
    }//EOM 
    
    private final void passwordUpgrade() throws Throwable { 
        Statement stmt = null ; 
        ResultSet rs = null ;
        
        try{ 
            final String stmtTemplate = "select %2$s.%1$s from %2$s" ;
            //final String stmtTemplate = "select %2$s.%1$s from %2$s where %1$s like 'ENC(%%' limit 2" ; 
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
                
                final ShaPasswordEncoder encoder = new ShaPasswordEncoder();
                
                String encryptedValue = null, qualifiedColumnName = null ;
                int rsCounter = 0 ; 
                try{ 
                    do { 
                       rs = stmt.getResultSet() ;
                       qualifiedColumnName = qualifiedColumnNames[rsCounter++] ; 
                       this.log("Re-encoding Encyrption for Table " + qualifiedColumnName + "...") ;
                       while(rs.next()) { 
                           encryptedValue = rs.getString(1) ; 
                           //now attempt to decrypt the value 
                           encoder.encodePassword(encryptedValue, null) ;
                       }//EO while there are more records 
                    }while(stmt.getMoreResults()) ; 
                }catch(SQLException sqle) { 
                    throw sqle ; 
                }catch(Throwable t) {
                    final String errorMsg = "\n\n>>>>>>>>>>>> User Password Upgrade ERROR: " + 
                            ":\nFailed to encode value(s) from " + 
                            qualifiedColumnName +  
                          " table.\n" +
                          "Aborting Installation/Startup!\n" +
                          "<<<<<<<<<<<<<\n\n" ; 
                    this.log(errorMsg, Project.MSG_ERR) ; 
                    throw new BuildException(errorMsg, t) ; 
                }//EO inner catch block 
            
            }//EO if resultset(s) exist
            
            this.log(": User Password upgrade was successful.", Project.MSG_INFO) ; 
        }catch(Throwable t) { 
            t.printStackTrace() ; 
            throw t ; 
        }finally{ 
            DBUtil.closeJDBCObjects(this._ctx, null/*connection*/, stmt, rs) ; 
        }//EO catch block 
        
    }//EOM 
    
    
}
