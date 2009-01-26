/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.ui;

import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.util.StringUtil;

/**
 * A class representing an individual page layout.
 *
 */
public class Portal {
    
    private String _name;
    private String _description;
    private int _columns = 0;
    private List _portlets = new ArrayList();
    private boolean _dialog = false;
    private boolean _workflowPortal = false;

    private Map _workflowParams = null;
    
    public Portal() {
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public int getColumns() {
        return _columns;
    }

    public void setColumns(int columns) {
        if (_portlets.size() < columns) {
            while (_columns < columns) {
                _portlets.add(new ArrayList());
                _columns = _portlets.size();
            }
        }
    }
    
    public void setColumns( String numCols ) { 
        setColumns( Integer.parseInt(numCols) );
    }

    public List getPortlets() {
        return _portlets;
    }

    public void setPortlets(List portlets) {
        _portlets = portlets;
    }

    /**
     * adds a single portlet to the column number provided
     */
    public void addPortlet(Portlet portlet, int column) {
        if (column > _columns) {
            setColumns(column);
        }
            
        List col = (List) _portlets.get(column - 1);
        col.add(portlet);
    }
    
    /**
     * Attach a list of portlets to the first column of the portal.
     *
     * @param definitions the <code>List</code> of either
     * <code>Portlet</code> instances or <code>String</code> instances
     * representing portlet definitions
     */
    public void addPortlets(List definitions) {
        addPortlets(definitions, 1);
    }

    /**
     * Attach a list of portlets to the indicated column of the
     * portal.
     *
     * @param definitions the <code>List</code> of either
     * <code>Portlet</code> instances or <code>String</code> instances
     * representing portlet definitions
     * @param column the column (1-based) to which the portlets are
     * added
     */
    public void addPortlets(List definitions, int column) {
        Iterator i = definitions.iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (o instanceof Portlet) {
                addPortlet((Portlet) o, column);
            }
            else {
                addPortlet(new Portlet((String) o), column);
            }
        }
    }

    public String toString() {
        return "Portal [" + getName()+ "]";
    }
    
    public boolean isDialog() {
        return _dialog;
    }

    public void setDialog(boolean dialog) {
        _dialog = dialog;
    }

    public void setDialog(String dialog) {
        _dialog = Boolean.valueOf(dialog).booleanValue();
    }
    
    /** Getter for property workflowPortal.
     *
     * If you wish to explictly set this screen
     * as part of a workflow, set this to true
     * in your controller action.
     *
     * @return Value of property workflowPortal.
     */
    public boolean isWorkflowPortal() {
        return _workflowPortal;
    }
    public void setWorkflowPortal(boolean workflowPortal) {
        _workflowPortal = workflowPortal;
    }    
    public void setWorkflowPortal(String workflowPortal) {
        if("true".equalsIgnoreCase( workflowPortal ) ){
            _workflowPortal = true;
        }
        else {
            _workflowPortal = false;
        }
    }

    public Map getWorkflowParams() {
        return _workflowParams;
    }

    public void setWorkflowParams(Map m) {
        _workflowParams = m;
    }

    /**
     * Create and return a new instance.
     */
    public static Portal createPortal() {
        Portal portal =  new Portal();

        portal.setColumns(1);
        portal.setDialog(false);

        return portal;
    }

    /**
     * Create and return a new named instance with a portlet in the
     * first column.
     *
     * @param portalName the portal name
     * @param portletName the portlet definition name
     */
    public static Portal createPortal(String portalName, String portletName) {
        Portal portal = createPortal();
        portal.setName(portalName);

        List definitions = new ArrayList();
        definitions.add(portletName);
        portal.addPortlets(definitions);

        return portal;
    }
    
    public void addPortletsFromString(String stringList, int column) {
        //convert string to portlets then call addPortlets().
        
        List StringColumn = StringUtil.explode(stringList,
                                               Constants.DASHBOARD_DELIMITER);

        for (int i = 0; i < StringColumn.size(); i++) {
            String tile = (String) StringColumn.get(i);
            
            Portlet portlet;
            int index;
            if ((index = tile.indexOf(DashboardUtils.MULTI_PORTLET_TOKEN)) < 0) {
                portlet = new Portlet(tile);
            }
            else {
                // This is a mult-portlet
                portlet = new Portlet(tile.substring(0, index), tile);
            }
            
            if (i == 0)
                portlet.setIsFirst();
            if (i == (StringColumn.size() - 1))
                portlet.setIsLast();
            addPortlet(portlet, column);
        }
    }

    /** Participate in workflow in the following circumstances:
     *
     * dialog isWorkflowPortal  participate
     *   0        0              1
     *   0        1              0
     *   1        0              0
     *   1        1              1
     */
    public boolean doWorkflow() {
        return isDialog() == isWorkflowPortal();
    }
    
}
