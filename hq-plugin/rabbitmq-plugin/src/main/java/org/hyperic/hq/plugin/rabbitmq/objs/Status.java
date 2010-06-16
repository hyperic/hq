/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.objs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author administrator
 */
public class Status {

    private Map applications = new HashMap();
    private List nodes = new ArrayList();
    private List runningModes = new ArrayList();

    public void addApplication(Application app) {
        applications.put(app.getName(), app);
    }

    public void addNode(String node) {
        nodes.add(node);
    }

    public void addRunningModes(String node) {
        runningModes.add(node);
    }

    public Application getApplication(String name) {
        Application res = (Application) applications.get(name);
        if (res == null) {
            throw new IllegalArgumentException("Application '" + name + "' not found");
        }
        return res;
    }

    public Iterator getApplications() {
        return applications.values().iterator();
    }

    public List getNodes() {
        return nodes;
    }

    public List getRunningModes() {
        return runningModes;
    }

    @Override
    public String toString() {
        return "['applications:" + applications + "' ,nodes:'" + nodes + "' ,runningModes:'" + runningModes + "']";
    }
}
