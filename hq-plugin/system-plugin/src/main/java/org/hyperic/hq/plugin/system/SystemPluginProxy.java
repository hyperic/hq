package org.hyperic.hq.plugin.system;

import java.io.InputStream;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class SystemPluginProxy extends ProductPlugin{
    
    private static final String PRODUCT_PLUGIN_FILE_NAME = "/custom-system-product-plugin.txt" ;
    private SystemPlugin delegate ; 
    
    @Override 
    public void init(PluginManager manager) throws PluginException {
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

