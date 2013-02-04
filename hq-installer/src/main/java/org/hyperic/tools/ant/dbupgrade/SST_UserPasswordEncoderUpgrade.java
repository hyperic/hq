package org.hyperic.tools.ant.dbupgrade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;



public class SST_UserPasswordEncoderUpgrade extends SchemaSpecTask {
    private String encryption ; 
    private int strength = 256;//set 256 as default just in case.
    private boolean encodeHashAsBase64;
    
    public final void setEncryption(final String encryption) { 
        this.encryption = encryption ; 
    }//EOM 
    
    public void setStrength(int strength) {
        this.strength = strength;
    }
    
    public void setEncodeHashAsBase64(boolean encodeHashAsBase64) {
        this.encodeHashAsBase64 = encodeHashAsBase64;
    }

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
            final String stmtTemplate = "select %1$s.%2$s from %1$s" ;
            stmt = this._conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY) ; 
            
            String[] tableColumntuples = null ; 
            tableColumntuples = encryption.split("\\.") ;
            
            final StringBuilder stmtBuilder = new StringBuilder() ;
            stmtBuilder.append(String.format(stmtTemplate, tableColumntuples[0], tableColumntuples[1])); 
            stmtBuilder.append(";") ;
             
            final boolean resultSetExists = stmt.execute(stmtBuilder.toString()) ;
            if(resultSetExists) { 
                this.log("Re-encoding Encyrption for Table " + tableColumntuples[0] + "...") ;
                final ShaPasswordEncoder encoder = new ShaPasswordEncoder(strength);
                encoder.setEncodeHashAsBase64(encodeHashAsBase64 );
                
                String encryptedValue = null, qualifiedColumnName = null ;
                int rsCounter = 0 ; 
                try{ 
                    do { 
                       rs = stmt.getResultSet();                     
                       while(rs.next()) { 
                           encryptedValue = rs.getString(1) ; 
                           //now attempt to decrypt the value 
                           updateDBwithNewEncodePassword(tableColumntuples[0], tableColumntuples[1], encryptedValue, encoder.encodePassword(encryptedValue, null));
                           rsCounter++;
                       }//EO while there are more records 
                    }while(stmt.getMoreResults()) ; 
                    this.log("Re-encoded " + rsCounter + " passwords.", Project.MSG_DEBUG);
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
            
            this.log(": User Password upgrade was successful.") ; 
        }catch(Throwable t) { 
            t.printStackTrace() ; 
            throw t ; 
        }finally{ 
            DBUtil.closeJDBCObjects(this._ctx, null/*connection*/, stmt, rs) ; 
        }//EO catch block 
        
    }//EOM 

    private void updateDBwithNewEncodePassword(String table, String column, String orgValue, String newValue) throws SQLException {
        PreparedStatement ps = null;
        String updateSql
        = "UPDATE " + table + " SET " + column + " = ? " + 
                "WHERE " + column + " = ? ;";
        ps = this._conn.prepareStatement(updateSql);
        ps.setObject(1, newValue, java.sql.Types.VARCHAR);
        ps.setObject(2, orgValue, java.sql.Types.VARCHAR);
        ps.executeUpdate();
    }

    
}
