package org.hyperic.hq.api.transfer.mapping;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.transfer.impl.ResourceTransferImpl.Context;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefUtil;

public enum ResourceDetailsTypeStrategy {
    BASIC{ 
        @Override
        public final ResourceModel populateResource(final Context flowContext) throws Throwable{ 
            return flowContext.currResource = flowContext.getVisitor().getResourceMapper().toResource(flowContext.backendResource) ;
            
        }//EOM 
    },//EO BASIC 
    PROPERTIES{ 
        /**
         * @throws PluginException 
         * @throws EncodingException 
         * @throws PermissionException 
         * @throws PluginNotFoundException 
         * @throws ConfigFetchException 
         * @throws AppdefEntityNotFoundException 
         */
        @Override
        public final ResourceModel populateResource(final Context flowContext) throws Throwable { 
            ResourceModel resource = flowContext.currResource ; 
            if(resource == null) { 
                resource = flowContext.currResource = new ResourceModel(flowContext.internalID) ; 
            }//EO if resource was not initialized yet 
            //init the response config metadata 
            
            flowContext.entityID = AppdefUtil.newAppdefEntityId(flowContext.backendResource) ;
            flowContext.getVisitor().initResourceConfig(flowContext) ;
            return flowContext.getVisitor().getResourceMapper().mergeConfig(
                flowContext.resourceType, flowContext.backendResource, resource,
                flowContext.configResponses, flowContext.cprops) ; 
        }//EOM 
    },//EO PROPERTIES
    VIRTUALDATA{
        @Override
        public final ResourceModel populateResource(final Context flowContext) throws Throwable {
            BASIC.populateResource(flowContext) ;
            ResourceModel resource = flowContext.currResource ; 
            if(resource == null) { 
                resource = flowContext.currResource = new ResourceModel(flowContext.internalID) ; 
            }//EO if resource was not initialized yet 
            //init the response config metadata 
            
            flowContext.entityID = AppdefUtil.newAppdefEntityId(flowContext.backendResource) ;
            flowContext.getVisitor().initResourceConfig(flowContext) ;
            return flowContext.getVisitor().getResourceMapper().mergeVirtualData(
                flowContext.resourceType, flowContext.backendResource, resource, flowContext.cprops) ; 
        }        
    },
    ALL{ 
        @Override
        public final ResourceModel populateResource(final Context flowContext) throws Throwable{ 
            BASIC.populateResource(flowContext) ;
            return PROPERTIES.populateResource(flowContext) ;
        }//EOM
        
        @Override 
        protected final SortedSet<ResourceDetailsTypeStrategy> addToSuperset(SortedSet<ResourceDetailsTypeStrategy> setUniqueResourceDetails) { 
            setUniqueResourceDetails.clear() ;
            return super.addToSuperset(setUniqueResourceDetails) ;  
        }//EOM
    };//EO ALL 
    
    protected SortedSet<ResourceDetailsTypeStrategy> addToSuperset(SortedSet<ResourceDetailsTypeStrategy> setUniqueResourceDetails) { 
        setUniqueResourceDetails.add(this) ; 
        return setUniqueResourceDetails ; 
    }//EOM 
            
    public abstract ResourceModel populateResource(final Context flowContext) throws Throwable;  
    
    public static final Set<ResourceDetailsTypeStrategy> valueOf(final ResourceDetailsType[] arrResourceDetailsTypes) { 
        final SortedSet<ResourceDetailsTypeStrategy> setUniqueResourceDetails = new TreeSet<ResourceDetailsTypeStrategy>() ; 

        if(arrResourceDetailsTypes == null || arrResourceDetailsTypes.length == 0) { 
            setUniqueResourceDetails.add(ALL) ; 
            return setUniqueResourceDetails ; 
        }//EO if all 

        ResourceDetailsTypeStrategy enumResourceDetailsTypeStrategy = null ; 
        
        for(ResourceDetailsType enumResourceDetailsType : arrResourceDetailsTypes) { 
            try{ 
                enumResourceDetailsTypeStrategy = ResourceDetailsTypeStrategy.valueOf(enumResourceDetailsType.name()) ; 
                enumResourceDetailsTypeStrategy.addToSuperset(setUniqueResourceDetails) ;  
            }catch(Throwable t) { 
                t.printStackTrace() ; 
            }
        }//EO while there are more arrResourceDetailsTypes
        
        return setUniqueResourceDetails ; 
    }//EOM 
}
