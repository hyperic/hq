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

package org.hyperic.tools.ant.dbupgrade;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.sql.Sql;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.codehaus.groovy.runtime.InvokerHelper;

public class SST_Groovy extends SchemaSpecTask {
    private String _script;
    private String _targetDB;
    private String _desc;
    
    public void execute()
        throws BuildException
    {
        try {
            if (_targetDB != null && !targetDbIsValid(_targetDB))
                return;
        } catch(SQLException e) {
            throw new BuildException(e);
        }
        
        if (_script == null) {
            throw new BuildException("No script-body specified");
        }
        
        log("Running script [" + _script + "]");
        if (_desc != null) {
            log(_desc);
        }
        try {
            GroovyClassLoader loader = 
                new GroovyClassLoader(Script.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(loader);
            Class c = loader.parseClass(_script);
            Map parms = new HashMap();
            
            parms.put("CONN", getConnection());
            parms.put("SQL", new Sql(getConnection()));
            parms.put("TASK", this);
            
            Binding context = new Binding(parms);
            Script script = InvokerHelper.createScript(c, context);
            InvokerHelper.invokeMethod(script, "run", new String[] {});
        } catch(BuildException e) {
            throw e;
        } catch(Exception e) {
            throw new BuildException(e);
        }
    }
    
    public void setDesc(String s) {
        _desc = s;
    }

    public String getDesc() {
        return _desc;
    }

    public void setTargetDB(String db) {
        _targetDB = db;
    }
    
    public void addText(String text) {
        _script = text;
    }
}
