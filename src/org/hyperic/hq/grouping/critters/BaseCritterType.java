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

package org.hyperic.hq.grouping.critters;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.CritterProp;
import org.hyperic.hq.grouping.prop.CritterPropDescription;
import org.hyperic.hq.grouping.prop.CritterPropType;

/**
 * BaseCritterType provides common routines for setting up type descriptions, 
 * getting localized strings, and validating creation parameters.
 * 
 * Subclasses should call initialize() in the constructor.
 */
public abstract class BaseCritterType
    implements CritterType
{
    private ResourceBundle _bundle;
    private String _propPrefix;
    private String _name;
    private String _desc;
    private List   _propDescs;
    
    public BaseCritterType() {
    }
    
    // XXX this is not really symmetrical since propName
    // in addPropDescription takes a propName where
    // the meaning is not the same.  Check the initialize() method
    // specifically the params it passes in to this method
    // I'd like to remove either this or getComponentName
    protected String getResourceProperty(String propName) {
        return _bundle.getString(_propPrefix + propName).trim();
    }
   
    /**
     * Initialize the name and description of the critter as well as 
     * internal storage for prop descriptions.  
     * 
     * @param bundleName Name of the resource bundle (org.hyperc.hq...Resources)
     * @param propPrefix Prefix to use before all properties.
     * 
     * The BaseCritterType will load properties for in the following form:
     *     propPrefix.critter.name=
     *     propPrefix.critter.desc=
     *     propPrefix.critterProp.propName.name=
     *     propPrefix.critterProp.propName.purpose=
     *     
     * Where propName can be specified for all the different properties a
     * resource supports. 
     */
    protected void initialize(String bundleName, String propPrefix) {
        _bundle     = ResourceBundle.getBundle(bundleName);
        _propPrefix = propPrefix + ".";
        _name       = getResourceProperty("critter.name"); 
        _desc       = getResourceProperty("critter.desc");
        _propDescs  = new ArrayList();
    }
    
    /**
     * Adds a prop description which will get returned via 
     * getPropDescriptions().
     * 
     * @param propName The property name to use when looking up the localized
     *                 value.
     * @param type     The critter prop type
     * @param required If true, the property is required to create the critter
     *                 
     * propName is used in conjunction with the propPrefix (from initialize())
     * The properties loaded from the resource bundle will be of the following
     * form: 
     *     propPrefix.critterProp.propName.name
     *     propPrefix.critterProp.propName.purpose
     *     
     * Each call to addPropDescription adds a new property, and thus will
     * require 2 more localized properties. 
     */
    protected void addPropDescription(String propName, CritterPropType type,
                                      boolean required) 
    { 
        String componentName = getComponentName(propName);
        String componentPurpose = getComponentPurpose(propName);
        _propDescs.add(new CritterPropDescription(type, componentName, 
                                                  componentPurpose, required));
    }

    protected String getComponentPurpose(String propName) {
        return _bundle.getString(new StringBuilder()
                .append(_propPrefix).append("critterProp.")
                .append(propName).append(".purpose").toString().trim());
    }

    protected String getComponentName(String propName) {
        return _bundle.getString(new StringBuilder()
                .append(_propPrefix).append("critterProp.")
                .append(propName).append(".name").toString().trim());
    }

    protected void addPropDescription(String propName, CritterPropType type) {
        addPropDescription(propName, type, true);
    }

    public List getPropDescriptions() {
        return Collections.unmodifiableList(_propDescs);
    }

    public String getDescription() {
        return _desc;
    }

    public String getName() {
        return _name;
    }
    
    public ResourceBundle getBundle() {
        return _bundle;
    }

    /**
     * Validate a list of {@link CritterPropDescription}s against the 
     * previously defined descriptions.
     * 
     * Calls to addPropDescription() pre-populate the critter type with the
     * props that are valid.  This method ensures that a list of 
     * {@link CritterPropDescription}s match the valid types.
     * 
     * @param propDescs a list of {@link CritterProp}s 
     */
    protected void validate(List propDescs) 
        throws GroupException
    {
        for (Iterator it=_propDescs.iterator(); it.hasNext(); ) {
            CritterPropDescription desc = (CritterPropDescription)it.next();
            if (!desc.isRequired()) {
                continue;
            } else if (!containsName(propDescs, desc.getName())) {
                throw new GroupException("CritterPropDescription Name, " +
                    desc.getName() + " does not exist in props being validated");
            }
        }
        for (Iterator it=propDescs.iterator(); it.hasNext(); ) {
            CritterProp prop = (CritterProp)it.next();
            if (!containsName(prop.getName())) {
                throw new GroupException("CritterPropDescription Name, " +
                    prop.getName() + " does not exist in this Object's " +
                    "CritterPropDescriptions");
            }
        }
    }
    
    private boolean containsName(String name) {
        for (Iterator it=_propDescs.iterator(); it.hasNext(); ) {
            CritterPropDescription desc = (CritterPropDescription)it.next();
            if (desc.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsName(List props, String name) {
        for (Iterator it=props.iterator(); it.hasNext(); ) {
            CritterProp prop = (CritterProp)it.next();
            if (prop.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns a localized MessageFormat, useful for returning the 
     * critter config.
     * 
     * @see Critter#getConfig()
     */
    public MessageFormat getInstanceConfig() {
        return new MessageFormat(_bundle.getString(_propPrefix + 
                                                   "critter.config"));
    }
}
