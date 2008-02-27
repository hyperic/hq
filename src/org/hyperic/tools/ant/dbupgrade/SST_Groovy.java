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

    public void setTargetDB(String db) {
        _targetDB = db;
    }
    
    public void addText(String text) {
        _script = text;
    }
}
