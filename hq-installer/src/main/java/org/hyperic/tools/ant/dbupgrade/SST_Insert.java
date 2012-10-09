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

package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Ant task to add data to a table.
 */
public class SST_Insert extends SchemaSpecTask {

    private static final String MATCH_REGEX = "\\$\\{(.*?)\\}" ;
    private static final Pattern MATCH_PATTERN = Pattern.compile(MATCH_REGEX, Pattern.MULTILINE)  ;
    
    private String table = null;
    private String insertCmd = null;
    private boolean completeStmt ;
    private boolean shouldCommit ; 
    private String bindingParams = null ;
    private String desc = null;
    private boolean dupFail = true;

    public SST_Insert () {}

    public String getDesc() {
        return desc;
    }

    public void setDesc(String d) {
        desc = d;
    }

    public void setTable(String t) {
        table = t;
    }

    public void setInsertCmd(String ic) {
        insertCmd = ic;
    }
    
    public final void setCompleteStmt(final String completeStmt)  { 
        this.insertCmd = completeStmt ;
        this.completeStmt = true ; 
    }//EOM 
    
    public final void setBindingParams(final String bindingParams) { 
        this.bindingParams = bindingParams ; 
    }//EOM 
    
    public final void setCommit(final boolean shouldCommit) { 
        this.shouldCommit = shouldCommit; 
    }//EOM 

    public void setDupFail(String df) {
        dupFail = Boolean.getBoolean(df);
    }

    public void execute() throws BuildException {

        validateAttributes();

        Connection c = getConnection();
        PreparedStatement ps = null;

        //either generate the insert statement or use the complete one 
        String insertSql = (this.completeStmt ? this.insertCmd : ("INSERT INTO " + table + " " + insertCmd) ) ; 
        
        try {
            if(this.bindingParams != null) { 
                ps = this.prepareStatementWithBindingParams(c, insertSql) ; 
            }else {
                
                insertSql = StringUtil.replace(insertSql, "%%TRUE%%", 
                        DBUtil.getBooleanValue(true, c));
                insertSql = StringUtil.replace(insertSql, "%%FALSE%%", 
                           DBUtil.getBooleanValue(false, c));
                ps = c.prepareStatement(insertSql);
            }//EO else if no binding params 
            
            String buf = ((desc == null) ? "" : ">>>>> " + desc + "\n") +
                         ">>>>> Inserting into " + table + " " + insertCmd;
            log(buf);
            ps.executeUpdate();
            if(this.shouldCommit) this._conn.commit() ; 
        } catch (SQLException e) {
            if (dupFail ||
                e.getMessage().toLowerCase().indexOf("constraint") == -1) {
                throw new BuildException("Error inserting data into " + table 
                        + ": " + e, e);
            } else {
                try {
                    log(">>>>> Duplicate insert into " + table + " ignored");
                    c.rollback();
                } catch (SQLException ex) {
                    throw new BuildException(
                        "Error rolling back insert into " + table + ": " + ex,
                            ex);
                }
            }
        } catch ( Throwable e ) {
            throw new BuildException("Error inserting data into " + table 
                                     + ": " + e, e);
        } finally {
            DBUtil.closeStatement(_ctx, ps);
        }
    }

    private void validateAttributes () throws BuildException {
        if (table == null)
            throw new BuildException("SchemaSpec: update: No " +
                                     "'table' attribute specified.");
        if (insertCmd == null)
            throw new BuildException("SchemaSpec: update: No " +
                                     "'insertCmd' attribute specified.");
    }
    
    
    private final PreparedStatement prepareStatementWithBindingParams(final Connection conn, String insertSql) throws Throwable { 
        
        final PreparedStatement ps = conn.prepareStatement(insertSql) ; 
       
        final Project project = this.getProject() ; 
         
        String propertyName = null, bindingParam = null ; 
        Matcher matcher = null ; 
        
        final String[] arrBindingParams = this.bindingParams.split(",") ; 
        final int length = arrBindingParams.length ; 
        
        //replace all ${<property_name} place holders with their actual respective values from the ant environment
        //Note: This is a safenet, the actual replacement should be performed by ant during the db-upgrade.xml
        //target initialization 
        for(int i=0; i < length; i++) { 
            bindingParam = arrBindingParams[i] ; 
            matcher = MATCH_PATTERN.matcher(bindingParam) ;
            if(matcher.find()) { 
                propertyName = matcher.group(1) ; 
                bindingParam = project.getProperty(propertyName) ; 
            }//EO if place holder
            
            ps.setObject(i+1, bindingParam) ; 
                
        }//EO while there are more binding params
        
        return ps ; 
    }//EOM 
    
}
