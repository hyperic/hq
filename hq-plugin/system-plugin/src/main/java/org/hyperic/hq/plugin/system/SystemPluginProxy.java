/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.system;

import java.io.InputStream;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

/**
 * Proxy class delegating all operations to a SystemPlugin instance.<br/>
 * <br/> 
 * The class is defined as the product sub plugin the hq-plugin.xml file. 
 * <br/>
 * Its purpose it to support the org and com different implementations of the actual<br/>
 * product system plugin instances 
 *    
 */
public class SystemPluginProxy extends ProductPlugin{
    
    private static final String PRODUCT_PLUGIN_FILE_NAME = "/custom-system-product-plugin.txt" ;
    private SystemPlugin delegate ;
    
    
    /**
     * If the {@link #PRODUCT_PLUGIN_FILE_NAME} file exists in classpath of the given system-plugin<br/>
     * load the instance defined in it else resort to the {@link SystemPlugin}
     */
    @Override 
    public final void init(final PluginManager manager) throws PluginException {
        //look for the PRODUCT_PLUGIN_FILE_NAME resource and if found, 
        //read the custom delegate class name, else resort to the SystemPlugin class        
        try{ 
            final InputStream is = this.getClass().getResourceAsStream(PRODUCT_PLUGIN_FILE_NAME) ;
            if(is != null) { 
                try{ 
                    final StringBuilder classNameBuilder = new StringBuilder() ; 
                    while(is.available() > 0) {
                        classNameBuilder.append((char)is.read()) ;
                    }//EO while there are more bytes to read 
                    
                    @SuppressWarnings("unchecked")
                    final Class<? extends SystemPlugin> customCls = (Class<? extends SystemPlugin>) Class.forName(classNameBuilder.toString()) ; 
                    this.delegate = customCls.newInstance() ; 
                }finally{ 
                    is.close() ; 
                }//EO catch block
            }else{ 
                this.delegate = new SystemPlugin() ; 
            }//EO if no custom class was defined 
            
            //init the delegate 
            this.delegate.init(manager) ;
            
        }catch(Throwable t) { 
            this.getLog().error(t) ; 
            throw new PluginException(t) ;
        }//EO catch block 
        
    }//EOM 
    
    @Override
    public final ConfigSchema getConfigSchema(final TypeInfo info, final ConfigResponse config) {
        return this.delegate.getConfigSchema(info, config) ; 
    }//EOM 
    
    @Override
    public final ConfigSchema getCustomPropertiesSchema(final String name) {
        return this.delegate.getCustomPropertiesSchema(name);
    }//EOM 
    
    @Override
    public final GenericPlugin getPlugin(final String type, final TypeInfo info) {
        return this.delegate.getPlugin(type, info);
    }//EOM 
    
    @Override
    public final TypeInfo[] getTypes() {
        return this.delegate.getTypes();
    }//EOM 
    
    @Override
    protected final int getDeploymentOrder() {
        return (this.delegate == null ? SystemPlugin.DEPLOYMENT_ORDER : this.delegate.getDeploymentOrder()) ;  
    }//EOM
    
    @Override
    public final void configure(final ConfigResponse config) throws PluginException {
        super.configure(config);
    }//EOM 

}//EOI 

