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
package org.hyperic.hq.bizapp.explorer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.explorer.providers.HQUGroupProvider;
import org.hyperic.hq.bizapp.explorer.types.GroupItemType;
import org.hyperic.hq.bizapp.explorer.types.GroupManagerRootItemType;
import org.hyperic.util.StringUtil;

/**
 * This manager provides a registry and utility methods when dealing with
 * ExplorerItems and trees.
 * 
 * The manager is also used to traverse an item hierarchy.  The methods
 * {@link #findAllChildren(ExplorerContext, ExplorerItem)} and
 * {@link #findChild(ExplorerContext, ExplorerItem, String)} can be used
 * to deal with item children.
 * 
 * {@link ExporerItem}s essentially have 2 sets of children .. ones which
 * are implicit to them {@link ExplorerItem#getChildren()} and ones which
 * are adopted, and specified by some other {@link ExplorerItemType}.  Calling
 * the hierarchy methods from the manager ensures that both sets of children
 * are accounted for.
 */
public class ExplorerManager {
    private static final ExplorerManager INSTANCE = new ExplorerManager();
    private static final Log _log = 
        LogFactory.getLog(ExplorerManager.class);

    private final Map _types;
    private final Map _viewProviders;
    
    private ExplorerManager() {
        _types = new HashMap();
        
        // Seed our internal types 
        GroupManagerRootItemType rootType = new GroupManagerRootItemType();
        _types.put(rootType.getName(), rootType);
        
        GroupItemType groupType = new GroupItemType();
        _types.put(groupType.getName(), groupType);
        
        _viewProviders = new HashMap();
        HQUGroupProvider gProvider = new HQUGroupProvider();
        
        _viewProviders.put(gProvider.getName(), gProvider);
    }
    
    public void registerProvider(HQUGroupProvider provider) {
        synchronized (_viewProviders) {
            _viewProviders.put(provider.getName(), provider);
        }
    }
    
    public void unregisterProvider(String providerName) {
        synchronized (_viewProviders) {
            _viewProviders.remove(providerName);
        }
    }
    
    /**
     * Get a list of {@link ExplorerView}s relevant to the specified item.
     * 
     * @return a list of {@link ExplorerView}s 
     */
    public List getViewsFor(ExplorerContext ctx, ExplorerItem item) {
        Collection providers;
        List res = new ArrayList();
        
        synchronized (_viewProviders) {
            providers = new ArrayList(_viewProviders.values());
        }
        
        for (Iterator i=providers.iterator(); i.hasNext(); ) {
            ExplorerViewProvider p = (ExplorerViewProvider)i.next();
            
            res.addAll(p.getViewFor(ctx, item));
        }
        return res;
    }
    
    /**
     * Register an item type which can be used for filling out explorer
     * trees.
     * 
     * The type is keyed off {@link ExplorerItemType#getName()}, so any
     * item type previously using that name will be unregistered.
     */
    public void registerType(ExplorerItemType type) {
        _log.info("Registring explorer item type [" + type.getName() + "]");
        synchronized (_types) {
            _types.put(type.getName(), type);
        }
    }
    
    /**
     * Unregister a previously registered item type.
     * 
     * @param name the name of the item type, as from 
     *             {@link ExplorerItemType#getName()}
     */
    public void unregisterType(String name) {
        _log.info("Unregistring explorer item type [" + name + "]");
        synchronized (_types) {
            _types.remove(name);
        }
    }

    /**
     * Get an item type by name
     * 
     * @see ExplorerItemType#getName()
     */
    public ExplorerItemType getType(String name) {
        synchronized (_types) {
            return (ExplorerItemType)_types.get(name);
        }
    }

    /**
     * Get a collection of all {@link ExplorerItemType}s
     */
    public Collection getAllTypes() {
        synchronized (_types) {
            return new ArrayList(_types.values());
        }
    }
    
    /**
     * Find a child of the given parent.
     * 
     * This method should be used (instead of parent.getChildren()), since
     * a parent may have an adopted child specifying that code.
     */
    public ExplorerItem findChild(ExplorerContext ctx, ExplorerItem parent,
                                  String code)
    {
        Collection types = getAllTypes();
        
        for (Iterator i=types.iterator(); i.hasNext(); ) {
            ExplorerItemType type = (ExplorerItemType)i.next();
            ExplorerItem res;
            
            res = type.getChild(ctx, parent, code);
            if (res != null)
                return res;
        }
        return null;
    }
    
    /**
     * Find all children of a given parent, adopted and otherwise.
     */
    public List findAllChildren(ExplorerContext ctx, ExplorerItem parent) {
        Collection types = getAllTypes();
        List res;
        
        if (parent != null) {
            res = new ArrayList(parent.getChildren());
        } else {
            res = new ArrayList();
        }
        
        for (Iterator i=types.iterator(); i.hasNext(); ) {
            ExplorerItemType type = (ExplorerItemType)i.next();
            Collection adopted = type.getAdoptedChildren(ctx, parent);
            
            if (adopted != null) {
                res.addAll(adopted);
            }
        }
        return res;
    }
    
    /**
     * Compose a path of codes leading up to (and including) the item.
     * 
     * @return a path like:  '/gRoot/g=1'
     */
    public String composePathTo(ExplorerItem item) {
        if (item == null)
            return "/";
        
        List codes = new ArrayList(5);
        codes.add(item.getCode());

        ExplorerItem up = item.getParent();
        while (up != null) {
            codes.add(up.getCode());
            up = up.getParent();
        }
        Collections.reverse(codes);
        
        return "/" + StringUtil.implode(codes, "/");
    }
    
    /**
     * Find an item by code, given a full path.
     * 
     * A path is simply a series of ExplorerItem codes, separated by a /
     * 
     * e.g.:  findItem(ctx, "/gRoot/g=1")
     *   finds the GroupItem representing group id=1 under the root 
     */
    public ExplorerItem findItem(ExplorerContext ctx, String path) {  
        List codes = StringUtil.explode(path, "/");
        ExplorerItem parent = null;
        
        for (Iterator i=codes.iterator(); i.hasNext(); ) {
            String code = (String)i.next();
            ExplorerItem child = findChild(ctx, parent, code);
            
            if (child == null) {
                _log.warn("Child [" + code + "] of parent=" + parent +
                          " not found in path [" + path + "]");
                return null;
            }
            if (!i.hasNext()) {
                return child;
            }
        }
        
        assert false: "Shouldn't ever get here";
        return null;
    }
    
    public static ExplorerManager getInstance() {
        return INSTANCE;
    }
}
